import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { AnalysisApiService } from './analysis-api.service';
import {
  AnalysisSessionResponse,
  ArtifactsResponse,
  CartographyResponse,
  ExportArtifactsResponse,
  MigrationPlanResponse,
  OrchestratedAnalysisResultResponse,
  RestitutionReportResponse,
  SubmitAnalysisRequest,
} from '../models/analysis.model';

const SUBMIT_REQUEST: SubmitAnalysisRequest = {
  sessionName: 'Audit MainController',
  sourceFilePaths: ['src/main/java/com/app/MainController.java'],
};

const SESSION_RESPONSE: AnalysisSessionResponse = {
  sessionId: 'session-1',
  status: 'CREATED',
  sessionName: 'Audit MainController',
  controllerRef: 'src/main/java/com/app/MainController.java',
  sourceSnippetRef: 'src/main/resources/view/Main.fxml',
  createdAt: '2026-03-30T10:15:00Z',
};

const CARTOGRAPHY: CartographyResponse = {
  controllerRef: 'Ctrl',
  fxmlRef: 'Ctrl.fxml',
  components: [],
  handlers: [],
  hasUnknowns: false,
};

const MIGRATION_PLAN: MigrationPlanResponse = {
  controllerRef: 'Ctrl',
  compilable: true,
  lots: [],
};

const ARTIFACTS: ArtifactsResponse = {
  controllerRef: 'Ctrl',
  artifacts: [],
  warnings: [],
};

const REPORT: RestitutionReportResponse = {
  controllerRef: 'Ctrl',
  ruleCount: 0,
  artifactCount: 0,
  confidence: 'LOW',
  isActionable: false,
  findings: [],
  unknowns: [],
  markdown: '',
};

const PIPELINE_RESULT: OrchestratedAnalysisResultResponse = {
  sessionId: 'session-1',
  finalStatus: 'COMPLETED',
  cartography: CARTOGRAPHY,
  classification: {
    controllerRef: 'Ctrl',
    ruleCount: 0,
    uncertainCount: 0,
    excludedLifecycleMethodsCount: 0,
    rules: [],
    parsingMode: 'AST',
  },
  migrationPlan: MIGRATION_PLAN,
  generationResult: ARTIFACTS,
  restitutionReport: REPORT,
  errors: [],
};

const EXPORT_RESULT: ExportArtifactsResponse = {
  targetDirectory: 'build/generated',
  exportedFiles: ['MainControllerUseCase.java'],
  errors: [],
};

describe('AnalysisApiService', () => {
  let service: AnalysisApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AnalysisApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should POST a session submission request', () => {
    service.submitSession(SUBMIT_REQUEST).subscribe(result => {
      expect(result).toEqual(SESSION_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(SUBMIT_REQUEST);
    req.flush(SESSION_RESPONSE);
  });

  it('should GET session metadata for a session', () => {
    service.getSession('session-1').subscribe(result => {
      expect(result).toEqual(SESSION_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1');
    expect(req.request.method).toBe('GET');
    req.flush(SESSION_RESPONSE);
  });

  it('should GET cartography for a session', () => {
    service.getCartography('session-1').subscribe(result => {
      expect(result).toEqual(CARTOGRAPHY);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1/cartography');
    expect(req.request.method).toBe('GET');
    req.flush(CARTOGRAPHY);
  });

  it('should GET classification for a session', () => {
    service.getClassification('session-1').subscribe(result => {
      expect(result).toEqual(PIPELINE_RESULT.classification);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1/classification');
    expect(req.request.method).toBe('GET');
    req.flush(PIPELINE_RESULT.classification);
  });

  it('should GET migration plan and report for a session', () => {
    service.getMigrationPlan('session-1').subscribe(result => {
      expect(result).toEqual(MIGRATION_PLAN);
    });
    service.getReport('session-1').subscribe(result => {
      expect(result).toEqual(REPORT);
    });

    const planReq = httpMock.expectOne('/api/v1/analysis/sessions/session-1/plan');
    expect(planReq.request.method).toBe('GET');
    planReq.flush(MIGRATION_PLAN);

    const reportReq = httpMock.expectOne('/api/v1/analysis/sessions/session-1/report');
    expect(reportReq.request.method).toBe('GET');
    reportReq.flush(REPORT);
  });

  it('should GET artifacts for a session', () => {
    service.getArtifacts('session-1').subscribe(result => {
      expect(result).toEqual(ARTIFACTS);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1/artifacts');
    expect(req.request.method).toBe('GET');
    req.flush(ARTIFACTS);
  });

  it('should POST the full pipeline endpoint with a null body', () => {
    service.runFullPipeline('session-1').subscribe(result => {
      expect(result).toEqual(PIPELINE_RESULT);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1/run');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(PIPELINE_RESULT);
  });

  it('should POST exportArtifacts with the requested directory', () => {
    service.exportArtifacts('session-1', 'build/generated').subscribe(result => {
      expect(result).toEqual(EXPORT_RESULT);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1/artifacts/export');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ targetDirectory: 'build/generated' });
    req.flush(EXPORT_RESULT);
  });
});
