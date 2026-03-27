import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { MigrationPlanResponse } from '../../../core/models/analysis.model';

@Component({
  selector: 'jas-migration-plan-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .compilable-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.3rem 0.8rem;
      border-radius: 999px;
      font-weight: 600;
      font-size: 0.82rem;
      margin-bottom: 1rem;
    }

    .compilable-yes {
      background: var(--surface-success-soft);
      color: var(--surface-success);
    }

    .compilable-no {
      background: var(--surface-danger-soft);
      color: var(--surface-danger);
    }

    .stepper {
      display: grid;
      gap: 0;
      position: relative;
      padding-left: 1.6rem;
    }

    .lot-step {
      position: relative;
      padding: 0 0 1.5rem 1.2rem;
      border-left: 2px solid var(--surface-line);
    }

    .lot-step:last-child {
      border-left-color: transparent;
      padding-bottom: 0;
    }

    .lot-number {
      position: absolute;
      left: -1.1rem;
      top: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      background: var(--surface-chip-strong);
      color: var(--surface-ink-strong);
      font-weight: 700;
      font-size: 0.85rem;
      border: 2px solid rgba(255, 255, 255, 0.7);
    }

    .lot-title {
      margin: 0 0 0.2rem;
      font-weight: 700;
      font-size: 0.95rem;
      color: var(--surface-ink-strong);
      font-family: var(--font-display);
    }

    .lot-objective {
      margin: 0 0 0.6rem;
      font-size: 0.85rem;
      color: var(--surface-ink-soft);
      line-height: 1.45;
    }

    .candidates {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }

    .candidate-chip {
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      background: var(--surface-chip);
      border: 1px solid var(--surface-line);
      color: var(--surface-ink);
      font-size: 0.78rem;
      font-family: monospace;
      font-weight: 500;
    }

    .empty-msg {
      padding: 0.6rem 0;
      color: var(--ink-soft);
      font-size: 0.85rem;
      font-style: italic;
    }
  `,
  template: `
    @if (data().compilable) {
      <span class="compilable-badge compilable-yes">Compilable</span>
    } @else {
      <span class="compilable-badge compilable-no">Non compilable</span>
    }

    @if (data().lots.length === 0) {
      <p class="empty-msg">Aucun lot planifie.</p>
    } @else {
      <div class="stepper">
        @for (lot of data().lots; track lot.lotNumber) {
          <div class="lot-step">
            <span class="lot-number">{{ lot.lotNumber }}</span>
            <p class="lot-title">{{ lot.title }}</p>
            <p class="lot-objective">{{ lot.objective }}</p>
            @if (lot.extractionCandidates.length > 0) {
              <div class="candidates">
                @for (candidate of lot.extractionCandidates; track candidate) {
                  <span class="candidate-chip">{{ candidate }}</span>
                }
              </div>
            }
          </div>
        }
      </div>
    }
  `
})
export class MigrationPlanViewComponent {
  readonly data = input.required<MigrationPlanResponse>();
}
