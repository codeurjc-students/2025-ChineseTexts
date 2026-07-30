import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { BlogEditorComponent } from './blog-editor.component';

import { translocoTesting } from '../../i18n/transloco-testing';

/**
 * Specs superficiales (sin renderizar Quill): la lógica de guardado y el
 * contrato con la API. El editor completo se valida manualmente en navegador.
 */
describe('BlogEditorComponent', () => {
  let component: BlogEditorComponent;
  let fixture: ComponentFixture<BlogEditorComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), BlogEditorComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(BlogEditorComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create (no id → new-post mode, nothing loaded)', () => {
    component.ngOnInit();
    expect(component).toBeTruthy();
    expect(component.id).toBeNull();
    httpMock.expectNone('/api/blog/undefined');
  });

  it('save() without id POSTs, omits the blank slug and reflects the server-derived one', () => {
    component.titleEn = 'My new post';
    component.contentEn = '<p>Body</p>';
    component.slug = '   '; // en blanco → el backend lo deriva del título

    component.save();
    const req = httpMock.expectOne('/api/blog');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.titleEn).toBe('My new post');
    expect(req.request.body.slug).toBeUndefined();
    req.flush({ id: 3, slug: 'my-new-post', titleEn: 'My new post', titleEs: null,
      excerptEn: null, excerptEs: null, contentEn: '<p>Body</p>', contentEs: null,
      hasCover: false, published: false, publishedOn: null, updatedAt: null });

    expect(component.id).toBe(3);
    expect(component.slug).toBe('my-new-post');

    // El siguiente guardado (con id) es un PUT parcial al post existente.
    component.save();
    const putReq = httpMock.expectOne('/api/blog/3');
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.slug).toBe('my-new-post');
  });
});
