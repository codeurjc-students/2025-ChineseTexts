# Objectives

## Functional Objectives

The main goal of ChineseReads is to provide a structured and accessible platform for learning Mandarin Chinese through graded texts, vocabulary tools, personalized study features, and AI-assisted utilities.

### Implemented Functionalities

| # | Functionality | Status |
|---|---|---|
| F01 | Display texts organized by HSK difficulty level | ✅ |
| F02 | User registration | ✅ |
| F03 | User login and logout (JWT) | ✅ |
| F04 | Edit profile (name, language, password) | ✅ |
| F05 | Break down texts by words with pinyin and translation | ✅ |
| F06 | Break down texts by sentences with translation | ✅ |
| F07 | Create vocabulary collections | ✅ |
| F08 | Add words to collections (flashcards) | ✅ |
| F09 | Rename and delete collections | ✅ |
| F10 | Study mode (spaced repetition flashcards) | ✅ |
| F11 | Exam mode (multiple choice from collections) | ✅ |
| F12 | Admin: upload new texts with image | ✅ |
| F13 | Admin: validate text before upload (missing words) | ✅ |
| F14 | Admin: generate AI text by HSK level and topic | ✅ |
| F15 | Admin: extract text from image via OCR + AI | ✅ |
| F16 | Admin: delete texts | ✅ |
| F17 | Listen to texts, words, sentences and flashcards (text-to-speech audio) | ✅ |
| F18 | Admin: manage dictionary words (add, edit, delete) | ✅ |
| F19 | Admin: manage users — search, block/unblock, delete, edit name/language/roles | ✅ |
| F20 | Buy a PREMIUM subscription (Stripe Checkout, monthly / yearly) | ✅ |
| F21 | Manage / cancel the subscription (Stripe Billing Portal) | ✅ |
| F22 | Admin: grant / revoke PREMIUM to a user until a chosen date & time | ✅ |
| F23 | Tiered usage limits (free: 10/month + global fuse; premium: unlimited, fuse-exempt, daily fair-use cap) | ✅ |
| F24 | Terms of Use page + required consent at signup (GDPR) | ✅ |

### Pending Functionalities

| # | Functionality |
|---|---|
| P01 | Personalized statistics |
| P02 | Modify another user's password (admin) |
| P03 | AI "ask about words / context" feature (planned as PREMIUM-only) |

---

## Technical Objectives

| # | Objective | Status |
|---|---|---|
| T01 | Distributed architecture with 3+ independent services | ✅ |
| T02 | Containerization with Docker | ✅ |
| T03 | Cloud deployment (Azure VM) | ✅ |
| T04 | REST API communication between services | ✅ |
| T05 | Relational database (MySQL) | ✅ |
| T06 | AI service integration (DeepSeek) | ✅ |
| T07 | OCR image processing (Google Cloud Vision) | ✅ |
| T08 | Modern SPA frontend (Angular 17) | ✅ |
| T09 | SSR + SEO optimization (per-route meta, Open Graph, robots.txt, sitemap.xml, structured data) | ✅ |
| T10 | Secure credential management (.env, secrets) | ✅ |
| T11 | CI/CD pipeline | 🔄 Planned |
| T12 | Text-to-speech synthesis (Google Cloud Text-to-Speech) | ✅ |
| T13 | Payment gateway integration (Stripe subscriptions + signature-verified webhooks) | ✅ |