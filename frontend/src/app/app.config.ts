import { ApplicationConfig, inject, PLATFORM_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors, HttpInterceptorFn } from '@angular/common/http';
import { isPlatformServer } from '@angular/common';

const ssrBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  if (isPlatformServer(platformId) && req.url.startsWith('/')) {
    const baseUrl = 'http://localhost:8080';
    const newReq = req.clone({ url: `${baseUrl}${req.url}` });
    return next(newReq);
  }
  return next(req);
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptors([ssrBaseUrlInterceptor])
    ),
  ]
};