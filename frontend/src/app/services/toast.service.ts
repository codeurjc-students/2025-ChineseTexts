import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/** A transient notice; an optional action renders as a button (e.g. "Sign up"). */
export interface ToastMessage {
  text: string;
  actionLabel?: string;
  action?: () => void;
}

/**
 * Lightweight app-wide toast/notice. Any component calls `show(...)`; a single
 * `<app-toast>` (rendered once in AppComponent) displays it. Used to nudge anonymous
 * users toward registration without yanking them off the page they're on.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {

  private readonly subject = new Subject<ToastMessage>();
  readonly messages$ = this.subject.asObservable();

  show(text: string, actionLabel?: string, action?: () => void): void {
    this.subject.next({ text, actionLabel, action });
  }
}
