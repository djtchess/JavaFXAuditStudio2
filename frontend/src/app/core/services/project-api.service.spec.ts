import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { ProjectApiService } from './project-api.service';
import { ProjectDashboardResponse } from '../models/analysis.model';

const MOCK_DASHBOARD: ProjectDashboardResponse = {
  projectId: 'proj-alpha',
  totalSessions: 10,
  analysingCount: 3,
  completedCount: 7,
  rulesByCategory: { UI: 5, APPLICATION: 8, BUSINESS: 12, TECHNICAL: 4, UNKNOWN: 1 },
  uncertainCount: 2,
  reclassifiedCount: 1,
  recommendedLotOrder: ['Lot 1 - UI', 'Lot 2 - Services'],
};

describe('ProjectApiService', () => {
  let service: ProjectApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  describe('getDashboard', () => {
    it('should GET /api/v1/projects/{projectId}/dashboard', () => {
      let result: ProjectDashboardResponse | undefined;

      service.getDashboard('proj-alpha').subscribe(r => { result = r; });

      const req = httpController.expectOne('/api/v1/projects/proj-alpha/dashboard');
      expect(req.request.method).toBe('GET');
      req.flush(MOCK_DASHBOARD);

      expect(result).toEqual(MOCK_DASHBOARD);
    });

    it('should use the projectId in the URL', () => {
      service.getDashboard('my-project-123').subscribe();

      const req = httpController.expectOne('/api/v1/projects/my-project-123/dashboard');
      expect(req.request.url).toContain('my-project-123');
      req.flush(MOCK_DASHBOARD);
    });
  });

  describe('listProjects', () => {
    it('should GET /api/v1/projects and return string[]', () => {
      const mockList = ['proj-alpha', 'proj-beta'];
      let result: string[] | undefined;

      service.listProjects().subscribe(r => { result = r; });

      const req = httpController.expectOne('/api/v1/projects');
      expect(req.request.method).toBe('GET');
      req.flush(mockList);

      expect(result).toEqual(mockList);
    });

    it('should return empty array when backend returns []', () => {
      let result: string[] | undefined;

      service.listProjects().subscribe(r => { result = r; });

      const req = httpController.expectOne('/api/v1/projects');
      req.flush([]);

      expect(result).toEqual([]);
    });
  });
});
