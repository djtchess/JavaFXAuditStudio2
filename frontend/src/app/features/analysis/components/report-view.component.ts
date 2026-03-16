import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { RestitutionReportResponse } from '../../../core/models/analysis.model';

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: '#059669',
  MEDIUM: '#ca8a04',
  LOW: '#ea580c',
  INSUFFICIENT: '#dc2626',
};

@Component({
  selector: 'jas-report-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .synthesis-card {
      padding: 1rem 1.25rem;
      border-radius: 16px;
      border: 1px solid var(--line);
      background: rgba(255, 255, 255, 0.7);
      margin-bottom: 1rem;
    }

    .synthesis-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
    }

    .metric {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 0.4rem 0.9rem;
      border-radius: 12px;
      background: rgba(18, 35, 56, 0.04);
    }

    .metric-value {
      font-weight: 700;
      font-size: 1.3rem;
      color: var(--slate);
      font-family: var(--font-display);
    }

    .metric-label {
      font-size: 0.72rem;
      color: var(--ink-soft);
      text-transform: uppercase;
      letter-spacing: 0.06em;
      font-weight: 600;
    }

    .confidence-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.3rem 0.8rem;
      border-radius: 999px;
      color: white;
      font-weight: 600;
      font-size: 0.82rem;
    }

    .actionable-badge {
      display: inline-flex;
      align-items: center;
      padding: 0.3rem 0.8rem;
      border-radius: 999px;
      font-weight: 600;
      font-size: 0.82rem;
    }

    .actionable-yes {
      background: rgba(16, 185, 129, 0.1);
      color: #059669;
    }

    .actionable-no {
      background: rgba(239, 68, 68, 0.1);
      color: #dc2626;
    }

    .section-label {
      margin: 1.2rem 0 0.5rem;
      font-weight: 700;
      font-size: 0.9rem;
      color: var(--slate);
    }

    .findings-list {
      margin: 0;
      padding-left: 1.3rem;
    }

    .findings-list li {
      margin-bottom: 0.35rem;
      font-size: 0.85rem;
      color: var(--slate);
      line-height: 1.45;
    }

    .unknowns-list {
      margin: 0;
      padding-left: 1.3rem;
    }

    .unknowns-list li {
      margin-bottom: 0.35rem;
      font-size: 0.85rem;
      color: #b94517;
      line-height: 1.45;
    }
  `,
  template: `
    <div class="synthesis-card">
      <div class="synthesis-row">
        <div class="metric">
          <span class="metric-value">{{ data().ruleCount }}</span>
          <span class="metric-label">Regles</span>
        </div>
        <div class="metric">
          <span class="metric-value">{{ data().artifactCount }}</span>
          <span class="metric-label">Artefacts</span>
        </div>
        <span
          class="confidence-badge"
          [style.background]="confidenceColor()"
        >
          {{ data().confidence }}
        </span>
        @if (data().isActionable) {
          <span class="actionable-badge actionable-yes">Actionnable</span>
        } @else {
          <span class="actionable-badge actionable-no">Non actionnable</span>
        }
      </div>
    </div>

    @if (data().findings.length > 0) {
      <p class="section-label">Conclusions</p>
      <ul class="findings-list">
        @for (f of data().findings; track f) {
          <li>{{ f }}</li>
        }
      </ul>
    }

    @if (data().unknowns.length > 0) {
      <p class="section-label">Inconnues</p>
      <ul class="unknowns-list">
        @for (u of data().unknowns; track u) {
          <li>{{ u }}</li>
        }
      </ul>
    }
  `
})
export class ReportViewComponent {
  readonly data = input.required<RestitutionReportResponse>();

  protected readonly confidenceColor = computed((): string => {
    return CONFIDENCE_COLORS[this.data().confidence] ?? CONFIDENCE_COLORS['INSUFFICIENT'];
  });
}
