import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { WordManagerComponent } from './word-manager.component';
import { WordsService, Word } from '../../services/words.service';

describe('WordManagerComponent', () => {
  let component: WordManagerComponent;
  let fixture: ComponentFixture<WordManagerComponent>;
  let wordsServiceSpy: jasmine.SpyObj<WordsService>;

  const mockWord: Word = {
    id: 5, chinese: '你好', pinyin: 'nǐ hǎo', english: 'hello', spanish: 'hola'
  };

  beforeEach(async () => {
    wordsServiceSpy = jasmine.createSpyObj('WordsService',
      ['getDictionaryWord', 'saveWord', 'updateWord', 'deleteWord']);

    await TestBed.configureTestingModule({
      imports: [WordManagerComponent],
      providers: [
        { provide: WordsService, useValue: wordsServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WordManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show an error and not search when the term is empty', () => {
    component.searchTerm = '   ';
    component.search();
    expect(wordsServiceSpy.getDictionaryWord).not.toHaveBeenCalled();
    expect(component.messageType).toBe('error');
  });

  it('should switch to edit mode when the word exists', () => {
    wordsServiceSpy.getDictionaryWord.and.returnValue(of(mockWord));
    component.searchTerm = '你好';
    component.search();
    expect(component.mode).toBe('edit');
    expect(component.word.id).toBe(5);
  });

  it('should switch to create mode with prefilled Chinese when the word is not found', () => {
    wordsServiceSpy.getDictionaryWord.and.returnValue(throwError(() => ({ status: 404 })));
    component.searchTerm = '新词';
    component.search();
    expect(component.mode).toBe('create');
    expect(component.word.chinese).toBe('新词');
    expect(component.word.id).toBeUndefined();
  });

  it('should not create when fields are incomplete', () => {
    component.mode = 'create';
    component.word = { chinese: '新词', pinyin: '', english: '', spanish: '' };
    component.create();
    expect(wordsServiceSpy.saveWord).not.toHaveBeenCalled();
    expect(component.messageType).toBe('error');
  });

  it('should create a word and move to edit mode on success', () => {
    wordsServiceSpy.saveWord.and.returnValue(of(mockWord));
    component.mode = 'create';
    component.word = { chinese: '你好', pinyin: 'nǐ hǎo', english: 'hello', spanish: 'hola' };
    component.create();
    expect(wordsServiceSpy.saveWord).toHaveBeenCalled();
    expect(component.mode).toBe('edit');
    expect(component.messageType).toBe('success');
  });

  it('should update a word on success', () => {
    wordsServiceSpy.updateWord.and.returnValue(of(mockWord));
    component.mode = 'edit';
    component.word = { ...mockWord, english: 'hi' };
    component.update();
    expect(wordsServiceSpy.updateWord).toHaveBeenCalledWith(5, jasmine.objectContaining({ english: 'hi' }));
    expect(component.messageType).toBe('success');
  });

  it('should show a conflict error when deleting a word used by flashcards', () => {
    wordsServiceSpy.deleteWord.and.returnValue(throwError(() => ({ status: 409 })));
    component.mode = 'edit';
    component.word = { ...mockWord };
    component.confirmDelete();
    expect(component.messageType).toBe('error');
    expect(component.message).toContain('flashcards');
  });

  it('should reset to search after a successful delete', () => {
    wordsServiceSpy.deleteWord.and.returnValue(of(undefined));
    component.mode = 'edit';
    component.word = { ...mockWord };
    component.confirmDelete();
    expect(component.mode).toBe('search');
    expect(component.messageType).toBe('success');
  });

  it('should emit exit when going back', () => {
    const exitSpy = spyOn(component.exit, 'emit');
    component.goBack();
    expect(exitSpy).toHaveBeenCalled();
  });
});
