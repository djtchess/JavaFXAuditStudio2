import { TestBed } from '@angular/core/testing';
import { Subject, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { AnalysisSubmitComponent } from './analysis-submit.component';
import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { AnalysisSessionResponse } from '../../core/models/analysis.model';
import { ActivatedRoute, Router } from '@angular/router';

const SUCCESS_RESPONSE: AnalysisSessionResponse = {
  sessionId: 'session-123',
  status: 'CREATED',
  sessionName: 'Audit MainController',
  controllerRef: 'src/main/java/com/app/MainController.java',
};

type AnalysisApiSpy = {
  submitSession: ReturnType<typeof vi.fn>;
};

type RouterSpy = {
  navigate: ReturnType<typeof vi.fn>;
};

describe('AnalysisSubmitComponent', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should keep submit disabled until fields are filled and navigate after success', async () => {
    const submitResponse$ = new Subject<AnalysisSessionResponse>();
    const apiSpy: AnalysisApiSpy = {
      submitSession: vi.fn().mockReturnValue(submitResponse$.asObservable()),
    };

    const routerSpy: RouterSpy = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [AnalysisSubmitComponent],
      providers: [
        { provide: AnalysisApiService, useValue: apiSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: {} } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AnalysisSubmitComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      sessionName: string;
      sourceFilePathsRaw: string;
      onSubmit: () => void;
    };
    const submitButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submitButton.disabled).toBe(true);

    component.sessionName = '  Audit MainController  ';
    component.sourceFilePathsRaw = 'src/main/java/com/app/MainController.java\n\nsrc/main/resources/view/Main.fxml  ';
    fixture.detectChanges();

    component.onSubmit();
    fixture.detectChanges();

    expect(submitButton.textContent).toContain('Soumission en cours');
    expect(apiSpy.submitSession).toHaveBeenCalledWith({
      sessionName: 'Audit MainController',
      sourceFilePaths: [
        'src/main/java/com/app/MainController.java',
        'src/main/resources/view/Main.fxml',
      ],
    });

    submitResponse$.next(SUCCESS_RESPONSE);
    submitResponse$.complete();
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/analysis', 'session-123']);
  });

  it('should show an error message when submission fails', async () => {
    const apiSpy: AnalysisApiSpy = {
      submitSession: vi.fn().mockReturnValue(
        throwError(() => ({ error: { message: 'Backend unavailable' } })),
      ),
    };

    const routerSpy: RouterSpy = {
      navigate: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AnalysisSubmitComponent],
      providers: [
        { provide: AnalysisApiService, useValue: apiSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: {} } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AnalysisSubmitComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      sessionName: string;
      sourceFilePathsRaw: string;
      onSubmit: () => void;
    };
    component.sessionName = 'Audit MainController';
    component.sourceFilePathsRaw = 'src/main/java/com/app/MainController.java';
    fixture.detectChanges();

    component.onSubmit();
    fixture.detectChanges();

    const errorPanel = fixture.nativeElement.querySelector('.status-panel.error');
    expect(errorPanel).toBeTruthy();
    expect(errorPanel.textContent).toContain('Backend unavailable');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
