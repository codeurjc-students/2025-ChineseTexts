import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AdminUsersComponent } from './admin-users.component';
import { AdminUserSummary, AdminUserDetail } from '../../services/users.service';

describe('AdminUsersComponent', () => {
  let component: AdminUsersComponent;
  let fixture: ComponentFixture<AdminUsersComponent>;
  let httpMock: HttpTestingController;

  const mockUser: AdminUserSummary = {
    id: 1, email: 'a@a.com', name: 'Alice', language: 'en',
    roles: ['USER'], blocked: false, registrationDate: null, lastAccess: null
  };

  const mockDetail: AdminUserDetail = {
    id: 2, email: 'admin@a.com', name: 'Bob', language: 'en',
    roles: ['USER', 'ADMIN'], blocked: false, registrationDate: null, lastAccess: null,
    collectionsCount: 0, flashcardsCount: 0, collections: []
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsersComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminUsersComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the user list from the API on init', () => {
    fixture.detectChanges(); // dispara ngOnInit -> loadUsers
    const req = httpMock.expectOne(r => r.url === '/api/users');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    req.flush([mockUser]);
    expect(component.users.length).toBe(1);
    expect(component.hasMore).toBeFalse();
  });

  it('detects when the detail user is an admin', () => {
    component.detail = mockDetail;
    expect(component.isTargetAdmin).toBeTrue();
  });
});
