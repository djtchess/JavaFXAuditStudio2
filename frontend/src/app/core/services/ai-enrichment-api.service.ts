import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

import {
  AiArtifactCoherenceResponse,
  AiCodeGenerationResponse,
  AiGeneratedArtifactCollectionResponse,
  AiArtifactRefineRequest,
  AiGenerationStreamEvent,
  AiEnrichmentResponse,
  AiEnrichmentProvider,
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
  private readonly sessionBase = '/api/v1/analysis/sessions';

  enrich(
    sessionId: string,
    taskType = 'NAMING',
    provider?: AiEnrichmentProvider,
  ): Observable<AiEnrichmentResponse> {
    const params: Record<string, string> = { taskType };
    const providerParams = this.buildProviderQueryParams(provider);
    if (providerParams) {
      Object.assign(params, providerParams);
    }
    return this.http.post<AiEnrichmentResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/enrich`,
      null,
      { params }
    );
  }

  getAuditLog(sessionId: string): Observable<LlmAuditEntryResponse[]> {
    return this.http.get<LlmAuditEntryResponse[]>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/llm-audit`
    );
  }

  getStatus(): Observable<AiEnrichmentStatusResponse> {
    return this.http.get<AiEnrichmentStatusResponse>('/api/v1/ai-enrichment/status');
  }

  review(sessionId: string, provider?: AiEnrichmentProvider): Observable<ArtifactReviewResponse> {
    return this.http.post<ArtifactReviewResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/review`,
      null,
      { params: this.buildProviderQueryParams(provider) }
    );
  }

  generate(sessionId: string, provider?: AiEnrichmentProvider): Observable<AiCodeGenerationResponse> {
    return this.http.post<AiCodeGenerationResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/generate/ai`,
      null,
      { params: this.buildProviderQueryParams(provider) }
    );
  }

  generateStream(
    sessionId: string,
    provider?: AiEnrichmentProvider,
  ): Observable<AiGenerationStreamEvent> {
    return new Observable<AiGenerationStreamEvent>(subscriber => {
      if (typeof EventSource === 'undefined') {
        const fallbackSubscription = this.syntheticGenerateStream(sessionId, provider).subscribe(subscriber);
        return () => fallbackSubscription.unsubscribe();
      }

      const streamUrl = this.buildStreamUrl(sessionId, provider);
      const eventSource = new EventSource(streamUrl);
      let receivedMessage = false;
      let fallbackSubscription: Subscription | null = null;

      eventSource.onmessage = event => {
        receivedMessage = true;
        const payload = this.parseGenerationStreamEvent(event.data);
        subscriber.next(payload);

        if (payload.stage === 'complete' || payload.stage === 'error') {
          eventSource.close();
          subscriber.complete();
        }
      };

      eventSource.onerror = () => {
        eventSource.close();

        if (receivedMessage) {
          subscriber.error(new Error('Erreur pendant le flux SSE de generation IA.'));
          return;
        }

        fallbackSubscription = this.syntheticGenerateStream(sessionId, provider).subscribe({
          next: value => subscriber.next(value),
          error: err => subscriber.error(err),
          complete: () => subscriber.complete(),
        });
      };

      return () => {
        eventSource.close();
        fallbackSubscription?.unsubscribe();
      };
    });
  }

  refineArtifact(
    sessionId: string,
    request: AiArtifactRefineRequest,
    provider?: AiEnrichmentProvider,
  ): Observable<AiCodeGenerationResponse> {
    return this.http.post<AiCodeGenerationResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/generate/ai/refine`,
      request,
      { params: this.buildProviderQueryParams(provider) }
    );
  }

  getPersistedArtifacts(sessionId: string): Observable<AiGeneratedArtifactCollectionResponse> {
    return this.http.get<AiGeneratedArtifactCollectionResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/artifacts/ai`,
    );
  }

  getPersistedArtifactVersions(sessionId: string, artifactType: string): Observable<AiGeneratedArtifactCollectionResponse> {
    return this.http.get<AiGeneratedArtifactCollectionResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/artifacts/ai/${encodeURIComponent(artifactType)}/versions`,
    );
  }

  verifyPersistedArtifactCoherence(sessionId: string): Observable<AiArtifactCoherenceResponse> {
    return this.http.post<AiArtifactCoherenceResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/artifacts/ai/coherence`,
      null,
    );
  }

  exportGeneratedZip(sessionId: string): Observable<Blob> {
    return this.http.post(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/generate/ai/export/zip`,
      null,
      { responseType: 'blob' },
    );
  }

  previewSanitized(sessionId: string): Observable<SanitizedSourcePreviewResponse> {
    return this.http.post<SanitizedSourcePreviewResponse>(
      `${this.sessionBase}/${encodeURIComponent(sessionId)}/preview-sanitized`,
      null
    );
  }

  private syntheticGenerateStream(
    sessionId: string,
    provider?: AiEnrichmentProvider,
  ): Observable<AiGenerationStreamEvent> {
    return new Observable<AiGenerationStreamEvent>(subscriber => {
      const emitProgress = (event: AiGenerationStreamEvent): void => {
        subscriber.next(event);
      };

      const subscription = this.generate(sessionId, provider).subscribe({
        next: result => {
          emitProgress({
            stage: 'sanitizing',
            message: 'Sanitisation et preparation du contexte IA',
            progress: 15,
          });
          emitProgress({
            stage: 'sending_to_llm',
            message: `Appel du fournisseur IA ${result.provider}`,
            progress: 45,
            provider: result.provider,
            degraded: result.degraded,
          });
          emitProgress({
            stage: 'parsing_response',
            message: 'Analyse de la reponse IA',
            progress: 75,
            provider: result.provider,
          });
          emitProgress({
            stage: 'validating',
            message: 'Validation des classes generees',
            progress: 90,
            provider: result.provider,
          });
          emitProgress({
            stage: 'complete',
            message: result.degraded
              ? 'Generation terminee en mode degrade'
              : 'Generation terminee avec succes',
            progress: 100,
            generatedClasses: result.generatedClasses,
            tokensUsed: result.tokensUsed,
            provider: result.provider,
            degraded: result.degraded,
          });
          subscriber.complete();
        },
        error: err => {
          subscriber.next({
            stage: 'error',
            message: 'Erreur lors de la generation IA',
            progress: 100,
            error: err?.error?.message ?? 'Generation IA indisponible',
          });
          subscriber.complete();
        },
      });

      emitProgress({
        stage: 'sanitizing',
        message: 'Sanitisation du code source et preparation de la generation',
        progress: 10,
      });
      emitProgress({
        stage: 'sending_to_llm',
        message: 'Preparation du flux de generation',
        progress: 35,
      });

      return () => subscription.unsubscribe();
    });
  }

  private parseGenerationStreamEvent(rawEvent: string): AiGenerationStreamEvent {
    try {
      const parsed = JSON.parse(rawEvent) as AiGenerationStreamEvent;
      return {
        stage: parsed.stage,
        message: parsed.message ?? 'Evenement SSE de generation IA',
        progress: typeof parsed.progress === 'number' ? parsed.progress : 0,
        artifactKey: parsed.artifactKey,
        chunk: parsed.chunk,
        generatedClasses: parsed.generatedClasses,
        tokensUsed: parsed.tokensUsed,
        provider: parsed.provider,
        degraded: parsed.degraded,
        error: parsed.error,
      };
    } catch {
      return {
        stage: 'streaming',
        message: rawEvent,
        progress: 50,
      };
    }
  }

  private buildProviderQueryParams(provider?: AiEnrichmentProvider): Record<string, string> | undefined {
    if (!provider) {
      return undefined;
    }

    return { provider };
  }

  private buildStreamUrl(sessionId: string, provider?: AiEnrichmentProvider): string {
    const baseUrl = `${this.sessionBase}/${encodeURIComponent(sessionId)}/generate/ai/stream`;
    if (!provider) {
      return baseUrl;
    }

    return `${baseUrl}?provider=${encodeURIComponent(provider)}`;
  }
}
