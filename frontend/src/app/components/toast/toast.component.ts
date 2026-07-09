import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

import { ToastMessage, ToastService } from '../../services/toast.service';

/**
 * Renders the current app toast (see ToastService). Auto-dismisses after a few
 * seconds; an optional action button runs the callback and closes. Text/labels are
 * passed already-localized by the caller, so this component needs no i18n.
 */
@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.scss'
})
export class ToastComponent implements OnInit, OnDestroy {

  current: ToastMessage | null = null;

  private sub?: Subscription;
  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor(private toast: ToastService) {}

  ngOnInit(): void {
    this.sub = this.toast.messages$.subscribe(msg => this.display(msg));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.clearTimer();
  }

  private display(msg: ToastMessage): void {
    this.current = msg;
    this.clearTimer();
    this.timer = setTimeout(() => this.dismiss(), 6000);
  }

  runAction(): void {
    this.current?.action?.();
    this.dismiss();
  }

  dismiss(): void {
    this.current = null;
    this.clearTimer();
  }

  private clearTimer(): void {
    if (this.timer) { clearTimeout(this.timer); this.timer = null; }
  }
}
