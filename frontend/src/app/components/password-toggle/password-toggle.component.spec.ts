import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PasswordToggleComponent } from './password-toggle.component';
import { translocoTesting } from '../../i18n/transloco-testing';

describe('PasswordToggleComponent', () => {
  let component: PasswordToggleComponent;
  let fixture: ComponentFixture<PasswordToggleComponent>;
  let input: HTMLInputElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [translocoTesting(), PasswordToggleComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordToggleComponent);
    component = fixture.componentInstance;
    input = document.createElement('input');
    input.type = 'password';
    input.value = 'secret-1234';
    component.target = input;
    fixture.detectChanges();
  });

  const button = () => fixture.nativeElement.querySelector('button') as HTMLButtonElement;

  // Test 1: el estado inicial oculta la contraseña y anuncia "mostrar"
  it('starts hidden with an accessible "show" label and never submits the form', () => {
    expect(input.type).toBe('password');
    expect(button().getAttribute('type')).toBe('button');
    expect(button().getAttribute('aria-label')).toBe('Show password');
    expect(button().getAttribute('aria-pressed')).toBe('false');
  });

  // Test 2: un clic muestra el texto sin tocar el valor; otro lo vuelve a ocultar
  it('reveals the password on click and hides it again on the next click, keeping the value', () => {
    button().click();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(input.value).toBe('secret-1234');
    expect(button().getAttribute('aria-label')).toBe('Hide password');
    expect(button().getAttribute('aria-pressed')).toBe('true');

    button().click();
    fixture.detectChanges();
    expect(input.type).toBe('password');
    expect(button().getAttribute('aria-label')).toBe('Show password');
  });
});
