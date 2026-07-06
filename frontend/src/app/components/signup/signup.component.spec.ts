import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';

import { SignupComponent } from './signup.component';
import { UserService, UserDTO } from '../../services/users.service';
import { LoginService } from '../../services/login.service';

@Component({ template: '' })
class DummyComponent {}

import { translocoTesting } from "../../i18n/transloco-testing";

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let router: Router;
  let loggedInSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    loggedInSubject = new BehaviorSubject<boolean>(false);
    userServiceSpy = jasmine.createSpyObj('UserService', ['register']);
    loginServiceSpy = jasmine.createSpyObj('LoginService',
      ['isLogged', 'reqIsLogged'],
      { loggedIn$: loggedInSubject.asObservable() }
    );
    loginServiceSpy.isLogged.and.returnValue(false);
    loginServiceSpy.reqIsLogged.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), SignupComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: LoginService, useValue: loginServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: '', component: DummyComponent },
          { path: 'success', component: DummyComponent },
          { path: 'error', component: DummyComponent }
        ])
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with an invalid form', () => {
    expect(component.signupForm.invalid).toBeTrue();
  });

  it('should be valid when all fields are filled correctly', () => {
    component.signupForm.setValue({
      name: 'Test User',
      email: 'test@test.com',
      password: 'password123',
      language: 'en'
    });
    expect(component.signupForm.valid).toBeTrue();
  });

  it('should mark email as invalid with wrong format', () => {
    component.signupForm.get('email')?.setValue('notanemail');
    expect(component.signupForm.get('email')?.invalid).toBeTrue();
  });

  // Test nuevo: password con menos de 6 caracteres es inválida
  it('should mark password as invalid when shorter than 6 characters', () => {
    component.signupForm.get('password')?.setValue('abc');
    expect(component.signupForm.get('password')?.invalid).toBeTrue();
    expect(component.signupForm.get('password')?.errors?.['minlength']).toBeTruthy();
  });

  // Test nuevo: password con 6 o más caracteres es válida
  it('should mark password as valid when 6 or more characters', () => {
    component.signupForm.get('password')?.setValue('abc123');
    expect(component.signupForm.get('password')?.valid).toBeTrue();
  });

  it('should not call register when form is invalid', () => {
    component.submitSignup();
    expect(userServiceSpy.register).not.toHaveBeenCalled();
  });

  it('should call register with correct data when form is valid', () => {
    const mockUser: UserDTO = {
      id: 1, email: 'test@test.com', name: 'Test User',
      language: 'en', collections: [], roles: ['USER'],
      password: 'password123', newPassword: null
    };
    userServiceSpy.register.and.returnValue(of(mockUser));

    component.signupForm.setValue({
      name: 'Test User', email: 'test@test.com',
      password: 'password123', language: 'en'
    });
    component.submitSignup();

    expect(userServiceSpy.register).toHaveBeenCalledWith(
      jasmine.objectContaining({
        email: 'test@test.com', name: 'Test User',
        language: 'en', roles: ['USER']
      })
    );
  });

  it('should navigate to /success after successful registration', () => {
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);
    const mockUser: UserDTO = {
      id: 1, email: 'test@test.com', name: 'Test',
      language: 'en', collections: [], roles: ['USER'],
      password: 'pass123', newPassword: null
    };
    userServiceSpy.register.and.returnValue(of(mockUser));

    component.signupForm.setValue({
      name: 'Test', email: 'test@test.com',
      password: 'pass123', language: 'en'
    });
    component.submitSignup();

    expect(navigateSpy).toHaveBeenCalledWith(['/success'], jasmine.any(Object));
  });

  it('should navigate to /error when registration fails', () => {
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);
    userServiceSpy.register.and.returnValue(
      throwError(() => ({ error: { message: 'Email already in use' } }))
    );

    component.signupForm.setValue({
      name: 'Test', email: 'existing@test.com',
      password: 'pass123', language: 'en'
    });
    component.submitSignup();

    expect(navigateSpy).toHaveBeenCalledWith(['/error'], jasmine.any(Object));
  });

  it('should redirect to home if user is already logged in', () => {
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);
    loginServiceSpy.isLogged.and.returnValue(true);

    component.ngOnInit();

    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('should redirect to home when user logs in while on signup page', () => {
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);

    component.ngOnInit();
    loggedInSubject.next(true);

    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});