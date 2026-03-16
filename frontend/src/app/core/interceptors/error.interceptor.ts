import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error) => {
      const correlationId = error.headers?.get('X-Correlation-Id') ?? 'unknown';
      console.error(
        `[HTTP Error] ${error.status} ${error.statusText} | Correlation-Id: ${correlationId} | URL: ${error.url}`
      );
      return throwError(() => error);
    })
  );
};
