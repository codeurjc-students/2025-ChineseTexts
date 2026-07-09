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

  // Premium plan — public marketing page, indexable.
  '/premium': {
    en: {
      title: 'ChineseReads Premium — Unlock Unlimited Chinese Reading Tools',
      description:
        'Upgrade to ChineseReads Premium for unlimited private graded readers, never blocked by the ' +
        'shared daily limit, plus early access to upcoming AI features. Keep learning Chinese without limits.',
      path: '/premium'
    },
    es: {
      title: 'ChineseReads Premium — Desbloquea las herramientas de lectura sin límites',
      description:
        'Hazte Premium en ChineseReads para crear lecturas graduadas privadas ilimitadas, que nunca se ' +
        'bloquean por el límite diario compartido, y accede antes a las próximas funciones de IA. Sigue aprendiendo chino sin límites.',
      path: '/premium'
    }
  },

  // Authenticated / transactional pages — kept out of the search index.
  '/premium/success': localizedPrivate('Welcome to Premium', 'Your ChineseReads Premium subscription is active.', 'Bienvenido a Premium', 'Tu suscripción Premium de ChineseReads está activa.', '/premium/success'),
  '/collections': localizedPrivate('My Collections', 'Your saved Chinese vocabulary collections and flashcards.', 'Mis colecciones', 'Tus colecciones de vocabulario chino y tarjetas guardadas.', '/collections'),
  '/profile':     localizedPrivate('My Profile', 'Manage your ChineseReads account.', 'Mi perfil', 'Gestiona tu cuenta de ChineseReads.', '/profile'),
  '/admin-tools': localizedPrivate('Admin Tools', 'Content management tools for administrators.', 'Herramientas de administración', 'Herramientas de gestión de contenido para administradores.', '/admin-tools'),
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

/** Fallback for an individual text page before the component sets the real title. */
const TEXT_DEFAULT: LocalizedSeo = {
  en: {
    title: 'Read a Chinese Text with Pinyin, Translation & Audio | ChineseReads',
    description:
      'Read a graded Chinese text with word-by-word pinyin, instant translations, sentence ' +
      'breakdown and natural audio pronunciation on ChineseReads.',
    path: '/texts'
  },
  es: {
    title: 'Lee un texto en chino con pinyin, traducción y audio | ChineseReads',
    description:
      'Lee un texto en chino graduado con pinyin palabra por palabra, traducciones instantáneas, ' +
      'desglose de frases y pronunciación con audio natural en ChineseReads.',
    path: '/texts'
  }
};

/**
 * Resolves the SEO config for a given un-prefixed URL path (the caller strips any
 * `/es` prefix and passes the language separately). Query string / fragment
 * already stripped by the caller. Unknown routes fall back to the home metadata.
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
  if (/^\/text\/[^/]+$/.test(clean)) return TEXT_DEFAULT[lang];

  return HOME[lang];
}
