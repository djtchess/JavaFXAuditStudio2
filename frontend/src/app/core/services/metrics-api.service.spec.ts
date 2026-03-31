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

    const aiHealth = httpMock.expectOne('/actuator/ai-health');
    aiHealth.flush({
      status: 'UP',
      enabled: true,
      provider: 'claude-code',
      circuitBreakerState: 'CLOSED',
      totalRequests: 5,
      successRate: 80,
      p95LatencyMs: 450,
      totalTokens: 1280,
      outcomes: {
        success: 4,
        failure: 1,
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

    const pipelineSummary = httpMock.expectOne('/actuator/metrics/jas.analysis.pipeline.count');
    pipelineSummary.flush({
      name: 'jas.analysis.pipeline.count',
      measurements: [{ statistic: 'COUNT', value: 0 }],
      availableTags: [
        { tag: 'outcome', values: ['success', 'failure'] },
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

    const successPipeline = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.pipeline.count' &&
      req.params.getAll('tag')?.includes('outcome:success') === true
    );
    successPipeline.flush({
      name: 'jas.analysis.pipeline.count',
      measurements: [{ statistic: 'VALUE', value: 9 }],
      availableTags: [],
    });

    const failurePipeline = httpMock.expectOne(req =>
      req.url === '/actuator/metrics/jas.analysis.pipeline.count' &&
      req.params.getAll('tag')?.includes('outcome:failure') === true
    );
    failurePipeline.flush({
      name: 'jas.analysis.pipeline.count',
      measurements: [{ statistic: 'VALUE', value: 2 }],
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
      aiHealth: {
        status: 'UP',
        enabled: true,
        provider: 'claude-code',
        circuitBreakerState: 'CLOSED',
        totalRequests: 5,
        successRate: 80,
        p95LatencyMs: 450,
        totalTokens: 1280,
      },
      pipelineOutcomes: {
        success: 9,
        failure: 2,
      },
      llmOutcomes: {
        success: 4,
        failure: 1,
      },
    });
    expect((snapshotResult as { refreshedAt: string }).refreshedAt).toBeTruthy();
  });
});
