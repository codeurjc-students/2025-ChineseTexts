import { SeoConfig } from './seo.service';
import { Lang } from '../i18n/locale.util';

/**
 * Maps each application route to its SEO metadata (title, description, keywords)
 * in both English and Spanish.
 *
 * Keeping this in one table makes the SEO strategy easy to read and extend: to
 * tune how a page ranks or looks in search/social results, edit it here — no
 * component code changes needed. `resolveSeo()` handles both the static routes
 * and the parameterised ones (`/texts/:level`, `/text/:id`), and returns the
 * variant for the requested language. Paths stay un-prefixed (English); the
 * SeoService adds the `/es` prefix when building the Spanish canonical/alternates.
 */

/** A route's SEO metadata in every supported language. */
type LocalizedSeo = Record<Lang, SeoConfig>;

const HOME: LocalizedSeo = {
  en: {
    title: 'ChineseReads — Learn Chinese by Reading Graded HSK Texts',
    description:
      'Learn Chinese by reading graded texts adapted to your HSK level. Free instant pinyin ' +
      'and translations, natural audio pronunciation, and vocabulary flashcards. Start reading Chinese for free.',
    path: '/'
  },
  es: {
    title: 'ChineseReads — Aprende chino leyendo textos graduados por nivel HSK',
    description:
      'Aprende chino leyendo textos graduados adaptados a tu nivel HSK. Pinyin y traducciones ' +
      'instantáneas gratis, pronunciación con audio natural y tarjetas de vocabulario. Empieza a leer chino gratis.',
    path: '/'
  }
};

const STATIC_SEO: Record<string, LocalizedSeo> = {
  '/': HOME,

  '/texts': {
    en: {
      title: 'Read Chinese Texts by HSK Level — Free Graded Readers | ChineseReads',
      description:
        'Browse free graded Chinese reading texts from HSK 1 to HSK 6. Improve your Mandarin ' +
        'reading with short stories, instant pinyin and translations, and audio pronunciation.',
      path: '/texts'
    },
    es: {
      title: 'Lee textos en chino por nivel HSK — Lecturas graduadas gratis | ChineseReads',
      description:
        'Explora textos de lectura en chino graduados y gratis, de HSK 1 a HSK 6. Mejora tu ' +
        'lectura de mandarín con relatos cortos, pinyin y traducciones instantáneas y pronunciación con audio.',
      path: '/texts'
    }
  },

  '/signup': {
    en: {
      title: 'Create a Free Account — Learn Chinese by Reading | ChineseReads',
      description:
        'Sign up free to unlock vocabulary collections, flashcards, study and exam modes, and ' +
        'save your progress while you learn Chinese by reading graded HSK texts.',
      path: '/signup'
    },
    es: {
      title: 'Crea una cuenta gratis — Aprende chino leyendo | ChineseReads',
      description:
        'Regístrate gratis para desbloquear colecciones de vocabulario, tarjetas, modos de estudio y ' +
        'examen, y guardar tu progreso mientras aprendes chino leyendo textos graduados HSK.',
      path: '/signup'
    }
  },

  '/privacy-policy': {
    en: {
      title: 'Privacy Policy | ChineseReads',
      description:
        'How ChineseReads collects, uses and protects your data, and your rights under the GDPR.',
      path: '/privacy-policy'
    },
    es: {
      title: 'Política de privacidad | ChineseReads',
      description:
        'Cómo ChineseReads recoge, usa y protege tus datos, y tus derechos según el RGPD.',
      path: '/privacy-policy'
    }
  },

  '/terms-of-use': {
    en: {
      title: 'Terms of Use | ChineseReads',
      description:
        'The terms and conditions for using ChineseReads: your account, acceptable use, ' +
        'intellectual property and the limits of our service.',
      path: '/terms-of-use'
    },
    es: {
      title: 'Términos de uso | ChineseReads',
      description:
        'Las condiciones de uso de ChineseReads: tu cuenta, uso aceptable, propiedad ' +
        'intelectual y los límites de nuestro servicio.',
      path: '/terms-of-use'
    }
  },

  '/founder': {
    en: {
      title: 'José Víctor García Llorente — Software Engineer & Creator of ChineseReads',
      description:
        'Software engineer and full-stack developer, creator of ChineseReads. Experience, ' +
        'education, skills and projects. Get in touch on GitHub, LinkedIn or by email.',
      path: '/founder'
    },
    es: {
      title: 'José Víctor García Llorente — Ingeniero de software y creador de ChineseReads',
      description:
        'Ingeniero de software y desarrollador full-stack, creador de ChineseReads. Experiencia, ' +
        'formación, habilidades y proyectos. Contacta por GitHub, LinkedIn o correo electrónico.',
      path: '/founder'
    }
  },

  // Hall of Fame — served by real SSR (frontend-ssr via Caddy, like /text/*);
  // this entry is the base/fallback and the component refines it with live data.
  '/hall-of-fame': {
    en: {
      title: 'Hall of Fame — Creators & Partners of ChineseReads',
      description:
        'Meet the creators, teachers and partners helping people learn Chinese with ChineseReads: ' +
        'who they are, what they do, and their exclusive discount codes.',
      path: '/hall-of-fame'
    },
    es: {
      title: 'Salón de la Fama — Creadores y colaboradores de ChineseReads',
      description:
        'Conoce a los creadores, profesores y colaboradores que ayudan a aprender chino con ' +
        'ChineseReads: quiénes son, qué hacen y sus códigos de descuento exclusivos.',
      path: '/hall-of-fame'
    }
  },

  // Blog — served by real SSR (frontend-ssr via Caddy, like /text/*); this
  // entry is the base/fallback and the list component refines it with live data.
  '/blog': {
    en: {
      title: 'Chinese Learning Blog — Tips, Guides & Stories | ChineseReads',
      description:
        'Articles about learning Chinese: study tips, HSK guides, reading strategies and stories ' +
        'from the ChineseReads community. New posts regularly.',
      path: '/blog'
    },
    es: {
      title: 'Blog para aprender chino — Consejos, guías e historias | ChineseReads',
      description:
        'Artículos sobre aprender chino: consejos de estudio, guías HSK, estrategias de lectura e ' +
        'historias de la comunidad de ChineseReads. Nuevos posts con regularidad.',
      path: '/blog'
    }
  },

  // Premium plan — public marketing page, indexable.
  '/premium': {
    en: {
      title: 'ChineseReads Premium — Unlock Unlimited Chinese Reading Tools',
      description:
        'Upgrade to ChineseReads Premium for unlimited graded readers, unlimited audio and an unlimited ' +
        'AI tutor that explains any word in context. Keep learning Chinese without limits.',
      path: '/premium'
    },
    es: {
      title: 'ChineseReads Premium — Desbloquea las herramientas de lectura sin límites',
      description:
        'Hazte Premium en ChineseReads para lecturas graduadas ilimitadas, audio ilimitado y un tutor IA ' +
        'ilimitado que te explica cualquier palabra en contexto. Sigue aprendiendo chino sin límites.',
      path: '/premium'
    }
  },

  // Free HSK level test — public onboarding + SEO magnet, indexable.
  '/level-test': {
    en: {
      title: 'Free Chinese Level Test (HSK 1–6) — Find Your Level in 3 Minutes',
      description:
        'Take a free, 12-question adaptive Chinese level test and find out your HSK level in about ' +
        '3 minutes — no sign-up needed. Then start reading graded Chinese texts at exactly your level.',
      path: '/level-test'
    },
    es: {
      title: 'Test de nivel de chino gratis (HSK 1–6) — Descubre tu nivel en 3 minutos',
      description:
        'Haz un test de nivel de chino gratuito y adaptativo de 12 preguntas y descubre tu nivel HSK en ' +
        'unos 3 minutos, sin registrarte. Después empieza a leer textos graduados en chino de tu nivel exacto.',
      path: '/level-test'
    }
  },

  // Beginner tutorial (/learn hub + 3 lessons) — public, prerendered SEO pages.
  '/learn': {
    en: {
      title: 'Learn to Read Chinese from Zero — Free Beginner Tutorial | ChineseReads',
      description:
        'Start reading Chinese from absolute zero: understand pinyin, master the 4 Mandarin tones with audio, ' +
        'and read your first 12 Chinese characters. A free 3-lesson tutorial for complete beginners — no sign-up needed.',
      path: '/learn'
    },
    es: {
      title: 'Aprende a leer chino desde cero — Tutorial gratis para principiantes | ChineseReads',
      description:
        'Empieza a leer chino desde cero: entiende el pinyin, domina los 4 tonos del mandarín con audio y lee ' +
        'tus primeros 12 caracteres chinos. Un tutorial gratuito de 3 lecciones para principiantes, sin registro.',
      path: '/learn'
    }
  },

  '/learn/pinyin': {
    en: {
      title: 'What Is Pinyin? Learn Pinyin Basics with Audio (Lesson 1) | ChineseReads',
      description:
        'Learn pinyin from zero: how every Chinese syllable is built from an initial, a final and a tone, with ' +
        'audio examples and the sounds that trip up beginners (x, q, zh, c). Free lesson 1 of 3.',
      path: '/learn/pinyin'
    },
    es: {
      title: '¿Qué es el pinyin? Aprende pinyin desde cero con audio (Lección 1) | ChineseReads',
      description:
        'Aprende pinyin desde cero: cómo cada sílaba china se forma con una inicial, una final y un tono, con ' +
        'ejemplos de audio y los sonidos que más confunden (x, q, zh, c). Lección 1 de 3, gratis.',
      path: '/learn/pinyin'
    }
  },

  '/learn/tones': {
    en: {
      title: 'Chinese Tones Explained — The 4 Mandarin Tones with Audio & Quiz | ChineseReads',
      description:
        'Master the 4 Chinese tones (plus the neutral tone) with the classic “ma” example, audio for every tone ' +
        'and example word, and a free 10-question ear-training quiz. Free lesson 2 of 3.',
      path: '/learn/tones'
    },
    es: {
      title: 'Los tonos del chino — Los 4 tonos del mandarín con audio y quiz | ChineseReads',
      description:
        'Domina los 4 tonos del chino (y el tono neutro) con el ejemplo clásico de «ma», audio de cada tono y ' +
        'palabra de ejemplo, y un quiz gratuito de 10 preguntas para entrenar el oído. Lección 2 de 3, gratis.',
      path: '/learn/tones'
    }
  },

  '/learn/characters': {
    en: {
      title: 'Your First 12 Chinese Characters with Pinyin & Audio (Lesson 3) | ChineseReads',
      description:
        'Read your first 12 HSK1 Chinese characters — 我, 你, 好, 水 and more — each with pinyin, meaning, an ' +
        'example word and audio. Then take the free level test and start reading real texts.',
      path: '/learn/characters'
    },
    es: {
      title: 'Tus primeros 12 caracteres chinos con pinyin y audio (Lección 3) | ChineseReads',
      description:
        'Lee tus primeros 12 caracteres chinos de HSK1 — 我, 你, 好, 水 y más — con pinyin, significado, palabra ' +
        'de ejemplo y audio. Después haz el test de nivel gratis y empieza a leer textos reales.',
      path: '/learn/characters'
    }
  },

  // Authenticated / transactional pages — kept out of the search index.
  '/premium/success': localizedPrivate('Welcome to Premium', 'Your ChineseReads Premium subscription is active.', 'Bienvenido a Premium', 'Tu suscripción Premium de ChineseReads está activa.', '/premium/success'),
  '/collections': localizedPrivate('My Collections', 'Your saved Chinese vocabulary collections and flashcards.', 'Mis colecciones', 'Tus colecciones de vocabulario chino y tarjetas guardadas.', '/collections'),
  '/profile':     localizedPrivate('My Profile', 'Manage your ChineseReads account.', 'Mi perfil', 'Gestiona tu cuenta de ChineseReads.', '/profile'),
  '/admin-tools': localizedPrivate('Admin Tools', 'Content management tools for administrators.', 'Herramientas de administración', 'Herramientas de gestión de contenido para administradores.', '/admin-tools'),
  '/blog-editor': localizedPrivate('Blog Editor', 'Write and edit blog posts.', 'Editor del blog', 'Escribe y edita los posts del blog.', '/blog-editor'),
  '/my-tools':    localizedPrivate('My Tools', 'Turn any Chinese text or photo into your own private graded reader.', 'Mis herramientas', 'Convierte cualquier texto o foto en chino en tu propia lectura graduada privada.', '/my-tools'),
  '/my-text':     localizedPrivate('My Text', 'Read your private Chinese text.', 'Mi texto', 'Lee tu texto privado en chino.', '/my-text'),
  '/ai-tools':    localizedPrivate('AI Tools', 'AI-assisted Chinese text tools.', 'Herramientas de IA', 'Herramientas de texto en chino asistidas por IA.', '/ai-tools'),
  '/upload-text': localizedPrivate('Upload Text', 'Upload a new Chinese text.', 'Subir texto', 'Sube un nuevo texto en chino.', '/upload-text'),
  '/success':     localizedPrivate('Success', 'Operation completed successfully.', 'Éxito', 'Operación completada correctamente.', '/success'),
  '/error':       localizedPrivate('Error', 'Something went wrong.', 'Error', 'Algo ha salido mal.', '/error')
};

/** Builds a noindex LocalizedSeo entry for a private/transactional page. */
function localizedPrivate(enTitle: string, enDesc: string, esTitle: string, esDesc: string, path: string): LocalizedSeo {
  return {
    en: { title: `${enTitle} | ChineseReads`, description: enDesc, path, noindex: true },
    es: { title: `${esTitle} | ChineseReads`, description: esDesc, path, noindex: true },
  };
}

/**
 * Unknown routes (the `**` wildcard → NotFoundComponent). The static hosting
 * always answers 200, so this is a "soft 404": noindex keeps the bad URL out
 * of the index and the canonical points at the URL itself (never at a real
 * page, which would wrongly consolidate garbage URLs into it).
 */
const NOT_FOUND: LocalizedSeo = {
  en: {
    title: 'Page Not Found | ChineseReads',
    description:
      'This page does not exist or has been moved. Browse free graded Chinese ' +
      'reading texts by HSK level on ChineseReads.',
    noindex: true
  },
  es: {
    title: 'Página no encontrada | ChineseReads',
    description:
      'Esta página no existe o ha cambiado de dirección. Explora textos de lectura ' +
      'en chino graduados por nivel HSK en ChineseReads.',
    noindex: true
  }
};

const HSK_LEVELS = ['HSK1', 'HSK2', 'HSK3', 'HSK4', 'HSK5', 'HSK6'];

function hskConfig(level: string, lang: Lang): SeoConfig {
  if (lang === 'es') {
    return {
      title: `Textos de lectura en chino ${level} — Lecturas graduadas gratis | ChineseReads`,
      description:
        `Lee textos en chino graduados de nivel ${level} con pinyin, traducciones y pronunciación con audio al instante. ` +
        `Práctica de lectura de mandarín gratis para estudiantes de ${level} en ChineseReads.`,
      path: `/texts/${level}`
    };
  }
  return {
    title: `${level} Chinese Reading Texts — Free Graded Readers | ChineseReads`,
    description:
      `Read ${level} graded Chinese texts with instant pinyin, translations and audio pronunciation. ` +
      `Free Mandarin reading practice for ${level} learners on ChineseReads.`,
    path: `/texts/${level}`
  };
}

/**
 * Fallback for an individual text page before the component sets the real title.
 * No `path` here: resolveSeo() fills in the page's own /text/:id path so the
 * canonical is self-referential from the first render (a canonical pointing at
 * the /texts listing would risk Google folding every text into the listing).
 */
const TEXT_DEFAULT: LocalizedSeo = {
  en: {
    title: 'Read a Chinese Text with Pinyin, Translation & Audio | ChineseReads',
    description:
      'Read a graded Chinese text with word-by-word pinyin, instant translations, sentence ' +
      'breakdown and natural audio pronunciation on ChineseReads.'
  },
  es: {
    title: 'Lee un texto en chino con pinyin, traducción y audio | ChineseReads',
    description:
      'Lee un texto en chino graduado con pinyin palabra por palabra, traducciones instantáneas, ' +
      'desglose de frases y pronunciación con audio natural en ChineseReads.'
  }
};

/**
 * Fallback for an individual blog post before the component sets the real title.
 * Same rationale as TEXT_DEFAULT: no `path` here — resolveSeo() fills in the
 * page's own /blog/:slug path so the canonical is self-referential from the
 * first render.
 */
const BLOG_POST_DEFAULT: LocalizedSeo = {
  en: {
    title: 'Blog Post | ChineseReads',
    description:
      'An article about learning Chinese on the ChineseReads blog: study tips, HSK guides, ' +
      'reading strategies and community stories.'
  },
  es: {
    title: 'Post del blog | ChineseReads',
    description:
      'Un artículo sobre aprender chino en el blog de ChineseReads: consejos de estudio, guías ' +
      'HSK, estrategias de lectura e historias de la comunidad.'
  }
};

/**
 * Resolves the SEO config for a given un-prefixed URL path (the caller strips any
 * `/es` prefix and passes the language separately). Query string / fragment
 * already stripped by the caller. Unknown routes resolve to the not-found
 * (soft-404, noindex) metadata, mirroring the router's `**` wildcard.
 */
export function resolveSeo(path: string, lang: Lang = 'en'): SeoConfig {
  const clean = path.split('?')[0].split('#')[0] || '/';

  if (STATIC_SEO[clean]) return STATIC_SEO[clean][lang];

  // /texts/:level
  const levelMatch = clean.match(/^\/texts\/([^/]+)$/);
  if (levelMatch) {
    const level = decodeURIComponent(levelMatch[1]).toUpperCase();
    if (HSK_LEVELS.includes(level)) return hskConfig(level, lang);
    return STATIC_SEO['/texts'][lang];
  }

  // /my-text/:id — private per-user reader, always noindex.
  if (/^\/my-text\/[^/]+$/.test(clean)) return STATIC_SEO['/my-text'][lang];

  // /text/:id — the TextComponent refines this with the actual text title.
  if (/^\/text\/[^/]+$/.test(clean)) return { ...TEXT_DEFAULT[lang], path: clean };

  // /blog/:slug — the BlogPostComponent refines this with the actual post title.
  if (/^\/blog\/[^/]+$/.test(clean)) return { ...BLOG_POST_DEFAULT[lang], path: clean };

  // /blog-editor/:id — private admin editor, always noindex.
  if (/^\/blog-editor\/[^/]+$/.test(clean)) return STATIC_SEO['/blog-editor'][lang];

  return { ...NOT_FOUND[lang], path: clean };
}
