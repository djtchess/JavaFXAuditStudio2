import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AnalysisSessionResponse,
  ArtifactsResponse,
  CartographyResponse,
  ClassificationResponse,
  MigrationPlanResponse,
  OrchestratedAnalysisResultResponse,
  RestitutionReportResponse,
  SubmitAnalysisRequest,
} from '../models/analysis.model';

@Injectable({ providedIn: 'root' })
export class AnalysisApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/analysis/sessions';

  submitSession(request: SubmitAnalysisRequest): Observable<AnalysisSessionResponse> {
    return this.http.post<AnalysisSessionResponse>(this.base, request);
  }

  getCartography(sessionId: string): Observable<CartographyResponse> {
    return this.http.get<CartographyResponse>(`${this.base}/${sessionId}/cartography`);
  }

  getClassification(sessionId: string): Observable<ClassificationResponse> {
    return this.http.get<ClassificationResponse>(`${this.base}/${sessionId}/classification`);
  }

  getMigrationPlan(sessionId: string): Observable<MigrationPlanResponse> {
    return this.http.get<MigrationPlanResponse>(`${this.base}/${sessionId}/plan`);
  }

  getArtifacts(sessionId: string): Observable<ArtifactsResponse> {
    return this.http.get<ArtifactsResponse>(`${this.base}/${sessionId}/artifacts`);
  }

  getReport(sessionId: string): Observable<RestitutionReportResponse> {
    return this.http.get<RestitutionReportResponse>(`${this.base}/${sessionId}/report`);
  }

  runFullPipeline(sessionId: string): Observable<OrchestratedAnalysisResultResponse> {
    return this.http.post<OrchestratedAnalysisResultResponse>(
      `${this.base}/${sessionId}/run`,
      null,
    );
  }
}
