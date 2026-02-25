import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { LoginService } from '../../services/login.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {

    loginEmail = '';
    loginPassword = '';
    messageError = '';

    constructor(
      public loginService: LoginService, // Cambiado a público para acceder desde la plantilla
      private router: Router,
    ) {}

    public login() {
      if (this.loginEmail && this.loginPassword) {
        this.loginService.login(this.loginEmail, this.loginPassword).subscribe(
        () => {
          this.loginService.reqIsLogged().subscribe(
            () => window.location.reload()// Cerrar el formulario de inicio de sesión
          )
        },
        () => {
          //this.modalService.open(this.loginErrorModal, { centered: true });
          this.loginEmail = '';
          this.loginPassword = '';
          this.messageError = '"Incorrect credentials. Please try again."'
        });
      } else {
        this.messageError = 'Please fill in all fields.';
      }
    }

    public logout(): void {
      this.loginService.logout().subscribe({
        next: () => {
          // Aquí el logout ya se completó
          this.router.navigate(['/']);
          console.log(this.loginService.isLogged()); // Debe mostrar false
        },
        error: (error) => {
          console.error("Error en logout:", error);
        }
      });
    }

}
