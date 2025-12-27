# FirstHand Compliance Domain Model (Step 1 Focus)

This document defines the compliance-focused domain model aligned with Ontario food safety regulations and available API data sources.

**FL** = For Later (waste management features deferred to Step 2)

## Research Summary: Available Regulatory Data Sources

### 1. **DineSafe API** (Toronto Open Data)
**Source**: [Toronto Open Data CKAN Portal](https://open.toronto.ca/dataset/dinesafe/)

**API Access**: CKAN API at `ckan0.cf.opendata.inter.prod-toronto.ca/dataset/dinesafe`

**Data Model Fields**:
- `establishment_id` - Unique identifier
- `establishment_name` - Business name
- `establishment_type` - Type of food premise
- `establishment_address` - Physical location
- `establishment_status` - Current status (Pass, Conditional Pass, Closed)
- `inspection_date` - Date of inspection
- `inspection_id` - Unique inspection identifier
- `minimum_inspections_peryear` - Required inspection frequency
- `infraction_details` - Description of violations
- `severity` - Severity level of infraction
- `action` - Enforcement action taken
- `court_outcome` - Legal outcome if applicable
- `amount_fined` - Fine amount

**Coverage**: Toronto only, but model is applicable to all Ontario health units

### 2. **CFIA Open Data Portal**
**Source**: [Canada Open Government Portal - CFIA](https://open.canada.ca/data/organization/cfia-acia)

**Format**: Downloadable datasets (CSV, JSON metadata), not real-time API

**Data Types**:
- Food safety testing reports
- Chemical residue monitoring
- Targeted survey results

**Note**: No public API for live compliance queries, but datasets provide reference data

### 3. **SFCR (Safe Food for Canadians Regulations)**
**Authority**: Canadian Food Inspection Agency (CFIA)

**Source**: [SFCR Toolkit for Food Businesses](https://inspection.canada.ca/en/food-safety-industry/toolkit-food-businesses)

**Three Compliance Pillars**:
1. **Licensing** - Required for import/export and interprovincial trade
2. **Preventive Control Plan (PCP)** - Sections 50-85 requirements
3. **Traceability** - One step back, one step forward

**Documentation Requirements**:
- Preventive Control Plan (PCP)
- Maintenance and Operations Checklist (Sections 50-85)
- Traceability records (2 year retention)
- Monitoring, verification, and corrective action records

### 4. **Provincial Premises Registry (PPR)**
**Source**: [Ontario Premises ID](https://www.ontariopid.com/)

**Purpose**: Assigns Premises Identification Number (PID) to agri-food businesses

**Coverage**: Ontario-wide for farms and food processors

## Compliance-Focused Domain Model

### Core Principles
1. **Align with SFCR Structure**: License, PCP, Traceability
2. **Map to DineSafe Schema**: Enable future data integration
3. **Ontario-Specific**: Provincial and municipal regulations
4. **Granular but UX-Friendly**: Track details without overwhelming users
5. **Data Transparency**: Show completeness and data sources

---

## 1. Organization & Identity

### User (Keycloak-Managed)
```typescript
interface User {
  id: string;                    // Keycloak user ID
  email: string;
  roles: Role[];                 // Producer, Buyer, Admin
  organizationMemberships: OrganizationMembership[];
  profile: UserProfile;
}

interface OrganizationMembership {
  organizationId: string;
  role: OrganizationRole;        // Owner, Manager, Staff
  joinedAt: timestamp;
}
```

### Producer
```typescript
interface Producer {
  id: string;

  // Basic Information
  businessName: string;
  legalName: string;
  businessType: BusinessType;    // Farm, Co-op, Processor, Retailer

  // Location (Ontario-specific)
  address: Address;
  region: RegionId;               // Golden Horseshoe, Southwestern ON, etc.
  municipality: string;
  postalCode: string;
  coordinates: GeoPoint;

  // Regulatory Identifiers
  ontarioPremisesId?: string;     // From PPR system
  sfcLicenseNumber?: string;      // If federally licensed
  municipalLicenseNumber?: string;

  // Contact
  contactInfo: ContactInfo;

  // Compliance Status (calculated from ComplianceRecord)
  complianceStatus: ComplianceStatus;
  complianceLastUpdated: timestamp;

  // Subscription
  subscriptionId: string;
  subscriptionTier: SubscriptionTier;

  // Visibility Settings (for FL: buyer matching)
  visibility: VisibilityLevel;    // Public, Restricted, Private
  allowedBuyerIds?: string[];     // If Restricted

  // FL: Waste management fields
  // pickupWindows: PickupWindow[];
  // operatingHours: OperatingHours;
}

enum BusinessType {
  FARM = "farm",
  COOP = "coop",
  PROCESSOR = "processor",
  INDEPENDENT_RETAILER = "independent_retailer",
  DISTRIBUTOR = "distributor"
}

enum ComplianceStatus {
  NOT_STARTED = "not_started",
  IN_PROGRESS = "in_progress",
  COMPLIANT = "compliant",
  EXPIRED = "expired",
  NON_COMPLIANT = "non_compliant"
}
```

### Buyer
```typescript
interface Buyer {
  id: string;

  // Basic Information
  businessName: string;
  legalName: string;
  buyerType: BuyerType;

  // Location
  address: Address;
  region: RegionId;
  coordinates: GeoPoint;

  // Contact
  contactInfo: ContactInfo;

  // Subscription
  subscriptionId: string;
  subscriptionTier: SubscriptionTier;

  // FL: Waste management fields
  // preferredCategories: ProductCategory[];
  // capacity: VolumeCapacity;
  // deliveryWindows: DeliveryWindow[];
}

enum BuyerType {
  RESTAURANT = "restaurant",
  FOOD_BANK = "food_bank",
  RETAILER = "retailer",
  PROCESSOR = "processor",
  INSTITUTION = "institution",        // Schools, hospitals
  CATERING = "catering"
}
```

---

## 2. Compliance Management (Core Focus)

### ComplianceFramework
**Master data defining regulatory requirements**

```typescript
interface ComplianceFramework {
  id: string;
  name: string;                   // "SFCR Federal", "Ontario Food Premises", etc.
  authority: RegulatoryAuthority; // CFIA, Ontario MECP, Local Health Unit
  jurisdiction: Jurisdiction;     // Federal, Provincial, Municipal

  // Applicability Rules
  applicableBusinessTypes: BusinessType[];
  applicableActivities: FoodActivity[];  // Processing, Retail, Import, Export

  // Requirements Structure
  categories: ComplianceCategory[];

  // Metadata
  effectiveDate: date;
  version: string;
  sourceUrl: string;
  lastUpdated: timestamp;
}

enum RegulatoryAuthority {
  CFIA = "cfia",                  // Canadian Food Inspection Agency
  ONTARIO_MECP = "ontario_mecp",  // Ministry of Environment, Conservation & Parks
  ONTARIO_OMAFRA = "ontario_omafra", // Ministry of Agriculture, Food & Rural Affairs
  LOCAL_HEALTH_UNIT = "local_health_unit",
  MUNICIPAL = "municipal"
}

enum FoodActivity {
  PROCESSING = "processing",
  RETAIL = "retail",
  WHOLESALE = "wholesale",
  IMPORT = "import",
  EXPORT = "export",
  INTERPROVINCIAL_TRADE = "interprovincial_trade"
}
```

### ComplianceCategory
**Organized sections of requirements (maps to SFCR Sections 50-85)**

```typescript
interface ComplianceCategory {
  id: string;
  frameworkId: string;
  name: string;                   // "Licensing", "Preventive Controls", "Traceability"
  description: string;

  // SFCR Mapping
  sfcrSection?: string;           // e.g., "50-60" for building requirements

  // Requirements
  requirements: ComplianceRequirement[];

  priority: RequirementPriority;
  displayOrder: number;
}

enum RequirementPriority {
  CRITICAL = "critical",          // Must complete before operations
  HIGH = "high",                  // Required for compliance
  MEDIUM = "medium",              // Recommended
  LOW = "low"                     // Optional/best practice
}
```

### ComplianceRequirement
**Individual requirement item (granular tracking)**

```typescript
interface ComplianceRequirement {
  id: string;
  categoryId: string;

  // Requirement Details
  title: string;
  description: string;
  regulatoryReference: string;    // e.g., "SFCR Section 52(1)(a)"

  // Completion Criteria
  requiresDocumentation: boolean;
  documentTypes: DocumentType[];  // PCP, Checklist, Certificate, etc.
  requiresInspection: boolean;
  requiresRenewal: boolean;
  renewalPeriod?: Duration;       // Annual, Biennial, etc.

  // Guidance
  guidanceText?: string;
  templateId?: string;            // Link to DocumentTemplate
  externalResourceUrl?: string;

  // Metadata
  priority: RequirementPriority;
  displayOrder: number;
}

enum DocumentType {
  // SFCR Documents
  PREVENTIVE_CONTROL_PLAN = "preventive_control_plan",
  MAINTENANCE_OPERATIONS_CHECKLIST = "maintenance_operations_checklist",
  TRACEABILITY_RECORDS = "traceability_records",
  LICENSE_APPLICATION = "license_application",

  // Additional Documents
  FOOD_SAFETY_PLAN = "food_safety_plan",
  HACCP_PLAN = "haccp_plan",
  RECALL_PLAN = "recall_plan",
  SANITATION_PLAN = "sanitation_plan",
  PEST_CONTROL_PLAN = "pest_control_plan",

  // Certificates & Permits
  BUSINESS_LICENSE = "business_license",
  HEALTH_PERMIT = "health_permit",
  PREMISES_ID_CERTIFICATE = "premises_id_certificate",

  // Training & Qualifications
  FOOD_HANDLER_CERTIFICATE = "food_handler_certificate",
  MANAGER_CERTIFICATE = "manager_certificate"
}
```

### ComplianceRecord
**Producer's compliance status per requirement**

```typescript
interface ComplianceRecord {
  id: string;
  producerId: string;
  requirementId: string;

  // Status Tracking
  status: ComplianceItemStatus;
  startedAt?: timestamp;
  completedAt?: timestamp;
  expiresAt?: timestamp;          // If renewable

  // Documentation
  documents: ComplianceDocument[];

  // Verification
  verifiedBy?: VerificationSource;
  verifiedAt?: timestamp;
  inspectionId?: string;          // Link to InspectionRecord if applicable

  // Notes
  notes?: string;
  internalNotes?: string;         // Private to producer

  // Reminders
  nextRenewalDate?: timestamp;
  reminderSent?: boolean;

  // Audit Trail
  history: ComplianceStatusChange[];
  lastUpdated: timestamp;
  updatedBy: string;              // User ID
}

enum ComplianceItemStatus {
  NOT_STARTED = "not_started",
  IN_PROGRESS = "in_progress",
  PENDING_VERIFICATION = "pending_verification",
  COMPLETED = "completed",
  EXPIRED = "expired",
  NON_COMPLIANT = "non_compliant"
}

enum VerificationSource {
  SELF_REPORTED = "self_reported",
  DOCUMENT_UPLOAD = "document_upload",
  INSPECTION = "inspection",        // From InspectionRecord
  THIRD_PARTY_AUDIT = "third_party_audit",
  AUTOMATED_CHECK = "automated_check" // e.g., API validation
}

interface ComplianceStatusChange {
  timestamp: timestamp;
  previousStatus: ComplianceItemStatus;
  newStatus: ComplianceItemStatus;
  changedBy: string;              // User ID
  reason?: string;
}
```

### ComplianceDocument
**Document attached to compliance record**

```typescript
interface ComplianceDocument {
  id: string;
  complianceRecordId: string;

  // Document Info
  documentType: DocumentType;
  fileName: string;
  fileSize: number;
  mimeType: string;
  storageUrl: string;             // Firebase Storage URL

  // Metadata
  uploadedAt: timestamp;
  uploadedBy: string;             // User ID
  expiresAt?: timestamp;

  // Verification
  verified: boolean;
  verifiedBy?: string;
  verifiedAt?: timestamp;

  // Document Details
  issueDate?: date;
  expiryDate?: date;
  issuingAuthority?: string;
  certificateNumber?: string;
}
```

---

## 3. Inspection Integration (DineSafe Model)

### InspectionRecord
**Imported from municipal health units (DineSafe, etc.)**

```typescript
interface InspectionRecord {
  id: string;                     // Our internal ID
  externalInspectionId: string;   // From DineSafe API

  // Establishment Mapping
  producerId?: string;            // Linked to our Producer if matched
  establishmentId: string;        // DineSafe establishment_id
  establishmentName: string;
  establishmentAddress: string;
  establishmentType: string;

  // Inspection Details
  inspectionDate: date;
  inspectionStatus: InspectionStatus;
  minimumInspectionsPerYear: number;

  // Infractions
  infractions: Infraction[];

  // Data Source
  dataSource: string;             // "DineSafe Toronto", "Ottawa Public Health", etc.
  importedAt: timestamp;
  lastSyncedAt: timestamp;
}

enum InspectionStatus {
  PASS = "pass",
  CONDITIONAL_PASS = "conditional_pass",
  CLOSED = "closed"
}

interface Infraction {
  infractionDetails: string;
  severity: InfractionSeverity;
  action: string;                 // Enforcement action taken
  courtOutcome?: string;
  amountFined?: number;
}

enum InfractionSeverity {
  MINOR = "minor",
  SIGNIFICANT = "significant",
  CRITICAL = "critical"
}
```

**Integration Strategy**:
1. **Auto-Match**: Match `establishmentAddress` + `businessName` to Producer records
2. **Manual Link**: Allow producers to claim inspection records
3. **Sync Status**: Update `ComplianceRecord` when inspection shows violations
4. **Transparency**: Show inspection history on producer profiles (if visibility allows)

---

## 4. Document Templates

### DocumentTemplate
**Downloadable compliance templates**

```typescript
interface DocumentTemplate {
  id: string;

  // Template Info
  name: string;
  description: string;
  documentType: DocumentType;

  // Regulatory Context
  applicableFrameworks: string[]; // ComplianceFramework IDs
  applicableRequirements: string[]; // ComplianceRequirement IDs

  // Template File
  templateFileUrl: string;        // Firebase Storage URL
  fileFormat: FileFormat;         // PDF, DOCX, XLSX

  // Guidance
  instructions: string;
  regulatoryReferences: string[];
  externalResourceUrls: string[];

  // Metadata
  version: string;
  lastUpdated: timestamp;
  createdBy: string;              // Admin user
  featured: boolean;              // Show on dashboard
}

enum FileFormat {
  PDF = "pdf",
  DOCX = "docx",
  XLSX = "xlsx",
  GOOGLE_DOC = "google_doc",
  FILLABLE_PDF = "fillable_pdf"
}
```

---

## 5. Ontario Geography

### Region
```typescript
interface Region {
  id: string;
  name: string;                   // "Golden Horseshoe", "Southwestern Ontario"
  type: RegionType;

  // Geographic Data
  postalCodePrefixes: string[];   // FSA codes, e.g., ["M", "L", "K"]
  municipalities: string[];
  boundingBox?: BoundingBox;

  // Metadata
  populationDensity: DensityCategory;
}

enum RegionType {
  PROVINCIAL = "provincial",
  ECONOMIC = "economic",          // Golden Horseshoe, etc.
  HEALTH_UNIT = "health_unit",
  MUNICIPAL = "municipal"
}

enum DensityCategory {
  URBAN = "urban",
  SUBURBAN = "suburban",
  RURAL = "rural"
}
```

---

## 6. Subscription Management

### Subscription
```typescript
interface Subscription {
  id: string;
  organizationId: string;
  organizationType: "producer" | "buyer";

  // Subscription Details
  tier: SubscriptionTier;
  status: SubscriptionStatus;

  // Billing
  billingCycle: BillingCycle;
  currentPeriodStart: timestamp;
  currentPeriodEnd: timestamp;
  autoRenew: boolean;

  // Features Enabled
  features: SubscriptionFeature[];

  // Usage Limits
  maxComplianceChecklists: number;
  maxDocumentStorage: number;     // GB
  maxUsers: number;

  // FL: Waste Reduction Metrics (for rebates)
  // wasteReductionMetrics: WasteMetrics;
  // rebateEligibility: boolean;
}

enum SubscriptionTier {
  FREE = "free",
  BASIC = "basic",
  PROFESSIONAL = "professional",
  ENTERPRISE = "enterprise"
}

enum SubscriptionFeature {
  COMPLIANCE_TRACKING = "compliance_tracking",
  DOCUMENT_TEMPLATES = "document_templates",
  INSPECTION_INTEGRATION = "inspection_integration",
  ANALYTICS_DASHBOARD = "analytics_dashboard",
  PRIORITY_SUPPORT = "priority_support",
  API_ACCESS = "api_access",
  CUSTOM_BRANDING = "custom_branding",

  // FL: Waste management features
  // SURPLUS_LISTINGS = "surplus_listings",
  // BUYER_MATCHING = "buyer_matching",
  // ROUTE_OPTIMIZATION = "route_optimization"
}
```

---

## 7. Support Entities

### Address
```typescript
interface Address {
  streetNumber: string;
  streetName: string;
  unit?: string;
  city: string;
  province: "ON";                 // Ontario only
  postalCode: string;
  country: "CA";                  // Canada only
}
```

### ContactInfo
```typescript
interface ContactInfo {
  primaryPhone: string;
  secondaryPhone?: string;
  email: string;
  website?: string;

  // For external communication (no in-platform messaging)
  preferredContactMethod: ContactMethod;
}

enum ContactMethod {
  PHONE = "phone",
  EMAIL = "email",
  BOTH = "both"
}
```

---

## Data Flows

### 1. Compliance Framework Setup (Admin)
```
Admin → Create ComplianceFramework (SFCR Federal)
     → Add ComplianceCategory (Licensing, PCP, Traceability)
     → Add ComplianceRequirement per category
     → Link DocumentTemplates to requirements
```

### 2. Producer Onboarding
```
Producer → Sign up (Keycloak)
         → Create Producer profile
         → Select business type & activities
         → System assigns applicable ComplianceFrameworks
         → System creates ComplianceRecords for all requirements
         → Producer completes requirements
         → Upload documents
         → Mark items complete
```

### 3. Inspection Integration
```
Background Job → Poll DineSafe API
               → Import InspectionRecords
               → Match to Producers by address/name
               → Update ComplianceRecord status if infractions
               → Notify producer of new inspection data
```

### 4. Compliance Dashboard
```
Producer Dashboard → Show ComplianceStatus summary
                   → List pending requirements by priority
                   → Show expiring documents
                   → Display recent inspections
                   → Link to templates
```

---

## Business Rules

### Compliance-Gated Features
1. **Producer Profile Visibility**:
   - CRITICAL requirements must be 100% complete to set visibility to Public
   - Producers with NON_COMPLIANT status automatically set to Private

2. **Document Expiry**:
   - System sends reminder 30 days before expiry
   - Auto-update status to EXPIRED when expiryDate passes
   - Weekly job checks for expired documents

3. **Inspection Impact**:
   - CRITICAL infractions → Set ComplianceStatus to NON_COMPLIANT
   - SIGNIFICANT infractions → Flag for producer review
   - MINOR infractions → Log but don't change status

### Data Transparency
1. **Source Indicators**:
   - Show verification source on each compliance item
   - Display last inspection date
   - Indicate data freshness (last updated timestamp)

2. **Completeness Tracking**:
   - Calculate % complete per category
   - Show missing documents clearly
   - Indicate optional vs. required items

---

## API Integration Priorities

### Phase 1 (MVP)
1. **DineSafe Toronto**: Import inspection data for Toronto producers
2. **Document Storage**: Firebase Cloud Storage for compliance docs
3. **Manual Entry**: Producers self-report compliance status

### Phase 2
1. **Additional Health Units**: Expand to Ottawa, Peel, York Region
2. **PPR Integration**: Auto-populate Premises ID if available
3. **CFIA Datasets**: Import reference data for federal requirements

### Phase 3 (FL)
1. **Real-time Inspection Alerts**: Webhook notifications from health units
2. **SFCR License Verification**: Validate license numbers against CFIA database
3. **Third-Party Audits**: Integration with food safety audit providers

---

## Firestore Collections

```
/users/{userId}
/producers/{producerId}
  - subcollections: /complianceRecords, /inspections, /documents
/buyers/{buyerId}
/complianceFrameworks/{frameworkId}
  - subcollections: /categories, /requirements
/documentTemplates/{templateId}
/inspectionRecords/{inspectionId}
/regions/{regionId}
/subscriptions/{subscriptionId}
```

---

## Akka Actors

### ComplianceActor
**Manages compliance lifecycle for a producer**

**Responsibilities**:
- Track ComplianceRecord state
- Handle document uploads
- Calculate overall ComplianceStatus
- Send renewal reminders
- Process inspection imports

**State**:
```scala
case class ComplianceState(
  producerId: String,
  records: Map[RequirementId, ComplianceRecord],
  overallStatus: ComplianceStatus,
  expiringDocuments: List[ComplianceDocument]
)
```

**Commands**:
```scala
sealed trait ComplianceCommand
case class UpdateRecordStatus(recordId: String, status: ComplianceItemStatus)
case class UploadDocument(recordId: String, document: ComplianceDocument)
case class ImportInspection(inspection: InspectionRecord)
case class CheckExpiry()
```

### InspectionSyncActor
**Background job to sync inspection data**

**Responsibilities**:
- Poll DineSafe API
- Match inspections to producers
- Update compliance records
- Notify producers of changes

**Schedule**: Daily at 2 AM

---

## Open Questions

1. **Document Verification**: Manual admin review or automated checks?
2. **Multi-Jurisdiction**: Handle producers with multiple locations across health units?
3. **SFCR License API**: Does CFIA provide license validation API?
4. **Renewal Automation**: Auto-renew recurring subscriptions with payment provider?
5. **Inspection Matching Confidence**: Threshold for auto-matching inspections vs. manual review?

---

## Sources

- [DineSafe Toronto Open Data](https://open.toronto.ca/dataset/dinesafe/)
- [CFIA SFCR Handbook](https://inspection.canada.ca/en/food-safety-industry/toolkit-food-businesses/sfcr-handbook-food-businesses)
- [CFIA Open Data Portal](https://open.canada.ca/data/organization/cfia-acia)
- [Ontario Premises Registry](https://www.ontariopid.com/)
- [SFCR Licensing Guide](https://qualitysmartsolutions.com/safe-food-for-canadians-regulations-sfcr-license/)
- [GitHub: DineSafe Toronto Data](https://github.com/notexploiting/dinesafe-toronto)
