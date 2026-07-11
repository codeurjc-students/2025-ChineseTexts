import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { ProfileComponent } from './profile.component';
import { LoginService } from '../../services/login.service';
import { UserService, UserDTO } from '../../services/users.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  const mockUser: UserDTO = {
    id: 1, email: 'user@test.com', name: 'Test User',
    language: 'es', collections: [], roles: ['USER'],
    password: '', newPassword: null
  };

  beforeEach(async () => {
    loginServiceSpy = jasmine.createSpyObj('LoginService',
      ['isLogged', 'reqIsLogged'], { loggedIn$: of(true) });
    userServiceSpy = jasmine.createSpyObj('UserService',
      ['updateProfile', 'checkPassword', 'changePassword']);

    loginServiceSpy.reqIsLogged.and.returnValue(of(mockUser));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), ProfileComponent, RouterTestingModule],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Test unitario 1: El componente carga el usuario correctamente
  it('should load the user on init', () => {
    expect(component.user).toEqual(mockUser);
    expect(component.section).toBe('view');
  });

  // Test unitario 2: openEditProfile rellena los campos con los datos actuales
  it('should populate edit fields with current user data', () => {
    component.openEditProfile();

    expect(component.section).toBe('editProfile');
    expect(component.editName).toBe('Test User');
    expect(component.editLanguage).toBe('es');
  });

  // Test unitario 3: saveProfile muestra error si el nombre está vacío
  it('should show error when saving profile with empty name', () => {
    component.openEditProfile();
    component.editName = '';
    component.saveProfile();

    expect(component.profileStatus).toBe('error');
    expect(component.profileError).toBe('Name cannot be empty.');
    expect(userServiceSpy.updateProfile).not.toHaveBeenCalled();
  });

  // Test unitario 4: saveProfile llama al servicio con los datos correctos
  it('should call updateProfile with correct data', () => {
    userServiceSpy.updateProfile.and.returnValue(of({ ...mockUser, name: 'New Name' }));
    loginServiceSpy.reqIsLogged.and.returnValue(of(mockUser));

    component.openEditProfile();
    component.editName = 'New Name';
    component.saveProfile();

    expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({
      name: 'New Name', language: 'es'
    });
    expect(component.profileStatus).toBe('success');
  });

  // Test unitario 5: savePassword muestra error si la contraseña nueva es muy corta
  it('should show error when new password is too short', () => {
    component.openEditPassword();
    component.currentPassword = 'current';
    component.newPassword = 'abc';
    component.confirmPassword = 'abc';
    component.savePassword();

    expect(component.passwordStatus).toBe('error');
    expect(component.passwordError).toContain('6 characters');
  });

  // Test unitario 6: savePassword muestra error si las contraseñas no coinciden
  it('should show error when passwords do not match', () => {
    component.openEditPassword();
    component.currentPassword = 'current';
    component.newPassword = 'newpass123';
    component.confirmPassword = 'different123';
    component.savePassword();

    expect(component.passwordStatus).toBe('error');
    expect(component.passwordError).toContain('do not match');
  });

  // Test unitario 7: savePassword muestra error si la contraseña actual es incorrecta
  it('should show error when current password is incorrect', () => {
    userServiceSpy.checkPassword.and.returnValue(throwError(() => ({ status: 401 })));

    component.openEditPassword();
    component.currentPassword = 'wrongpass';
    component.newPassword = 'newpass123';
    component.confirmPassword = 'newpass123';
    component.savePassword();

    expect(component.passwordStatus).toBe('error');
    expect(component.passwordError).toContain('incorrect');
  });
});