# End-to-End Tests

This directory contains browser-based end-to-end tests using Playwright.

## Setup

1. Install dependencies:
```bash
npm install
npx playwright install --with-deps
```

## Running Tests

### Run all tests:
```bash
npm run test:e2e
```

### Run tests in UI mode (interactive):
```bash
npm run test:e2e:ui
```

### Run tests in headed mode (see browser):
```bash
npm run test:e2e:headed
```

### Run specific test file:
```bash
npx playwright test navigation.spec.ts
```

## Test Files

- **navigation.spec.ts**: Tests navigation between pages and link functionality
- **pages.spec.ts**: Tests that all pages load correctly
- **links.spec.ts**: Validates all links work and don't return 404s
- **ui-functionality.spec.ts**: Tests UI functionality and responsiveness

## Configuration

Tests are configured in `playwright.config.ts`. The default base URL is `http://localhost:3000`.

To run against a different URL:
```bash
PLAYWRIGHT_TEST_BASE_URL=http://127.0.0.1:8080 npm run test:e2e
```

## CI/CD Integration

These tests should be run in CI/CD pipelines to catch UI regressions before deployment.

