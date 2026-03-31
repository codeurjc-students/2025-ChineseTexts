import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

import { UploadTextComponent } from './upload-text.component';
import { TextsService, ValidationResult } from '../../services/texts.service';
import { WordsService, Word } from '../../services/words.service';
import { LoginService } from '../../services/login.service';

describe('UploadTextComponent', () => {
  let component: UploadTextComponent;
  let fixture: ComponentFixture<UploadTextComponent>;
  let textsServiceSpy: jasmine.SpyObj<TextsService>;
  let wordsServiceSpy: jasmine.SpyObj<WordsService>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let router: Router;

  const mockAdminUser: any = {
    id: 1, email: 'admin@test.com', name: 'Admin',
    language: 'es', collections: [], roles: ['ADMIN'],
    password: '', newPassword: null
  };

  const mockValidationOk: ValidationResult = {
    valid: true, missingWords: [], segments: ['你好', '。']
  };

  const mockValidationFail: ValidationResult = {
    valid: false, missingWords: ['世界'], segments: ['你好', '世界', '。']
  };

  beforeEach(async () => {
    textsServiceSpy = jasmine.createSpyObj('TextsService', ['validateText', 'uploadText']);
    wordsServiceSpy = jasmine.createSpyObj('WordsService', ['saveWord']);
    loginServiceSpy = jasmine.createSpyObj('LoginService', ['reqIsLogged']);
    loginServiceSpy.reqIsLogged.and.returnValue(of(mockAdminUser));

    await TestBed.configureTestingModule({
      imports: [UploadTextComponent],
      providers: [
        { provide: TextsService, useValue: textsServiceSpy },
        { provide: WordsService, useValue: wordsServiceSpy },
        { provide: LoginService, useValue: loginServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UploadTextComponent);
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

  it('should redirect to home if user is not admin', () => {
    const navigateSpy = spyOn(router, 'navigate');
    loginServiceSpy.reqIsLogged.and.returnValue(of({ ...mockAdminUser, roles: ['USER'] }));
    component.ngOnInit();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('should return false for formComplete when image is missing', () => {
    component.titleEnglish = 'Title';
    component.titleSpanish = 'Título';
    component.chineseText = '你好。';
    component.englishTranslation = 'Hello.';
    component.spanishTranslation = 'Hola.';
    component.englishDescription = 'Desc';
    component.spanishDescription = 'Desc';
    component.imageFile = null;
    expect(component.formComplete).toBeFalse();
  });

  it('should return false for formComplete when a text field is missing', () => {
    component.titleEnglish = '';
    component.imageFile = new File([''], 'test.jpg', { type: 'image/jpeg' });
    expect(component.formComplete).toBeFalse();
  });

  it('should set error status when image is missing on validate', () => {
    component.titleEnglish = 'Title';
    component.titleSpanish = 'Título';
    component.chineseText = '你好。';
    component.englishTranslation = 'Hello.';
    component.spanishTranslation = 'Hola.';
    component.englishDescription = 'Desc';
    component.spanishDescription = 'Desc';
    component.imageFile = null;

    component.validate();

    expect(component.status).toBe('error');
    expect(component.errorMessage).toContain('image is required');
  });

  it('should set error when sentence count does not match', () => {
    component.titleEnglish = 'Title';
    component.titleSpanish = 'Título';
    component.chineseText = '你好。再见。';
    component.englishTranslation = 'Hello.';
    component.spanishTranslation = 'Hola.';
    component.englishDescription = 'Desc';
    component.spanishDescription = 'Desc';
    component.imageFile = new File([''], 'test.jpg', { type: 'image/jpeg' });

    component.validate();

    expect(component.status).toBe('error');
    expect(component.errorMessage).toContain('sentences');
  });

  it('should set status to valid when all words are in the dictionary', () => {
    textsServiceSpy.validateText.and.returnValue(of(mockValidationOk));
    fillCompleteForm(component);

    component.validate();

    expect(textsServiceSpy.validateText).toHaveBeenCalledWith('你好。');
    expect(component.status).toBe('valid');
    expect(component.missingWordForms.length).toBe(0);
  });

  it('should set status to invalid and create missing word forms when words are missing', () => {
    textsServiceSpy.validateText.and.returnValue(of(mockValidationFail));
    component.titleEnglish = 'Title';
    component.titleSpanish = 'Título';
    component.chineseText = '你好世界。';
    component.englishTranslation = 'Hello world.';
    component.spanishTranslation = 'Hola mundo.';
    component.englishDescription = 'Desc';
    component.spanishDescription = 'Desc';
    component.imageFile = new File([''], 'test.jpg', { type: 'image/jpeg' });

    component.validate();

    expect(component.status).toBe('invalid');
    expect(component.missingWordForms.length).toBe(1);
    expect(component.missingWordForms[0].chinese).toBe('世界');
  });

  it('should show error on saveWord when fields are empty', () => {
    const form = {
      chinese: '世界', pinyin: '', english: '', spanish: '',
      saved: false, saving: false, error: ''
    };
    component.saveWord(form);
    expect(form.error).toBe('All fields are required.');
    expect(wordsServiceSpy.saveWord).not.toHaveBeenCalled();
  });

  it('should mark word as saved after successful saveWord', () => {
    const mockWord: Word = {
      chinese: '世界', pinyin: 'shìjiè', english: 'world', spanish: 'mundo'
    };
    wordsServiceSpy.saveWord.and.returnValue(of(mockWord));
    textsServiceSpy.validateText.and.returnValue(of(mockValidationOk));
    fillCompleteForm(component);

    const form = {
      chinese: '世界', pinyin: 'shìjiè', english: 'world', spanish: 'mundo',
      saved: false, saving: false, error: ''
    };
    component.missingWordForms = [form];
    component.saveWord(form);

    expect(form.saved).toBeTrue();
    expect(wordsServiceSpy.saveWord).toHaveBeenCalled();
  });

  it('should set status to success after successful upload', () => {
    textsServiceSpy.uploadText.and.returnValue(of({} as any));
    component.status = 'valid';
    fillCompleteForm(component);

    component.upload();

    expect(textsServiceSpy.uploadText).toHaveBeenCalled();
    expect(component.status).toBe('success');
  });

  it('should show conflict error when title already exists', () => {
    textsServiceSpy.uploadText.and.returnValue(throwError(() => ({ status: 409 })));
    component.status = 'valid';
    fillCompleteForm(component);

    component.upload();

    expect(component.status).toBe('error');
    expect(component.errorMessage).toContain('already exists');
  });

  it('should reset all fields to initial state', () => {
    component.titleEnglish = 'Test';
    component.status = 'valid';
    component.imageFile = new File([''], 'test.jpg');

    component.reset();

    expect(component.titleEnglish).toBe('');
    expect(component.status).toBe('idle');
    expect(component.imageFile).toBeNull();
    expect(component.missingWordForms.length).toBe(0);
  });

  it('should return true for sentenceCountMatch when periods match', () => {
    component.chineseText = '你好。再见。';
    component.englishTranslation = 'Hello. Goodbye.';
    component.spanishTranslation = 'Hola. Adiós.';
    expect(component.sentenceCountMatch).toBeTrue();
  });

  it('should return false for sentenceCountMatch when periods do not match', () => {
    component.chineseText = '你好。再见。';
    component.englishTranslation = 'Hello.';
    component.spanishTranslation = 'Hola.';
    expect(component.sentenceCountMatch).toBeFalse();
  });

  function fillCompleteForm(c: UploadTextComponent): void {
    c.titleEnglish = 'Title';
    c.titleSpanish = 'Título';
    c.chineseText = '你好。';
    c.englishTranslation = 'Hello.';
    c.spanishTranslation = 'Hola.';
    c.englishDescription = 'Desc';
    c.spanishDescription = 'Desc';
    c.imageFile = new File([''], 'test.jpg', { type: 'image/jpeg' });
  }
});