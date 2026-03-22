import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';

const CATEGORY_COLORS: Record<string, string> = {
  UI: '#3b82f6',
  PRESENTATION: '#6366f1',
  APPLICATION: '#10b981',
  BUSINESS: '#f59e0b',
  TECHNICAL: '#6b7280',
  UNKNOWN: '#ef4444',
};

const DEFAULT_COLOR = '#94a3b8';

const SVG_WIDTH = 400;
const SVG_HEIGHT = 200;
const PADDING_TOP = 24;
const PADDING_BOTTOM = 40;
const PADDING_SIDE = 16;
const LABEL_FONT_SIZE = 10;
const VALUE_FONT_SIZE = 10;

interface BarEntry {
  category: string;
  count: number;
  x: number;
  y: number;
  width: number;
  height: number;
  color: string;
  labelX: number;
  labelY: number;
  valueX: number;
  valueY: number;
}

/**
 * Graphe en barres verticales SVG natif pour les règles par catégorie.
 * Standalone, OnPush, sans librairie externe.
 * JAS-014
 */
@Component({
  selector: 'jas-category-bar-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: block;
      width: 100%;
    }

    .chart-svg {
      width: 100%;
      height: auto;
      display: block;
    }

    .chart-empty {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 80px;
      color: var(--ink-soft, #94a3b8);
      font-size: 0.85rem;
    }
  `,
  template: `
    @if (bars().length === 0) {
      <div class="chart-empty">Aucune donnée à afficher</div>
    } @else {
      <svg
        class="chart-svg"
        [attr.viewBox]="viewBox"
        aria-label="Graphe des règles par catégorie"
        role="img"
      >
        @for (bar of bars(); track bar.category) {
          <rect
            [attr.x]="bar.x"
            [attr.y]="bar.y"
            [attr.width]="bar.width"
            [attr.height]="bar.height"
            [attr.fill]="bar.color"
            rx="3"
          />
          <text
            [attr.x]="bar.valueX"
            [attr.y]="bar.valueY"
            text-anchor="middle"
            [attr.font-size]="valueFontSize"
            fill="#374151"
            font-weight="600"
          >{{ bar.count }}</text>
          <text
            [attr.x]="bar.labelX"
            [attr.y]="bar.labelY"
            text-anchor="middle"
            [attr.font-size]="labelFontSize"
            fill="#6b7280"
          >{{ bar.category }}</text>
        }
      </svg>
    }
  `,
})
export class CategoryBarChartComponent {
  readonly data = input.required<Record<string, number>>();

  protected readonly viewBox = `0 0 ${SVG_WIDTH} ${SVG_HEIGHT}`;
  protected readonly labelFontSize = LABEL_FONT_SIZE;
  protected readonly valueFontSize = VALUE_FONT_SIZE;

  protected readonly bars = computed<BarEntry[]>(() => {
    const entries = Object.entries(this.data());
    if (entries.length === 0) return [];

    const maxCount = Math.max(...entries.map(([, v]) => v), 1);
    const chartWidth = SVG_WIDTH - PADDING_SIDE * 2;
    const chartHeight = SVG_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
    const barWidth = Math.max(
      8,
      Math.floor(chartWidth / entries.length) - 6
    );

    return entries.map(([category, count], index) =>
      this.buildBar(category, count, index, entries.length, maxCount, chartHeight, chartWidth, barWidth)
    );
  });

  private buildBar(
    category: string,
    count: number,
    index: number,
    total: number,
    maxCount: number,
    chartHeight: number,
    chartWidth: number,
    barWidth: number
  ): BarEntry {
    const slotWidth = chartWidth / total;
    const centerX = PADDING_SIDE + slotWidth * index + slotWidth / 2;
    const barHeight = Math.max(4, Math.round((count / maxCount) * chartHeight));
    const x = centerX - barWidth / 2;
    const y = PADDING_TOP + (chartHeight - barHeight);

    return {
      category,
      count,
      x,
      y,
      width: barWidth,
      height: barHeight,
      color: CATEGORY_COLORS[category] ?? DEFAULT_COLOR,
      labelX: centerX,
      labelY: SVG_HEIGHT - PADDING_BOTTOM + LABEL_FONT_SIZE + 4,
      valueX: centerX,
      valueY: y - 4,
    };
  }
}
