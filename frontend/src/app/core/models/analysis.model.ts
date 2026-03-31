// --- Request ---

export interface SubmitAnalysisRequest {
  sourceFilePaths: string[];
  sessionName: string;
}

// --- Responses ---

export interface AnalysisSessionResponse {
  sessionId: string;
  status: string;
  sessionName: string;
  controllerRef: string;
  sourceSnippetRef: string;
  createdAt: string;
}

// --- Cartography ---

export interface FxmlComponentDto {
  fxId: string;
  componentType: string;
  eventHandler: string;
}

export interface HandlerBindingDto {
  methodName: string;
  fxmlRef: string;
  injectedType: string;
}

export interface CartographyResponse {
  controllerRef: string;
  fxmlRef: string;
  components: FxmlComponentDto[];
  handlers: HandlerBindingDto[];
  hasUnknowns: boolean;
}

// --- Classification ---

export interface MethodParameterDto {
  type: string;
  name: string;
  unknown: boolean;
}

export interface MethodSignatureDto {
  returnType: string;
  parameters: MethodParameterDto[];
  hasUnknowns: boolean;
}

export interface BusinessRuleDto {
  ruleId: string;
  description: string;
  responsibilityClass: string;
  extractionCandidate: string;
  uncertain: boolean;
  signature?: MethodSignatureDto;  // null si mode REGEX_FALLBACK
}

export interface ClassificationResponse {
  controllerRef: string;
  ruleCount: number;
  uncertainCount: number;
  excludedLifecycleMethodsCount: number;
  rules: BusinessRuleDto[];
  parsingMode: 'AST' | 'REGEX_FALLBACK';
  parsingFallbackReason?: string;
}

// --- Migration Plan ---

export interface PlannedLotDto {
  lotNumber: number;
  title: string;
  objective: string;
  extractionCandidates: string[];
}

export interface MigrationPlanResponse {
  controllerRef: string;
  compilable: boolean;
  lots: PlannedLotDto[];
}

// --- Artifacts ---

export interface CodeArtifactDto {
  artifactId: string;
  type: string;
  lotNumber: number;
  className: string;
  content: string;
  transitionalBridge: boolean;
  generationWarnings: string[];
  generationStatus: string;
}

export interface ArtifactsResponse {
  controllerRef: string;
  artifacts: CodeArtifactDto[];
  warnings: string[];
}

// --- Export ---

export interface ExportArtifactsRequest {
  targetDirectory: string;
}

export interface ExportArtifactsResponse {
  targetDirectory: string;
  exportedFiles: string[];
  errors: string[];
}

// --- Restitution Report ---

export interface RestitutionReportResponse {
  controllerRef: string;
  ruleCount: number;
  artifactCount: number;
  confidence: string;
  isActionable: boolean;
  findings: string[];
  unknowns: string[];
  markdown: string;
}

// --- Orchestrated (full pipeline) ---

export interface OrchestratedAnalysisResultResponse {
  sessionId: string;
  finalStatus: string;
  cartography: CartographyResponse | null;
  classification: ClassificationResponse | null;
  migrationPlan: MigrationPlanResponse | null;
  generationResult: ArtifactsResponse | null;
  restitutionReport: RestitutionReportResponse | null;
  errors: string[];
}

// --- Error ---

export interface ErrorResponse {
  status: number;
  error: string;
  correlationId: string;
}

// --- Monitoring / Actuator ---

export interface ActuatorHealthComponent {
  status: string;
  details?: Record<string, unknown>;
}

export interface ActuatorHealthResponse {
  status: string;
  components?: Record<string, ActuatorHealthComponent>;
}

export interface ActuatorAiHealthResponse {
  status: string;
  enabled: boolean;
  provider: string;
  circuitBreakerState: string;
  totalRequests: number;
  successRate: number;
  p95LatencyMs: number;
  totalTokens: number;
  outcomes: Record<string, number>;
}

export interface ActuatorMetricMeasurement {
  statistic: string;
  value: number;
}

export interface ActuatorMetricAvailableTag {
  tag: string;
  values: string[];
}

export interface ActuatorMetricResponse {
  name: string;
  baseUnit?: string;
  measurements: ActuatorMetricMeasurement[];
  availableTags?: ActuatorMetricAvailableTag[];
}

export interface MonitoringMetricValue {
  key: string;
  label: string;
  value: number;
}

export interface MonitoringStageDuration {
  stage: string;
  averageMs: number;
  sampleCount: number;
}

export interface MonitoringDashboardResponse {
  health: ActuatorHealthResponse;
  aiHealth: ActuatorAiHealthResponse;
  totalSessions: number;
  sessionsByStatus: MonitoringMetricValue[];
  stageDurations: MonitoringStageDuration[];
  pipelineOutcomes: MonitoringMetricValue[];
}

// --- Project Dashboard ---

export interface ProjectDashboardResponse {
  projectId: string;
  totalSessions: number;
  analysingCount: number;
  completedCount: number;
  rulesByCategory: Record<string, number>;
  uncertainCount: number;
  reclassifiedCount: number;
  recommendedLotOrder: string[];
}

// --- Reclassification ---

export interface ReclassifyRuleRequest {
  category: string;
  reason: string;
}

export interface ReclassifiedRuleResponse {
  ruleId: string;
  description: string;
  responsibilityClass: string;
  extractionCandidate: string;
  uncertain: boolean;
  manuallyReclassified: boolean;
}

export interface ReclassificationAuditEntryResponse {
  ruleId: string;
  fromCategory: string;
  toCategory: string;
  reason: string;
  timestamp: string;
}

// --- LLM Audit (JAS-029) ---

export interface LlmAuditEntryResponse {
  auditId: string;
  sessionId: string;
  timestamp: string;          // ISO-8601
  provider: string;           // "claude-code" | "openai-gpt54" | "none"
  taskType: string;
  sanitizationVersion: string;
  payloadHash: string;        // SHA-256, jamais la source
  promptTokensEstimate: number;
  degraded: boolean;
  degradationReason: string;
}

export interface AiEnrichmentStatusResponse {
  enabled: boolean;
  provider: string;
  credentialPresent: boolean;
  timeoutMs: number;
}

export interface AiEnrichmentResponse {
  requestId: string;
  degraded: boolean;
  degradationReason: string;
  suggestions: Record<string, string>;
  tokensUsed: number;
  provider: string;
}

/**
 * Evenement de progression pour la generation IA en SSE.
 */
export interface AiGenerationStreamEvent {
  stage: 'sanitizing' | 'sending_to_llm' | 'streaming' | 'parsing_response' | 'validating' | 'complete' | 'error';
  message: string;
  progress: number;
  artifactKey?: string;
  chunk?: string;
  generatedClasses?: Record<string, string>;
  tokensUsed?: number;
  provider?: string;
  degraded?: boolean;
  error?: string;
}

/**
 * Requete de raffinement multi-tour d'un artefact IA.
 */
export interface AiArtifactRefineRequest {
  artifactType: string;
  instruction: string;
  previousCode: string;
}

/**
 * Reponse minimale pour un export d'artefacts generes.
 */
export interface AiArtifactExportResponse {
  targetDirectory?: string;
  exportedFiles?: string[];
  errors?: string[];
  fileName?: string;
}

export interface AiGeneratedArtifactResponse {
  artifactType: string;
  className: string;
  content: string;
  versionNumber: number;
  parentVersionId?: string | null;
  requestId: string;
  provider: string;
  originTask: string;
  createdAt: string;
  implementationStatus: 'READY' | 'INCOMPLETE';
  implementationWarning?: string | null;
}

export interface AiGeneratedArtifactCollectionResponse {
  sessionId: string;
  artifacts: AiGeneratedArtifactResponse[];
}

export interface AiArtifactCoherenceResponse {
  requestId: string;
  degraded: boolean;
  degradationReason: string;
  summary: string;
  artifactFindings: Record<string, string>;
  globalFindings: string[];
  tokensUsed: number;
  provider: string;
}

/**
 * Resultat de la revue IA des artefacts generes (JAS-030).
 */
export interface ArtifactReviewResponse {
  requestId: string;
  degraded: boolean;
  degradationReason: string;
  migrationScore: number;
  artifactReviews: Record<string, string>;
  uncertainReclassifications: Record<string, string>;
  globalSuggestions: string[];
  provider: string;
}

/**
 * Résultat de la génération IA des classes Spring Boot cibles (JAS-031).
 */
export interface AiCodeGenerationResponse {
  requestId: string;
  degraded: boolean;
  degradationReason: string;
  generatedClasses: Record<string, string>;
  tokensUsed: number;
  provider: string;
}

/**
 * Résultat de la prévisualisation du code sanitisé avant envoi au LLM (JAS-031).
 */
export interface SanitizedSourcePreviewResponse {
  sessionId: string;
  controllerRef: string;
  sanitizedSource: string;
  estimatedTokens: number;
  sanitizationVersion: string;
  sanitized: boolean;
}
