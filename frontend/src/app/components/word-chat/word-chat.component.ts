import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { ChatMessage, ChatService } from '../../services/chat.service';
import { LocaleNavService } from '../../i18n/locale-nav.service';

/**
 * Guardrailed AI chat about ONE word in its reading context. Opened from the word
 * popover in either reader. The conversation is ephemeral (kept only in this
 * component) and re-sent to the backend each turn. Registered-only: free users have a
 * small monthly quota (shown as a dwindling counter), premium/admins are unlimited.
 */
@Component({
  selector: 'app-word-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './word-chat.component.html',
  styleUrl: './word-chat.component.scss'
})
export class WordChatComponent implements OnInit {

  @Input() word = '';
  @Input() sentence = '';
  @Input() text = '';
  @Input() translation = '';
  @Input() level = '';
  @Output() closed = new EventEmitter<void>();

  /** Full history sent to the backend; index 0 is the hidden auto-opener. */
  messages: ChatMessage[] = [];
  draft = '';
  loading = false;
  errored = false;
  limitReached = false;
  unlimited = false;
  remaining: number | null = null;

  constructor(
    private chat: ChatService,
    private transloco: TranslocoService,
    private localeNav: LocaleNavService
  ) {}

  ngOnInit(): void {
    // Auto-request the first explanation; the opener message is not rendered.
    this.messages = [{ role: 'user', content: this.transloco.translate('wordChat.openerPrompt') }];
    this.callBackend();
  }

  /** Messages to render (hides the synthetic opener at index 0). */
  get visibleMessages(): ChatMessage[] {
    return this.messages.slice(1);
  }

  send(): void {
    const q = this.draft.trim();
    if (!q || this.loading) return;
    this.messages.push({ role: 'user', content: q });
    this.draft = '';
    this.callBackend();
  }

  private callBackend(): void {
    this.loading = true;
    this.errored = false;
    this.chat.askWord({
      word: this.word,
      sentence: this.sentence,
      text: this.text,
      translation: this.translation,
      level: this.level,
      language: this.transloco.getActiveLang(),
      history: this.messages
    }).subscribe({
      next: (res) => {
        this.messages.push({ role: 'assistant', content: this.stripMarkdown(res.reply) });
        this.unlimited = res.unlimited;
        this.remaining = res.unlimited ? null : res.remaining;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        // Drop the unanswered user turn so the thread stays clean and retryable.
        if (this.messages.length && this.messages[this.messages.length - 1].role === 'user') {
          this.messages.pop();
        }
        if (err?.status === 429) this.limitReached = true;
        else if (err?.status === 401 || err?.status === 403) this.localeNav.navigate(['/signup']);
        else this.errored = true;
      }
    });
  }

  /**
   * Strips Markdown markers so the reply reads as plain, natural text. The AI is asked
   * not to use Markdown, but this is a safety net so the user never sees stray **, __,
   * `#` or bullet symbols if the model slips.
   */
  private stripMarkdown(text: string): string {
    return (text || '')
      .replace(/\*\*(.*?)\*\*/g, '$1')   // **bold**
      .replace(/__(.*?)__/g, '$1')       // __bold__
      .replace(/\*(.*?)\*/g, '$1')       // *italic*
      .replace(/`([^`]*)`/g, '$1')       // `code`
      .replace(/^\s{0,3}#{1,6}\s+/gm, '') // # headings
      .replace(/^\s*[-*+]\s+/gm, '• ')   // bullet markers → •
      .trim();
  }

  goPremium(): void {
    this.localeNav.navigate(['/premium']);
  }

  onClose(): void {
    this.closed.emit();
  }
}
