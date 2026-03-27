import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpHeaders, withInterceptors, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should log the correlation id when a request fails', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    let receivedError: unknown;

    http.get('/api/v1/fail').subscribe({
      error: err => {
        receivedError = err;
      },
    });

    const req = httpMock.expectOne('/api/v1/fail');
    req.flush(
      { message: 'boom' },
      {
        status: 500,
        statusText: 'Server Error',
        headers: new HttpHeaders({ 'X-Correlation-Id': 'corr-42' }),
      },
    );

    expect(receivedError).toBeTruthy();
    expect(consoleSpy).toHaveBeenCalled();
    expect(consoleSpy.mock.calls.at(-1)?.[0]).toContain('Correlation-Id: corr-42');
    consoleSpy.mockRestore();
  });
});
