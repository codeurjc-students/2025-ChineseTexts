import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CollectionsService, CollectionDTO, FlashcardDTO } from '../../services/collections.service';
import { LoginService } from '../../services/login.service';

type AppMode = 'list' | 'detail' | 'study' | 'exam';
type CardFace = 'chinese' | 'translation';

interface ExamQuestion {
  flashcard: FlashcardDTO;
  questionType: 'chinese' | 'pinyin' | 'translation';
  options: string[];
  correctAnswer: string;
  selectedAnswer: string | null;
}

@Component({
  selector: 'app-collections',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './collections.component.html',
  styleUrl: './collections.component.scss'
})
export class CollectionsComponent implements OnInit {

  mode: AppMode = 'list';
  collections: CollectionDTO[] = [];
  selectedCollection: CollectionDTO | null = null;
  flashcards: FlashcardDTO[] = [];
  loading = false;

  selectedCollectionIds: Set<number> = new Set();
  allFlashcardsForStudy: FlashcardDTO[] = [];

  // ——— Estudio ———
  studyCards: FlashcardDTO[] = [];
  studyIndex = 0;
  studyFace: CardFace = 'chinese';
  studyRevealed = false;
  studyCorrect = 0;
  studyIncorrect = 0;
  studyDone = false;
  studyNoWordsError = false;

  // ——— Examen ———
  examQuestions: ExamQuestion[] = [];
  examIndex = 0;
  examDone = false;
  examScore = 0;
  examAnswered = false;
  examMinWordsError = false;

  // ——— Modal confirmación borrar ———
  showDeleteModal = false;
  deleteTarget: CollectionDTO | null = null;

  // ——— Modal nueva colección ———
  showAddModal = false;
  newCollectionTitle = '';
  addingCollection = false;

  constructor(
    private collectionsService: CollectionsService,
    private loginService: LoginService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) this.router.navigate(['/']);
        else this.loadCollections();
      },
      error: () => this.router.navigate(['/'])
    });
  }

  loadCollections(): void {
    this.loading = true;
    this.collectionsService.getUserCollections().subscribe({
      next: (cols) => { this.collections = cols; this.loading = false; },
      error: () => this.loading = false
    });
  }

  selectCollection(col: CollectionDTO): void {
    this.selectedCollection = col;
    this.mode = 'detail';
    this.loading = true;
    this.collectionsService.getCollectionFlashcards(col.id).subscribe({
      next: (cards) => { this.flashcards = cards; this.loading = false; },
      error: () => this.loading = false
    });
  }

  openAddModal(): void {
    this.newCollectionTitle = '';
    this.showAddModal = true;
  }

  closeAddModal(): void {
    this.showAddModal = false;
    this.newCollectionTitle = '';
  }

  confirmAddCollection(): void {
    if (!this.newCollectionTitle.trim()) return;
    this.addingCollection = true;
    this.collectionsService.createCollection(this.newCollectionTitle.trim()).subscribe({
      next: (col) => {
        this.collections.push(col);
        this.addingCollection = false;
        this.closeAddModal();
      },
      error: () => this.addingCollection = false
    });
  }

  deleteCollection(col: CollectionDTO, event: Event): void {
    event.stopPropagation();
    this.deleteTarget = col;
    this.showDeleteModal = true;
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    const col = this.deleteTarget;
    this.collectionsService.deleteCollection(col.id).subscribe({
      next: () => {
        this.collections = this.collections.filter(c => c.id !== col.id);
        this.selectedCollectionIds.delete(col.id);
        if (this.selectedCollection?.id === col.id) this.backToList();
        this.cancelDelete();
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.deleteTarget = null;
  }

  deleteFlashcard(card: FlashcardDTO, event: Event): void {
    event.stopPropagation();
    this.collectionsService.deleteFlashcard(card.id).subscribe({
      next: () => this.flashcards = this.flashcards.filter(f => f.id !== card.id)
    });
  }

  backToList(): void {
    this.mode = 'list';
    this.selectedCollection = null;
    this.flashcards = [];
    this.selectedCollectionIds.clear();
  }

  backToDetail(): void {
    this.mode = 'detail';
    this.studyDone = false;
    this.examDone = false;
  }

  toggleCollectionSelection(id: number): void {
    if (this.selectedCollectionIds.has(id)) this.selectedCollectionIds.delete(id);
    else this.selectedCollectionIds.add(id);
    this.studyNoWordsError = false;
    this.examMinWordsError = false;
  }

  isSelected(id: number): boolean {
    return this.selectedCollectionIds.has(id);
  }

  get canStartStudyOrExam(): boolean {
    return this.selectedCollectionIds.size > 0;
  }

  private async loadAllSelectedFlashcards(): Promise<FlashcardDTO[]> {
    const all: FlashcardDTO[] = [];
    for (const id of this.selectedCollectionIds) {
      const cards = await new Promise<FlashcardDTO[]>((resolve, reject) => {
        this.collectionsService.getCollectionFlashcards(id).subscribe({ next: resolve, error: reject });
      });
      all.push(...cards);
    }
    return all;
  }

  // ——— ESTUDIO ———

  async startStudy(): Promise<void> {
    this.studyNoWordsError = false;
    this.loading = true;
    try {
      const all = await this.loadAllSelectedFlashcards();
      if (all.length === 0) {
        this.studyNoWordsError = true;
        this.loading = false;
        return;
      }
      this.studyCards = this.shuffle(all);
      this.studyIndex = 0;
      this.studyFace = Math.random() < 0.5 ? 'chinese' : 'translation';
      this.studyRevealed = false;
      this.studyCorrect = 0;
      this.studyIncorrect = 0;
      this.studyDone = false;
      this.mode = 'study';
    } finally {
      this.loading = false;
    }
  }

  get currentStudyCard(): FlashcardDTO { return this.studyCards[this.studyIndex]; }
  get studyProgress(): number { return Math.round((this.studyIndex / this.studyCards.length) * 100); }

  revealStudyCard(): void { this.studyRevealed = true; }
  studyAgain(): void { this.startStudy(); }
  markKnown(): void { this.studyCorrect++; this.nextStudyCard(); }
  markUnknown(): void { this.studyIncorrect++; this.nextStudyCard(); }

  private nextStudyCard(): void {
    if (this.studyIndex < this.studyCards.length - 1) {
      this.studyIndex++;
      this.studyFace = Math.random() < 0.5 ? 'chinese' : 'translation';
      this.studyRevealed = false;
    } else {
      this.studyDone = true;
    }
  }

  // ——— EXAMEN ———

  async startExam(): Promise<void> {
    this.examMinWordsError = false;
    this.loading = true;
    try {
      const all = await this.loadAllSelectedFlashcards();
      if (all.length < 4) {
        this.examMinWordsError = true;
        this.loading = false;
        return;
      }
      const shuffled = this.shuffle(all);
      const sample = shuffled.slice(0, Math.min(10, shuffled.length));
      this.examQuestions = sample.map(card => this.buildQuestion(card, all));
      this.examIndex = 0;
      this.examDone = false;
      this.examScore = 0;
      this.examAnswered = false;
      this.mode = 'exam';
    } finally {
      this.loading = false;
    }
  }

  private buildQuestion(card: FlashcardDTO, pool: FlashcardDTO[]): ExamQuestion {
    const types: Array<'chinese' | 'pinyin' | 'translation'> = ['chinese', 'pinyin', 'translation'];
    const questionType = types[Math.floor(Math.random() * types.length)];
    let correctAnswer: string;
    let getOption: (f: FlashcardDTO) => string;

    if (questionType === 'chinese') { correctAnswer = card.word.chinese; getOption = f => f.word.chinese; }
    else if (questionType === 'pinyin') { correctAnswer = card.word.pinyin; getOption = f => f.word.pinyin; }
    else { correctAnswer = card.word.english; getOption = f => f.word.english; }

    const distractors = this.shuffle(pool.filter(f => f.id !== card.id)).slice(0, 3).map(getOption);
    const options = this.shuffle([correctAnswer, ...distractors]);
    return { flashcard: card, questionType, options, correctAnswer, selectedAnswer: null };
  }

  get currentQuestion(): ExamQuestion { return this.examQuestions[this.examIndex]; }
  get examProgress(): number { return Math.round((this.examIndex / this.examQuestions.length) * 100); }

  selectExamAnswer(answer: string): void {
    if (this.examAnswered) return;
    this.currentQuestion.selectedAnswer = answer;
    this.examAnswered = true;
    if (answer === this.currentQuestion.correctAnswer) this.examScore++;
  }

  nextExamQuestion(): void {
    if (this.examIndex < this.examQuestions.length - 1) { this.examIndex++; this.examAnswered = false; }
    else this.examDone = true;
  }

  repeatExam(): void { this.startExam(); }
  newExam(): void { this.mode = 'list'; this.selectedCollectionIds.clear(); }

  getOptionClass(option: string): string {
    if (!this.examAnswered) return '';
    if (option === this.currentQuestion.correctAnswer) return 'option--correct';
    if (option === this.currentQuestion.selectedAnswer) return 'option--wrong';
    return 'option--dim';
  }

  getQuestionPrompt(q: ExamQuestion): string {
    if (q.questionType === 'chinese') return `What is the correct character for "${q.flashcard.word.english}"?`;
    if (q.questionType === 'pinyin') return `What is the correct pinyin for "${q.flashcard.word.chinese}"?`;
    return `What does "${q.flashcard.word.chinese}" (${q.flashcard.word.pinyin}) mean?`;
  }

  get examGrade(): string {
    const pct = this.examScore / this.examQuestions.length;
    if (pct === 1) return 'Perfect!';
    if (pct >= 0.8) return 'Great job!';
    if (pct >= 0.6) return 'Not bad!';
    if (pct >= 0.4) return 'Keep practicing!';
    return 'Keep studying!';
  }

  private shuffle<T>(arr: T[]): T[] {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }
}