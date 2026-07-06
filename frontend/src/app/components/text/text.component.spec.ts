import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { TextComponent } from './text.component';
import { TextsService, TextItem } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';
import { WordsService, Word } from '../../services/words.service';
import { CollectionsService } from '../../services/collections.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('TextComponent', () => {
  let component: TextComponent;
  let fixture: ComponentFixture<TextComponent>;
  let textsServiceSpy: jasmine.SpyObj<TextsService>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let wordsServiceSpy: jasmine.SpyObj<WordsService>;
  let collectionsServiceSpy: jasmine.SpyObj<CollectionsService>;

  const mockText: TextItem = {
    id: 1,
    titleEnglish: 'Daily Routine',
    titleSpanish: 'Rutina Diaria',
    text: '我每天七点起床。我吃早饭。然后去学校。',
    spanishTranslation: 'Me levanto a las siete. Desayuno. Luego voy a la escuela.',
    englishTranslation: 'I get up at seven. I eat breakfast. Then I go to school.',
    level: 'HSK2',
    englishDescription: 'A daily routine text',
    spanishDescription: 'Un texto de rutina diaria',
    creationDate: '2025-05-30'
  };

  const mockWords: Word[] = [
    { id: 1, chinese: '我', pinyin: 'wǒ', english: 'I', spanish: 'Yo' },
    { id: 2, chinese: '每天', pinyin: 'měitiān', english: 'Every day', spanish: 'Cada día' },
    { id: 3, chinese: '七点', pinyin: 'qī diǎn', english: 'Seven o\'clock', spanish: 'Siete en punto' },
    { id: 4, chinese: '起床', pinyin: 'qǐchuáng', english: 'Get up', spanish: 'Levantarse' },
    { id: 5, chinese: '。', pinyin: '。', english: '.', spanish: '.' },
  ];

  beforeEach(async () => {
    textsServiceSpy = jasmine.createSpyObj('TextsService',
      ['getText', 'getSpanishText', 'getEnglishText']);
    loginServiceSpy = jasmine.createSpyObj('LoginService',
      ['reqIsLogged', 'isLogged', 'isRoleAdmin', 'isRoleUser'],
      { loggedIn$: of(true) }
    );
    wordsServiceSpy = jasmine.createSpyObj('WordsService', ['getTextWords']);
    collectionsServiceSpy = jasmine.createSpyObj('CollectionsService',
      ['getUserCollections', 'addFlashcard', 'createCollection']);

    loginServiceSpy.reqIsLogged.and.returnValue(of(null));
    loginServiceSpy.isLogged.and.returnValue(false);
    loginServiceSpy.isRoleAdmin.and.returnValue(false);
    loginServiceSpy.isRoleUser.and.returnValue(false);
    textsServiceSpy.getText.and.returnValue(of(mockText));
    textsServiceSpy.getSpanishText.and.returnValue(of([
      ['我', '每天', '七点', '起床', '。', '我', '吃', '早饭', '。', '然后', '去', '学校', '。'],
      ['Yo', 'Cada día', 'Siete en punto', 'Levantarse', '.', 'Yo', 'Comer', 'Desayuno', '.', 'Luego', 'Ir', 'Escuela', '.']
    ]));
    textsServiceSpy.getEnglishText.and.returnValue(of([
      ['我', '每天', '七点', '起床', '。', '我', '吃', '早饭', '。', '然后', '去', '学校', '。'],
      ['I', 'Every day', 'Seven o\'clock', 'Get up', '.', 'I', 'Eat', 'Breakfast', '.', 'Then', 'Go', 'School', '.']
    ]));
    wordsServiceSpy.getTextWords.and.returnValue(of(mockWords));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), TextComponent],
      providers: [
        { provide: TextsService, useValue: textsServiceSpy },
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: WordsService, useValue: wordsServiceSpy },
        { provide: CollectionsService, useValue: collectionsServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TextComponent);
    component = fixture.componentInstance;
  });

  // Test 1: El componente se crea correctamente
  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  // Test 2: El texto chino se divide en palabras correctamente
  // Este test verifica que originalText se puebla con el array de palabras
  it('should split chinese text into words array', () => {
    fixture.detectChanges();

    expect(component.originalText.length).toBeGreaterThan(0);
    expect(component.originalText).toContain('我');
    expect(component.originalText).toContain('每天');
  });

  // Test 3: Las frases se forman correctamente agrupando palabras hasta el punto chino
  // Este es el test más importante — verifica getSentences indirectamente
  it('should group words into sentences ending with 。', () => {
    fixture.detectChanges();

    // El texto tiene 3 frases separadas por 。
    expect(component.originalTextSeparatedBySentences.length).toBe(3);
    // La primera frase incluye 我每天七点起床。
    expect(component.originalTextSeparatedBySentences[0]).toContain('。');
  });

  // Test 4: Las traducciones al español se cargan correctamente
  it('should load spanish translations', () => {
    fixture.detectChanges();

    expect(component.translatedSpanishText.length).toBeGreaterThan(0);
    expect(component.translatedSpanishText[0]).toBe('Yo');
  });

  // Test 5: Las traducciones al inglés se cargan correctamente
  it('should load english translations', () => {
    fixture.detectChanges();

    expect(component.translatedEnglishText.length).toBeGreaterThan(0);
    expect(component.translatedEnglishText[0]).toBe('I');
  });

  // Test 6: El popover se abre al hacer clic en una palabra
  it('should open word popover on word click', () => {
    fixture.detectChanges();

    const event = new MouseEvent('click');
    component.onWordClick(event, 0);

    expect(component.activeWordIndex).toBe(0);
  });

  // Test 7: El popover se cierra al hacer clic en la misma palabra dos veces
  it('should close word popover when clicking the same word twice', () => {
    fixture.detectChanges();

    const event = new MouseEvent('click');
    component.onWordClick(event, 0);
    expect(component.activeWordIndex).toBe(0);

    component.onWordClick(event, 0);
    expect(component.activeWordIndex).toBeNull();
  });

  // Test 8: Al abrir el popover de una palabra, se cierra el modal de frase si estaba abierto
  it('should close sentence modal when opening word popover', () => {
    fixture.detectChanges();

    component.activeSentenceIndex = 1;
    const event = new MouseEvent('click');
    component.onWordClick(event, 0);

    expect(component.activeSentenceIndex).toBeNull();
    expect(component.activeWordIndex).toBe(0);
  });

  // Test 9: El modal de frase se abre al hacer clic en una frase
  it('should open sentence modal on sentence click', () => {
    fixture.detectChanges();

    component.onSentenceClick(0);
    expect(component.activeSentenceIndex).toBe(0);
  });

  // Test 10: El modal de frase se cierra al hacer clic en la misma frase dos veces
  it('should close sentence modal when clicking same sentence twice', () => {
    fixture.detectChanges();

    component.onSentenceClick(0);
    component.onSentenceClick(0);

    expect(component.activeSentenceIndex).toBeNull();
  });

  // Test 11: toggleLike cambia el estado del like
  it('should toggle liked state', () => {
    fixture.detectChanges();

    expect(component.liked).toBeFalse();
    component.toggleLike();
    expect(component.liked).toBeTrue();
    component.toggleLike();
    expect(component.liked).toBeFalse();
  });

  // Test 12: addWord muestra el panel de guardado — si no está logueado, muestra not-logged
  it('should show not-logged status when saving word without being logged in', () => {
    loginServiceSpy.isLogged.and.returnValue(false);
    fixture.detectChanges();

    component.addWord('你好');

    expect(component.saveStatus).toBe('not-logged');
    expect(component.showSavePanel).toBeTrue();
  });

  // Test 13: getSentencesString divide correctamente una traducción por puntos
  // Probamos indirectamente que las frases traducidas se dividen igual que las chinas
  it('should split translated text into sentences matching chinese sentence count', () => {
    fixture.detectChanges();

    // La traducción inglesa tiene 3 frases separadas por punto
    expect(component.translatedEnglishTextSeparatedBySentences.length).toBe(3);
  });
});