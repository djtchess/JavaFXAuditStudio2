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
  }
];
