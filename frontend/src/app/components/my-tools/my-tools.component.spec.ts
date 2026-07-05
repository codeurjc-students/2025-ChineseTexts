import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { MyToolsComponent } from './my-tools.component';

describe('MyToolsComponent', () => {
  let component: MyToolsComponent;
  let fixture: ComponentFixture<MyToolsComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyToolsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyToolsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('switches between paste and OCR input modes', () => {
    component.setMode('ocr');
    expect(component.mode).toBe('ocr');
    component.setMode('paste');
    expect(component.mode).toBe('paste');
  });

  it('rejects an empty paste submission without calling the API', () => {
    component.mode = 'paste';
    component.pasteText = '   ';
    component.create();
    expect(component.messageType).toBe('error');
    httpMock.expectNone('/api/my-texts');
  });
});
