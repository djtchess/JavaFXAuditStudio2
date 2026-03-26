import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { ClassificationViewComponent } from './classification-view.component';
import { ClassificationResponse, MethodSignatureDto } from '../../../core/models/analysis.model';

const BASE_CLASSIFICATION: ClassificationResponse = {
  controllerRef: 'TestController',
  ruleCount: 2,
  uncertainCount: 0,
  excludedLifecycleMethodsCount: 0,
  rules: [
    {
      ruleId: 'R-001',
      description: 'Validate input data',
      responsibilityClass: 'BUSINESS',
      extractionCandidate: 'SERVICE',
      uncertain: false,
    },
    {
      ruleId: 'R-002',
      description: 'Render result to UI',
      responsibilityClass: 'UI',
      extractionCandidate: '',
      uncertain: false,
    },
  ],
  parsingMode: 'AST',
  parsingFallbackReason: undefined,
};

describe('ClassificationViewComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClassificationViewComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  it('should display regex banner when parsingMode is REGEX_FALLBACK', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      parsingMode: 'REGEX_FALLBACK',
      parsingFallbackReason: 'JavaParser failed',
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const banner = fixture.debugElement.query(By.css('.regex-banner'));
    expect(banner).not.toBeNull();
    expect(banner.nativeElement.textContent).toContain('Analyse en mode regex');
  });

  it('should not display regex banner when parsingMode is AST', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      parsingMode: 'AST',
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const banner = fixture.debugElement.query(By.css('.regex-banner'));
    expect(banner).toBeNull();
  });

  it('should display excluded lifecycle count when > 0', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      excludedLifecycleMethodsCount: 3,
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const chip = fixture.debugElement.query(By.css('.lifecycle-chip'));
    expect(chip).not.toBeNull();
    expect(chip.nativeElement.textContent.trim()).toContain('3 lifecycle exclus');
  });

  it('should not display lifecycle chip when excludedLifecycleMethodsCount is 0', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      excludedLifecycleMethodsCount: 0,
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const chip = fixture.debugElement.query(By.css('.lifecycle-chip'));
    expect(chip).toBeNull();
  });

  it('should display low confidence badge in regex fallback mode', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      parsingMode: 'REGEX_FALLBACK',
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const badges = fixture.debugElement.queryAll(By.css('.confidence-badge.low'));
    expect(badges.length).toBe(BASE_CLASSIFICATION.rules.length);
    expect(badges[0].nativeElement.textContent.trim()).toBe('Faible');
  });

  it('should not display low confidence badge in AST mode', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      parsingMode: 'AST',
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const badges = fixture.debugElement.queryAll(By.css('.confidence-badge.low'));
    expect(badges.length).toBe(0);
  });

  it('should display method signature for a rule with signature (AST mode)', () => {
    const signature: MethodSignatureDto = {
      returnType: 'void',
      parameters: [
        { type: 'ActionEvent', name: 'event', unknown: false },
      ],
      hasUnknowns: false,
    };

    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      rules: [
        {
          ruleId: 'R-001',
          description: 'Validate input data',
          responsibilityClass: 'BUSINESS',
          extractionCandidate: 'SERVICE',
          uncertain: false,
          signature,
        },
      ],
      ruleCount: 1,
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const signatureEl = fixture.debugElement.query(By.css('.method-signature'));
    expect(signatureEl).not.toBeNull();
    const text = signatureEl.nativeElement.textContent as string;
    expect(text).toContain('void');
    expect(text).toContain('ActionEvent event');

    const unknownHint = fixture.debugElement.query(By.css('.unknown-hint'));
    expect(unknownHint).toBeNull();
  });

  it('should not display method signature for a rule without signature (REGEX_FALLBACK)', () => {
    const fixture = TestBed.createComponent(ClassificationViewComponent);
    fixture.componentRef.setInput('data', {
      ...BASE_CLASSIFICATION,
      parsingMode: 'REGEX_FALLBACK',
      rules: [
        {
          ruleId: 'R-001',
          description: 'Validate input data',
          responsibilityClass: 'BUSINESS',
          extractionCandidate: 'SERVICE',
          uncertain: false,
          // signature absent : mode REGEX_FALLBACK
        },
      ],
      ruleCount: 1,
    });
    fixture.componentRef.setInput('sessionId', 'session-1');
    fixture.detectChanges();

    const signatureEl = fixture.debugElement.query(By.css('.method-signature'));
    expect(signatureEl).toBeNull();
  });
});
