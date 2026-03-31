import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { ReportViewComponent } from './report-view.component';
import { RestitutionReportResponse } from '../../../core/models/analysis.model';

const REPORT_WITH_MARKDOWN: RestitutionReportResponse = {
  controllerRef: 'Ctrl',
  ruleCount: 4,
  artifactCount: 2,
  confidence: 'HIGH',
  isActionable: true,
  findings: ['Move logic to use case'],
  unknowns: ['Needs manual review'],
  markdown: '# Restitution\n\n## Synthese\n- item',
};

const REPORT_WITHOUT_MARKDOWN: RestitutionReportResponse = {
  controllerRef: 'Ctrl',
  ruleCount: 0,
  artifactCount: 0,
  confidence: 'UNEXPECTED',
  isActionable: false,
  findings: [],
  unknowns: [],
  markdown: '',
};

describe('ReportViewComponent', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should render summary, findings, unknowns and markdown when present', async () => {
    await TestBed.configureTestingModule({
      imports: [ReportViewComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(ReportViewComponent);
    fixture.componentRef.setInput('data', REPORT_WITH_MARKDOWN);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.metric-value')).nativeElement.textContent).toContain('4');
    expect(fixture.debugElement.query(By.css('.actionable-yes'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.findings-list'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.unknowns-list'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.section-label')).nativeElement.textContent).toContain('Conclusions');
    expect(fixture.debugElement.query(By.css('.markdown-panel pre')).nativeElement.textContent).toContain('# Restitution');
  });

  it('should hide markdown block when markdown is empty and mark report as non actionable', async () => {
    await TestBed.configureTestingModule({
      imports: [ReportViewComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(ReportViewComponent);
    fixture.componentRef.setInput('data', REPORT_WITHOUT_MARKDOWN);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      confidenceColor: () => string;
    };

    expect(fixture.debugElement.query(By.css('.actionable-no'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.findings-list'))).toBeNull();
    expect(fixture.debugElement.query(By.css('.unknowns-list'))).toBeNull();
    expect(fixture.debugElement.query(By.css('.markdown-panel'))).toBeNull();
    expect(component.confidenceColor()).toBe('#a23c1c');
  });
});
