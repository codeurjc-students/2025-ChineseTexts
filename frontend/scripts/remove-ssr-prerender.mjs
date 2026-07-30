/**
 * Borra del dist los index.html prerenderizados de las rutas que se sirven
 * por SSR REAL (Caddy @ssrPages → frontend-ssr).
 *
 * Por qué: CommonEngine (server.ts) sirve —y cachea en memoria— la página
 * prerenderizada si existe en el dist, en lugar de renderizar en vivo. Como
 * el prerender del build corre sin backend accesible, esas páginas quedaban
 * congeladas con su estado vacío/de error y los listados JAMÁS mostraban
 * datos en producción (bug latente desde el lanzamiento del Hall of Fame,
 * invisible hasta que hubo datos). Sin el fichero, CommonEngine renderiza en
 * vivo en cada petición, que es el comportamiento diseñado.
 *
 * Se ejecuta desde `npm run build` (deploy.sh lo usa). Mantener esta lista en
 * sincronía con el matcher @ssrPages de docker/Caddyfile: al añadir una ruta
 * SSR nueva con página prerenderizable, añadirla aquí.
 */
import { rmSync, existsSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const browserDist = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist', 'frontend', 'browser');

// Rutas del matcher @ssrPages que discoverRoutes prerenderiza (las
// parametrizadas como /blog/:slug o /text/:id no se prerenderizan nunca).
const SSR_ROUTES = ['blog', 'hall-of-fame', 'es/blog', 'es/hall-of-fame'];

let removed = 0;
for (const route of SSR_ROUTES) {
  const file = join(browserDist, route, 'index.html');
  if (existsSync(file)) {
    rmSync(file);
    removed++;
    // El directorio queda vacío salvo que contenga subrutas prerenderizadas.
    const dir = join(browserDist, route);
    if (readdirSync(dir).length === 0) rmSync(dir, { recursive: true });
    console.log(`[remove-ssr-prerender] eliminado ${route}/index.html (la ruta se sirve por SSR en vivo)`);
  } else {
    console.warn(`[remove-ssr-prerender] AVISO: no existía ${route}/index.html (¿ruta renombrada? revisar la lista)`);
  }
}
console.log(`[remove-ssr-prerender] ${removed}/${SSR_ROUTES.length} páginas prerenderizadas retiradas del dist`);
