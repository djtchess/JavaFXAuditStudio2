import { HttpErrorResponse, HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize, tap } from 'rxjs';

import { FrontendMonitoringService } from '../services/frontend-monitoring.service';

export const frontendMonitoringInterceptor: HttpInterceptorFn = (req, next) => {
  const monitoring = inject(FrontendMonitoringService);
  const requestStartedAt = Date.now();
  const requestCorrelationId = req.headers.get('X-Correlation-Id') ?? 'unknown';

  monitoring.requestStarted();

  return next(req).pipe(
    tap({
      next: (event) => {
        if (event.type !== HttpEventType.Response) {
          return;
        }

        monitoring.recordCompletedRequest({
          method: req.method,
          url: req.urlWithParams,
          status: event.status,
          durationMs: Date.now() - requestStartedAt,
          correlationId: event.headers.get('X-Correlation-Id') ?? requestCorrelationId,
          succeeded: true,
          message: null,
          completedAt: new Date().toISOString(),
        });
      },
      error: (error: unknown) => {
        monitoring.recordCompletedRequest({
          method: req.method,
          url: req.urlWithParams,
          status: resolveStatus(error),
          durationMs: Date.now() - requestStartedAt,
          correlationId: resolveCorrelationId(error, requestCorrelationId),
          succeeded: false,
          message: resolveMessage(error),
          completedAt: new Date().toISOString(),
        });
      },
    }),
    finalize(() => {
      monitoring.requestFinished();
    })
  );
};

function resolveStatus(error: unknown): number {
  if (error instanceof HttpErrorResponse) {
    return error.status;
  }
  return 0;
}

function resolveCorrelationId(error: unknown, fallbackCorrelationId: string): string {
  if (error instanceof HttpErrorResponse) {
    return error.headers?.get('X-Correlation-Id') ?? fallbackCorrelationId;
  }
  return fallbackCorrelationId;
}

function resolveMessage(error: unknown): string | null {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error?.message === 'string' && error.error.message.trim().length > 0) {
      return error.error.message;
    }
    if (typeof error.message === 'string' && error.message.trim().length > 0) {
      return error.message;
    }
    if (typeof error.statusText === 'string' && error.statusText.trim().length > 0) {
      return error.statusText;
    }
  }
  return null;
}
