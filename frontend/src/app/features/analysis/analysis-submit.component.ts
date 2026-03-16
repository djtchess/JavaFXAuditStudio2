import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { SubmitAnalysisRequest, AnalysisSessionResponse } from '../../core/models/analysis.model';

@Component({
  selector: 'jas-analysis-submit',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .page-shell {
      width: min(860px, calc(100% - 2rem));
      margin: 0 auto;
      padding: 2rem 0 4rem;
    }

    .form-panel {
      padding: 2rem;
      border: 1px solid var(--line);
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.75);
      box-shadow: var(--shadow);
    }

    .eyebrow {
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.18em;
      font-size: 0.72rem;
      color: var(--ink-soft);
    }

    h1 {
      margin: 0.4rem 0 1.5rem;
      font-family: var(--font-display);
      font-weight: 700;
      font-size: clamp(1.6rem, 3vw, 2.4rem);
      line-height: 1.1;
    }

    .form-group {
      display: grid;
      gap: 0.4rem;
      margin-bottom: 1.25rem;
    }

    label {
      font-weight: 600;
      font-size: 0.88rem;
      color: var(--slate);
    }

    input, textarea {
      padding: 0.6rem 0.8rem;
      border: 1px solid var(--line);
      border-radius: 12px;
      font-family: inherit;
      font-size: 0.95rem;
      background: rgba(247, 244, 235, 0.5);
      outline: none;
      transition: border-color 0.2s;
    }

    input:focus, textarea:focus {
      border-color: var(--slate);
    }

    textarea {
      min-height: 140px;
      resize: vertical;
      font-family: monospace;
    }

    .hint {
      margin: 0;
      font-size: 0.78rem;
      color: var(--ink-soft);
    }

    .submit-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.7rem 1.6rem;
      border: none;
      border-radius: 999px;
      background: var(--slate);
      color: white;
      font-weight: 600;
      font-size: 0.95rem;
      cursor: pointer;
      transition: opacity 0.2s;
    }

    .submit-btn:hover:not(:disabled) {
      opacity: 0.85;
    }

    .submit-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .status-panel {
      margin-top: 1rem;
      padding: 1rem 1.25rem;
      border: 1px solid var(--line);
      border-radius: 16px;
      background: rgba(255, 255, 255, 0.75);
    }

    .status-panel.error {
      border-color: rgba(217, 95, 51, 0.32);
      color: #b94517;
    }

    .status-panel.loading {
      border-color: rgba(14, 139, 114, 0.28);
      color: var(--ink-soft);
    }
  `,
  template: `
    <main class="page-shell">
      <section class="form-panel">
        <p class="eyebrow">Analyse</p>
        <h1>Nouvelle session d'analyse</h1>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="sessionName">Nom de la session</label>
            <input
              id="sessionName"
              type="text"
              [(ngModel)]="sessionName"
              name="sessionName"
              placeholder="ex: Audit MainController"
              required
            />
          </div>

          <div class="form-group">
            <label for="sourceFilePaths">Fichiers source</label>
            <textarea
              id="sourceFilePaths"
              [(ngModel)]="sourceFilePathsRaw"
              name="sourceFilePaths"
              placeholder="Un chemin par ligne&#10;ex: src/main/java/com/app/MainController.java"
              required
            ></textarea>
            <p class="hint">Un chemin de fichier par ligne</p>
          </div>

          <button
            type="submit"
            class="submit-btn"
            [disabled]="isLoading() || !sessionName.trim() || !sourceFilePathsRaw.trim()"
          >
            @if (isLoading()) {
              Soumission en cours...
            } @else {
              Lancer l'analyse
            }
          </button>
        </form>

        @if (isLoading()) {
          <div class="status-panel loading">
            <p>Soumission de la session en cours...</p>
          </div>
        }

        @if (errorMessage()) {
          <div class="status-panel error">
            <p>{{ errorMessage() }}</p>
          </div>
        }
      </section>
    </main>
  `
})
export class AnalysisSubmitComponent {
  private readonly analysisApi = inject(AnalysisApiService);
  private readonly router = inject(Router);

  protected sessionName = '';
  protected sourceFilePathsRaw = '';
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected onSubmit(): void {
    const paths = this.sourceFilePathsRaw
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0);

    if (paths.length === 0 || !this.sessionName.trim()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const request: SubmitAnalysisRequest = {
      sessionName: this.sessionName.trim(),
      sourceFilePaths: paths
    };

    this.analysisApi.submitSession(request).subscribe({
      next: (session: AnalysisSessionResponse) => {
        this.isLoading.set(false);
        this.router.navigate(['/analysis', session.sessionId]);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err?.error?.message ?? 'Erreur lors de la soumission. Verifiez que le backend est en cours d\'execution.'
        );
      }
    });
  }
}
