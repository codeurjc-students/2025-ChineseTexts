import { inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs/operators';
import { TranslocoService } from '@jsverse/transloco';
import { SeoService } from '../services/seo.service';
import { Lang } from '../i18n/locale.util';

/**
 * Wires page-specific JSON-LD structured data for a static routed component.
 * Call it from the component's CONSTRUCTOR (it relies on the injection context).
 *
 * Why not ngOnInit: AppComponent's NavigationEnd handler calls seo.update(),
 * which clears any per-page JSON-LD — and ngOnInit runs during route
 * activation, i.e. BEFORE NavigationEnd, so data set there would be wiped.
 * Subscribing here works because AppComponent subscribed to router.events
 * first (at bootstrap): on each NavigationEnd its update() runs before this
 * handler, so the JSON-LD set here survives — during prerender too. Language
 * switches (`/learn` ↔ `/es/learn`) recreate the component, so the wiring
 * re-runs naturally; the builder receives the language active at that moment.
 */
export function wirePageJsonLd(build: (lang: Lang) => object): void {
  const seo = inject(SeoService);
  const transloco = inject(TranslocoService);
  inject(Router).events
    .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd), takeUntilDestroyed())
    .subscribe(() => seo.setPageJsonLd(build(transloco.getActiveLang() as Lang)));
}
