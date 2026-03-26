import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TextsService, TextItem } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';
import { WordsService, Word } from '../../services/words.service';

@Component({
  selector: 'app-text',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './text.component.html',
  styleUrl: './text.component.scss'
})
export class TextComponent implements OnInit {

  text: TextItem = {
    id: 0,
    titleSpanish: '',
    titleEnglish: '',
    text: '',
    spanishTranslation: '',
    englishTranslation: '',
    level: '',
    englishDescription: '',
    spanishDescription: '',
    creationDate: '',
    liked: false
  };

  originalText: string[] = [];
  wordsArray: Word[] = [];
  translatedSpanishText: string[] = [];
  translatedEnglishText: string[] = [];
  originalTextSeparatedBySentences: string[] = [];
  translatedSpanishTextSeparatedBySentences: string[] = [];
  translatedEnglishTextSeparatedBySentences: string[] = [];

  liked = false;
  showTranslation = true;
  showSentences = false;
  showPinyin = true;

  // Popover palabra
  activeWordIndex: number | null = null;

  // Modal frase
  activeSentenceIndex: number | null = null;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private textService: TextsService,
    private loginService: LoginService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private wordService: WordsService
  ) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe(() => this.init());
  }

  // ——— Palabra popover ———

  onWordClick(event: MouseEvent, index: number): void {
    event.stopPropagation();
    this.activeSentenceIndex = null;
    this.activeWordIndex = this.activeWordIndex === index ? null : index;
  }

  closeWordPopover(): void {
    this.activeWordIndex = null;
  }

  addWord(word: string): void {
    console.log('Add word:', word);
    this.closeWordPopover();
  }

  // ——— Frase modal ———

  onSentenceClick(index: number): void {
    this.activeWordIndex = null;
    this.activeSentenceIndex = this.activeSentenceIndex === index ? null : index;
  }

  closeSentenceModal(): void {
    this.activeSentenceIndex = null;
  }

  toggleLike(): void {
    this.liked = !this.liked;
  }

  goBack(): void {
    this.router.navigate(['/texts']);
  }

  // ——— Carga de datos ———

  private init(): void {
    const id = this.activatedRoute.snapshot.params['id'];
    this.getText(id);
    this.getSpanishText(id);
  }

  private getText(id: number): void {
    this.textService.getText(id).subscribe({
      next: (text) => this.text = text,
      error: (err) => console.error('Error loading text', err)
    });
  }

  private getSpanishText(id: number): void {
    this.textService.getSpanishText(id).subscribe({
      next: (data) => {
        this.originalText = data[0];
        this.translatedSpanishText = data[1];
        this.originalTextSeparatedBySentences = this.getSentences(data[0]);
        this.translatedSpanishTextSeparatedBySentences = this.getSentencesString(this.text.spanishTranslation);
        this.getEnglishText(id);
      },
      error: (err) => console.error('Error loading Spanish text', err)
    });
  }

  private getEnglishText(id: number): void {
    this.textService.getEnglishText(id).subscribe({
      next: (data) => {
        this.translatedEnglishText = data[1];
        this.translatedEnglishTextSeparatedBySentences = this.getSentencesString(this.text.englishTranslation);
        this.getWords(data[0]);
      },
      error: (err) => console.error('Error loading English text', err)
    });
  }

  private getWords(chineseText: string[]): void {
    this.wordService.getTextWords(chineseText).subscribe({
      next: (wordsArray) => this.wordsArray = wordsArray,
      error: (err) => console.error('Error loading words', err)
    });
  }

  private getSentences(text: string[]): string[] {
    const sentences: string[] = [];
    let current: string[] = [];
    for (const word of text) {
      current.push(word);
      if (word.endsWith('.') || word.endsWith('。')) {
        sentences.push(current.join(''));
        current = [];
      }
    }
    if (current.length > 0) sentences.push(current.join(''));
    return sentences;
  }

  private getSentencesString(text: string): string[] {
    return text.split(/(?<=\.|。)(?=\s|$)/).filter(s => s.trim() !== '');
  }
}