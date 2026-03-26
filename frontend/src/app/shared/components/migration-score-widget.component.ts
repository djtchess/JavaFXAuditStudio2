import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AiEnrichmentApiService } from '../../core/services/ai-enrichment-api.service';
import { ArtifactReviewResponse } from '../../core/models/analysis.model';

/**
 * Widget autonome affichant le score de migration IA pour une session donnee.
 * Lance POST /api/v1/analyses/{sessionId}/review a la demande de l'utilisateur.
 * JAS-030
 *
 * Usage :
 *   <jas-migration-score-widget [sessionId]="mySessionId" />
 */
@Component({
  selector: 'jas-migration-score-widget',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: block;
    }

    .msw-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }

    .msw-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.5rem 1.2rem;
      border: 1.5px solid #122338;
      border-radius: 999px;
      background: white;
      color: #122338;
      font-weight: 700;
      font-size: 0.85rem;
      cursor: pointer;
      transition: background 0.15s, opacity 0.2s;
      white-space: nowrap;
    }

    .msw-btn:hover:not(:disabled) {
      background: rgba(18, 35, 56, 0.06);
    }

    .msw-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .msw-error {
      padding: 0.65rem 0.9rem;
      border-radius: 8px;
      background: rgba(217, 95, 51, 0.06);
      border: 1px solid rgba(217, 95, 51, 0.25);
      color: #b94517;
      font-size: 0.84rem;
      margin-bottom: 0.75rem;
    }

    .msw-result {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      padding: 1rem 1.1rem;
      border-radius: 10px;
      border: 1px solid rgba(16, 185, 129, 0.25);
      background: rgba(16, 185, 129, 0.05);
    }

    .msw-result.degraded {
      border-color: rgba(245, 158, 11, 0.3);
      background: rgba(245, 158, 11, 0.05);
    }

    .msw-circle {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 56px;
      height: 56px;
      border-radius: 50%;
      border: 3px solid;
      font-size: 1.05rem;
      font-weight: 800;
      flex-shrink: 0;
    }

    .msw-info {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .msw-info-title {
      font-size: 0.9rem;
      font-weight: 700;
      color: #122338;
    }

    .msw-info-provider {
      font-size: 0.8rem;
      color: #6b7280;
    }

    .msw-info-degraded {
      font-size: 0.82rem;
      color: #92400e;
      margin-top: 0.15rem;
    }

    .msw-info-na {
      font-size: 0.82rem;
      color: #9ca3af;
      font-style: italic;
    }

    .msw-loading {
      font-size: 0.85rem;
      color: #6b7280;
    }
  `,
  template: `
    <div class="msw-actions">
      <button
        class="msw-btn"
        [disabled]="isLoading()"
        (click)="launchReview()"
      >
        @if (isLoading()) { Revue en cours... }
        @else { Analyser la migration }
      </button>
      @if (isLoading()) {
        <span class="msw-loading">Appel en cours...</span>
      }
    </div>

    @if (error()) {
      <div class="msw-error" role="alert">{{ error() }}</div>
    }

    @if (reviewResult()) {
      <div class="msw-result" [class.degraded]="reviewResult()!.degraded">
        <span
          class="msw-circle"
          [style.color]="scoreColor()"
          [style.border-color]="scoreColor()"
        >
          @if (reviewResult()!.migrationScore >= 0) {
            {{ reviewResult()!.migrationScore }}
          } @else {
            --
          }
        </span>
        <div class="msw-info">
          <span class="msw-info-title">Score de migration</span>
          <span class="msw-info-provider">Fournisseur : {{ reviewResult()!.provider }}</span>
          @if (reviewResult()!.degraded) {
            <span class="msw-info-degraded">Mode degrade : {{ reviewResult()!.degradationReason }}</span>
          } @else if (reviewResult()!.migrationScore < 0) {
            <span class="msw-info-na">Score indisponible pour cette session</span>
          }
        </div>
      </div>
    }
  `,
})
export class MigrationScoreWidgetComponent {
  readonly sessionId = input.required<string>();

  private readonly aiApi = inject(AiEnrichmentApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly reviewResult = signal<ArtifactReviewResponse | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly scoreColor = computed(() => {
    const score = this.reviewResult()?.migrationScore ?? -1;
    if (score < 0) {
      return '#9ca3af';
    }
    if (score >= 75) {
      return '#10b981';
    }
    if (score >= 50) {
      return '#f59e0b';
    }
    return '#ef4444';
  });

  protected launchReview(): void {
    if (this.isLoading()) {
      return;
    }
    this.isLoading.set(true);
    this.error.set(null);

    this.aiApi
      .review(this.sessionId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.isLoading.set(false);
          this.reviewResult.set(result);
        },
        error: err => {
          this.isLoading.set(false);
          this.error.set(err?.error?.message ?? 'Erreur lors de la revue de migration IA.');
        },
      });
  }
}
