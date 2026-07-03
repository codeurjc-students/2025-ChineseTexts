# Follow-up

## Development Progress

| Phase | Description | Status |
|---|---|---|
| Phase 1 | Core architecture, text reading, user auth, collections, flashcards, study/exam modes | ✅ Complete |
|Phase 2 | Project setup, testing files and essential features | ✅ Complete |
| Phase 3 | AI tools (DeepSeek + Google OCR), text-to-speech audio, dictionary word management, SEO optimization, deployment, CI/CD | 🔄 In Progress |
| Phase 4 | Statistics, payment gateway, advanced admin features | 📋 Planned |

## Known Limitations

- The SSR prerender during build attempts to call the backend, which may not be running. This produces warnings in the build log but does not affect the deployed application.
- The AI microservice uses the DeepSeek API which may have rate limits or latency depending on usage.
- OCR quality depends on image resolution and clarity.
- The text-to-speech microservice requires the **Cloud Text-to-Speech API** to be enabled in the Google Cloud project; audio uses the WaveNet Mandarin voice and is free up to 1 million characters/month.

## Pending Work

- CI/CD pipeline via GitHub Actions
- Personalized statistics dashboard
- Payment gateway integration
- Admin user management (ban, delete, password reset)
- User Terms of Use and Contact page (Privacy Policy already implemented)