import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/** A button shown in a toast (e.g. "Log in", "Create free account"). */
export interface ToastAction {
  label: string;
  run: () => void;
}

/** A transient notice; optional actions render as buttons. */
export interface ToastMessage {
  text: string;
  actions: ToastAction[];
}

/**
 * Lightweight app-wide toast/notice. Any component calls `show(...)`; a single
 * `<app-toast>` (rendered once in AppComponent) displays it. Used to nudge anonymous
 * users toward logging in / registering without yanking them off the page they're on.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {

  private readonly subject = new Subject<ToastMessage>();
  readonly messages$ = this.subject.asObservable();

  show(text: string, actions: ToastAction[] = []): void {
    this.subject.next({ text, actions });
  }
}
