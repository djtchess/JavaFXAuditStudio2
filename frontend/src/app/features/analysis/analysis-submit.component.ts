import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { SubmitAnalysisRequest, AnalysisSessionResponse } from '../../core/models/analysis.model';

@Component({
  selector: 'jas-analysis-submit',
  imports: [FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .page-shell {
      width: min(1240px, calc(100% - 1rem));
      margin: 0 auto;
      padding: 1.5rem 0 4rem;
    }

    .page-grid {
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
    }

    .intel-panel,
    .form-panel {
      border-radius: 30px;
      border: 1px solid var(--line-strong);
      overflow: hidden;
      box-shadow: var(--glow);
    }

    .intel-panel {
      padding: 1.5rem;
      background:
        linear-gradient(145deg, rgba(8, 20, 32, 0.88), rgba(12, 28, 43, 0.86)),
        radial-gradient(circle at top left, rgba(101, 223, 255, 0.12), transparent 36%);
      color: var(--ink-strong);
      position: relative;
    }

    .intel-panel::after {
      content: "";
      position: absolute;
      inset: auto -18% -24% 44%;
      height: 240px;
      background: radial-gradient(circle, rgba(255, 154, 77, 0.2), transparent 68%);
      pointer-events: none;
    }

    .form-panel {
      padding: 1.7rem;
      background:
        radial-gradient(circle at top right, rgba(101, 223, 255, 0.12), transparent 34%),
        radial-gradient(circle at bottom left, rgba(255, 154, 77, 0.12), transparent 34%),
        linear-gradient(160deg, rgba(10, 22, 34, 0.96), rgba(15, 30, 45, 0.92));
      color: var(--surface-ink);
    }

    .eyebrow,
    .panel-label {
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.18em;
      font-size: 0.72rem;
    }

    .eyebrow {
      color: rgba(157, 190, 218, 0.82);
    }

    .panel-label {
      color: var(--surface-ink-soft);
    }

    h1 {
      margin: 0.4rem 0 1rem;
      font-family: var(--font-display);
      font-weight: 700;
      font-size: clamp(2rem, 3vw, 3.2rem);
      line-height: 0.98;
      max-width: 10ch;
      letter-spacing: 0.04em;
    }

    h2 {
      margin: 0.35rem 0 1.4rem;
      font-family: var(--font-display);
      font-size: clamp(1.45rem, 2.5vw, 2.15rem);
      line-height: 1.02;
    }

    .lead {
      margin: 0;
      max-width: 34ch;
      color: var(--ink-soft);
      line-height: 1.7;
    }

    .intel-chip-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.65rem;
      margin-top: 1.3rem;
    }

    .intel-chip {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0.5rem 0.8rem;
      border-radius: 999px;
      border: 1px solid rgba(157, 190, 218, 0.18);
      background: rgba(255, 255, 255, 0.05);
      color: var(--ink-strong);
      font-size: 0.78rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .intel-steps {
      display: grid;
      gap: 0.8rem;
      margin-top: 1.5rem;
      position: relative;
      z-index: 1;
    }

    .intel-step {
      display: grid;
      gap: 0.35rem;
      padding: 1rem;
      border-radius: 22px;
      border: 1px solid rgba(157, 190, 218, 0.16);
      background: rgba(255, 255, 255, 0.04);
      backdrop-filter: blur(8px);
    }

    .intel-step strong {
      font-size: 0.8rem;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: rgba(157, 190, 218, 0.82);
    }

    .intel-step p {
      margin: 0;
      color: var(--ink-soft);
      line-height: 1.6;
      font-size: 0.9rem;
    }

    .intel-link {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      margin-top: 1.4rem;
      min-height: 2.9rem;
      padding: 0.65rem 1rem;
      border-radius: 999px;
      border: 1px solid rgba(101, 223, 255, 0.28);
      background: rgba(101, 223, 255, 0.08);
      color: white;
      text-decoration: none;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      width: fit-content;
    }

    .intel-link:hover {
      transform: translateY(-1px);
      background: rgba(101, 223, 255, 0.14);
    }

    .form-group {
      display: grid;
      gap: 0.45rem;
      margin-bottom: 1.35rem;
    }

    label {
      font-weight: 700;
      font-size: 0.82rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--surface-ink-strong);
    }

    input, textarea {
      width: 100%;
      padding: 0.9rem 1rem;
      border: 1px solid var(--surface-line);
      border-radius: 18px;
      font-family: inherit;
      font-size: 0.97rem;
      background: var(--surface-input);
      color: var(--surface-ink-strong);
      outline: none;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
    }

    input:focus, textarea:focus {
      border-color: rgba(101, 223, 255, 0.42);
      box-shadow:
        0 0 0 4px rgba(101, 223, 255, 0.14),
        inset 0 1px 0 rgba(255, 255, 255, 0.08);
    }

    textarea {
      min-height: 220px;
      resize: vertical;
      font-family: var(--font-mono);
    }

    .hint {
      margin: 0;
      font-size: 0.8rem;
      color: var(--surface-ink-soft);
    }

    .form-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
      margin-top: 1.6rem;
    }

    .footer-copy {
      display: grid;
      gap: 0.25rem;
    }

    .footer-copy strong {
      font-size: 0.82rem;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }

    .footer-copy span {
      color: var(--surface-ink-soft);
      font-size: 0.86rem;
      line-height: 1.5;
    }

    .submit-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      justify-content: center;
      min-height: 3.2rem;
      padding: 0.8rem 1.5rem;
      border: none;
      border-radius: 999px;
      background:
        linear-gradient(135deg, rgba(16, 38, 56, 1), rgba(42, 82, 116, 1));
      color: white;
      font-weight: 700;
      font-size: 0.85rem;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      cursor: pointer;
      box-shadow: 0 20px 40px rgba(17, 40, 60, 0.18);
    }

    .submit-btn:hover:not(:disabled) {
      transform: translateY(-1px);
    }

    .submit-btn:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }

    .status-panel {
      margin-top: 1rem;
      padding: 1rem 1.1rem;
      border-radius: 20px;
      border: 1px solid var(--surface-line);
      background: var(--surface-raised-strong);
    }

    .status-panel.error {
      border-color: rgba(255, 118, 92, 0.34);
      color: #a23c1c;
    }

    .status-panel.loading {
      border-color: rgba(76, 225, 198, 0.3);
      color: var(--surface-ink);
    }

    @media (max-width: 980px) {
      .page-grid {
        grid-template-columns: 1fr;
      }

      h1 {
        max-width: none;
      }
    }

    @media (max-width: 640px) {
      .page-shell {
        width: min(1240px, 100%);
        padding-top: 0.9rem;
      }

      .intel-panel,
      .form-panel {
        padding: 1.2rem;
        border-radius: 24px;
      }
    }
  `,
  template: `
    <main class="page-shell">
      <div class="page-grid">
        <aside class="intel-panel">
          <p class="eyebrow">Launchpad</p>
          <h1>Nouvelle session d'analyse</h1>
          <p class="lead">
            Prepare le lot source, declenche le pipeline d'extraction et alimente le cockpit avec
            une session exploitable de bout en bout.
          </p>

          <div class="intel-chip-row">
            <span class="intel-chip">1 chemin par ligne</span>
            <span class="intel-chip">Controllers JavaFX</span>
            <span class="intel-chip">Pipeline orchestrable</span>
          </div>

          <div class="intel-steps">
            <article class="intel-step">
              <strong>01 Ingestion</strong>
              <p>Declare la session et pointe les fichiers sources qui serviront d'entree.</p>
            </article>

            <article class="intel-step">
              <strong>02 Analyse</strong>
              <p>Le backend orchestre cartographie, classification, lots de migration et restitution.</p>
            </article>

            <article class="intel-step">
              <strong>03 Pilotage</strong>
              <p>Observe ensuite les projets, le score de migration et le monitoring technique.</p>
            </article>
          </div>

          <a class="intel-link" routerLink="/monitoring">Voir le monitoring</a>
        </aside>

        <section class="form-panel">
          <p class="panel-label">Session</p>
          <h2>Declare le lot de sources a auditer</h2>

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
              <p class="hint">Un chemin de fichier par ligne, sans logique metier dupliquee cote frontend.</p>
            </div>

            <div class="form-footer">
              <div class="footer-copy">
                <strong>Sortie attendue</strong>
                <span>Une session exploitable pour lancer ensuite les etapes du pipeline d'analyse.</span>
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
            </div>
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
      </div>
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
