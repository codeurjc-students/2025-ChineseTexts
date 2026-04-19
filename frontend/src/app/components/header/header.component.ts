import { CommonModule } from '@angular/common';
import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { LoginService } from '../../services/login.service';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {

  loginEmail = '';
  loginPassword = '';
  messageError = '';
  emailError = '';
  passwordError = '';
  loginAttempted = false;
  isLoggedIn$: Observable<boolean>;
  authReady = false;

  constructor(
    public loginService: LoginService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isLoggedIn$ = this.loginService.loggedIn$;
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      // Esperamos a que el LoginService haya verificado el estado real
      this.loginService.loggedIn$.subscribe(() => {
        this.authReady = true;
      });
    } else {
      // En SSR no mostramos nada condicionado al login
      this.authReady = false;
    }
  }

  public login() {
    this.loginAttempted = true;
    this.emailError = '';
    this.passwordError = '';
    this.messageError = '';

    let valid = true;

    if (!this.loginEmail.trim()) {
      this.emailError = 'Email is required.';
      valid = false;
    } else if (!this.isValidEmail(this.loginEmail)) {
      this.emailError = 'Please enter a valid email address.';
      valid = false;
    }

    if (!this.loginPassword.trim()) {
      this.passwordError = 'Password is required.';
      valid = false;
    }

    if (!valid) return;

    this.loginService.login(this.loginEmail, this.loginPassword).subscribe({
      next: () => {
        this.loginService.reqIsLogged().subscribe({
          next: () => {
            this.loginEmail = '';
            this.loginPassword = '';
            this.loginAttempted = false;
            this.emailError = '';
            this.passwordError = '';
            this.messageError = '';
            const modalEl = document.getElementById('loginModal');
            if (modalEl) {
              const modal = (window as any).bootstrap?.Modal?.getInstance(modalEl);
              modal?.hide();
            }
            document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('overflow');
            document.body.style.removeProperty('padding-right');
          }
        });
      },
      error: () => {
        this.loginEmail = '';
        this.loginPassword = '';
        this.loginAttempted = false;
        this.messageError = 'Incorrect credentials. Please try again.';
      }
    });
  }

  public logout(): void {
    this.loginService.logout().subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (error) => {
        console.error('Error en logout:', error);
      }
    });
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}