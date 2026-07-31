import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';

import { ResetPasswordComponent } from './reset-password.component';
import { LoginService } from '../../services/login.service';
import { AuthUiService } from '../../services/auth-ui.service';
import { translocoTesting } from '../../i18n/transloco-testing';

@Component({ template: '' })
class DummyComponent {}

/** Crea el componente con un ?token=… dado (o sin él). */
function setup(token: string | null) {
  const loginServiceSpy = jasmine.createSpyObj<LoginService>('LoginService', ['resetPassword']);
  const queryParamMap = of({ get: (_: string) => token } as any);

  TestBed.configureTestingModule({
    imports: [translocoTesting(), ResetPasswordComponent],
    providers: [
      { provide: LoginService, useValue: loginServiceSpy },
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([{ path: '', component: DummyComponent }]),
      // Después de provideRouter a propósito: el último provider gana y así el
      // mock del query param no queda pisado por el ActivatedRoute del router.
      { provide: ActivatedRoute, useValue: { queryParamMap } }
    ],
    schemas: [NO_ERRORS_SCHEMA]
  });

  const fixture: ComponentFixture<ResetPasswordComponent> =
    TestBed.createComponent(ResetPasswordComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance, loginServiceSpy };
}

describe('ResetPasswordComponent', () => {

  // Test 1: sin token en la URL se muestra el estado de enlace inválido
  it('shows the invalid-link state when the URL has no token', () => {
    const { component } = setup(null);
    expect(component.invalidLink).toBeTrue();
  });

  // Test 2: contraseña corta o no coincidente se rechaza localmente
  it('rejects short or mismatched passwords locally without calling the backend', () => {
    const { component, loginServiceSpy } = setup('tok-1');
    component.newPassword = 'abc';
    component.submit();
    expect(component.passwordError).toBeTruthy();

    component.newPassword = 'clave-123';
    component.confirmPassword = 'otra-123';
    component.submit();
    expect(component.passwordError).toBeTruthy();
    expect(loginServiceSpy.resetPassword).not.toHaveBeenCalled();
  });

  // Test 3: con datos válidos llama al backend y muestra el éxito
  it('resets the password and shows the success state', () => {
    const { component, loginServiceSpy } = setup('tok-1');
    loginServiceSpy.resetPassword.and.returnValue(of({}));
    component.newPassword = 'clave-123';
    component.confirmPassword = 'clave-123';
    component.submit();
    expect(loginServiceSpy.resetPassword).toHaveBeenCalledWith('tok-1', 'clave-123');
    expect(component.done).toBeTrue();
  });

  // Test 4: el código INVALID_OR_EXPIRED_TOKEN del backend lleva al estado inválido
  it('switches to the invalid-link state when the backend rejects the token', () => {
    const { component, loginServiceSpy } = setup('tok-consumido');
    loginServiceSpy.resetPassword.and.returnValue(
      throwError(() => ({ status: 400, error: { code: 'INVALID_OR_EXPIRED_TOKEN' } })));
    component.newPassword = 'clave-123';
    component.confirmPassword = 'clave-123';
    component.submit();
    expect(component.done).toBeFalse();
    expect(component.invalidLink).toBeTrue();
  });

  // Test 5: tras el éxito, "Iniciar sesión" navega a la home y abre el modal de login
  it('goToLogin opens the header login modal after success', () => {
    const { component } = setup('tok-1');
    const authUi = TestBed.inject(AuthUiService);
    const openSpy = spyOn(authUi, 'openLogin');
    component.goToLogin();
    expect(openSpy).toHaveBeenCalled();
  });
});
