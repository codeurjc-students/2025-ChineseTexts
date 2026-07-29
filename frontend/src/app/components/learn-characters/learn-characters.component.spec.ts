import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LearnCharactersComponent } from './learn-characters.component';
import { translocoTesting } from '../../i18n/transloco-testing';

describe('LearnCharactersComponent', () => {
  let component: LearnCharactersComponent;
  let fixture: ComponentFixture<LearnCharactersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), LearnCharactersComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LearnCharactersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the 12 starter character cards', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.lc-card').length).toBe(12);
  });
});
