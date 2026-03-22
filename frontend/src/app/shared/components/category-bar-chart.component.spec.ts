import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { describe, it, expect, beforeEach } from 'vitest';

import { CategoryBarChartComponent } from './category-bar-chart.component';

@Component({
  template: `<jas-category-bar-chart [data]="chartData" />`,
  imports: [CategoryBarChartComponent],
})
class TestHostComponent {
  chartData: Record<string, number> = {};
}

describe('CategoryBarChartComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();
  });

  it('should render one rect per category', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.chartData = {
      UI: 5,
      APPLICATION: 8,
      BUSINESS: 12,
    };
    fixture.detectChanges();

    const rects: NodeListOf<SVGRectElement> = fixture.nativeElement.querySelectorAll('rect');
    expect(rects.length).toBe(3);
  });

  it('should render 5 bars for 5 categories', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.chartData = {
      UI: 5,
      APPLICATION: 8,
      BUSINESS: 12,
      TECHNICAL: 4,
      UNKNOWN: 1,
    };
    fixture.detectChanges();

    const rects: NodeListOf<SVGRectElement> = fixture.nativeElement.querySelectorAll('rect');
    expect(rects.length).toBe(5);
  });

  it('should display empty message when data is empty', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.chartData = {};
    fixture.detectChanges();

    const svg: SVGElement | null = fixture.nativeElement.querySelector('svg');
    expect(svg).toBeNull();

    const emptyDiv: HTMLElement | null = fixture.nativeElement.querySelector('.chart-empty');
    expect(emptyDiv).not.toBeNull();
  });

  it('should render an SVG with correct viewBox', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.chartData = { UI: 3 };
    fixture.detectChanges();

    const svg: SVGElement | null = fixture.nativeElement.querySelector('svg');
    expect(svg).not.toBeNull();
    expect(svg!.getAttribute('viewBox')).toBe('0 0 400 200');
  });

  it('should assign colors to bars', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.chartData = { UI: 5, UNKNOWN: 1 };
    fixture.detectChanges();

    const rects: NodeListOf<SVGRectElement> = fixture.nativeElement.querySelectorAll('rect');
    expect(rects.length).toBe(2);
    const fills = Array.from(rects).map(r => r.getAttribute('fill'));
    expect(fills).toContain('#3b82f6');
    expect(fills).toContain('#ef4444');
  });

  it('should render value and category label text elements', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.chartData = { BUSINESS: 7, TECHNICAL: 3 };
    fixture.detectChanges();

    const texts: NodeListOf<SVGTextElement> = fixture.nativeElement.querySelectorAll('text');
    // 4 text elements: 2 values + 2 category labels
    expect(texts.length).toBe(4);
    const textContents = Array.from(texts).map(t => t.textContent?.trim() ?? '');
    expect(textContents).toContain('7');
    expect(textContents).toContain('3');
    expect(textContents).toContain('BUSINESS');
    expect(textContents).toContain('TECHNICAL');
  });
});
