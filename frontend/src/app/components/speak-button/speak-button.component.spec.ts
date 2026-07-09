import { TestBed, ComponentFixture } from '@angular/core/testing';

import { SpeakButtonComponent } from './speak-button.component';
import { AudioService } from '../../services/audio.service';
import { LoginService } from '../../services/login.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { ToastService } from '../../services/toast.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('SpeakButtonComponent', () => {
  let component: SpeakButtonComponent;
  let fixture: ComponentFixture<SpeakButtonComponent>;
  let audioSpy: jasmine.SpyObj<AudioService>;
  let loginSpy: jasmine.SpyObj<LoginService>;
  let navSpy: jasmine.SpyObj<LocaleNavService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    audioSpy = jasmine.createSpyObj('AudioService', ['speak', 'stop'], {
      state$: { subscribe: () => ({ unsubscribe: () => {} }) }
    });
    loginSpy = jasmine.createSpyObj('LoginService', ['isLogged']);
    loginSpy.isLogged.and.returnValue(true); // logged in by default
    navSpy = jasmine.createSpyObj('LocaleNavService', ['navigate']);
    toastSpy = jasmine.createSpyObj('ToastService', ['show']);

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), SpeakButtonComponent],
      providers: [
        { provide: AudioService, useValue: audioSpy },
        { provide: LoginService, useValue: loginSpy },
        { provide: LocaleNavService, useValue: navSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SpeakButtonComponent);
    component = fixture.componentInstance;
    component.text = '你好';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should ask the AudioService to speak (with the audio type) when idle and go to loading', () => {
    audioSpy.speak.and.returnValue(7);
    component.state = 'idle';
    component.audioType = 'word';
    component.toggle(new Event('click'));
    expect(audioSpy.speak).toHaveBeenCalledWith('你好', 'word');
    expect(component.state).toBe('loading');
  });

  it('should show a sign-up notice for anonymous users instead of playing', () => {
    loginSpy.isLogged.and.returnValue(false);
    component.state = 'idle';
    component.toggle(new Event('click'));
    expect(toastSpy.show).toHaveBeenCalled();
    expect(audioSpy.speak).not.toHaveBeenCalled();
    expect(navSpy.navigate).not.toHaveBeenCalled();
  });

  it('should send users who hit the audio limit to the premium page', () => {
    component.state = 'limitReached';
    component.toggle(new Event('click'));
    expect(navSpy.navigate).toHaveBeenCalledWith(['/premium']);
    expect(audioSpy.speak).not.toHaveBeenCalled();
  });

  it('should stop playback when clicked while playing', () => {
    component.state = 'playing';
    component.toggle(new Event('click'));
    expect(audioSpy.stop).toHaveBeenCalled();
    expect(component.state).toBe('idle');
    expect(audioSpy.speak).not.toHaveBeenCalled();
  });

  it('should expose the right icon per state when logged in', () => {
    component.state = 'idle';    expect(component.icon).toBe('bi-volume-up');
    component.state = 'playing';  expect(component.icon).toBe('bi-volume-up-fill');
    component.state = 'error';    expect(component.icon).toBe('bi-volume-mute');
    component.state = 'limitReached'; expect(component.icon).toBe('bi-star');
  });

  it('should show a lock icon for anonymous visitors', () => {
    loginSpy.isLogged.and.returnValue(false);
    component.state = 'idle';
    expect(component.icon).toBe('bi-lock');
  });
});
