import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { from, of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { AiEnrichmentViewComponent } from './ai-enrichment-view.component';
import { AiEnrichmentApiService } from '../../../core/services/ai-enrichment-api.service';
import {
  AiArtifactCoherenceResponse,
  AiGenerationStreamEvent,
  AiGeneratedArtifactCollectionResponse,
  AiEnrichmentResponse,
  AiEnrichmentStatusResponse,
  ArtifactsResponse,
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

const MOCK_TEMPLATE_ARTIFACTS: ArtifactsResponse = {
  controllerRef: 'MyController',
  warnings: [],
  artifacts: [
    {
      artifactId: 'MyControllerUseCase',
      type: 'USE_CASE',
      lotNumber: 1,
      className: 'MyControllerUseCase',
      content: 'class MyControllerUseCase {\n  void run() {}\n}',
      transitionalBridge: false,
      generationWarnings: [],
      generationStatus: 'OK',
    },
  ],
};

const MOCK_GENERATION_STREAM: AiGenerationStreamEvent[] = [
  {
    stage: 'sanitizing',
    message: 'Sanitisation du code source',
    progress: 20,
  },
  {
    stage: 'complete',
    message: 'Generation terminee',
    progress: 100,
    generatedClasses: {
      MyControllerUseCase: 'class MyControllerUseCase {\n  void run() {}\n}',
    },
    tokensUsed: 120,
    provider: 'claude-code',
    degraded: false,
  },
];

const MOCK_PERSISTED_ARTIFACTS: AiGeneratedArtifactCollectionResponse = {
  sessionId: 'session-abc',
  artifacts: [
    {
      artifactType: 'USE_CASE',
      className: 'MyControllerUseCase',
      content: 'class MyControllerUseCase {\n  void run() {}\n}',
      versionNumber: 2,
      parentVersionId: 'artifact-1',
      requestId: 'req-persisted',
      provider: 'claude-code',
      originTask: 'SPRING_BOOT_GENERATION',
      createdAt: '2026-03-26T11:00:00Z',
      implementationStatus: 'READY',
      implementationWarning: null,
    },
  ],
};

const MOCK_INCOMPLETE_PERSISTED_ARTIFACTS: AiGeneratedArtifactCollectionResponse = {
  sessionId: 'session-abc',
  artifacts: [
    {
      artifactType: 'POLICY',
      className: 'MyControllerPolicy',
      content: 'class MyControllerPolicy {\n  // TODO: implementer\n  boolean isReady() { return false; }\n}',
      versionNumber: 3,
      parentVersionId: 'artifact-2',
      requestId: 'req-todo',
      provider: 'claude-code',
      originTask: 'SPRING_BOOT_GENERATION',
      createdAt: '2026-03-26T12:00:00Z',
      implementationStatus: 'INCOMPLETE',
      implementationWarning: 'Artefact IA incomplet detecte : placeholder d\'implementation residuel.',
    },
  ],
};

const MOCK_PERSISTED_COHERENCE: AiArtifactCoherenceResponse = {
  requestId: 'req-coherence',
  degraded: false,
  degradationReason: '',
  summary: 'Coherence satisfaisante.',
  artifactFindings: { USE_CASE: 'OK' },
  globalFindings: ['Aucun conflit detecte.'],
  tokensUsed: 48,
  provider: 'claude-code',
};

function buildApiSpy(
  statusResponse: AiEnrichmentStatusResponse,
  auditEntries: LlmAuditEntryResponse[],
): {
  getStatus: ReturnType<typeof vi.fn>;
  getAuditLog: ReturnType<typeof vi.fn>;
  getPersistedArtifacts: ReturnType<typeof vi.fn>;
  getPersistedArtifactVersions: ReturnType<typeof vi.fn>;
  verifyPersistedArtifactCoherence: ReturnType<typeof vi.fn>;
  enrich: ReturnType<typeof vi.fn>;
  review: ReturnType<typeof vi.fn>;
  generateStream: ReturnType<typeof vi.fn>;
  refineArtifact: ReturnType<typeof vi.fn>;
  exportGeneratedZip: ReturnType<typeof vi.fn>;
} {
  return {
    getStatus: vi.fn().mockReturnValue(of(statusResponse)),
    getAuditLog: vi.fn().mockReturnValue(of(auditEntries)),
    getPersistedArtifacts: vi.fn().mockReturnValue(of(MOCK_PERSISTED_ARTIFACTS)),
    getPersistedArtifactVersions: vi.fn().mockReturnValue(of(MOCK_PERSISTED_ARTIFACTS)),
    verifyPersistedArtifactCoherence: vi.fn().mockReturnValue(of(MOCK_PERSISTED_COHERENCE)),
    enrich: vi.fn().mockReturnValue(of(MOCK_ENRICH_RESPONSE)),
    review: vi.fn().mockReturnValue(of({
      requestId: 'req-review',
      degraded: false,
      degradationReason: '',
      migrationScore: 84,
      artifactReviews: {},
      uncertainReclassifications: {},
      globalSuggestions: [],
      provider: 'claude-code',
    })),
    generateStream: vi.fn().mockReturnValue(from(MOCK_GENERATION_STREAM)),
    refineArtifact: vi.fn().mockReturnValue(of({
      requestId: 'req-refine',
      degraded: false,
      degradationReason: '',
      generatedClasses: {
        MyControllerUseCase: 'class MyControllerUseCase {\n  void runRefined() {}\n}',
      },
      tokensUsed: 150,
      provider: 'claude-code',
    })),
    exportGeneratedZip: vi.fn().mockReturnValue(of(new Blob(['zip'], { type: 'application/zip' }))),
  };
}

async function createFixture(
  statusResponse: AiEnrichmentStatusResponse,
  auditEntries: LlmAuditEntryResponse[],
): Promise<{
  fixture: ComponentFixture<AiEnrichmentViewComponent>;
  apiSpy: {
    getStatus: ReturnType<typeof vi.fn>;
    getAuditLog: ReturnType<typeof vi.fn>;
    getPersistedArtifacts: ReturnType<typeof vi.fn>;
    getPersistedArtifactVersions: ReturnType<typeof vi.fn>;
    verifyPersistedArtifactCoherence: ReturnType<typeof vi.fn>;
    enrich: ReturnType<typeof vi.fn>;
    review: ReturnType<typeof vi.fn>;
    generateStream: ReturnType<typeof vi.fn>;
    refineArtifact: ReturnType<typeof vi.fn>;
    exportGeneratedZip: ReturnType<typeof vi.fn>;
  };
}> {
  const apiSpy = buildApiSpy(statusResponse, auditEntries);

  await TestBed.configureTestingModule({
    imports: [AiEnrichmentViewComponent],
    providers: [{ provide: AiEnrichmentApiService, useValue: apiSpy }],
  }).compileComponents();

  const fixture = TestBed.createComponent(AiEnrichmentViewComponent);
  fixture.componentRef.setInput('sessionId', 'session-abc');
  fixture.componentRef.setInput('templateArtifacts', MOCK_TEMPLATE_ARTIFACTS);
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
    expect(enrichBtn.nativeElement.disabled).toBe(true);
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

    expect(apiSpy.enrich).toHaveBeenCalledWith('session-abc', 'NAMING', 'claude-code');
  });

  it('should let the user choose a provider and propagate it to the review action', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);

    const select = fixture.debugElement.query(By.css('.provider-select')).nativeElement as HTMLSelectElement;
    select.value = 'openai-codex-cli';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Action : OpenAI Codex CLI');
    expect(fixture.nativeElement.textContent).toContain('Different du backend courant');

    const reviewBtn = fixture.debugElement.query(By.css('.review-btn')).nativeElement as HTMLButtonElement;
    reviewBtn.click();
    fixture.detectChanges();

    expect(apiSpy.review).toHaveBeenCalledWith('session-abc', 'openai-codex-cli');
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

  it('should stream generation progress incrementally, reset state between runs and allow copy/download/refine actions', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);
    const firstStream = new Subject<AiGenerationStreamEvent>();
    const secondStream = new Subject<AiGenerationStreamEvent>();
    apiSpy.generateStream.mockReturnValue(firstStream.asObservable());

    const clipboardWrite = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: clipboardWrite },
      configurable: true,
    });
    const createObjectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock');
    const revokeObjectUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const anchorClickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    const generateBtn = fixture.debugElement.query(By.css('.generate-btn')).nativeElement as HTMLButtonElement;
    generateBtn.click();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.status-loading'))).toBeTruthy();
    expect(apiSpy.generateStream).toHaveBeenCalledWith('session-abc', 'claude-code');

    firstStream.next({
      stage: 'streaming',
      message: 'Premier artefact transmis',
      progress: 60,
      artifactKey: 'MyControllerUseCase',
      chunk: 'class MyControllerUseCase {\n  void run() {}\n}',
      provider: 'claude-code',
    });
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.generation-progress'))).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Generation en cours - affichage incremental des artefacts.');
    expect(fixture.nativeElement.textContent).toContain('MyControllerUseCase');
    expect(fixture.nativeElement.textContent).toContain('run()');

    firstStream.next({
      stage: 'complete',
      message: 'Generation terminee',
      progress: 100,
      generatedClasses: {
        MyControllerUseCase: 'class MyControllerUseCase {\n  void run() {}\n}',
      },
      tokensUsed: 120,
      provider: 'claude-code',
      degraded: false,
    });
    firstStream.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Generation nominale');
    expect(apiSpy.getPersistedArtifacts).toHaveBeenCalledWith('session-abc');

    apiSpy.generateStream.mockReturnValue(secondStream.asObservable());
    generateBtn.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Generation nominale');
    expect(fixture.debugElement.queryAll(By.css('.generated-class-item.streaming')).length).toBe(0);

    secondStream.next({
      stage: 'streaming',
      message: 'Second artefact transmis',
      progress: 55,
      artifactKey: 'MyControllerUseCase',
      chunk: 'class MyControllerUseCase {\n  void runTwice() {}\n}',
      provider: 'claude-code',
    });
    fixture.detectChanges();

    const streamingCode = fixture.debugElement.query(By.css('.generated-class-code'));
    expect(streamingCode.nativeElement.textContent).toContain('runTwice()');

    secondStream.next({
      stage: 'complete',
      message: 'Generation terminee',
      progress: 100,
      generatedClasses: {
        MyControllerUseCase: 'class MyControllerUseCase {\n  void runTwice() {}\n}',
      },
      tokensUsed: 126,
      provider: 'claude-code',
      degraded: false,
    });
    secondStream.complete();
    fixture.detectChanges();

    const copyBtn = fixture.debugElement.query(By.css('.generated-class-btn'));
    copyBtn.nativeElement.click();
    fixture.detectChanges();
    expect(clipboardWrite).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Code copie dans le presse-papiers');

    const actionButtons = fixture.debugElement.queryAll(By.css('.generated-class-btn'));
    const downloadBtn = actionButtons[1];
    downloadBtn.nativeElement.click();
    fixture.detectChanges();
    expect(createObjectUrlSpy).toHaveBeenCalled();
    expect(anchorClickSpy).toHaveBeenCalled();
    expect(revokeObjectUrlSpy).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Fichier MyControllerUseCase.java telecharge');

    const refineBtn = actionButtons[2];
    refineBtn.nativeElement.click();
    fixture.detectChanges();

    const textarea = fixture.nativeElement.querySelector('.refine-panel textarea') as HTMLTextAreaElement;
    textarea.value = 'Ajoute une methode de validation';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const applyRefineBtn = fixture.nativeElement.querySelector('.refine-btn') as HTMLButtonElement;
    applyRefineBtn.click();
    fixture.detectChanges();

    expect(apiSpy.refineArtifact).toHaveBeenCalledWith(
      'session-abc',
      {
        artifactType: 'MyControllerUseCase',
        instruction: 'Ajoute une methode de validation',
        previousCode: 'class MyControllerUseCase {\n  void runTwice() {}\n}',
      },
      'claude-code',
    );
    expect(fixture.nativeElement.textContent).toContain('raffine');
    expect(fixture.nativeElement.textContent).toContain('runRefined');

    const diffBtn = actionButtons[3];
    diffBtn.nativeElement.click();
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('jas-ai-generation-diff'))).toBeTruthy();

    anchorClickSpy.mockRestore();
    createObjectUrlSpy.mockRestore();
    revokeObjectUrlSpy.mockRestore();
  });

  it('should load persisted artifacts, versions and coherence data', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);

    expect(apiSpy.getPersistedArtifacts).toHaveBeenCalledWith('session-abc');
    expect(fixture.nativeElement.textContent).toContain('Artefacts IA persistes');
    expect(fixture.nativeElement.textContent).toContain('MyControllerUseCase');

    const versionsBtn = fixture.debugElement.query(By.css('.persisted-btn.tiny'));
    versionsBtn.nativeElement.click();
    fixture.detectChanges();

    expect(apiSpy.getPersistedArtifactVersions).toHaveBeenCalledWith('session-abc', 'USE_CASE');
    expect(fixture.nativeElement.textContent).toContain('v2');
    expect(fixture.nativeElement.textContent).toContain('req-persisted');

    const coherenceBtn = fixture.debugElement.query(By.css('.persisted-btn.secondary'));
    coherenceBtn.nativeElement.click();
    fixture.detectChanges();

    expect(apiSpy.verifyPersistedArtifactCoherence).toHaveBeenCalledWith('session-abc');
    expect(fixture.nativeElement.textContent).toContain('Coherence verifiee');
    expect(fixture.nativeElement.textContent).toContain('Aucun conflit detecte.');
  });

  it('should highlight incomplete persisted artifacts', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);
    apiSpy.getPersistedArtifacts.mockReturnValue(of(MOCK_INCOMPLETE_PERSISTED_ARTIFACTS));
    apiSpy.getPersistedArtifactVersions.mockReturnValue(of(MOCK_INCOMPLETE_PERSISTED_ARTIFACTS));

    const refreshBtn = fixture.debugElement.query(By.css('.persisted-btn'));
    refreshBtn.nativeElement.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('contiennent encore des placeholders d\'implementation');
    expect(fixture.nativeElement.textContent).toContain('Incomplet');
    expect(fixture.nativeElement.textContent).toContain('placeholder d\'implementation residuel');

    const versionsBtn = fixture.debugElement.query(By.css('.persisted-btn.tiny'));
    versionsBtn.nativeElement.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('req-todo');
    expect(fixture.nativeElement.textContent).toContain('Incomplet');
  });

  it('should export the generated ZIP as a blob when the button is clicked', async () => {
    const { fixture, apiSpy } = await createFixture(MOCK_STATUS_ENABLED, [MOCK_AUDIT_ENTRY]);
    const createObjectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:zip');
    const revokeObjectUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const anchorClickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    const generateBtn = fixture.debugElement.query(By.css('.generate-btn')).nativeElement as HTMLButtonElement;
    generateBtn.click();
    fixture.detectChanges();

    const zipBtn = fixture.debugElement.query(By.css('.zip-export-btn')).nativeElement as HTMLButtonElement;
    zipBtn.click();
    fixture.detectChanges();

    expect(apiSpy.exportGeneratedZip).toHaveBeenCalledWith('session-abc');
    expect(createObjectUrlSpy).toHaveBeenCalled();
    expect(anchorClickSpy).toHaveBeenCalled();
    expect(revokeObjectUrlSpy).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Archive ZIP telechargee pour session-abc');

    anchorClickSpy.mockRestore();
    createObjectUrlSpy.mockRestore();
    revokeObjectUrlSpy.mockRestore();
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
    expect(apiSpy.enrich).toHaveBeenCalledWith('session-abc', 'NAMING', 'claude-code');
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
