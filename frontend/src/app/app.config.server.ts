import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering } from '@angular/platform-server';
import { appConfig } from './app.config';

// No extra provideHttpClient here: appConfig's one (with ssrBaseUrlInterceptor)
// must stay in charge — re-providing HttpClient would drop the interceptor that
// makes relative /api URLs absolute during server-side rendering.
const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(),
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);