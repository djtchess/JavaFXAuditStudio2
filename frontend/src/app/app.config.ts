import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { correlationIdInterceptor } from './core/interceptors/correlation-id.interceptor';
import { frontendMonitoringInterceptor } from './core/interceptors/frontend-monitoring.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([correlationIdInterceptor, frontendMonitoringInterceptor, errorInterceptor])),
    provideRouter(routes)
  ]
};
