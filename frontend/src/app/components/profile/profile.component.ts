import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { LoginService } from '../../services/login.service';
import { UserService, UserDTO } from '../../services/users.service';

type ProfileSection = 'view' | 'editProfile' | 'editPassword';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
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

  languages = [
    { value: 'en', label: 'English' },
    { value: 'es', label: 'Spanish' }
  ];

  constructor(
    private loginService: LoginService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user) {
          this.router.navigate(['/']);
        } else {
          this.user = user;
        }
      },
      error: () => this.router.navigate(['/'])
    });
  }

  get languageLabel(): string {
    return this.languages.find(l => l.value === this.user?.language)?.label ?? '';
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
      this.profileError = 'Name cannot be empty.';
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
        this.profileError = 'Error updating profile. Please try again.';
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
      this.passwordError = 'Please enter your current password.';
      this.passwordStatus = 'error';
      return;
    }

    if (this.newPassword.length < 6) {
      this.passwordError = 'New password must be at least 6 characters.';
      this.passwordStatus = 'error';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = 'New passwords do not match.';
      this.passwordStatus = 'error';
      return;
    }

    if (this.newPassword === this.currentPassword) {
      this.passwordError = 'New password must be different from the current one.';
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
            this.passwordError = 'Error changing password. Please try again.';
          }
        });
      },
      error: () => {
        this.passwordStatus = 'error';
        this.passwordError = 'Current password is incorrect.';
      }
    });
  }

  cancel(): void {
    this.section = 'view';
  }
}