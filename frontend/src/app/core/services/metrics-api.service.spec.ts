import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { MetricsApiService } from './metrics-api.service';

describe('MetricsApiService', () => {
  let service: MetricsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MetricsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load and map monitoring snapshot from Actuator metrics', () => {
    let snapshotResult: unknown;

    service.loadMonitoringSnapshot().subscribe(result => {
      snapshotResult = result;
    });

    const health = httpMock.expectOne('/actuator/health');
    health.flush({
      status: 'UP',
      components: {
        analysisWorkflow: { status: 'UP' },
        llmEnrichment: { status: 'UP' },
      },
    });

    const sessionsSummary = httpMock.expectOne('/actuator/metrics/jas.analysis.sessions');
    sessionsSummary.flush({
      name: 'jas.analysis.sessions',
      measurements: [{ statistic: 'VALUE', value: 0 }],
      availableTags: [
        { tag: 'status', values: ['total', 'created', 'completed'] },
      ],
    });

    const stageSummary = httpMock.expectOne('/actuator/metrics/jas.analysis.pipeline.stage.duration');
    stageSummary.flush({
      name: 'jas.analysis.pipeline.stage.duration',
      measurements: [{ statistic: 'COUNT', value: 0 }],
      availableTags: [
        { tag: 'stage', values: ['ingest', 'cartography'] },
      ],
    });

    const totalSessions = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.sessions' &&
      req.params.getAll('tag')?.includes('status:total') === true
    );
    totalSessions.flush({
      name: 'jas.analysis.sessions',
      measurements: [{ statistic: 'VALUE', value: 12 }],
      availableTags: [],
    });

    const createdSessions = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.sessions' &&
      req.params.getAll('tag')?.includes('status:created') === true
    );
    createdSessions.flush({
      name: 'jas.analysis.sessions',
      measurements: [{ statistic: 'VALUE', value: 4 }],
      availableTags: [],
    });

    const completedSessions = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.sessions' &&
      req.params.getAll('tag')?.includes('status:completed') === true
    );
    completedSessions.flush({
      name: 'jas.analysis.sessions',
      measurements: [{ statistic: 'VALUE', value: 8 }],
      availableTags: [],
    });

    const ingestStage = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.pipeline.stage.duration' &&
      req.params.getAll('tag')?.includes('stage:ingest') === true
    );
    ingestStage.flush({
      name: 'jas.analysis.pipeline.stage.duration',
      baseUnit: 'seconds',
      measurements: [
        { statistic: 'COUNT', value: 2 },
        { statistic: 'TOTAL_TIME', value: 1.2 },
      ],
      availableTags: [],
    });

    const cartographyStage = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.pipeline.stage.duration' &&
      req.params.getAll('tag')?.includes('stage:cartography') === true
    );
    cartographyStage.flush({
      name: 'jas.analysis.pipeline.stage.duration',
      baseUnit: 'seconds',
      measurements: [
        { statistic: 'COUNT', value: 1 },
        { statistic: 'TOTAL_TIME', value: 0.6 },
      ],
      availableTags: [],
    });

    expect(snapshotResult).toMatchObject({
      totalSessions: 12,
      statusMetrics: [
        { status: 'created', label: 'Creees', count: 4 },
        { status: 'completed', label: 'Terminees', count: 8 },
      ],
      stageMetrics: [
        { stage: 'ingest', label: 'Ingestion', count: 2, averageMs: 600 },
        { stage: 'cartography', label: 'Cartographie', count: 1, averageMs: 600 },
      ],
      healthStatus: 'UP',
      healthComponents: [
        { name: 'analysisWorkflow', status: 'UP' },
        { name: 'llmEnrichment', status: 'UP' },
      ],
      pipelineOutcomes: {},
      llmOutcomes: {},
    });
    expect((snapshotResult as { refreshedAt: string }).refreshedAt).toBeTruthy();
  });
});
