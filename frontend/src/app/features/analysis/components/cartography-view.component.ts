import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CartographyResponse } from '../../../core/models/analysis.model';

@Component({
  selector: 'jas-cartography-view',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .summary-bar {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }

    .count-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.3rem 0.8rem;
      border-radius: 999px;
      background: var(--surface-chip);
      color: var(--surface-ink-strong);
      font-weight: 600;
      font-size: 0.82rem;
    }

    .warning-banner {
      padding: 0.7rem 1rem;
      border-radius: 12px;
      background: rgba(245, 158, 11, 0.08);
      border: 1px solid rgba(245, 158, 11, 0.3);
      color: var(--surface-warning);
      font-size: 0.88rem;
      font-weight: 500;
      margin-bottom: 1rem;
    }

    .section-label {
      margin: 1rem 0 0.5rem;
      font-weight: 700;
      font-size: 0.9rem;
      color: var(--surface-ink-strong);
    }

    .section-label:first-of-type {
      margin-top: 0;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.85rem;
    }

    th {
      text-align: left;
      padding: 0.55rem 0.75rem;
      background: var(--surface-chip);
      color: var(--surface-ink-soft);
      font-weight: 600;
      font-size: 0.78rem;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      border-bottom: 1px solid var(--surface-line);
    }

    td {
      padding: 0.5rem 0.75rem;
      border-bottom: 1px solid var(--surface-line);
      color: var(--surface-ink);
    }

    tr:last-child td {
      border-bottom: none;
    }

    .mono {
      font-family: monospace;
      font-size: 0.82rem;
    }

    .empty-msg {
      padding: 0.6rem 0;
      color: var(--ink-soft);
      font-size: 0.85rem;
      font-style: italic;
    }
  `,
  template: `
    <div class="summary-bar">
      <span class="count-badge">{{ componentCount() }} composants</span>
      <span class="count-badge">{{ handlerCount() }} handlers</span>
    </div>

    @if (data().hasUnknowns) {
      <div class="warning-banner">
        Attention : des elements inconnus ont ete detectes dans la cartographie.
      </div>
    }

    <p class="section-label">Composants FXML</p>
    @if (data().components.length === 0) {
      <p class="empty-msg">Aucun composant detecte.</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>fx:id</th>
            <th>Type</th>
            <th>Event Handler</th>
          </tr>
        </thead>
        <tbody>
          @for (comp of data().components; track comp.fxId) {
            <tr>
              <td class="mono">{{ comp.fxId }}</td>
              <td>{{ comp.componentType }}</td>
              <td class="mono">{{ comp.eventHandler || '-' }}</td>
            </tr>
          }
        </tbody>
      </table>
    }

    <p class="section-label">Handlers</p>
    @if (data().handlers.length === 0) {
      <p class="empty-msg">Aucun handler detecte.</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>Methode</th>
            <th>Reference FXML</th>
            <th>Type injecte</th>
          </tr>
        </thead>
        <tbody>
          @for (h of data().handlers; track h.methodName) {
            <tr>
              <td class="mono">{{ h.methodName }}</td>
              <td class="mono">{{ h.fxmlRef }}</td>
              <td class="mono">{{ h.injectedType || '-' }}</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `
})
export class CartographyViewComponent {
  readonly data = input.required<CartographyResponse>();

  protected readonly componentCount = computed(() => this.data().components.length);
  protected readonly handlerCount = computed(() => this.data().handlers.length);
}
