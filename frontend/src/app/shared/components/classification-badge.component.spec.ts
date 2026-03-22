import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { describe, it, expect, beforeEach } from 'vitest';

import { ClassificationBadgeComponent } from './classification-badge.component';

@Component({
  template: `<jas-classification-badge [responsibilityClass]="category" [uncertain]="uncertain" [showParsingMode]="showParsing" [parsingMode]="parsMode" />`,
  imports: [ClassificationBadgeComponent]
})
class TestHostComponent {
  category = 'UI';
  uncertain = false;
  showParsing = false;
  parsMode: 'AST' | 'REGEX_FALLBACK' = 'AST';
}

describe('ClassificationBadgeComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();
  });

  it('should show UI badge with blue color', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'UI';
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.class-badge') as HTMLElement;
    expect(badge.style.background).toBe('rgb(59, 130, 246)');
  });

  it('should show APPLICATION badge with green color', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'APPLICATION';
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.class-badge') as HTMLElement;
    expect(badge.style.background).toBe('rgb(16, 185, 129)');
  });

  it('should show BUSINESS badge with orange color', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'BUSINESS';
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.class-badge') as HTMLElement;
    expect(badge.style.background).toBe('rgb(245, 158, 11)');
  });

  it('should show TECHNICAL badge with gray color', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'TECHNICAL';
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.class-badge') as HTMLElement;
    expect(badge.style.background).toBe('rgb(107, 114, 128)');
  });

  it('should show UNKNOWN badge with red color', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'UNKNOWN';
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.class-badge') as HTMLElement;
    expect(badge.style.background).toBe('rgb(239, 68, 68)');
  });

  it('should show uncertain dot when uncertain is true', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'APPLICATION';
    fixture.componentInstance.uncertain = true;
    fixture.detectChanges();
    const dot = fixture.nativeElement.querySelector('.uncertain-dot');
    expect(dot).not.toBeNull();
  });

  it('should not show uncertain dot when uncertain is false', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'APPLICATION';
    fixture.componentInstance.uncertain = false;
    fixture.detectChanges();
    const dot = fixture.nativeElement.querySelector('.uncertain-dot');
    expect(dot).toBeNull();
  });

  it('should show AST parsing badge when showParsingMode is true and mode is AST', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'APPLICATION';
    fixture.componentInstance.showParsing = true;
    fixture.componentInstance.parsMode = 'AST';
    fixture.detectChanges();
    const parsingBadge = fixture.nativeElement.querySelector('.parsing-badge');
    expect(parsingBadge).not.toBeNull();
    expect(parsingBadge.textContent.trim()).toBe('AST');
    expect(parsingBadge.classList.contains('ast')).toBe(true);
  });

  it('should show REGEX parsing badge when mode is REGEX_FALLBACK', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'APPLICATION';
    fixture.componentInstance.showParsing = true;
    fixture.componentInstance.parsMode = 'REGEX_FALLBACK';
    fixture.detectChanges();
    const parsingBadge = fixture.nativeElement.querySelector('.parsing-badge');
    expect(parsingBadge).not.toBeNull();
    expect(parsingBadge.textContent.trim()).toBe('REGEX');
    expect(parsingBadge.classList.contains('regex')).toBe(true);
  });

  it('should not show parsing badge when showParsingMode is false', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.category = 'APPLICATION';
    fixture.componentInstance.showParsing = false;
    fixture.detectChanges();
    const parsingBadge = fixture.nativeElement.querySelector('.parsing-badge');
    expect(parsingBadge).toBeNull();
  });
});
