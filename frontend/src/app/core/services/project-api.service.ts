import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ProjectDashboardResponse } from '../models/analysis.model';

/**
 * Service d'accès aux endpoints projets.
 * Expose la liste des projets et le dashboard de progression par projet.
 * JAS-014
 */
@Injectable({ providedIn: 'root' })
export class ProjectApiService {
  private readonly http = inject(HttpClient);
  private readonly baseProjects = '/api/v1/projects';

  getDashboard(projectId: string): Observable<ProjectDashboardResponse> {
    return this.http.get<ProjectDashboardResponse>(
      `${this.baseProjects}/${encodeURIComponent(projectId)}/dashboard`
    );
  }

  listProjects(): Observable<string[]> {
    return this.http.get<string[]>(this.baseProjects);
  }
}
