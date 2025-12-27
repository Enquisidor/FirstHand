# Contributing to FirstHand

Thank you for your interest in contributing to FirstHand! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help maintain a welcoming environment

## Getting Started

1. **Fork the repository**
2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/FirstHand.git
   cd FirstHand
   ```
3. **Set up development environment** (see README.md)

## Development Workflow

### 1. Create a Branch

```bash
# Feature branch
git checkout -b feature/your-feature-name

# Bug fix branch
git checkout -b fix/bug-description
```

### 2. Make Changes

- Write clean, maintainable code
- Follow existing code style
- Add tests for new features
- Update documentation as needed

### 3. Test Your Changes

**Backend:**
```bash
cd backend
sbt test
```

**Frontend:**
```bash
cd frontend
npm run lint
npm run type-check
npm run build
```

### 4. Commit Changes

Use clear, descriptive commit messages:

```bash
git add .
git commit -m "feat: add compliance checklist feature"
```

**Commit message format:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Adding or updating tests
- `chore:` Maintenance tasks

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

## Code Style Guidelines

### Scala/Backend

- Follow [Scala Style Guide](https://docs.scala-lang.org/style/)
- Use meaningful variable names
- Document public APIs with Scaladoc
- Write unit tests for business logic
- Keep actors focused on single responsibilities

Example:
```scala
/**
 * Manages producer compliance status
 */
class ProducerComplianceActor extends Actor {
  def receive: Receive = {
    case CheckCompliance(producerId) =>
      // Implementation
  }
}
```

### TypeScript/Frontend

- Use TypeScript for all files
- Use functional components with hooks
- Avoid `any` types
- Document complex logic
- Use meaningful component names

Example:
```typescript
interface ComplianceChecklistProps {
  producerId: string;
  onComplete: () => void;
}

export function ComplianceChecklist({ producerId, onComplete }: ComplianceChecklistProps) {
  // Implementation
}
```

## Testing

### Backend Tests

```scala
class ProducerServiceSpec extends AnyWordSpec with Matchers {
  "ProducerService" should {
    "validate compliance requirements" in {
      // Test implementation
    }
  }
}
```

### Frontend Tests

(Add testing framework as needed - Jest, React Testing Library, etc.)

## Pull Request Guidelines

### Before Submitting

- [ ] Code follows project style guidelines
- [ ] Tests pass locally
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] No merge conflicts with main branch

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How has this been tested?

## Screenshots (if applicable)

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Tests added/updated
- [ ] Documentation updated
```

## Feature Development Guidelines

When adding features, consider the two-step strategy:

### Step 1 Features (Safe - Build Trust)
- Compliance checklists
- Buyer/seller directories
- Documentation templates
- Logistics coordination tools
- Information sharing

### Step 2 Features (Routing Optimization)
- Surplus matching algorithms
- Route optimization
- Pickup coordination
- Waste metrics

### Red Lines (Never Build)
- Auto-assigning transactions
- Mandatory payment processing
- Price-setting features
- Per-transaction fees
- Market operator functions

## Architectural Decisions

### Backend Patterns

1. **Use Akka Actors for:**
   - State management
   - Long-running processes
   - Message-driven workflows

2. **Use Akka HTTP for:**
   - REST API endpoints
   - Request/response handling

3. **Use Firebase for:**
   - User authentication
   - Document storage
   - Real-time data sync

### Frontend Patterns

1. **Use Redux for:**
   - Global application state
   - Complex state logic
   - Cross-component data

2. **Use Local State for:**
   - Component-specific UI state
   - Form inputs
   - Temporary data

3. **Use Firebase for:**
   - Real-time listeners
   - Authentication state
   - File uploads

## Questions?

- Open an issue for bugs or feature requests
- Use discussions for questions
- Tag maintainers for urgent issues

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
