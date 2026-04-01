import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AiEnrichmentApiService } from '../../../core/services/ai-enrichment-api.service';
import {
  AiArtifactCoherenceResponse,
  AiArtifactRefineRequest,
  AiCodeGenerationResponse,
  AiGenerationStreamEvent,
  AiGeneratedArtifactCollectionResponse,
  AiGeneratedArtifactResponse,
  AiEnrichmentResponse,
  AiEnrichmentProvider,
  AiEnrichmentStatusResponse,
  ArtifactReviewResponse,
  ArtifactsResponse,
  CodeArtifactDto,
  LlmAuditEntryResponse,
  SanitizedSourcePreviewResponse,
} from '../../../core/models/analysis.model';
import { AiAuditLogPanelComponent } from './ai-audit-log-panel.component';
import { AiGenerationDiffComponent } from './ai-generation-diff.component';
import { AiPersistedArtifactsPanelComponent } from './ai-persisted-artifacts-panel.component';
import { AiSanitizedPreviewPanelComponent } from './ai-sanitized-preview-panel.component';

type ProviderChoice = {
  value: AiEnrichmentProvider;
  label: string;
};

const PROVIDER_CHOICES: ProviderChoice[] = [
  { value: 'claude-code', label: 'Claude API' },
  { value: 'openai-gpt54', label: 'OpenAI API' },
  { value: 'claude-code-cli', label: 'Claude CLI' },
  { value: 'openai-codex-cli', label: 'OpenAI Codex CLI' },
];

const DEFAULT_PROVIDER: AiEnrichmentProvider = 'claude-code';

/**
 * Vue "Analyse IA" - enrichissement LLM avec confirmation avant 1er envoi et audit trail.
 * JAS-029
 */
@Component({
  selector: 'jas-ai-enrichment-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AiAuditLogPanelComponent,
    AiGenerationDiffComponent,
    AiPersistedArtifactsPanelComponent,
    AiSanitizedPreviewPanelComponent,
  ],
  styles: [`
    :host {
      display: block;
    }
  `],
  template: `
    @if (status()) {
      <div class="status-row">
        <span class="status-dot" [class.active]="isEnabled()"></span>
        Enrichissement IA : {{ isEnabled() ? 'Active' : 'Desactive' }}
        @if (isEnabled()) {
          - backend {{ backendProviderLabel() }}
          @if (!status()!.credentialPresent) {
            <span class="credential-missing">Credential absent</span>
          }
        }
      </div>
      <div class="provider-panel" [class.disabled]="!isEnabled()">
        <div class="provider-panel-header">
          <span>Choix du LLM</span>
          <span>Backend courant : {{ backendProviderLabel() }}</span>
        </div>
        <label class="provider-panel-label">
          <span>Provider utilise pour les prochaines actions</span>
          <select
            class="provider-select"
            [value]="selectedProvider()"
            (change)="onProviderSelectionChange($event)"
            [disabled]="!isEnabled()"
          >
            @for (option of providerChoices; track option.value) {
              <option [value]="option.value">{{ option.label }}</option>
            }
          </select>
        </label>
        <div class="provider-meta">
          <span class="provider-pill">Action : {{ selectedProviderLabel() }}</span>
          @if (selectedProvider() !== backendProvider()) {
            <span>Different du backend courant</span>
          }
        </div>
      </div>
    }

    <button
      class="enrich-btn"
      (click)="requestEnrich()"
      [disabled]="!isEnabled() || isLoading()"
    >
      @if (isLoading()) { Enrichissement en cours... }
      @else { Enrichir avec l'IA }
    </button>

    @if (error()) {
      <div class="error-block" role="alert">{{ error() }}</div>
    }

    @if (lastResult()) {
      <div class="result-block" [class.degraded]="lastResult()!.degraded">
        @if (lastResult()!.degraded) {
          <p class="degraded-msg">Mode degrade : {{ lastResult()!.degradationReason }}</p>
        } @else {
          <p class="nominal-msg">Enrichissement nominal - {{ lastResult()!.tokensUsed }} tokens utilises</p>
          @if (suggestionEntries().length > 0) {
            <div class="suggestions-list">
              @for (entry of suggestionEntries(); track entry.key) {
                <div class="suggestion-item">
                  <div class="suggestion-handler">{{ entry.key }}</div>
                  <div class="suggestion-text">{{ entry.value }}</div>
                </div>
              }
            </div>
          } @else {
            <p class="no-suggestions">Aucune suggestion retournee par l'IA.</p>
          }
        }
      </div>
    }
    <jas-ai-audit-log-panel [auditLog]="auditLog()" />

    <hr class="section-divider" />

    <p class="audit-section-title">Revue IA de la migration</p>

    <button
      class="review-btn"
      (click)="launchReview()"
      [disabled]="!isEnabled() || isReviewLoading()"
    >
      @if (isReviewLoading()) { Revue en cours... }
      @else { Lancer une Revue IA }
    </button>

    @if (reviewError()) {
      <div class="error-block" role="alert">{{ reviewError() }}</div>
    }

    @if (hasReviewResult()) {
      @if (reviewResult()!.degraded) {
        <div class="result-block degraded">
          <p class="degraded-msg">Mode degrade : {{ reviewResult()!.degradationReason }}</p>
        </div>
      } @else {
        <div class="result-block">
          <div class="review-header">
            <span
              class="review-score-circle"
              [style.color]="scoreColor()"
              [style.border-color]="scoreColor()"
            >
              @if (reviewResult()!.migrationScore >= 0) {
                {{ reviewResult()!.migrationScore }}
              } @else {
                -
              }
            </span>
            <div class="review-header-text">
              <strong>Score de migration</strong>
              Fournisseur : {{ reviewResult()!.provider }}
            </div>
          </div>

          @if (artifactReviewEntries().length > 0) {
            <p class="review-section-label">Revue par artefact</p>
            @for (entry of artifactReviewEntries(); track entry.key) {
              <div class="artifact-review-item">
                <span class="artifact-type-badge">{{ entry.key }}</span>
                <div class="artifact-review-text">{{ entry.value }}</div>
              </div>
            }
          }

          @if (uncertainEntries().length > 0) {
            <p class="review-section-label">Reclassifications suggerees</p>
            @for (entry of uncertainEntries(); track entry.key) {
              <div class="uncertain-item">
                <span class="uncertain-rule-id">{{ entry.key }}</span>
                {{ entry.value }}
              </div>
            }
          }

          @if (reviewResult()!.globalSuggestions.length > 0) {
            <p class="review-section-label">Suggestions globales</p>
            @for (s of reviewResult()!.globalSuggestions; track s) {
              <div class="global-suggestion-item">{{ s }}</div>
            }
          }
        </div>
      }
    }

    <hr class="section-divider" />

    <p class="audit-section-title">Generation IA Spring Boot</p>

    <button
      class="generate-btn"
      (click)="launchGenerate()"
      [disabled]="!isEnabled() || isGenerateLoading()"
    >
      @if (isGenerateLoading()) { Generation en cours... }
      @else { Generer les classes Spring Boot }
    </button>

    @if (generationProgress()) {
      <div class="generation-progress">
        <div class="generation-progress-header">
          <span>{{ generationProgress()!.message }}</span>
          <span>{{ generationProgress()!.progress }}%</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" [style.width.%]="generationProgress()!.progress"></div>
        </div>
        <ul class="generation-log">
          <li>Etape : {{ generationProgress()!.stage }}</li>
          @if (generationProgress()!.provider) {
            <li>Fournisseur : {{ generationProgress()!.provider }}</li>
          }
          @if (generationProgress()!.tokensUsed !== undefined) {
            <li>Tokens utilises : {{ generationProgress()!.tokensUsed }}</li>
          }
        </ul>
      </div>
    }

    @if (generateError()) {
      <div class="error-block" role="alert">{{ generateError() }}</div>
    }

    @if (isGenerateLoading() && visibleGeneratedClassEntries().length > 0) {
      <div class="result-block">
        <p class="nominal-msg">Generation en cours - affichage incremental des artefacts.</p>
        <div style="margin-top: 0.75rem;">
          @for (entry of visibleGeneratedClassEntries(); track entry.key) {
            <div class="generated-class-item streaming">
              <div class="generated-class-header">
                <span class="artifact-type-badge">{{ entry.key }}</span>
              </div>
              <code class="generated-class-code">{{ entry.value }}</code>
            </div>
          }
        </div>
      </div>
    } @else if (isGenerateLoading()) {
      <div class="status-loading">En attente des premiers artefacts...</div>
    }

    @if (hasGenerateResult()) {
      @if (generateResult()!.degraded) {
        <div class="result-block degraded">
          <p class="degraded-msg">Mode degrade : {{ generateResult()!.degradationReason }}</p>
        </div>
      } @else {
        <div class="result-block">
          <p class="nominal-msg">Generation nominale - {{ generateResult()!.tokensUsed }} tokens utilises - {{ generateResult()!.provider }}</p>
          <button
            class="zip-export-btn"
            (click)="launchZipExport()"
            [disabled]="isZipExportLoading()"
          >
            @if (isZipExportLoading()) { Export ZIP... }
            @else { Exporter les classes IA en ZIP }
          </button>
          @if (zipExportError()) {
            <div class="zip-export-result error">{{ zipExportError() }}</div>
          }
          @if (zipExportMessage()) {
            <div class="zip-export-result ok">{{ zipExportMessage() }}</div>
          }
          @if (visibleGeneratedClassEntries().length > 0) {
            <div style="margin-top: 0.75rem;">
              @for (entry of visibleGeneratedClassEntries(); track entry.key) {
                <div class="generated-class-item">
                  <div class="generated-class-header">
                    <span class="artifact-type-badge">{{ entry.key }}</span>
                    <div class="generated-class-actions">
                      <button class="generated-class-btn" (click)="copyGeneratedClass(entry.key, entry.value)">Copier</button>
                      <button class="generated-class-btn" (click)="downloadGeneratedClass(entry.key, entry.value)">Telecharger</button>
                      <button class="generated-class-btn" (click)="toggleRefinePanel(entry.key)">Raffiner</button>
                      @if (resolveTemplateArtifact(entry.key)) {
                        <button class="generated-class-btn" (click)="toggleDiff(entry.key)">Diff template / IA</button>
                      }
                    </div>
                  </div>
                  @if (generatedFeedback()[entry.key]) {
                    <div class="generated-feedback" [class.error]="generatedFeedback()[entry.key]?.startsWith('Erreur')">
                      {{ generatedFeedback()[entry.key] }}
                    </div>
                  }
                  @if (activeRefineArtifact() === entry.key) {
                    <div class="refine-panel">
                      <textarea
                        [value]="refineInstructions()[entry.key] ?? ''"
                        (input)="updateRefineInstruction(entry.key, $any($event.target).value)"
                        placeholder="Precise l'evolution attendue pour ce tour de raffinement..."
                      ></textarea>
                      <div class="refine-actions">
                        <button
                          class="refine-btn"
                          (click)="launchRefine(entry.key, entry.value)"
                          [disabled]="isRefining(entry.key)"
                        >
                          @if (isRefining(entry.key)) { Raffinement... }
                          @else { Appliquer le raffinement }
                        </button>
                        <span class="refine-note">Tu peux relancer plusieurs tours sur le meme artefact.</span>
                      </div>
                      @if (refineErrors()[entry.key]) {
                        <div class="generated-feedback error">{{ refineErrors()[entry.key] }}</div>
                      }
                    </div>
                  }
                  <code class="generated-class-code">{{ entry.value }}</code>
                  @if (expandedDiffArtifact() === entry.key) {
                    @if (resolveTemplateArtifact(entry.key); as templateArtifact) {
                      <jas-ai-generation-diff
                        [templateLabel]="templateArtifact.className"
                        [generatedLabel]="entry.key"
                        [templateCode]="templateArtifact.content"
                        [generatedCode]="entry.value"
                      />
                    } @else {
                      <div class="generated-feedback">Aucun artefact template correspondant n'a ete trouve.</div>
                    }
                  }
                </div>
              }
            </div>
          } @else {
            <p class="no-suggestions">Aucune classe generee.</p>
          }
        </div>
      }
    }
    <jas-ai-persisted-artifacts-panel
      [persistedArtifactsLoading]="persistedArtifactsLoading()"
      [persistedArtifactsError]="persistedArtifactsError()"
      [persistedArtifactEntries]="persistedArtifactEntries()"
      [incompletePersistedArtifactCount]="incompletePersistedArtifactCount()"
      [activePersistedArtifactType]="activePersistedArtifactType()"
      [persistedArtifactVersions]="persistedArtifactVersions()"
      [persistedArtifactVersionsLoading]="persistedArtifactVersionsLoading()"
      [persistedArtifactVersionsError]="persistedArtifactVersionsError()"
      [persistedCoherenceLoading]="persistedCoherenceLoading()"
      [persistedCoherenceError]="persistedCoherenceError()"
      [persistedCoherence]="persistedCoherence()"
      [persistedCoherenceFindings]="persistedCoherenceFindings()"
      (reloadRequested)="reloadPersistedArtifacts()"
      (coherenceRequested)="verifyPersistedCoherence()"
      (toggleVersionsRequested)="togglePersistedArtifactVersions($event)"
    />

    <jas-ai-sanitized-preview-panel
      [isLoading]="isPreviewLoading()"
      [error]="previewError()"
      [previewResult]="previewResult()"
      (previewRequested)="launchPreview()"
    />

    @if (showConfirmModal()) {
      <div class="confirm-overlay" role="dialog" aria-modal="true">
        <div class="confirm-card">
          <h3>Confirmer l'envoi au fournisseur IA</h3>
          <p>Le code source sera sanitise avant envoi. Aucune donnee sensible ne transpirera.</p>
          <p>Backend courant : <strong>{{ backendProviderLabel() }}</strong></p>
          <p>Action cible : <strong>{{ selectedProviderLabel() }}</strong></p>
          <div class="confirm-actions">
            <button class="btn-cancel" (click)="cancelEnrich()">Annuler</button>
            <button class="btn-confirm" (click)="confirmAndEnrich()">Confirmer l'envoi</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class AiEnrichmentViewComponent implements OnInit {
  readonly sessionId = input.required<string>();
  readonly templateArtifacts = input<ArtifactsResponse | null>(null);

  private readonly aiApi = inject(AiEnrichmentApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly status = signal<AiEnrichmentStatusResponse | null>(null);
  protected readonly auditLog = signal<LlmAuditEntryResponse[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly lastResult = signal<AiEnrichmentResponse | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly showConfirmModal = signal(false);
  protected readonly providerChoices = PROVIDER_CHOICES;
  protected readonly selectedProvider = signal<AiEnrichmentProvider>(DEFAULT_PROVIDER);
  protected readonly providerSelectionLocked = signal(false);

  protected readonly isEnabled = computed(() => this.status()?.enabled ?? false);
  protected readonly backendProvider = computed(
    () => this.status()?.provider as AiEnrichmentProvider | 'none' | undefined
  );
  protected readonly backendProviderLabel = computed(() => this.providerLabel(this.backendProvider()));
  protected readonly selectedProviderLabel = computed(() => this.providerLabel(this.selectedProvider()));
  protected readonly hasAuditEntries = computed(() => this.auditLog().length > 0);
  protected readonly suggestionEntries = computed(() =>
    Object.entries(this.lastResult()?.suggestions ?? {}).map(([key, value]) => ({ key, value }))
  );

  protected readonly reviewResult = signal<ArtifactReviewResponse | null>(null);
  protected readonly isReviewLoading = signal(false);
  protected readonly reviewError = signal<string | null>(null);

  protected readonly artifactReviewEntries = computed(() =>
    Object.entries(this.reviewResult()?.artifactReviews ?? {}).map(([key, value]) => ({ key, value }))
  );
  protected readonly uncertainEntries = computed(() =>
    Object.entries(this.reviewResult()?.uncertainReclassifications ?? {}).map(([key, value]) => ({ key, value }))
  );
  protected readonly hasReviewResult = computed(() => this.reviewResult() !== null);
  protected readonly scoreColor = computed(() => {
    const score = this.reviewResult()?.migrationScore ?? -1;
    if (score < 0) return '#9ca3af';
    if (score >= 75) return '#10b981';
    if (score >= 50) return '#f59e0b';
    return '#ef4444';
  });

  protected readonly generateResult = signal<AiCodeGenerationResponse | null>(null);
  protected readonly isGenerateLoading = signal(false);
  protected readonly generateError = signal<string | null>(null);
  protected readonly generationProgress = signal<AiGenerationStreamEvent | null>(null);
  protected readonly zipExportMessage = signal<string | null>(null);
  protected readonly zipExportError = signal<string | null>(null);
  protected readonly isZipExportLoading = signal(false);
  protected readonly generatedFeedback = signal<Record<string, string>>({});
  protected readonly streamingGeneratedClasses = signal<Record<string, string>>({});
  protected readonly refineInstructions = signal<Record<string, string | undefined>>({});
  protected readonly refineErrors = signal<Record<string, string>>({});
  protected readonly refineLoading = signal<Record<string, boolean>>({});
  protected readonly activeRefineArtifact = signal<string | null>(null);
  protected readonly expandedDiffArtifact = signal<string | null>(null);
  protected readonly templateArtifactLookup = computed(() => this.buildTemplateArtifactLookup());
  protected readonly persistedArtifacts = signal<AiGeneratedArtifactCollectionResponse | null>(null);
  protected readonly persistedArtifactsLoading = signal(false);
  protected readonly persistedArtifactsError = signal<string | null>(null);
  protected readonly persistedArtifactVersions = signal<Record<string, AiGeneratedArtifactResponse[]>>({});
  protected readonly persistedArtifactVersionsLoading = signal<Record<string, boolean>>({});
  protected readonly persistedArtifactVersionsError = signal<string | null>(null);
  protected readonly activePersistedArtifactType = signal<string | null>(null);
  protected readonly persistedCoherence = signal<AiArtifactCoherenceResponse | null>(null);
  protected readonly persistedCoherenceLoading = signal(false);
  protected readonly persistedCoherenceError = signal<string | null>(null);

  protected readonly visibleGeneratedClassEntries = computed(() =>
    Object.entries({
      ...this.streamingGeneratedClasses(),
      ...(this.generateResult()?.generatedClasses ?? {}),
    }).map(([key, value]) => ({ key, value }))
  );
  protected readonly hasGenerateResult = computed(() => this.generateResult() !== null);
  protected readonly persistedArtifactEntries = computed(() =>
    this.persistedArtifacts()?.artifacts ?? []
  );
  protected readonly hasPersistedArtifacts = computed(() => this.persistedArtifactEntries().length > 0);
  protected readonly incompletePersistedArtifactCount = computed(() =>
    this.persistedArtifactEntries()
      .filter(artifact => artifact.implementationStatus === 'INCOMPLETE')
      .length
  );
  protected readonly hasIncompletePersistedArtifacts = computed(() =>
    this.incompletePersistedArtifactCount() > 0
  );
  protected readonly persistedCoherenceFindings = computed(() =>
    Object.entries(this.persistedCoherence()?.artifactFindings ?? {}).map(([key, value]) => ({ key, value }))
  );

  protected readonly previewResult = signal<SanitizedSourcePreviewResponse | null>(null);
  protected readonly isPreviewLoading = signal(false);
  protected readonly previewError = signal<string | null>(null);
  protected readonly hasPreviewResult = computed(() => this.previewResult() !== null);

  ngOnInit(): void {
    this.aiApi.getStatus().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: s => {
        this.status.set(s);
        this.syncSelectedProviderWithStatus(s.provider);
      },
      error: () => this.status.set(null),
    });

    this.loadAuditLog();
    this.reloadPersistedArtifacts();
  }

  private loadAuditLog(): void {
    this.aiApi.getAuditLog(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: entries => this.auditLog.set(entries),
      error: () => this.auditLog.set([]),
    });
  }

  protected reloadPersistedArtifacts(): void {
    if (typeof this.aiApi.getPersistedArtifacts !== 'function') {
      this.persistedArtifactsLoading.set(false);
      return;
    }

    this.persistedArtifactsLoading.set(true);
    this.persistedArtifactsError.set(null);

    this.aiApi.getPersistedArtifacts(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        this.persistedArtifacts.set(response);
        this.persistedArtifactsLoading.set(false);
      },
      error: err => {
        this.persistedArtifacts.set(null);
        this.persistedArtifactsLoading.set(false);
        this.persistedArtifactsError.set(err?.error?.message ?? 'Erreur lors du chargement des artefacts IA persistes.');
      },
    });
  }

  protected togglePersistedArtifactVersions(artifactType: string): void {
    if (this.activePersistedArtifactType() === artifactType) {
      this.activePersistedArtifactType.set(null);
      return;
    }

    this.activePersistedArtifactType.set(artifactType);
    if (this.persistedArtifactVersions()[artifactType]) {
      return;
    }

    this.loadPersistedArtifactVersions(artifactType);
  }

  protected isPersistedArtifactVersionsLoading(artifactType: string): boolean {
    return this.persistedArtifactVersionsLoading()[artifactType] ?? false;
  }

  protected isPersistedArtifactsLoading(): boolean {
    return this.persistedArtifactsLoading();
  }

  protected isPersistedCoherenceLoading(): boolean {
    return this.persistedCoherenceLoading();
  }

  protected getPersistedArtifactVersionsFor(artifactType: string): AiGeneratedArtifactResponse[] {
    return this.persistedArtifactVersions()[artifactType] ?? [];
  }

  protected verifyPersistedCoherence(): void {
    if (typeof this.aiApi.verifyPersistedArtifactCoherence !== 'function') {
      this.persistedCoherenceLoading.set(false);
      return;
    }

    if (this.isPersistedCoherenceLoading() || !this.hasPersistedArtifacts()) {
      return;
    }

    this.persistedCoherenceLoading.set(true);
    this.persistedCoherenceError.set(null);

    this.aiApi.verifyPersistedArtifactCoherence(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.persistedCoherence.set(result);
        this.persistedCoherenceLoading.set(false);
      },
      error: err => {
        this.persistedCoherence.set(null);
        this.persistedCoherenceLoading.set(false);
        this.persistedCoherenceError.set(err?.error?.message ?? 'Erreur lors de la verification de coherence.');
      },
    });
  }

  private loadPersistedArtifactVersions(artifactType: string): void {
    if (typeof this.aiApi.getPersistedArtifactVersions !== 'function') {
      this.persistedArtifactVersionsLoading.update(current => ({
        ...current,
        [artifactType]: false,
      }));
      return;
    }

    this.persistedArtifactVersionsLoading.update(current => ({
      ...current,
      [artifactType]: true,
    }));
    this.persistedArtifactVersionsError.set(null);

    this.aiApi.getPersistedArtifactVersions(this.sessionId(), artifactType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: response => {
          this.persistedArtifactVersions.update(current => ({
            ...current,
            [artifactType]: response.artifacts,
          }));
          this.persistedArtifactVersionsLoading.update(current => ({
            ...current,
            [artifactType]: false,
          }));
        },
        error: err => {
          this.persistedArtifactVersionsLoading.update(current => ({
            ...current,
            [artifactType]: false,
          }));
          this.persistedArtifactVersionsError.set(err?.error?.message ?? 'Erreur lors du chargement des versions.');
        },
      });
  }

  protected onProviderSelectionChange(event: Event): void {
    const target = event.target as HTMLSelectElement | null;
    const nextProvider = this.normalizeProviderSelection(target?.value);

    if (nextProvider) {
      this.selectedProvider.set(nextProvider);
      this.providerSelectionLocked.set(true);
    }
  }

  protected requestEnrich(): void {
    if (!this.isEnabled() || this.isLoading()) {
      return;
    }
    if (!this.hasAuditEntries()) {
      this.showConfirmModal.set(true);
    } else {
      this.launchEnrich();
    }
  }

  protected confirmAndEnrich(): void {
    this.showConfirmModal.set(false);
    this.launchEnrich();
  }

  protected cancelEnrich(): void {
    this.showConfirmModal.set(false);
  }

  private syncSelectedProviderWithStatus(provider: string | undefined): void {
    if (this.providerSelectionLocked()) {
      return;
    }

    this.selectedProvider.set(this.normalizeProviderSelection(provider) ?? DEFAULT_PROVIDER);
  }

  private clearGenerationState(): void {
    this.generateResult.set(null);
    this.generateError.set(null);
    this.generationProgress.set(null);
    this.zipExportMessage.set(null);
    this.zipExportError.set(null);
    this.isZipExportLoading.set(false);
    this.generatedFeedback.set({});
    this.streamingGeneratedClasses.set({});
    this.refineInstructions.set({});
    this.refineErrors.set({});
    this.refineLoading.set({});
    this.activeRefineArtifact.set(null);
    this.expandedDiffArtifact.set(null);
    this.persistedCoherence.set(null);
    this.persistedCoherenceError.set(null);
    this.persistedArtifactVersions.set({});
    this.persistedArtifactVersionsLoading.set({});
    this.persistedArtifactVersionsError.set(null);
    this.activePersistedArtifactType.set(null);
  }

  private launchEnrich(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.aiApi.enrich(this.sessionId(), 'NAMING', this.selectedProvider()).subscribe({
      next: result => {
        this.isLoading.set(false);
        this.lastResult.set(result);
        this.loadAuditLog();
      },
      error: err => {
        this.isLoading.set(false);
        this.error.set(err?.error?.message ?? 'Erreur lors de l\'enrichissement IA.');
      },
    });
  }

  protected launchReview(): void {
    if (!this.isEnabled() || this.isReviewLoading()) return;
    this.isReviewLoading.set(true);
    this.reviewError.set(null);
    this.aiApi.review(this.sessionId(), this.selectedProvider()).subscribe({
      next: result => {
        this.isReviewLoading.set(false);
        this.reviewResult.set(result);
      },
      error: err => {
        this.isReviewLoading.set(false);
        this.reviewError.set(err?.error?.message ?? 'Erreur lors de la revue IA.');
      },
    });
  }

  protected launchGenerate(): void {
    if (!this.isEnabled() || this.isGenerateLoading()) {
      return;
    }

    this.isGenerateLoading.set(true);
    this.clearGenerationState();

    this.aiApi.generateStream(this.sessionId(), this.selectedProvider()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: event => this.handleGenerationEvent(event),
      error: err => {
        this.isGenerateLoading.set(false);
        this.generateError.set(err?.error?.message ?? 'Erreur lors de la generation IA.');
        this.generationProgress.set({
          stage: 'error',
          message: 'Generation IA interrompue',
          progress: 100,
          error: err?.error?.message ?? 'Erreur lors de la generation IA.',
        });
      },
      complete: () => {
        this.isGenerateLoading.set(false);
      },
    });
  }

  protected toggleRefinePanel(artifactKey: string): void {
    this.activeRefineArtifact.update(current => current === artifactKey ? null : artifactKey);
    this.setRefineError(artifactKey, null);
  }

  protected updateRefineInstruction(artifactKey: string, instruction: string): void {
    this.refineInstructions.update(current => ({
      ...current,
      [artifactKey]: instruction,
    }));
  }

  protected launchRefine(artifactKey: string, previousCode: string): void {
    if (this.isRefining(artifactKey)) {
      return;
    }

    const instruction = (this.refineInstructions()[artifactKey] ?? '').trim();
    if (!instruction) {
      this.setRefineError(artifactKey, 'Ajoute une instruction de raffinement.');
      return;
    }

    this.setRefineLoading(artifactKey, true);
    this.setRefineError(artifactKey, null);

    const request: AiArtifactRefineRequest = {
      artifactType: artifactKey,
      instruction,
      previousCode,
    };

    this.aiApi.refineArtifact(this.sessionId(), request, this.selectedProvider()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.applyGeneratedClasses(result.generatedClasses);
        this.setGeneratedFeedback(artifactKey, `Artefact raffine avec ${result.provider}`);
        this.setRefineLoading(artifactKey, false);
      },
      error: err => {
        this.setRefineLoading(artifactKey, false);
        this.setRefineError(artifactKey, err?.error?.message ?? 'Le raffinement IA n\'est pas disponible sur ce backend.');
      },
    });
  }

  protected copyGeneratedClass(artifactKey: string, content: string): void {
    const copied = this.copyTextToClipboard(content);
    this.setGeneratedFeedback(
      artifactKey,
      copied ? 'Code copie dans le presse-papiers' : 'Erreur lors de la copie du code',
    );
  }

  protected downloadGeneratedClass(artifactKey: string, content: string): void {
    const fileName = `${artifactKey}.java`;
    try {
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = fileName;
      anchor.click();
      URL.revokeObjectURL(url);
      this.setGeneratedFeedback(artifactKey, `Fichier ${fileName} telecharge`);
    } catch {
      this.setGeneratedFeedback(artifactKey, 'Erreur lors du telechargement du fichier');
    }
  }

  protected toggleDiff(artifactKey: string): void {
    this.expandedDiffArtifact.update(current => current === artifactKey ? null : artifactKey);
  }

  protected resolveTemplateArtifact(artifactKey: string): CodeArtifactDto | null {
    return this.templateArtifactLookup().get(this.normalizeArtifactKey(artifactKey)) ?? null;
  }

  protected launchZipExport(): void {
    if (this.isZipExportLoading() || !this.hasGenerateResult()) {
      return;
    }

    this.isZipExportLoading.set(true);
    this.zipExportError.set(null);
    this.zipExportMessage.set(null);

    this.aiApi.exportGeneratedZip(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        this.downloadZip(blob);
        this.zipExportMessage.set(`Archive ZIP telechargee pour ${this.sessionId()}`);
        this.isZipExportLoading.set(false);
      },
      error: err => {
        this.isZipExportLoading.set(false);
        this.zipExportError.set(err?.error?.message ?? 'L\'export ZIP IA n\'est pas disponible sur ce backend.');
      },
    });
  }

  private downloadZip(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `ai-artifacts-${this.sessionId()}.zip`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  protected isRefining(artifactKey: string): boolean {
    return this.refineLoading()[artifactKey] ?? false;
  }

  protected launchPreview(): void {
    if (this.isPreviewLoading()) return;
    this.isPreviewLoading.set(true);
    this.previewError.set(null);
    this.aiApi.previewSanitized(this.sessionId()).subscribe({
      next: result => {
        this.isPreviewLoading.set(false);
        this.previewResult.set(result);
      },
      error: err => {
        this.isPreviewLoading.set(false);
        this.previewError.set(err?.error?.message ?? 'Erreur lors de la previsualisation.');
      },
    });
  }

  private buildTemplateArtifactLookup(): Map<string, CodeArtifactDto> {
    const lookup = new Map<string, CodeArtifactDto>();
    for (const artifact of this.templateArtifacts()?.artifacts ?? []) {
      lookup.set(this.normalizeArtifactKey(artifact.className), artifact);
      lookup.set(this.normalizeArtifactKey(artifact.artifactId), artifact);
    }
    return lookup;
  }

  private handleGenerationEvent(event: AiGenerationStreamEvent): void {
    this.generationProgress.set(event);

    if (event.stage === 'streaming' && event.artifactKey) {
      const nextChunk = event.chunk ?? event.generatedClasses?.[event.artifactKey] ?? '';
      if (nextChunk) {
        this.streamingGeneratedClasses.update(current => ({
          ...current,
          [event.artifactKey!]: nextChunk,
        }));
      }
    }

    if (event.stage === 'complete') {
      this.generateResult.set({
        requestId: `stream-${this.sessionId()}`,
        degraded: event.degraded ?? false,
        degradationReason: event.error ?? '',
        generatedClasses: event.generatedClasses ?? {},
        tokensUsed: event.tokensUsed ?? 0,
        provider: event.provider ?? this.selectedProvider() ?? this.status()?.provider ?? 'unknown',
      });
      this.streamingGeneratedClasses.set(event.generatedClasses ?? this.streamingGeneratedClasses());
      this.isGenerateLoading.set(false);
      this.reloadPersistedArtifacts();
      return;
    }

    if (event.stage === 'error') {
      this.generateError.set(event.error ?? event.message);
      this.isGenerateLoading.set(false);
    }
  }

  private applyGeneratedClasses(nextClasses: Record<string, string>): void {
    this.streamingGeneratedClasses.update(current => ({
      ...current,
      ...nextClasses,
    }));

    this.generateResult.update(result => {
      if (!result) {
        return result;
      }

      return {
        ...result,
        generatedClasses: {
          ...result.generatedClasses,
          ...nextClasses,
        },
      };
    });
  }

  private copyTextToClipboard(content: string): boolean {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(content).catch(() => undefined);
      return true;
    }

    try {
      const textarea = document.createElement('textarea');
      textarea.value = content;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      return true;
    } catch {
      return false;
    }
  }

  private setGeneratedFeedback(artifactKey: string, message: string): void {
    this.generatedFeedback.update(current => ({
      ...current,
      [artifactKey]: message,
    }));
  }

  private setRefineError(artifactKey: string, message: string | null): void {
    this.refineErrors.update(current => {
      const next = { ...current };
      if (message === null) {
        delete next[artifactKey];
      } else {
        next[artifactKey] = message;
      }
      return next;
    });
  }

  private setRefineLoading(artifactKey: string, isLoading: boolean): void {
    this.refineLoading.update(current => ({
      ...current,
      [artifactKey]: isLoading,
    }));
  }

  private normalizeProviderSelection(value: string | undefined | null): AiEnrichmentProvider | null {
    if (
      value === 'claude-code'
      || value === 'openai-gpt54'
      || value === 'claude-code-cli'
      || value === 'openai-codex-cli'
    ) {
      return value;
    }

    return null;
  }

  private providerLabel(value: string | undefined | null): string {
    switch (value) {
      case 'claude-code':
        return 'Claude API';
      case 'openai-gpt54':
        return 'OpenAI API';
      case 'claude-code-cli':
        return 'Claude CLI';
      case 'openai-codex-cli':
        return 'OpenAI Codex CLI';
      case 'none':
        return 'Aucun provider';
      default:
        return 'Provider inconnu';
    }
  }

  private normalizeArtifactKey(value: string): string {
    return value.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-');
  }
}
