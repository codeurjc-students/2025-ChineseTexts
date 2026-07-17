import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * One row of the influencer tracking table. Stripe-side fields (id, active,
 * percentOff, ...) are null for codes that only exist as ?ref sources.
 */
export interface InfluencerCode {
  id: string | null;
  code: string;
  active: boolean | null;
  percentOff: number | null;
  duration: string | null;
  durationInMonths: number | null;
  /** True when restricted to first-time customers (anti-"discount farming" guard). */
  firstTimeOnly: boolean | null;
  timesRedeemed: number | null;
  signups: number;
  conversions: number;
  activePremium: number;
}

export interface InfluencerCreateRequest {
  code: string;
  percentOff: number;
  duration: 'once' | 'repeating' | 'forever';
  durationInMonths: number | null;
  /** Default true: only customers with no previous payment can redeem the code. */
  firstTimeOnly: boolean;
}

/** ADMIN-only API for influencer discount codes and their tracking stats. */
@Injectable({ providedIn: 'root' })
export class InfluencersService {

  private apiUrl = '/api/influencers';

  constructor(private http: HttpClient) {}

  getStats(): Observable<InfluencerCode[]> {
    return this.http.get<InfluencerCode[]>(this.apiUrl, { withCredentials: true });
  }

  createCode(request: InfluencerCreateRequest): Observable<InfluencerCode> {
    return this.http.post<InfluencerCode>(this.apiUrl, request, { withCredentials: true });
  }

  /** Deactivates the code in Stripe (history preserved; it can never be redeemed again). */
  deactivateCode(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }
}
