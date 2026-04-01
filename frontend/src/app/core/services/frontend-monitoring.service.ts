import { Injectable, computed, signal } from '@angular/core';

export interface FrontendRequestEvent {
  method: string;
  url: string;
  status: number;
  durationMs: number;
  correlationId: string;
  succeeded: boolean;
  message: string | null;
  completedAt: string;
}

export interface FrontendMonitoringSummary {
  inflightRequests: number;
  totalCompletedRequests: number;
  failedRequests: number;
  successRate: number;
  averageDurationMs: number;
  lastCompletedAt: string | null;
}

const MAX_RECENT_EVENTS = 20;

@Injectable({ providedIn: 'root' })
export class FrontendMonitoringService {
  private readonly inflightRequestsSignal = signal(0);
  private readonly recentEventsSignal = signal<FrontendRequestEvent[]>([]);
  private readonly totalCompletedRequestsSignal = signal(0);
  private readonly failedRequestsSignal = signal(0);
  private readonly totalDurationMsSignal = signal(0);
  private readonly lastCompletedAtSignal = signal<string | null>(null);

  readonly recentRequests = computed(() => this.recentEventsSignal());

  readonly summary = computed<FrontendMonitoringSummary>(() => {
    const totalCompletedRequests = this.totalCompletedRequestsSignal();
    const failedRequests = this.failedRequestsSignal();
    const totalDurationMs = this.totalDurationMsSignal();

    return {
      inflightRequests: this.inflightRequestsSignal(),
      totalCompletedRequests,
      failedRequests,
      successRate: totalCompletedRequests > 0
        ? ((totalCompletedRequests - failedRequests) / totalCompletedRequests) * 100
        : 100,
      averageDurationMs: totalCompletedRequests > 0 ? totalDurationMs / totalCompletedRequests : 0,
      lastCompletedAt: this.lastCompletedAtSignal(),
    };
  });

  readonly recentFailures = computed(() =>
    this.recentEventsSignal().filter(event => !event.succeeded)
  );

  requestStarted(): void {
    this.inflightRequestsSignal.update(value => value + 1);
  }

  requestFinished(): void {
    this.inflightRequestsSignal.update(value => (value > 0 ? value - 1 : 0));
  }

  recordCompletedRequest(event: FrontendRequestEvent): void {
    this.totalCompletedRequestsSignal.update(value => value + 1);
    this.totalDurationMsSignal.update(value => value + event.durationMs);
    this.lastCompletedAtSignal.set(event.completedAt);
    if (!event.succeeded) {
      this.failedRequestsSignal.update(value => value + 1);
    }

    this.recentEventsSignal.update(events => [event, ...events].slice(0, MAX_RECENT_EVENTS));
  }
}
