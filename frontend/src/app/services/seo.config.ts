import { SeoConfig } from './seo.service';

/**
 * Maps each application route to its SEO metadata (title, description, keywords).
 *
 * Keeping this in one table makes the SEO strategy easy to read and extend: to
 * tune how a page ranks or looks in search/social results, edit it here — no
 * component code changes needed. `resolveSeo()` handles both the static routes
 * and the parameterised ones (`/texts/:level`, `/text/:id`).
 */

const HOME: SeoConfig = {
  title: 'ChineseReads — Learn Chinese by Reading Graded HSK Texts',
  description:
    'Learn Chinese by reading graded texts adapted to your HSK level. Free instant pinyin ' +
    'and translations, natural audio pronunciation, and vocabulary flashcards. Start reading Chinese for free.',
  path: '/'
};

const STATIC_SEO: Record<string, SeoConfig> = {
  '/': HOME,

  '/texts': {
    title: 'Read Chinese Texts by HSK Level — Free Graded Readers | ChineseReads',
    description:
      'Browse free graded Chinese reading texts from HSK 1 to HSK 6. Improve your Mandarin ' +
      'reading with short stories, instant pinyin and translations, and audio pronunciation.',
    path: '/texts'
  },

  '/signup': {
    title: 'Create a Free Account — Learn Chinese by Reading | ChineseReads',
    description:
      'Sign up free to unlock vocabulary collections, flashcards, study and exam modes, and ' +
      'save your progress while you learn Chinese by reading graded HSK texts.',
    path: '/signup'
  },

  '/privacy-policy': {
    title: 'Privacy Policy | ChineseReads',
    description:
      'How ChineseReads collects, uses and protects your data, and your rights under the GDPR.',
    path: '/privacy-policy'
  },

  // Authenticated / transactional pages — kept out of the search index.
  '/collections': { title: 'My Collections | ChineseReads', description: 'Your saved Chinese vocabulary collections and flashcards.', path: '/collections', noindex: true },
  '/profile':     { title: 'My Profile | ChineseReads',     description: 'Manage your ChineseReads account.',                      path: '/profile',     noindex: true },
  '/admin-tools': { title: 'Admin Tools | ChineseReads',    description: 'Content management tools for administrators.',           path: '/admin-tools', noindex: true },
  '/ai-tools':    { title: 'AI Tools | ChineseReads',       description: 'AI-assisted Chinese text tools.',                        path: '/ai-tools',    noindex: true },
  '/upload-text': { title: 'Upload Text | ChineseReads',    description: 'Upload a new Chinese text.',                             path: '/upload-text', noindex: true },
  '/success':     { title: 'Success | ChineseReads',        description: 'Operation completed successfully.',                      path: '/success',     noindex: true },
  '/error':       { title: 'Error | ChineseReads',          description: 'Something went wrong.',                                  path: '/error',       noindex: true }
};

const HSK_LEVELS = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];

function hskConfig(level: string): SeoConfig {
  return {
    title: `${level} Chinese Reading Texts — Free Graded Readers | ChineseReads`,
    description:
      `Read ${level} graded Chinese texts with instant pinyin, translations and audio pronunciation. ` +
      `Free Mandarin reading practice for ${level} learners on ChineseReads.`,
    path: `/texts/${level}`
  };
}

/** Fallback for an individual text page before the component sets the real title. */
const TEXT_DEFAULT: SeoConfig = {
  title: 'Read a Chinese Text with Pinyin, Translation & Audio | ChineseReads',
  description:
    'Read a graded Chinese text with word-by-word pinyin, instant translations, sentence ' +
    'breakdown and natural audio pronunciation on ChineseReads.',
  path: '/texts'
};

/**
 * Resolves the SEO config for a given URL path (query string / fragment already
 * stripped by the caller). Unknown routes fall back to the home metadata.
 */
export function resolveSeo(path: string): SeoConfig {
  const clean = path.split('?')[0].split('#')[0] || '/';

  if (STATIC_SEO[clean]) return STATIC_SEO[clean];

  // /texts/:level
  const levelMatch = clean.match(/^\/texts\/([^/]+)$/);
  if (levelMatch) {
    const level = decodeURIComponent(levelMatch[1]).toUpperCase();
    if (HSK_LEVELS.includes(level)) return hskConfig(level);
    return STATIC_SEO['/texts'];
  }

  // /text/:id — the TextComponent refines this with the actual text title.
  if (/^\/text\/[^/]+$/.test(clean)) return TEXT_DEFAULT;

  return HOME;
}
