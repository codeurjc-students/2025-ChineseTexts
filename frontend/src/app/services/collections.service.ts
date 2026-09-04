import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

export interface CollectionDTO {
  id: number;
  title: string;
  date: string;
}

export interface FlashcardDTO {
  id: number;
  word: {
    id: number;
    chinese: string;
    pinyin: string;
    english: string;
    spanish: string;
  };
  example: any;
  collection: CollectionDTO;
}

/** Result of grading one card with SM-2: when it will come back. */
export interface SrsReviewResult {
  flashcardId: number;
  intervalDays: number;
  nextDue: string;
}

@Injectable({ providedIn: 'root' })
export class CollectionsService {

  private apiUrl = '/api/collections';
  private srsUrl = '/api/flashcards';

  /**
   * Fires after a review session touches the queue, so the header badge can
   * refresh its count without polling.
   */
  readonly reviewsChanged$ = new Subject<void>();

  constructor(private http: HttpClient) {}

  // ————————————————————— SRS (SM-2 spaced repetition) —————————————————————

  /** Every card due today across all the user's collections. */
  getDueFlashcards(): Observable<FlashcardDTO[]> {
    return this.http.get<FlashcardDTO[]>(`${this.srsUrl}/due`, { withCredentials: true });
  }

  /** Just the due-today count (cheap; used by the header badge). */
  getDueCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.srsUrl}/due/count`, { withCredentials: true });
  }

  /** Grades a card: 0 = again, 3 = hard, 4 = good, 5 = easy. */
  reviewFlashcard(flashcardId: number, quality: number): Observable<SrsReviewResult> {
    return this.http.post<SrsReviewResult>(
      `${this.srsUrl}/${flashcardId}/review`,
      { quality },
      { withCredentials: true }
    );
  }

  notifyReviewsChanged(): void {
    this.reviewsChanged$.next();
  }

  getUserCollections(): Observable<CollectionDTO[]> {
    return this.http.get<CollectionDTO[]>(this.apiUrl, { withCredentials: true });
  }

  getCollectionFlashcards(collectionId: number): Observable<FlashcardDTO[]> {
    return this.http.get<FlashcardDTO[]>(`${this.apiUrl}/${collectionId}/flashcards`, { withCredentials: true });
  }

  createCollection(title: string): Observable<CollectionDTO> {
    return this.http.post<CollectionDTO>(`${this.apiUrl}?title=${encodeURIComponent(title)}`, {}, { withCredentials: true });
  }

  addFlashcard(collectionId: number, chinese: string, textId: number): Observable<FlashcardDTO> {
    return this.http.post<FlashcardDTO>(
      `${this.apiUrl}/${collectionId}/flashcards?chinese=${encodeURIComponent(chinese)}&textId=${textId}`,
      {},
      { withCredentials: true }
    );
  }

  deleteCollection(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  deleteFlashcard(flashcardId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/flashcards/${flashcardId}`, { withCredentials: true });
  }

  renameCollection(id: number, title: string): Observable<CollectionDTO> {
    return this.http.patch<CollectionDTO>(
      `${this.apiUrl}/${id}`,
      { title },
      { withCredentials: true }
    );
  }
}