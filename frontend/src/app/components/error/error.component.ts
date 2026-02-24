import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss']
})
export class ErrorComponent {

  errorMessage = 'An unexpected error occurred.';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private meta: Meta,
    private title: Title
  ) {
    // SEO
    this.title.setTitle('Error | Page Not Found or Unavailable');
    this.meta.updateTag({
      name: 'description',
      content: 'The page you are looking for cannot be found or is temporarily unavailable. Return to ChineseReads to continue learning Chinese through reading.'
    });

    // Recibir mensaje dinámico desde query params
    this.route.queryParams.subscribe(params => {
      if (params['msg']) {
        this.errorMessage = params['msg'];
      }
    });
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
