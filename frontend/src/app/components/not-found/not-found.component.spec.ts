import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { NotFoundComponent } from './not-found.component';
import { routes } from '../../app.routes';
import { translocoTesting } from '../../i18n/transloco-testing';

describe('NotFoundComponent', () => {

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), NotFoundComponent],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
  });

  it('should create and render the 404 content', () => {
    const fixture = TestBed.createComponent(NotFoundComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.code')?.textContent).toContain('404');
    expect(el.textContent).toContain('Page not found');
  });

  it('is reached by the root wildcard route for an unknown URL', async () => {
    const harness = await RouterTestingHarness.create('/rutanoexistente');
    expect(harness.routeDebugElement?.componentInstance).toBeInstanceOf(NotFoundComponent);
  });

  it('is reached by the /es wildcard route for an unknown Spanish URL', async () => {
    const harness = await RouterTestingHarness.create('/es/rutanoexistente');
    expect(harness.routeDebugElement?.componentInstance).toBeInstanceOf(NotFoundComponent);
  });

  it('does NOT swallow real routes (the wildcard is last)', async () => {
    const harness = await RouterTestingHarness.create('/signup');
    expect(harness.routeDebugElement?.componentInstance).not.toBeInstanceOf(NotFoundComponent);
  });

  it('navigates home when the primary button is clicked', () => {
    const fixture = TestBed.createComponent(NotFoundComponent);
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    const spy = spyOn(router, 'navigate');
    (fixture.nativeElement.querySelector('.btn-main') as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalledWith(['/']);
  });
});
