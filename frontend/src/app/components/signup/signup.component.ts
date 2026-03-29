import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Location } from '@angular/common';
import { UserService, UserDTO } from '../../services/users.service';
import { LoginService } from '../../services/login.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {

  signupForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private location: Location,
    private userService: UserService,
    private loginService: LoginService,
    private router: Router
  ) {
    this.signupForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      language: ['en', Validators.required]
    });
  }

  ngOnInit(): void {
    // Si ya está logueado, redirige a home
    if (this.loginService.isLogged()) {
      this.router.navigate(['/']);
      return;
    }

    // Si inicia sesión mientras está en signup, redirige a home
    this.loginService.loggedIn$.subscribe(isLogged => {
      if (isLogged) this.router.navigate(['/']);
    });
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
      newPassword: null
    };

    this.userService.register(userDTO).subscribe({
      next: (response: UserDTO) => {
        this.router.navigate(['/success'], {
          queryParams: { msg: 'Your account has been created successfully!' }
        });
      },
      error: (err: any) => {
        const msg = err.error?.message || err.error || 'An unexpected error occurred.';
        this.router.navigate(['/error'], { queryParams: { msg } });
      }
    });
  }

  goBack() {
    this.location.back();
  }
}