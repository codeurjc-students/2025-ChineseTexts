import { TestBed, ComponentFixture } from '@angular/core/testing';

import { SpeakButtonComponent } from './speak-button.component';
import { AudioService } from '../../services/audio.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('SpeakButtonComponent', () => {
  let component: SpeakButtonComponent;
  let fixture: ComponentFixture<SpeakButtonComponent>;
  let audioSpy: jasmine.SpyObj<AudioService>;

  beforeEach(async () => {
    audioSpy = jasmine.createSpyObj('AudioService', ['speak', 'stop'], {
      state$: { subscribe: () => ({ unsubscribe: () => {} }) }
    });

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), SpeakButtonComponent],
      providers: [{ provide: AudioService, useValue: audioSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(SpeakButtonComponent);
    component = fixture.componentInstance;
    component.text = '你好';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should ask the AudioService to speak when idle and go to loading', () => {
    audioSpy.speak.and.returnValue(7);
    component.state = 'idle';
    component.toggle(new Event('click'));
    expect(audioSpy.speak).toHaveBeenCalledWith('你好');
    expect(component.state).toBe('loading');
  });

  it('should stop playback when clicked while playing', () => {
    component.state = 'playing';
    component.toggle(new Event('click'));
    expect(audioSpy.stop).toHaveBeenCalled();
    expect(component.state).toBe('idle');
    expect(audioSpy.speak).not.toHaveBeenCalled();
  });

  it('should expose the right icon per state', () => {
    component.state = 'idle';    expect(component.icon).toBe('bi-volume-up');
    component.state = 'playing';  expect(component.icon).toBe('bi-volume-up-fill');
    component.state = 'error';    expect(component.icon).toBe('bi-volume-mute');
  });
});
