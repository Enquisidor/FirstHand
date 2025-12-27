# FirstHand Frontend

Next.js/TypeScript frontend for the FirstHand food waste platform.

## Tech Stack

- **Framework:** Next.js 14 (Pages Router)
- **Language:** TypeScript 5.3
- **UI Library:** React 18 + Mantine 7
- **Icons:** Tabler Icons
- **State Management:** Redux Toolkit
- **Utilities:** Lodash
- **Auth & Database:** Firebase
- **HTTP Client:** Axios

## Project Structure

```
frontend/
├── src/
│   ├── pages/              # Next.js pages (routing)
│   │   ├── _app.tsx        # App wrapper
│   │   ├── _document.tsx   # HTML document
│   │   └── index.tsx       # Home page
│   ├── components/         # React components
│   ├── store/              # Redux store
│   │   ├── index.ts        # Store configuration
│   │   └── slices/         # Redux slices
│   ├── lib/                # Utilities
│   │   ├── firebase.ts     # Firebase initialization
│   │   ├── api.ts          # API client
│   │   └── theme.ts        # Mantine theme configuration
│   ├── types/              # TypeScript types
│   └── styles/             # CSS styles
│       └── globals.css
├── public/                 # Static assets
├── .env.example            # Environment template
├── next.config.js          # Next.js configuration
├── tsconfig.json           # TypeScript configuration
└── package.json            # Dependencies
```

## Development

### Prerequisites

- Node.js 20 or higher
- npm or yarn

### Setup

```bash
# Install dependencies
npm install

# Copy environment template
cp .env.example .env.local

# Edit .env.local with your configuration
```

### Environment Variables

Create `.env.local`:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_FIREBASE_API_KEY=your-api-key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your-sender-id
NEXT_PUBLIC_FIREBASE_APP_ID=your-app-id
```

### Running Locally

```bash
# Development server (http://localhost:3000)
npm run dev

# Production build
npm run build

# Start production server
npm start

# Lint
npm run lint

# Type check
npm run type-check
```

## Mantine UI

FirstHand uses Mantine v7 for UI components with a custom green/sustainability theme.

### Theme Configuration

The theme is configured in `src/lib/theme.ts`:

```typescript
import { MantineThemeOverride } from '@mantine/core';

export const theme: MantineThemeOverride = {
  primaryColor: 'green',
  colors: {
    green: [/* custom green palette */],
  },
};
```

### Using Mantine Components

```typescript
import { Button, Text, Paper, Container } from '@mantine/core';
import { IconLeaf } from '@tabler/icons-react';

function MyComponent() {
  return (
    <Container>
      <Paper shadow="sm" p="md">
        <Button leftSection={<IconLeaf size={16} />}>
          Reduce Waste
        </Button>
      </Paper>
    </Container>
  );
}
```

### Available Packages

- **@mantine/core**: Core components (Button, Text, Paper, etc.)
- **@mantine/hooks**: Useful React hooks
- **@mantine/form**: Form management
- **@mantine/notifications**: Toast notifications
- **@tabler/icons-react**: Icon library

### Resources

- [Mantine Documentation](https://mantine.dev/)
- [Mantine Components](https://mantine.dev/core/button/)
- [Tabler Icons](https://tabler-icons.io/)

## Redux Store

### Creating a Slice

```typescript
// src/store/slices/exampleSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface ExampleState {
  value: number;
}

const initialState: ExampleState = {
  value: 0,
};

export const exampleSlice = createSlice({
  name: 'example',
  initialState,
  reducers: {
    increment: (state) => {
      state.value += 1;
    },
    setValue: (state, action: PayloadAction<number>) => {
      state.value = action.payload;
    },
  },
});

export const { increment, setValue } = exampleSlice.actions;
export default exampleSlice.reducer;
```

### Using Redux in Components

```typescript
import { useAppDispatch, useAppSelector } from '@/store';
import { increment } from '@/store/slices/exampleSlice';

function MyComponent() {
  const dispatch = useAppDispatch();
  const value = useAppSelector((state) => state.example.value);

  return (
    <button onClick={() => dispatch(increment())}>
      Count: {value}
    </button>
  );
}
```

## API Integration

### Using the API Client

```typescript
import api from '@/lib/api';

// GET request
const response = await api.get('/endpoint');

// POST request
const response = await api.post('/endpoint', { data });

// With auth token (automatically added by interceptor)
// The interceptor will add Firebase auth token if available
```

## Firebase Usage

### Authentication

```typescript
import { auth } from '@/lib/firebase';
import { signInWithEmailAndPassword } from 'firebase/auth';

const login = async (email: string, password: string) => {
  const userCredential = await signInWithEmailAndPassword(auth, email, password);
  return userCredential.user;
};
```

### Firestore

```typescript
import { db } from '@/lib/firebase';
import { collection, getDocs } from 'firebase/firestore';

const getUsers = async () => {
  const querySnapshot = await getDocs(collection(db, 'users'));
  return querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
};
```

## Styling

- Global styles in `src/styles/globals.css`
- Component-scoped CSS modules (create `Component.module.css`)
- Supports dark mode via CSS variables

## Building for Production

```bash
# Create optimized production build
npm run build

# Test production build locally
npm start
```

## Deployment

### Deploy to Firebase Hosting

```bash
# Build the app
npm run build

# Deploy
firebase deploy --only hosting
```

### Automatic Deployment

Push to `main` branch triggers GitHub Actions deployment.

## Code Style

- Use TypeScript for all files
- Use functional components with hooks
- Use Redux Toolkit for state management
- Use Lodash for utility functions
- Follow ESLint rules

## Troubleshooting

### Build Errors

```bash
# Clear cache and rebuild
rm -rf .next node_modules
npm install
npm run build
```

### Type Errors

```bash
# Run type checker
npm run type-check
```

### Firebase Connection Issues

Verify environment variables are set correctly in `.env.local`.

## Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [React Documentation](https://react.dev/)
- [Redux Toolkit](https://redux-toolkit.js.org/)
- [Firebase Documentation](https://firebase.google.com/docs)
