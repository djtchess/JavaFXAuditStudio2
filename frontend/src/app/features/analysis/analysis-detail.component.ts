import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AnalysisApiService } from '../../core/services/analysis-api.service';
import {
  CartographyResponse,
  ClassificationResponse,
  MigrationPlanResponse,
  ArtifactsResponse,
  RestitutionReportResponse,
  OrchestratedAnalysisResultResponse
} from '../../core/models/analysis.model';

import { CartographyViewComponent } from './components/cartography-view.component';
import { ClassificationViewComponent } from './components/classification-view.component';
import { MigrationPlanViewComponent } from './components/migration-plan-view.component';
import { ArtifactsViewComponent } from './components/artifacts-view.component';
import { ReportViewComponent } from './components/report-view.component';

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
  imports: [
    CartographyViewComponent,
    ClassificationViewComponent,
    MigrationPlanViewComponent,
    ArtifactsViewComponent,
    ReportViewComponent,
  ],
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
      margin: 0 0 1.2rem;
      font-size: 0.82rem;
      color: var(--ink-soft);
      font-family: monospace;
    }

    .pipeline-bar {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1.5rem;
      padding: 1rem 1.25rem;
      border: 1px solid var(--line);
      border-radius: 16px;
      background: rgba(255, 255, 255, 0.75);
      box-shadow: var(--shadow);
      flex-wrap: wrap;
    }

    .pipeline-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.6rem 1.4rem;
      border: none;
      border-radius: 999px;
      background: linear-gradient(135deg, var(--slate), #2d4a6f);
      color: white;
      font-weight: 700;
      font-size: 0.9rem;
      cursor: pointer;
      transition: opacity 0.2s;
      flex-shrink: 0;
    }

    .pipeline-btn:hover:not(:disabled) {
      opacity: 0.85;
    }

    .pipeline-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .pipeline-progress {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.85rem;
      color: var(--ink-soft);
    }

    .progress-bar-track {
      width: 180px;
      height: 6px;
      border-radius: 999px;
      background: rgba(18, 35, 56, 0.08);
      overflow: hidden;
    }

    .progress-bar-fill {
      height: 100%;
      border-radius: 999px;
      background: var(--slate);
      transition: width 0.3s ease;
    }

    .pipeline-error {
      font-size: 0.82rem;
      color: #b94517;
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

    .parsing-warning-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.7rem 1rem;
      border-radius: 12px;
      background: rgba(245, 158, 11, 0.08);
      border: 1px solid rgba(245, 158, 11, 0.3);
      color: #92400e;
      font-size: 0.85rem;
      margin-bottom: 1rem;
    }

    .parsing-warning-banner .reason {
      font-style: italic;
      opacity: 0.8;
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
  `,
  template: `
    <main class="page-shell">
      <p class="eyebrow">Analyse</p>
      <h1>Detail de la session</h1>
      <p class="session-id">Session : {{ sessionId }}</p>

      <!-- Pipeline complet -->
      <div class="pipeline-bar">
        <button
          class="pipeline-btn"
          [disabled]="pipelineRunning()"
          (click)="runFullPipeline()"
        >
          @if (pipelineRunning()) { Pipeline en cours... } @else { Executer le pipeline complet }
        </button>

        @if (pipelineRunning()) {
          <div class="pipeline-progress">
            <div class="progress-bar-track">
              <div class="progress-bar-fill" [style.width.%]="pipelineProgressPercent()"></div>
            </div>
            <span>Etape {{ pipelineStep() }}/5 en cours...</span>
          </div>
        }

        @if (pipelineError()) {
          <span class="pipeline-error">{{ pipelineError() }}</span>
        }
      </div>

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
                <jas-cartography-view [data]="cartography().data!" />
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
                @if (classification().data!.parsingMode === 'REGEX_FALLBACK') {
                  <div class="parsing-warning-banner">
                    ⚠ Analyse en mode regex — précision réduite (JavaParser a échoué)
                    @if (classification().data!.parsingFallbackReason) {
                      <span class="reason">{{ classification().data!.parsingFallbackReason }}</span>
                    }
                  </div>
                }
                <jas-classification-view [data]="classification().data!" [sessionId]="sessionId" />
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
                <jas-migration-plan-view [data]="migrationPlan().data!" />
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
                <jas-artifacts-view [data]="artifacts().data!" [sessionId]="sessionId" />
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
                <jas-report-view [data]="report().data!" />
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

  protected readonly pipelineRunning = signal(false);
  protected readonly pipelineStep = signal(0);
  protected readonly pipelineProgressPercent = signal(0);
  protected readonly pipelineError = signal<string | null>(null);

  protected runFullPipeline(): void {
    this.pipelineRunning.set(true);
    this.pipelineStep.set(1);
    this.pipelineProgressPercent.set(10);
    this.pipelineError.set(null);

    // Set all steps to loading
    this.cartography.set({ isLoading: true, error: null, data: null });
    this.classification.set({ isLoading: true, error: null, data: null });
    this.migrationPlan.set({ isLoading: true, error: null, data: null });
    this.artifacts.set({ isLoading: true, error: null, data: null });
    this.report.set({ isLoading: true, error: null, data: null });

    // Simulate step progress while waiting for the single API call
    const progressInterval = setInterval(() => {
      const current = this.pipelineStep();
      if (current < 5) {
        this.pipelineStep.set(current + 1);
        this.pipelineProgressPercent.set(Math.min(90, (current + 1) * 18));
      }
    }, 2000);

    this.analysisApi.runFullPipeline(this.sessionId).subscribe({
      next: (result: OrchestratedAnalysisResultResponse) => {
        clearInterval(progressInterval);
        this.pipelineStep.set(5);
        this.pipelineProgressPercent.set(100);

        this.cartography.set({
          isLoading: false,
          error: null,
          data: result.cartography,
        });
        this.classification.set({
          isLoading: false,
          error: null,
          data: result.classification,
        });
        this.migrationPlan.set({
          isLoading: false,
          error: null,
          data: result.migrationPlan,
        });
        this.artifacts.set({
          isLoading: false,
          error: null,
          data: result.generationResult,
        });
        this.report.set({
          isLoading: false,
          error: null,
          data: result.restitutionReport,
        });

        this.pipelineRunning.set(false);

        if (result.errors && result.errors.length > 0) {
          this.pipelineError.set(
            'Pipeline termine avec erreurs : ' + result.errors.join(' | ')
          );
        }
      },
      error: (err) => {
        clearInterval(progressInterval);
        this.pipelineRunning.set(false);
        this.pipelineError.set(
          err?.error?.message ?? 'Erreur lors de l\'execution du pipeline complet.'
        );

        // Reset all loading states
        this.cartography.set(emptyStep());
        this.classification.set(emptyStep());
        this.migrationPlan.set(emptyStep());
        this.artifacts.set(emptyStep());
        this.report.set(emptyStep());
      },
    });
  }

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
