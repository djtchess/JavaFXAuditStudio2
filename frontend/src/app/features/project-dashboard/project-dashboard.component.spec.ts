import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach } from 'vitest';

import { ProjectDashboardComponent } from './project-dashboard.component';
import { ProjectApiService } from '../../core/services/project-api.service';
import { ProjectDashboardResponse } from '../../core/models/analysis.model';

const MOCK_DASHBOARD: ProjectDashboardResponse = {
  projectId: 'proj-alpha',
  totalSessions: 10,
  analysingCount: 3,
  completedCount: 7,
  rulesByCategory: { UI: 5, APPLICATION: 8 },
  uncertainCount: 2,
  reclassifiedCount: 1,
  recommendedLotOrder: ['Lot 1 - UI', 'Lot 2 - Services'],
};

function getTextContents(el: HTMLElement, selector: string): string[] {
  const nodes: NodeListOf<HTMLElement> = el.querySelectorAll(selector);
  return Array.from(nodes).map(n => n.textContent?.trim() ?? '');
}

describe('ProjectDashboardComponent', () => {
  function createComponent(
    projectsMock: string[] = ['proj-alpha'],
    dashboardMock: ProjectDashboardResponse = MOCK_DASHBOARD
  ) {
    const projectApiSpy = {
      listProjects: () => of(projectsMock),
      getDashboard: (_id: string) => of(dashboardMock),
    };

    TestBed.configureTestingModule({
      imports: [ProjectDashboardComponent],
      providers: [{ provide: ProjectApiService, useValue: projectApiSpy }],
    });

    const fixture = TestBed.createComponent(ProjectDashboardComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should create', async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDashboardComponent],
      providers: [
        {
          provide: ProjectApiService,
          useValue: {
            listProjects: () => of(['proj-alpha']),
            getDashboard: () => of(MOCK_DASHBOARD),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProjectDashboardComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display totalSessions counter', () => {
    const fixture = createComponent();
    const values = getTextContents(fixture.nativeElement, '.pd-card-value');
    expect(values).toContain('10');
  });

  it('should display analysingCount counter', () => {
    const fixture = createComponent();
    const values = getTextContents(fixture.nativeElement, '.pd-card-value');
    expect(values).toContain('3');
  });

  it('should display completedCount counter', () => {
    const fixture = createComponent();
    const values = getTextContents(fixture.nativeElement, '.pd-card-value');
    expect(values).toContain('7');
  });

  it('should display uncertainCount and reclassifiedCount', () => {
    const fixture = createComponent();
    const values = getTextContents(fixture.nativeElement, '.pd-card-value');
    expect(values).toContain('2');
    expect(values).toContain('1');
  });

  it('should display recommended lot order list', () => {
    const fixture = createComponent();
    const lotItems: NodeListOf<HTMLElement> =
      fixture.nativeElement.querySelectorAll('.pd-lot-item');
    expect(lotItems.length).toBe(2);
    const texts = Array.from(lotItems).map(n => n.textContent?.trim() ?? '');
    expect(texts[0]).toContain('Lot 1 - UI');
    expect(texts[1]).toContain('Lot 2 - Services');
  });

  it('should show error message when listProjects fails', async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDashboardComponent],
      providers: [
        {
          provide: ProjectApiService,
          useValue: {
            listProjects: () => throwError(() => new Error('network error')),
            getDashboard: () => of(MOCK_DASHBOARD),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProjectDashboardComponent);
    fixture.detectChanges();

    const errorEl: HTMLElement | null = fixture.nativeElement.querySelector('.pd-error');
    expect(errorEl).not.toBeNull();
    expect(errorEl!.textContent).toContain('projets');
  });

  it('should show project selector buttons when multiple projects exist', async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDashboardComponent],
      providers: [
        {
          provide: ProjectApiService,
          useValue: {
            listProjects: () => of(['proj-alpha', 'proj-beta', 'proj-gamma']),
            getDashboard: () => of(MOCK_DASHBOARD),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProjectDashboardComponent);
    fixture.detectChanges();

    const buttons: NodeListOf<HTMLElement> =
      fixture.nativeElement.querySelectorAll('.pd-project-btn');
    expect(buttons.length).toBe(3);
  });

  it('should not show project selector when only one project', () => {
    const fixture = createComponent(['proj-alpha']);
    const selector: HTMLElement | null =
      fixture.nativeElement.querySelector('.pd-project-selector');
    expect(selector).toBeNull();
  });
});
