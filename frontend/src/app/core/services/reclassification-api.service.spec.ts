import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';

import { ReclassificationApiService } from './reclassification-api.service';
import {
  ReclassifiedRuleResponse,
  ReclassificationAuditEntryResponse,
} from '../models/analysis.model';

describe('ReclassificationApiService', () => {
  let service: ReclassificationApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ReclassificationApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('reclassify', () => {
    it('should PATCH the correct URL with request body', () => {
      const analysisId = 'session-1';
      const ruleId = 'rule-42';
      const request = { category: 'APPLICATION', reason: 'Test reason' };
      const mockResponse: ReclassifiedRuleResponse = {
        ruleId: 'rule-42',
        description: 'Test rule',
        responsibilityClass: 'APPLICATION',
        extractionCandidate: 'SERVICE',
        uncertain: false,
        manuallyReclassified: true,
      };

      service.reclassify(analysisId, ruleId, request).subscribe(result => {
        expect(result).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(
        `/api/v1/analysis/sessions/${analysisId}/rules/${ruleId}/classification`,
      );
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });

    it('should propagate HTTP errors', () => {
      const analysisId = 'session-1';
      const ruleId = 'rule-42';
      const request = { category: 'BUSINESS', reason: '' };
      let receivedError: unknown;

      service.reclassify(analysisId, ruleId, request).subscribe({
        error: err => {
          receivedError = err;
        },
      });

      const req = httpMock.expectOne(
        `/api/v1/analysis/sessions/${analysisId}/rules/${ruleId}/classification`,
      );
      req.flush({ message: 'Analyse locked' }, { status: 409, statusText: 'Conflict' });
      expect(receivedError).toBeTruthy();
    });
  });

  describe('getHistory', () => {
    it('should GET the correct URL and return history entries', () => {
      const analysisId = 'session-1';
      const ruleId = 'rule-42';
      const mockEntries: ReclassificationAuditEntryResponse[] = [
        {
          ruleId: 'rule-42',
          fromCategory: 'UI',
          toCategory: 'APPLICATION',
          reason: 'Moved to service layer',
          timestamp: '2026-03-22T10:00:00Z',
        },
      ];

      service.getHistory(analysisId, ruleId).subscribe(result => {
        expect(result).toEqual(mockEntries);
        expect(result.length).toBe(1);
      });

      const req = httpMock.expectOne(
        `/api/v1/analysis/sessions/${analysisId}/rules/${ruleId}/classification/history`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockEntries);
    });

    it('should return empty array when history is empty', () => {
      const analysisId = 'session-1';
      const ruleId = 'rule-99';

      service.getHistory(analysisId, ruleId).subscribe(result => {
        expect(result).toEqual([]);
      });

      const req = httpMock.expectOne(
        `/api/v1/analysis/sessions/${analysisId}/rules/${ruleId}/classification/history`,
      );
      req.flush([]);
    });
  });
});
