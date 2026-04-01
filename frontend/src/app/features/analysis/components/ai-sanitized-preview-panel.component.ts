import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';

import { SanitizedSourcePreviewResponse } from '../../../core/models/analysis.model';

@Component({
  selector: 'jas-ai-sanitized-preview-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <hr class="section-divider" />

    <p class="audit-section-title">Previsualisation du code sanitise</p>

    <button
      class="preview-btn"
      (click)="requestPreview()"
      [disabled]="isLoading()"
    >
      @if (isLoading()) { Chargement... }
      @else { Voir le code envoye a l'IA }
    </button>

    @if (error()) {
      <div class="error-block" role="alert">{{ error() }}</div>
    }

    @if (hasPreviewResult()) {
      <div class="result-block">
        <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
          <span class="artifact-type-badge">{{ previewResult()!.controllerRef }}</span>
          <span class="sanitized-badge" [class.yes]="previewResult()!.sanitized" [class.no]="!previewResult()!.sanitized">
            {{ previewResult()!.sanitized ? 'Sanitise' : 'Brut (sans sanitisation)' }}
          </span>
        </div>
        <p class="source-meta">
          ~{{ previewResult()!.estimatedTokens }} tokens estimes - version sanitisation {{ previewResult()!.sanitizationVersion }}
        </p>
        <code class="source-code-block">{{ previewResult()!.sanitizedSource }}</code>
      </div>
    }
  `,
})
export class AiSanitizedPreviewPanelComponent {
  readonly isLoading = input(false);
  readonly error = input<string | null>(null);
  readonly previewResult = input<SanitizedSourcePreviewResponse | null>(null);

  readonly previewRequested = output<void>();

  protected readonly hasPreviewResult = computed(() => this.previewResult() !== null);

  protected requestPreview(): void {
    this.previewRequested.emit();
  }
}
