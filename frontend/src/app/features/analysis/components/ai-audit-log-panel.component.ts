import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';

import { LlmAuditEntryResponse } from '../../../core/models/analysis.model';

@Component({
  selector: 'jas-ai-audit-log-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
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
  `,
})
export class AiAuditLogPanelComponent {
  readonly auditLog = input<LlmAuditEntryResponse[]>([]);

  protected readonly hasAuditEntries = computed(() => this.auditLog().length > 0);
}
