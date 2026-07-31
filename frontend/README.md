# ChineseReads — Frontend

Angular 17 frontend (standalone components, Transloco i18n, hybrid prerender + live SSR).

- Development server: `npm start` (http://localhost:4200)
- Tests: `npx ng test`
- Production build: `npm run build` — **always use this instead of `ng build`**: it also runs `scripts/remove-ssr-prerender.mjs`, which removes the prerendered snapshots of the live-SSR routes (see `docs/dev-guide.md`).

Full documentation: [docs/dev-guide.md](../docs/dev-guide.md)
