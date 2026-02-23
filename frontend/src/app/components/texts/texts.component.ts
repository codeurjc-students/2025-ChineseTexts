import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TextsService, TextItem } from '../../services/texts.service';

@Component({
  selector: 'app-texts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './texts.component.html',
  styleUrl: './texts.component.scss'
})
export class TextsComponent implements OnInit {

  texts: TextItem[] = [];
  page = 0;
  size = 2;
  hasMore: boolean = true;

  constructor(private textsService: TextsService) {}

  ngOnInit(): void {
    this.loadTexts();
  }

  loadTexts(): void {
    this.textsService.getTexts(this.page, this.size).subscribe({
      next: (data) => {
        this.texts = [...this.texts, ...data];

        // 👇 Si el backend devuelve menos de "size", ya no hay más páginas
        if (data.length < this.size) {
          this.hasMore = false;
        }
      },
      error: (err) => {
        console.error('Error loading texts:', err);
      }
    });
  }

  loadMore(): void {
    this.page++;   // siguiente página
    this.loadTexts();
  }

  toggleLike(text: TextItem, event: Event): void {
    event.stopPropagation();
    text.liked = !text.liked;
  }
}
