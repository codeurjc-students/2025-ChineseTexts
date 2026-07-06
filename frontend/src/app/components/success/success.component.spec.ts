import { TestBed } from '@angular/core/testing';
import { SuccessComponent } from './success.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('SuccessComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), SuccessComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(SuccessComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });
});