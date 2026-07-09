# Follow-up

## Development Progress

| Phase | Description | Status |
|---|---|---|
| Phase 1 | Core architecture, text reading, user auth, collections, flashcards, study/exam modes | ✅ Complete |
|Phase 2 | Project setup, testing files and essential features | ✅ Complete |
| Phase 3 | AI tools (DeepSeek + Google OCR), text-to-speech audio, dictionary word management, SEO optimization, deployment, CI/CD | 🔄 In Progress |
| Phase 4 | Payment gateway (Stripe), premium plan, advanced admin (user management) | ✅ Complete |
| Phase 5 | Personalized statistics, planned AI "ask about words/context" feature | 📋 Planned |

## Recently added (July 2026)

- **PREMIUM subscription plan (Stripe).** Self-service monthly (€3.99) / yearly (€33.50) checkout on Stripe's hosted page (redirect flow, no card data on our servers), signature-verified webhooks that set a time-boxed premium expiry (`premiumUntil`), a Billing Portal to manage/cancel, and a public bilingual, prerendered `/premium` pricing page. Free users have a small monthly generation quota (10) plus a global daily cost fuse; **premium is unlimited monthly and exempt from that fuse** (bounded only by a high daily fair-use cap). See the [Payments & Premium](dev-guide.md#payments--premium-stripe) section.
- **Admin user management + premium grant.** Admins can search users and block/unblock, delete, or edit them, and **grant/revoke premium until a chosen date & time** — all from the users panel. Admins are exempt from the monthly generation quota.
- **Terms of Use + GDPR consent.** A Terms of Use page plus a required consent checkbox at signup, with the acceptance timestamp stored as proof (`termsAcceptedAt`).
- **Bilingual interface (English / Spanish).** The whole UI is now translatable via Transloco, with a working flag switcher in the header. English is served at the root and Spanish under `/es`, keeping SEO intact (per-locale `hreflang`, prerendered `/es` pages, bilingual sitemap). See the [Internationalization](dev-guide.md#internationalization-i18n) section of the Development Guide.
- **Registered-only audio with tiered quotas.** Audio (`/api/tts`, paid Google TTS) now requires a free account — anonymous visitors are prompted to sign up to listen. Free users get a per-user monthly quota split by play type (single words vs sentences/full text, which cost very differently); premium and admins get unlimited audio. On top of that the endpoint keeps the earlier cost protection: a length cap (413), a Caffeine cache keyed by the exact text, and a per-IP rate limit (429) — all configurable.
- **Private-text count in the admin user detail.** The admin user-detail page now shows how many private texts a user has created, alongside the existing collections and flashcards stats.

## Known Limitations

- The SSR prerender during build attempts to call the backend, which may not be running. This produces warnings in the build log but does not affect the deployed application.
- The AI microservice uses the DeepSeek API which may have rate limits or latency depending on usage.
- OCR quality depends on image resolution and clarity.
- The text-to-speech microservice requires the **Cloud Text-to-Speech API** to be enabled in the Google Cloud project; audio uses the WaveNet Mandarin voice and is free up to 1 million characters/month.

## Pending Work

- CI/CD pipeline via GitHub Actions
- Personalized statistics dashboard
- AI "ask about words / context" feature (planned as PREMIUM-only)
- Admin: reset another user's password
- Contact page (Terms of Use and Privacy Policy already implemented)
- Switch Stripe from test mode to live (recreate products/prices/keys/webhook in live and swap the four Stripe values in `docker/.env`)