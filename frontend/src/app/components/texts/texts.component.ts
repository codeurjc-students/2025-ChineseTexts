import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TextsService, TextItem } from '../../services/texts.service';
import { LoginService } from '../../services/login.service';

@Component({
  selector: 'app-texts',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './texts.component.html',
  styleUrl: './texts.component.scss'
})
export class TextsComponent implements OnInit {

  texts: TextItem[] = [];
  page = 0;
  size = 2;
  hasMore: boolean = true;
  currentLevel: string | null = null;

  showDeleteModal = false;
  textToDelete: TextItem | null = null;

  constructor(
    private textsService: TextsService,
    private route: ActivatedRoute,
    private loginService: LoginService
  ) {}

  get isAdmin(): boolean {
    return this.loginService.isRoleAdmin();
  }

  ngOnInit(): void {
    // Detecta cambios en la URL (por ejemplo /texts/HSK3)
    this.route.paramMap.subscribe(params => {
      this.currentLevel = params.get('level');

      // Reiniciar estado
      this.page = 0;
      this.texts = [];
      this.hasMore = true;

      if (this.currentLevel) {
        this.loadTextsByLevel(this.currentLevel);
      } else {
        this.loadTexts();
      }
    });
  }

  // Carga general sin filtro
  loadTexts(): void {
    this.textsService.getTexts(this.page, this.size).subscribe({
      next: (data) => {
        this.texts = [...this.texts, ...data];

        if (data.length < this.size) {
          this.hasMore = false;
        }
      },
      error: (err) => console.error('Error loading texts:', err)
    });
  }

  // Carga filtrada por nivel
  loadTextsByLevel(level: string): void {
    this.textsService.getTextsByLevel(level, this.page, this.size).subscribe({
      next: (data) => {
        this.texts = [...this.texts, ...data];

        if (data.length < this.size) {
          this.hasMore = false;
        }
      },
      error: (err) => console.error('Error loading texts by level:', err)
    });
  }

  // Load more respeta si hay nivel o no
  loadMore(): void {
    this.page++;

    if (this.currentLevel) {
      this.loadTextsByLevel(this.currentLevel);
    } else {
      this.loadTexts();
    }
  }

  toggleLike(text: TextItem, event: Event): void {
    event.stopPropagation();
    text.liked = !text.liked;
  }

  openDeleteModal(text: TextItem, event: Event): void {
    event.stopPropagation();
    this.textToDelete = text;
    this.showDeleteModal = true;
  }

  confirmDeleteText(): void {
    if (!this.textToDelete) return;
    this.textsService.deleteText(this.textToDelete.id).subscribe({
      next: () => {
        this.texts = this.texts.filter(t => t.id !== this.textToDelete!.id);
        this.cancelDelete();
      },
      error: (err) => console.error('Error deleting text', err)
    });
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.textToDelete = null;
  }
}
