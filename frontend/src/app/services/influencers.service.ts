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

/** One customer inside a settlement row (status is relative to the requested period). */
export interface SettlementCustomer {
  userId: number | null;
  username: string | null;
  plan: 'monthly' | 'yearly' | string | null;
  status: 'new' | 'renewal' | 'churned' | 'active';
  charges: number;
  payoutCents: number;
  firstPaidOn: string;
  lastPaidOn: string;
  coveredUntil: string;
}

/** One influencer's commission settlement for the requested period. */
export interface SettlementRow {
  code: string;
  newCustomers: number;
  renewals: number;
  churned: number;
  active: number;
  monthlyCharges: number;
  yearlyCharges: number;
  payoutCents: number;
  customers: SettlementCustomer[];
}

export interface Settlements {
  from: string;
  to: string;
  payoutMonthlyCents: number;
  payoutYearlyCents: number;
  rows: SettlementRow[];
}

/** Signups per acquisition source; source is null for organic/direct signups. */
export interface MetricsSourceRow {
  source: string | null;
  signups: number;
}

/**
 * Activation metrics of the signup cohort registered in the range: did new users
 * read on day 1, save words, come back within a week and hold premium today.
 * D7 retention is judged only on `measurableD7` users (day-7 window elapsed).
 */
export interface ActivationMetrics {
  from: string;
  to: string;
  signups: number;
  activatedDay1: number;
  savedWord: number;
  measurableD7: number;
  retainedD7: number;
  activePremium: number;
  bySource: MetricsSourceRow[];
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

  /**
   * Commission settlement for an inclusive date range (ISO dates). Computed on demand
   * from the immutable payment ledger, so any past period returns identical results.
   */
  getSettlements(from: string, to: string): Observable<Settlements> {
    return this.http.get<Settlements>(`${this.apiUrl}/settlements`,
      { params: { from, to }, withCredentials: true });
  }

  /** Activation metrics of the signup cohort of an inclusive date range (local DB only). */
  getMetrics(from: string, to: string): Observable<ActivationMetrics> {
    return this.http.get<ActivationMetrics>(`${this.apiUrl}/metrics`,
      { params: { from, to }, withCredentials: true });
  }
}
