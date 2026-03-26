import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AiCodeGenerationResponse,
  AiEnrichmentResponse,
  AiEnrichmentStatusResponse,
  ArtifactReviewResponse,
  LlmAuditEntryResponse,
  SanitizedSourcePreviewResponse,
} from '../models/analysis.model';

/**
 * Service d'accès aux endpoints enrichissement IA et audit LLM.
 * JAS-029
 */
@Injectable({ providedIn: 'root' })
export class AiEnrichmentApiService {
  private readonly http = inject(HttpClient);

  enrich(sessionId: string, taskType = 'NAMING'): Observable<AiEnrichmentResponse> {
    return this.http.post<AiEnrichmentResponse>(
      `/api/v1/analyses/${encodeURIComponent(sessionId)}/enrich`,
      null,
      { params: { taskType } }
    );
  }

  getAuditLog(sessionId: string): Observable<LlmAuditEntryResponse[]> {
    return this.http.get<LlmAuditEntryResponse[]>(
      `/api/v1/analysis/sessions/${encodeURIComponent(sessionId)}/llm-audit`
    );
  }

  getStatus(): Observable<AiEnrichmentStatusResponse> {
    return this.http.get<AiEnrichmentStatusResponse>('/api/v1/ai-enrichment/status');
  }

  review(sessionId: string): Observable<ArtifactReviewResponse> {
    return this.http.post<ArtifactReviewResponse>(
      `/api/v1/analyses/${encodeURIComponent(sessionId)}/review`,
      null
    );
  }

  generate(sessionId: string): Observable<AiCodeGenerationResponse> {
    return this.http.post<AiCodeGenerationResponse>(
      `/api/v1/analyses/${encodeURIComponent(sessionId)}/generate/ai`,
      null
    );
  }

  previewSanitized(sessionId: string): Observable<SanitizedSourcePreviewResponse> {
    return this.http.post<SanitizedSourcePreviewResponse>(
      `/api/v1/analyses/${encodeURIComponent(sessionId)}/preview-sanitized`,
      null
    );
  }
}
