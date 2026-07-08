import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Talks to the backend PREMIUM (Stripe) endpoints. The backend returns a Stripe-hosted
 * URL that the caller redirects to — the app never handles card data.
 */
@Injectable({ providedIn: 'root' })
export class PremiumService {

  private base = '/api/premium';

  constructor(private http: HttpClient) {}

  /** Starts a subscription checkout; resolves to the Stripe Checkout URL to redirect to. */
  checkout(plan: 'monthly' | 'yearly'): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.base}/checkout`, { plan }, { withCredentials: true });
  }

  /** Opens the Stripe billing portal to manage/cancel; resolves to the portal URL. */
  portal(): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.base}/portal`, {}, { withCredentials: true });
  }
}
