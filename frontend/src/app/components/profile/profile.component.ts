import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LoginService } from '../../services/login.service';
import { UserService, UserDTO } from '../../services/users.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { ActivityService, Stats } from '../../services/activity.service';

type ProfileSection = 'view' | 'editProfile' | 'editPassword' | 'deleteAccount';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  user: UserDTO | null = null;
  section: ProfileSection = 'view';

  // Editar perfil
  editName = '';
  editLanguage = '';
  profileStatus: 'idle' | 'saving' | 'success' | 'error' = 'idle';
  profileError = '';

  // Cambiar contraseña
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordStatus: 'idle' | 'checking' | 'saving' | 'success' | 'error' = 'idle';
  passwordError = '';

  // Borrar cuenta
  deleteConfirmText = '';
  deleteStatus: 'idle' | 'deleting' | 'error' = 'idle';
  deleteError = '';

  /** Reading progress (streak, totals, weekly chart); null until loaded. */
  stats: Stats | null = null;

  languages = [
    { value: 'en', label: 'English' },
    { value: 'es', label: 'Spanish' }
  ];

  constructor(
    private loginService: LoginService,
    private userService: UserService,
    private router: Router,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService,
    private activity: ActivityService
  ) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) {
          this.localeNav.navigate(['/']);
        } else {
          this.user = user;
          this.loadStats();
        }
      },
      error: () => this.localeNav.navigate(['/'])
    });
  }


  /** Loads the progress card; failures just hide it (non-critical). */
  private loadStats(): void {
    this.activity.getStats().subscribe({
      next: (s) => this.stats = s,
      error: () => this.stats = null
    });
  }

  /** Bar height (%) for the weekly chart, scaled to the busiest day. */
  barHeight(count: number): number {
    if (!this.stats || count === 0) return 0;
    const max = Math.max(...this.stats.week.map(d => d.count), 1);
    return Math.round((count / max) * 100);
  }

  /** Localized short weekday label ("mon"/"lun") for a chart day. */
  dayLabel(day: string): string {
    const idx = new Date(day + 'T00:00:00').getDay(); // 0 = Sunday
    const keys = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
    return this.transloco.translate('profile.progress.days.' + keys[idx]);
  }

  get languageLabel(): string {
    const lang = this.languages.find(l => l.value === this.user?.language);
    return lang ? this.transloco.translate('profile.languages.' + lang.value) : '';
  }

  /** True while the user's PREMIUM subscription is active (not yet expired). */
  get isPremium(): boolean {
    const until = this.user?.premiumUntil;
    return !!until && new Date(until).getTime() > Date.now();
  }

  /** Opens the premium plan page (upgrade or manage subscription). */
  goToPremium(): void {
    this.localeNav.navigate(['/premium']);
  }

  // ——— Editar perfil ———

  openEditProfile(): void {
    this.editName = this.user?.name ?? '';
    this.editLanguage = this.user?.language ?? 'en';
    this.profileStatus = 'idle';
    this.profileError = '';
    this.section = 'editProfile';
  }

  get profileFormValid(): boolean {
    return this.editName.trim().length > 0;
  }

  saveProfile(): void {
    this.profileError = '';
    this.profileStatus = 'idle';

    if (!this.editName.trim()) {
      this.profileError = this.transloco.translate('profile.errors.nameEmpty');
      this.profileStatus = 'error';
      return;
    }

    this.profileStatus = 'saving';

    this.userService.updateProfile({
      name: this.editName.trim(),
      language: this.editLanguage
    }).subscribe({
      next: (updated) => {
        this.user = updated;
        this.loginService.reqIsLogged().subscribe();
        this.profileStatus = 'success';
        setTimeout(() => this.section = 'view', 1200);
      },
      error: () => {
        this.profileStatus = 'error';
        this.profileError = this.transloco.translate('profile.errors.updateFailed');
      }
    });
  }

  // ——— Cambiar contraseña ———

  openEditPassword(): void {
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.passwordStatus = 'idle';
    this.passwordError = '';
    this.section = 'editPassword';
  }

  get passwordFormValid(): boolean {
    return this.currentPassword.length > 0 &&
          this.newPassword.length >= 6 &&
          this.newPassword === this.confirmPassword;
  }

  savePassword(): void {
    this.passwordError = '';
    this.passwordStatus = 'idle';

    if (!this.currentPassword.trim()) {
      this.passwordError = this.transloco.translate('profile.errors.currentPasswordEmpty');
      this.passwordStatus = 'error';
      return;
    }

    if (this.newPassword.length < 6) {
      this.passwordError = this.transloco.translate('profile.errors.newPasswordTooShort');
      this.passwordStatus = 'error';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = this.transloco.translate('profile.errors.newPasswordsMismatch');
      this.passwordStatus = 'error';
      return;
    }

    if (this.newPassword === this.currentPassword) {
      this.passwordError = this.transloco.translate('profile.errors.newPasswordSameAsCurrent');
      this.passwordStatus = 'error';
      return;
    }

    this.passwordStatus = 'checking';

    this.userService.checkPassword(this.currentPassword).subscribe({
      next: () => {
        this.passwordStatus = 'saving';
        this.userService.changePassword(this.newPassword).subscribe({
          next: () => {
            this.passwordStatus = 'success';
            setTimeout(() => this.section = 'view', 1200);
          },
          error: () => {
            this.passwordStatus = 'error';
            this.passwordError = this.transloco.translate('profile.errors.changePasswordFailed');
          }
        });
      },
      error: () => {
        this.passwordStatus = 'error';
        this.passwordError = this.transloco.translate('profile.errors.currentPasswordIncorrect');
      }
    });
  }

  // ——— Borrar cuenta ———

  openDeleteAccount(): void {
    this.deleteConfirmText = '';
    this.deleteStatus = 'idle';
    this.deleteError = '';
    this.section = 'deleteAccount';
  }

  get deleteConfirmValid(): boolean {
    return this.deleteConfirmText.trim().toUpperCase() === 'DELETE';
  }

  confirmDeleteAccount(): void {
    if (!this.deleteConfirmValid) {
      this.deleteError = this.transloco.translate('profile.errors.typeDeleteToConfirm');
      this.deleteStatus = 'error';
      return;
    }
    this.deleteError = '';
    this.deleteStatus = 'deleting';

    this.userService.deleteOwnAccount().subscribe({
      next: () => {
        // Clear the (now orphaned) session and go home.
        this.loginService.logout().subscribe({
          next: () => this.localeNav.navigate(['/']),
          error: () => this.localeNav.navigate(['/'])
        });
      },
      error: () => {
        this.deleteStatus = 'error';
        this.deleteError = this.transloco.translate('profile.errors.deleteFailed');
      }
    });
  }

  cancel(): void {
    this.section = 'view';
  }
}