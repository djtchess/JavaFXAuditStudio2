import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ReclassificationApiService } from '../../../core/services/reclassification-api.service';
import { ReclassifiedRuleResponse } from '../../../core/models/analysis.model';

const CATEGORY_COLORS: Record<string, string> = {
  UI: '#3b82f6',
  APPLICATION: '#10b981',
  BUSINESS: '#f59e0b',
  TECHNICAL: '#6b7280',
  UNKNOWN: '#ef4444',
};

const CATEGORIES = ['UI', 'APPLICATION', 'BUSINESS', 'TECHNICAL', 'UNKNOWN'] as const;

/**
 * Modale de reclassification manuelle d'une regle de gestion.
 * JAS-012
 */
@Component({
  selector: 'jas-reclassify-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  styles: `
    .overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
    }

    .modal {
      background: white;
      border-radius: 20px;
      padding: 2rem;
      width: min(520px, 100%);
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .modal-title {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 700;
      color: #122338;
    }

    .modal-subtitle {
      margin: 0;
      font-size: 0.85rem;
      color: #6b7280;
      font-style: italic;
    }

    .categories-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .category-btn {
      padding: 0.4rem 1rem;
      border-radius: 999px;
      border: 2px solid transparent;
      color: white;
      font-weight: 600;
      font-size: 0.82rem;
      cursor: pointer;
      transition: opacity 0.15s, transform 0.15s, border-color 0.15s;
    }

    .category-btn:hover {
      opacity: 0.85;
    }

    .category-btn.selected {
      border-color: #122338;
      transform: scale(1.06);
    }

    .reason-label {
      display: block;
      font-size: 0.85rem;
      font-weight: 600;
      color: #374151;
      margin-bottom: 0.4rem;
    }

    .reason-textarea {
      width: 100%;
      min-height: 4rem;
      padding: 0.6rem 0.8rem;
      border: 1px solid #d1d5db;
      border-radius: 10px;
      font-size: 0.85rem;
      resize: vertical;
      font-family: inherit;
      box-sizing: border-box;
    }

    .reason-textarea:focus {
      outline: 2px solid #3b82f6;
      border-color: transparent;
    }

    .error-locked {
      padding: 0.7rem 1rem;
      border-radius: 10px;
      background: rgba(239, 68, 68, 0.08);
      border: 1px solid rgba(239, 68, 68, 0.3);
      color: #dc2626;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .error-generic {
      padding: 0.7rem 1rem;
      border-radius: 10px;
      background: rgba(245, 158, 11, 0.08);
      border: 1px solid rgba(245, 158, 11, 0.3);
      color: #92400e;
      font-size: 0.85rem;
    }

    .modal-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: flex-end;
    }

    .btn-confirm {
      padding: 0.55rem 1.4rem;
      border: none;
      border-radius: 999px;
      background: #122338;
      color: white;
      font-weight: 700;
      font-size: 0.88rem;
      cursor: pointer;
      transition: opacity 0.2s;
    }

    .btn-confirm:hover:not(:disabled) {
      opacity: 0.85;
    }

    .btn-confirm:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .btn-cancel {
      padding: 0.55rem 1.2rem;
      border: 1px solid #d1d5db;
      border-radius: 999px;
      background: white;
      color: #374151;
      font-weight: 600;
      font-size: 0.88rem;
      cursor: pointer;
      transition: background 0.15s;
    }

    .btn-cancel:hover {
      background: #f9fafb;
    }
  `,
  template: `
    <div class="overlay" (click)="onOverlayClick($event)">
      <div class="modal" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
        <h2 class="modal-title">Reclassifier la regle {{ ruleId() }}</h2>
        <p class="modal-subtitle">{{ truncatedDescription() }}</p>

        <div class="categories-grid">
          @for (cat of categories; track cat) {
            <button
              class="category-btn"
              [class.selected]="selectedCategory() === cat"
              [style.background]="getCategoryColor(cat)"
              (click)="selectCategory(cat)"
            >
              {{ cat }}
            </button>
          }
        </div>

        <div>
          <label class="reason-label" for="reason-input">Raison (optionnel)</label>
          <textarea
            id="reason-input"
            class="reason-textarea"
            [(ngModel)]="reason"
            placeholder="Expliquez pourquoi vous reclassifiez cette regle..."
          ></textarea>
        </div>

        @if (lockedError()) {
          <div class="error-locked" role="alert">
            Analyse verrouillee — reclassification impossible
          </div>
        }

        @if (genericError()) {
          <div class="error-generic" role="alert">{{ genericError() }}</div>
        }

        <div class="modal-actions">
          <button class="btn-cancel" (click)="onCancel()">Annuler</button>
          <button
            class="btn-confirm"
            [disabled]="confirmDisabled()"
            (click)="onConfirm()"
          >
            @if (loading()) { Confirmation... } @else { Confirmer }
          </button>
        </div>
      </div>
    </div>
  `,
})
export class ReclassifyModalComponent implements OnInit {
  readonly ruleId = input.required<string>();
  readonly ruleDescription = input.required<string>();
  readonly currentCategory = input.required<string>();
  readonly analysisId = input.required<string>();

  readonly reclassified = output<ReclassifiedRuleResponse>();
  readonly closed = output<void>();

  private readonly reclassificationApi = inject(ReclassificationApiService);

  protected readonly categories = CATEGORIES;
  protected readonly selectedCategory = signal<string | null>(null);
  protected reason = '';
  protected readonly loading = signal(false);
  protected readonly lockedError = signal(false);
  protected readonly genericError = signal<string | null>(null);

  protected readonly truncatedDescription = computed(() => {
    const desc = this.ruleDescription();
    return desc.length > 80 ? desc.slice(0, 80) + '...' : desc;
  });

  protected readonly confirmDisabled = computed(
    () => !this.selectedCategory() || this.loading(),
  );

  ngOnInit(): void {
    this.selectedCategory.set(this.currentCategory());
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (!this.loading()) {
      this.closed.emit();
    }
  }

  protected onOverlayClick(event: MouseEvent): void {
    if (!this.loading()) {
      this.closed.emit();
    }
  }

  protected onCancel(): void {
    this.closed.emit();
  }

  protected selectCategory(cat: string): void {
    this.selectedCategory.set(cat);
  }

  protected getCategoryColor(cat: string): string {
    return CATEGORY_COLORS[cat] ?? CATEGORY_COLORS['UNKNOWN'];
  }

  protected onConfirm(): void {
    const category = this.selectedCategory();
    if (!category || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.lockedError.set(false);
    this.genericError.set(null);

    this.reclassificationApi
      .reclassify(this.analysisId(), this.ruleId(), { category, reason: this.reason })
      .subscribe({
        next: (result) => {
          this.loading.set(false);
          this.reclassified.emit(result);
          this.closed.emit();
        },
        error: (err) => {
          this.loading.set(false);
          if (err?.status === 409) {
            this.lockedError.set(true);
          } else {
            this.genericError.set(
              err?.error?.message ?? 'Une erreur est survenue lors de la reclassification.',
            );
          }
        },
      });
  }
}
