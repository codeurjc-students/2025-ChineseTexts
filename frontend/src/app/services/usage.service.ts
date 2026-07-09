import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** The current user's generation usage, for the meter on the My Tools page. */
export interface UsageStatus {
  plan: 'free' | 'premium' | 'admin';
  unlimited: boolean;
  used: number;
  limit: number;
}

/** Reads the authenticated user's own usage from the backend. */
@Injectable({ providedIn: 'root' })
export class UsageService {

  constructor(private http: HttpClient) {}

  getStatus(): Observable<UsageStatus> {
    return this.http.get<UsageStatus>('/api/usage/me', { withCredentials: true });
  }
}
