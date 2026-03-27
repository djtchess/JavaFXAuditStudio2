import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';

import { ReclassificationApiService } from '../../../core/services/reclassification-api.service';
import { ReclassificationAuditEntryResponse } from '../../../core/models/analysis.model';

const CATEGORY_COLORS: Record<string, string> = {
  UI: '#3d86c6',
  APPLICATION: '#0f766e',
  BUSINESS: '#b96a2c',
  TECHNICAL: '#627487',
  UNKNOWN: '#a23c1c',
};

/**
 * Panneau d'historique de reclassification d'une regle.
 * JAS-012
 */
@Component({
  selector: 'jas-reclassification-history-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .panel {
      border: 1px solid var(--surface-line);
      border-radius: 16px;
      background: var(--panel-soft-alt);
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .panel-title {
      margin: 0;
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--surface-ink-strong);
    }

    .btn-close {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2rem;
      height: 2rem;
      border: 1px solid var(--surface-line);
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.72);
      cursor: pointer;
      font-size: 1rem;
      color: var(--surface-ink-soft);
      flex-shrink: 0;
      transition: background 0.15s;
    }

    .btn-close:hover {
      background: var(--surface-chip);
    }

    .loading-msg {
      font-size: 0.85rem;
      color: var(--surface-ink-soft);
      font-style: italic;
    }

    .empty-msg {
      font-size: 0.85rem;
      color: var(--surface-ink-soft);
      font-style: italic;
    }

    .error-msg {
      font-size: 0.85rem;
      color: var(--surface-danger);
    }

    .history-list {
      display: grid;
      gap: 0.6rem;
    }

    .history-entry {
      padding: 0.65rem 0.85rem;
      border-radius: 10px;
      border: 1px solid var(--surface-line);
      background: rgba(255, 255, 255, 0.54);
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }

    .entry-date {
      font-size: 0.75rem;
      color: var(--surface-ink-soft);
      font-family: monospace;
    }

    .entry-categories {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .cat-badge {
      display: inline-block;
      padding: 0.18rem 0.55rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      color: white;
      white-space: nowrap;
    }

    .arrow {
      font-size: 0.85rem;
      color: var(--surface-ink-soft);
    }

    .entry-reason {
      font-size: 0.8rem;
      color: var(--surface-ink-soft);
      font-style: italic;
    }
  `,
  template: `
    <div class="panel">
      <div class="panel-header">
        <h3 class="panel-title">Historique — {{ ruleId() }}</h3>
        <button class="btn-close" (click)="onClose()" title="Fermer">x</button>
      </div>

      @if (loading()) {
        <p class="loading-msg">Chargement de l'historique...</p>
      } @else if (error()) {
        <p class="error-msg">{{ error() }}</p>
      } @else if (isEmpty()) {
        <p class="empty-msg">Aucun historique de reclassification.</p>
      } @else {
        <div class="history-list">
          @for (entry of entries(); track entry.timestamp) {
            <div class="history-entry">
              <span class="entry-date">{{ formatDate(entry.timestamp) }}</span>
              <div class="entry-categories">
                <span class="cat-badge" [style.background]="getCategoryColor(entry.fromCategory)">
                  {{ entry.fromCategory }}
                </span>
                <span class="arrow">-&gt;</span>
                <span class="cat-badge" [style.background]="getCategoryColor(entry.toCategory)">
                  {{ entry.toCategory }}
                </span>
              </div>
              @if (entry.reason) {
                <span class="entry-reason">{{ entry.reason }}</span>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class ReclassificationHistoryPanelComponent implements OnInit {
  readonly ruleId = input.required<string>();
  readonly analysisId = input.required<string>();
  readonly closed = output<void>();

  private readonly reclassificationApi = inject(ReclassificationApiService);

  protected readonly entries = signal<ReclassificationAuditEntryResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly isEmpty = computed(() => !this.loading() && this.entries().length === 0);

  ngOnInit(): void {
    this.loadHistory();
  }

  protected onClose(): void {
    this.closed.emit();
  }

  protected getCategoryColor(category: string): string {
    return CATEGORY_COLORS[category] ?? CATEGORY_COLORS['UNKNOWN'];
  }

  protected formatDate(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private loadHistory(): void {
    this.loading.set(true);
    this.error.set(null);
    this.reclassificationApi.getHistory(this.analysisId(), this.ruleId()).subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err?.error?.message ?? "Erreur lors du chargement de l'historique.",
        );
      },
    });
  }
}
