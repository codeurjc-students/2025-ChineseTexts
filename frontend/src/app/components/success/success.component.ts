import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';

@Component({
  selector: 'app-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './success.component.html',
  styleUrls: ['./success.component.scss']
})
export class SuccessComponent {

  successMessage = 'Action completed successfully.';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private meta: Meta,
    private title: Title
  ) {
    // SEO
    this.title.setTitle('Success | Your Premium Access is Active');
    this.meta.updateTag({
      name: 'description',
      content: 'Your ChineseReads Premium subscription is now active. Start learning Chinese through reading with instant translations, AI tools, and personalized statistics.'
    });

    // Recibir mensaje dinámico desde query params
    this.route.queryParams.subscribe(params => {
      if (params['msg']) {
        this.successMessage = params['msg'];
      }
    });
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
