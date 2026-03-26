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
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AiEnrichmentApiService } from '../../../core/services/ai-enrichment-api.service';
import {
  AiCodeGenerationResponse,
  AiEnrichmentResponse,
  AiEnrichmentStatusResponse,
  ArtifactReviewResponse,
  LlmAuditEntryResponse,
  SanitizedSourcePreviewResponse,
} from '../../../core/models/analysis.model';

/**
 * Vue "Analyse IA" — enrichissement LLM avec confirmation avant 1er envoi et audit trail.
 * JAS-029
 */
@Component({
  selector: 'jas-ai-enrichment-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  styles: `
    :host {
      display: block;
    }

    .status-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.9rem;
      margin-bottom: 1rem;
      color: #374151;
    }

    .status-dot {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #9ca3af;
      flex-shrink: 0;
    }

    .status-dot.active {
      background: #10b981;
    }

    .credential-missing {
      color: #b45309;
      font-size: 0.82rem;
    }

    .enrich-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.55rem 1.4rem;
      border: none;
      border-radius: 999px;
      background: #122338;
      color: white;
      font-weight: 700;
      font-size: 0.88rem;
      cursor: pointer;
      transition: opacity 0.2s;
      margin-bottom: 1.25rem;
    }

    .enrich-btn:hover:not(:disabled) {
      opacity: 0.85;
    }

    .enrich-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .error-block {
      padding: 0.7rem 1rem;
      border-radius: 10px;
      background: rgba(217, 95, 51, 0.06);
      border: 1px solid rgba(217, 95, 51, 0.25);
      color: #b94517;
      font-size: 0.85rem;
      margin-bottom: 1rem;
    }

    .result-block {
      padding: 0.7rem 1rem;
      border-radius: 10px;
      background: rgba(16, 185, 129, 0.06);
      border: 1px solid rgba(16, 185, 129, 0.25);
      margin-bottom: 1.25rem;
    }

    .result-block.degraded {
      background: rgba(245, 158, 11, 0.06);
      border-color: rgba(245, 158, 11, 0.3);
    }

    .nominal-msg {
      margin: 0;
      font-size: 0.85rem;
      color: #065f46;
    }

    .degraded-msg {
      margin: 0;
      font-size: 0.85rem;
      color: #92400e;
    }

    .confirm-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
    }

    .confirm-card {
      background: white;
      border-radius: 20px;
      padding: 2rem;
      width: min(480px, 100%);
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .confirm-card h3 {
      margin: 0;
      font-size: 1.05rem;
      font-weight: 700;
      color: #122338;
    }

    .confirm-card p {
      margin: 0;
      font-size: 0.88rem;
      color: #374151;
    }

    .confirm-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: flex-end;
      margin-top: 0.5rem;
    }

    .btn-cancel {
      padding: 0.5rem 1.2rem;
      border: 1px solid #d1d5db;
      border-radius: 999px;
      background: white;
      color: #374151;
      font-weight: 600;
      font-size: 0.88rem;
      cursor: pointer;
      transition: background 0.15s;
    }

    .btn-cancel:hover {
      background: #f9fafb;
    }

    .btn-confirm {
      padding: 0.5rem 1.4rem;
      border: none;
      border-radius: 999px;
      background: #122338;
      color: white;
      font-weight: 700;
      font-size: 0.88rem;
      cursor: pointer;
      transition: opacity 0.2s;
    }

    .btn-confirm:hover {
      opacity: 0.85;
    }

    .audit-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.82rem;
      margin-top: 0.5rem;
    }

    .audit-table th {
      text-align: left;
      padding: 0.5rem 0.75rem;
      background: rgba(18, 35, 56, 0.04);
      border-bottom: 2px solid rgba(18, 35, 56, 0.1);
      font-weight: 600;
      color: #374151;
      white-space: nowrap;
    }

    .audit-table td {
      padding: 0.5rem 0.75rem;
      border-bottom: 1px solid rgba(18, 35, 56, 0.06);
      color: #4b5563;
      vertical-align: middle;
    }

    .audit-table tr:last-child td {
      border-bottom: none;
    }

    .degraded-row td {
      background: rgba(239, 68, 68, 0.04);
    }

    .badge-nominal {
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      background: rgba(16, 185, 129, 0.12);
      color: #065f46;
      font-weight: 600;
      font-size: 0.78rem;
    }

    .badge-degraded {
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      background: rgba(245, 158, 11, 0.12);
      color: #92400e;
      font-weight: 600;
      font-size: 0.78rem;
    }

    .hash-cell {
      font-family: monospace;
      font-size: 0.8rem;
      max-width: 110px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: #6b7280;
    }

    .no-audit {
      margin: 0.5rem 0 0;
      font-size: 0.85rem;
      color: #9ca3af;
      font-style: italic;
    }

    .suggestions-list {
      margin: 0.75rem 0 0;
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
    }

    .suggestion-item {
      background: white;
      border: 1px solid rgba(16, 185, 129, 0.2);
      border-radius: 8px;
      padding: 0.6rem 0.85rem;
    }

    .suggestion-handler {
      font-weight: 700;
      font-size: 0.82rem;
      color: #065f46;
      font-family: monospace;
      margin-bottom: 0.3rem;
    }

    .suggestion-text {
      font-size: 0.84rem;
      color: #374151;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .no-suggestions {
      font-size: 0.83rem;
      color: #9ca3af;
      font-style: italic;
      margin: 0.5rem 0 0;
    }

    .audit-section-title {
      margin: 1.25rem 0 0.5rem;
      font-size: 0.9rem;
      font-weight: 700;
      color: #122338;
      letter-spacing: 0.03em;
    }

    .review-score-circle {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 52px;
      height: 52px;
      border-radius: 50%;
      font-size: 1.1rem;
      font-weight: 800;
      border: 3px solid;
      margin-right: 1rem;
      flex-shrink: 0;
    }

    .review-header {
      display: flex;
      align-items: center;
      margin-bottom: 1rem;
    }

    .review-header-text {
      font-size: 0.88rem;
      color: #374151;
    }

    .review-header-text strong {
      display: block;
      font-size: 0.95rem;
      color: #122338;
      margin-bottom: 0.2rem;
    }

    .review-section-label {
      font-size: 0.78rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: #6b7280;
      margin: 1rem 0 0.4rem;
    }

    .artifact-review-item {
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 0.6rem 0.85rem;
      margin-bottom: 0.5rem;
    }

    .artifact-type-badge {
      display: inline-block;
      padding: 0.15rem 0.55rem;
      border-radius: 999px;
      background: rgba(18, 35, 56, 0.08);
      color: #122338;
      font-weight: 700;
      font-size: 0.75rem;
      font-family: monospace;
      margin-bottom: 0.35rem;
    }

    .artifact-review-text {
      font-size: 0.83rem;
      color: #374151;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .uncertain-item {
      background: rgba(245, 158, 11, 0.05);
      border: 1px solid rgba(245, 158, 11, 0.25);
      border-radius: 8px;
      padding: 0.5rem 0.85rem;
      margin-bottom: 0.4rem;
      font-size: 0.83rem;
    }

    .uncertain-rule-id {
      font-weight: 700;
      font-family: monospace;
      color: #92400e;
      margin-right: 0.5rem;
    }

    .global-suggestion-item {
      padding: 0.4rem 0;
      font-size: 0.84rem;
      color: #374151;
      border-bottom: 1px dashed #e5e7eb;
    }

    .global-suggestion-item:last-child {
      border-bottom: none;
    }

    .review-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.5rem 1.2rem;
      border: 1px solid #122338;
      border-radius: 999px;
      background: white;
      color: #122338;
      font-weight: 700;
      font-size: 0.85rem;
      cursor: pointer;
      transition: background 0.15s;
      margin-bottom: 1.25rem;
    }

    .review-btn:hover:not(:disabled) {
      background: rgba(18, 35, 56, 0.06);
    }

    .review-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .section-divider {
      border: none;
      border-top: 1px solid #e5e7eb;
      margin: 1.5rem 0;
    }

    .generate-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.5rem 1.2rem;
      border: 1px solid #4f46e5;
      border-radius: 999px;
      background: white;
      color: #4f46e5;
      font-weight: 700;
      font-size: 0.85rem;
      cursor: pointer;
      transition: background 0.15s;
      margin-bottom: 1.25rem;
    }

    .generate-btn:hover:not(:disabled) {
      background: rgba(79, 70, 229, 0.06);
    }

    .generate-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .generated-class-item {
      margin-bottom: 1rem;
    }

    .generated-class-code {
      display: block;
      background: #1e1e2e;
      color: #cdd6f4;
      font-family: monospace;
      font-size: 0.78rem;
      padding: 1rem;
      border-radius: 8px;
      overflow-x: auto;
      white-space: pre;
      max-height: 320px;
      overflow-y: auto;
      margin-top: 0.4rem;
    }

    .preview-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.5rem 1.2rem;
      border: 1px solid #6b7280;
      border-radius: 999px;
      background: white;
      color: #374151;
      font-weight: 700;
      font-size: 0.85rem;
      cursor: pointer;
      transition: background 0.15s;
      margin-bottom: 1.25rem;
    }

    .preview-btn:hover:not(:disabled) {
      background: #f9fafb;
    }

    .preview-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .sanitized-badge {
      display: inline-block;
      padding: 0.15rem 0.55rem;
      border-radius: 999px;
      font-weight: 600;
      font-size: 0.75rem;
      margin-left: 0.5rem;
    }

    .sanitized-badge.yes {
      background: rgba(16, 185, 129, 0.12);
      color: #065f46;
    }

    .sanitized-badge.no {
      background: rgba(245, 158, 11, 0.12);
      color: #92400e;
    }

    .source-meta {
      font-size: 0.82rem;
      color: #6b7280;
      margin: 0.4rem 0 0.75rem;
    }

    .source-code-block {
      display: block;
      background: #1e1e2e;
      color: #cdd6f4;
      font-family: monospace;
      font-size: 0.78rem;
      padding: 1rem;
      border-radius: 8px;
      overflow-x: auto;
      white-space: pre;
      max-height: 400px;
      overflow-y: auto;
    }
  `,
  template: `
    @if (status()) {
      <div class="status-row">
        <span class="status-dot" [class.active]="isEnabled()"></span>
        Enrichissement IA : {{ isEnabled() ? 'Active' : 'Desactive' }}
        @if (isEnabled()) {
          — {{ status()!.provider }}
          @if (!status()!.credentialPresent) {
            <span class="credential-missing">Credential absent</span>
          }
        }
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
          <p class="nominal-msg">Enrichissement nominal — {{ lastResult()!.tokensUsed }} tokens utilises</p>
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

    <p class="audit-section-title">Historique des appels LLM</p>

    @if (hasAuditEntries()) {
      <table class="audit-table">
        <thead>
          <tr>
            <th>Horodatage</th>
            <th>Fournisseur</th>
            <th>Tache</th>
            <th>Tokens</th>
            <th>Statut</th>
            <th>Hash</th>
          </tr>
        </thead>
        <tbody>
          @for (entry of auditLog(); track entry.auditId) {
            <tr [class.degraded-row]="entry.degraded">
              <td>{{ entry.timestamp | date:'dd/MM/yyyy HH:mm:ss' }}</td>
              <td>{{ entry.provider }}</td>
              <td>{{ entry.taskType }}</td>
              <td>{{ entry.promptTokensEstimate }}</td>
              <td>
                @if (entry.degraded) {
                  <span class="badge-degraded">Degrade</span>
                } @else {
                  <span class="badge-nominal">Nominal</span>
                }
              </td>
              <td class="hash-cell" [title]="entry.payloadHash">{{ entry.payloadHash.substring(0, 12) }}...</td>
            </tr>
          }
        </tbody>
      </table>
    } @else {
      <p class="no-audit">Aucun appel LLM enregistre pour cette session.</p>
    }

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
                —
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

    <p class="audit-section-title">Génération IA Spring Boot</p>

    <button
      class="generate-btn"
      (click)="launchGenerate()"
      [disabled]="!isEnabled() || isGenerateLoading()"
    >
      @if (isGenerateLoading()) { Génération en cours... }
      @else { Générer les classes Spring Boot }
    </button>

    @if (generateError()) {
      <div class="error-block" role="alert">{{ generateError() }}</div>
    }

    @if (hasGenerateResult()) {
      @if (generateResult()!.degraded) {
        <div class="result-block degraded">
          <p class="degraded-msg">Mode dégradé : {{ generateResult()!.degradationReason }}</p>
        </div>
      } @else {
        <div class="result-block">
          <p class="nominal-msg">Génération nominale — {{ generateResult()!.tokensUsed }} tokens utilisés — {{ generateResult()!.provider }}</p>
          @if (generatedClassEntries().length > 0) {
            <div style="margin-top: 0.75rem;">
              @for (entry of generatedClassEntries(); track entry.key) {
                <div class="generated-class-item">
                  <span class="artifact-type-badge">{{ entry.key }}</span>
                  <code class="generated-class-code">{{ entry.value }}</code>
                </div>
              }
            </div>
          } @else {
            <p class="no-suggestions">Aucune classe générée.</p>
          }
        </div>
      }
    }

    <hr class="section-divider" />

    <p class="audit-section-title">Prévisualisation du code sanitisé</p>

    <button
      class="preview-btn"
      (click)="launchPreview()"
      [disabled]="isPreviewLoading()"
    >
      @if (isPreviewLoading()) { Chargement... }
      @else { Voir le code envoyé à l'IA }
    </button>

    @if (previewError()) {
      <div class="error-block" role="alert">{{ previewError() }}</div>
    }

    @if (hasPreviewResult()) {
      <div class="result-block">
        <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
          <span class="artifact-type-badge">{{ previewResult()!.controllerRef }}</span>
          <span class="sanitized-badge" [class.yes]="previewResult()!.sanitized" [class.no]="!previewResult()!.sanitized">
            {{ previewResult()!.sanitized ? 'Sanitisé' : 'Brut (sans sanitisation)' }}
          </span>
        </div>
        <p class="source-meta">
          ~{{ previewResult()!.estimatedTokens }} tokens estimés — version sanitisation {{ previewResult()!.sanitizationVersion }}
        </p>
        <code class="source-code-block">{{ previewResult()!.sanitizedSource }}</code>
      </div>
    }

    @if (showConfirmModal()) {
      <div class="confirm-overlay" role="dialog" aria-modal="true">
        <div class="confirm-card">
          <h3>Confirmer l'envoi au fournisseur IA</h3>
          <p>Le code source sera sanitise avant envoi. Aucune donnee sensible ne transpirera.</p>
          <p>Fournisseur : <strong>{{ status()?.provider }}</strong></p>
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

  private readonly aiApi = inject(AiEnrichmentApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly status = signal<AiEnrichmentStatusResponse | null>(null);
  protected readonly auditLog = signal<LlmAuditEntryResponse[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly lastResult = signal<AiEnrichmentResponse | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly showConfirmModal = signal(false);

  protected readonly isEnabled = computed(() => this.status()?.enabled ?? false);
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

  protected readonly generatedClassEntries = computed(() =>
    Object.entries(this.generateResult()?.generatedClasses ?? {}).map(([key, value]) => ({ key, value }))
  );
  protected readonly hasGenerateResult = computed(() => this.generateResult() !== null);

  protected readonly previewResult = signal<SanitizedSourcePreviewResponse | null>(null);
  protected readonly isPreviewLoading = signal(false);
  protected readonly previewError = signal<string | null>(null);
  protected readonly hasPreviewResult = computed(() => this.previewResult() !== null);

  ngOnInit(): void {
    this.aiApi.getStatus().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: s => this.status.set(s),
      error: () => this.status.set(null),
    });

    this.loadAuditLog();
  }

  private loadAuditLog(): void {
    this.aiApi.getAuditLog(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: entries => this.auditLog.set(entries),
      error: () => this.auditLog.set([]),
    });
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

  private launchEnrich(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.aiApi.enrich(this.sessionId()).subscribe({
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
    this.aiApi.review(this.sessionId()).subscribe({
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
    if (!this.isEnabled() || this.isGenerateLoading()) return;
    this.isGenerateLoading.set(true);
    this.generateError.set(null);
    this.aiApi.generate(this.sessionId()).subscribe({
      next: result => {
        this.isGenerateLoading.set(false);
        this.generateResult.set(result);
      },
      error: err => {
        this.isGenerateLoading.set(false);
        this.generateError.set(err?.error?.message ?? 'Erreur lors de la génération IA.');
      },
    });
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
        this.previewError.set(err?.error?.message ?? 'Erreur lors de la prévisualisation.');
      },
    });
  }
}
