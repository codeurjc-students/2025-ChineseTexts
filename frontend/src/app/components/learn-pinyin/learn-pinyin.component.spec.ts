import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LearnPinyinComponent } from './learn-pinyin.component';
import { translocoTesting } from '../../i18n/transloco-testing';
import { ALL_PINYIN_SOUNDS, CONSONANT_GROUPS } from '../../data/learn-pinyin';

describe('LearnPinyinComponent', () => {
  let component: LearnPinyinComponent;
  let fixture: ComponentFixture<LearnPinyinComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), LearnPinyinComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LearnPinyinComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the four sound tables with one row (and one audio button) per sound', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.lp-table').length).toBe(4);
    const rows = el.querySelectorAll('.lp-table tbody tr');
    expect(rows.length).toBe(ALL_PINYIN_SOUNDS.length);
    // every row plays its own file; +1 for the nǐ hǎo syllable demo
    expect(el.querySelectorAll('app-audio-button').length).toBe(ALL_PINYIN_SOUNDS.length + 1);
  });

  it('separates the consonant chart into the school groups', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.lp-table--grouped tbody').length).toBe(CONSONANT_GROUPS.length);
  });

  it('shows the three spelling rules', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.lp-rule').length).toBe(3);
  });
});
