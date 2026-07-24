import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';

import { HeartBurstComponent, LIKE_PHRASES } from './heart-burst.component';

import { translocoTesting } from '../../i18n/transloco-testing';

describe('HeartBurstComponent', () => {
  let component: HeartBurstComponent;
  let fixture: ComponentFixture<HeartBurstComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), HeartBurstComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(HeartBurstComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Test 1: sin interacción no hay nada en el DOM (el prerender no ve nada)
  it('should render nothing until a burst happens', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(component.particles.length).toBe(0);
    expect(component.phraseBox.length).toBe(0);
    expect(el.querySelector('.heart-burst-overlay')).toBeNull();
    expect(el.querySelector('.phrase-card')).toBeNull();
  });

  // Test 2: burst crea corazones y una frase del catálogo, y se pintan
  it('should spawn hearts and a catalog phrase on burst', fakeAsync(() => {
    component.burst(100, 200);
    fixture.detectChanges();

    expect(component.particles.length).toBe(12);
    expect(component.phraseBox.length).toBe(1);
    expect(LIKE_PHRASES).toContain(component.phraseBox[0].phrase);

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.heart-particle').length).toBe(12);
    expect(el.querySelector('.phrase-chinese')?.textContent)
      .toContain(component.phraseBox[0].phrase.chinese);

    tick(4000);
  }));

  // Test 3: al acabar la animación todo se limpia solo
  it('should clean up hearts and phrase after the animation', fakeAsync(() => {
    component.burst(100, 200);
    tick(4000);
    fixture.detectChanges();

    expect(component.particles.length).toBe(0);
    expect(component.phraseBox.length).toBe(0);
    expect(fixture.nativeElement.querySelector('.heart-burst-overlay')).toBeNull();
  }));

  // Test 4: dos pulsaciones seguidas nunca repiten frase
  it('should not repeat the same phrase twice in a row', fakeAsync(() => {
    for (let i = 0; i < 10; i++) {
      component.burst(0, 0);
      const first = component.phraseBox[0].phrase;
      component.burst(0, 0);
      expect(component.phraseBox[0].phrase).not.toBe(first);
    }
    tick(4000);
  }));

  // Test 5: activación por teclado (clientX/Y = 0) usa el centro del botón
  it('should fall back to the button center on keyboard activation', fakeAsync(() => {
    const button = document.createElement('button');
    spyOn(button, 'getBoundingClientRect').and.returnValue(
      { left: 40, top: 60, width: 20, height: 10 } as DOMRect
    );
    const event = { clientX: 0, clientY: 0, target: button } as unknown as MouseEvent;

    component.burstFromEvent(event);

    expect(component.particles[0].x).toBe(50);
    expect(component.particles[0].y).toBe(65);
    tick(4000);
  }));
});
