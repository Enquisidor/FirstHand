# FirstHand Backend Architecture

Comprehensive backend design using Scala/Akka for compliance management.

## Design Principles

1. **Event Sourcing**: Audit trail for all compliance changes
2. **Actor Hierarchy**: Clear supervision and fault tolerance
3. **Persistence**: Durable state for critical entities
4. **Idempotency**: Safe retry of external API calls
5. **Scalability**: Cluster-ready for horizontal scaling
6. **Separation of Concerns**: Clear boundaries between domains

---

## Akka Actor Architecture

### Actor Hierarchy

```
ActorSystem("firsthand-system")
├── Guardian (User)
│   ├── ProducerSupervisor
│   │   └── ProducerActor[producerId]
│   │       ├── ComplianceActor[producerId]
│   │       ├── DocumentManagerActor[producerId]
│   │       └── NotificationActor[producerId]
│   │
│   ├── BuyerSupervisor (FL: minimal for Step 1)
│   │   └── BuyerActor[buyerId]
│   │
│   ├── ComplianceFrameworkSupervisor
│   │   └── ComplianceFrameworkActor[frameworkId]
│   │
│   └── SystemSupervisor
│       ├── InspectionSyncCoordinator
│       │   └── InspectionSyncActor[healthUnit]
│       │       └── InspectionImportActor[inspectionId] (transient)
│       │
│       ├── DocumentExpiryChecker (scheduled)
│       ├── ReminderScheduler (scheduled)
│       └── AnalyticsAggregator (scheduled)
│
├── HttpServer (Akka HTTP)
│
└── ClusterSingleton (for scheduled jobs)
```

---

## Core Actors (Step 1 Focus)

### 1. ProducerActor
**Lifecycle**: Persistent, one per producer

**Purpose**: Aggregate root for producer entity

**State**:
```scala
case class ProducerState(
  id: ProducerId,
  profile: ProducerProfile,
  visibilitySettings: VisibilitySettings,
  subscriptionId: SubscriptionId,
  complianceStatus: ComplianceStatus,
  lastUpdated: Instant,
  version: Long
)
```

**Commands**:
```scala
sealed trait ProducerCommand
case class CreateProducer(profile: ProducerProfile) extends ProducerCommand
case class UpdateProfile(updates: ProfileUpdates) extends ProducerCommand
case class UpdateVisibility(settings: VisibilitySettings) extends ProducerCommand
case class UpdateSubscription(subscriptionId: SubscriptionId) extends ProducerCommand
case class GetProducerState() extends ProducerCommand
```

**Events** (persisted):
```scala
sealed trait ProducerEvent
case class ProducerCreated(profile: ProducerProfile, timestamp: Instant)
case class ProfileUpdated(updates: ProfileUpdates, timestamp: Instant)
case class VisibilityChanged(settings: VisibilitySettings, timestamp: Instant)
case class ComplianceStatusChanged(status: ComplianceStatus, timestamp: Instant)
```

**Supervision**: Restart on failure, escalate after 3 retries

**Children**:
- ComplianceActor (manages compliance for this producer)
- DocumentManagerActor (handles documents)
- NotificationActor (sends notifications)

**Persistence**: Akka Persistence with event sourcing

---

### 2. ComplianceActor
**Lifecycle**: Persistent, child of ProducerActor

**Purpose**: Manages compliance state for a single producer

**State**:
```scala
case class ComplianceState(
  producerId: ProducerId,
  frameworkAssignments: Map[FrameworkId, FrameworkAssignment],
  records: Map[RequirementId, ComplianceRecord],
  overallStatus: ComplianceStatus,
  expiringDocuments: List[DocumentReference],
  lastCalculated: Instant
)

case class FrameworkAssignment(
  frameworkId: FrameworkId,
  applicableCategories: Set[CategoryId],
  applicableRequirements: Set[RequirementId],
  assignedAt: Instant
)
```

**Commands**:
```scala
sealed trait ComplianceCommand
case class AssignFramework(frameworkId: FrameworkId) extends ComplianceCommand
case class UpdateRecordStatus(
  recordId: RecordId,
  status: ComplianceItemStatus,
  updatedBy: UserId
) extends ComplianceCommand
case class AttachDocument(recordId: RecordId, document: DocumentMetadata)
case class RemoveDocument(recordId: RecordId, documentId: DocumentId)
case class ImportInspection(inspection: InspectionRecord)
case class CheckExpiry() extends ComplianceCommand
case class CalculateOverallStatus() extends ComplianceCommand
case class GetComplianceState() extends ComplianceCommand
```

**Events**:
```scala
sealed trait ComplianceEvent
case class FrameworkAssigned(frameworkId: FrameworkId, requirements: Set[RequirementId])
case class RecordStatusChanged(
  recordId: RecordId,
  previousStatus: ComplianceItemStatus,
  newStatus: ComplianceItemStatus,
  timestamp: Instant,
  updatedBy: UserId
)
case class DocumentAttached(recordId: RecordId, document: DocumentMetadata)
case class InspectionImported(inspection: InspectionRecord, affectedRecords: List[RecordId])
case class DocumentExpired(documentId: DocumentId, recordId: RecordId)
case class OverallStatusCalculated(status: ComplianceStatus)
```

**Business Logic**:
- Calculate overall compliance status from individual records
- Determine critical vs. non-critical gaps
- Auto-update status when inspections show infractions
- Trigger reminders for expiring documents
- Enforce gating rules (e.g., can't go public without critical compliance)

**Persistence**: Event sourced with snapshots every 50 events

---

### 3. DocumentManagerActor
**Lifecycle**: Persistent, child of ProducerActor

**Purpose**: Manages document lifecycle for a producer

**State**:
```scala
case class DocumentManagerState(
  producerId: ProducerId,
  documents: Map[DocumentId, DocumentRecord],
  storageUsage: Long, // bytes
  uploadQuota: Long   // from subscription
)

case class DocumentRecord(
  id: DocumentId,
  complianceRecordId: RecordId,
  metadata: DocumentMetadata,
  storageUrl: String,
  uploadedAt: Instant,
  uploadedBy: UserId,
  verified: Boolean,
  verifiedBy: Option[UserId],
  expiryDate: Option[LocalDate]
)
```

**Commands**:
```scala
sealed trait DocumentCommand
case class UploadDocument(
  complianceRecordId: RecordId,
  fileName: String,
  fileSize: Long,
  mimeType: String,
  uploadedBy: UserId
) extends DocumentCommand
case class ConfirmUpload(documentId: DocumentId, storageUrl: String)
case class DeleteDocument(documentId: DocumentId, deletedBy: UserId)
case class VerifyDocument(documentId: DocumentId, verifiedBy: UserId)
case class CheckStorageQuota() extends DocumentCommand
```

**Events**:
```scala
case class DocumentUploadInitiated(documentId: DocumentId, metadata: DocumentMetadata)
case class DocumentUploadConfirmed(documentId: DocumentId, storageUrl: String)
case class DocumentDeleted(documentId: DocumentId, deletedBy: UserId)
case class DocumentVerified(documentId: DocumentId, verifiedBy: UserId)
```

**Integration**:
- Pre-signed URLs for Firebase Storage uploads
- Quota enforcement based on subscription tier
- Virus scanning (future via Cloud Functions)

---

### 4. ComplianceFrameworkActor
**Lifecycle**: Persistent, singleton per framework

**Purpose**: Manages master compliance framework data

**State**:
```scala
case class FrameworkState(
  id: FrameworkId,
  metadata: FrameworkMetadata,
  categories: Map[CategoryId, ComplianceCategory],
  requirements: Map[RequirementId, ComplianceRequirement],
  version: Int
)
```

**Commands**:
```scala
sealed trait FrameworkCommand
case class CreateFramework(metadata: FrameworkMetadata)
case class AddCategory(category: ComplianceCategory)
case class UpdateCategory(categoryId: CategoryId, updates: CategoryUpdates)
case class AddRequirement(requirement: ComplianceRequirement)
case class UpdateRequirement(requirementId: RequirementId, updates: RequirementUpdates)
case class GetFramework()
case class GetApplicableRequirements(businessType: BusinessType, activities: Set[FoodActivity])
```

**Events**:
```scala
case class FrameworkCreated(metadata: FrameworkMetadata)
case class CategoryAdded(category: ComplianceCategory)
case class RequirementAdded(requirement: ComplianceRequirement)
case class FrameworkVersionIncremented(newVersion: Int)
```

**Note**: Admin-only modifications via separate admin API

---

## Integration Actors

### 5. InspectionSyncCoordinator
**Lifecycle**: Cluster singleton (one per cluster)

**Purpose**: Coordinates inspection data sync from multiple health units

**State**:
```scala
case class SyncCoordinatorState(
  healthUnits: Map[HealthUnitId, HealthUnitConfig],
  lastSyncTimestamps: Map[HealthUnitId, Instant],
  activeSyncs: Set[HealthUnitId]
)

case class HealthUnitConfig(
  id: HealthUnitId,
  name: String,
  apiEndpoint: String,
  syncSchedule: String, // cron expression
  enabled: Boolean
)
```

**Behavior**:
- Scheduled job runs daily at configured times
- Spawns InspectionSyncActor per health unit
- Aggregates sync results
- Reports errors to admin dashboard

---

### 6. InspectionSyncActor
**Lifecycle**: Transient, spawned per health unit per sync

**Purpose**: Sync inspections from a single health unit API

**Commands**:
```scala
case class StartSync(healthUnitId: HealthUnitId, since: Option[Instant])
case class ProcessInspectionBatch(inspections: List[RawInspection])
```

**Behavior**:
1. Fetch inspections from API (with pagination)
2. Transform to InspectionRecord
3. Spawn InspectionImportActor per inspection
4. Aggregate results and report to coordinator

**Error Handling**:
- Circuit breaker for API calls
- Exponential backoff on failures
- Record failed inspections for manual review

---

### 7. InspectionImportActor
**Lifecycle**: Transient, one per inspection

**Purpose**: Import a single inspection and match to producer

**Workflow**:
```scala
1. Receive InspectionRecord
2. Attempt auto-match to Producer by:
   - Exact address match
   - Fuzzy name match (Levenshtein distance)
   - Premises ID match (if available)
3. If matched:
   - Send ImportInspection command to ComplianceActor
   - Update InspectionRecord with producerId
4. If not matched:
   - Store as unmatched for manual review
   - Create admin notification
5. Persist to Firestore
6. Stop self
```

**Matching Algorithm**:
```scala
def matchProducer(inspection: InspectionRecord): Option[ProducerId] = {
  // 1. Try exact premises ID match
  premisesIdMatch(inspection.establishmentId) orElse

  // 2. Try exact address + name match
  exactAddressNameMatch(inspection) orElse

  // 3. Try fuzzy match (threshold: 85% similarity)
  fuzzyMatch(inspection)
}
```

---

## Scheduled Jobs Actors

### 8. DocumentExpiryChecker
**Lifecycle**: Cluster singleton, scheduled daily

**Purpose**: Check for expired documents and update status

**Workflow**:
```scala
1. Query Firestore for documents expiring in next 30 days
2. Group by ProducerId
3. For each producer:
   - Send CheckExpiry to ComplianceActor
   - ComplianceActor updates record status
   - Triggers NotificationActor to send reminder
```

**Schedule**: Daily at 1 AM EST

---

### 9. ReminderScheduler
**Lifecycle**: Cluster singleton, scheduled

**Purpose**: Send renewal reminders for compliance items

**State**:
```scala
case class ReminderState(
  pendingReminders: List[ReminderTask],
  sentReminders: Map[RecordId, Instant]
)

case class ReminderTask(
  producerId: ProducerId,
  recordId: RecordId,
  requirementName: String,
  expiryDate: LocalDate,
  daysUntilExpiry: Int
)
```

**Reminder Schedule**:
- 30 days before expiry
- 14 days before expiry
- 7 days before expiry
- 1 day before expiry
- Day of expiry

**Behavior**:
- Query for items expiring soon
- Check if reminder already sent
- Send reminder via NotificationActor
- Record reminder sent timestamp

**Schedule**: Daily at 9 AM EST

---

### 10. AnalyticsAggregator
**Lifecycle**: Cluster singleton, scheduled

**Purpose**: Aggregate compliance metrics for analytics

**Metrics Calculated**:
```scala
- Overall compliance rate by region
- Average time to compliance by business type
- Most common compliance gaps
- Document upload trends
- Inspection infraction patterns
- Subscription tier distribution
```

**Output**:
- Write aggregated metrics to Firestore
- Expose via analytics API
- Admin dashboard visualization

**Schedule**: Hourly

---

## Supporting Actors

### 11. NotificationActor
**Lifecycle**: Child of ProducerActor

**Purpose**: Send notifications for a producer

**Commands**:
```scala
case class SendReminderEmail(
  recipientEmail: String,
  requirementName: String,
  expiryDate: LocalDate
)
case class SendInspectionAlert(inspection: InspectionRecord)
case class SendComplianceStatusChange(
  previousStatus: ComplianceStatus,
  newStatus: ComplianceStatus
)
```

**Integration**:
- SendGrid for email (or Firebase Cloud Functions)
- Future: SMS via Twilio
- Future: Push notifications via Firebase Cloud Messaging

**Error Handling**:
- Retry failed sends (max 3 attempts)
- Dead letter queue for permanent failures

---

### 12. SubscriptionActor
**Lifecycle**: Persistent, one per subscription

**Purpose**: Manage subscription lifecycle and feature access

**State**:
```scala
case class SubscriptionState(
  id: SubscriptionId,
  organizationId: OrganizationId,
  tier: SubscriptionTier,
  status: SubscriptionStatus,
  currentPeriodStart: Instant,
  currentPeriodEnd: Instant,
  features: Set[SubscriptionFeature],
  usage: UsageMetrics
)

case class UsageMetrics(
  documentStorageUsed: Long,
  activeComplianceChecklists: Int,
  userCount: Int
)
```

**Commands**:
```scala
case class CreateSubscription(organizationId: OrganizationId, tier: SubscriptionTier)
case class UpgradeSubscription(newTier: SubscriptionTier)
case class DowngradeSubscription(newTier: SubscriptionTier)
case class CancelSubscription()
case class RenewSubscription()
case class CheckFeatureAccess(feature: SubscriptionFeature)
case class UpdateUsageMetrics(metrics: UsageMetrics)
```

**Events**:
```scala
case class SubscriptionCreated(organizationId: OrganizationId, tier: SubscriptionTier)
case class SubscriptionUpgraded(previousTier: SubscriptionTier, newTier: SubscriptionTier)
case class SubscriptionCancelled(reason: Option[String])
case class SubscriptionRenewed(newPeriodEnd: Instant)
```

**Integration**:
- Stripe for payment processing (future)
- Feature gate checks throughout system

---

## Akka HTTP Routes

### Route Structure

```scala
/api/v1
├── /producers
│   ├── POST   /                    # Create producer
│   ├── GET    /:id                 # Get producer
│   ├── PUT    /:id                 # Update producer
│   ├── GET    /:id/compliance      # Get compliance status
│   ├── POST   /:id/compliance/assign-framework
│   ├── PUT    /:id/compliance/records/:recordId
│   ├── POST   /:id/compliance/documents/upload
│   └── GET    /:id/inspections     # Get inspection history
│
├── /buyers (FL: minimal for Step 1)
│   ├── POST   /                    # Create buyer
│   ├── GET    /:id                 # Get buyer
│   └── PUT    /:id                 # Update buyer
│
├── /compliance
│   ├── GET    /frameworks          # List frameworks
│   ├── GET    /frameworks/:id      # Get framework details
│   ├── GET    /templates           # List document templates
│   └── GET    /templates/:id/download
│
├── /search
│   ├── GET    /producers           # Search producers (buyer view)
│   └── GET    /regions             # List Ontario regions
│
├── /admin
│   ├── POST   /frameworks          # Create framework (admin)
│   ├── POST   /frameworks/:id/categories
│   ├── POST   /frameworks/:id/requirements
│   ├── GET    /inspections/unmatched # List unmatched inspections
│   ├── POST   /inspections/:id/match # Manual match
│   └── GET    /analytics           # System analytics
│
└── /health                         # Health check
```

---

## Akka Persistence Strategy

### Event Store
**Backend**: Firestore (via Akka Persistence Firestore plugin)

**Collections**:
```
/eventJournal/{persistenceId}/events/{sequenceNr}
/snapshots/{persistenceId}/snapshots/{sequenceNr}
```

**Persistence IDs**:
```scala
- producer-{producerId}
- compliance-{producerId}
- document-manager-{producerId}
- framework-{frameworkId}
- subscription-{subscriptionId}
```

### Snapshot Strategy
- ProducerActor: Every 100 events
- ComplianceActor: Every 50 events
- FrameworkActor: Every 20 events (less frequent updates)

### Recovery
- On actor restart, replay events from last snapshot
- Fallback to full replay if snapshot corrupted

---

## Akka Cluster Configuration

### Cluster Roles
```
- api: HTTP server nodes
- worker: Background job nodes
- singleton: Nodes eligible for cluster singletons
```

### Cluster Singletons
- InspectionSyncCoordinator
- DocumentExpiryChecker
- ReminderScheduler
- AnalyticsAggregator

### Sharding Strategy
**ProducerActor sharding**:
```scala
val extractEntityId: ShardRegion.ExtractEntityId = {
  case cmd: ProducerCommand => (cmd.producerId.value, cmd)
}

val extractShardId: ShardRegion.ExtractShardId = {
  case cmd: ProducerCommand =>
    (cmd.producerId.value.hashCode % 100).toString
}
```

**Number of shards**: 100 (adjustable based on scale)

---

## Error Handling & Resilience

### Supervision Strategy

**ProducerSupervisor**:
```scala
override val supervisorStrategy: SupervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.minute) {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case _: Exception => Restart
  }
```

**InspectionSyncCoordinator**:
```scala
override val supervisorStrategy: SupervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 5.minutes) {
    case _: ApiException => Restart
    case _: Exception => Escalate
  }
```

### Circuit Breakers

**DineSafe API**:
```scala
val dinesafeCircuitBreaker = CircuitBreaker(
  scheduler = system.scheduler,
  maxFailures = 5,
  callTimeout = 30.seconds,
  resetTimeout = 1.minute
)
```

**Firebase API**:
```scala
val firebaseCircuitBreaker = CircuitBreaker(
  maxFailures = 10,
  callTimeout = 10.seconds,
  resetTimeout = 30.seconds
)
```

### Retry Logic
```scala
// Exponential backoff for external API calls
def withRetry[T](
  operation: => Future[T],
  maxRetries: Int = 3,
  initialDelay: FiniteDuration = 1.second
): Future[T] = {
  Retry.retryWithBackoff(
    operation,
    minBackoff = initialDelay,
    maxBackoff = 10.seconds,
    randomFactor = 0.2,
    maxRetries = maxRetries
  )
}
```

---

## Data Flow Examples

### Create Producer & Assign Compliance
```
1. POST /api/v1/producers
2. → ProducerSupervisor creates ProducerActor
3. → ProducerActor persists ProducerCreated event
4. → ProducerActor spawns ComplianceActor child
5. → ComplianceActor queries ComplianceFrameworkActor for applicable frameworks
6. → ComplianceActor assigns frameworks based on businessType + activities
7. → ComplianceActor creates ComplianceRecord for each requirement
8. → ComplianceActor persists FrameworkAssigned events
9. ← Response: Producer created with compliance initialized
```

### Upload Compliance Document
```
1. POST /api/v1/producers/{id}/compliance/documents/upload
2. → ProducerActor forwards to DocumentManagerActor
3. → DocumentManagerActor checks storage quota
4. → DocumentManagerActor generates pre-signed Firebase Storage URL
5. ← Response: { uploadUrl, documentId }
6. Frontend uploads file to Firebase Storage directly
7. POST /api/v1/producers/{id}/compliance/documents/{docId}/confirm
8. → DocumentManagerActor persists DocumentUploadConfirmed
9. → DocumentManagerActor notifies ComplianceActor
10. → ComplianceActor attaches document to ComplianceRecord
11. → ComplianceActor recalculates overall status
12. → If status changed, persists OverallStatusCalculated event
13. → ProducerActor receives status update, persists ComplianceStatusChanged
```

### Import Inspection (Scheduled Job)
```
1. Daily at 2 AM: InspectionSyncCoordinator wakes up
2. → Spawns InspectionSyncActor for "DineSafe Toronto"
3. → InspectionSyncActor calls DineSafe CKAN API (paginated)
4. → For each inspection, spawns InspectionImportActor
5. → InspectionImportActor attempts auto-match:
   a. Query Firestore for producers in same postal code
   b. Fuzzy match on business name
   c. If match confidence > 85%, link to ProducerId
6. → If matched, send ImportInspection to ComplianceActor
7. → ComplianceActor checks infraction severity
8. → If CRITICAL infraction, updates affected ComplianceRecords to NON_COMPLIANT
9. → Persists InspectionImported event
10. → InspectionImportActor writes InspectionRecord to Firestore
11. → InspectionImportActor stops
12. → InspectionSyncActor aggregates results, reports to coordinator
13. → Coordinator logs summary to admin dashboard
```

---

## Things We Might Have Missed

### 1. **User Session Management**
Do we need a UserActor to track active sessions, recent activity, preferences?

**Proposal**:
```scala
// UserActor (per user, not per organization)
case class UserState(
  userId: UserId,
  email: String,
  roles: Map[OrganizationId, OrganizationRole],
  preferences: UserPreferences,
  lastLogin: Instant
)
```

**Decision needed**: Keycloak handles sessions, but do we need app-level user state?

---

### 2. **Multi-Tenancy Isolation**
How do we ensure strict data isolation between producers/buyers?

**Current approach**:
- ActorRef per producer (good isolation)
- Firestore security rules enforce read/write permissions
- API layer validates organizationId from JWT

**Concern**: Shared Akka actors (like InspectionSyncActor) access multi-tenant data. Need to ensure no leakage.

**Mitigation**:
- All queries include organizationId filter
- Audit logs for all data access
- Unit tests for permission enforcement

---

### 3. **Rate Limiting**
Do we need rate limiting per organization or per user?

**Proposal**: Add RateLimiterActor
```scala
case class RateLimiterState(
  limits: Map[OrganizationId, RateLimit],
  usage: Map[OrganizationId, UsageWindow]
)

case class RateLimit(
  requestsPerMinute: Int,
  requestsPerHour: Int,
  requestsPerDay: Int
)
```

**Integration**: HTTP middleware checks rate limit before routing to actors

**Limits by subscription tier**:
- Free: 100/min, 1000/hour
- Basic: 500/min, 5000/hour
- Professional: 2000/min, 20000/hour
- Enterprise: Unlimited

---

### 4. **Audit Logging**
Should all compliance changes be auditable?

**Proposal**: AuditLogActor (cluster singleton)
```scala
case class AuditEntry(
  timestamp: Instant,
  actorType: String,
  entityId: String,
  action: String,
  userId: UserId,
  changes: Map[String, (Any, Any)], // field -> (old, new)
  metadata: Map[String, String]
)
```

**Write to**:
- Firestore collection `/auditLogs/{timestamp}`
- BigQuery for analytics (streamed)

**Retention**: 7 years (regulatory requirement)

---

### 5. **Caching Strategy**
Should we cache frequently accessed data?

**Candidates**:
- ComplianceFramework (rarely changes, frequently read)
- DocumentTemplates (static data)
- Region data (static)

**Proposal**:
- In-memory cache with TTL (Caffeine)
- Distributed cache (Redis) for multi-node clusters
- Cache invalidation on admin updates

---

### 6. **Search Indexing**
How do buyers search for producers?

**Current**: Query Firestore directly

**Concern**: Complex filters (distance, categories, compliance status) are slow on Firestore

**Proposal**: SearchIndexActor
- Maintain Algolia index (or Elasticsearch)
- Update index when producer profile changes
- Support fuzzy search, geo-radius, faceted filters

**Alternative**: Use Firestore Composite Indexes (simpler but less flexible)

---

### 7. **Idempotency Keys**
How do we handle duplicate API requests?

**Proposal**: Add IdempotencyActor
```scala
case class IdempotencyState(
  requestIds: Map[IdempotencyKey, (Response, Instant)]
)
```

**Flow**:
1. Client sends `Idempotency-Key: uuid` header
2. API checks IdempotencyActor if key exists
3. If exists, return cached response
4. If not, process request and cache response
5. Expire keys after 24 hours

---

### 8. **Feature Flags**
How do we enable/disable features dynamically?

**Proposal**: FeatureFlagActor
```scala
case class FeatureFlagState(
  flags: Map[FeatureFlag, FeatureFlagConfig]
)

case class FeatureFlagConfig(
  enabled: Boolean,
  enabledForOrgs: Set[OrganizationId],
  enabledForTiers: Set[SubscriptionTier],
  rolloutPercentage: Int // 0-100
)
```

**Use cases**:
- Gradual rollout of new features
- A/B testing
- Emergency kill switch

---

### 9. **Webhook Support** (Future)
Should we allow external systems to subscribe to events?

**Proposal**: WebhookManagerActor
```scala
case class WebhookSubscription(
  organizationId: OrganizationId,
  url: String,
  events: Set[EventType], // COMPLIANCE_STATUS_CHANGED, INSPECTION_IMPORTED, etc.
  secret: String, // for HMAC signature
  active: Boolean
)
```

**Flow**:
- ProducerActor publishes events to event stream
- WebhookManagerActor subscribes to stream
- Filters events by subscription
- POSTs to webhook URL with signature
- Retries on failure (exponential backoff)

---

### 10. **Data Export**
Should producers be able to export their compliance data?

**Proposal**: DataExportActor
```scala
case class ExportRequest(
  organizationId: OrganizationId,
  format: ExportFormat, // CSV, JSON, PDF
  entities: Set[EntityType], // COMPLIANCE_RECORDS, DOCUMENTS, INSPECTIONS
  dateRange: Option[(LocalDate, LocalDate)]
)
```

**Flow**:
1. Producer requests export
2. DataExportActor gathers data from Firestore
3. Formats as CSV/JSON/PDF
4. Uploads to Firebase Storage
5. Sends download link via email
6. Link expires after 7 days

**Use case**: Regulatory audits, switching platforms, backup

---

### 11. **Internationalization (i18n)**
Do we need to support French (bilingual Canada)?

**Current**: English only

**Future**:
- Store user language preference
- Translate UI strings (frontend)
- Translate compliance requirement descriptions (backend)
- Email notifications in user's language

**Impact**: Medium complexity, defer to v2

---

### 12. **Metrics & Monitoring**
How do we monitor actor health and performance?

**Proposal**: Akka Management + Prometheus
- Expose `/metrics` endpoint
- Track actor mailbox sizes
- Track message processing times
- Track event sourcing recovery times
- Alert on high error rates

**Dashboard**: Grafana with Akka metrics

---

### 13. **Graceful Shutdown**
How do we handle deployments without losing data?

**Proposal**: CoordinatedShutdown hooks
```scala
CoordinatedShutdown(system).addTask(
  PhaseServiceUnbind, "unbind-http"
) { () =>
  // Stop accepting new HTTP requests
  httpBinding.unbind()
}

CoordinatedShutdown(system).addTask(
  PhaseServiceRequestsDone, "drain-actors"
) { () =>
  // Wait for actors to finish processing
  shardRegion.gracefulShutdown(30.seconds)
}
```

---

### 14. **Development vs. Production Config**
How do we handle environment-specific config?

**Proposal**: Multi-environment config
```
application.conf           # Default
application-dev.conf       # Development overrides
application-staging.conf   # Staging overrides
application-prod.conf      # Production overrides
```

**Environment variables**:
- `FIREBASE_PROJECT_ID`
- `DINESAFE_API_KEY` (if required)
- `SENDGRID_API_KEY`
- `DATABASE_URL`

---

### 15. **Testing Strategy**
How do we test Akka actors?

**Unit Tests**: Akka TestKit
```scala
class ComplianceActorSpec extends ScalaTestWithActorTestKit {
  "ComplianceActor" should {
    "update record status" in {
      val actor = spawn(ComplianceActor(producerId))
      val probe = testKit.createTestProbe[ComplianceResponse]()

      actor ! UpdateRecordStatus(recordId, Completed, userId)
      probe.expectMessage(StatusUpdated(recordId, Completed))
    }
  }
}
```

**Integration Tests**: Test with real Firestore (emulator)

**Load Tests**: Gatling for HTTP endpoints

---

## Open Architecture Questions

1. **Event Versioning**: How do we handle schema changes in persisted events?
   - Use Akka Serialization with schema evolution
   - Version events (v1, v2)
   - Upcasting old events on replay

2. **Cross-Actor Transactions**: What if updating ComplianceRecord + ProducerStatus fails halfway?
   - Use Saga pattern
   - Compensating actions
   - Eventual consistency is acceptable for most cases

3. **Actor Passivation**: When should actors stop to free memory?
   - ProducerActor: 30 minutes of inactivity
   - ComplianceActor: Same as parent
   - Scheduled actors: Never passivate (singleton)

4. **Cluster Scaling**: How many nodes do we need?
   - Start with 2 nodes (HA)
   - Scale to 5-10 nodes at 1000 producers
   - Auto-scaling based on CPU/memory

5. **Backup & Disaster Recovery**: How do we backup Firestore?
   - Daily exports to Cloud Storage
   - Point-in-time recovery (Firestore built-in)
   - Test restore procedure monthly

---

## Summary: Recommended Actor Set

**Core Actors** (must implement):
1. ProducerActor ✅
2. ComplianceActor ✅
3. DocumentManagerActor ✅
4. ComplianceFrameworkActor ✅
5. InspectionSyncCoordinator ✅
6. InspectionSyncActor ✅
7. InspectionImportActor ✅
8. DocumentExpiryChecker ✅
9. ReminderScheduler ✅
10. NotificationActor ✅
11. SubscriptionActor ✅

**Optional Actors** (consider for v2):
12. AnalyticsAggregator (defer if no analytics in MVP)
13. SearchIndexActor (use Firestore queries initially)
14. RateLimiterActor (add if abuse occurs)
15. AuditLogActor (compliance requirement, add soon)
16. IdempotencyActor (nice-to-have)
17. FeatureFlagActor (useful for gradual rollouts)
18. WebhookManagerActor (v2 feature)
19. DataExportActor (regulatory requirement, add soon)

**Not Needed Yet**:
20. BuyerActor (minimal logic in Step 1)
21. UserActor (Keycloak handles this)

---

Does this architecture make sense? Any actors or concerns I'm missing?
