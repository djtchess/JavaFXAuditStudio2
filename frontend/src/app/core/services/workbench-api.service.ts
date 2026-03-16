import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { WorkbenchOverview } from '../models/workbench-overview.model';

@Injectable({ providedIn: 'root' })
export class WorkbenchApiService {
  private readonly http = inject(HttpClient);

  loadOverview(): Observable<WorkbenchOverview> {
    return this.http.get<WorkbenchOverview>('/api/v1/workbench/overview');
  }
}
