import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { InfluencersService, InfluencerCode, Settlements } from '../../services/influencers.service';

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
  /**
   * Anti-"discount farming" guard, ON by default: the code only works for customers
   * with no previous payment, so cancel+resubscribe cannot re-redeem a first-payment
   * discount every cycle. Untick only for deliberate win-back campaigns.
   */
  newFirstTimeOnly = true;

  // ——— Settlements (commission per collected charge) ———
  /** 'month' = natural-month shortcut; 'range' = free from/to day pickers. */
  settleMode: 'month' | 'range' = 'month';
  settleMonth = '';
  settleFrom = '';
  settleTo = '';
  settlements: Settlements | null = null;
  settleLoading = false;
  settleError = '';
  /** Influencer code whose customer detail list is currently expanded, if any. */
  expandedCode: string | null = null;

  constructor(
    private influencersService: InfluencersService,
    private transloco: TranslocoService
  ) {}

  ngOnInit(): void {
    this.load();
    // Default the settlement pickers to the current month, ready to consult.
    const now = new Date();
    this.settleMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
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
      durationInMonths: this.newDuration === 'repeating' ? this.newMonths : null,
      firstTimeOnly: this.newFirstTimeOnly
    }).subscribe({
      next: (created) => {
        this.creating = false;
        this.showSuccess(this.transloco.translate('influencers.messages.created',
          { code: created.code }));
        this.newCode = '';
        this.newPercent = null;
        this.newDuration = 'once';
        this.newMonths = null;
        this.newFirstTimeOnly = true;
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

  // ——— Settlements ———

  /**
   * Resolves the selected period to inclusive ISO dates. The month picker is just a
   * shortcut that fills the same from/to pair the free range mode edits directly.
   */
  settleRange(): { from: string; to: string } | null {
    if (this.settleMode === 'month') {
      if (!/^\d{4}-\d{2}$/.test(this.settleMonth)) return null;
      const [year, month] = this.settleMonth.split('-').map(Number);
      const lastDay = new Date(year, month, 0).getDate();
      return {
        from: `${this.settleMonth}-01`,
        to: `${this.settleMonth}-${String(lastDay).padStart(2, '0')}`
      };
    }
    if (!this.settleFrom || !this.settleTo || this.settleFrom > this.settleTo) return null;
    return { from: this.settleFrom, to: this.settleTo };
  }

  get settleRangeValid(): boolean {
    return this.settleRange() !== null;
  }

  /** True while the selected period includes today — its numbers are still growing. */
  get settlePeriodOpen(): boolean {
    if (!this.settlements) return false;
    return this.settlements.to >= new Date().toISOString().slice(0, 10);
  }

  get settleTotalCents(): number {
    return (this.settlements?.rows ?? []).reduce((sum, r) => sum + r.payoutCents, 0);
  }

  loadSettlements(): void {
    const range = this.settleRange();
    if (!range) return;
    this.settleLoading = true;
    this.settleError = '';
    this.expandedCode = null;
    this.influencersService.getSettlements(range.from, range.to).subscribe({
      next: (settlements) => {
        this.settleLoading = false;
        this.settlements = settlements;
      },
      error: (err) => {
        this.settleLoading = false;
        this.settlements = null;
        this.settleError = this.transloco.translate(err.error?.code === 'INVALID_RANGE'
          ? 'influencers.settlements.errors.invalidRange'
          : 'influencers.settlements.errors.loadFailed');
      }
    });
  }

  toggleExpand(code: string): void {
    this.expandedCode = this.expandedCode === code ? null : code;
  }

  euros(cents: number): string {
    return (cents / 100).toFixed(2) + ' €';
  }

  statusLabel(status: string): string {
    return this.transloco.translate('influencers.settlements.status.' + status);
  }

  /**
   * Downloads the current settlement as CSV: one summary line per influencer followed
   * by the nominal customer detail. Generated client-side from the loaded data — the
   * numbers are exactly what the table shows.
   */
  exportCsv(): void {
    if (!this.settlements) return;
    const t = (key: string) => this.transloco.translate('influencers.settlements.csv.' + key);
    const esc = (value: unknown) => {
      const s = value == null ? '' : String(value);
      return /[",;\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
    };
    const lines: string[] = [];
    lines.push([t('period'), this.settlements.from, this.settlements.to].join(';'));
    lines.push('');
    lines.push([t('code'), t('newCustomers'), t('renewals'), t('churned'), t('active'),
      t('monthlyCharges'), t('yearlyCharges'), t('payout')].join(';'));
    for (const row of this.settlements.rows) {
      lines.push([esc(row.code), row.newCustomers, row.renewals, row.churned, row.active,
        row.monthlyCharges, row.yearlyCharges, (row.payoutCents / 100).toFixed(2)].join(';'));
    }
    lines.push('');
    lines.push([t('code'), t('customer'), t('plan'), t('statusCol'), t('charges'),
      t('payout'), t('firstPaidOn'), t('lastPaidOn'), t('coveredUntil')].join(';'));
    for (const row of this.settlements.rows) {
      for (const c of row.customers) {
        lines.push([esc(row.code), esc(c.username), c.plan, this.statusLabel(c.status),
          c.charges, (c.payoutCents / 100).toFixed(2), c.firstPaidOn, c.lastPaidOn,
          c.coveredUntil].join(';'));
      }
    }
    // BOM so Excel opens the accents correctly.
    const blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `chinesereads-influencers-${this.settlements.from}_${this.settlements.to}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
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
