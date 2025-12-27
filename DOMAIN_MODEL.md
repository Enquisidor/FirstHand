# FirstHand Domain Model

Compliance-focused domain model aligned with Ontario food safety regulations.

**FL** = For Later (waste management features deferred to Step 2)

---

## Data Sources

### DineSafe API (Toronto Open Data)
**Endpoint**: `ckan0.cf.opendata.inter.prod-toronto.ca/dataset/dinesafe`

**Fields**:
- `establishment_id`, `establishment_name`, `establishment_type`
- `establishment_address`, `establishment_status`
- `inspection_date`, `inspection_id`
- `infraction_details`, `severity`, `action`
- `amount_fined`, `court_outcome`

### SFCR (Safe Food for Canadians Regulations)
**Authority**: CFIA

**Structure**:
1. Licensing - Required for import/export and interprovincial trade
2. Preventive Control Plan (PCP) - Sections 50-85
3. Traceability - One step back, one step forward

**Documents**:
- Preventive Control Plan (PCP)
- Maintenance & Operations Checklist (Sections 50-85)
- Traceability records (2 year retention)

### Ontario Premises Registry (PPR)
Assigns Premises Identification Number (PID) to agri-food businesses.

---

## Entities

### User (Keycloak)
```typescript
interface User {
  id: string;
  email: string;
  roles: Role[];
  organizationMemberships: OrganizationMembership[];
}

interface OrganizationMembership {
  organizationId: string;
  role: OrganizationRole;
  joinedAt: timestamp;
}
```

### Producer
```typescript
interface Producer {
  id: string;
  businessName: string;
  legalName: string;
  businessType: BusinessType;
  address: Address;
  region: RegionId;
  municipality: string;
  postalCode: string;
  coordinates: GeoPoint;
  ontarioPremisesId?: string;
  sfcLicenseNumber?: string;
  municipalLicenseNumber?: string;
  contactInfo: ContactInfo;
  complianceStatus: ComplianceStatus;
  subscriptionId: string;
  visibility: VisibilityLevel;
  allowedBuyerIds?: string[];
}

enum BusinessType {
  FARM, COOP, PROCESSOR, INDEPENDENT_RETAILER, DISTRIBUTOR
}

enum ComplianceStatus {
  NOT_STARTED, IN_PROGRESS, COMPLIANT, EXPIRED, NON_COMPLIANT
}

enum VisibilityLevel {
  PUBLIC, RESTRICTED, PRIVATE
}
```

### Buyer
```typescript
interface Buyer {
  id: string;
  businessName: string;
  legalName: string;
  buyerType: BuyerType;
  address: Address;
  region: RegionId;
  contactInfo: ContactInfo;
  subscriptionId: string;
}

enum BuyerType {
  RESTAURANT, FOOD_BANK, RETAILER, PROCESSOR, INSTITUTION, CATERING
}
```

---

## Compliance

### ComplianceFramework
```typescript
interface ComplianceFramework {
  id: string;
  name: string;
  authority: RegulatoryAuthority;
  jurisdiction: Jurisdiction;
  applicableBusinessTypes: BusinessType[];
  applicableActivities: FoodActivity[];
  categories: ComplianceCategory[];
  effectiveDate: date;
  version: string;
}

enum RegulatoryAuthority {
  CFIA, ONTARIO_MECP, ONTARIO_OMAFRA, LOCAL_HEALTH_UNIT, MUNICIPAL
}

enum FoodActivity {
  PROCESSING, RETAIL, WHOLESALE, IMPORT, EXPORT, INTERPROVINCIAL_TRADE
}
```

### ComplianceCategory
```typescript
interface ComplianceCategory {
  id: string;
  frameworkId: string;
  name: string;
  description: string;
  sfcrSection?: string;
  requirements: ComplianceRequirement[];
  priority: RequirementPriority;
  displayOrder: number;
}

enum RequirementPriority {
  CRITICAL, HIGH, MEDIUM, LOW
}
```

### ComplianceRequirement
```typescript
interface ComplianceRequirement {
  id: string;
  categoryId: string;
  title: string;
  description: string;
  regulatoryReference: string;
  requiresDocumentation: boolean;
  documentTypes: DocumentType[];
  requiresInspection: boolean;
  requiresRenewal: boolean;
  renewalPeriod?: Duration;
  guidanceText?: string;
  templateId?: string;
  externalResourceUrl?: string;
  priority: RequirementPriority;
}

enum DocumentType {
  PREVENTIVE_CONTROL_PLAN,
  MAINTENANCE_OPERATIONS_CHECKLIST,
  TRACEABILITY_RECORDS,
  LICENSE_APPLICATION,
  FOOD_SAFETY_PLAN,
  HACCP_PLAN,
  RECALL_PLAN,
  SANITATION_PLAN,
  PEST_CONTROL_PLAN,
  BUSINESS_LICENSE,
  HEALTH_PERMIT,
  PREMISES_ID_CERTIFICATE,
  FOOD_HANDLER_CERTIFICATE,
  MANAGER_CERTIFICATE
}
```

### ComplianceRecord
```typescript
interface ComplianceRecord {
  id: string;
  producerId: string;
  requirementId: string;
  status: ComplianceItemStatus;
  startedAt?: timestamp;
  completedAt?: timestamp;
  expiresAt?: timestamp;
  documents: ComplianceDocument[];
  verifiedBy?: VerificationSource;
  verifiedAt?: timestamp;
  inspectionId?: string;
  notes?: string;
  history: ComplianceStatusChange[];
  lastUpdated: timestamp;
  updatedBy: string;
}

enum ComplianceItemStatus {
  NOT_STARTED, IN_PROGRESS, PENDING_VERIFICATION, COMPLETED, EXPIRED, NON_COMPLIANT
}

enum VerificationSource {
  SELF_REPORTED, DOCUMENT_UPLOAD, INSPECTION, THIRD_PARTY_AUDIT, AUTOMATED_CHECK
}
```

### ComplianceDocument
```typescript
interface ComplianceDocument {
  id: string;
  complianceRecordId: string;
  documentType: DocumentType;
  fileName: string;
  fileSize: number;
  mimeType: string;
  storageUrl: string;
  uploadedAt: timestamp;
  uploadedBy: string;
  expiresAt?: timestamp;
  verified: boolean;
  verifiedBy?: string;
  verifiedAt?: timestamp;
  issueDate?: date;
  expiryDate?: date;
  issuingAuthority?: string;
  certificateNumber?: string;
}
```

---

## Inspection Integration

### InspectionRecord
```typescript
interface InspectionRecord {
  id: string;
  externalInspectionId: string;
  producerId?: string;
  establishmentId: string;
  establishmentName: string;
  establishmentAddress: string;
  establishmentType: string;
  inspectionDate: date;
  inspectionStatus: InspectionStatus;
  minimumInspectionsPerYear: number;
  infractions: Infraction[];
  dataSource: string;
  importedAt: timestamp;
  lastSyncedAt: timestamp;
}

enum InspectionStatus {
  PASS, CONDITIONAL_PASS, CLOSED
}

interface Infraction {
  infractionDetails: string;
  severity: InfractionSeverity;
  action: string;
  courtOutcome?: string;
  amountFined?: number;
}

enum InfractionSeverity {
  MINOR, SIGNIFICANT, CRITICAL
}
```

---

## Document Templates

### DocumentTemplate
```typescript
interface DocumentTemplate {
  id: string;
  name: string;
  description: string;
  documentType: DocumentType;
  applicableFrameworks: string[];
  applicableRequirements: string[];
  templateFileUrl: string;
  fileFormat: FileFormat;
  instructions: string;
  regulatoryReferences: string[];
  externalResourceUrls: string[];
  version: string;
  lastUpdated: timestamp;
}

enum FileFormat {
  PDF, DOCX, XLSX, GOOGLE_DOC, FILLABLE_PDF
}
```

---

## Geography

### Region
```typescript
interface Region {
  id: string;
  name: string;
  type: RegionType;
  postalCodePrefixes: string[];
  municipalities: string[];
  boundingBox?: BoundingBox;
  populationDensity: DensityCategory;
}

enum RegionType {
  PROVINCIAL, ECONOMIC, HEALTH_UNIT, MUNICIPAL
}

enum DensityCategory {
  URBAN, SUBURBAN, RURAL
}
```

---

## Subscription

### Subscription
```typescript
interface Subscription {
  id: string;
  organizationId: string;
  organizationType: "producer" | "buyer";
  tier: SubscriptionTier;
  status: SubscriptionStatus;
  billingCycle: BillingCycle;
  currentPeriodStart: timestamp;
  currentPeriodEnd: timestamp;
  autoRenew: boolean;
  features: SubscriptionFeature[];
  maxComplianceChecklists: number;
  maxDocumentStorage: number;
  maxUsers: number;
}

enum SubscriptionTier {
  FREE, BASIC, PROFESSIONAL, ENTERPRISE
}

enum SubscriptionFeature {
  COMPLIANCE_TRACKING,
  DOCUMENT_TEMPLATES,
  INSPECTION_INTEGRATION,
  ANALYTICS_DASHBOARD,
  PRIORITY_SUPPORT,
  API_ACCESS,
  CUSTOM_BRANDING
}
```

---

## APIs

### Producer APIs

#### Create Producer
```
POST /api/v1/producers
Body: { profile: ProducerProfile }
Response: { id, profile, complianceStatus }
```

#### Get Producer
```
GET /api/v1/producers/:id
Response: Producer
```

#### Update Producer
```
PUT /api/v1/producers/:id
Body: { updates: Partial<Producer> }
Response: Producer
```

#### Get Compliance Status
```
GET /api/v1/producers/:id/compliance
Response: {
  overallStatus: ComplianceStatus,
  frameworks: FrameworkAssignment[],
  records: ComplianceRecord[],
  expiringDocuments: ComplianceDocument[]
}
```

#### Assign Framework
```
POST /api/v1/producers/:id/compliance/assign-framework
Body: { frameworkId: string }
Response: { requirements: ComplianceRequirement[] }
```

#### Update Compliance Record
```
PUT /api/v1/producers/:id/compliance/records/:recordId
Body: {
  status: ComplianceItemStatus,
  notes?: string
}
Response: ComplianceRecord
```

#### Upload Document
```
POST /api/v1/producers/:id/compliance/documents/upload
Body: {
  complianceRecordId: string,
  fileName: string,
  fileSize: number,
  mimeType: string
}
Response: {
  documentId: string,
  uploadUrl: string (pre-signed Firebase Storage URL)
}
```

#### Confirm Document Upload
```
POST /api/v1/producers/:id/compliance/documents/:docId/confirm
Body: { storageUrl: string }
Response: ComplianceDocument
```

#### Get Inspections
```
GET /api/v1/producers/:id/inspections
Query: { since?: date, limit?: number }
Response: InspectionRecord[]
```

---

### Buyer APIs

#### Create Buyer
```
POST /api/v1/buyers
Body: { profile: BuyerProfile }
Response: Buyer
```

#### Get Buyer
```
GET /api/v1/buyers/:id
Response: Buyer
```

#### Update Buyer
```
PUT /api/v1/buyers/:id
Body: { updates: Partial<Buyer> }
Response: Buyer
```

#### Search Producers
```
GET /api/v1/search/producers
Query: {
  region?: string,
  municipality?: string,
  postalCode?: string,
  businessType?: BusinessType,
  complianceStatus?: ComplianceStatus,
  lat?: number,
  lon?: number,
  radius?: number (km)
}
Response: {
  producers: Producer[],
  total: number
}
```

**Visibility Rules**:
- Only PUBLIC and approved RESTRICTED producers returned
- PRIVATE producers excluded
- Requires buyer authentication

---

### Compliance APIs

#### List Frameworks
```
GET /api/v1/compliance/frameworks
Response: ComplianceFramework[]
```

#### Get Framework Details
```
GET /api/v1/compliance/frameworks/:id
Response: {
  framework: ComplianceFramework,
  categories: ComplianceCategory[],
  requirements: ComplianceRequirement[]
}
```

#### List Document Templates
```
GET /api/v1/compliance/templates
Query: { documentType?: DocumentType }
Response: DocumentTemplate[]
```

#### Download Template
```
GET /api/v1/compliance/templates/:id/download
Response: File download (PDF, DOCX, etc.)
```

---

### Admin APIs

#### Create Framework
```
POST /api/v1/admin/frameworks
Body: ComplianceFramework
Response: ComplianceFramework
```

#### Add Category
```
POST /api/v1/admin/frameworks/:id/categories
Body: ComplianceCategory
Response: ComplianceCategory
```

#### Add Requirement
```
POST /api/v1/admin/frameworks/:id/requirements
Body: ComplianceRequirement
Response: ComplianceRequirement
```

#### List Unmatched Inspections
```
GET /api/v1/admin/inspections/unmatched
Response: InspectionRecord[]
```

#### Manual Match Inspection
```
POST /api/v1/admin/inspections/:id/match
Body: { producerId: string }
Response: InspectionRecord
```

#### Analytics
```
GET /api/v1/admin/analytics
Response: {
  complianceRateByRegion: Map<RegionId, number>,
  averageTimeToCompliance: number,
  commonComplianceGaps: RequirementId[],
  documentUploadTrends: TimeSeries,
  infractionPatterns: Map<InfractionSeverity, number>
}
```

---

## Firestore Collections

```
/users/{userId}
/producers/{producerId}
  /complianceRecords/{recordId}
  /documents/{documentId}
  /inspections/{inspectionId}
/buyers/{buyerId}
/complianceFrameworks/{frameworkId}
  /categories/{categoryId}
  /requirements/{requirementId}
/documentTemplates/{templateId}
/inspectionRecords/{inspectionId}
/regions/{regionId}
/subscriptions/{subscriptionId}
```

---

## Backend (Akka)

### Core Actors
- **ProducerActor**: Aggregate root, event sourced
- **ComplianceActor**: Manages compliance state per producer
- **DocumentManagerActor**: Handles uploads, quota, expiry
- **ComplianceFrameworkActor**: Master framework data
- **InspectionSyncCoordinator**: Daily sync from DineSafe
- **InspectionImportActor**: Match inspections to producers
- **SubscriptionActor**: Subscription lifecycle

### Event Sourcing
- Backend: Firestore via Akka Persistence plugin
- Snapshots: Every 50-100 events
- Collections: `/eventJournal/{persistenceId}/events/{sequenceNr}`

### Cluster
- Sharding: ProducerActors (100 shards)
- Singletons: InspectionSyncCoordinator, ReminderScheduler

---

## Business Rules

### Compliance Gating
- CRITICAL requirements must be 100% complete for PUBLIC visibility
- NON_COMPLIANT status automatically sets visibility to PRIVATE
- CRITICAL infractions update ComplianceStatus to NON_COMPLIANT

### Document Expiry
- Reminder: 30 days before expiry
- Auto-update status to EXPIRED when expiryDate passes

### Inspection Matching
- Auto-match by: exact address, fuzzy name (85% threshold), premises ID
- CRITICAL infractions → affected ComplianceRecords set to NON_COMPLIANT

### Subscription Limits
- Free: 5 compliance checklists/month, 100 MB storage
- Basic: 20 checklists, 1 GB storage
- Professional: Unlimited checklists, 10 GB storage
- Enterprise: Unlimited + API access
