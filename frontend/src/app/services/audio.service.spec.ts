import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AudioService, SpeakState } from './audio.service';

describe('AudioService', () => {
  let service: AudioService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AudioService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AudioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST the text to /api/tts and emit loading', () => {
    const states: SpeakState[] = [];
    const id = service.speak('你好');
    service.state$.subscribe(e => { if (e.id === id) states.push(e.state); });

    const req = httpMock.expectOne('/api/tts');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ text: '你好', type: 'phrase' });
    req.flush(new Blob(['x'], { type: 'audio/mpeg' }));
  });

  it('should not call the backend for blank text', () => {
    service.speak('   ');
    httpMock.expectNone('/api/tts');
  });
});
