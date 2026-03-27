import { describe, expect, it } from 'vitest';

import { FrontendMonitoringService } from './frontend-monitoring.service';

describe('FrontendMonitoringService', () => {
  it('should aggregate request outcomes and expose recent failures', () => {
    const service = new FrontendMonitoringService();

    service.requestStarted();
    service.recordCompletedRequest({
      method: 'GET',
      url: '/api/v1/ok',
      status: 200,
      durationMs: 120,
      correlationId: 'corr-ok',
      succeeded: true,
      message: null,
      completedAt: '2026-03-27T10:00:00.000Z',
    });
    service.requestFinished();

    service.requestStarted();
    service.recordCompletedRequest({
      method: 'GET',
      url: '/api/v1/fail',
      status: 500,
      durationMs: 80,
      correlationId: 'corr-fail',
      succeeded: false,
      message: 'boom',
      completedAt: '2026-03-27T10:00:01.000Z',
    });
    service.requestFinished();

    expect(service.summary().inflightRequests).toBe(0);
    expect(service.summary().totalCompletedRequests).toBe(2);
    expect(service.summary().failedRequests).toBe(1);
    expect(service.summary().averageDurationMs).toBe(100);
    expect(service.recentFailures()).toHaveLength(1);
    expect(service.recentFailures()[0].correlationId).toBe('corr-fail');
  });
});
