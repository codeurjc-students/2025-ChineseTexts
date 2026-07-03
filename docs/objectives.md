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

### Pending Functionalities

| # | Functionality |
|---|---|
| P01 | Payment gateway for premium registration |
| P02 | Personalized statistics |
| P03 | Modify another user's password (admin) |
| P04 | Ban/delete users (admin) |

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