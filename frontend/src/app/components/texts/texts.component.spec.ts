import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of, BehaviorSubject } from 'rxjs';

import { TextsComponent } from './texts.component';
import { TextsService, TextItem } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('TextsComponent', () => {
  let component: TextsComponent;
  let fixture: ComponentFixture<TextsComponent>;
  let textsServiceSpy: jasmine.SpyObj<TextsService>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;

  const mockTexts: TextItem[] = [
    {
      id: 1, titleEnglish: 'My Friend', titleSpanish: 'Mi Amiga',
      text: '我有一个朋友。', spanishTranslation: 'Tengo una amiga.',
      englishTranslation: 'I have a friend.', level: 'HSK1',
      englishDescription: 'A text about a friend',
      spanishDescription: 'Un texto sobre una amiga',
      creationDate: '2025-05-30'
    },
    {
      id: 2, titleEnglish: 'Daily Routine', titleSpanish: 'Rutina Diaria',
      text: '我每天七点起床。', spanishTranslation: 'Me levanto a las siete.',
      englishTranslation: 'I get up at seven.', level: 'HSK2',
      englishDescription: 'A text about routines',
      spanishDescription: 'Un texto sobre rutinas',
      creationDate: '2025-05-31'
    }
  ];

  beforeEach(async () => {
    textsServiceSpy = jasmine.createSpyObj('TextsService',
      ['getTexts', 'getTextsByLevel', 'deleteText', 'updateTextMetadata']);
    loginServiceSpy = jasmine.createSpyObj('LoginService',
      ['isLogged', 'isRoleAdmin', 'reqIsLogged'],
      { loggedIn$: new BehaviorSubject<boolean>(false).asObservable() }
    );
    loginServiceSpy.isLogged.and.returnValue(false);
    loginServiceSpy.isRoleAdmin.and.returnValue(false);
    loginServiceSpy.reqIsLogged.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), TextsComponent],
      providers: [
        { provide: TextsService, useValue: textsServiceSpy },
        { provide: LoginService, useValue: loginServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TextsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    textsServiceSpy.getTexts.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load all texts when no level is specified', () => {
    textsServiceSpy.getTexts.and.returnValue(of(mockTexts));
    fixture.detectChanges();
    expect(textsServiceSpy.getTexts).toHaveBeenCalledWith(0, 2, undefined);
    expect(component.texts.length).toBe(2);
  });

  it('should accumulate texts when loading more', () => {
    textsServiceSpy.getTexts.and.returnValue(of(mockTexts));
    fixture.detectChanges();
    textsServiceSpy.getTexts.and.returnValue(of([mockTexts[0]]));
    component.loadMore();
    expect(component.texts.length).toBe(3);
  });

  it('should set hasMore to false when response has fewer items than size', () => {
    textsServiceSpy.getTexts.and.returnValue(of([mockTexts[0]]));
    fixture.detectChanges();
    expect(component.hasMore).toBeFalse();
  });

  it('should call getTextsByLevel when level is provided', () => {
    textsServiceSpy.getTextsByLevel.and.returnValue(of(mockTexts));
    component.currentLevel = 'HSK1';
    component.loadTextsByLevel('HSK1');
    expect(textsServiceSpy.getTextsByLevel).toHaveBeenCalledWith('HSK1', 0, 2, undefined);
  });

  it('should pass the active topic filter to the service', () => {
    textsServiceSpy.getTexts.and.returnValue(of([]));
    textsServiceSpy.getTextsByLevel.and.returnValue(of([]));

    component.currentTopic = 'food';
    component.loadTexts();
    expect(textsServiceSpy.getTexts).toHaveBeenCalledWith(0, 2, 'food');

    component.currentLevel = 'HSK1';
    component.loadTextsByLevel('HSK1');
    expect(textsServiceSpy.getTextsByLevel).toHaveBeenCalledWith('HSK1', 0, 2, 'food');
  });

  it('selectTopic should update the topic query param in the URL', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');

    component.selectTopic('sports');

    expect(navigateSpy).toHaveBeenCalledWith([], jasmine.objectContaining({
      queryParams: { topic: 'sports' },
      queryParamsHandling: 'merge'
    }));
  });

  it('openEditModal should preload the form with the text metadata', () => {
    const text: TextItem = { ...mockTexts[0], topics: ['food', 'family'] };
    component.openEditModal(text, new MouseEvent('click'));

    expect(component.showEditModal).toBeTrue();
    expect(component.editTitleEnglish).toBe('My Friend');
    expect(component.editLevel).toBe('HSK1');
    expect(component.editTopics).toEqual(['food', 'family']);
    // Copia defensiva: tocar el formulario no muta el texto de la lista
    component.toggleEditTopic('food');
    expect(text.topics).toEqual(['food', 'family']);
  });

  it('saveEdit should PATCH the metadata and update the listed text', () => {
    textsServiceSpy.getTexts.and.returnValue(of(mockTexts));
    fixture.detectChanges();

    const updated: TextItem = { ...mockTexts[0], level: 'HSK3', topics: ['travel'] };
    textsServiceSpy.updateTextMetadata.and.returnValue(of(updated));

    component.openEditModal(component.texts[0], new MouseEvent('click'));
    component.editLevel = 'HSK3';
    component.editTopics = ['travel'];
    component.saveEdit();

    expect(textsServiceSpy.updateTextMetadata).toHaveBeenCalledWith(1,
      jasmine.objectContaining({ level: 'HSK3', topics: ['travel'] }));
    expect(component.texts[0].level).toBe('HSK3');
    expect(component.texts[0].topics).toEqual(['travel']);
    expect(component.showEditModal).toBeFalse();
  });

  it('toggleEditTopic should enforce the maximum number of topics', () => {
    component.editTopics = ['daily-life', 'family', 'food', 'travel', 'culture'];
    component.toggleEditTopic('sports');
    expect(component.editTopics.length).toBe(5);
    expect(component.editTopics).not.toContain('sports');

    component.toggleEditTopic('food');
    expect(component.editTopics).not.toContain('food');
  });

  it('should toggle the liked state of a text', () => {
    textsServiceSpy.getTexts.and.returnValue(of(mockTexts));
    fixture.detectChanges();
    const text = component.texts[0];
    const event = new MouseEvent('click');
    expect(text.liked).toBeFalsy();
    component.toggleLike(text, event);
    expect(text.liked).toBeTrue();
    component.toggleLike(text, event);
    expect(text.liked).toBeFalse();
  });
});