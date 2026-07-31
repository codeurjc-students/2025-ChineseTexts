import { addLangPrefix, langFromUrl, stripLangPrefix } from './locale.util';

/**
 * Candados del esquema de prefijo de idioma. El caso estrella es el de la home
 * con query string (enlaces de campaña "?ref=CODIGO"): cambiar a español desde
 * "/?ref=X" debe dar "/es?ref=X" — "/es/?ref=X" no casa con ninguna ruta y
 * producía un 404 en la primera visita del seguidor (bug del 31-07).
 */
describe('locale.util', () => {

  describe('addLangPrefix', () => {
    it('keeps English URLs untouched', () => {
      expect(addLangPrefix('/', 'en')).toBe('/');
      expect(addLangPrefix('/texts', 'en')).toBe('/texts');
      expect(addLangPrefix('/?ref=MARIA30', 'en')).toBe('/?ref=MARIA30');
    });

    it('prefixes normal paths for Spanish', () => {
      expect(addLangPrefix('/', 'es')).toBe('/es');
      expect(addLangPrefix('/texts', 'es')).toBe('/es/texts');
      expect(addLangPrefix('/texts?page=2', 'es')).toBe('/es/texts?page=2');
    });

    // Regresión: home + query (enlace de campaña) al cambiar a español
    it('handles the home with a query string without the extra slash (campaign links)', () => {
      expect(addLangPrefix('/?ref=MARIA30', 'es')).toBe('/es?ref=MARIA30');
      expect(addLangPrefix('/#section', 'es')).toBe('/es#section');
    });
  });

  describe('stripLangPrefix', () => {
    it('strips the /es prefix in all its forms', () => {
      expect(stripLangPrefix('/es')).toBe('/');
      expect(stripLangPrefix('/es/texts')).toBe('/texts');
      expect(stripLangPrefix('/es?ref=MARIA30')).toBe('/?ref=MARIA30');
      expect(stripLangPrefix('/texts')).toBe('/texts');
    });
  });

  describe('round trip', () => {
    // El ciclo completo del conmutador de idioma: strip + add en ambos sentidos.
    it('switching to Spanish and back preserves the URL exactly', () => {
      for (const url of ['/', '/?ref=MARIA30', '/texts', '/texts?page=2', '/text/59']) {
        const es = addLangPrefix(stripLangPrefix(url), 'es');
        expect(addLangPrefix(stripLangPrefix(es), 'en')).toBe(url);
      }
    });
  });

  describe('langFromUrl', () => {
    it('derives the language from the URL prefix only', () => {
      expect(langFromUrl('/es?ref=MARIA30')).toBe('es');
      expect(langFromUrl('/es/texts')).toBe('es');
      expect(langFromUrl('/?ref=MARIA30')).toBe('en');
    });
  });
});
