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

@Injectable({
    providedIn: 'root'
})
export class WordsService {
    private apiUrl = '/api/words';

    constructor(private http: HttpClient) {}

    getTextWords(originalText: string[]): Observable<Word[]> {
      // Convertimos el array originalText a una cadena separada por comas
      const params = new HttpParams().set('text', originalText.join(','));
      return this.http.get<Word[]>(`${this.apiUrl}/textWords`, { params });
    }

}