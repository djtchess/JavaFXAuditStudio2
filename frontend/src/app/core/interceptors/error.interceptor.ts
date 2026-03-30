import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error) => {
      const correlationId = error.headers?.get('X-Correlation-Id') ?? 'unknown';
      const normalizedError = normalizeHttpError(error, req.url);
      console.error(
        `[HTTP Error] ${error.status} ${error.statusText} | Correlation-Id: ${correlationId} | URL: ${error.url ?? req.url}`
      );
      return throwError(() => normalizedError);
    })
  );
};

function normalizeHttpError(error: HttpErrorResponse, requestUrl: string): HttpErrorResponse {
  const userMessage = resolveUserMessage(error, requestUrl);
  if (userMessage === null) {
    return error;
  }
  return new HttpErrorResponse({
    error: buildErrorPayload(error, userMessage),
    headers: error.headers,
    status: error.status,
    statusText: error.statusText,
    url: error.url ?? requestUrl,
  });
}

function resolveUserMessage(error: HttpErrorResponse, requestUrl: string): string | null {
  const backendUnavailable = isBackendUnavailableThroughDevProxy(error, requestUrl);
  if (backendUnavailable) {
    return 'Backend Spring Boot indisponible sur http://localhost:8080. Verifie que le service est demarre et que le proxy Angular peut le joindre.';
  }
  const apiMessage = extractApiMessage(error.error);
  return apiMessage;
}

function isBackendUnavailableThroughDevProxy(error: HttpErrorResponse, requestUrl: string): boolean {
  const correlationId = error.headers?.get('X-Correlation-Id') ?? 'unknown';
  const requestTargetsApi = requestUrl.startsWith('/api/');
  const isDevHost = typeof window !== 'undefined' && window.location.port === '4200';
  const looksLikeProxyFailure = error.status === 0 || error.status === 500;
  return requestTargetsApi && isDevHost && correlationId === 'unknown' && looksLikeProxyFailure;
}

function extractApiMessage(payload: unknown): string | null {
  if (payload !== null && typeof payload === 'object' && 'message' in payload) {
    const message = payload.message;
    if (typeof message === 'string' && message.trim().length > 0) {
      return message;
    }
  }
  return null;
}

function buildErrorPayload(error: HttpErrorResponse, userMessage: string): Record<string, unknown> {
  const payload = (error.error !== null && typeof error.error === 'object') ? error.error as Record<string, unknown> : {};
  return {
    ...payload,
    message: userMessage,
  };
}
