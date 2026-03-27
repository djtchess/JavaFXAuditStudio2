import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';

import {
  ActuatorHealthResponse,
  ActuatorMetricResponse,
  MonitoringDashboardResponse,
  MonitoringMetricValue,
  MonitoringStageDuration,
} from '../models/analysis.model';
import { MonitoringSnapshot } from '../models/monitoring.model';

const STATUS_LABELS: Record<string, string> = {
  total: 'Total',
  created: 'Creees',
  in_progress: 'En cours',
  running: 'En execution',
  ingesting: 'Ingestion',
  cartographing: 'Cartographie',
  classifying: 'Classification',
  planning: 'Planification',
  generating: 'Generation',
  reporting: 'Restitution',
  completed: 'Terminees',
  failed: 'En echec',
  locked: 'Verrouillees',
};

const STAGE_LABELS: Record<string, string> = {
  ingest: 'Ingestion',
  cartography: 'Cartographie',
  classification: 'Classification',
  planning: 'Planification',
  generation: 'Generation',
  reporting: 'Restitution',
};

const STATUS_ORDER = [
  'created',
  'in_progress',
  'running',
  'ingesting',
  'cartographing',
  'classifying',
  'planning',
  'generating',
  'reporting',
  'completed',
  'failed',
  'locked',
];

const STAGE_ORDER = ['ingest', 'cartography', 'classification', 'planning', 'generation', 'reporting'];

@Injectable({ providedIn: 'root' })
export class MetricsApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/actuator';

  loadDashboard(): Observable<MonitoringDashboardResponse> {
    return forkJoin({
      health: this.http.get<ActuatorHealthResponse>(`${this.base}/health`),
      sessionsMetric: this.getMetric('jas.analysis.sessions'),
      stageMetric: this.getMetric('jas.analysis.pipeline.stage.duration'),
    }).pipe(
      switchMap(({ health, sessionsMetric, stageMetric }) =>
        forkJoin({
          health: of(health),
          totalSessions: this.loadTotalSessions(sessionsMetric),
          sessionsByStatus: this.loadTaggedMetricValues(
            'jas.analysis.sessions',
            sessionsMetric,
            'status',
            'total'
          ),
          stageDurations: this.loadStageDurations(stageMetric),
        })
      )
    );
  }

  loadMonitoringSnapshot(): Observable<MonitoringSnapshot> {
    return this.loadDashboard().pipe(
      map(dashboard => ({
        totalSessions: dashboard.totalSessions,
        statusMetrics: dashboard.sessionsByStatus.map(metric => ({
          status: metric.key,
          label: metric.label,
          count: metric.value,
        })),
        stageMetrics: dashboard.stageDurations.map(metric => ({
          stage: metric.stage,
          label: STAGE_LABELS[metric.stage] ?? this.formatLabel(metric.stage),
          count: metric.sampleCount,
          averageMs: metric.averageMs,
        })),
        healthStatus: dashboard.health.status,
        healthComponents: Object.entries(dashboard.health.components ?? {}).map(([name, component]) => ({
          name,
          status: component.status,
        })),
        pipelineOutcomes: {},
        llmOutcomes: {},
        refreshedAt: new Date().toISOString(),
      }))
    );
  }

  private loadTotalSessions(metric: ActuatorMetricResponse): Observable<number> {
    const statuses = this.availableTagValues(metric, 'status');
    if (statuses.includes('total')) {
      return this.getMetric('jas.analysis.sessions', { status: 'total' }).pipe(
        map(response => this.readMeasurementValue(response))
      );
    }
    return of(this.readMeasurementValue(metric));
  }

  private loadTaggedMetricValues(
    metricName: string,
    metric: ActuatorMetricResponse,
    tagName: string,
    excludedValue?: string
  ): Observable<MonitoringMetricValue[]> {
    const tagValues = this.availableTagValues(metric, tagName)
      .filter(value => value !== excludedValue)
      .sort((left, right) => this.compareByKnownOrder(left, right, STATUS_ORDER));
    if (tagValues.length === 0) {
      return of([]);
    }
    return forkJoin(
      tagValues.map(value =>
        this.getMetric(metricName, { [tagName]: value }).pipe(
          map(response => ({
            key: value,
            label: STATUS_LABELS[value] ?? this.formatLabel(value),
            value: this.readMeasurementValue(response),
          }))
        )
      )
    );
  }

  private loadStageDurations(metric: ActuatorMetricResponse): Observable<MonitoringStageDuration[]> {
    const stages = this.availableTagValues(metric, 'stage')
      .sort((left, right) => this.compareByKnownOrder(left, right, STAGE_ORDER));
    if (stages.length === 0) {
      return of([]);
    }
    return forkJoin(
      stages.map(stage =>
        this.getMetric('jas.analysis.pipeline.stage.duration', { stage }).pipe(
          map(response => this.toStageDuration(stage, response))
        )
      )
    );
  }

  private toStageDuration(stage: string, metric: ActuatorMetricResponse): MonitoringStageDuration {
    const totalTime = this.findMeasurement(metric, 'TOTAL_TIME');
    const count = this.findMeasurement(metric, 'COUNT');
    const totalTimeMs = metric.baseUnit === 'seconds' ? totalTime * 1000 : totalTime;
    const averageMs = count > 0 ? totalTimeMs / count : 0;
    return {
      stage,
      averageMs,
      sampleCount: count,
    };
  }

  private getMetric(
    metricName: string,
    tags: Record<string, string> = {}
  ): Observable<ActuatorMetricResponse> {
    let params = new HttpParams();
    Object.entries(tags).forEach(([key, value]) => {
      params = params.append('tag', `${key}:${value}`);
    });
    return this.http.get<ActuatorMetricResponse>(`${this.base}/metrics/${metricName}`, { params });
  }

  private availableTagValues(metric: ActuatorMetricResponse, tagName: string): string[] {
    const tag = metric.availableTags?.find(candidate => candidate.tag === tagName);
    return tag?.values ?? [];
  }

  private readMeasurementValue(metric: ActuatorMetricResponse): number {
    const count = this.findMeasurement(metric, 'VALUE');
    return count;
  }

  private findMeasurement(metric: ActuatorMetricResponse, statistic: string): number {
    const measurement = metric.measurements.find(candidate => candidate.statistic === statistic);
    return measurement?.value ?? 0;
  }

  private formatLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private compareByKnownOrder(left: string, right: string, order: string[]): number {
    const leftIndex = order.indexOf(left);
    const rightIndex = order.indexOf(right);
    if (leftIndex >= 0 && rightIndex >= 0) {
      return leftIndex - rightIndex;
    }
    if (leftIndex >= 0) {
      return -1;
    }
    if (rightIndex >= 0) {
      return 1;
    }
    return left.localeCompare(right);
  }
}
