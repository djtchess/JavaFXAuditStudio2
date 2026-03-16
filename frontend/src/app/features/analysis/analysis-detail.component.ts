import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AnalysisApiService } from '../../core/services/analysis-api.service';
import {
  CartographyResponse,
  ClassificationResponse,
  MigrationPlanResponse,
  ArtifactsResponse,
  RestitutionReportResponse
} from '../../core/models/analysis.model';

interface StepState<T> {
  isLoading: boolean;
  error: string | null;
  data: T | null;
}

function emptyStep<T>(): StepState<T> {
  return { isLoading: false, error: null, data: null };
}

@Component({
  selector: 'jas-analysis-detail',
  imports: [JsonPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .page-shell {
      width: min(1080px, calc(100% - 2rem));
      margin: 0 auto;
      padding: 2rem 0 4rem;
    }

    .eyebrow {
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.18em;
      font-size: 0.72rem;
      color: var(--ink-soft);
    }

    h1 {
      margin: 0.4rem 0 0.5rem;
      font-family: var(--font-display);
      font-weight: 700;
      font-size: clamp(1.6rem, 3vw, 2.4rem);
      line-height: 1.1;
    }

    .session-id {
      margin: 0 0 2rem;
      font-size: 0.82rem;
      color: var(--ink-soft);
      font-family: monospace;
    }

    .steps-grid {
      display: grid;
      gap: 1rem;
    }

    .step-card {
      border: 1px solid var(--line);
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.75);
      box-shadow: var(--shadow);
      overflow: hidden;
    }

    .step-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1.1rem 1.5rem;
    }

    .step-header h2 {
      margin: 0;
      font-family: var(--font-display);
      font-weight: 700;
      font-size: 1.15rem;
    }

    .step-number {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      background: var(--accent-soft);
      color: var(--slate);
      font-weight: 700;
      font-size: 0.85rem;
      flex-shrink: 0;
    }

    .step-title-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .run-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.5rem 1.2rem;
      border: none;
      border-radius: 999px;
      background: var(--slate);
      color: white;
      font-weight: 600;
      font-size: 0.85rem;
      cursor: pointer;
      transition: opacity 0.2s;
      flex-shrink: 0;
    }

    .run-btn:hover:not(:disabled) {
      opacity: 0.85;
    }

    .run-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .step-body {
      padding: 0 1.5rem 1.25rem;
    }

    .status-loading {
      padding: 0.8rem 1rem;
      border-radius: 12px;
      background: rgba(14, 139, 114, 0.06);
      border: 1px solid rgba(14, 139, 114, 0.2);
      color: var(--ink-soft);
      font-size: 0.9rem;
    }

    .status-error {
      padding: 0.8rem 1rem;
      border-radius: 12px;
      background: rgba(217, 95, 51, 0.06);
      border: 1px solid rgba(217, 95, 51, 0.25);
      color: #b94517;
      font-size: 0.9rem;
    }

    .result-block {
      padding: 1rem;
      border-radius: 12px;
      background: rgba(247, 244, 235, 0.84);
      border: 1px solid rgba(18, 35, 56, 0.08);
      overflow-x: auto;
    }

    .result-block pre {
      margin: 0;
      font-size: 0.82rem;
      line-height: 1.55;
      white-space: pre-wrap;
      word-break: break-word;
    }
  `,
  template: `
    <main class="page-shell">
      <p class="eyebrow">Analyse</p>
      <h1>Detail de la session</h1>
      <p class="session-id">Session : {{ sessionId }}</p>

      <div class="steps-grid">

        <!-- Step 1: Cartographie -->
        <section class="step-card">
          <div class="step-header">
            <div class="step-title-row">
              <span class="step-number">1</span>
              <h2>Cartographie</h2>
            </div>
            <button
              class="run-btn"
              [disabled]="cartography().isLoading"
              (click)="runCartography()"
            >
              @if (cartography().isLoading) { Chargement... } @else { Executer }
            </button>
          </div>
          @if (cartography().isLoading || cartography().error || cartography().data) {
            <div class="step-body">
              @if (cartography().isLoading) {
                <div class="status-loading">Analyse de la cartographie en cours...</div>
              } @else if (cartography().error) {
                <div class="status-error">{{ cartography().error }}</div>
              } @else if (cartography().data) {
                <div class="result-block"><pre>{{ cartography().data | json }}</pre></div>
              }
            </div>
          }
        </section>

        <!-- Step 2: Classification -->
        <section class="step-card">
          <div class="step-header">
            <div class="step-title-row">
              <span class="step-number">2</span>
              <h2>Classification</h2>
            </div>
            <button
              class="run-btn"
              [disabled]="classification().isLoading"
              (click)="runClassification()"
            >
              @if (classification().isLoading) { Chargement... } @else { Executer }
            </button>
          </div>
          @if (classification().isLoading || classification().error || classification().data) {
            <div class="step-body">
              @if (classification().isLoading) {
                <div class="status-loading">Classification des regles metier en cours...</div>
              } @else if (classification().error) {
                <div class="status-error">{{ classification().error }}</div>
              } @else if (classification().data) {
                <div class="result-block"><pre>{{ classification().data | json }}</pre></div>
              }
            </div>
          }
        </section>

        <!-- Step 3: Plan de migration -->
        <section class="step-card">
          <div class="step-header">
            <div class="step-title-row">
              <span class="step-number">3</span>
              <h2>Plan de migration</h2>
            </div>
            <button
              class="run-btn"
              [disabled]="migrationPlan().isLoading"
              (click)="runMigrationPlan()"
            >
              @if (migrationPlan().isLoading) { Chargement... } @else { Executer }
            </button>
          </div>
          @if (migrationPlan().isLoading || migrationPlan().error || migrationPlan().data) {
            <div class="step-body">
              @if (migrationPlan().isLoading) {
                <div class="status-loading">Generation du plan de migration en cours...</div>
              } @else if (migrationPlan().error) {
                <div class="status-error">{{ migrationPlan().error }}</div>
              } @else if (migrationPlan().data) {
                <div class="result-block"><pre>{{ migrationPlan().data | json }}</pre></div>
              }
            </div>
          }
        </section>

        <!-- Step 4: Artefacts -->
        <section class="step-card">
          <div class="step-header">
            <div class="step-title-row">
              <span class="step-number">4</span>
              <h2>Artefacts</h2>
            </div>
            <button
              class="run-btn"
              [disabled]="artifacts().isLoading"
              (click)="runArtifacts()"
            >
              @if (artifacts().isLoading) { Chargement... } @else { Executer }
            </button>
          </div>
          @if (artifacts().isLoading || artifacts().error || artifacts().data) {
            <div class="step-body">
              @if (artifacts().isLoading) {
                <div class="status-loading">Generation des artefacts en cours...</div>
              } @else if (artifacts().error) {
                <div class="status-error">{{ artifacts().error }}</div>
              } @else if (artifacts().data) {
                <div class="result-block"><pre>{{ artifacts().data | json }}</pre></div>
              }
            </div>
          }
        </section>

        <!-- Step 5: Rapport -->
        <section class="step-card">
          <div class="step-header">
            <div class="step-title-row">
              <span class="step-number">5</span>
              <h2>Rapport de restitution</h2>
            </div>
            <button
              class="run-btn"
              [disabled]="report().isLoading"
              (click)="runReport()"
            >
              @if (report().isLoading) { Chargement... } @else { Executer }
            </button>
          </div>
          @if (report().isLoading || report().error || report().data) {
            <div class="step-body">
              @if (report().isLoading) {
                <div class="status-loading">Generation du rapport en cours...</div>
              } @else if (report().error) {
                <div class="status-error">{{ report().error }}</div>
              } @else if (report().data) {
                <div class="result-block"><pre>{{ report().data | json }}</pre></div>
              }
            </div>
          }
        </section>

      </div>
    </main>
  `
})
export class AnalysisDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly analysisApi = inject(AnalysisApiService);

  protected readonly sessionId = this.route.snapshot.paramMap.get('sessionId') ?? '';

  protected readonly cartography = signal<StepState<CartographyResponse>>(emptyStep());
  protected readonly classification = signal<StepState<ClassificationResponse>>(emptyStep());
  protected readonly migrationPlan = signal<StepState<MigrationPlanResponse>>(emptyStep());
  protected readonly artifacts = signal<StepState<ArtifactsResponse>>(emptyStep());
  protected readonly report = signal<StepState<RestitutionReportResponse>>(emptyStep());

  protected runCartography(): void {
    this.cartography.set({ isLoading: true, error: null, data: null });
    this.analysisApi.getCartography(this.sessionId).subscribe({
      next: data => this.cartography.set({ isLoading: false, error: null, data }),
      error: err => this.cartography.set({
        isLoading: false,
        error: err?.error?.message ?? 'Erreur lors de la cartographie.',
        data: null
      })
    });
  }

  protected runClassification(): void {
    this.classification.set({ isLoading: true, error: null, data: null });
    this.analysisApi.getClassification(this.sessionId).subscribe({
      next: data => this.classification.set({ isLoading: false, error: null, data }),
      error: err => this.classification.set({
        isLoading: false,
        error: err?.error?.message ?? 'Erreur lors de la classification.',
        data: null
      })
    });
  }

  protected runMigrationPlan(): void {
    this.migrationPlan.set({ isLoading: true, error: null, data: null });
    this.analysisApi.getMigrationPlan(this.sessionId).subscribe({
      next: data => this.migrationPlan.set({ isLoading: false, error: null, data }),
      error: err => this.migrationPlan.set({
        isLoading: false,
        error: err?.error?.message ?? 'Erreur lors de la generation du plan de migration.',
        data: null
      })
    });
  }

  protected runArtifacts(): void {
    this.artifacts.set({ isLoading: true, error: null, data: null });
    this.analysisApi.getArtifacts(this.sessionId).subscribe({
      next: data => this.artifacts.set({ isLoading: false, error: null, data }),
      error: err => this.artifacts.set({
        isLoading: false,
        error: err?.error?.message ?? 'Erreur lors de la generation des artefacts.',
        data: null
      })
    });
  }

  protected runReport(): void {
    this.report.set({ isLoading: true, error: null, data: null });
    this.analysisApi.getReport(this.sessionId).subscribe({
      next: data => this.report.set({ isLoading: false, error: null, data }),
      error: err => this.report.set({
        isLoading: false,
        error: err?.error?.message ?? 'Erreur lors de la generation du rapport.',
        data: null
      })
    });
  }
}
