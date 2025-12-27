# FirstHand Domain Model

This document outlines the core domain entities and business rules for the FirstHand platform.

## Design Principles

1. **No Price Setting**: Platform suggests connections but does not set prices or handle payments
2. **No Auto-Assignment**: All matches are suggestions requiring manual action
3. **No In-Platform Communication**: Buyers and sellers communicate externally (phone, email)
4. **Compliance First**: Producers must meet minimum compliance before creating surplus listings
5. **Ontario-Only**: Geographic constraints limited to Ontario regions
6. **Subscription-Gated Features**: Feature access controlled by subscription tier
7. **Producer Privacy**: Producers control which buyers can see their listings

## Authentication & Identity (Keycloak)

### User
- Managed via **Keycloak** for multi-tenancy
- Email, authentication status
- Roles (Producer, Buyer, Admin)
- Can belong to multiple organizations
- Profile data (name, phone, preferences)

**Keycloak Benefits**:
- Multi-tenancy support
- SSO capabilities
- Role-based access control (RBAC)
- Federation with external identity providers
- OAuth2/OIDC standards

## Organizations

### Producer
**Core Fields**:
- Business details (name, legal name, contact info)
- Ontario region/geography (postal code, municipality)
- Business type (farm, co-op, independent retailer, processor)
- Operating hours/pickup windows
- Subscription status and tier
- Compliance status (see Compliance section)
- Visibility settings (public, restricted, private)
- Allowed buyer list (if restricted visibility)

**Visibility Rules**:
- **Public**: All buyers in the region can see listings
- **Restricted**: Only approved buyers can see listings
- **Private**: Not discoverable in search, only direct connections

### Buyer
**Core Fields**:
- Business details (name, legal name, contact info)
- Ontario region/geography
- Buyer type (restaurant, food bank, retailer, processor, institution)
- Capacity/volume needs
- Preferred product categories
- Operating hours/delivery windows
- Subscription status

## Regulatory Compliance (Step 1 Core)

### ComplianceChecklist
**Template/Master Data**:
- Name (e.g., "Ontario Food Safety Requirements")
- Category (food safety, licensing, transportation, labeling)
- Applicable business types
- List of requirement items
- Renewal cycle (annual, biennial, etc.)
- Priority level (required, recommended, optional)

**Item-Level Granularity** (within UX limits):
- Item description
- Regulatory reference (SFCR, HACCP, etc.)
- Required documentation
- Completion criteria

### ComplianceRecord
**Per-Producer Tracking**:
- Producer reference
- Checklist reference
- Per-item status:
  - Not started
  - In progress
  - Completed
  - Expired/needs renewal
- Attached documents (Cloud Storage references)
- Completion timestamps
- Expiry/renewal dates
- Notes/comments

**Data Availability Transparency**:
- Clearly show which compliance data is available
- Indicate missing or incomplete items
- Track data source (producer-entered, verified, auto-imported)

### DocumentTemplate
**Downloadable Templates**:
- Template type (food safety plan, HACCP, traceability log, shipping manifest)
- Province-specific (Ontario regulations)
- Category tags
- File format (PDF, DOCX, XLSX)
- Instructions/guidance
- Regulatory references

## Surplus Management

### SurplusListing
**Core Fields**:
- Producer reference
- Product name, description
- Product category reference
- Quantity, unit of measure
- Available from date/time
- Available until date/time (expiry consideration)
- Pickup window
- Storage requirements (refrigerated, frozen, dry, ambient)
- Food safety notes
- Photos (Cloud Storage references)
- Status (available, reserved, picked up, expired, withdrawn)
- Visibility (inherits from producer settings)

**CRITICAL**: NO price field - pricing is negotiated externally

**Data Transparency**:
- Show when listing was created
- Show when it was last updated
- Indicate if critical data is missing (e.g., no pickup window)

### ProductCategory
**Taxonomy**:
- Name (produce, dairy, baked goods, meat, prepared foods, beverages)
- Ontario food safety category mapping
- Storage requirements
- Typical shelf life guidelines
- Handling precautions
- Icon/image

## Connection & Coordination (Step 1 Focus)

### BuyerMatch
**Suggestion System (NOT auto-assignment)**:
- Surplus listing reference
- Suggested buyer references (multiple)
- Match score/reasoning:
  - Geographic proximity (distance)
  - Category fit
  - Capacity match
  - Historical pickup success
- Status per buyer:
  - Suggested (not yet contacted)
  - Producer contacted buyer externally
  - Declined
  - Accepted/picked up
- Initiated by (producer or buyer)
- Created timestamp

**No Communication**: Contact details shown for external communication

### PickupWindow
**Coordination Data**:
- Organization reference (producer or buyer)
- Days of week (bitmask or list)
- Time ranges (multiple per day)
- Geographic constraints (will travel X km)
- Capacity limits (per day, per week)
- Special instructions
- Active/inactive status

## Geography (Ontario-specific)

### Region
**Ontario Subdivisions**:
- Name (e.g., "Golden Horseshoe", "Southwestern Ontario", "Eastern Ontario")
- Postal code ranges (FSA - Forward Sortation Area)
- Municipalities list
- Coordinates (bounding box or polygon)
- Population density category (urban, suburban, rural)

**Proximity Matching**:
- Calculate distance between producers and buyers
- Suggest matches within configurable radius
- Consider transportation corridors (highways)

## Subscription Management

### Subscription
**Per-Organization**:
- Organization reference (producer or buyer)
- Tier (free, basic, professional, enterprise)
- Features enabled:
  - Max surplus listings
  - Advanced search/filtering
  - Analytics dashboard
  - Priority matching
  - API access
  - Custom branding
- Billing cycle (monthly, annual)
- Waste reduction metrics (for rebate calculation):
  - Total quantity listed
  - Total quantity picked up
  - Waste reduction percentage
- Start date
- End date (null if active)
- Auto-renew flag

**Rebate Calculation**:
- Track quarterly waste reduction
- Apply rebate when threshold met (e.g., 80% pickup rate)
- Reduce next billing cycle amount

## Search & Discovery

### ProducerSearch
**Buyer Capabilities**:
- Search by region, municipality, postal code
- Filter by:
  - Business type
  - Product categories offered
  - Compliance status (verified, in progress)
  - Active surplus listings
  - Pickup window compatibility
- Sort by:
  - Distance
  - Number of active listings
  - Most recent activity

**Visibility Enforcement**:
- Respect producer visibility settings
- Show only "Public" and approved "Restricted" producers
- Exclude "Private" producers from search results

### SurplusSearch
**Buyer Capabilities**:
- Search by product name, category
- Filter by:
  - Available date range
  - Pickup window compatibility
  - Distance/region
  - Storage requirements
  - Quantity range
- Sort by:
  - Distance
  - Available until (expiring soon)
  - Quantity
  - Most recent

## Business Rules

### Compliance-Gated Features
1. Producers cannot create surplus listings until:
   - Basic compliance checklist is 100% complete, OR
   - Minimum required items are complete (configurable)
2. Compliance status shown on producer profiles
3. Expired compliance items trigger warnings

### Visibility & Privacy
1. Producer visibility defaults to "Public"
2. "Restricted" producers maintain buyer allowlist
3. Buyers request access to "Restricted" producers
4. Producers approve/deny access requests
5. "Private" producers are not discoverable

### Matching Algorithm (Suggestions)
1. Calculate geographic distance
2. Check category compatibility
3. Verify pickup window overlap
4. Consider historical success rate
5. Respect visibility restrictions
6. Return ranked list of suggestions

### Data Transparency
1. Show timestamps for all data
2. Indicate data completeness
3. Mark verified vs. self-reported data
4. Show data source (user-entered, imported, verified)

### Subscription Enforcement
1. Free tier: max 5 surplus listings per month
2. Basic tier: max 20 listings, basic search
3. Professional tier: unlimited listings, advanced search, analytics
4. Enterprise tier: all features + API access

## Event Sourcing Candidates

For audit trails and analytics, consider event sourcing for:
- SurplusListing state changes
- ComplianceRecord completions
- BuyerMatch outcomes (success/failure)
- Access requests and approvals

## Future Enhancements (Step 2)

When moving to Step 2 (Waste-Reduction Routing):
- **RouteOptimization**: Multi-stop pickup routes
- **RouteStop**: Individual stops on optimized routes
- **TransportationProvider**: Third-party logistics integration
- **WasteMetrics**: Aggregated analytics per producer, region, category

## Technology Mapping

### Firestore Collections
```
/users/{userId}
/organizations/{orgId}
  - subcollections: /compliance, /pickupWindows
/producers/{producerId}
/buyers/{buyerId}
/surplusListings/{listingId}
/complianceChecklists/{checklistId}
/complianceRecords/{recordId}
/productCategories/{categoryId}
/regions/{regionId}
/subscriptions/{subscriptionId}
/buyerMatches/{matchId}
```

### Akka Actors
- **ProducerActor**: Manages producer state and compliance
- **BuyerActor**: Manages buyer state and preferences
- **SurplusActor**: Manages surplus listing lifecycle
- **MatchingActor**: Calculates buyer matches for surplus
- **ComplianceActor**: Tracks compliance status and renewals

## Open Questions

1. **Document Storage**: Max document size? Retention policy?
2. **Photo Uploads**: Image size limits? Compression? CDN?
3. **Search Performance**: Full-text search via Algolia or Firestore only?
4. **Analytics**: Real-time vs. batch processing for waste metrics?
5. **Notifications**: Email? SMS? Push notifications for match suggestions?
6. **Historical Data**: How long to retain completed/expired listings?
