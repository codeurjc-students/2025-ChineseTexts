# Follow-up

## Development Progress

| Phase | Description | Status |
|---|---|---|
| Phase 1 | Core architecture, text reading, user auth, collections, flashcards, study/exam modes | ✅ Complete |
|Phase 2 | Project setup, testing files and essential features | ✅ Complete |
| Phase 3 | AI tools (DeepSeek + Google OCR), text-to-speech audio, dictionary word management, SEO optimization, deployment | ✅ Complete |
| Phase 4 | Payment gateway (Stripe), premium plan, advanced admin (user management), AI "ask about words/context" tutor chat | ✅ Complete |
| Phase 5 | Retention & growth: streaks + personal statistics, SRS review, level test, `/learn` tutorial, transactional email, blog, Hall of Fame + influencer campaign tooling, password reset | ✅ Complete |
| Phase 6 | CI/CD pipeline | 📋 Planned |

## Recently added (July 2026)

- **PREMIUM subscription plan (Stripe).** Self-service monthly (€6.99) / yearly (€59.99) checkout on Stripe's hosted page (redirect flow, no card data on our servers), signature-verified webhooks that set a time-boxed premium expiry (`premiumUntil`), a Billing Portal to manage/cancel, and a public bilingual, prerendered `/premium` pricing page. Free users have a small monthly generation quota (10) plus a global daily cost fuse; **premium is unlimited monthly and exempt from that fuse** (bounded only by a high daily fair-use cap). See the [Payments & Premium](dev-guide.md#payments--premium-stripe) section.
- **Admin user management + premium grant.** Admins can search users and block/unblock, delete, or edit them, and **grant/revoke premium until a chosen date & time** — all from the users panel. Admins are exempt from the monthly generation quota.
- **Terms of Use + GDPR consent.** A Terms of Use page plus a required consent checkbox at signup, with the acceptance timestamp stored as proof (`termsAcceptedAt`).
- **Bilingual interface (English / Spanish).** The whole UI is now translatable via Transloco, with a working flag switcher in the header. English is served at the root and Spanish under `/es`, keeping SEO intact (per-locale `hreflang`, prerendered `/es` pages, bilingual sitemap). See the [Internationalization](dev-guide.md#internationalization-i18n) section of the Development Guide.
- **Registered-only audio with tiered quotas.** Audio (`/api/tts`, paid Google TTS) now requires a free account — anonymous visitors are prompted to sign up to listen. Free users get a per-user monthly quota split by play type (single words vs sentences/full text, which cost very differently); premium and admins get unlimited audio. On top of that the endpoint keeps the earlier cost protection: a length cap (413), a Caffeine cache keyed by the exact text, and a per-IP rate limit (429) — all configurable.
- **Private-text count in the admin user detail.** The admin user-detail page now shows how many private texts a user has created, alongside the existing collections and flashcards stats.
- **AI contextual word chat.** In both readers, clicking a word opens a guardrailed AI tutor chat about that word in context (its sentence, the full text, the translation, the HSK level and the user's language). Registered-only (paid DeepSeek per message): free users get 10 messages/month shown as a dwindling counter, premium and admins are unlimited. The conversation is ephemeral (re-sent each turn).
- **In-context selling.** Free-vs-Premium pricing cards on the home page, a monthly generation usage meter on the My Tools page, and upgrade nudges at every limit wall.

## Recently added (late July 2026)

- **Streaks + personal statistics.** A reading log feeds daily streaks (current/best) and a personal stats page; flashcards now use real **SM-2 spaced repetition** with due-card scheduling.
- **HSK level test + `/learn` tutorial.** A free 12-question level test and a learn-to-read hub (pinyin, tones, characters) with static mp3 audio — fully prerendered, the project's main pure-SEO asset.
- **Transactional email (Brevo API).** Welcome email at signup, an opt-in daily SRS review reminder (max one/day, one-click unsubscribe + `List-Unsubscribe` header) and the password-reset email. No SMTP server to run.
- **Blog.** Admin-authored bilingual posts with a Quill rich-text editor (lazy-loaded, admin-only), server-side jsoup sanitization, covers/drafts, `BlogPosting` JSON-LD and a dynamic sitemap — posts are indexable without a redeploy.
- **Hall of Fame + influencer campaign tooling.** Public showcase of collaborators (badges, socials, discount codes) with per-member profile pages (`/hall-of-fame/:slug`, `ProfilePage`/`Person` JSON-LD, own dynamic sitemap); Stripe promotion codes created from an admin panel with conversion attribution and commission settlements.
- **Live SSR for every DB-backed page (PR #140/#141).** Texts and listings, blog, Hall of Fame and founder are rendered live by the `frontend-ssr` container; a build script removes their prerendered snapshots, which used to shadow live SSR with frozen empty states.
- **Self-service password reset.** `/forgot-password` + `/reset-password`: emailed single-use token (SHA-256-hashed at rest), 60-minute expiry, anti-enumeration responses, per-IP rate limit.
- **Cookie-free analytics.** Google Analytics and the cookie banner were removed; Umami (anonymous aggregates) is the only analytics.
- **Legal pages refreshed.** The Privacy Policy now covers the full data inventory, legal bases, international transfers and the current email set; Terms describe the real feature set and payment conditions.

## Known Limitations

- The SSR prerender during build runs without a backend, so DB-backed pages would be baked as empty snapshots; `npm run build` therefore removes those snapshots (`frontend/scripts/remove-ssr-prerender.mjs`) so they never shadow the live SSR (bug fixed in PR #140). "fetch failed" warnings in the build log are expected and harmless.
- The AI microservice uses the DeepSeek API which may have rate limits or latency depending on usage.
- OCR quality depends on image resolution and clarity.
- The text-to-speech microservice requires the **Cloud Text-to-Speech API** to be enabled in the Google Cloud project; audio uses the WaveNet Mandarin voice and is free up to 1 million characters/month.

## Pending Work

- CI/CD pipeline via GitHub Actions
- Angular 17 → 20 upgrade (clears most remaining `npm audit` advisories)
- Admin: reset another user's password (largely superseded by the self-service reset)
- Contact page (Terms of Use and Privacy Policy already implemented)