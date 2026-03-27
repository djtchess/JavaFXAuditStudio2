import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { MonitoringDashboardComponent } from './monitoring-dashboard.component';
import { MetricsApiService } from '../../core/services/metrics-api.service';

function createMetricsServiceSpy() {
  return {
    loadMonitoringSnapshot: vi.fn().mockReturnValue(of({
      totalSessions: 10,
      statusMetrics: [
        { status: 'created', label: 'Creees', count: 3 },
        { status: 'completed', label: 'Terminees', count: 7 },
      ],
      stageMetrics: [
        { stage: 'ingest', label: 'Ingestion', count: 2, averageMs: 150 },
        { stage: 'cartography', label: 'Cartographie', count: 2, averageMs: 220 },
      ],
      healthStatus: 'UP',
      healthComponents: [],
      pipelineOutcomes: {},
      llmOutcomes: {},
      refreshedAt: '2026-03-27T07:00:00Z',
    })),
  };
}

describe('MonitoringDashboardComponent', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('should display monitoring metrics and refresh automatically', async () => {
    vi.useFakeTimers();

    const metricsSpy = createMetricsServiceSpy();

    TestBed.configureTestingModule({
      imports: [MonitoringDashboardComponent],
      providers: [{ provide: MetricsApiService, useValue: metricsSpy }],
    });

    const fixture: ComponentFixture<MonitoringDashboardComponent> = TestBed.createComponent(
      MonitoringDashboardComponent
    );
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();

    expect(metricsSpy.loadMonitoringSnapshot).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Sessions totales');
    expect(fixture.nativeElement.textContent).toContain('10');
    expect(fixture.nativeElement.textContent).toContain('Creees');
    expect(fixture.nativeElement.textContent).toContain('150 ms');

    vi.advanceTimersByTime(30_000);
    await Promise.resolve();
    fixture.detectChanges();

    expect(metricsSpy.loadMonitoringSnapshot).toHaveBeenCalledTimes(2);
  });
});
