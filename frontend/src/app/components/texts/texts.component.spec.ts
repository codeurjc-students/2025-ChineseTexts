import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { TextsComponent } from './texts.component';
import { TextsService, TextItem } from '../../services/texts.service';

describe('TextsComponent', () => {
  let component: TextsComponent;
  let fixture: ComponentFixture<TextsComponent>;
  let textsServiceSpy: jasmine.SpyObj<TextsService>;

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
    textsServiceSpy = jasmine.createSpyObj('TextsService', ['getTexts', 'getTextsByLevel']);

    await TestBed.configureTestingModule({
      imports: [TextsComponent, RouterTestingModule],
      providers: [
        { provide: TextsService, useValue: textsServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TextsComponent);
    component = fixture.componentInstance;
  });

  // Test unitario 1: El componente se crea correctamente
  it('should create', () => {
    textsServiceSpy.getTexts.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  // Test unitario 2: Cuando no hay nivel, carga todos los textos
  it('should load all texts when no level is specified', () => {
    textsServiceSpy.getTexts.and.returnValue(of(mockTexts));
    fixture.detectChanges();

    expect(textsServiceSpy.getTexts).toHaveBeenCalledWith(0, 2);
    expect(component.texts.length).toBe(2);
  });

  // Test unitario 3: Los textos se acumulan al paginar
  it('should accumulate texts when loading more', () => {
    textsServiceSpy.getTexts.and.returnValue(of(mockTexts));
    fixture.detectChanges();

    textsServiceSpy.getTexts.and.returnValue(of([mockTexts[0]]));
    component.loadMore();

    expect(component.texts.length).toBe(3);
  });

  // Test unitario 4: hasMore se pone a false cuando la respuesta tiene menos items que el size
  it('should set hasMore to false when response has fewer items than size', () => {
    textsServiceSpy.getTexts.and.returnValue(of([mockTexts[0]]));
    fixture.detectChanges();

    expect(component.hasMore).toBeFalse();
  });

  // Test unitario 5: Cuando hay nivel, llama a getTextsByLevel
  it('should call getTextsByLevel when level is provided', () => {
    textsServiceSpy.getTextsByLevel.and.returnValue(of(mockTexts));
    component.currentLevel = 'HSK1';
    component.loadTextsByLevel('HSK1');

    expect(textsServiceSpy.getTextsByLevel).toHaveBeenCalledWith('HSK1', 0, 2);
  });

  // Test unitario 6: toggleLike cambia el estado liked del texto
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