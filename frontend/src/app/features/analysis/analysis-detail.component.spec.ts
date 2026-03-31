import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { convertToParamMap, ActivatedRoute } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { AnalysisDetailComponent } from './analysis-detail.component';
import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { AiEnrichmentApiService } from '../../core/services/ai-enrichment-api.service';
import {
  AnalysisSessionResponse,
  ArtifactsResponse,
  CartographyResponse,
  ClassificationResponse,
  MigrationPlanResponse,
  OrchestratedAnalysisResultResponse,
  RestitutionReportResponse,
} from '../../core/models/analysis.model';

const CARTOGRAPHY: CartographyResponse = {
  controllerRef: 'Ctrl',
  fxmlRef: 'src/main/resources/view/Main.fxml',
  hasUnknowns: false,
  components: [
    { fxId: 'rootPane', componentType: 'AnchorPane', eventHandler: '' },
  ],
  handlers: [
    { methodName: 'onSave', fxmlRef: '#saveButton', injectedType: 'ActionEvent' },
  ],
};

const CLASSIFICATION: ClassificationResponse = {
  controllerRef: 'Ctrl',
  ruleCount: 1,
  uncertainCount: 0,
  excludedLifecycleMethodsCount: 0,
  parsingMode: 'AST',
  rules: [
    {
      ruleId: 'R-1',
      description: 'Validate input',
      responsibilityClass: 'BUSINESS',
      extractionCandidate: 'USE_CASE',
      uncertain: false,
    },
  ],
};

const MIGRATION_PLAN: MigrationPlanResponse = {
  controllerRef: 'Ctrl',
  compilable: true,
  lots: [
    {
      lotNumber: 1,
      title: 'Lot 1 - UI',
      objective: 'Extract UI flow',
      extractionCandidates: ['MainController'],
    },
  ],
};

const ARTIFACTS: ArtifactsResponse = {
  controllerRef: 'Ctrl',
  warnings: [],
  artifacts: [
    {
      artifactId: 'art-1',
      type: 'USE_CASE',
      lotNumber: 1,
      className: 'MainControllerUseCase',
      content: 'class MainControllerUseCase {}',
      transitionalBridge: false,
      generationWarnings: [],
      generationStatus: 'OK',
    },
  ],
};

const REPORT: RestitutionReportResponse = {
  controllerRef: 'Ctrl',
  ruleCount: 1,
  artifactCount: 1,
  confidence: 'HIGH',
  isActionable: true,
  findings: ['Move logic out of controller'],
  unknowns: [],
  markdown: '# Restitution\n\n## Synthese',
};

const SESSION: AnalysisSessionResponse = {
  sessionId: 'session-42',
  status: 'CREATED',
  sessionName: 'Audit Ctrl',
  controllerRef: 'Ctrl',
  sourceSnippetRef: 'Ctrl.fxml',
  createdAt: '2026-03-30T10:15:00Z',
};

const PIPELINE_RESULT: OrchestratedAnalysisResultResponse = {
  sessionId: 'session-42',
  finalStatus: 'COMPLETED',
  cartography: CARTOGRAPHY,
  classification: CLASSIFICATION,
  migrationPlan: MIGRATION_PLAN,
  generationResult: ARTIFACTS,
  restitutionReport: REPORT,
  errors: [],
};

type AnalysisApiSpy = {
  getSession: ReturnType<typeof vi.fn>;
  getCartography: ReturnType<typeof vi.fn>;
  getClassification: ReturnType<typeof vi.fn>;
  getMigrationPlan: ReturnType<typeof vi.fn>;
  getArtifacts: ReturnType<typeof vi.fn>;
  getReport: ReturnType<typeof vi.fn>;
  runFullPipeline: ReturnType<typeof vi.fn>;
  exportArtifacts: ReturnType<typeof vi.fn>;
};

type AiApiSpy = {
  getStatus: ReturnType<typeof vi.fn>;
  getAuditLog: ReturnType<typeof vi.fn>;
  enrich: ReturnType<typeof vi.fn>;
  review: ReturnType<typeof vi.fn>;
  generate: ReturnType<typeof vi.fn>;
  previewSanitized: ReturnType<typeof vi.fn>;
};

function buildAnalysisApiSpy(): AnalysisApiSpy {
  return {
    getSession: vi.fn().mockReturnValue(of(SESSION)),
    getCartography: vi.fn().mockReturnValue(of(CARTOGRAPHY)),
    getClassification: vi.fn().mockReturnValue(of(CLASSIFICATION)),
    getMigrationPlan: vi.fn().mockReturnValue(of(MIGRATION_PLAN)),
    getArtifacts: vi.fn().mockReturnValue(of(ARTIFACTS)),
    getReport: vi.fn().mockReturnValue(of(REPORT)),
    runFullPipeline: vi.fn(),
    exportArtifacts: vi.fn().mockReturnValue(of({ targetDirectory: '/tmp', exportedFiles: [], errors: [] })),
  };
}

function buildAiApiSpy(): AiApiSpy {
  return {
    getStatus: vi.fn().mockReturnValue(of({
      enabled: false,
      provider: 'none',
      credentialPresent: false,
      timeoutMs: 5000,
    })),
    getAuditLog: vi.fn().mockReturnValue(of([])),
    enrich: vi.fn().mockReturnValue(of({
      requestId: 'req-1',
      degraded: false,
      degradationReason: '',
      suggestions: {},
      tokensUsed: 0,
      provider: 'none',
    })),
    review: vi.fn().mockReturnValue(of({
      requestId: 'req-2',
      degraded: false,
      degradationReason: '',
      migrationScore: 80,
      artifactReviews: {},
      uncertainReclassifications: {},
      globalSuggestions: [],
      provider: 'none',
    })),
    generate: vi.fn().mockReturnValue(of({
      requestId: 'req-3',
      degraded: false,
      degradationReason: '',
      generatedClasses: {},
      tokensUsed: 0,
      provider: 'none',
    })),
    previewSanitized: vi.fn().mockReturnValue(of({
      sessionId: 'session-42',
      controllerRef: 'Ctrl',
      sanitizedSource: 'class Ctrl {}',
      estimatedTokens: 0,
      sanitizationVersion: '1.0',
      sanitized: true,
    })),
  };
}

describe('AnalysisDetailComponent', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should show the cartography error state when the step call fails', async () => {
    const analysisApiSpy = buildAnalysisApiSpy();
    analysisApiSpy.getCartography.mockReturnValue(
      throwError(() => ({ error: { message: 'Cartography failed' } })),
    );
    const aiApiSpy = buildAiApiSpy();

    await TestBed.configureTestingModule({
      imports: [AnalysisDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ sessionId: 'session-42' }) },
          },
        },
        { provide: AnalysisApiService, useValue: analysisApiSpy },
        { provide: AiEnrichmentApiService, useValue: aiApiSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AnalysisDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Audit Ctrl');

    const runButtons = fixture.debugElement.queryAll(By.css('.run-btn'));
    runButtons[0].nativeElement.click();
    fixture.detectChanges();

    const errorBlock = fixture.debugElement.query(By.css('.status-error'));
    expect(errorBlock.nativeElement.textContent).toContain('Cartography failed');
  });

  it('should render all pipeline sections after a successful full run', async () => {
    const analysisApiSpy = buildAnalysisApiSpy();
    const aiApiSpy = buildAiApiSpy();
    const pipelineResult$ = new Subject<OrchestratedAnalysisResultResponse>();
    analysisApiSpy.runFullPipeline.mockReturnValue(pipelineResult$.asObservable());

    await TestBed.configureTestingModule({
      imports: [AnalysisDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ sessionId: 'session-42' }) },
          },
        },
        { provide: AnalysisApiService, useValue: analysisApiSpy },
        { provide: AiEnrichmentApiService, useValue: aiApiSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AnalysisDetailComponent);
    fixture.detectChanges();

    expect(analysisApiSpy.getSession).toHaveBeenCalledWith('session-42');

    const pipelineButton = fixture.debugElement.query(By.css('.pipeline-btn')).nativeElement as HTMLButtonElement;
    pipelineButton.click();
    fixture.detectChanges();

    expect(analysisApiSpy.runFullPipeline).toHaveBeenCalledWith('session-42');
    expect(pipelineButton.textContent).toContain('Pipeline en cours');
    expect(fixture.debugElement.query(By.css('.pipeline-progress'))).toBeTruthy();

    pipelineResult$.next(PIPELINE_RESULT);
    pipelineResult$.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Statut COMPLETED');
    expect(fixture.nativeElement.textContent).toContain('Audit Ctrl');
    expect(fixture.nativeElement.textContent).toContain('Ctrl.fxml');
    expect(pipelineButton.textContent).toContain('Executer le pipeline complet');
    expect(fixture.debugElement.query(By.css('jas-cartography-view'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('jas-migration-plan-view'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('jas-artifacts-view'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('jas-report-view'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.pipeline-error'))).toBeNull();
  });

  it('should render session metadata and show the fallback pipeline error message', async () => {
    const analysisApiSpy = buildAnalysisApiSpy();
    const aiApiSpy = buildAiApiSpy();
    analysisApiSpy.runFullPipeline.mockReturnValue(throwError(() => ({})));

    await TestBed.configureTestingModule({
      imports: [AnalysisDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ sessionId: 'session-42' }) },
          },
        },
        { provide: AnalysisApiService, useValue: analysisApiSpy },
        { provide: AiEnrichmentApiService, useValue: aiApiSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AnalysisDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Audit Ctrl');
    expect(fixture.nativeElement.textContent).toContain('Ctrl.fxml');
    expect(fixture.nativeElement.textContent).toContain('Statut CREATED');

    const pipelineButton = fixture.debugElement.query(By.css('.pipeline-btn')).nativeElement as HTMLButtonElement;
    pipelineButton.click();
    fixture.detectChanges();

    const errorLine = fixture.debugElement.query(By.css('.pipeline-error'));
    expect(errorLine.nativeElement.textContent).toContain('Erreur lors de l\'execution du pipeline complet.');
    expect(pipelineButton.textContent).toContain('Executer le pipeline complet');
  });
});
