import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const CATEGORY_COLORS: Record<string, string> = {
  UI: '#3b82f6',
  PRESENTATION: '#6366f1',
  APPLICATION: '#10b981',
  BUSINESS: '#f59e0b',
  TECHNICAL: '#6b7280',
  UNKNOWN: '#ef4444',
};

/**
 * Badge de classification d'une regle de gestion.
 * Affiche la categorie avec code couleur, indicateur d'incertitude et mode de parsing.
 * JAS-016
 */
@Component({
  selector: 'jas-classification-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .badge-container {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
    }

    .class-badge {
      padding: 0.2rem 0.55rem;
      border-radius: 999px;
      font-size: 0.72rem;
      font-weight: 600;
      color: white;
      white-space: nowrap;
    }

    .uncertain-dot {
      width: 1.1rem;
      height: 1.1rem;
      border-radius: 50%;
      background: rgba(239, 68, 68, 0.15);
      color: #dc2626;
      font-size: 0.65rem;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .parsing-badge {
      padding: 0.15rem 0.4rem;
      border-radius: 4px;
      font-size: 0.65rem;
      font-weight: 700;
    }

    .parsing-badge.ast {
      background: #dcfce7;
      color: #16a34a;
    }

    .parsing-badge.regex {
      background: #fff7ed;
      color: #c2410c;
    }
  `,
  template: `
    <span class="badge-container">
      <span class="class-badge" [style.background]="categoryColor()">
        {{ responsibilityClass() }}
      </span>
      @if (uncertain()) {
        <span class="uncertain-dot" title="Classification incertaine">?</span>
      }
      @if (showParsingMode()) {
        <span class="parsing-badge" [class.ast]="parsingMode() === 'AST'" [class.regex]="parsingMode() === 'REGEX_FALLBACK'">
          {{ parsingMode() === 'AST' ? 'AST' : 'REGEX' }}
        </span>
      }
    </span>
  `
})
export class ClassificationBadgeComponent {
  readonly responsibilityClass = input.required<string>();
  readonly uncertain = input<boolean>(false);
  readonly parsingMode = input<'AST' | 'REGEX_FALLBACK'>('AST');
  readonly showParsingMode = input<boolean>(false);

  protected readonly categoryColor = computed(() =>
    CATEGORY_COLORS[this.responsibilityClass()] ?? CATEGORY_COLORS['UNKNOWN']
  );
}
