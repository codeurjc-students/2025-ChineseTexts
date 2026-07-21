import { ApplicationConfig, inject, PLATFORM_ID } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors, HttpInterceptorFn } from '@angular/common/http';
import { isPlatformServer } from '@angular/common';
import { provideTransloco } from '@jsverse/transloco';
import { InlineTranslocoLoader } from './transloco-loader';

const ssrBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  if (isPlatformServer(platformId) && req.url.startsWith('/')) {
    // Relative /api URLs only work in a browser. On the server (SSR container or
    // prerender) they must become absolute: API_BASE_URL points at the backend
    // (http://backend:8080 inside the Docker network); localhost is the dev default.
    // process is read via globalThis so this file typechecks in every build
    // context (browser bundle, SSR bundle, specs) without Node typings.
    const proc = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process;
    const baseUrl = proc?.env?.['API_BASE_URL'] || 'http://localhost:8080';
    const newReq = req.clone({ url: `${baseUrl}${req.url}` });
    return next(newReq);
  }
  return next(req);
};

export const appConfig: ApplicationConfig = {
  providers: [
    // Scroll to the top on every forward navigation (so pages like /signup always
    // start at their title, not wherever the previous page was scrolled), and restore
    // the saved position on browser back/forward.
    provideRouter(routes, withInMemoryScrolling({
      scrollPositionRestoration: 'enabled',
      anchorScrolling: 'enabled',
    })),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptors([ssrBaseUrlInterceptor])
    ),
    provideTransloco({
      config: {
        availableLangs: ['en', 'es'],
        defaultLang: 'en',
        fallbackLang: 'en',
        // Re-render bound templates when the language changes so the header,
        // footer and current page update instantly on flag click.
        reRenderOnLangChange: true,
        prodMode: true,
      },
      loader: InlineTranslocoLoader,
    }),
  ]
};