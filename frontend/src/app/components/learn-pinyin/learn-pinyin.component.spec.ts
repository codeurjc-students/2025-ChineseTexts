import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LearnPinyinComponent } from './learn-pinyin.component';
import { translocoTesting } from '../../i18n/transloco-testing';
import { SAMPLE_INITIALS, SAMPLE_FINALS } from '../../data/learn-pinyin';

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

  it('renders one row per sample sound', () => {
    const el: HTMLElement = fixture.nativeElement;
    const rows = el.querySelectorAll('.lp-table tbody tr');
    expect(rows.length).toBe(SAMPLE_INITIALS.length + SAMPLE_FINALS.length);
    expect(el.querySelectorAll('.lp-pitfall').length).toBe(4);
  });
});
