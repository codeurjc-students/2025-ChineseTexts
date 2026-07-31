# Functional Specifications

## User Roles

| Role | Description |
|---|---|
| **Unregistered user** | Can browse and read texts with word/sentence breakdowns, take the level test, use the `/learn` tutorial, and read the blog, Hall of Fame (with member profiles) and founder pages (audio requires a free account) |
| **Registered user** | Can create collections, save words, study and take exams with spaced repetition, track streaks and stats, edit profile, generate private texts, listen to audio, use the AI tutor chat (within the free monthly limits), reset a forgotten password and delete their own account |
| **Admin** | All registered user capabilities plus text management, dictionary word management, AI tools, user management (block/delete/edit, grant premium), and management of the blog, Hall of Fame, influencer codes/settlements and the founder page |

> **PREMIUM is a subscription state, not a separate role.** Any registered user can subscribe (via Stripe) to become premium; internally this is a time-boxed expiry (`User.premiumUntil`), set either by a successful Stripe payment or by an admin grant. While active, it gives the user unlimited monthly text generation (exempt from the global daily cost fuse, bounded only by a high daily fair-use cap). It is deliberately **not** a static role, so it can expire on its own.

## Permissions Table

| Functionality | Unregistered | Registered | Admin |
|---|:---:|:---:|:---:|
| Browse texts by level | ✅ | ✅ | ✅ |
| Read text with word/sentence breakdown | ✅ | ✅ | ✅ |
| Take the HSK level test | ✅ | ✅ | ✅ |
| Use the `/learn` tutorial (pinyin, tones, characters) | ✅ | ✅ | ✅ |
| Read the blog / Hall of Fame + member profiles / founder page | ✅ | ✅ | ✅ |
| Reset a forgotten password (emailed link) | ✅ | ✅ | ✅ |
| Listen to text / word / sentence (audio) | ❌ | ✅ (monthly limit) | ✅ |
| AI tutor chat — ask about a word in context | ❌ | ✅ (10 msgs/month) | ✅ |
| Switch UI language (English / Spanish) | ✅ | ✅ | ✅ |
| Register | ✅ | ✅ | ✅ |
| Login / Logout | ❌ | ✅ | ✅ |
| Edit profile | ❌ | ✅ | ✅ |
| Create collections | ❌ | ✅ | ✅ |
| Save words to collections | ❌ | ✅ | ✅ |
| Listen to flashcard word (audio) | ❌ | ✅ | ✅ |
| Rename / delete collections | ❌ | ✅ | ✅ |
| Study mode | ❌ | ✅ | ✅ |
| Exam mode | ❌ | ✅ | ✅ |
| Spaced-repetition review (SM-2) + daily email reminder (opt-in) | ❌ | ✅ | ✅ |
| Reading streaks + personal statistics | ❌ | ✅ | ✅ |
| Delete own account (cancels any active subscription) | ❌ | ✅ | ✅ |
| Generate a private text (OCR photo / paste) | ❌ | ✅ | ✅ |
| Subscribe to PREMIUM (Stripe) | ❌ | ✅ | ✅ |
| Manage / cancel subscription (Stripe portal) | ❌ | ✅ | ✅ |
| Unlimited generation, audio & AI chat (generation also exempt from the global fuse) | ❌ | Premium only | ✅ (exempt) |
| Manage users: block / unblock, delete, edit | ❌ | ❌ | ✅ |
| Grant / revoke PREMIUM to a user | ❌ | ❌ | ✅ |
| Upload new text | ❌ | ❌ | ✅ |
| Validate text (missing words) | ❌ | ❌ | ✅ |
| Delete text | ❌ | ❌ | ✅ |
| Generate AI text | ❌ | ❌ | ✅ |
| Extract text from image (OCR) | ❌ | ❌ | ✅ |
| Manage dictionary: add word | ❌ | ❌ | ✅ |
| Manage dictionary: edit word | ❌ | ❌ | ✅ |
| Manage dictionary: delete word | ❌ | ❌ | ✅ |
| Write / edit / delete blog posts | ❌ | ❌ | ✅ |
| Manage Hall of Fame members (badges, socials, photos) | ❌ | ❌ | ✅ |
| Manage influencer codes, stats and settlements | ❌ | ❌ | ✅ |
| Edit the founder page in place | ❌ | ❌ | ✅ |

## Data Model

### Entities

- **User** — id, email, name, language, password (bcrypt), roles, blocked, registrationDate, lastAccess, termsAcceptedAt (GDPR consent proof), the monthly usage counters (text generation, word/phrase audio, AI chat) with their period starts, streak fields (currentStreak, bestStreak, lastReadingDay), email-consent pair (emailConsent, emailConsentAt) + lastReviewReminderDay + unsubscribeToken, password-reset pair (passwordResetTokenHash, passwordResetExpiresAt), premiumUntil (premium expiry — single source of truth), stripeCustomerId, stripeSubscriptionId, referral attribution (referralSource, stripePromotionCodeId)
- **Text** — id, titleEnglish, titleSpanish, text (Chinese), englishTranslation, spanishTranslation, englishDescription, spanishDescription, level (HSK1–HSK6), topics, creationDate, image (BLOB); **TextSentence** holds the per-sentence breakdown
- **UserText** — a registered user's own private text generated from a photo (OCR) or pasted text: id, owner (FK), Chinese text, per-sentence translations (**UserTextSentence**) and per-word definitions (**UserTextWord**)
- **Word** — id, chinese, pinyin, english, spanish
- **Collection** — id, title, date, user (FK)
- **Flashcard** — id, collection (FK), word (FK), example/text (FK), plus the SM-2 spaced-repetition state (interval, ease factor, next due date)
- **ReadingLog** — one row per user per reading day; feeds streaks and the personal statistics page
- **AppUsage** — global daily generation counter (day, count) backing the per-day cost fuse
- **BlogPost / BlogImage** — admin-authored bilingual posts (unique slug, sanitized HTML bodies, cover BLOB, draft/published state) and their inline images
- **HallOfFameEntry / HallOfFameSocial** — public influencer showcase: unique slug, bilingual tagline and bio, badges (whitelisted keys), discount code, photo BLOB, ordered social links
- **FounderProfile / FounderSection / FounderItem / FounderSocial** — the DB-backed founder CV page, editable in place by the admin
- **InfluencerPayment** — commission ledger: one row per collected charge attributed to a promotion code (unique invoiceId, plan, amount)

### Entity-Relationship Diagram

![Entity-Relationship Diagram](img/entity-relationship-diagram_v2.drawio.svg)