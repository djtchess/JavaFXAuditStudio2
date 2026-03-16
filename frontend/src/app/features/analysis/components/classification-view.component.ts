import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { ClassificationResponse, BusinessRuleDto } from '../../../core/models/analysis.model';

const RESPONSIBILITY_COLORS: Record<string, string> = {
  UI: '#3b82f6',
  PRESENTATION: '#6366f1',
  APPLICATION: '#10b981',
  BUSINESS: '#f59e0b',
  TECHNICAL: '#6b7280',
  UNKNOWN: '#ef4444',
};

@Component({
  selector: 'jas-classification-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .summary-bar {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }

    .class-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.28rem 0.7rem;
      border-radius: 999px;
      color: white;
      font-weight: 600;
      font-size: 0.78rem;
      cursor: pointer;
      transition: opacity 0.15s, transform 0.15s;
      user-select: none;
      border: 2px solid transparent;
    }

    .class-badge:hover {
      opacity: 0.85;
    }

    .class-badge.active {
      border-color: var(--slate);
      transform: scale(1.05);
    }

    .uncertain-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.28rem 0.7rem;
      border-radius: 999px;
      background: rgba(239, 68, 68, 0.1);
      color: #dc2626;
      font-weight: 600;
      font-size: 0.78rem;
      margin-left: auto;
    }

    .rules-list {
      display: grid;
      gap: 0.5rem;
    }

    .rule-row {
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
      padding: 0.65rem 0.75rem;
      border-radius: 12px;
      border: 1px solid var(--line);
      background: rgba(255, 255, 255, 0.6);
      font-size: 0.85rem;
    }

    .rule-id {
      font-family: monospace;
      font-size: 0.78rem;
      color: var(--ink-soft);
      white-space: nowrap;
      min-width: 4rem;
      flex-shrink: 0;
    }

    .rule-desc {
      flex: 1;
      color: var(--slate);
      line-height: 1.4;
    }

    .rule-badges {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      flex-shrink: 0;
      flex-wrap: wrap;
    }

    .mini-badge {
      display: inline-block;
      padding: 0.2rem 0.55rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      color: white;
      white-space: nowrap;
    }

    .extraction-badge {
      background: rgba(18, 35, 56, 0.08);
      color: var(--slate);
    }

    .uncertain-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.3rem;
      height: 1.3rem;
      border-radius: 50%;
      background: rgba(239, 68, 68, 0.12);
      color: #dc2626;
      font-size: 0.72rem;
      font-weight: 700;
      flex-shrink: 0;
    }

    .empty-msg {
      padding: 0.6rem 0;
      color: var(--ink-soft);
      font-size: 0.85rem;
      font-style: italic;
    }
  `,
  template: `
    <div class="summary-bar">
      @for (entry of classCounts(); track entry.className) {
        <span
          class="class-badge"
          [class.active]="filterBy() === entry.className"
          [style.background]="getColor(entry.className)"
          (click)="toggleFilter(entry.className)"
        >
          {{ entry.className }} ({{ entry.count }})
        </span>
      }
      @if (data().uncertainCount > 0) {
        <span class="uncertain-badge">{{ data().uncertainCount }} incertaines</span>
      }
    </div>

    @if (filteredRules().length === 0) {
      <p class="empty-msg">Aucune regle correspondante.</p>
    } @else {
      <div class="rules-list">
        @for (rule of filteredRules(); track rule.ruleId) {
          <div class="rule-row">
            <span class="rule-id">{{ rule.ruleId }}</span>
            <span class="rule-desc">{{ rule.description }}</span>
            <div class="rule-badges">
              <span class="mini-badge" [style.background]="getColor(rule.responsibilityClass)">
                {{ rule.responsibilityClass }}
              </span>
              @if (rule.extractionCandidate) {
                <span class="mini-badge extraction-badge">{{ rule.extractionCandidate }}</span>
              }
              @if (rule.uncertain) {
                <span class="uncertain-icon" title="Classification incertaine">?</span>
              }
            </div>
          </div>
        }
      </div>
    }
  `
})
export class ClassificationViewComponent {
  readonly data = input.required<ClassificationResponse>();

  protected readonly filterBy = signal<string | null>(null);

  protected readonly classCounts = computed(() => {
    const map = new Map<string, number>();
    for (const rule of this.data().rules) {
      map.set(rule.responsibilityClass, (map.get(rule.responsibilityClass) ?? 0) + 1);
    }
    return Array.from(map.entries())
      .map(([className, count]) => ({ className, count }))
      .sort((a, b) => b.count - a.count);
  });

  protected readonly filteredRules = computed((): BusinessRuleDto[] => {
    const filter = this.filterBy();
    if (!filter) {
      return this.data().rules;
    }
    return this.data().rules.filter(r => r.responsibilityClass === filter);
  });

  protected getColor(responsibilityClass: string): string {
    return RESPONSIBILITY_COLORS[responsibilityClass] ?? RESPONSIBILITY_COLORS['UNKNOWN'];
  }

  protected toggleFilter(className: string): void {
    this.filterBy.update(current => current === className ? null : className);
  }
}
