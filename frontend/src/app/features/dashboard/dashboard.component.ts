import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith } from 'rxjs';

import { WorkbenchApiService } from '../../core/services/workbench-api.service';
import { WorkbenchOverview } from '../../core/models/workbench-overview.model';

interface DashboardViewModel {
  errorMessage: string | null;
  isLoading: boolean;
  overview: WorkbenchOverview | null;
}

@Component({
  selector: 'jas-dashboard',
  imports: [AsyncPipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  private readonly workbenchApiService = inject(WorkbenchApiService);

  protected readonly viewModel$ = this.workbenchApiService.loadOverview().pipe(
    map((overview): DashboardViewModel => ({
      errorMessage: null,
      isLoading: false,
      overview
    })),
    startWith({
      errorMessage: null,
      isLoading: true,
      overview: null
    } satisfies DashboardViewModel),
    catchError(() =>
      of({
        errorMessage: 'Backend indisponible. Lance le service Spring Boot pour hydrater le cockpit.',
        isLoading: false,
        overview: null
      } satisfies DashboardViewModel))
  );
}
