import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AiTextResult {
  chineseText: string;
  titleEnglish: string;
  titleSpanish: string;
  englishTranslation: string;
  spanishTranslation: string;
  englishDescription: string;
  spanishDescription: string;
  missingWords: string[];
  missingWordSuggestions: {
    chinese: string;
    pinyin: string;
    english: string;
    spanish: string;
  }[];
}

@Injectable({ providedIn: 'root' })
export class AiService {

  private apiUrl = '/api/ai';

  constructor(private http: HttpClient) {}

  generateText(level: string): Observable<AiTextResult> {
    return this.http.post<AiTextResult>(
      `${this.apiUrl}/generate?level=${level}`,
      {},
      { withCredentials: true }
    );
  }

  processOcr(image: File): Observable<AiTextResult> {
    const formData = new FormData();
    formData.append('image', image);
    return this.http.post<AiTextResult>(
      `${this.apiUrl}/ocr`,
      formData,
      { withCredentials: true }
    );
  }
}