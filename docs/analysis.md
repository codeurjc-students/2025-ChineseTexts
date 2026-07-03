# Analysis

## Architecture Overview

ChineseReads uses a **distributed MVC architecture** composed of five independent services:

1. **Spring Boot Backend** — REST API, business logic, JWT authentication
2. **Angular Frontend** — SPA with SSR (Angular Universal), SEO-optimized, served as static files by Caddy
3. **DeepSeek AI Microservice** — Python/Flask, text generation and translation
4. **Google OCR Microservice** — Python/Flask, Chinese text extraction from images
5. **Google TTS Microservice** — Python/Flask, text-to-speech audio synthesis of Chinese text

### Cloud Architecture Diagram

![Cloud Architecture](img/architecture_v2.svg)

## API Design

The backend exposes a RESTful API organized around the following resources:

| Resource | Base path | Description |
|---|---|---|
| Texts | `/api/texts` | CRUD for Chinese texts, word/sentence breakdown |
| Words | `/api/words` | Dictionary lookup, plus admin word management (create, update, delete) |
| Users | `/api/users` | Registration, profile management |
| Auth | `/api/auth` | Login, logout, token refresh |
| Collections | `/api/collections` | Collection management, flashcards |
| AI | `/api/ai` | Text generation and OCR processing |
| Audio (TTS) | `/api/tts` | Text-to-speech synthesis of Chinese text (public) |

## Security

- Authentication via **JWT tokens stored in HttpOnly cookies** (not accessible from JavaScript).
- Role-based access control: `ROLE_USER` and `ROLE_ADMIN`.
- Passwords hashed with **BCrypt**.
- CSRF disabled (stateless JWT architecture).
- HTTPS enforced via Caddy with automatic Let's Encrypt certificates.

## SEO

SEO is a first-class, permanent requirement of the project. The frontend is optimized so the public pages rank for queries such as *"learn chinese by reading"*, *"chinese reads"*, *"read chinese texts"* and *"chinese graded readers"*:

- **Server-Side Rendering (Angular Universal):** public pages are rendered to full HTML so crawlers index real content, not an empty `<app-root>`.
- **Per-route metadata (`SeoService`):** a single reusable, SSR-safe service sets a unique title, meta description, canonical URL, Open Graph and Twitter Card tags on every navigation (route → metadata table in `seo.config.ts`). Individual text pages get their own title/description from the text itself.
- **Structured data (JSON-LD):** `WebSite`, `EducationalOrganization` and `WebApplication` schemas in `index.html` enable richer search results.
- **`robots.txt` + `sitemap.xml`:** crawl the public routes, exclude private/authenticated ones.
- **Performance signals:** `preconnect` hints and a lightweight `favicon.ico` for the tab icon improve Core Web Vitals.