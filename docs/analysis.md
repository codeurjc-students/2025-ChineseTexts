# Analysis

## Architecture Overview

ChineseReads uses a **distributed MVC architecture** composed of seven containers (see `docker/docker-compose.yml`):

1. **Caddy** — reverse proxy, HTTPS (Let's Encrypt), static file serving, and routing of dynamic pages to the SSR container
2. **Spring Boot Backend** — REST API, business logic, JWT authentication, transactional email (Brevo API), dynamic sitemaps
3. **Angular Frontend** — SPA built with prerendered static HTML for fixed-content pages, plus a **live SSR container** (`frontend-ssr`, Node) that renders every page showing database data (texts, blog, Hall of Fame, founder) on demand
4. **MySQL** — relational database
5. **DeepSeek AI Microservice** — Python/Flask, text generation, translation and the contextual word tutor
6. **Google OCR Microservice** — Python/Flask, Chinese text extraction from images
7. **Google TTS Microservice** — Python/Flask, text-to-speech audio synthesis of Chinese text

Web analytics are collected with **Umami** (cookie-free, anonymous aggregates only — no cookie banner needed).

### Cloud Architecture Diagram

![Cloud Architecture](img/architecture_v2.svg)

## API Design

The backend exposes a RESTful API organized around the following resources:

| Resource | Base path | Description |
|---|---|---|
| Texts | `/api/texts` | CRUD for Chinese texts, word/sentence breakdown |
| Words | `/api/words` | Dictionary lookup, plus admin word management (create, update, delete) |
| Users | `/api/users` | Registration, profile management, self-service account deletion, one-click email unsubscribe |
| Auth | `/api/auth` | Login, logout, token refresh, forgot/reset password |
| Collections | `/api/collections` | Collection management, flashcards |
| Flashcards | `/api/flashcards` | Spaced-repetition (SM-2) review of saved words |
| My Texts | `/api/my-texts` | User-generated private texts (paste or OCR photo), quota-limited |
| AI | `/api/ai` | Admin text generation and OCR processing |
| Audio (TTS) | `/api/tts` | Text-to-speech synthesis (registered-only, quota-limited, cached, rate-limited) |
| Chat | `/api/chat` | AI contextual word tutor (registered-only, monthly quota) |
| Activity | `/api/activity` | Reading log, streaks and personal statistics |
| Usage | `/api/usage` | The caller's current quota counters |
| Premium | `/api/premium` | Stripe checkout, billing portal, signature-verified webhook |
| Influencers | `/api/influencers` | Admin: promotion codes, conversion stats, commission settlements |
| Blog | `/api/blog` | Public reading of published posts; admin CRUD with sanitized rich text |
| Hall of Fame | `/api/hall-of-fame` | Public influencer showcase (list + per-slug profile); admin CRUD |
| Founder | `/api/founder` | Public creator CV page; admin in-place editing |
| Health | `/api/health` | Liveness of the backend and each microservice |
| Sitemaps | `/sitemap-texts.xml`, `/sitemap-blog.xml`, `/sitemap-hall-of-fame.xml` | Dynamic sitemaps for DB-backed pages (outside `/api` on purpose — the sitemap protocol requires root scope) |

## Security

- Authentication via **JWT tokens stored in HttpOnly cookies** (not accessible from JavaScript); stable `JWT_SECRET` so sessions survive redeploys.
- Role-based access control: `ROLE_USER` and `ROLE_ADMIN`. PREMIUM is a time-boxed state (`premiumUntil`), not a role.
- Passwords hashed with **BCrypt**.
- Self-service password reset: emailed single-use token (only its **SHA-256 hash** is stored), 60-minute expiry, identical response whether the email exists (no account enumeration), per-IP rate limit.
- Anonymous cost-sensitive endpoints protected by per-IP rate limiting (Caffeine) and per-user quotas.
- Blog content sanitized server-side with a **jsoup safelist** (plus Angular's default `[innerHTML]` sanitizer — double defense).
- CSRF disabled (stateless JWT architecture).
- HTTPS enforced via Caddy with automatic Let's Encrypt certificates.

## SEO

SEO is a first-class, permanent requirement of the project. The frontend is optimized so the public pages rank for queries such as *"learn chinese by reading"*, *"chinese reads"*, *"read chinese texts"* and *"chinese graded readers"*:

- **Hybrid rendering:** fixed-content pages (home, `/learn`, legal pages…) are prerendered to static HTML at build time; **every page showing database data** (texts and listings, blog, Hall of Fame and member profiles, founder) is **live server-side rendered** by the `frontend-ssr` container via Caddy's `@ssrPages` matcher, so crawlers always see current content. A build script (`frontend/scripts/remove-ssr-prerender.mjs`) deletes the prerendered snapshots of SSR routes — otherwise they would shadow live SSR with frozen empty states (the bug fixed in PR #140).
- **Per-route metadata (`SeoService`):** a single reusable, SSR-safe service sets a unique title, meta description, canonical URL, Open Graph and Twitter Card tags on every navigation (route → metadata table in `seo.config.ts`). Texts, blog posts and Hall of Fame profiles get their title/description from the data itself.
- **Structured data (JSON-LD):** site-wide `WebSite`, `EducationalOrganization` and `WebApplication` schemas, plus per-page `Article`, `BlogPosting`, `Course`/`LearningResource`, `CollectionPage` and `ProfilePage`/`Person` schemas.
- **`robots.txt` + four sitemaps:** the static `sitemap.xml` lists fixed public routes; three **dynamic sitemaps** served by the backend keep texts, blog posts and Hall of Fame profiles indexable without a redeploy. Private routes are `noindex` and disallowed.
- **Performance signals:** `preconnect` hints and a lightweight `favicon.ico` for the tab icon improve Core Web Vitals.
