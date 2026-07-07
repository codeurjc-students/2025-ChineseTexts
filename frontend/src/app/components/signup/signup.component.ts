import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Location } from '@angular/common';
import { UserService, UserDTO } from '../../services/users.service';
import { LoginService } from '../../services/login.service';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { LocaleNavService } from '../../i18n/locale-nav.service';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit, OnDestroy {

  signupForm: FormGroup;
  private loginSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private location: Location,
    private userService: UserService,
    private loginService: LoginService,
    private router: Router,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {
    this.signupForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email, this.strictEmailValidator]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      language: ['en', Validators.required],
      // GDPR: must actively accept the terms of use to register (unchecked by default).
      acceptTerms: [false, Validators.requiredTrue]
    });
  }

  ngOnInit(): void {
    if (this.loginService.isLogged()) {
      this.localeNav.navigate(['/']);
      return;
    }

    this.loginSub = this.loginService.loggedIn$.subscribe(isLogged => {
      if (isLogged) this.localeNav.navigate(['/']);
    });
  }

  ngOnDestroy(): void {
    this.loginSub?.unsubscribe();
  }

  submitSignup() {
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    const userDTO: UserDTO = {
      id: null,
      email: this.signupForm.value.email,
      name: this.signupForm.value.name,
      language: this.signupForm.value.language,
      collections: [],
      roles: ['USER'],
      password: this.signupForm.value.password,
      newPassword: null,
      // Consent is validated server-side too; sent from the required checkbox.
      termsAccepted: this.signupForm.value.acceptTerms
    };

    this.userService.register(userDTO).subscribe({
      next: (response: UserDTO) => {
        this.localeNav.navigate(['/success'], {
          queryParams: { msg: this.transloco.translate('signup.successMessage') }
        });
      },
      error: (err: any) => {
        // Prefer the backend's stable error "code"; fall back to the HTTP STATUS, which
        // survives even where the JSON error body is replaced by a generic one in prod.
        const codeMap: Record<string, string> = {
          EMAIL_IN_USE: 'signup.errors.emailInUse',
          INVALID_EMAIL: 'signup.errors.invalidEmail',
          NAME_REQUIRED: 'signup.errors.nameRequired',
          PASSWORD_TOO_SHORT: 'signup.errors.passwordTooShort',
          TERMS_NOT_ACCEPTED: 'signup.errors.termsNotAccepted'
        };
        let key = codeMap[err.error?.code];
        if (!key && err?.status === 409) key = 'signup.errors.emailInUse';
        const msg = this.transloco.translate(key || 'signup.errorGeneric');
        this.localeNav.navigate(['/error'], { queryParams: { msg } });
      }
    });
  }

  goBack() {
    this.location.back();
  }

  private strictEmailValidator(control: any) {
    const value = control.value;
    if (!value) return null;
    const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    return valid ? null : { invalidEmail: true };
  }
}