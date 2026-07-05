import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { MyTextReaderComponent } from './my-text-reader.component';

describe('MyTextReaderComponent', () => {
  let component: MyTextReaderComponent;
  let fixture: ComponentFixture<MyTextReaderComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyTextReaderComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyTextReaderComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows pinyin and translation by default', () => {
    expect(component.showPinyin).toBeTrue();
    expect(component.showTranslation).toBeTrue();
  });
});
