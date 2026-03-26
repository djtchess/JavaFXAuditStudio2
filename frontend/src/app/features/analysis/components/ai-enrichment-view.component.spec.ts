import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { AiEnrichmentViewComponent } from './ai-enrichment-view.component';
import { AiEnrichmentApiService } from '../../../core/services/ai-enrichment-api.service';
import {
  AiEnrichmentResponse,
  AiEnrichmentStatusResponse,
  LlmAuditEntryResponse,
} from '../../../core/models/analysis.model';

const MOCK_STATUS_DISABLED: AiEnrichmentStatusResponse = {
  enabled: false,
  provider: 'none',
  credentialPresent: false,
  timeoutMs: 5000,
};

const MOCK_STATUS_ENABLED: AiEnrichmentStatusResponse = {
  enabled: true,
  provider: 'claude-code',
  credentialPresent: true,
  timeoutMs: 5000,
};

const MOCK_AUDIT_ENTRY: LlmAuditEntryResponse = {
  auditId: 'audit-1',
  sessionId: 'session-abc',
  timestamp: '2026-03-23T10:00:00Z',
  provider: 'claude-code',
  taskType: 'NAMING',
  sanitizationVersion: '1.0',
  payloadHash: 'abcdef123456789012345678',
  promptTokensEstimate: 350,
  degraded: false,
  degradationReason: '',
};

const MOCK_ENRICH_RESPONSE: AiEnrichmentResponse = {
  requestId: 'req-1',
  degraded: false,
  degradationReason: '',
  suggestions: { 'MyController.doSave': 'saveEntity' },
  tokensUsed: 420,
  provider: 'claude-code',
};

function buildApiSpy(
  statusResponse: AiEnrichmentStatusResponse,
  auditEntries: LlmAuditEntryResponse[],
): jasmine.SpyObj<AiEnrichmentApiService> {
  const spy = jasmine.createSpyObj<AiEnrichmentApiService>('AiEnrichmentApiService', [
    'getStatus',
    'getAuditLog',
    'enrich',
  ]);
  spy.getStatus.and.returnValue(of(statusResponse));
  spy.getAuditLog.and.returnValue(of(auditEntries));
  spy.enrich.and.returnValue(of(MOCK_ENRICH_RESPONSE));
  return spy;
}

async function createFixture(
  statusResponse: AiEnrichmentStatusResponse,
  auditEntries: LlmAuditEntryResponse[],
): Promise<{ fixture: ComponentFixture<AiEnrichmentViewComponent>; apiSpy: jasmine.SpyObj<AiEnrichmentApiService> }> {
  const apiSpy = buildApiSpy(statusResponse, auditEntries);

  await TestBed.configureTestingModule({
    imports: [AiEnrichmentViewComponent],
    providers: [{ provide: AiEnrichmentApiService, useValue: apiSpy }],
  }).compileComponents();

  const fixture = TestBed.createComponent(AiEnrichmentViewComponent);
  fixture.componentRef.setInput('sessionId', 'session-abc');
  fixture.detectChanges();

  return { fixture, apiSpy };
}

describe('AiEnrichmentViewComponent', () => {

  it('should display disabled status when AI is disabled', async () => {
    const { fixture } = await createFixture(MOCK_STATUS_DISABLED, []);

    const statusRow = fixture.debugElement.query(By.css('.status-row'));
    expect(statusRow).toBeTruthy();
    expect(statusRow.nativeElement.textContent).toContain('Desactive');

    const enrichBtn = fixture.debugElement.query(By.css('.enrich-btn'));
    expect(enrichBtn.nativeElement.disabled).toBeTrue();
  });

  it('should show confirm modal on first enrich click when no audit entries exist', async () => {
    const { fixture } = await createFixture(MOCK_STATUS_ENABLED, []);

    let modal = fixture.debugElement.query(By.css('.confirm-overlay'));
    expect(modal).toBeNull();

    const enrichBtn = fixture.debugElement.query(By.css('.enrich-btn'));
    enrichBtn.nativeElement.click();
    fixture.detectChanges();

    modal = fixture.debugElement.query(By.css('.confirm-overlay'));
    expect(modal).toBeTruthy();
    expect(modal.nativeElement.textContent).toContain('Confirmer l\'envoi au fournisseur IA');
  });

  it('should not show confirm modal on subsequent enrich when audit entries already exist', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);

    const enrichBtn = fixture.debugElement.query(By.css('.enrich-btn'));
    enrichBtn.nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.css('.confirm-overlay'));
    expect(modal).toBeNull();

    expect(apiSpy.enrich).toHaveBeenCalledWith('session-abc');
  });

  it('should display audit table when entries are present', async () => {
    const { fixture } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);

    const table = fixture.debugElement.query(By.css('.audit-table'));
    expect(table).toBeTruthy();

    const rows = fixture.debugElement.queryAll(By.css('.audit-table tbody tr'));
    expect(rows.length).toBe(1);

    const cells = rows[0].queryAll(By.css('td'));
    expect(cells[1].nativeElement.textContent.trim()).toBe('claude-code');
    expect(cells[2].nativeElement.textContent.trim()).toBe('NAMING');
    expect(cells[3].nativeElement.textContent.trim()).toBe('350');

    const badge = rows[0].query(By.css('.badge-nominal'));
    expect(badge).toBeTruthy();
  });

  it('should truncate payload hash to 12 chars followed by ellipsis in display', async () => {
    const { fixture } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);

    const hashCell = fixture.debugElement.query(By.css('.hash-cell'));
    expect(hashCell).toBeTruthy();

    const displayed = hashCell.nativeElement.textContent.trim();
    // "abcdef123456..." — first 12 chars of the hash + ellipsis
    expect(displayed).toBe('abcdef123456...');
    expect(hashCell.nativeElement.title).toBe(MOCK_AUDIT_ENTRY.payloadHash);
  });

  it('should close confirm modal and launch enrich when confirmAndEnrich is called', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, []);

    const enrichBtn = fixture.debugElement.query(By.css('.enrich-btn'));
    enrichBtn.nativeElement.click();
    fixture.detectChanges();

    const confirmBtn = fixture.debugElement.query(By.css('.btn-confirm'));
    expect(confirmBtn).toBeTruthy();
    confirmBtn.nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.css('.confirm-overlay'));
    expect(modal).toBeNull();
    expect(apiSpy.enrich).toHaveBeenCalledWith('session-abc');
  });

  it('should close confirm modal without calling enrich when cancelEnrich is called', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, []);

    const enrichBtn = fixture.debugElement.query(By.css('.enrich-btn'));
    enrichBtn.nativeElement.click();
    fixture.detectChanges();

    const cancelBtn = fixture.debugElement.query(By.css('.btn-cancel'));
    cancelBtn.nativeElement.click();
    fixture.detectChanges();

    const modal = fixture.debugElement.query(By.css('.confirm-overlay'));
    expect(modal).toBeNull();
    expect(apiSpy.enrich).not.toHaveBeenCalled();
  });
});
