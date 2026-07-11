import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

/** One day of the weekly activity chart. */
export interface DayCount {
  day: string;
  count: number;
}

/** The user's progress snapshot (streak, totals, weekly chart). */
export interface Stats {
  currentStreak: number;
  bestStreak: number;
  readToday: boolean;
  textsRead: number;
  wordsSaved: number;
  week: DayCount[];
}

/**
 * Reading-activity API: logs a reading (streak fuel) and fetches the progress stats.
 * `statsChanged$` lets the header refresh its streak flame right after a reading is
 * recorded, without polling.
 */
@Injectable({ providedIn: 'root' })
export class ActivityService {

  private readonly statsChangedSubject = new Subject<void>();
  readonly statsChanged$ = this.statsChangedSubject.asObservable();

  constructor(private http: HttpClient) {}

  /** Fire-and-forget log of a reading; refreshes streak listeners on success. */
  recordReading(textKey: string): void {
    this.http.post('/api/activity/reading', { textKey }, { withCredentials: true }).subscribe({
      next: () => this.statsChangedSubject.next(),
      error: () => { /* stats are non-critical; never disturb the reading */ }
    });
  }

  getStats(): Observable<Stats> {
    return this.http.get<Stats>('/api/activity/stats', { withCredentials: true });
  }
}
