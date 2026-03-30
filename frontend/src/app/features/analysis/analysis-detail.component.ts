import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AnalysisApiService } from '../../core/services/analysis-api.service';
import {
  ArtifactsResponse,
  CartographyResponse,
  ClassificationResponse,
  MigrationPlanResponse,
  OrchestratedAnalysisResultResponse,
  RestitutionReportResponse
} from '../../core/models/analysis.model';

import { AiEnrichmentViewComponent } from './components/ai-enrichment-view.component';
import { ArtifactsViewComponent } from './components/artifacts-view.component';
import { CartographyViewComponent } from './components/cartography-view.component';
import { ClassificationViewComponent } from './components/classification-view.component';
import { MigrationPlanViewComponent } from './components/migration-plan-view.component';
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
    AiEnrichmentViewComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .page-shell {
      width: min(1240px, calc(100% - 1rem));
      margin: 0 auto;
      padding: 1.5rem 0 4rem;
    }

    .session-hero {
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
      padding: 1.7rem;
      border-radius: 32px;
      border: 1px solid var(--line-strong);
      background:
        linear-gradient(145deg, rgba(8, 20, 32, 0.9), rgba(12, 28, 43, 0.85)),
        radial-gradient(circle at top left, rgba(101, 223, 255, 0.12), transparent 34%),
        radial-gradient(circle at bottom right, rgba(255, 154, 77, 0.14), transparent 28%);
      box-shadow: var(--glow);
      overflow: hidden;
      position: relative;
    }

    .session-hero::after {
      content: "";
      position: absolute;
      inset: auto -8% -34% 50%;
      height: 220px;
      background: radial-gradient(circle, rgba(101, 223, 255, 0.14), transparent 68%);
      pointer-events: none;
    }

    .eyebrow {
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.18em;
      font-size: 0.72rem;
      color: var(--ink-soft);
    }

    h1 {
      margin: 0.4rem 0 0.75rem;
      font-family: var(--font-display);
      font-weight: 700;
      font-size: clamp(2.2rem, 3.6vw, 3.5rem);
      line-height: 0.95;
      max-width: 11ch;
      letter-spacing: 0.04em;
    }

    .lead {
      margin: 0;
      max-width: 56ch;
      color: var(--ink-soft);
      line-height: 1.7;
    }

    .session-id {
      margin: 1.1rem 0 0;
      display: inline-flex;
      width: fit-content;
      align-items: center;
      gap: 0.65rem;
      padding: 0.55rem 0.85rem;
      border-radius: 999px;
      border: 1px solid rgba(157, 190, 218, 0.18);
      background: rgba(255, 255, 255, 0.04);
      font-size: 0.78rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: rgba(157, 190, 218, 0.88);
    }

    .session-id span {
      color: white;
      font-family: var(--font-mono);
      letter-spacing: 0.04em;
      text-transform: none;
    }

    .hero-meta {
      display: grid;
      gap: 0.8rem;
      align-content: start;
      position: relative;
      z-index: 1;
    }

    .meta-card {
      display: grid;
      gap: 0.25rem;
      padding: 1rem 1.05rem;
      border-radius: 22px;
      border: 1px solid rgba(157, 190, 218, 0.16);
      background: rgba(255, 255, 255, 0.04);
      backdrop-filter: blur(8px);
    }

    .meta-label {
      margin: 0;
      color: rgba(157, 190, 218, 0.76);
      font-size: 0.72rem;
      letter-spacing: 0.18em;
      text-transform: uppercase;
    }

    .meta-card strong {
      font-family: var(--font-display);
      font-size: 1.55rem;
      line-height: 1;
      letter-spacing: 0.04em;
    }

    .meta-card span {
      color: var(--ink-soft);
      font-size: 0.84rem;
      line-height: 1.55;
    }

    .command-bar {
      display: flex;
      align-items: center;
      gap: 1rem;
      justify-content: space-between;
      margin: 1rem 0 1.5rem;
      padding: 1rem 1.1rem;
      border: 1px solid var(--line-strong);
      border-radius: 24px;
      background:
        linear-gradient(135deg, rgba(8, 20, 32, 0.84), rgba(12, 28, 43, 0.82)),
        rgba(255, 255, 255, 0.02);
      box-shadow: var(--glow);
      flex-wrap: wrap;
    }

    .pipeline-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      justify-content: center;
      min-height: 3rem;
      padding: 0.7rem 1.25rem;
      border: 1px solid rgba(101, 223, 255, 0.22);
      border-radius: 999px;
      background:
        linear-gradient(135deg, rgba(101, 223, 255, 0.2), rgba(255, 154, 77, 0.18)),
        rgba(255, 255, 255, 0.08);
      color: white;
      font-weight: 700;
      font-size: 0.82rem;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      cursor: pointer;
      flex-shrink: 0;
    }

    .pipeline-btn:hover:not(:disabled) {
      transform: translateY(-1px);
    }

    .pipeline-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .command-side {
      display: grid;
      gap: 0.45rem;
      min-width: min(100%, 320px);
    }

    .command-caption {
      margin: 0;
      color: rgba(157, 190, 218, 0.76);
      font-size: 0.78rem;
      line-height: 1.5;
    }

    .pipeline-progress {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.85rem;
      color: var(--ink-soft);
    }

    .progress-bar-track {
      width: 200px;
      height: 6px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.08);
      overflow: hidden;
    }

    .progress-bar-fill {
      height: 100%;
      border-radius: 999px;
      background: linear-gradient(90deg, var(--cyan), var(--accent));
      transition: width 0.3s ease;
    }

    .pipeline-error {
      font-size: 0.82rem;
      color: #ffd1c6;
    }

    .steps-grid {
      display: grid;
      gap: 1rem;
    }

    .step-card {
      border: 1px solid var(--surface-line-strong);
      border-radius: 26px;
      background:
        radial-gradient(circle at top right, rgba(101, 223, 255, 0.12), transparent 34%),
        radial-gradient(circle at bottom left, rgba(255, 154, 77, 0.1), transparent 34%),
        linear-gradient(160deg, rgba(10, 22, 34, 0.96), rgba(15, 31, 46, 0.92));
      color: var(--surface-ink);
      box-shadow: var(--shadow);
      overflow: hidden;
    }

    .step-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1.15rem 1.35rem;
      border-bottom: 1px solid var(--surface-line);
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
      border-radius: 0.8rem;
      background:
        linear-gradient(135deg, rgba(17, 40, 60, 1), rgba(39, 76, 108, 1));
      color: white;
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
      justify-content: center;
      min-height: 2.7rem;
      padding: 0.55rem 1rem;
      border: 1px solid var(--surface-line);
      border-radius: 999px;
      background: var(--surface-chip);
      color: var(--surface-ink-strong);
      font-weight: 700;
      font-size: 0.76rem;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      cursor: pointer;
      flex-shrink: 0;
    }

    .run-btn:hover:not(:disabled) {
      transform: translateY(-1px);
      border-color: rgba(101, 223, 255, 0.32);
      background: var(--surface-chip-strong);
    }

    .run-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .step-body {
      padding: 1rem 1.35rem 1.25rem;
    }

    .parsing-warning-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.7rem 1rem;
      border-radius: 16px;
      background: rgba(255, 154, 77, 0.08);
      border: 1px solid rgba(255, 154, 77, 0.26);
      color: var(--surface-warning);
      font-size: 0.85rem;
      margin-bottom: 1rem;
    }

    .parsing-warning-banner .reason {
      font-style: italic;
      opacity: 0.8;
    }

    .status-loading {
      padding: 0.8rem 1rem;
      border-radius: 16px;
      background: rgba(76, 225, 198, 0.08);
      border: 1px solid rgba(76, 225, 198, 0.24);
      color: var(--surface-success);
      font-size: 0.9rem;
    }

    .status-error {
      padding: 0.8rem 1rem;
      border-radius: 16px;
      background: rgba(255, 118, 92, 0.08);
      border: 1px solid rgba(255, 118, 92, 0.25);
      color: var(--surface-danger);
      font-size: 0.9rem;
    }

    @media (max-width: 960px) {
      .session-hero {
        grid-template-columns: 1fr;
      }
    }
  `,
  template: `
    <main class="page-shell">
      <section class="session-hero">
        <div>
          <p class="eyebrow">Mission Control</p>
          <h1>Detail de la session</h1>
          <p class="lead">
            Execute les etapes une par une pour inspecter finement les sorties, ou lance le pipeline
            complet pour piloter la migration depuis une seule console.
          </p>
          <p class="session-id">Session <span>{{ sessionId }}</span></p>
        </div>

        <div class="hero-meta">
          <article class="meta-card">
            <p class="meta-label">Pipeline</p>
            <strong>5 etapes</strong>
            <span>Cartographie, classification, plan, artefacts et restitution.</span>
          </article>
          <article class="meta-card">
            <p class="meta-label">Mode</p>
            <strong>Orchestration guidee</strong>
            <span>Run complet ou execution ciblee selon le besoin d'analyse.</span>
          </article>
          <article class="meta-card">
            <p class="meta-label">Extension</p>
            <strong>Analyse IA</strong>
            <span>Enrichissement additionnel depuis la meme session de travail.</span>
          </article>
        </div>
      </section>

      <section class="command-bar">
        <button
          class="pipeline-btn"
          [disabled]="pipelineRunning()"
          (click)="runFullPipeline()"
        >
          @if (pipelineRunning()) { Pipeline en cours... } @else { Executer le pipeline complet }
        </button>

        @if (pipelineRunning()) {
          <div class="command-side">
            <div class="pipeline-progress">
              <div class="progress-bar-track">
                <div class="progress-bar-fill" [style.width.%]="pipelineProgressPercent()"></div>
              </div>
              <span>Etape {{ pipelineStep() }}/5 en cours...</span>
            </div>
            <p class="command-caption">Progression du pipeline global en cours d'orchestration.</p>
          </div>
        } @else {
          <div class="command-side">
            <p class="command-caption">
              Le pipeline complet enchaine automatiquement les cinq modules et peuple la console de
              restitution sans changer les contrats backend.
            </p>
          </div>
        }

        @if (pipelineError()) {
          <span class="pipeline-error">{{ pipelineError() }}</span>
        }
      </section>

      <div class="steps-grid">
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
                    Attention : analyse en mode regex, precision reduite (JavaParser a echoue)
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

        <section class="step-card">
          <div class="step-header">
            <div class="step-title-row">
              <span class="step-number">AI</span>
              <h2>Analyse IA</h2>
            </div>
          </div>
          <div class="step-body">
            <jas-ai-enrichment-view [sessionId]="sessionId" [templateArtifacts]="artifacts().data" />
          </div>
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
