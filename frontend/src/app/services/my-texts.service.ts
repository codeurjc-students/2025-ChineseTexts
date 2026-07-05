import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** One of the user's private texts, as shown in the "My Tools" list. */
export interface UserTextSummary {
  id: number;
  title: string;
  creationDate: string;
  wordCount: number;
}

export interface UserTextWord {
  chinese: string;
  pinyin: string;
  english: string;
  spanish: string;
}

/** Everything the private reader needs to render one of the user's texts. */
export interface UserTextReader {
  id: number;
  title: string;
  text: string;
  englishTranslation: string;
  spanishTranslation: string;
  creationDate: string;
  words: UserTextWord[];
}

@Injectable({ providedIn: 'root' })
export class MyTextsService {

  private apiUrl = '/api/my-texts';

  constructor(private http: HttpClient) {}

  list(): Observable<UserTextSummary[]> {
    return this.http.get<UserTextSummary[]>(this.apiUrl, { withCredentials: true });
  }

  get(id: number): Observable<UserTextReader> {
    return this.http.get<UserTextReader>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  /** Creates a private text from pasted Chinese text. */
  createFromText(text: string): Observable<UserTextSummary> {
    const form = new FormData();
    form.append('text', text);
    return this.http.post<UserTextSummary>(this.apiUrl, form, { withCredentials: true });
  }

  /** Creates a private text from an uploaded image (OCR). */
  createFromImage(image: File): Observable<UserTextSummary> {
    const form = new FormData();
    form.append('image', image);
    return this.http.post<UserTextSummary>(this.apiUrl, form, { withCredentials: true });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
