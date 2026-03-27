import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

interface DiffLine {
  left: string;
  right: string;
  kind: 'same' | 'changed' | 'added' | 'removed';
}

function splitLines(code: string): string[] {
  return code.replace(/\r\n/g, '\n').split('\n');
}

@Component({
  selector: 'jas-ai-generation-diff',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: block;
      margin-top: 0.85rem;
    }

    .diff-shell {
      border-radius: 12px;
      border: 1px solid var(--surface-line);
      background: rgba(255, 255, 255, 0.72);
      overflow: hidden;
    }

    .diff-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.7rem 0.9rem;
      border-bottom: 1px solid var(--surface-line);
      background: var(--surface-chip);
    }

    .diff-title {
      margin: 0;
      font-size: 0.86rem;
      font-weight: 700;
      color: var(--surface-ink-strong);
    }

    .diff-subtitle {
      margin: 0;
      font-size: 0.76rem;
      color: var(--surface-ink-soft);
    }

    .diff-table {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    }

    .diff-col {
      min-width: 0;
    }

    .diff-col + .diff-col {
      border-left: 1px solid var(--surface-line);
    }

    .diff-col-header {
      padding: 0.5rem 0.75rem;
      font-size: 0.75rem;
      font-weight: 700;
      color: var(--surface-ink-soft);
      background: rgba(255, 255, 255, 0.36);
      border-bottom: 1px solid var(--surface-line);
    }

    .diff-lines {
      font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
      font-size: 0.76rem;
      line-height: 1.5;
      max-height: 320px;
      overflow: auto;
    }

    .diff-line {
      display: flex;
      gap: 0.5rem;
      padding: 0.2rem 0.75rem;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .diff-line + .diff-line {
      border-top: 1px dotted var(--surface-line);
    }

    .line-marker {
      flex-shrink: 0;
      width: 1rem;
      font-weight: 700;
      opacity: 0.85;
    }

    .line-content {
      min-width: 0;
      flex: 1;
    }

    .same {
      color: var(--surface-ink);
      background: rgba(16, 38, 56, 0.03);
    }

    .changed-left,
    .removed {
      color: var(--surface-warning);
      background: var(--surface-warning-soft);
    }

    .changed-right,
    .added {
      color: var(--surface-success);
      background: var(--surface-success-soft);
    }

    .empty-state {
      padding: 0.8rem 0.9rem;
      color: var(--surface-ink-soft);
      font-size: 0.8rem;
      font-style: italic;
    }
  `,
  template: `
    <section class="diff-shell">
      <div class="diff-header">
        <div>
          <p class="diff-title">{{ title() }}</p>
          <p class="diff-subtitle">{{ templateLabel() }} versus {{ generatedLabel() }}</p>
        </div>
      </div>

      @if (diffLines().length > 0) {
        <div class="diff-table">
          <div class="diff-col">
            <div class="diff-col-header">{{ templateLabel() }}</div>
            <div class="diff-lines">
              @for (line of diffLines(); track $index) {
                <div class="diff-line"
                     [class.same]="line.kind === 'same'"
                     [class.changed-left]="line.kind === 'changed'"
                     [class.removed]="line.kind === 'removed'">
                  <span class="line-marker">-</span>
                  <span class="line-content">{{ line.left }}</span>
                </div>
              }
            </div>
          </div>
          <div class="diff-col">
            <div class="diff-col-header">{{ generatedLabel() }}</div>
            <div class="diff-lines">
              @for (line of diffLines(); track $index) {
                <div class="diff-line"
                     [class.same]="line.kind === 'same'"
                     [class.changed-right]="line.kind === 'changed'"
                     [class.added]="line.kind === 'added'">
                  <span class="line-marker">+</span>
                  <span class="line-content">{{ line.right }}</span>
                </div>
              }
            </div>
          </div>
        </div>
      } @else {
        <div class="empty-state">Aucun diff disponible pour ce couple de sources.</div>
      }
    </section>
  `,
})
export class AiGenerationDiffComponent {
  readonly title = input('Diff template vs IA');
  readonly templateLabel = input('Template');
  readonly generatedLabel = input('IA');
  readonly templateCode = input('');
  readonly generatedCode = input('');

  protected readonly diffLines = computed<DiffLine[]>(() => {
    const templateLines = splitLines(this.templateCode());
    const generatedLines = splitLines(this.generatedCode());
    const max = Math.max(templateLines.length, generatedLines.length);
    const lines: DiffLine[] = [];

    for (let index = 0; index < max; index += 1) {
      const left = templateLines[index] ?? '';
      const right = generatedLines[index] ?? '';
      if (left === right) {
        lines.push({ left, right, kind: 'same' });
      } else if (left && !right) {
        lines.push({ left, right: '', kind: 'removed' });
      } else if (!left && right) {
        lines.push({ left: '', right, kind: 'added' });
      } else {
        lines.push({ left, right, kind: 'changed' });
      }
    }

    return lines;
  });
}
