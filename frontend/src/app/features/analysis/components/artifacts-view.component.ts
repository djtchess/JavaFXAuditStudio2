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
      background: rgba(245, 158, 11, 0.08);
      border: 1px solid rgba(245, 158, 11, 0.3);
      color: #92400e;
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
      border: 1px solid var(--line);
      border-radius: 8px;
      font-size: 0.82rem;
      background: rgba(255,255,255,0.8);
      color: var(--ink);
      font-family: monospace;
    }

    .export-input:focus {
      outline: 2px solid var(--slate);
      outline-offset: 1px;
    }

    .export-btn {
      padding: 0.4rem 1rem;
      border: 1px solid var(--line);
      border-radius: 999px;
      background: var(--slate);
      color: white;
      font-weight: 600;
      font-size: 0.82rem;
      cursor: pointer;
      white-space: nowrap;
      transition: opacity 0.15s;
    }

    .export-btn:hover:not(:disabled) { opacity: 0.8; }
    .export-btn:disabled { opacity: 0.45; cursor: not-allowed; }

    .export-result {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      font-size: 0.8rem;
    }

    .export-ok  { color: #16a34a; }
    .export-err { color: #dc2626; }

    .tabs-bar {
      display: flex;
      gap: 0.35rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }

    .tab-btn {
      padding: 0.35rem 0.85rem;
      border: 1px solid var(--line);
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.6);
      color: var(--ink-soft);
      font-weight: 600;
      font-size: 0.82rem;
      cursor: pointer;
      transition: background 0.15s, color 0.15s, border-color 0.15s;
    }

    .tab-btn:hover { background: var(--accent-soft); color: var(--slate); }
    .tab-btn.active { background: var(--slate); color: white; border-color: var(--slate); }

    .artifacts-grid { display: grid; gap: 0.5rem; }

    .artifact-card {
      border-radius: 12px;
      border: 1px solid var(--line);
      background: rgba(255, 255, 255, 0.6);
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
      color: var(--slate);
      flex: 1;
    }

    .type-badge {
      display: inline-block;
      padding: 0.18rem 0.55rem;
      border-radius: 999px;
      background: rgba(99, 102, 241, 0.1);
      color: #6366f1;
      font-size: 0.72rem;
      font-weight: 600;
    }

    .bridge-badge {
      display: inline-block;
      padding: 0.18rem 0.55rem;
      border-radius: 999px;
      background: rgba(245, 158, 11, 0.1);
      color: #d97706;
      font-size: 0.72rem;
      font-weight: 600;
    }

    .code-toggle {
      padding: 0.22rem 0.65rem;
      border: 1px solid var(--line);
      border-radius: 999px;
      background: transparent;
      color: var(--ink-soft);
      font-size: 0.75rem;
      cursor: pointer;
      transition: background 0.12s;
    }

    .code-toggle:hover { background: var(--accent-soft); color: var(--slate); }

    .code-block {
      background: #1e1e2e;
      color: #cdd6f4;
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
      color: var(--ink-soft);
      font-size: 0.85rem;
      font-style: italic;
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

    <!-- Panneau export -->
    <div class="export-panel">
      <input
        class="export-input"
        type="text"
        [value]="exportDir()"
        (input)="exportDir.set($any($event.target).value)"
        placeholder="Répertoire cible (ex: C:\\monprojet\\src\\main\\java\\migration)"
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
            <span class="export-ok">✓ {{ result.exportedFiles.length }} fichier(s) exporté(s) dans {{ result.targetDirectory }}</span>
          }
          @for (e of result.errors; track e) {
            <span class="export-err">✗ {{ e }}</span>
          }
        </div>
      }
    </div>

    <!-- Onglets par lot -->
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
              <button
                class="code-toggle"
                (click)="toggleCode(art.artifactId)"
              >
                {{ expandedArtifact() === art.artifactId ? 'Masquer' : 'Voir le code' }}
              </button>
            </div>
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

  protected toggleCode(artifactId: string): void {
    this.expandedArtifact.update(current => current === artifactId ? null : artifactId);
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
        this.exportResult.set({ targetDirectory: this.exportDir(), exportedFiles: [], errors: ['Erreur lors de la communication avec le serveur'] });
        this.isExporting.set(false);
      },
    });
  }
}
