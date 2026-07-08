import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import {
  UserService,
  AdminUserSummary,
  AdminUserDetail
} from '../../services/users.service';

/**
 * Admin tool to manage registered users. Two views:
 *  - 'list':   searchable, paginated ("load more") list of users.
 *  - 'detail': one user's full profile + stats, with block/unblock, edit
 *              (name / language / admin role) and permanent delete actions.
 *
 * Admins cannot block, delete or de-admin their OWN account (both the UI and the
 * backend guard against it), so they can't lock themselves out of the panel.
 */
type View = 'list' | 'detail';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent implements OnInit {

  /** Emitted when the admin wants to go back to the Admin Tools menu. */
  @Output() exit = new EventEmitter<void>();

  view: View = 'list';

  // ——— List state ———
  users: AdminUserSummary[] = [];
  searchTerm = '';
  page = 0;
  hasMore = true;
  listLoading = false;

  // ——— Detail state ———
  detail: AdminUserDetail | null = null;
  detailLoading = false;
  editing = false;
  editName = '';
  editLanguage = '';
  confirmingDelete = false;
  editingPremium = false;
  premiumInput = ''; // datetime-local value (YYYY-MM-DDTHH:mm)

  message = '';
  messageType: 'success' | 'error' | '' = '';

  constructor(
    private userService: UserService,
    private loginService: LoginService,
    private transloco: TranslocoService
  ) {}

  ngOnInit(): void {
    this.loadUsers(true);
  }

  // ——————————————————————————— List ———————————————————————————

  loadUsers(reset: boolean): void {
    if (reset) {
      this.page = 0;
      this.users = [];
      this.hasMore = true;
    }
    this.listLoading = true;
    this.userService.getUsers(this.searchTerm.trim(), this.page, PAGE_SIZE).subscribe({
      next: (batch) => {
        this.listLoading = false;
        this.users = [...this.users, ...batch];
        this.hasMore = batch.length === PAGE_SIZE;
      },
      error: () => {
        this.listLoading = false;
        this.showError(this.transloco.translate('adminUsers.errors.loadUsers'));
      }
    });
  }

  search(): void {
    this.clearFeedback();
    this.loadUsers(true);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.clearFeedback();
    this.loadUsers(true);
  }

  loadMore(): void {
    this.page++;
    this.loadUsers(false);
  }

  // —————————————————————————— Detail ——————————————————————————

  openDetail(id: number): void {
    this.clearFeedback();
    this.editing = false;
    this.confirmingDelete = false;
    this.editingPremium = false;
    this.detailLoading = true;
    this.view = 'detail';
    this.userService.getUserDetail(id).subscribe({
      next: (d) => {
        this.detailLoading = false;
        this.detail = d;
      },
      error: (err) => {
        this.detailLoading = false;
        this.detail = null;
        this.showError(err.status === 404
          ? this.transloco.translate('adminUsers.errors.userGone')
          : this.transloco.translate('adminUsers.errors.loadUser'));
      }
    });
  }

  backToList(): void {
    this.view = 'list';
    this.detail = null;
    this.editing = false;
    this.confirmingDelete = false;
    this.editingPremium = false;
    this.clearFeedback();
    // Refresh the list so any change (block/delete/role) is reflected.
    this.loadUsers(true);
  }

  /** True when the shown user is the admin currently logged in. */
  get isSelf(): boolean {
    return !!this.detail && this.detail.email === this.loginService.currentUser()?.email;
  }

  get isTargetAdmin(): boolean {
    return !!this.detail?.roles.includes('ADMIN');
  }

  /** True when the shown user currently has an active (non-expired) PREMIUM subscription. */
  get isTargetPremium(): boolean {
    return this.isActivePremium(this.detail?.premiumUntil);
  }

  /** Whether an ISO premiumUntil string represents an active (future) subscription. */
  isActivePremium(premiumUntil?: string | null): boolean {
    return !!premiumUntil && new Date(premiumUntil).getTime() > Date.now();
  }

  // ——— Block / unblock ———

  toggleBlocked(): void {
    if (!this.detail || this.isSelf) return;
    const next = !this.detail.blocked;
    this.clearFeedback();
    this.detailLoading = true;
    this.userService.setUserBlocked(this.detail.id, next).subscribe({
      next: (updated) => {
        this.detailLoading = false;
        if (this.detail) this.detail.blocked = updated.blocked;
        this.showSuccess(this.transloco.translate(next ? 'adminUsers.messages.blocked' : 'adminUsers.messages.unblocked'));
      },
      error: () => {
        this.detailLoading = false;
        this.showError(this.transloco.translate('adminUsers.errors.updateUser'));
      }
    });
  }

  // ——— Admin role ———

  toggleAdmin(): void {
    if (!this.detail || this.isSelf) return;
    const roles = this.isTargetAdmin ? ['USER'] : ['USER', 'ADMIN'];
    this.saveRoles(roles, this.transloco.translate(this.isTargetAdmin ? 'adminUsers.messages.adminRemoved' : 'adminUsers.messages.adminGranted'));
  }

  private saveRoles(roles: string[], successMsg: string): void {
    if (!this.detail) return;
    this.clearFeedback();
    this.detailLoading = true;
    this.userService.updateUser(this.detail.id, { roles }).subscribe({
      next: (d) => {
        this.detailLoading = false;
        this.detail = d;
        this.showSuccess(successMsg);
      },
      error: () => {
        this.detailLoading = false;
        this.showError(this.transloco.translate('adminUsers.errors.updateUser'));
      }
    });
  }

  // ——— Premium subscription ———

  /** Opens the grant form, pre-filling the datetime with the current expiry or +30 days. */
  startPremiumEdit(): void {
    if (!this.detail) return;
    this.clearFeedback();
    const base = this.isTargetPremium && this.detail.premiumUntil
      ? new Date(this.detail.premiumUntil)
      : new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // default: 30 days from now
    this.premiumInput = this.toDatetimeLocal(base);
    this.editingPremium = true;
  }

  cancelPremiumEdit(): void {
    this.editingPremium = false;
  }

  grantPremium(): void {
    if (!this.detail) return;
    if (!this.premiumInput) {
      this.showError(this.transloco.translate('adminUsers.errors.premiumDateEmpty'));
      return;
    }
    if (new Date(this.premiumInput).getTime() <= Date.now()) {
      this.showError(this.transloco.translate('adminUsers.errors.premiumDatePast'));
      return;
    }
    this.savePremium(this.premiumInput,
      this.transloco.translate('adminUsers.messages.premiumGranted'));
  }

  revokePremium(): void {
    this.savePremium(null, this.transloco.translate('adminUsers.messages.premiumRevoked'));
  }

  private savePremium(premiumUntil: string | null, successMsg: string): void {
    if (!this.detail) return;
    this.clearFeedback();
    this.detailLoading = true;
    this.userService.setUserPremium(this.detail.id, premiumUntil).subscribe({
      next: (d) => {
        this.detailLoading = false;
        this.detail = d;
        this.editingPremium = false;
        this.showSuccess(successMsg);
      },
      error: () => {
        this.detailLoading = false;
        this.showError(this.transloco.translate('adminUsers.errors.updatePremium'));
      }
    });
  }

  /** Formats a Date as a `YYYY-MM-DDTHH:mm` string for a native datetime-local input. */
  private toDatetimeLocal(d: Date): string {
    const pad = (n: number) => `${n}`.padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
      + `T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  // ——— Edit name / language ———

  startEdit(): void {
    if (!this.detail) return;
    this.clearFeedback();
    this.editName = this.detail.name;
    this.editLanguage = this.detail.language;
    this.editing = true;
  }

  cancelEdit(): void {
    this.editing = false;
  }

  saveEdit(): void {
    if (!this.detail) return;
    if (!this.editName.trim()) {
      this.showError(this.transloco.translate('adminUsers.errors.nameEmpty'));
      return;
    }
    this.clearFeedback();
    this.detailLoading = true;
    this.userService.updateUser(this.detail.id, {
      name: this.editName.trim(),
      language: this.editLanguage.trim()
    }).subscribe({
      next: (d) => {
        this.detailLoading = false;
        this.detail = d;
        this.editing = false;
        this.showSuccess(this.transloco.translate('adminUsers.messages.changesSaved'));
      },
      error: () => {
        this.detailLoading = false;
        this.showError(this.transloco.translate('adminUsers.errors.saveChanges'));
      }
    });
  }

  // ——— Delete ———

  askDelete(): void {
    if (this.isSelf) return;
    this.message = '';
    this.messageType = '';
    this.confirmingDelete = true;
  }

  cancelDelete(): void {
    this.confirmingDelete = false;
  }

  confirmDelete(): void {
    if (!this.detail || this.isSelf) return;
    const removed = this.detail.email;
    this.confirmingDelete = false;
    this.detailLoading = true;
    this.userService.deleteUser(this.detail.id).subscribe({
      next: () => {
        this.detailLoading = false;
        this.backToList();
        this.showSuccess(this.transloco.translate('adminUsers.messages.deleted', { email: removed }));
      },
      error: () => {
        this.detailLoading = false;
        this.showError(this.transloco.translate('adminUsers.errors.deleteUser'));
      }
    });
  }

  // ——— Helpers ———

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
