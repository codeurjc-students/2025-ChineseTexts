import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { LoginService } from '../../services/login.service';
import { UploadTextComponent } from '../upload-text/upload-text.component';
import { AiToolsComponent } from '../ai-tools/ai-tools.component';
import { WordManagerComponent } from '../word-manager/word-manager.component';

/**
 * Single admin entry point. Shows a menu of four tools and hosts each one as a child
 * component, so every feature stays self-contained and independently testable while the
 * navigation lives in one place.
 */
type Tool = 'menu' | 'manual' | 'ai' | 'ocr' | 'words';

@Component({
  selector: 'app-admin-tools',
  standalone: true,
  imports: [CommonModule, UploadTextComponent, AiToolsComponent, WordManagerComponent],
  templateUrl: './admin-tools.component.html',
  styleUrl: './admin-tools.component.scss'
})
export class AdminToolsComponent implements OnInit {

  tool: Tool = 'menu';

  constructor(
    private loginService: LoginService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loginService.reqIsLogged().subscribe({
      next: (user) => {
        if (!user || !user.roles.includes('ADMIN')) {
          this.router.navigate(['/']);
        }
      },
      error: () => this.router.navigate(['/'])
    });
  }

  select(tool: Tool): void {
    this.tool = tool;
  }

  backToMenu(): void {
    this.tool = 'menu';
  }
}
