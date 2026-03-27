import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { MigrationPlanViewComponent } from './migration-plan-view.component';
import { MigrationPlanResponse } from '../../../core/models/analysis.model';

const COMPILABLE_PLAN: MigrationPlanResponse = {
  controllerRef: 'Ctrl',
  compilable: true,
  lots: [
    {
      lotNumber: 1,
      title: 'Lot 1 - UI',
      objective: 'Extract view logic',
      extractionCandidates: ['MainController', 'DialogController'],
    },
    {
      lotNumber: 2,
      title: 'Lot 2 - Services',
      objective: 'Move orchestration to use cases',
      extractionCandidates: [],
    },
  ],
};

const EMPTY_PLAN: MigrationPlanResponse = {
  controllerRef: 'Ctrl',
  compilable: false,
  lots: [],
};

describe('MigrationPlanViewComponent', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should render a compilable badge, lots and extraction candidates', async () => {
    await TestBed.configureTestingModule({
      imports: [MigrationPlanViewComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(MigrationPlanViewComponent);
    fixture.componentRef.setInput('data', COMPILABLE_PLAN);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.compilable-yes'))).toBeTruthy();
    const lots = fixture.debugElement.queryAll(By.css('.lot-step'));
    expect(lots.length).toBe(2);
    expect(lots[0].nativeElement.textContent).toContain('Lot 1 - UI');
    expect(lots[0].nativeElement.textContent).toContain('MainController');
    expect(lots[0].nativeElement.textContent).toContain('DialogController');
  });

  it('should render the empty state when there are no lots', async () => {
    await TestBed.configureTestingModule({
      imports: [MigrationPlanViewComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(MigrationPlanViewComponent);
    fixture.componentRef.setInput('data', EMPTY_PLAN);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.compilable-no'))).toBeTruthy();
    const emptyMessage = fixture.debugElement.query(By.css('.empty-msg'));
    expect(emptyMessage.nativeElement.textContent).toContain('Aucun lot planifie');
  });
});
