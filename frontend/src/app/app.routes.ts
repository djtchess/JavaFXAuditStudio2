import { Routes } from '@angular/router';

import { DashboardComponent } from './features/dashboard/dashboard.component';

export const routes: Routes = [
  {
    path: '',
    component: DashboardComponent
  },
  {
    path: 'analysis',
    loadComponent: () =>
      import('./features/analysis/analysis-submit.component').then(m => m.AnalysisSubmitComponent)
  },
  {
    path: 'analysis/:sessionId',
    loadComponent: () =>
      import('./features/analysis/analysis-detail.component').then(m => m.AnalysisDetailComponent)
  },
  {
    path: 'projects',
    loadComponent: () =>
      import('./features/project-dashboard/project-dashboard.component').then(
        m => m.ProjectDashboardComponent
      )
  },
  {
    path: 'monitoring',
    loadComponent: () =>
      import('./features/monitoring/monitoring-dashboard.component').then(
        m => m.MonitoringDashboardComponent
      )
  }
];
