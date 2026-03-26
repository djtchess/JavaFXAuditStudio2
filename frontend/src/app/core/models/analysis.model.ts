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
