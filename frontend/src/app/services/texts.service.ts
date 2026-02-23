import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TextItem { 
  id: number; 
  titleEnglish: string; 
  titleSpanish: string; 
  text: string; 
  spanishTranslation: string; 
  englishTranslation: string; 
  level: string; 
  englishDescription: string; 
  spanishDescription: string; 
  creationDate: string; // LocalDate llega como string ISO 
  liked?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TextsService {

  private apiUrl = '/api/texts';

  constructor(private http: HttpClient) {}

  getTexts(page: number, size: number): Observable<TextItem[]> {
    return this.http.get<TextItem[]>(`${this.apiUrl}?page=${page}&size=${size}`);
  }
}
