import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LearnHubComponent } from './learn-hub.component';
import { translocoTesting } from '../../i18n/transloco-testing';

describe('LearnHubComponent', () => {
  let component: LearnHubComponent;
  let fixture: ComponentFixture<LearnHubComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), LearnHubComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LearnHubComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the 4-step roadmap (3 lessons + level test)', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.lh-step').length).toBe(4);
  });
});
