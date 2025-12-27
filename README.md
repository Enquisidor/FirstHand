# FirstHand

**Reducing food waste through regulatory friction reduction and smart coordination for Ontario's small food producers.**

## The Problem

Small food producers, co-ops, and independent retailers in Ontario waste food because there's no good way to connect surplus with nearby buyers. Existing tools serve big chains, not the little guys.

## The Solution

FirstHand uses a two-step strategy to build trust and reduce waste:

### Step 1: Build Trust Through Regulatory Friction Reduction
- Help small food actors navigate Ontario's food safety regulations
- Provide compliance checklists and documentation templates
- Connect producers with nearby buyers
- Coordinate logistics and pickup windows
- **Goal:** Build trust and adoption without triggering "market operator" regulations

### Step 2: Layer On Waste-Reduction Routing
- Treat food waste as a routing optimization problem
- Match surplus food with nearby buyers
- Optimize multi-stop pickup routes
- **Goal:** Reduce waste through coordination (not control)

## Tech Stack

### Backend
- **Language:** Scala 2.13
- **Framework:** Akka (Actor model, HTTP, Persistence, Clustering)
- **Database:** Firebase Firestore
- **Cloud:** Google Cloud Platform (App Engine, Cloud Storage)

### Frontend
- **Framework:** Next.js 14 (React 18)
- **Language:** TypeScript
- **State Management:** Redux Toolkit
- **Utilities:** Lodash
- **Auth & Database:** Firebase

### DevOps
- **CI/CD:** GitHub Actions
- **Hosting:** Firebase Hosting (frontend), GCP App Engine (backend)

## Project Structure

```
FirstHand/
├── backend/              # Scala/Akka backend
│   ├── src/
│   │   ├── main/scala/   # Application code
│   │   └── test/scala/   # Tests
│   ├── build.sbt         # SBT build configuration
│   └── app.yaml          # GCP App Engine config
├── frontend/             # Next.js/TypeScript frontend
│   ├── src/
│   │   ├── pages/        # Next.js pages
│   │   ├── components/   # React components
│   │   ├── store/        # Redux store
│   │   ├── lib/          # Utilities, Firebase config
│   │   └── styles/       # CSS styles
│   └── package.json      # NPM dependencies
├── .github/
│   └── workflows/        # CI/CD workflows
├── firebase.json         # Firebase configuration
├── firestore.rules       # Firestore security rules
└── storage.rules         # Cloud Storage security rules
```

## Getting Started

### Prerequisites

- Java 17 or higher (for Scala/Akka backend)
- Node.js 20 or higher (for Next.js frontend)
- SBT (Scala Build Tool)
- Firebase CLI (`npm install -g firebase-tools`)
- Google Cloud SDK (`gcloud`)

### Backend Setup

```bash
cd backend

# Copy environment template
cp .env.example .env
# Edit .env with your Firebase/GCP credentials

# Run tests
sbt test

# Start development server (runs on http://localhost:8080)
sbt run
```

See [backend/README.md](backend/README.md) for detailed backend documentation.

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Copy environment template
cp .env.example .env.local
# Edit .env.local with your Firebase config

# Start development server (runs on http://localhost:3000)
npm run dev
```

See [frontend/README.md](frontend/README.md) for detailed frontend documentation.

### Firebase Setup

```bash
# Login to Firebase
firebase login

# Initialize Firebase project
firebase init

# Deploy Firestore rules
firebase deploy --only firestore:rules

# Deploy Storage rules
firebase deploy --only storage:rules
```

## Development Workflow

1. **Start backend:** `cd backend && sbt run`
2. **Start frontend:** `cd frontend && npm run dev`
3. **Access app:** http://localhost:3000
4. **API endpoint:** http://localhost:8080/api/v1

## Deployment

### Deploy Backend to GCP App Engine

```bash
cd backend
gcloud app deploy app.yaml
```

### Deploy Frontend to Firebase Hosting

```bash
cd frontend
npm run build
firebase deploy --only hosting
```

### Automated Deployment

Push to `main` branch triggers automatic deployment via GitHub Actions.

## Environment Variables

### Backend (.env)
```
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
FIREBASE_PROJECT_ID=your-project-id
GCP_PROJECT_ID=your-gcp-project-id
GCP_STORAGE_BUCKET=your-storage-bucket
```

### Frontend (.env.local)
```
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_FIREBASE_API_KEY=your-api-key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your-sender-id
NEXT_PUBLIC_FIREBASE_APP_ID=your-app-id
```

## Business Model

**Subscription-based pricing** (flat or tiered fees) with outcome-based rebates when clients hit waste reduction goals. This keeps us out of "market operator" territory.

### Safe (We Do This)
- Information and coordination tools
- Compliance checklists and templates
- Buyer/seller suggestions
- Subscription fees
- Outcome-based rebates

### Dangerous (We Avoid This)
- Auto-assigning buyers
- Setting prices
- Mandatory payment processing
- Per-transaction fees
- Becoming a regulated market operator

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

See [LICENSE](LICENSE) for details.

## Contact

For questions or support, please open an issue on GitHub
