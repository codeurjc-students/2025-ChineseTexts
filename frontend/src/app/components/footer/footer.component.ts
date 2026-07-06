import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { LocalizeLinkPipe } from '../../i18n/localize-link.pipe';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterModule, TranslocoModule, LocalizeLinkPipe],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {

}
