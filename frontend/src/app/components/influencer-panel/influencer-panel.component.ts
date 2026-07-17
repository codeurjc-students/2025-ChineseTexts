import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { InfluencersService, InfluencerCode } from '../../services/influencers.service';

/**
 * Admin tool for influencer campaigns: create/deactivate Stripe discount codes and
 * read the funnel per influencer — signups from their ?ref link, redemptions counted
 * by Stripe, checkout conversions tied by the webhook, and how many of those still
 * hold an active premium. Payment itself never touches this panel: codes are applied
 * on Stripe's hosted Checkout page.
 */
@Component({
  selector: 'app-influencer-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './influencer-panel.component.html',
  styleUrl: './influencer-panel.component.scss'
})
export class InfluencerPanelComponent implements OnInit {

  /** Emitted when the admin wants to go back to the Admin Tools menu. */
  @Output() exit = new EventEmitter<void>();

  codes: InfluencerCode[] = [];
  loading = false;
  creating = false;
  /** Stripe id of the code awaiting deactivation confirmation, if any. */
  confirmingId: string | null = null;
  deactivatingId: string | null = null;

  message = '';
  messageType: 'success' | 'error' | '' = '';

  // Create form
  newCode = '';
  newPercent: number | null = null;
  newDuration: 'once' | 'repeating' | 'forever' = 'once';
  newMonths: number | null = null;

  constructor(
    private influencersService: InfluencersService,
    private transloco: TranslocoService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  /** Base for the shareable links shown in the help box (empty during prerender). */
  get shareBase(): string {
    return typeof window !== 'undefined' ? window.location.origin : '';
  }

  load(): void {
    this.loading = true;
    this.influencersService.getStats().subscribe({
      next: (codes) => {
        this.loading = false;
        this.codes = codes;
      },
      error: (err) => {
        this.loading = false;
        this.showError(this.transloco.translate(err.status === 503
          ? 'influencers.errors.billingUnavailable'
          : 'influencers.errors.loadFailed'));
      }
    });
  }

  get formComplete(): boolean {
    return !!(this.newCode.trim() && this.newPercent != null &&
      (this.newDuration !== 'repeating' || this.newMonths != null));
  }

  create(): void {
    this.clearFeedback();
    if (!this.formComplete) return;
    this.creating = true;
    this.influencersService.createCode({
      code: this.newCode.trim(),
      percentOff: this.newPercent as number,
      duration: this.newDuration,
      durationInMonths: this.newDuration === 'repeating' ? this.newMonths : null
    }).subscribe({
      next: (created) => {
        this.creating = false;
        this.showSuccess(this.transloco.translate('influencers.messages.created',
          { code: created.code }));
        this.newCode = '';
        this.newPercent = null;
        this.newDuration = 'once';
        this.newMonths = null;
        this.load();
      },
      error: (err) => {
        this.creating = false;
        const codeMap: Record<string, string> = {
          INVALID_CODE: 'influencers.errors.invalidCode',
          INVALID_PERCENT: 'influencers.errors.invalidPercent',
          INVALID_DURATION: 'influencers.errors.invalidDuration',
          INVALID_MONTHS: 'influencers.errors.invalidMonths',
          CODE_EXISTS: 'influencers.errors.codeExists',
          BILLING_UNAVAILABLE: 'influencers.errors.billingUnavailable'
        };
        this.showError(this.transloco.translate(
          codeMap[err.error?.code] || 'influencers.errors.createFailed'));
      }
    });
  }

  askDeactivate(id: string): void {
    this.clearFeedback();
    this.confirmingId = id;
  }

  cancelDeactivate(): void {
    this.confirmingId = null;
  }

  confirmDeactivate(id: string): void {
    this.confirmingId = null;
    this.deactivatingId = id;
    this.influencersService.deactivateCode(id).subscribe({
      next: () => {
        this.deactivatingId = null;
        this.showSuccess(this.transloco.translate('influencers.messages.deactivated'));
        this.load();
      },
      error: () => {
        this.deactivatingId = null;
        this.showError(this.transloco.translate('influencers.errors.deactivateFailed'));
      }
    });
  }

  /** Human summary of the coupon terms, e.g. "20% · 3 months". */
  durationLabel(code: InfluencerCode): string {
    if (code.duration === 'forever') {
      return this.transloco.translate('influencers.durations.forever');
    }
    if (code.duration === 'repeating') {
      return this.transloco.translate('influencers.durations.months',
        { months: code.durationInMonths });
    }
    return this.transloco.translate('influencers.durations.once');
  }

  goBack(): void {
    this.exit.emit();
  }

  private clearFeedback(): void {
    this.message = '';
    this.messageType = '';
  }

  private showError(msg: string): void {
    this.message = msg;
    this.messageType = 'error';
  }

  private showSuccess(msg: string): void {
    this.message = msg;
    this.messageType = 'success';
  }
}
