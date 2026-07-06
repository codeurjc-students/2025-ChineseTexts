import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

}
