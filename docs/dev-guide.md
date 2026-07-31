# Development Guide

## Index

- [Introduction](#introduction)
- [Technologies](#technologies)
- [Tools](#tools)
- [Architecture](#architecture)
- [SEO](#seo)
- [Internationalization (i18n)](#internationalization-i18n)
- [Payments & Premium (Stripe)](#payments--premium-stripe)
- [Quality Control](quality-control.md)
- [Development Process](#development-process)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Deployment](#deployment)
- [Creating a Release](#creating-a-release)

---

## Introduction

ChineseReads follows a **distributed MVC architecture** composed of multiple independent services that communicate via REST APIs. The system is split into four main components: a Spring Boot backend (REST API + business logic), an Angular frontend (SPA), a MySQL database, and three Python microservices for AI text generation, OCR image processing, and text-to-speech audio.

| Aspect | Description |
|---|---|
| **Type** | Web MVC + SPA frontend + REST API + AI Microservices |
| **Technologies** | Java 21, Spring Boot 4, Angular 17, Transloco (i18n), MySQL 8, Caffeine (cache), Python 3.11, Flask, DeepSeek API, Google Cloud Vision, Google Cloud Text-to-Speech, Stripe (payments) |
| **Tools** | IntelliJ IDEA, VS Code, Docker, Docker Compose, Caddy, GitHub, Postman |
| **Quality Control** | Unit tests (JUnit + Mockito), Integration tests (H2), E2E tests (MockMvc), Frontend tests (Jasmine/Karma) |
| **Deployment** | Docker Compose + Caddy (HTTPS automatic via Let's Encrypt), Azure VM |
| **Development Process** | Iterative and incremental, GitHub Issues + Projects, feature branches |

---

## Technologies

### Backend — Spring Boot
Java 21 + Spring Boot 4. Exposes a REST API consumed by the Angular frontend. Handles authentication via JWT stored in HttpOnly cookies, text management, dictionary word management (admin CRUD), vocabulary collections, flashcards, user management, and PREMIUM subscriptions via Stripe (see [Payments & Premium](#payments--premium-stripe)).  
Official site: https://spring.io/projects/spring-boot

### Frontend — Angular 17
Standalone components architecture with SSR (Server Side Rendering) enabled via Angular Universal. Communicates with the backend exclusively via HTTP. SEO is a first-class requirement (see the [SEO](#seo) section below): a reusable `SeoService` sets per-route title, meta description, canonical, Open Graph and Twitter tags on every navigation — including during SSR — so crawlers receive page-specific metadata. The UI is bilingual (English / Spanish) via [Transloco](https://jsverse.github.io/transloco/) runtime i18n, served from distinct URLs (English at the root, Spanish under `/es`) so it stays SEO-safe (see [Internationalization](#internationalization-i18n)).  
Official site: https://angular.io

### Database — MySQL 8
Relational database storing users, texts, words, collections, and flashcards. Managed via Spring Data JPA / Hibernate.  
Official site: https://www.mysql.com

### AI Microservice — DeepSeek API
Python 3.11 + Flask microservice running on port 5001. Calls the DeepSeek API to generate Chinese texts by HSK level and topic, produce titles and descriptions, translate sentence by sentence, and suggest translations for missing dictionary words.  
Official site: https://www.deepseek.com

### OCR Microservice — Google Cloud Vision
Python 3.11 + Flask microservice running on port 5000. Uses the Google Cloud Vision `text_detection` API to extract Chinese text from uploaded images. Free up to 1 million requests/month.  
Official site: https://cloud.google.com/vision

### TTS Microservice — Google Cloud Text-to-Speech
Python 3.11 + Flask microservice running on port 5002. Uses the Google Cloud Text-to-Speech API (WaveNet Mandarin voice `cmn-CN-Wavenet-A`) to synthesize natural audio for a full text, an individual word, a sentence, or a saved flashcard. The backend exposes it at `/api/tts` for **registered users only** (anonymous → `401`; see the plans table for the per-user monthly audio quotas). Free up to 1 million WaveNet characters/month.  
Because it is paid per character, the backend protects it on several fronts (all configurable in `application.properties`): a **length cap** (`tts.max-chars`, rejects an oversized request with `413`), a **Caffeine cache** of the synthesized audio keyed by the exact text (`tts.cache.*`, so identical words/sentences/flashcards are never re-synthesized — the biggest ongoing saving), a **per-IP rate limit** (`tts.rate-limit.per-minute`, returns `429` when exceeded; the client IP is read from `X-Forwarded-For` since the app runs behind Caddy), and a **per-user monthly quota** (`AudioUsageService`).  
Official site: https://cloud.google.com/text-to-speech

### Payments — Stripe
Recurring subscriptions for the **PREMIUM** plan, integrated in the backend via the Stripe Java SDK (`com.stripe:stripe-java`). Payment happens on **Stripe Checkout's hosted page** (redirect flow), so the app never handles card data (no PCI scope). When Stripe confirms a payment it calls a **signature-verified webhook** (`POST /api/premium/webhook`) that sets the user's premium expiry. A **Billing Portal** session lets users manage or cancel. All Stripe credentials are injected from the environment and empty by default, so the app boots and runs normally with billing disabled. See the [Payments & Premium (Stripe)](#payments--premium-stripe) section for the full flow and configuration.  
Official site: https://stripe.com

### Reverse Proxy — Caddy
Serves the Angular static files, proxies `/api/*` requests to the Spring Boot backend, and manages HTTPS certificates automatically via Let's Encrypt.  
Official site: https://caddyserver.com

### Chinese Tokenizer — Jieba
Java library integrated in the backend to segment Chinese text into individual words. Used for text breakdown and dictionary validation.  
Official site: https://github.com/huaban/jieba-analysis

---

## Tools

### Visual Studio Code
IDE for Java/Spring Boot backend development, Angular frontend development and Python microservices.  
Official site: https://code.visualstudio.com

### Docker + Docker Compose
Used to containerize all services (backend, database, frontend proxy, AI microservice, OCR microservice, TTS microservice) and orchestrate them in a single `docker-compose.yml`.  
Official site: https://www.docker.com

### GitHub
Version control, issue tracking, and project management via GitHub Issues and GitHub Projects.  
Official site: https://github.com

### Postman
Used to test and document the REST API. A collection with examples of all API endpoints is available at `docs/ChineseReads.postman_collection.json`.  
Official site: https://www.postman.com

---

## Architecture

To see full details about the architecture, refer to the [Analysis](analysis.md) doc.

### Deployment Architecture

All services communicate within the same Docker network (`app-network`). Only ports 80 and 443 are exposed to the internet via Caddy.

### API Documentation

The REST API documentation is available in OpenAPI format. You can view it at:  
👉 [API Documentation](https://raw.githack.com/codeurjc-students/2025-ChineseTexts/main/docs/api/openapi.html) *(pending generation)*

A Postman collection with examples of all endpoints is available at:  
`docs/ChineseReads.postman_collection.json` *(pending)*

---

## SEO

> ⚠️ **SEO is a permanent, first-class requirement.** Any new public page or content change must keep the app fully optimized for search engines. When adding a route, add its metadata to the SEO route table.

The frontend is optimized so public pages rank for queries like *"learn chinese by reading"*, *"chinese reads"*, *"chinese texts"*, *"read chinese texts"*, *"learn chinese with graded texts"* and *"chinese graded readers"*.

### How it works

| Piece | File | Purpose |
|---|---|---|
| **Server-Side Rendering** | Angular Universal (`server.ts`, `main.server.ts`) | Public pages render to full HTML so crawlers index real content. |
| **Per-route metadata** | `src/app/services/seo.service.ts` | Reusable, SSR-safe service that sets title, description, canonical, Open Graph and Twitter tags on every navigation. |
| **Route → metadata table** | `src/app/services/seo.config.ts` | One place mapping each route (incl. `/texts/:level`) to its SEO metadata. **Add new public routes here.** |
| **Dynamic text pages** | `src/app/components/text/text.component.ts` | Each `/text/:id` page sets a unique title/description from the text itself. |
| **Prerendered parameterised routes** | `prerender-routes.txt` (referenced from `angular.json` → `prerender.routesFile`) | Lists fixed parameterised URLs to prerender to static HTML — currently the six `/texts/HSK1…6` level pages — so they get real per-route meta in the initial HTML (not just client-side). |
| **Default tags + structured data** | `src/index.html` | Site-wide defaults, plus JSON-LD (`WebSite`, `EducationalOrganization`, `WebApplication`) for rich results. |
| **Crawl directives** | `src/robots.txt` | Allows public routes, disallows private/authenticated ones, points to the sitemap. |
| **Sitemap** | `src/sitemap.xml` | Lists the public, indexable URLs. |
| **PWA manifest** | `src/manifest.webmanifest` | App name, theme color and icon. |

All four static files (`robots.txt`, `sitemap.xml`, `manifest.webmanifest`, `favicon.ico`) are declared in `angular.json` → `assets`, so they are copied to the build output and served at the site root.

### Adding a new public page (checklist)

1. Add the route in `app.routes.ts` (a single `appRoutes` array is served both at the root and under the `/es` prefix, so one entry covers both languages — see [Internationalization](#internationalization-i18n)).
2. Add its metadata (title, description, `path`) to `STATIC_SEO` in `seo.config.ts` **in both `en` and `es`**. Keep titles keyword-rich and under ~60 characters where possible.
3. If it should be indexed, add its `<loc>` to `sitemap.xml` **for both the English and the `/es` URL, with `hreflang` alternates**, and add the route (and its `/es` twin) to `prerender-routes.txt`; if it is private/authenticated, set `noindex: true` and add a `Disallow` line to `robots.txt` (root and `/es`).
4. **If the page shows database data, it must live-render (SSR), never serve a prerendered snapshot** — a snapshot baked at build time (with no backend reachable) would shadow live SSR forever with a frozen empty state (the bug fixed in PR #140). For every such page: add its path (and the `/es` twin) to `@ssrPages` in `docker/Caddyfile` **and**, if the path is static (no `:param` — parameterised routes are never prerendered and are safe), to `SSR_ROUTES` in `frontend/scripts/remove-ssr-prerender.mjs`. Never ignore that script's "missing file" warning at build time, and verify the deployed page **with real data in the raw HTML** (`curl | grep`), not just with a correct title — "right title + empty list" is exactly what the frozen snapshot looks like.

---

## Internationalization (i18n)

The UI is available in **English and Spanish** (a flag switcher in the header). Only the interface chrome is translated — the DB text content already carries its own English/Spanish fields. i18n is implemented with **[Transloco](https://jsverse.github.io/transloco/)** (runtime, single build) and is designed so it never compromises SEO.

| Piece | File | Purpose |
|---|---|---|
| **Translation dictionaries** | `src/assets/i18n/en.json`, `es.json` | All UI strings, namespaced per component. Keep both files in key parity. |
| **Synchronous loader** | `src/app/transloco-loader.ts` | Statically imports both dictionaries so `setActiveLang` applies within the same tick — essential so the **prerender bakes each page in the right language**. |
| **URL scheme** | `src/app/app.routes.ts` | English at the root (`/texts`), Spanish under `/es` (`/es/texts`). A single `appRoutes` array is mounted at both, so components are never duplicated. |
| **Active language** | `src/app/app.component.ts` + `src/app/i18n/locale.util.ts` | The active language is derived **solely from the URL prefix** (never from `navigator`/`localStorage`), so server (prerender) and client always agree — no hydration mismatch. |
| **Locale-aware links** | `src/app/i18n/localize-link.pipe.ts` (`\| localizeLink`), `locale-nav.service.ts` (`LocaleNavService`) | Keep navigation within the current language for `routerLink`s and imperative `router.navigate` calls. |
| **Per-locale SEO** | `src/app/services/seo.service.ts`, `seo.config.ts` | `SeoService.update(config, lang)` emits `hreflang` alternates (`en`/`es`/`x-default`), `og:locale`, `<html lang>` and a self-referencing canonical; `seo.config.ts` is bilingual. `sitemap.xml` carries `hreflang` alternates and `/es` URLs; `prerender-routes.txt` prerenders the `/es` pages to static HTML. |

**Design choices (why):** production serves **prerendered static HTML** (Caddy serves `dist/frontend/browser`; there is no live Node SSR), so each language must be baked into static HTML at build time — hence the synchronous loader. Transloco (runtime, one build, one Caddy mount) was chosen over Angular's compile-time `@angular/localize` (which would need one build per locale and Caddy changes). English stays at the root so existing indexed URLs never move (zero SEO regression); Spanish `/es` URLs are purely additive.

**Adding or changing a UI string:** add the key to **both** `en.json` and `es.json` (keep parity) and reference it with `{{ 'namespace.key' | transloco }}` in templates or `transloco.translate('namespace.key')` in TypeScript. In tests, add `translocoTesting()` (from `src/app/i18n/transloco-testing.ts`) to the component's `TestBed` `imports`.

---

## Payments & Premium (Stripe)

ChineseReads follows a **freemium** model: every reading/learning tool stays usable for free (within limits), and a paid **PREMIUM** plan lifts the limits on the parts that cost money — AI text generation, audio (TTS), and the AI "ask about words/context" tutor chat. Premium is sold as a **recurring subscription** (monthly / yearly) through Stripe.

### Premium status — single source of truth

A user is premium while `User.premiumUntil` (a nullable `LocalDateTime`) is in the future (`User.isPremiumActive()`). This one timestamp is the **only** source of truth — deliberately not a static role, which would not expire on its own and would drift with the stateless JWT. `premiumUntil` is set from **two** paths that share the same field:

- **Admin grant** — an admin can grant/extend/revoke premium *until a chosen date & time* from the users panel (`PATCH /api/users/{id}/premium`). Useful for demos, support and comps, and needs no Stripe.
- **Stripe** — a self-service subscription payment sets it via the webhook (below).

### Plans & limits

The concrete guard is the **text-generation quota**, tiered by plan. Free users have a small monthly quota; **premium is unlimited monthly and exempt from the global daily fuse** (they paid for "unlimited", so the shared cap must never block them, and their usage must not starve free users of it) — bounded only by a high per-user daily fair-use cap against a single abusive account. All limits live in `application.properties`.

| Limit | Free | Premium | Admin | Property |
|---|---|---|---|---|
| Own-text generations / month | 10 | unlimited | unlimited (exempt) | `usage.user.monthly-limit` |
| Own-text generations / day (fair-use cap) | — | 100 | — | `usage.premium.daily-limit` |
| Global generations / day (cost fuse) | 200 | exempt | 200 | `usage.global.daily-limit` |
| Max chars per generated text | 1500 | 1500 | 1500 | `usage.text.max-chars` |
| Audio (TTS) plays / month — words · sentences+text | registered-only · 100 · 15 | unlimited | unlimited | `usage.audio.word.monthly-limit` / `usage.audio.phrase.monthly-limit` |
| AI word-chat messages / month | registered-only · 10 | unlimited | unlimited | `usage.chat.monthly-limit` |

`UsageService.reserveGeneration` (called by `POST /api/my-texts`) applies one of three lanes: **free** users spend their monthly quota **and** the global daily fuse; **premium** users spend only a per-user daily fair-use counter (no monthly limit, exempt from the global fuse); **admins** are exempt from personal quotas but still bound by the global fuse. `reserveOcr` (the OCR extract step) charges the global fuse only.

Audio (`POST /api/tts`) is **registered-only** (anonymous → 401): `AudioUsageService.reserveAudio` meters two per-user monthly buckets — single words (cheap, high quota) and sentences/full text (expensive, small quota) — with premium and admins exempt. On top of that the endpoint keeps the hard length cap, the per-IP rate limit and the text-keyed cache (`tts.*`).

The AI contextual word chat (`POST /api/chat/word`) is likewise **registered-only**: `ChatUsageService.reserveChat` meters a per-user monthly message quota (`usage.chat.monthly-limit`, free 10 / premium·admin unlimited). The controller forwards the word, its context (sentence, full text, translation, HSK level), the user's language and the running message history to the AI service's `/chatWord` route, which holds a guardrail system prompt. The chat is stateless server-side (the history is re-sent each turn).

### How the Stripe integration works

| Piece | File | Purpose |
|---|---|---|
| **Service** | `backend/.../Service/StripeService.java` | Creates the subscription Checkout session and the Billing Portal session; verifies and applies webhook events. Credential-gated by `isConfigured()`. |
| **Controller** | `backend/.../Controller/PremiumController.java` | `POST /api/premium/checkout` and `/portal` (authenticated) return a Stripe-hosted URL to redirect to; `POST /api/premium/webhook` (public, signature-verified) receives events. |
| **Security** | `backend/.../Security/SecurityConfig.java` | The webhook is `permitAll` (Stripe is unauthenticated; verified by signature); the rest of `/api/premium/**` requires `USER`/`ADMIN`. |
| **User fields** | `backend/.../Model/User.java` | `premiumUntil`, plus `stripeCustomerId` / `stripeSubscriptionId` to reconcile webhook events. Added automatically on deploy (`ddl-auto=update`, nullable). |
| **Frontend service** | `frontend/.../services/premium.service.ts` | `checkout(plan)` / `portal()` → the app just redirects to the returned URL. |
| **Pricing page** | `frontend/.../components/premium/` | Public, bilingual, prerendered `/premium` marketing page with monthly/yearly plans (and a per-month equivalent for the annual plan). |
| **Success page** | `frontend/.../components/premium-success/` | Post-checkout `/premium/success`; refreshes the cached user so the new `premiumUntil` shows immediately. |
| **Plan awareness** | `frontend/.../services/login.service.ts` (`isPremiumActive()`), header & profile | An "upgrade / manage" link and the plan shown on the profile. |

### Payment flow

1. The user clicks **Get Premium** → `POST /api/premium/checkout {plan}` → the backend ensures a Stripe **Customer** (stored on the user), builds a **Checkout Session** (`mode=subscription`, `client_reference_id = userId`, success/cancel URLs from `app.public-url`, the plan's `price_…`) and returns its URL.
2. The frontend redirects to Stripe's hosted page; the user pays.
3. Stripe redirects to `/premium/success` **and** — the authoritative path — calls the **webhook** (`checkout.session.completed`, `customer.subscription.created/updated`). `StripeService.handleWebhook` verifies the signature, then sets `premiumUntil` from the subscription's `current_period_end` on the matching user (found by `client_reference_id`, else subscription/customer id). Renewals push `premiumUntil` forward; `customer.subscription.deleted` lets it lapse.

> **Webhook robustness:** the account's Stripe API version can be newer than the pinned SDK, so the handler parses the event from the **raw JSON** (`id`, `customer`, `subscription`, `status`, `current_period_end` are stable field names) rather than the SDK's typed deserializer. This is why `jackson-databind` is a direct backend dependency.

### Configuration

Five environment variables, injected via `docker-compose.yml` and mapped in `application.properties` (`stripe.*`, `app.public-url`) — all empty by default so the app runs with billing disabled. They live only in `docker/.env` on the server (never committed): `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_MONTHLY`, `STRIPE_PRICE_YEARLY`, `APP_PUBLIC_URL`. See [Deployment → Required secret files](#deployment).

**Test vs live:** Stripe fully separates *test* (test cards, no real money) and *live*. To go live, recreate the products/prices, secret key, webhook and Customer Portal config in **live mode** and swap those four Stripe values in `docker/.env` — **the code never changes**.

---

## Development Process

ChineseReads follows an **iterative and incremental** development process inspired by Agile principles and some XP (Extreme Programming) and Kanban practices.

### Task Management
- **GitHub Issues** — each feature, bug, or task is tracked as an issue.
- **GitHub Projects** — visual Kanban board with columns: Backlog, In Progress, Done.

### Git Strategy
- `main` — stable production branch, only receives merges from feature branches via Pull Requests.
- `action/<name>` — where action can be `feature, fix, test, docs, etc.`.
- Commit messages have a descriptive name.

Example workflow:
```bash
git checkout -b feature/my-feature
git add .
git commit -m "feat: add my feature"
git push -u origin feature/my-feature
# Open Pull Request on GitHub, review, merge
git checkout main
git pull
```

### Continuous Integration
*(CI/CD workflows via GitHub Actions — planned for future phases)*

---

## Running the Application

### Prerequisites
- Java 21
- Node.js 20+
- Docker + Docker Compose
- Python 3.11 (for local microservice development)

### Clone the repository
```bash
git clone https://github.com/codeurjc-students/2025-ChineseTexts.git
cd 2025-ChineseTexts
```

### Database (local)
```bash
docker run --name chinesereads-mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=chinesereads \
  -p 3306:3306 -d mysql:8
```

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
The API will be available at `http://localhost:8080`.

### Frontend
```bash
cd frontend
npm install
ng serve
```
The app will be available at `http://localhost:4200`.

### AI Microservice
```bash
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
# Create .env with: DEEPSEEK_API_KEY=your_key_here
python3 deepseekService.py
```
The service will be available at `http://localhost:5001`.

### OCR Microservice
```bash
cd ocr-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
# Place credentials.json from Google Cloud in this folder
export GOOGLE_APPLICATION_CREDENTIALS="./credentials.json"
python3 googleOCRService.py
```
The service will be available at `http://localhost:5000`.

### TTS Microservice
```bash
cd tts-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
# Place credentials.json in this folder (the same Google Cloud key used by the
# OCR service; the "Cloud Text-to-Speech API" must be enabled in the GCP project)
export GOOGLE_APPLICATION_CREDENTIALS="./credentials.json"
python3 ttsService.py
```
The service will be available at `http://localhost:5002`.

---

## Running Tests

### Backend tests
```bash
cd backend
./mvnw test
```
Runs unit tests (Mockito), integration tests (H2 in-memory database), and E2E API tests (MockMvc).

### Frontend tests
```bash
cd frontend
ng test
```
Runs all Jasmine/Karma component tests. Opens a Chrome browser and shows test results.

---

## Deployment

### Required secret files (not committed to Git)

**`docker/.env`:**
```
DEEPSEEK_API_KEY=sk-your-deepseek-key-here

# Stripe (PREMIUM subscription). Leave unset to run without billing.
# From the Stripe dashboard (use the test keys until going live):
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...        # from the webhook endpoint you register at /api/premium/webhook
STRIPE_PRICE_MONTHLY=price_...         # recurring monthly Price ID
STRIPE_PRICE_YEARLY=price_...          # recurring yearly Price ID
APP_PUBLIC_URL=https://chinesereads.com  # origin used for Checkout return URLs
```

**`ocr-service/credentials.json`:**
```json
{
  "type": "service_account",
  "project_id": "chinesereads",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "chinesereads@chinesereads.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "...",
  "universe_domain": "googleapis.com"
}
```

**`tts-service/credentials.json`:** the same Google service-account key as the OCR service. The **Cloud Text-to-Speech API** must be enabled in the Google Cloud project (Console → APIs & Services → Enable APIs → "Cloud Text-to-Speech API").

### Deploy to Azure VM

1. Create a Resource Group and an Ubuntu 24.04 LTS VM on Azure.
2. Open inbound ports 80 (HTTP) and 443 (HTTPS) in the networking settings.
3. Connect via SSH:
```bash
chmod 600 ./chinesereads_key.pem
ssh -i ./chinesereads_key.pem azureuser@<VM_IP>
```

4. Prepare the VM:
```bash
# Install Docker
curl -fsSL https://get.docker.com | sh

# Install Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Add user to docker group
sudo groupadd docker
sudo usermod -aG docker $USER
exit
# Reconnect via SSH

# Clone the repository
git clone https://github.com/codeurjc-students/2025-ChineseTexts.git
cd 2025-ChineseTexts/docker
chmod +x ./deploy.sh
```

5. Create the required secret files:
   - `docker/.env` with the DeepSeek API key
   - `ocr-service/credentials.json` with Google Cloud credentials
   - `tts-service/credentials.json` with the same Google Cloud credentials (Cloud Text-to-Speech API enabled)

6. Deploy:
```bash
./deploy.sh
```

### Verify HTTPS certificates
```bash
# Enter the Caddy container
docker exec -it docker-caddy-1 sh
ls -R /data/caddy/certificates/acme-v02.api.letsencrypt.org-directory/
# Caddy should have generated 2 certificates: chinesereads.com and www.chinesereads.com
exit

# If certificates failed, restart Caddy
docker restart docker-caddy-1
```

### Verify SSR is working
```bash
curl https://chinesereads.com | grep "<title>"
```
If the HTML title ``<title>ChineseReads — Learn Chinese by Reading Graded HSK Texts</title>`` appears in the response, SSR is working correctly. If ``<app-root></app-root>`` or nothing shows, it is not working.

### Verify SEO files are served
```bash
curl https://chinesereads.com/robots.txt      # should list the sitemap and Disallow rules
curl https://chinesereads.com/sitemap.xml      # should list the public URLs
curl -s https://chinesereads.com/texts | grep -o '<meta name="description"[^>]*>'  # route-specific description
```

### Update the deployment after code changes
```bash
cd ~/2025-ChineseTexts
git pull
cd docker
./deploy.sh

# Clean up unused Docker images
docker image prune -f

# Check Docker disk usage
docker system df
```

---

## Creating a Release

1. Make sure all tests pass locally.
2. Merge your feature branch into `main` via Pull Request.
3. Create a new GitHub Release from the `main` branch with a version tag (e.g., `v1.0.0`).
4. Add a description of the changes included in this release.