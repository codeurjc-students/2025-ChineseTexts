import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';

/**
 * Show/hide button for a password field. It flips the type of the input it is
 * given between "password" and "text", nothing else — the form (reactive or
 * template-driven), its validators and its value are untouched, so it can be
 * dropped next to any existing password input:
 *
 *   <div class="password-field">
 *     <input #pwd type="password" formControlName="password">
 *     <app-password-toggle [target]="pwd"></app-password-toggle>
 *   </div>
 *
 * The `.password-field` wrapper (global styles) positions the button inside the
 * input and reserves room for it. Letting people see what they typed is the
 * modern replacement for a "repeat your password" field: one field, no typos.
 */
@Component({
  selector: 'app-password-toggle',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  templateUrl: './password-toggle.component.html',
  styleUrl: './password-toggle.component.scss'
})
export class PasswordToggleComponent {
  /** The password input this button controls. */
  @Input({ required: true }) target!: HTMLInputElement;

  visible = false;

  toggle(): void {
    this.visible = !this.visible;
    this.target.type = this.visible ? 'text' : 'password';
  }
}
