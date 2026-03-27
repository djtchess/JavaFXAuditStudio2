export interface ActuatorMetricMeasurement {
  statistic: string;
  value: number;
}

export interface ActuatorMetricTag {
  tag: string;
  values: string[];
}

export interface ActuatorMetricResponse {
  name: string;
  baseUnit?: string;
  measurements: ActuatorMetricMeasurement[];
  availableTags: ActuatorMetricTag[];
}

export interface ActuatorHealthComponent {
  status: string;
  details?: Record<string, unknown>;
}

export interface ActuatorHealthResponse {
  status: string;
  components?: Record<string, ActuatorHealthComponent>;
}

export interface MonitoringStatusMetric {
  status: string;
  label: string;
  count: number;
}

export interface MonitoringStageMetric {
  stage: string;
  label: string;
  count: number;
  averageMs: number;
}

export interface MonitoringHealthComponent {
  name: string;
  status: string;
}

export interface MonitoringSnapshot {
  totalSessions: number;
  statusMetrics: MonitoringStatusMetric[];
  stageMetrics: MonitoringStageMetric[];
  healthStatus: string;
  healthComponents: MonitoringHealthComponent[];
  pipelineOutcomes: Record<string, number>;
  llmOutcomes: Record<string, number>;
  refreshedAt: string;
}
