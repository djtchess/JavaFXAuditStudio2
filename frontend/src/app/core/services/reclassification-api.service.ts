import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ReclassificationAuditEntryResponse,
  ReclassifiedRuleResponse,
  ReclassifyRuleRequest,
} from '../models/analysis.model';

/**
 * Service d'acces aux endpoints de reclassification manuelle des regles.
 * JAS-012
 */
@Injectable({ providedIn: 'root' })
export class ReclassificationApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/analysis/sessions';

  reclassify(
    analysisId: string,
    ruleId: string,
    request: ReclassifyRuleRequest,
  ): Observable<ReclassifiedRuleResponse> {
    return this.http.patch<ReclassifiedRuleResponse>(
      `${this.base}/${analysisId}/rules/${ruleId}/classification`,
      request,
    );
  }

  getHistory(
    analysisId: string,
    ruleId: string,
  ): Observable<ReclassificationAuditEntryResponse[]> {
    return this.http.get<ReclassificationAuditEntryResponse[]>(
      `${this.base}/${analysisId}/rules/${ruleId}/classification/history`,
    );
  }
}
