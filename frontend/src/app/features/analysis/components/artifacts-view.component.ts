import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';

import { ArtifactsResponse, CodeArtifactDto } from '../../../core/models/analysis.model';
import { AnalysisApiService } from '../../../core/services/analysis-api.service';

@Component({
  selector: 'jas-artifacts-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .warning-banner {
      padding: 0.7rem 1rem;
      border-radius: 12px;
      background: var(--surface-warning-soft);
      border: 1px solid rgba(185, 106, 44, 0.24);
      color: var(--surface-warning);
      font-size: 0.85rem;
      margin-bottom: 1rem;
    }

    .warning-banner ul {
      margin: 0.3rem 0 0;
      padding-left: 1.2rem;
    }

    .warning-banner li {
      margin-bottom: 0.2rem;
    }

    .export-panel {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }

    .export-input {
      flex: 1;
      min-width: 220px;
      padding: 0.4rem 0.8rem;
      border: 1px solid var(--surface-line);
      border-radius: 10px;
      font-size: 0.82rem;
      background: var(--surface-input);
      color: var(--surface-ink);
      font-family: monospace;
    }

    .export-input:focus {
      outline: 2px solid rgba(101, 223, 255, 0.4);
      outline-offset: 1px;
    }

    .export-btn {
      padding: 0.42rem 1rem;
      border: 1px solid rgba(16, 38, 56, 0.12);
      border-radius: 999px;
      background: linear-gradient(135deg, rgba(16, 38, 56, 1), rgba(42, 82, 116, 1));
      color: white;
      font-weight: 700;
      font-size: 0.82rem;
      cursor: pointer;
      white-space: nowrap;
      transition: opacity 0.15s, transform 0.15s;
    }

    .export-btn:hover:not(:disabled) {
      opacity: 0.88;
      transform: translateY(-1px);
    }

    .export-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .export-result {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      font-size: 0.8rem;
    }

    .export-ok {
      color: var(--surface-success);
    }

    .export-err {
      color: var(--surface-danger);
    }

    .tabs-bar {
      display: flex;
      gap: 0.35rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }

    .tab-btn {
      padding: 0.35rem 0.85rem;
      border: 1px solid var(--surface-line);
      border-radius: 999px;
      background: var(--surface-chip);
      color: var(--surface-ink-soft);
      font-weight: 600;
      font-size: 0.82rem;
      cursor: pointer;
      transition: background 0.15s, color 0.15s, border-color 0.15s;
    }

    .tab-btn:hover {
      background: var(--surface-chip-strong);
      color: var(--surface-ink-strong);
    }

    .tab-btn.active {
      background: var(--surface-chip-strong);
      color: var(--surface-ink-strong);
      border-color: rgba(101, 223, 255, 0.22);
    }

    .artifacts-grid {
      display: grid;
      gap: 0.5rem;
    }

    .artifact-card {
      border-radius: 14px;
      border: 1px solid var(--surface-line);
      background: var(--surface-raised);
      overflow: hidden;
    }

    .artifact-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.7rem 0.9rem;
      flex-wrap: wrap;
    }

    .artifact-name {
      font-weight: 700;
      font-size: 0.9rem;
      font-family: monospace;
      color: var(--surface-ink-strong);
      flex: 1;
    }

    .type-badge {
      display: inline-block;
      padding: 0.18rem 0.55rem;
      border-radius: 999px;
      background: rgba(61, 134, 198, 0.12);
      color: #3d86c6;
      font-size: 0.72rem;
      font-weight: 600;
    }

    .bridge-badge {
      display: inline-block;
      padding: 0.18rem 0.55rem;
      border-radius: 999px;
      background: var(--surface-warning-soft);
      color: var(--surface-warning);
      font-size: 0.72rem;
      font-weight: 600;
    }

    .code-toggle {
      padding: 0.22rem 0.65rem;
      border: 1px solid var(--surface-line);
      border-radius: 999px;
      background: transparent;
      color: var(--surface-ink-soft);
      font-size: 0.75rem;
      cursor: pointer;
      transition: background 0.12s, color 0.12s;
    }

    .code-toggle:hover {
      background: var(--surface-chip);
      color: var(--surface-ink-strong);
    }

    .artifact-actions {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      flex-wrap: wrap;
    }

    .artifact-action-btn {
      padding: 0.22rem 0.65rem;
      border: 1px solid var(--surface-line);
      border-radius: 999px;
      background: var(--surface-chip);
      color: var(--surface-ink-soft);
      font-size: 0.75rem;
      cursor: pointer;
      transition: background 0.12s, color 0.12s;
    }

    .artifact-action-btn:hover {
      background: var(--surface-chip-strong);
      color: var(--surface-ink-strong);
    }

    .artifact-feedback {
      padding: 0.35rem 0.9rem 0.7rem;
      font-size: 0.75rem;
      color: var(--surface-success);
    }

    .artifact-feedback.error {
      color: var(--surface-danger);
    }

    .code-block {
      background: var(--surface-code-bg);
      color: var(--surface-code-ink);
      font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
      font-size: 0.78rem;
      line-height: 1.55;
      padding: 1rem 1.1rem;
      margin: 0;
      overflow-x: auto;
      white-space: pre;
      max-height: 420px;
      overflow-y: auto;
    }

    .empty-msg {
      padding: 0.6rem 0;
      color: var(--surface-ink-soft);
      font-size: 0.85rem;
      font-style: italic;
    }

    .needs-review-badge {
      background: var(--surface-warning-soft);
      color: var(--surface-warning);
      border: 1px solid rgba(185, 106, 44, 0.24);
      border-radius: 999px;
      padding: 0.15rem 0.45rem;
      font-size: 0.68rem;
      font-weight: 700;
      white-space: nowrap;
    }

    .warning-list {
      margin: 0.3rem 0.9rem 0.5rem;
      padding-left: 0;
      list-style: none;
    }

    .warning-item {
      font-size: 0.72rem;
      color: var(--surface-warning);
      margin-bottom: 0.15rem;
    }

    .needs-review-summary {
      padding: 0.5rem 1rem;
      background: var(--surface-warning-soft);
      border: 1px solid rgba(185, 106, 44, 0.22);
      border-radius: 8px;
      font-size: 0.82rem;
      color: var(--surface-warning);
      margin-bottom: 0.75rem;
    }
  `,
  template: `
    @if (data().warnings.length > 0) {
      <div class="warning-banner">
        <strong>Avertissements</strong>
        <ul>
          @for (w of data().warnings; track w) { <li>{{ w }}</li> }
        </ul>
      </div>
    }

    @if (needsReviewCount() > 0) {
      <div class="needs-review-summary">
        Attention : {{ needsReviewCount() }} artefact(s) necessitent une revision.
      </div>
    }

    <div class="export-panel">
      <input
        class="export-input"
        type="text"
        [value]="exportDir()"
        (input)="exportDir.set($any($event.target).value)"
        placeholder="Repertoire cible (ex: C:\\monprojet\\src\\main\\java\\migration)"
      />
      <button
        class="export-btn"
        [disabled]="!exportDir().trim() || isExporting()"
        (click)="doExport()"
      >
        {{ isExporting() ? 'Export...' : 'Exporter .java' }}
      </button>
      @if (exportResult(); as result) {
        <div class="export-result">
          @if (result.exportedFiles.length > 0) {
            <span class="export-ok">OK {{ result.exportedFiles.length }} fichier(s) exporte(s) dans {{ result.targetDirectory }}</span>
          }
          @for (e of result.errors; track e) {
            <span class="export-err">Erreur {{ e }}</span>
          }
        </div>
      }
    </div>

    @if (lotNumbers().length > 1) {
      <div class="tabs-bar">
        @for (lot of lotNumbers(); track lot) {
          <button
            class="tab-btn"
            [class.active]="activeLot() === lot"
            (click)="activeLot.set(lot)"
          >
            Lot {{ lot }}
          </button>
        }
      </div>
    }

    @if (activeArtifacts().length === 0) {
      <p class="empty-msg">Aucun artefact pour ce lot.</p>
    } @else {
      <div class="artifacts-grid">
        @for (art of activeArtifacts(); track art.artifactId) {
          <div class="artifact-card">
            <div class="artifact-header">
              <span class="artifact-name">{{ art.className }}</span>
              <span class="type-badge">{{ art.type }}</span>
              @if (art.transitionalBridge) {
                <span class="bridge-badge">Bridge</span>
              }
              @if (art.generationStatus === 'WARNING') {
                <span class="needs-review-badge">A verifier</span>
              }
              <div class="artifact-actions">
                <button
                  class="artifact-action-btn"
                  (click)="copyArtifact(art)"
                >
                  Copier
                </button>
                <button
                  class="artifact-action-btn"
                  (click)="downloadArtifact(art)"
                >
                  Telecharger
                </button>
                <button
                  class="code-toggle"
                  (click)="toggleCode(art.artifactId)"
                >
                  {{ expandedArtifact() === art.artifactId ? 'Masquer' : 'Voir le code' }}
                </button>
              </div>
            </div>
            @if (art.generationWarnings.length > 0) {
              <ul class="warning-list">
                @for (w of art.generationWarnings; track w) {
                  <li class="warning-item">Attention {{ formatWarning(w) }}</li>
                }
              </ul>
            }
            @if (artifactFeedback()[art.artifactId]) {
              <div class="artifact-feedback" [class.error]="artifactFeedback()[art.artifactId]?.startsWith('Erreur')">
                {{ artifactFeedback()[art.artifactId] }}
              </div>
            }
            @if (expandedArtifact() === art.artifactId) {
              <pre class="code-block">{{ art.content }}</pre>
            }
          </div>
        }
      </div>
    }
  `
})
export class ArtifactsViewComponent {
  readonly data = input.required<ArtifactsResponse>();
  readonly sessionId = input.required<string>();

  private readonly api = inject(AnalysisApiService);

  protected readonly activeLot = signal<number>(1);
  protected readonly expandedArtifact = signal<string | null>(null);
  protected readonly exportDir = signal('');
  protected readonly isExporting = signal(false);
  protected readonly exportResult = signal<{ targetDirectory: string; exportedFiles: string[]; errors: string[] } | null>(null);
  protected readonly artifactFeedback = signal<Record<string, string>>({});

  protected readonly lotNumbers = computed((): number[] => {
    const lots = new Set<number>();
    for (const art of this.data().artifacts) {
      lots.add(art.lotNumber);
    }
    return Array.from(lots).sort((a, b) => a - b);
  });

  protected readonly activeArtifacts = computed((): CodeArtifactDto[] => {
    return this.data().artifacts.filter(a => a.lotNumber === this.activeLot());
  });

  protected readonly needsReviewCount = computed((): number =>
    this.data().artifacts.filter(a => a.generationStatus === 'WARNING').length
  );

  protected formatWarning(warning: string): string {
    const translations: Record<string, string> = {
      'DUPLICATE_METHOD_NAME': 'Noms de methodes dupliques',
      'MISSING_IMPORT': 'Import(s) potentiellement manquant(s)',
      'EMPTY_BODY': 'Artefact sans methode significative',
      'PARSE_ERROR': 'Erreur de syntaxe Java detectee',
    };
    return translations[warning] ?? warning;
  }

  protected toggleCode(artifactId: string): void {
    this.expandedArtifact.update(current => current === artifactId ? null : artifactId);
  }

  protected copyArtifact(artifact: CodeArtifactDto): void {
    const copied = this.copyTextToClipboard(artifact.content);
    this.setArtifactFeedback(
      artifact.artifactId,
      copied ? 'Code copie dans le presse-papiers' : 'Erreur lors de la copie du code',
    );
  }

  protected downloadArtifact(artifact: CodeArtifactDto): void {
    const fileName = `${artifact.className}.java`;
    try {
      const blob = new Blob([artifact.content], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = fileName;
      anchor.click();
      URL.revokeObjectURL(url);
      this.setArtifactFeedback(artifact.artifactId, `Fichier ${fileName} telecharge`);
    } catch {
      this.setArtifactFeedback(artifact.artifactId, 'Erreur lors du telechargement du fichier');
    }
  }

  protected doExport(): void {
    this.isExporting.set(true);
    this.exportResult.set(null);
    this.api.exportArtifacts(this.sessionId(), this.exportDir().trim()).subscribe({
      next: result => {
        this.exportResult.set(result);
        this.isExporting.set(false);
      },
      error: () => {
        this.exportResult.set({
          targetDirectory: this.exportDir(),
          exportedFiles: [],
          errors: ['Erreur lors de la communication avec le serveur']
        });
        this.isExporting.set(false);
      },
    });
  }

  private copyTextToClipboard(content: string): boolean {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(content).catch(() => undefined);
      return true;
    }

    try {
      const textarea = document.createElement('textarea');
      textarea.value = content;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      return true;
    } catch {
      return false;
    }
  }

  private setArtifactFeedback(artifactId: string, message: string): void {
    this.artifactFeedback.update(current => ({
      ...current,
      [artifactId]: message,
    }));
  }
}
