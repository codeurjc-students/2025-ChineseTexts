import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Word {
  id?: number;
  chinese: string;
  pinyin: string;
  english: string;
  spanish: string;
}

@Injectable({ providedIn: 'root' })
export class WordsService {

  private apiUrl = '/api/words';

  constructor(private http: HttpClient) {}

  getTextWords(originalText: string[]): Observable<Word[]> {
    const params = new HttpParams().set('text', originalText.join('|'));
    return this.http.get<Word[]>(`${this.apiUrl}/textWords`, { params });
  }

  saveWord(word: Word): Observable<Word> {
    return this.http.post<Word>(this.apiUrl, word, { withCredentials: true });
  }

  /** Admin: look up a single word by its Chinese characters (404 if it does not exist). */
  getDictionaryWord(chinese: string): Observable<Word> {
    const params = new HttpParams().set('chinese', chinese);
    return this.http.get<Word>(`${this.apiUrl}/dictionary`, { params, withCredentials: true });
  }

  /** Admin: update an existing word (pinyin/english/spanish). */
  updateWord(id: number, word: Word): Observable<Word> {
    return this.http.put<Word>(`${this.apiUrl}/${id}`, word, { withCredentials: true });
  }

  /** Admin: delete a word (409 if it is still used by flashcards). */
  deleteWord(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}