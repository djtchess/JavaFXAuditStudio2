import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpHeaders, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { correlationIdInterceptor } from './correlation-id.interceptor';
import { errorInterceptor } from './error.interceptor';
import { frontendMonitoringInterceptor } from './frontend-monitoring.interceptor';
import { FrontendMonitoringService } from '../services/frontend-monitoring.service';

describe('frontendMonitoringInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let monitoring: FrontendMonitoringService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withInterceptors([correlationIdInterceptor, frontendMonitoringInterceptor, errorInterceptor])
        ),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    monitoring = TestBed.inject(FrontendMonitoringService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should record successful requests with the response correlation id', () => {
    http.get('/api/v1/monitoring/success').subscribe();

    const req = httpMock.expectOne('/api/v1/monitoring/success');
    req.flush(
      { ok: true },
      {
        status: 200,
        statusText: 'OK',
        headers: new HttpHeaders({ 'X-Correlation-Id': 'corr-response-42' }),
      }
    );

    expect(monitoring.summary().inflightRequests).toBe(0);
    expect(monitoring.summary().totalCompletedRequests).toBe(1);
    expect(monitoring.summary().failedRequests).toBe(0);
    expect(monitoring.recentRequests()).toHaveLength(1);
    expect(monitoring.recentRequests()[0].correlationId).toBe('corr-response-42');
    expect(monitoring.recentRequests()[0].status).toBe(200);
    expect(monitoring.recentRequests()[0].succeeded).toBe(true);
  });

  it('should record normalized frontend failures with the request correlation id fallback', () => {
    let receivedError: unknown;

    http.get('/api/v1/monitoring/failure').subscribe({
      error: (error) => {
        receivedError = error;
      },
    });

    const req = httpMock.expectOne('/api/v1/monitoring/failure');
    const requestCorrelationId = req.request.headers.get('X-Correlation-Id');
    req.flush(
      { message: 'backend exploded' },
      {
        status: 500,
        statusText: 'Server Error',
      }
    );

    expect(receivedError).toBeTruthy();
    expect(monitoring.summary().inflightRequests).toBe(0);
    expect(monitoring.summary().totalCompletedRequests).toBe(1);
    expect(monitoring.summary().failedRequests).toBe(1);
    expect(monitoring.recentFailures()).toHaveLength(1);
    expect(monitoring.recentFailures()[0].correlationId).toBe(requestCorrelationId);
    expect(monitoring.recentFailures()[0].message).toBe('backend exploded');
    expect(monitoring.recentFailures()[0].status).toBe(500);
  });
});
