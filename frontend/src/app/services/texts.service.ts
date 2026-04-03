import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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
  creationDate: string;
  liked?: boolean;
}

export interface ValidationResult {
  valid: boolean;
  missingWords: string[];
  segments: string[];
}

@Injectable({ providedIn: 'root' })
export class TextsService {

  private apiUrl = '/api/texts';

  constructor(private http: HttpClient) {}

  getTexts(page: number, size: number): Observable<TextItem[]> {
    return this.http.get<TextItem[]>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  getTextsByLevel(level: string, page: number, size: number): Observable<TextItem[]> {
    return this.http.get<TextItem[]>(`${this.apiUrl}/level/${level}?page=${page}&size=${size}`);
  }

  getText(id: number): Observable<TextItem> {
    return this.http.get<TextItem>(`${this.apiUrl}/${id}`);
  }

  getSpanishText(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}/SpanishText`);
  }

  getEnglishText(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}/EnglishText`);
  }

  validateText(chineseText: string): Observable<ValidationResult> {
    const params = new HttpParams().set('chineseText', chineseText);
    return this.http.post<ValidationResult>(`${this.apiUrl}/validate`, null,
      { params, withCredentials: true });
  }

  uploadText(formData: FormData): Observable<TextItem> {
    return this.http.post<TextItem>(this.apiUrl, formData, { withCredentials: true });
  }

  deleteText(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}