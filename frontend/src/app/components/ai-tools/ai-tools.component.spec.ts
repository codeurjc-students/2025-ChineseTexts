import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { AiToolsComponent } from './ai-tools.component';
import { AiService } from '../../services/ai.service';
import { LoginService } from '../../services/login.service';
import { TextsService } from '../../services/texts.service';
import { WordsService } from '../../services/words.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('AiToolsComponent', () => {
  let component: AiToolsComponent;
  let fixture: ComponentFixture<AiToolsComponent>;
  let aiServiceSpy: jasmine.SpyObj<AiService>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;

  const mockAdminUser: any = {
    id: 1, email: 'admin@test.com', name: 'Admin',
    language: 'es', collections: [], roles: ['ADMIN'],
    password: '', newPassword: null
  };

  beforeEach(async () => {
    aiServiceSpy = jasmine.createSpyObj('AiService', ['generateText', 'processOcr']);
    loginServiceSpy = jasmine.createSpyObj('LoginService', ['reqIsLogged']);
    loginServiceSpy.reqIsLogged.and.returnValue(of(mockAdminUser));

    const textsServiceSpy = jasmine.createSpyObj('TextsService', ['uploadText', 'validateText']);
    const wordsServiceSpy = jasmine.createSpyObj('WordsService', ['saveWord']);

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), AiToolsComponent],
      providers: [
        { provide: AiService, useValue: aiServiceSpy },
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: TextsService, useValue: textsServiceSpy },
        { provide: WordsService, useValue: wordsServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AiToolsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start in choose mode', () => {
    expect(component.mode).toBe('choose');
  });

  it('should switch to generate mode', () => {
    component.selectMode('generate');
    expect(component.mode).toBe('generate');
  });

  it('should switch to ocr mode', () => {
    component.selectMode('ocr');
    expect(component.mode).toBe('ocr');
  });

  it('should reset state when switching mode', () => {
    component.chineseText = 'some text';
    component.selectMode('generate');
    expect(component.chineseText).toBe('');
    expect(component.status).toBe('idle');
  });

  it('should not upload when image is missing', () => {
    component.status = 'ready';
    component.imageFile = null;
    component.chineseText = '你好。';

    const valid = component.validateBeforeUpload();
    expect(valid).toBeFalse();
  expect(component.validationError).toBe('Please fill in all fields before uploading.');
  });

  it('should not upload when missing words are not saved', () => {
    component.status = 'ready';
    component.imageFile = new File([''], 'test.jpg');
    component.chineseText = '你好。';
    component.titleEnglish = 'Title';
    component.titleSpanish = 'Título';
    component.englishTranslation = 'Hello.';
    component.spanishTranslation = 'Hola.';
    component.englishDescription = 'Desc';
    component.spanishDescription = 'Desc';
    component.missingWordForms = [
      { chinese: '世界', pinyin: '', english: '', spanish: '', saved: false, saving: false, error: '' }
    ];

    const valid = component.validateBeforeUpload();
    expect(valid).toBeFalse();
    expect(component.validationError).toContain('missing words');
  });

  it('should allow upload when all conditions are met', () => {
    component.status = 'ready';
    component.imageFile = new File([''], 'test.jpg');
    component.chineseText = '你好。';
    component.missingWordForms = [];
    expect(component.canUpload).toBeTrue();
  });

  // La SEGUNDA comprobación de palabras: antes de subir, el backend re-valida el
  // texto final contra el diccionario; si faltan palabras, NO se sube y aparecen
  // como formularios pendientes de guardar
  it('should re-validate words against the dictionary before uploading and block if any is missing', () => {
    const textsService = TestBed.inject(TextsService) as jasmine.SpyObj<TextsService>;
    textsService.validateText.and.returnValue(of({ valid: false, missingWords: ['谢谢'], segments: [] }));

    component.status = 'ready';
    component.imageFile = new File([''], 'test.jpg');
    component.chineseText = '你好。谢谢。';
    component.titleEnglish = 't'; component.titleSpanish = 't';
    component.englishTranslation = 'Hello. Thanks.';
    component.spanishTranslation = 'Hola. Gracias.';
    component.englishDescription = 'd'; component.spanishDescription = 'd';
    component.missingWordForms = [];

    component.upload();

    expect(textsService.uploadText).not.toHaveBeenCalled();
    expect(component.missingWordForms.map(f => f.chinese)).toEqual(['谢谢']);
    expect(component.status).toBe('ready');
  });

  it('should upload when the backend word re-validation passes', () => {
    const textsService = TestBed.inject(TextsService) as jasmine.SpyObj<TextsService>;
    textsService.validateText.and.returnValue(of({ valid: true, missingWords: [], segments: [] }));
    textsService.uploadText.and.returnValue(of({} as any));

    component.status = 'ready';
    component.imageFile = new File([''], 'test.jpg');
    component.chineseText = '你好。谢谢。';
    component.titleEnglish = 't'; component.titleSpanish = 't';
    component.englishTranslation = 'Hello. Thanks.';
    component.spanishTranslation = 'Hola. Gracias.';
    component.englishDescription = 'd'; component.spanishDescription = 'd';
    component.missingWordForms = [];

    component.upload();

    expect(textsService.uploadText).toHaveBeenCalled();
    expect(component.status).toBe('success');
  });

  it('should redirect to home if user is not admin', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');
    loginServiceSpy.reqIsLogged.and.returnValue(of({ ...mockAdminUser, roles: ['USER'] }));
    component.ngOnInit();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});