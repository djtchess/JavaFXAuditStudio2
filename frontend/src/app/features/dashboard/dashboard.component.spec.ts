import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { WorkbenchApiService } from '../../core/services/workbench-api.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  it('should create with a mocked overview', async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        {
          provide: WorkbenchApiService,
          useValue: {
            loadOverview: () =>
              of({
                productName: 'JavaFX Audit Studio',
                summary: 'Cockpit de refactoring progressif',
                frontendTarget: 'Angular 21.2.x',
                backendTarget: 'JDK 21 / Spring Boot 4.0.3',
                lots: [],
                agents: []
              })
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(DashboardComponent);

    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });
});
