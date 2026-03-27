import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { RestitutionReportResponse } from '../../../core/models/analysis.model';

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: '#0f766e',
  MEDIUM: '#9a5520',
  LOW: '#c4612c',
  INSUFFICIENT: '#a23c1c',
};

@Component({
  selector: 'jas-report-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .synthesis-card {
      padding: 1rem 1.25rem;
      border-radius: 16px;
      border: 1px solid var(--surface-line);
      background: var(--panel-soft-alt);
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
      background: var(--surface-chip);
    }

    .metric-value {
      font-weight: 700;
      font-size: 1.3rem;
      color: var(--surface-ink-strong);
      font-family: var(--font-display);
    }

    .metric-label {
      font-size: 0.72rem;
      color: var(--surface-ink-soft);
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
      background: var(--surface-success-soft);
      color: var(--surface-success);
    }

    .actionable-no {
      background: var(--surface-danger-soft);
      color: var(--surface-danger);
    }

    .section-label {
      margin: 1.2rem 0 0.5rem;
      font-weight: 700;
      font-size: 0.9rem;
      color: var(--surface-ink-strong);
    }

    .findings-list,
    .unknowns-list {
      margin: 0;
      padding-left: 1.3rem;
    }

    .findings-list li,
    .unknowns-list li {
      margin-bottom: 0.35rem;
      font-size: 0.85rem;
      line-height: 1.45;
    }

    .findings-list li {
      color: var(--surface-ink);
    }

    .unknowns-list li {
      color: var(--surface-danger);
    }

    .markdown-panel {
      margin-top: 1rem;
      padding: 1rem 1.1rem;
      border-radius: 16px;
      border: 1px solid var(--surface-line);
      background: var(--surface-chip);
      overflow: auto;
    }

    .markdown-panel pre {
      margin: 0;
      white-space: pre-wrap;
      word-break: break-word;
      font-family: var(--font-mono, monospace);
      font-size: 0.8rem;
      color: var(--surface-ink);
      line-height: 1.5;
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

    @if (data().markdown) {
      <p class="section-label">Markdown de restitution</p>
      <div class="markdown-panel">
        <pre>{{ data().markdown }}</pre>
      </div>
    }
  `
})
export class ReportViewComponent {
  readonly data = input.required<RestitutionReportResponse>();

  protected readonly confidenceColor = computed((): string => {
    return CONFIDENCE_COLORS[this.data().confidence] ?? CONFIDENCE_COLORS['INSUFFICIENT'];
  });
}
