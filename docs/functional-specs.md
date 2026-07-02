# Functional Specifications

## User Roles

| Role | Description |
|---|---|
| **Unregistered user** | Can browse and read texts, see word/sentence breakdowns, and listen to text/word/sentence audio |
| **Registered user** | Can create collections, save words, study and take exams, edit profile |
| **Admin** | All registered user capabilities plus text management and AI tools |

## Permissions Table

| Functionality | Unregistered | Registered | Admin |
|---|:---:|:---:|:---:|
| Browse texts by level | ✅ | ✅ | ✅ |
| Read text with word/sentence breakdown | ✅ | ✅ | ✅ |
| Listen to text / word / sentence (audio) | ✅ | ✅ | ✅ |
| Register | ✅ | ✅ | ✅ |
| Login / Logout | ❌ | ✅ | ✅ |
| Edit profile | ❌ | ✅ | ✅ |
| Create collections | ❌ | ✅ | ✅ |
| Save words to collections | ❌ | ✅ | ✅ |
| Listen to flashcard word (audio) | ❌ | ✅ | ✅ |
| Rename / delete collections | ❌ | ✅ | ✅ |
| Study mode | ❌ | ✅ | ✅ |
| Exam mode | ❌ | ✅ | ✅ |
| Upload new text | ❌ | ❌ | ✅ |
| Validate text (missing words) | ❌ | ❌ | ✅ |
| Delete text | ❌ | ❌ | ✅ |
| Generate AI text | ❌ | ❌ | ✅ |
| Extract text from image (OCR) | ❌ | ❌ | ✅ |

## Data Model

### Entities

- **User** — id, email, name, language, password (bcrypt), roles
- **Text** — id, titleEnglish, titleSpanish, text (Chinese), englishTranslation, spanishTranslation, englishDescription, spanishDescription, level (HSK1–HSK6), creationDate, image (BLOB)
- **Word** — id, chinese, pinyin, english, spanish
- **Collection** — id, title, date, user (FK)
- **Flashcard** — id, collection (FK), word (FK), example/text (FK)

### Entity-Relationship Diagram

![Entity-Relationship Diagram](img/entity-relationship-diagram_v2.drawio.svg)