import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LearnTonesComponent } from './learn-tones.component';
import { translocoTesting } from '../../i18n/transloco-testing';

describe('LearnTonesComponent', () => {
  let component: LearnTonesComponent;
  let fixture: ComponentFixture<LearnTonesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), LearnTonesComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LearnTonesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the 5-row tone table', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.lt2-table tbody tr').length).toBe(5);
  });

  it('starting the quiz shows the first of 10 questions', fakeAsync(() => {
    component.startQuiz();
    tick();  // flush the auto-play timeout

    expect(component.quizPhase).toBe('question');
    expect(component.quizIndex).toBe(1);
    expect(component.current).not.toBeNull();
    expect(component.totalQuestions).toBe(10);
  }));

  it('correct answers score and the quiz ends in a result', fakeAsync(() => {
    component.startQuiz();
    tick();

    for (let i = 0; i < component.totalQuestions; i++) {
      component.answerTone(component.current!.tone);   // always answer right
      tick(900);                                       // feedback delay
      tick();                                          // next question's auto-play timeout
    }

    expect(component.quizPhase).toBe('result');
    expect(component.score).toBe(10);
    expect(component.resultKey).toBe('perfect');
  }));

  it('a wrong answer does not score and repeated clicks are ignored', fakeAsync(() => {
    component.startQuiz();
    tick();

    const wrong = component.current!.tone === 1 ? 2 : 1;
    component.answerTone(wrong);
    component.answerTone(component.current!.tone);     // ignored: already answered

    expect(component.score).toBe(0);
    tick(900);
    tick();
    expect(component.quizIndex).toBe(2);
  }));
});
