import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';

import {
  AiArtifactCoherenceResponse,
  AiGeneratedArtifactResponse,
} from '../../../core/models/analysis.model';

type CoherenceFindingEntry = {
  key: string;
  value: string;
};

@Component({
  selector: 'jas-ai-persisted-artifacts-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <hr class="section-divider" />

    <p class="audit-section-title">Artefacts IA persistes</p>

    <div class="persisted-toolbar">
      <button class="persisted-btn" (click)="requestReload()" [disabled]="persistedArtifactsLoading()">
        @if (persistedArtifactsLoading()) { Chargement... }
        @else { Rafraichir les artefacts }
      </button>
      <button class="persisted-btn secondary" (click)="requestCoherenceCheck()" [disabled]="persistedCoherenceLoading() || !hasPersistedArtifacts()">
        @if (persistedCoherenceLoading()) { Verification... }
        @else { Verifier la coherence }
      </button>
    </div>

    @if (persistedArtifactsError()) {
      <div class="error-block" role="alert">{{ persistedArtifactsError() }}</div>
    }

    @if (persistedCoherenceError()) {
      <div class="error-block" role="alert">{{ persistedCoherenceError() }}</div>
    }

    @if (hasIncompletePersistedArtifacts()) {
      <div class="result-block degraded">
        <p class="degraded-msg">
          {{ incompletePersistedArtifactCount() }} artefact(s) persiste(s) contiennent encore des placeholders d'implementation.
          Ces versions ne doivent pas etre considerees comme une migration finalisee.
        </p>
      </div>
    }

    @if (hasPersistedArtifacts()) {
      <div class="persisted-list">
        @for (artifact of persistedArtifactEntries(); track artifact.artifactType + '-' + artifact.versionNumber) {
          <article class="persisted-card">
            <div class="persisted-card-header">
              <div>
                <div class="persisted-title">{{ artifact.artifactType }} - {{ artifact.className }}</div>
                <div class="persisted-meta">
                  v{{ artifact.versionNumber }} - {{ artifact.provider }} - {{ artifact.originTask }} - {{ artifact.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
                </div>
              </div>
              <span
                class="persisted-status-badge"
                [class.ready]="artifact.implementationStatus === 'READY'"
                [class.incomplete]="artifact.implementationStatus === 'INCOMPLETE'"
              >
                {{ artifact.implementationStatus === 'INCOMPLETE' ? 'Incomplet' : 'Pret' }}
              </span>
              <button class="persisted-btn tiny" (click)="toggleVersions(artifact.artifactType)">
                @if (activePersistedArtifactType() === artifact.artifactType) { Masquer versions }
                @else { Voir versions }
              </button>
            </div>
            @if (artifact.implementationWarning) {
              <p class="persisted-warning">{{ artifact.implementationWarning }}</p>
            }
            <code class="generated-class-code persisted-code">{{ artifact.content }}</code>

            @if (activePersistedArtifactType() === artifact.artifactType) {
              <div class="persisted-versions">
                @if (isPersistedArtifactVersionsLoading(artifact.artifactType)) {
                  <div class="status-loading">Chargement de l'historique...</div>
                } @else if (persistedArtifactVersionsError()) {
                  <div class="status-error">{{ persistedArtifactVersionsError() }}</div>
                } @else if (getPersistedArtifactVersionsFor(artifact.artifactType).length > 0) {
                  @for (version of getPersistedArtifactVersionsFor(artifact.artifactType); track version.versionNumber) {
                    <div class="persisted-version-row">
                      <span class="persisted-version-badge">v{{ version.versionNumber }}</span>
                      <span
                        class="persisted-status-badge"
                        [class.ready]="version.implementationStatus === 'READY'"
                        [class.incomplete]="version.implementationStatus === 'INCOMPLETE'"
                      >
                        {{ version.implementationStatus === 'INCOMPLETE' ? 'Incomplet' : 'Pret' }}
                      </span>
                      <span>{{ version.requestId }}</span>
                      <span>{{ version.provider }}</span>
                      <span>{{ version.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</span>
                    </div>
                  }
                } @else {
                  <div class="empty-persisted-state">Aucune version detaillee disponible pour ce type.</div>
                }
              </div>
            }
          </article>
        }
      </div>
    } @else if (!persistedArtifactsLoading()) {
      <p class="no-suggestions">Aucun artefact IA persiste pour cette session.</p>
    }

    @if (persistedCoherence()) {
      <div class="result-block" [class.degraded]="persistedCoherence()!.degraded">
        @if (persistedCoherence()!.degraded) {
          <p class="degraded-msg">Coherence degradee : {{ persistedCoherence()!.degradationReason }}</p>
        } @else {
          <p class="nominal-msg">Coherence verifiee - {{ persistedCoherence()!.provider }} - {{ persistedCoherence()!.tokensUsed }} tokens</p>
        }
        <p class="source-meta">{{ persistedCoherence()!.summary }}</p>
        @if (persistedCoherenceFindings().length > 0) {
          <div class="persisted-findings">
            @for (entry of persistedCoherenceFindings(); track entry.key) {
              <div class="persisted-finding">
                <span class="persisted-finding-key">{{ entry.key }}</span>
                <span class="persisted-finding-value">{{ entry.value }}</span>
              </div>
            }
          </div>
        }
        @if (persistedCoherence()!.globalFindings.length > 0) {
          <ul class="persisted-global-findings">
            @for (finding of persistedCoherence()!.globalFindings; track finding) {
              <li>{{ finding }}</li>
            }
          </ul>
        }
      </div>
    }
  `,
})
export class AiPersistedArtifactsPanelComponent {
  readonly persistedArtifactsLoading = input(false);
  readonly persistedArtifactsError = input<string | null>(null);
  readonly persistedArtifactEntries = input<AiGeneratedArtifactResponse[]>([]);
  readonly incompletePersistedArtifactCount = input(0);
  readonly activePersistedArtifactType = input<string | null>(null);
  readonly persistedArtifactVersions = input<Record<string, AiGeneratedArtifactResponse[]>>({});
  readonly persistedArtifactVersionsLoading = input<Record<string, boolean>>({});
  readonly persistedArtifactVersionsError = input<string | null>(null);
  readonly persistedCoherenceLoading = input(false);
  readonly persistedCoherenceError = input<string | null>(null);
  readonly persistedCoherence = input<AiArtifactCoherenceResponse | null>(null);
  readonly persistedCoherenceFindings = input<CoherenceFindingEntry[]>([]);

  readonly reloadRequested = output<void>();
  readonly coherenceRequested = output<void>();
  readonly toggleVersionsRequested = output<string>();

  protected readonly hasPersistedArtifacts = computed(() => this.persistedArtifactEntries().length > 0);
  protected readonly hasIncompletePersistedArtifacts = computed(() => this.incompletePersistedArtifactCount() > 0);

  protected requestReload(): void {
    this.reloadRequested.emit();
  }

  protected requestCoherenceCheck(): void {
    this.coherenceRequested.emit();
  }

  protected toggleVersions(artifactType: string): void {
    this.toggleVersionsRequested.emit(artifactType);
  }

  protected isPersistedArtifactVersionsLoading(artifactType: string): boolean {
    return this.persistedArtifactVersionsLoading()[artifactType] ?? false;
  }

  protected getPersistedArtifactVersionsFor(artifactType: string): AiGeneratedArtifactResponse[] {
    return this.persistedArtifactVersions()[artifactType] ?? [];
  }
}
