import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Subject } from 'rxjs';

import { MyTextReaderComponent } from './my-text-reader.component';
import { AudioService, SpeakState } from '../../services/audio.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('MyTextReaderComponent', () => {
  let component: MyTextReaderComponent;
  let fixture: ComponentFixture<MyTextReaderComponent>;
  let httpMock: HttpTestingController;
  // Stub AudioService exposing its streams as Subjects so tests can drive playback.
  let audioState$: Subject<{ id: number; state: SpeakState }>;
  let audioProgress$: Subject<{ id: number; ratio: number }>;

  beforeEach(async () => {
    audioState$ = new Subject();
    audioProgress$ = new Subject();
    const audioStub: Partial<AudioService> = {
      state$: audioState$.asObservable(),
      progress$: audioProgress$.asObservable(),
      speak: () => 1,
      stop: () => {}
    };

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), MyTextReaderComponent],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: AudioService, useValue: audioStub }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyTextReaderComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    // Drena peticiones pendientes (LoginService consulta /api/users/me al arrancar).
    httpMock.match(() => true).forEach(r => r.flush(null, { status: 200, statusText: 'OK' }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows pinyin and translation by default', () => {
    expect(component.showPinyin).toBeTrue();
    expect(component.showTranslation).toBeTrue();
  });

  // Lector privado y público comparten la misma regla de disposición de la
  // traducción (utils/sentence.util): con saltos reales entre frases, las
  // líneas de la traducción reflejan las del original; sin saltos, una frase
  // por línea (comportamiento de siempre).
  function readerWith(newlineAfterSecond: boolean) {
    return {
      id: 1, title: 'Diálogo', text: '你好！很高兴。\n再见。',
      englishTranslation: 'Hello! Nice to meet you. Goodbye.',
      spanishTranslation: '¡Hola! Encantado. Adiós.',
      creationDate: '2026-07-16',
      words: [
        { chinese: '你好！', pinyin: '', english: '', spanish: '', newlineAfter: false },
        { chinese: '很高兴。', pinyin: '', english: '', spanish: '', newlineAfter: newlineAfterSecond },
        { chinese: '再见。', pinyin: '', english: '', spanish: '', newlineAfter: false },
      ],
      sentences: [
        { chinese: '你好！', english: 'Hello!', spanish: '¡Hola!' },
        { chinese: '很高兴。', english: 'Nice to meet you.', spanish: 'Encantado.' },
        { chinese: '再见。', english: 'Goodbye.', spanish: 'Adiós.' },
      ],
    };
  }

  it('mirrors the original lines in the translation when the text has layout', () => {
    component.reader = readerWith(true);
    expect(component.displayTranslation).toBe('Hello! Nice to meet you.\nGoodbye.');
  });

  it('keeps one sentence per line when the text has no layout', () => {
    component.reader = readerWith(false);
    expect(component.displayTranslation).toBe('Hello!\nNice to meet you.\nGoodbye.');
  });

  // Karaoke: same behaviour as the public reader — the word being spoken is
  // highlighted while the FULL-TEXT audio plays, and only for OUR playback id.
  it('highlights the estimated word during full-text playback and clears when it ends', () => {
    fixture.detectChanges(); // subscribes to the audio streams in ngOnInit
    component.reader = readerWith(false);
    component.originalText = component.reader.words.map(w => w.chinese);

    component.onFullTextPlayback(42);
    audioProgress$.next({ id: 42, ratio: 0.05 });
    expect(component.karaokeIndex).toBe(0);   // start → 你好！

    audioProgress$.next({ id: 42, ratio: 0.9 });
    expect(component.karaokeIndex).toBe(2);   // end of audio → 再见。

    audioProgress$.next({ id: 7, ratio: 0.2 }); // someone else's playback
    expect(component.karaokeIndex).toBe(2);

    audioState$.next({ id: 42, state: 'idle' });
    expect(component.karaokeIndex).toBe(-1);
  });
});
