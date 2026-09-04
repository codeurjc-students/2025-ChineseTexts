# Quality Control

## Overview

ChineseReads applies automated testing at multiple levels across both the backend (Java/Spring Boot) and the frontend (Angular). The goal is to verify that the core business logic, API endpoints, and UI components behave correctly. Every feature ships with its own tests, and both full suites are re-run before every merge — the "never break existing" rule.

---

## Backend Tests

### Unit Tests (Mockito)

Located in `backend/src/test/java/com/chinesereads/backend/unit/` (24 test classes, one per service).

Tests that verify the business logic of individual service classes in isolation, using Mockito to mock dependencies. Representative examples:

| Test class | Service under test | Functionalities covered |
|---|---|---|
| `UserServiceTest` | `UserService` | User creation, duplicate email validation, password encoding, role assignment |
| `TextServiceTest` | `TextService` | Text pagination, level filtering, text upload, duplicate title detection |
| `FlashcardServiceTest` | `FlashcardService` | SM-2 spaced-repetition scheduling (intervals, ease factor, due dates) |
| `StripeServiceTest` | `StripeService` | Webhook event dispatch, subscription apply/revoke, plan labelling |
| `PasswordResetServiceTest` | `PasswordResetService` | Token hashing (SHA-256 at rest), expiry, single-use semantics, anti-enumeration |
| `BlogServiceTest` | `BlogService` | Slug derivation, partial updates, jsoup sanitization safelist |
| `HallOfFameServiceTest` | `HallOfFameService` | Slug uniqueness/suffixing, badge whitelist, partial updates |
| `EmailServiceTest` | `EmailService` | Configuration gate, bilingual templates for the three email types |
| `ReviewReminderServiceTest` | `ReviewReminderService` | The four sending conditions, max-1/day idempotency, retry on failure |

Other covered services include usage quotas (text/audio/chat), rate limiters, influencer stats and settlements, activity/streaks, JWT and health checks.

### Integration Tests (H2)

Tests that verify the interaction between the service layer and the database using an in-memory H2 database. Spring Boot test profile (`application-test.properties`) configures H2 with `NON_KEYWORDS=COLLECTION,USER` to avoid SQL conflicts.

| Test class | Functionalities covered |
|---|---|
| `UserServiceIntegrationTest` | End-to-end user registration and retrieval through the real JPA layer |
| `TextServiceIntegrationTest` | Text creation, retrieval, and deletion with real database operations |

### E2E API Tests (RestAssured)

Located in `backend/src/test/java/com/chinesereads/backend/e2e/` (13 test classes). They start the full application on a random port and exercise the real HTTP request/response cycle: `TextApiTest`, `SignupApiTest`, `ProfileApiTest`, `PasswordResetApiTest` (+ its rate-limit twin), `UnsubscribeApiTest`, `BlogApiTest`, `HallOfFameApiTest`, `FounderApiTest`, `InfluencerApiTest`, and one per sitemap (`SitemapApiTest`, `SitemapBlogApiTest`, `SitemapHallOfFameApiTest`).

These tests also verify authorization: that unauthenticated requests return 401, that regular users cannot access admin endpoints (403), and that authenticated admin users can perform all operations.

### Test execution screenshot

![backend-tests](img/backend-test-output.png)

*(screenshot from an early April run; current totals in the table below)*

### Statistics

| Category | Count |
|---|---|
| Test classes | 42 (24 unit, 2 integration, 13 E2E, 2 web-layer, 1 context load) |
| **Total backend tests** | **278** |
| Failures | 0 |

---

## Frontend Tests

### Component Tests (Jasmine/Karma)

Located in `frontend/src/app/**/*.spec.ts` (48 spec files covering components, services, pipes and data catalogs).

Each test file verifies the logic of an Angular component in isolation. HTTP calls are replaced with Jasmine spies or `HttpTestingController`, so no backend is needed. Translations use the real dictionaries via a `translocoTesting()` helper. Representative examples:

| Spec file | Functionalities tested |
|---|---|
| `text.component.spec.ts` | Word splitting, sentence grouping, popovers, translations, save-word panel |
| `collections.component.spec.ts` | Collection loading, study/exam mode start, delete/rename modals |
| `signup.component.spec.ts` | Form validation, registration success/error flows, backend error-code translation |
| `blog-post.component.spec.ts` | Load by slug, language fallback, dynamic SEO + JSON-LD ordering, soft-404 |
| `hall-of-fame-member.component.spec.ts` | Profile rendering, `ProfilePage`/`Person` JSON-LD, soft-404 |
| `forgot-password` / `reset-password` specs | Local validation, neutral success state, invalid/expired-token state |
| `seo.config.spec.ts` / `seo.service.spec.ts` | Route → metadata resolution, noindex locks, Open Graph tag lifecycle |

### Test execution screenshot

![frontend-tests](img/frontend-test-output.png)

*(screenshot from an early April run; current totals in the table below)*

### Statistics

| Metric | Value |
|---|---|
| Total specs | 297 |
| Failures | 0 |
| Spec files | 48 |

---

## Code Metrics

Approximate line counts (source only, excluding generated code and dependencies):

| Layer | Files | Approx. lines of code |
|---|---|---|
| Backend (Java, main) | 144 | ~12,400 |
| Backend (Java, tests) | 42 | ~6,300 |
| Frontend (TypeScript/HTML/SCSS) | 216 | ~25,900 |
| Python microservices | 4 | ~1,000 |
| **Total** | **~400** | **~45,600** |
