import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { vi } from 'vitest';

import { AiEnrichmentApiService } from './ai-enrichment-api.service';
import {
  AiArtifactCoherenceResponse,
  AiCodeGenerationResponse,
  AiArtifactRefineRequest,
  AiGenerationStreamEvent,
  AiGeneratedArtifactCollectionResponse,
  AiEnrichmentResponse,
  AiEnrichmentStatusResponse,
  ArtifactReviewResponse,
  LlmAuditEntryResponse,
  SanitizedSourcePreviewResponse,
} from '../models/analysis.model';

const STATUS: AiEnrichmentStatusResponse = {
  enabled: true,
  provider: 'claude-code',
  credentialPresent: true,
  timeoutMs: 5000,
};

const AUDIT_LOG: LlmAuditEntryResponse[] = [
  {
    auditId: 'audit-1',
    sessionId: 'session-1',
    timestamp: '2026-03-26T10:00:00Z',
    provider: 'claude-code',
    taskType: 'NAMING',
    sanitizationVersion: '1.0',
    payloadHash: 'abcdef1234567890',
    promptTokensEstimate: 320,
    degraded: false,
    degradationReason: '',
  },
];

const ENRICH_RESPONSE: AiEnrichmentResponse = {
  requestId: 'req-1',
  degraded: false,
  degradationReason: '',
  suggestions: { 'Ctrl.run': 'execute' },
  tokensUsed: 120,
  provider: 'claude-code',
};

const REVIEW_RESPONSE: ArtifactReviewResponse = {
  requestId: 'req-2',
  degraded: false,
  degradationReason: '',
  migrationScore: 80,
  artifactReviews: {},
  uncertainReclassifications: {},
  globalSuggestions: [],
  provider: 'claude-code',
};

const GENERATE_RESPONSE: AiCodeGenerationResponse = {
  requestId: 'req-3',
  degraded: false,
  degradationReason: '',
  generatedClasses: {},
  tokensUsed: 150,
  provider: 'claude-code',
};

const PREVIEW_RESPONSE: SanitizedSourcePreviewResponse = {
  sessionId: 'session-1',
  controllerRef: 'Ctrl',
  sanitizedSource: 'class Ctrl {}',
  estimatedTokens: 90,
  sanitizationVersion: '1.0',
  sanitized: true,
};

const PERSISTED_ARTIFACTS: AiGeneratedArtifactCollectionResponse = {
  sessionId: 'session-1',
  artifacts: [
    {
      artifactType: 'USE_CASE',
      className: 'CtrlUseCase',
      content: 'class CtrlUseCase {}',
      versionNumber: 2,
      parentVersionId: 'artifact-1',
      requestId: 'req-persisted',
      provider: 'claude-code',
      originTask: 'SPRING_BOOT_GENERATION',
      createdAt: '2026-03-26T11:00:00Z',
      implementationStatus: 'READY',
      implementationWarning: null,
    },
  ],
};

const PERSISTED_COHERENCE: AiArtifactCoherenceResponse = {
  requestId: 'req-coherence',
  degraded: false,
  degradationReason: '',
  summary: 'Coherence globale satisfaisante.',
  artifactFindings: { USE_CASE: 'OK' },
  globalFindings: ['Aucun conflit detecte.'],
  tokensUsed: 85,
  provider: 'claude-code',
};

describe('AiEnrichmentApiService', () => {
  let service: AiEnrichmentApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AiEnrichmentApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    httpMock.verify();
  });

  it('should GET the AI status endpoint', () => {
    service.getStatus().subscribe(result => {
      expect(result).toEqual(STATUS);
    });

    const req = httpMock.expectOne('/api/v1/ai-enrichment/status');
    expect(req.request.method).toBe('GET');
    req.flush(STATUS);
  });

  it('should GET the audit log for a session', () => {
    service.getAuditLog('session-1').subscribe(result => {
      expect(result).toEqual(AUDIT_LOG);
    });

    const req = httpMock.expectOne('/api/v1/analysis/sessions/session-1/llm-audit');
    expect(req.request.method).toBe('GET');
    req.flush(AUDIT_LOG);
  });

  it('should POST the enrich endpoint with the task type query param', () => {
    service.enrich('session-1', 'NAMING', 'openai-codex-cli').subscribe(result => {
      expect(result).toEqual(ENRICH_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/enrich?taskType=NAMING&provider=openai-codex-cli');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(ENRICH_RESPONSE);
  });

  it('should POST the review endpoint', () => {
    service.review('session-1', 'claude-code-cli').subscribe(result => {
      expect(result).toEqual(REVIEW_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/review?provider=claude-code-cli');
    expect(req.request.method).toBe('POST');
    req.flush(REVIEW_RESPONSE);
  });

  it('should POST the generate endpoint', () => {
    service.generate('session-1', 'openai-gpt54').subscribe(result => {
      expect(result).toEqual(GENERATE_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/generate/ai?provider=openai-gpt54');
    expect(req.request.method).toBe('POST');
    req.flush(GENERATE_RESPONSE);
  });

  it('should stream generation progress and fall back to the existing generate endpoint', () => {
    vi.stubGlobal('EventSource', undefined);
    const events: AiGenerationStreamEvent[] = [];

    service.generateStream('session-1', 'openai-codex-cli').subscribe(event => {
      events.push(event);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/generate/ai?provider=openai-codex-cli');
    expect(req.request.method).toBe('POST');
    req.flush(GENERATE_RESPONSE);

    expect(events[0].stage).toBe('sanitizing');
    expect(events[events.length - 1].stage).toBe('complete');
    expect(events[events.length - 1].generatedClasses).toEqual(GENERATE_RESPONSE.generatedClasses);
  });

  it('should POST the refine endpoint with the refinement request', () => {
    const request: AiArtifactRefineRequest = {
      artifactType: 'MainControllerUseCase',
      instruction: 'Rends le code plus lisible',
      previousCode: 'class MainControllerUseCase {}',
    };

    service.refineArtifact('session-1', request, 'claude-code-cli').subscribe(result => {
      expect(result).toEqual(GENERATE_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/generate/ai/refine?provider=claude-code-cli');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(GENERATE_RESPONSE);
  });

  it('should POST the ZIP export endpoint for generated artifacts', () => {
    service.exportGeneratedZip('session-1').subscribe(result => {
      expect(result).toBeInstanceOf(Blob);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/generate/ai/export/zip');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['zip'], { type: 'application/zip' }));
  });

  it('should GET persisted artifacts, versions and coherence endpoints', () => {
    service.getPersistedArtifacts('session-1').subscribe(result => {
      expect(result).toEqual(PERSISTED_ARTIFACTS);
    });

    service.getPersistedArtifactVersions('session-1', 'USE_CASE').subscribe(result => {
      expect(result).toEqual(PERSISTED_ARTIFACTS);
    });

    service.verifyPersistedArtifactCoherence('session-1').subscribe(result => {
      expect(result).toEqual(PERSISTED_COHERENCE);
    });

    const artifactsReq = httpMock.expectOne('/api/v1/analyses/session-1/artifacts/ai');
    expect(artifactsReq.request.method).toBe('GET');
    artifactsReq.flush(PERSISTED_ARTIFACTS);

    const versionsReq = httpMock.expectOne('/api/v1/analyses/session-1/artifacts/ai/USE_CASE/versions');
    expect(versionsReq.request.method).toBe('GET');
    versionsReq.flush(PERSISTED_ARTIFACTS);

    const coherenceReq = httpMock.expectOne('/api/v1/analyses/session-1/artifacts/ai/coherence');
    expect(coherenceReq.request.method).toBe('POST');
    expect(coherenceReq.request.body).toBeNull();
    coherenceReq.flush(PERSISTED_COHERENCE);
  });

  it('should POST the preview endpoint', () => {
    service.previewSanitized('session-1').subscribe(result => {
      expect(result).toEqual(PREVIEW_RESPONSE);
    });

    const req = httpMock.expectOne('/api/v1/analyses/session-1/preview-sanitized');
    expect(req.request.method).toBe('POST');
    req.flush(PREVIEW_RESPONSE);
  });
});
