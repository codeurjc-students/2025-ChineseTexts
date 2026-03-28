import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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

@Injectable({ providedIn: 'root' })
export class CollectionsService {

  private apiUrl = '/api/collections';

  constructor(private http: HttpClient) {}

  getUserCollections(): Observable<CollectionDTO[]> {
    return this.http.get<CollectionDTO[]>(this.apiUrl, { withCredentials: true });
  }

  getCollectionFlashcards(collectionId: number): Observable<FlashcardDTO[]> {
    return this.http.get<FlashcardDTO[]>(`${this.apiUrl}/${collectionId}/flashcards`, { withCredentials: true });
  }

  createCollection(title: string): Observable<CollectionDTO> {
    return this.http.post<CollectionDTO>(`${this.apiUrl}?title=${title}`, {}, { withCredentials: true });
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
}