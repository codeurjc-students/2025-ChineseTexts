import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Location } from '@angular/common';
import { UserService, UserDTO } from '../../services/users.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent {

  signupForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private location: Location,
    private userService: UserService,
    private router: Router
  ) {
    this.signupForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      language: ['en', Validators.required]
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
        console.log('User registered:', response);
        this.router.navigate(['/success'], {
          queryParams: { msg: 'Your account has been created successfully!' }
        });
      },
      error: (err: any) => {
        console.error('Error registering user:', err);

        // Extraer mensaje del backend
        const msg =
          err.error?.message ||
          err.error ||
          'An unexpected error occurred.';

        // Redirigir a /error con el mensaje
        this.router.navigate(['/error'], {
          queryParams: { msg }
        });
      }
    });
  }


  goBack() {
    this.location.back();
  }
}
