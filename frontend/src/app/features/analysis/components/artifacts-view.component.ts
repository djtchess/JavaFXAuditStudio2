import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { ArtifactsResponse, CodeArtifactDto } from '../../../core/models/analysis.model';

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

    .tab-btn:hover {
      background: var(--accent-soft);
      color: var(--slate);
    }

    .tab-btn.active {
      background: var(--slate);
      color: white;
      border-color: var(--slate);
    }

    .artifacts-grid {
      display: grid;
      gap: 0.5rem;
    }

    .artifact-card {
      padding: 0.7rem 0.9rem;
      border-radius: 12px;
      border: 1px solid var(--line);
      background: rgba(255, 255, 255, 0.6);
    }

    .artifact-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .artifact-name {
      font-weight: 700;
      font-size: 0.9rem;
      font-family: monospace;
      color: var(--slate);
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
          @for (w of data().warnings; track w) {
            <li>{{ w }}</li>
          }
        </ul>
      </div>
    }

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
            </div>
          </div>
        }
      </div>
    }
  `
})
export class ArtifactsViewComponent {
  readonly data = input.required<ArtifactsResponse>();

  protected readonly activeLot = signal<number>(1);

  protected readonly lotNumbers = computed((): number[] => {
    const lots = new Set<number>();
    for (const art of this.data().artifacts) {
      lots.add(art.lotNumber);
    }
    return Array.from(lots).sort((a, b) => a - b);
  });

  protected readonly activeArtifacts = computed((): CodeArtifactDto[] => {
    const lot = this.activeLot();
    return this.data().artifacts.filter(a => a.lotNumber === lot);
  });
}
