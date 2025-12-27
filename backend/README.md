# FirstHand Backend

Scala/Akka backend for the FirstHand food waste platform.

## Architecture

The backend uses Akka's actor model for scalability and resilience:

- **Akka HTTP:** REST API endpoints
- **Akka Actors:** Business logic and state management
- **Akka Persistence:** Event sourcing for audit trails
- **Akka Cluster:** Distributed system support (future)

## Tech Stack

- **Scala:** 2.13.12
- **Akka:** 2.8.5
- **Akka HTTP:** 10.5.3
- **Firebase Admin SDK:** 9.2.0
- **Google Cloud Firestore:** 3.14.5
- **Google Cloud Storage:** 2.29.1

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── scala/firsthand/
│   │   │   ├── Main.scala              # Application entry point
│   │   │   ├── FirebaseInitializer.scala
│   │   │   └── CborSerializable.scala  # Serialization marker
│   │   └── resources/
│   │       └── application.conf        # Akka configuration
│   └── test/
│       └── scala/                      # Test files
├── project/
│   ├── build.properties               # SBT version
│   └── plugins.sbt                    # SBT plugins
├── build.sbt                          # Build configuration
└── app.yaml                           # GCP App Engine config
```

## Configuration

Configuration is managed via `application.conf` and environment variables.

### application.conf

```hocon
akka {
  loglevel = "INFO"
  actor.provider = "cluster"
}

firsthand {
  http {
    interface = "0.0.0.0"
    port = 8080
  }

  firebase {
    credentials-path = ${?GOOGLE_APPLICATION_CREDENTIALS}
    project-id = ${?FIREBASE_PROJECT_ID}
  }
}
```

### Environment Variables

Create a `.env` file (from `.env.example`):

```bash
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
FIREBASE_PROJECT_ID=your-project-id
GCP_PROJECT_ID=your-gcp-project-id
GCP_STORAGE_BUCKET=your-storage-bucket
```

## Development

### Prerequisites

- Java 17 or higher
- SBT 1.9.7 or higher

### Running Locally

```bash
# Run tests
sbt test

# Compile
sbt compile

# Run server (http://localhost:8080)
sbt run

# Run with auto-reload (using sbt-revolver)
sbt ~reRun
```

### API Endpoints

#### Health Check
```
GET /health
Response: "OK"
```

#### Status
```
GET /api/v1/status
Response: {"status": "running", "service": "FirstHand API"}
```

## Testing

```bash
# Run all tests
sbt test

# Run specific test
sbt "testOnly firsthand.SomeSpec"

# Run tests with coverage
sbt clean coverage test coverageReport
```

## Building

```bash
# Create distributable package
sbt stage

# Create Docker image (future)
sbt docker:publishLocal
```

## Deployment

### Deploy to GCP App Engine

```bash
# Authenticate
gcloud auth login

# Set project
gcloud config set project YOUR_PROJECT_ID

# Deploy
gcloud app deploy app.yaml
```

### Using GitHub Actions

Push to `main` branch triggers automatic deployment.

## Code Style

- Follow [Scala Style Guide](https://docs.scala-lang.org/style/)
- Use meaningful variable names
- Write tests for business logic
- Document public APIs

## Adding New Features

1. Create actor hierarchy in `src/main/scala/firsthand/actors/`
2. Add HTTP routes in `src/main/scala/firsthand/routes/`
3. Update `Main.scala` to wire routes
4. Write tests in `src/test/scala/`

## Troubleshooting

### Firebase Connection Issues

Make sure `GOOGLE_APPLICATION_CREDENTIALS` points to a valid service account key with proper permissions.

### Port Already in Use

Change the port in `application.conf`:

```hocon
firsthand.http.port = 8081
```

## Resources

- [Akka Documentation](https://doc.akka.io/)
- [Scala Documentation](https://docs.scala-lang.org/)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
