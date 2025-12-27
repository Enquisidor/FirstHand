# FirstHand Domain Model

Compliance-focused domain model aligned with Ontario food safety regulations.

**FL** = For Later (waste management features deferred to Step 2)

---

## External Data Sources & APIs

### DineSafe API (Toronto)
**Type**: REST API (CKAN)
**Base URL**: `https://ckan0.cf.opendata.inter.prod-toronto.ca/api/3`
**Dataset**: `dinesafe`

**Endpoints**:
```
GET /action/datastore_search
Query Parameters:
  resource_id: "dinesafe"
  limit: 100
  offset: 0
  filters: { "INSPECTION_DATE": "2024-01-01" }

Response:
{
  "records": [
    {
      "ESTABLISHMENT_ID": "12345",
      "ESTABLISHMENT_NAME": "Example Restaurant",
      "ESTABLISHMENT_TYPE": "Restaurant",
      "ESTABLISHMENT_ADDRESS": "123 Main St",
      "ESTABLISHMENT_STATUS": "Pass",
      "INSPECTION_DATE": "2024-01-15",
      "INFRACTION_DETAILS": null,
      "SEVERITY": null,
      "ACTION": null,
      "AMOUNT_FINED": null
    }
  ],
  "total": 15000
}
```

**Update Frequency**: Bi-weekly
**Coverage**: Toronto only
**Auth**: None required (public API)

**Similar APIs**:
- Ottawa Public Health: `https://open.ottawa.ca` (different schema)
- Peel Region: `https://opendata.peelregion.ca`
- York Region: No public API (manual export)

---

### CFIA Open Data Portal
**Type**: Bulk Downloads (CSV, JSON)
**Base URL**: `https://open.canada.ca/data/en`
**Organization**: `/organization/cfia-acia`

**Available Datasets**:
```
1. Food Recall Notices
   URL: /dataset/cfia-food-recall-notices
   Format: CSV, JSON metadata
   Fields: recall_id, product_name, hazard_classification, recall_date

2. License Holder Directory (if public)
   URL: /dataset/sfcr-license-holders (check availability)
   Format: CSV
   Fields: license_number, business_name, license_type, status

3. Inspection Results (limited public data)
   URL: /dataset/inspection-results
   Format: CSV
   Fields: establishment_name, inspection_date, compliance_status
```

**Update Frequency**: Varies by dataset (monthly to quarterly)
**Auth**: None required
**Note**: No real-time API - batch downloads only

---

### Ontario Premises Registry (PPR)
**Type**: Web Portal (no public API)
**URL**: `https://www.ontariopid.com`

**Data Available**:
- Premises ID assignment
- Business name verification
- Location validation

**Access Method**: Manual lookup or bulk export (by request)
**Integration Strategy**: Screen scraping or partnership for API access

---

### Ontario Business Registry
**Type**: ServiceOntario API (requires license)
**URL**: `https://www.ontario.ca/page/business-name-search`

**Data Available**:
- Business registration status
- Legal business name
- Business address
- Directors/officers (for corporations)

**Access**: Paid API or web scraping (terms of use restrictions)

---

### Canada Business Registries API
**Type**: REST API
**Base URL**: `https://www.ic.gc.ca/app/scr/cc/CorporationsCanada`

**Endpoints**:
```
GET /corporations/{corporationNumber}

Response:
{
  "corporationNumber": "123456-7",
  "corporationName": "Example Food Co.",
  "status": "Active",
  "governingLegislation": "Canada Business Corporations Act",
  "corporationType": "For-Profit Corporation"
}
```

**Auth**: API key required (apply through Innovation, Science and Economic Development Canada)
**Rate Limits**: 1000 requests/day

---

### Municipal Health Unit APIs

#### Ottawa Public Health
**Type**: CKAN API
**Base URL**: `https://opendata.ottawa.ca/api/3`
**Dataset**: `food-premise-inspections`

```
GET /action/datastore_search
resource_id: "food-premise-inspections"
```

#### Peel Region
**Type**: ArcGIS REST API
**Base URL**: `https://opendata.peelregion.ca/api`

```
GET /datasets/food-premises/query
where: 1=1
outFields: *
f: json
```

#### Halton Region
**Type**: Open Data Portal
**URL**: `https://data.halton.ca`
**Format**: CSV download, no API

---

### SFCR Framework Data
**Source**: CFIA Toolkit
**URL**: `https://inspection.canada.ca/en/food-safety-industry/toolkit-food-businesses`

**Available Documents** (manual extraction):
- Preventive Control Plan Template (PDF)
- Maintenance & Operations Checklist (PDF/Excel)
- Traceability requirements (text/HTML)
- SFCR Handbook (HTML sections 50-85)

**Integration Strategy**:
1. Manual download of templates
2. Parse PDF/HTML to extract requirement text
3. Store in ComplianceFramework entities
4. Update quarterly when CFIA publishes changes

---

### Google Maps API (for geocoding)
**Type**: REST API
**Base URL**: `https://maps.googleapis.com/maps/api`

**Endpoints**:
```
GET /geocode/json
address: "123 Main St, Toronto, ON"
key: YOUR_API_KEY

Response:
{
  "results": [{
    "geometry": {
      "location": { "lat": 43.65, "lng": -79.38 }
    },
    "formatted_address": "123 Main St, Toronto, ON M5H 2N2"
  }]
}
```

**Use Case**: Convert producer addresses to coordinates for proximity matching

---

## Integration Priority

**Phase 1 (MVP)**:
1. DineSafe Toronto - Automated daily sync
2. SFCR Framework - Manual extraction, quarterly updates
3. Google Geocoding - Real-time for producer onboarding

**Phase 2**:
1. Ottawa Public Health - Expand inspection imports
2. Peel Region - Add coverage
3. CFIA Recall API - Alert producers of recalls affecting their products

**Phase 3**:
1. Ontario Business Registry - Validate business names
2. Additional health units (Halton, Durham, York)
3. PPR integration (if API becomes available)

---

## API Integration Patterns

### Polling Schedule
```
DineSafe: Daily at 2 AM EST
CFIA Datasets: Weekly on Sunday
Geocoding: Real-time on demand
```

### Error Handling
```
- Retry with exponential backoff (2s, 4s, 8s, 16s)
- Circuit breaker: 5 failures → 5 min cooldown
- Fallback: Store failed records for manual review
- Alert: Email admin if sync fails 3 consecutive times
```

### Rate Limiting
```
DineSafe: No published limit (use 100 req/min to be safe)
Google Geocoding: 50 req/sec (paid tier)
CFIA Open Data: No limit (bulk downloads)
```

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

See [openapi.yaml](openapi.yaml) for full API specification.

**Interactive Documentation**: Import `openapi.yaml` into [Swagger Editor](https://editor.swagger.io/) or [Postman](https://www.postman.com/)

**Key Endpoints**:
- `POST /api/v1/producers` - Create producer
- `GET /api/v1/producers/{id}/compliance` - Get compliance status
- `POST /api/v1/producers/{id}/compliance/documents/upload` - Upload compliance document
- `GET /api/v1/search/producers` - Search producers (buyer)
- `GET /api/v1/compliance/frameworks` - List compliance frameworks
- `GET /api/v1/admin/analytics` - System analytics (admin)

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
