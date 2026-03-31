import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { ArtifactsViewComponent } from './artifacts-view.component';
import { AnalysisApiService } from '../../../core/services/analysis-api.service';
import { ArtifactsResponse } from '../../../core/models/analysis.model';

const ARTIFACTS: ArtifactsResponse = {
  controllerRef: 'Ctrl',
  warnings: ['Artifact order should be reviewed'],
  artifacts: [
    {
      artifactId: 'art-1',
      type: 'USE_CASE',
      lotNumber: 1,
      className: 'MainControllerUseCase',
      content: 'class MainControllerUseCase {}',
      transitionalBridge: false,
      generationWarnings: ['DUPLICATE_METHOD_NAME'],
      generationStatus: 'OK',
    },
    {
      artifactId: 'art-2',
      type: 'POLICY',
      lotNumber: 2,
      className: 'DialogPolicy',
      content: 'class DialogPolicy {}',
      transitionalBridge: true,
      generationWarnings: [],
      generationStatus: 'WARNING',
    },
  ],
};

type AnalysisApiSpy = {
  exportArtifacts: ReturnType<typeof vi.fn>;
};

describe('ArtifactsViewComponent', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should render lots, allow code toggle and export successfully', async () => {
    const apiSpy: AnalysisApiSpy = {
      exportArtifacts: vi.fn().mockReturnValue(
        of({ targetDirectory: '/tmp/out', exportedFiles: ['MainControllerUseCase.java'], errors: [] }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ArtifactsViewComponent],
      providers: [{ provide: AnalysisApiService, useValue: apiSpy }],
    }).compileComponents();

    const fixture = TestBed.createComponent(ArtifactsViewComponent);
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.componentRef.setInput('data', ARTIFACTS);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.warning-banner'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.needs-review-summary'))).toBeTruthy();
    expect(fixture.debugElement.queryAll(By.css('.tab-btn')).length).toBe(2);

    const firstToggle = fixture.debugElement.query(By.css('.code-toggle'));
    firstToggle.nativeElement.click();
    fixture.detectChanges();

    const codeBlock = fixture.debugElement.query(By.css('.code-block'));
    expect(codeBlock.nativeElement.textContent).toContain('MainControllerUseCase');

    const exportInput = fixture.nativeElement.querySelector('.export-input') as HTMLInputElement;
    exportInput.value = '  /tmp/out  ';
    exportInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const exportButton = fixture.debugElement.query(By.css('.export-btn')).nativeElement as HTMLButtonElement;
    exportButton.click();
    fixture.detectChanges();

    expect(apiSpy.exportArtifacts).toHaveBeenCalledWith('session-1', '/tmp/out');
    const exportOk = fixture.debugElement.query(By.css('.export-ok'));
    expect(exportOk.nativeElement.textContent).toContain('1 fichier');
  });

  it('should allow copying and downloading a single artifact', async () => {
    const apiSpy: AnalysisApiSpy = {
      exportArtifacts: vi.fn().mockReturnValue(
        of({ targetDirectory: '/tmp/out', exportedFiles: ['MainControllerUseCase.java'], errors: [] }),
      ),
    };

    const clipboardWrite = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: clipboardWrite },
      configurable: true,
    });
    const createObjectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock');
    const revokeObjectUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const anchorClickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await TestBed.configureTestingModule({
      imports: [ArtifactsViewComponent],
      providers: [{ provide: AnalysisApiService, useValue: apiSpy }],
    }).compileComponents();

    const fixture = TestBed.createComponent(ArtifactsViewComponent);
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.componentRef.setInput('data', ARTIFACTS);
    fixture.detectChanges();

    const actionButtons = fixture.debugElement.queryAll(By.css('.artifact-action-btn'));
    expect(actionButtons.length).toBeGreaterThan(0);

    actionButtons[0].nativeElement.click();
    fixture.detectChanges();
    expect(clipboardWrite).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Code copie dans le presse-papiers');

    actionButtons[1].nativeElement.click();
    fixture.detectChanges();
    expect(createObjectUrlSpy).toHaveBeenCalled();
    expect(revokeObjectUrlSpy).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Fichier MainControllerUseCase.java telecharge');

    anchorClickSpy.mockRestore();
    createObjectUrlSpy.mockRestore();
    revokeObjectUrlSpy.mockRestore();
  });

  it('should show an export error when the backend call fails', async () => {
    const apiSpy: AnalysisApiSpy = {
      exportArtifacts: vi.fn().mockReturnValue(
        throwError(() => ({ error: { message: 'Export refused' } })),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ArtifactsViewComponent],
      providers: [{ provide: AnalysisApiService, useValue: apiSpy }],
    }).compileComponents();

    const fixture = TestBed.createComponent(ArtifactsViewComponent);
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.componentRef.setInput('data', ARTIFACTS);
    fixture.detectChanges();

    const exportInput = fixture.nativeElement.querySelector('.export-input') as HTMLInputElement;
    exportInput.value = 'C:/tmp/out';
    exportInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.export-btn')).nativeElement.click();
    fixture.detectChanges();

    const errorLine = fixture.debugElement.query(By.css('.export-err'));
    expect(errorLine.nativeElement.textContent).toContain('Erreur lors de la communication avec le serveur');
  });
});
