import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpHeaders, withInterceptors, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { correlationIdInterceptor } from './correlation-id.interceptor';

describe('correlationIdInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add a correlation id header to outgoing requests', () => {
    http.get('/api/v1/ping').subscribe();

    const req = httpMock.expectOne('/api/v1/ping');
    const correlationId = req.request.headers.get('X-Correlation-Id');
    expect(req.request.method).toBe('GET');
    expect(correlationId).toBeTruthy();
    req.flush({}, { headers: new HttpHeaders() });
  });
});
