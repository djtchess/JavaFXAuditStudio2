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

export interface BusinessRuleDto {
  ruleId: string;
  description: string;
  responsibilityClass: string;
  extractionCandidate: string;
  uncertain: boolean;
}

export interface ClassificationResponse {
  controllerRef: string;
  ruleCount: number;
  uncertainCount: number;
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
