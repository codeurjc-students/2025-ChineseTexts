import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { MyToolsComponent } from './my-tools.component';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('MyToolsComponent', () => {
  let component: MyToolsComponent;
  let fixture: ComponentFixture<MyToolsComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), MyToolsComponent],
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

  it('rejects an empty text submission without calling the API', () => {
    component.pasteText = '   ';
    component.create();
    expect(component.messageType).toBe('error');
    httpMock.expectNone('/api/my-texts');
  });

  it('rejects extract with no image selected without calling the API', () => {
    component.selectedFile = null;
    component.extract();
    expect(component.messageType).toBe('error');
    httpMock.expectNone('/api/my-texts/extract');
  });

  it('shows the upload placeholder (no preview) until a photo is chosen', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.image-placeholder')).toBeTruthy();
    expect(el.querySelector('.image-preview')).toBeFalsy();
  });

  it('renders the photo preview inside the upload area once loaded', () => {
    fixture.detectChanges();
    component.imagePreview = 'data:image/png;base64,AAAA';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.image-preview')).toBeTruthy();
    expect(el.querySelector('.image-placeholder')).toBeFalsy();
  });

  it('clearImage resets the selected photo and its preview', () => {
    component.selectedFile = new File(['x'], 'foto.png', { type: 'image/png' });
    component.imagePreview = 'data:image/png;base64,AAAA';
    component.clearImage();
    expect(component.selectedFile).toBeNull();
    expect(component.imagePreview).toBeNull();
  });
});
