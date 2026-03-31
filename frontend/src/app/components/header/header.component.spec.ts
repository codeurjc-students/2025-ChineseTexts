import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError, BehaviorSubject, EMPTY } from 'rxjs';
import { Router } from '@angular/router';

import { HeaderComponent } from './header.component';
import { LoginService } from '../../services/login.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let router: Router;

  beforeEach(async () => {
    loginServiceSpy = jasmine.createSpyObj('LoginService',
      ['isLogged', 'isRoleAdmin', 'isRoleUser', 'login', 'logout', 'reqIsLogged'],
      { loggedIn$: new BehaviorSubject<boolean>(false).asObservable() }
    );
    loginServiceSpy.isLogged.and.returnValue(false);
    loginServiceSpy.isRoleAdmin.and.returnValue(false);
    loginServiceSpy.isRoleUser.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  // Test 1: El componente se crea correctamente
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Test 2: Login no se llama si los campos están vacíos
  it('should show error and not call login when fields are empty', () => {
    component.loginEmail = '';
    component.loginPassword = '';
    component.login();

    expect(loginServiceSpy.login).not.toHaveBeenCalled();
    expect(component.emailError).toBe('Email is required.');
    expect(component.passwordError).toBe('Password is required.');
  });

  // Test 3: Login exitoso llama al servicio con credenciales correctas
  it('should call login service with correct credentials on successful login', () => {
    loginServiceSpy.login.and.returnValue(EMPTY);

    component.loginEmail = 'test@test.com';
    component.loginPassword = 'password123';
    component.login();

    expect(loginServiceSpy.login).toHaveBeenCalledWith('test@test.com', 'password123');
  });

  // Test 4: Login fallido muestra mensaje de error y limpia los campos
  it('should show error message and clear fields when login fails', () => {
    loginServiceSpy.login.and.returnValue(throwError(() => new Error('Unauthorized')));

    component.loginEmail = 'wrong@test.com';
    component.loginPassword = 'wrongpass';
    component.login();

    expect(component.loginEmail).toBe('');
    expect(component.loginPassword).toBe('');
    expect(component.messageError).toContain('Incorrect credentials');
  });

  // Test 5: Logout llama al servicio y redirige a home
  it('should call logout and navigate to home', () => {
    const navigateSpy = spyOn(router, 'navigate');
    loginServiceSpy.logout.and.returnValue(of({}));

    component.logout();

    expect(loginServiceSpy.logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  // Test 6: isLogged devuelve false cuando no hay sesión
  it('should report not logged in when no session exists', () => {
    expect(loginServiceSpy.isLogged()).toBeFalse();
  });

  // Test 7: isRoleAdmin devuelve true cuando el usuario es admin
  it('should report admin role correctly', () => {
    loginServiceSpy.isRoleAdmin.and.returnValue(true);
    expect(component.loginService.isRoleAdmin()).toBeTrue();
  });
});