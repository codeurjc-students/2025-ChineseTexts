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

    const textsServiceSpy = jasmine.createSpyObj('TextsService', ['uploadText']);
    const wordsServiceSpy = jasmine.createSpyObj('WordsService', ['saveWord']);

    await TestBed.configureTestingModule({
      imports: [AiToolsComponent],
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
    expect(component.canUpload).toBeFalse();
  });

  it('should not upload when missing words are not saved', () => {
    component.status = 'ready';
    component.imageFile = new File([''], 'test.jpg');
    component.missingWordForms = [
      { chinese: '世界', pinyin: '', english: '', spanish: '', saved: false, saving: false, error: '' }
    ];
    expect(component.canUpload).toBeFalse();
  });

  it('should allow upload when all conditions are met', () => {
    component.status = 'ready';
    component.imageFile = new File([''], 'test.jpg');
    component.chineseText = '你好。';
    component.missingWordForms = [];
    expect(component.canUpload).toBeTrue();
  });

  it('should redirect to home if user is not admin', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');
    loginServiceSpy.reqIsLogged.and.returnValue(of({ ...mockAdminUser, roles: ['USER'] }));
    component.ngOnInit();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});