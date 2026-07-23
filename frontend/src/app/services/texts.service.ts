import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** One aligned sentence of a public text (stored server-side at upload). */
export interface TextSentencePair {
  chinese: string;
  english: string;
  spanish: string;
}

/**
 * Closed vocabulary of topic tags, in display order. Keys are the stable
 * identifiers stored in the database; the visible labels live in the i18n
 * dictionaries under topics.*. Keep in sync with TextTopics.ALLOWED in
 * backend/.../Model/TextTopics.java.
 */
export const TEXT_TOPICS = [
  'daily-life', 'family', 'food', 'travel', 'culture', 'history',
  'school', 'work', 'sports', 'games', 'nature', 'animals',
  'science', 'technology', 'health', 'society', 'politics',
  'entertainment'
] as const;

export const MAX_TOPICS_PER_TEXT = 5;

/** Partial metadata edit for PATCH /api/texts/:id (admin). Omitted fields stay unchanged. */
export interface TextMetadataUpdate {
  titleEnglish?: string;
  titleSpanish?: string;
  englishDescription?: string;
  spanishDescription?: string;
  level?: string;
  topics?: string[];
}

export interface TextItem {
  id: number;
  titleEnglish: string;
  titleSpanish: string;
  text: string;
  spanishTranslation: string;
  englishTranslation: string;
  level: string;
  // Topic tag keys (TEXT_TOPICS); absent/empty on texts that were never tagged.
  topics?: string[];
  englishDescription: string;
  spanishDescription: string;
  creationDate: string;
  liked?: boolean;
  // Aligned chinese↔EN↔ES sentences; absent/empty on texts created before
  // they existed (the reader then falls back to the heuristic split).
  sentences?: TextSentencePair[];
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

  getTexts(page: number, size: number, topic?: string): Observable<TextItem[]> {
    const topicParam = topic ? `&topic=${encodeURIComponent(topic)}` : '';
    return this.http.get<TextItem[]>(`${this.apiUrl}?page=${page}&size=${size}${topicParam}`);
  }

  getTextsByLevel(level: string, page: number, size: number, topic?: string): Observable<TextItem[]> {
    const topicParam = topic ? `&topic=${encodeURIComponent(topic)}` : '';
    return this.http.get<TextItem[]>(`${this.apiUrl}/level/${level}?page=${page}&size=${size}${topicParam}`);
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

  /** Admin: partial metadata edit (titles, descriptions, level, topics). */
  updateTextMetadata(id: number, patch: TextMetadataUpdate): Observable<TextItem> {
    return this.http.patch<TextItem>(`${this.apiUrl}/${id}`, patch, { withCredentials: true });
  }

  /** Admin: replace the cover image of a text. */
  updateTextImage(id: number, image: File): Observable<void> {
    const formData = new FormData();
    formData.append('image', image);
    return this.http.put<void>(`${this.apiUrl}/${id}/image`, formData, { withCredentials: true });
  }
}