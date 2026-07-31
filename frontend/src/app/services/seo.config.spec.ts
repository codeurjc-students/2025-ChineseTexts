import { resolveSeo } from './seo.config';

describe('resolveSeo', () => {

  it('resolves known static routes normally', () => {
    expect(resolveSeo('/texts', 'en').title).toContain('HSK');
    expect(resolveSeo('/', 'en').noindex).toBeUndefined();
  });

  it('returns noindex soft-404 metadata for unknown routes', () => {
    const en = resolveSeo('/rutanoexistente', 'en');
    expect(en.noindex).toBeTrue();
    expect(en.title).toContain('Page Not Found');

    const es = resolveSeo('/rutanoexistente', 'es');
    expect(es.noindex).toBeTrue();
    expect(es.title).toContain('Página no encontrada');
  });

  it('keeps the canonical of an unknown route self-referential (never a real page)', () => {
    expect(resolveSeo('/rutanoexistente', 'en').path).toBe('/rutanoexistente');
  });

  it('still falls back to the texts listing for unknown HSK levels', () => {
    expect(resolveSeo('/texts/HSK9', 'en').path).toBe('/texts');
  });

  it('resolves /blog statically and /blog/:slug with a self-referential indexable fallback', () => {
    expect(resolveSeo('/blog', 'en').title).toContain('Blog');
    expect(resolveSeo('/blog', 'en').noindex).toBeUndefined();

    const post = resolveSeo('/blog/my-first-post', 'en');
    expect(post.noindex).toBeUndefined();
    expect(post.path).toBe('/blog/my-first-post');
  });

  it('keeps the blog editor noindex in both forms (/blog-editor and /blog-editor/:id)', () => {
    expect(resolveSeo('/blog-editor', 'en').noindex).toBeTrue();
    expect(resolveSeo('/blog-editor/5', 'es').noindex).toBeTrue();
  });

  // Candado: las páginas del flujo de reset son transaccionales → noindex, y el
  // ?token=… del enlace del email no cambia la resolución (la query se descarta).
  it('keeps the password-reset pages noindex, with or without the token query', () => {
    expect(resolveSeo('/forgot-password', 'en').noindex).toBeTrue();
    expect(resolveSeo('/forgot-password', 'es').noindex).toBeTrue();
    expect(resolveSeo('/reset-password?token=abc123', 'en').noindex).toBeTrue();
    expect(resolveSeo('/reset-password?token=abc123', 'es').title).toContain('Nueva contraseña');
  });
});
