import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AudioButtonComponent } from './audio-button.component';

describe('AudioButtonComponent', () => {
  let component: AudioButtonComponent;
  let fixture: ComponentFixture<AudioButtonComponent>;
  let audioStub: { pause: jasmine.Spy; play: jasmine.Spy; onended: (() => void) | null; onerror: (() => void) | null };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AudioButtonComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(AudioButtonComponent);
    component = fixture.componentInstance;
    component.src = 'assets/audio/learn/ma1.mp3';
    fixture.detectChanges();

    audioStub = {
      pause: jasmine.createSpy('pause'),
      play: jasmine.createSpy('play').and.returnValue(Promise.resolve()),
      onended: null,
      onerror: null
    };
    spyOn(window as any, 'Audio').and.returnValue(audioStub);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('plays the given asset on click and resets when playback ends', () => {
    component.play();

    expect((window as any).Audio).toHaveBeenCalledWith('assets/audio/learn/ma1.mp3');
    expect(audioStub.play).toHaveBeenCalled();
    expect(component.playing).toBeTrue();

    audioStub.onended!();
    expect(component.playing).toBeFalse();
  });

  it('fails silently when the file is missing', () => {
    component.play();
    audioStub.onerror!();
    expect(component.playing).toBeFalse();
  });
});
