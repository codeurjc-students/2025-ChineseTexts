import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { AdminToolsComponent } from './admin-tools.component';
import { LoginService } from '../../services/login.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('AdminToolsComponent', () => {
  let component: AdminToolsComponent;
  let fixture: ComponentFixture<AdminToolsComponent>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;

  const mockAdminUser: any = {
    id: 1, email: 'admin@test.com', name: 'Admin',
    language: 'es', collections: [], roles: ['ADMIN'],
    password: '', newPassword: null
  };

  beforeEach(async () => {
    loginServiceSpy = jasmine.createSpyObj('LoginService', ['reqIsLogged']);
    loginServiceSpy.reqIsLogged.and.returnValue(of(mockAdminUser));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), AdminToolsComponent],
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminToolsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start on the menu', () => {
    expect(component.tool).toBe('menu');
  });

  it('should select a tool', () => {
    component.select('words');
    expect(component.tool).toBe('words');
  });

  it('should return to the menu', () => {
    component.select('ai');
    component.backToMenu();
    expect(component.tool).toBe('menu');
  });

  it('should redirect to home if user is not admin', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');
    loginServiceSpy.reqIsLogged.and.returnValue(of({ ...mockAdminUser, roles: ['USER'] }));
    component.ngOnInit();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});
