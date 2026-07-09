import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface WordChatRequest {
  word: string;
  sentence: string;
  text: string;
  translation: string;
  level: string;
  language: string;
  history: ChatMessage[];
}

export interface WordChatResponse {
  reply: string;
  unlimited: boolean;
  remaining: number;
}

/**
 * Talks to the AI contextual word-chat endpoint. The conversation is stateless on the
 * server, so the whole message history is sent on every turn (ephemeral chat).
 */
@Injectable({ providedIn: 'root' })
export class ChatService {

  constructor(private http: HttpClient) {}

  askWord(req: WordChatRequest): Observable<WordChatResponse> {
    return this.http.post<WordChatResponse>('/api/chat/word', req, { withCredentials: true });
  }
}
