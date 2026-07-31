import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';

import { ForgotPasswordComponent } from './forgot-password.component';
import { LoginService } from '../../services/login.service';
import { translocoTesting } from '../../i18n/transloco-testing';

@Component({ template: '' })
class DummyComponent {}

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;

  beforeEach(async () => {
    loginServiceSpy = jasmine.createSpyObj('LoginService', ['forgotPassword']);

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), ForgotPasswordComponent],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '', component: DummyComponent }])
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Test 1: un email mal formado no llama al backend y muestra el error local
  it('rejects a malformed email locally without calling the backend', () => {
    component.email = 'esto-no-es-un-email';
    component.submit();
    expect(component.emailError).toBeTruthy();
    expect(loginServiceSpy.forgotPassword).not.toHaveBeenCalled();
  });

  // Test 2: con email válido llama al backend y muestra la confirmación neutra
  it('sends the request and shows the neutral confirmation on success', () => {
    loginServiceSpy.forgotPassword.and.returnValue(of({}));
    component.email = '  ana@test.com ';
    component.submit();
    expect(loginServiceSpy.forgotPassword).toHaveBeenCalledWith('ana@test.com');
    expect(component.sent).toBeTrue();
    expect(component.errorMessage).toBe('');
  });

  // Test 3: el 429 del rate limit muestra su mensaje específico
  it('shows the rate-limit message on a 429 response', () => {
    loginServiceSpy.forgotPassword.and.returnValue(throwError(() => ({ status: 429 })));
    component.email = 'ana@test.com';
    component.submit();
    expect(component.sent).toBeFalse();
    expect(component.errorMessage).toContain('Too many requests');
  });
});
