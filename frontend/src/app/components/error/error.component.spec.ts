import { TestBed } from '@angular/core/testing';
import { ErrorComponent } from './error.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('ErrorComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), ErrorComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(ErrorComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });
});