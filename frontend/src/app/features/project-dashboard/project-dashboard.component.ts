import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  effect,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { interval } from 'rxjs';

import { ProjectApiService } from '../../core/services/project-api.service';
import { ProjectDashboardResponse } from '../../core/models/analysis.model';
import { CategoryBarChartComponent } from '../../shared/components/category-bar-chart.component';
import { MigrationScoreWidgetComponent } from '../../shared/components/migration-score-widget.component';

/**
 * Page dashboard de progression de migration par projet.
 * Polling automatique toutes les 30 secondes.
 * JAS-014
 */
@Component({
  selector: 'jas-project-dashboard',
  standalone: true,
  imports: [DatePipe, FormsModule, CategoryBarChartComponent, MigrationScoreWidgetComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .pd-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem 1.5rem;
    }

    .pd-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .pd-title {
      font-size: 1.4rem;
      font-weight: 700;
      color: var(--slate, #1e293b);
      margin: 0;
    }

    .pd-meta {
      font-size: 0.78rem;
      color: var(--ink-soft, #94a3b8);
    }

    .pd-project-selector {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
    }

    .pd-project-btn {
      padding: 0.3rem 0.85rem;
      border-radius: 999px;
      border: 1.5px solid var(--line, #e2e8f0);
      background: white;
      font-size: 0.82rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s;
    }

    .pd-project-btn.active {
      background: var(--slate, #1e293b);
      color: white;
      border-color: var(--slate, #1e293b);
    }

    .pd-project-btn:not(.active):hover {
      background: var(--accent-soft, #f1f5f9);
      border-color: var(--slate, #1e293b);
    }

    .pd-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .pd-card {
      background: white;
      border: 1px solid var(--line, #e2e8f0);
      border-radius: 12px;
      padding: 1.2rem 1rem;
      text-align: center;
    }

    .pd-card-value {
      font-size: 2rem;
      font-weight: 700;
      color: var(--slate, #1e293b);
      line-height: 1.1;
    }

    .pd-card-label {
      font-size: 0.75rem;
      color: var(--ink-soft, #94a3b8);
      margin-top: 0.25rem;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .pd-section {
      background: white;
      border: 1px solid var(--line, #e2e8f0);
      border-radius: 12px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }

    .pd-section-title {
      font-size: 1rem;
      font-weight: 600;
      color: var(--slate, #1e293b);
      margin: 0 0 1rem;
    }

    .pd-lot-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .pd-lot-item {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      font-size: 0.88rem;
      color: var(--slate, #1e293b);
    }

    .pd-lot-index {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.5rem;
      height: 1.5rem;
      border-radius: 50%;
      background: var(--accent-soft, #f1f5f9);
      font-size: 0.72rem;
      font-weight: 700;
      color: var(--ink-soft, #64748b);
      flex-shrink: 0;
    }

    .pd-error {
      padding: 1rem 1.5rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 8px;
      color: #dc2626;
      font-size: 0.88rem;
      margin-bottom: 1.5rem;
    }

    .pd-loading {
      text-align: center;
      padding: 3rem;
      color: var(--ink-soft, #94a3b8);
      font-size: 0.9rem;
    }

    .pd-empty {
      text-align: center;
      padding: 4rem 2rem;
      color: var(--ink-soft, #94a3b8);
    }

    .pd-empty-title {
      font-size: 1.1rem;
      font-weight: 600;
      margin-bottom: 0.5rem;
    }

    .pd-session-input-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }

    .pd-session-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--slate, #1e293b);
      white-space: nowrap;
    }

    .pd-session-input {
      flex: 1;
      min-width: 220px;
      max-width: 400px;
      padding: 0.45rem 0.85rem;
      border: 1.5px solid var(--line, #e2e8f0);
      border-radius: 8px;
      font-size: 0.85rem;
      font-family: monospace;
      color: var(--slate, #1e293b);
      background: white;
      outline: none;
      transition: border-color 0.15s;
    }

    .pd-session-input:focus {
      border-color: var(--slate, #1e293b);
    }

    .pd-session-hint {
      font-size: 0.78rem;
      color: var(--ink-soft, #94a3b8);
      font-style: italic;
    }
  `,
  template: `
    <div class="pd-container">
      <div class="pd-header">
        <h1 class="pd-title">Dashboard de progression</h1>
        @if (lastRefreshed()) {
          <span class="pd-meta">
            Actualisé le {{ lastRefreshed() | date:'HH:mm:ss' }}
          </span>
        }
      </div>

      @if (error()) {
        <div class="pd-error" role="alert">{{ error() }}</div>
      }

      @if (projects().length > 1) {
        <div class="pd-project-selector" role="group" aria-label="Sélection du projet">
          @for (project of projects(); track project) {
            <button
              class="pd-project-btn"
              [class.active]="selectedProject() === project"
              (click)="selectProject(project)"
            >
              {{ project }}
            </button>
          }
        </div>
      }

      @if (loading()) {
        <div class="pd-loading">Chargement en cours...</div>
      } @else if (!dashboard() && !error()) {
        <div class="pd-empty">
          <p class="pd-empty-title">Aucun projet disponible</p>
          <p>Lancez une première analyse pour voir apparaître les données ici.</p>
        </div>
      } @else if (dashboard(); as dash) {
        <div class="pd-cards">
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.totalSessions }}</div>
            <div class="pd-card-label">Sessions totales</div>
          </div>
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.analysingCount }}</div>
            <div class="pd-card-label">En cours</div>
          </div>
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.completedCount }}</div>
            <div class="pd-card-label">Complétées</div>
          </div>
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.uncertainCount }}</div>
            <div class="pd-card-label">Incertaines</div>
          </div>
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.reclassifiedCount }}</div>
            <div class="pd-card-label">Reclassifiées</div>
          </div>
        </div>

        <div class="pd-section">
          <h2 class="pd-section-title">Règles par catégorie</h2>
          <jas-category-bar-chart [data]="dash.rulesByCategory" />
        </div>

        @if (dash.recommendedLotOrder.length > 0) {
          <div class="pd-section">
            <h2 class="pd-section-title">Ordre recommandé des lots</h2>
            <ol class="pd-lot-list">
              @for (lot of dash.recommendedLotOrder; track lot; let i = $index) {
                <li class="pd-lot-item">
                  <span class="pd-lot-index">{{ i + 1 }}</span>
                  {{ lot }}
                </li>
              }
            </ol>
          </div>
        }

        <div class="pd-section">
          <h2 class="pd-section-title">Score de migration</h2>
          <div class="pd-session-input-row">
            <label class="pd-session-label" for="pd-session-id-input">Session :</label>
            <input
              id="pd-session-id-input"
              class="pd-session-input"
              type="text"
              placeholder="Identifiant de session (ex: abc-123)"
              [ngModel]="reviewSessionId()"
              (ngModelChange)="reviewSessionId.set($event)"
            />
            @if (!reviewSessionId()) {
              <span class="pd-session-hint">Entrez un identifiant de session pour lancer la revue</span>
            }
          </div>
          @if (reviewSessionId()) {
            <jas-migration-score-widget [sessionId]="reviewSessionId()" />
          }
        </div>
      }
    </div>
  `,
})
export class ProjectDashboardComponent implements OnInit {
  private readonly projectApiService = inject(ProjectApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly projects = signal<string[]>([]);
  readonly selectedProject = signal<string | null>(null);
  readonly dashboard = signal<ProjectDashboardResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly lastRefreshed = signal<Date | null>(null);
  readonly reviewSessionId = signal<string>('');

  constructor() {
    effect(() => {
      const projectId = this.selectedProject();
      if (projectId !== null) {
        this.loadDashboard(projectId);
      }
    });
  }

  ngOnInit(): void {
    this.loadProjectsAndStartPolling();
  }

  protected selectProject(projectId: string): void {
    this.selectedProject.set(projectId);
  }

  private loadProjectsAndStartPolling(): void {
    this.projectApiService
      .listProjects()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (projectList) => this.handleProjectsLoaded(projectList),
        error: () => this.error.set('Impossible de charger la liste des projets.'),
      });

    interval(30_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshCurrentDashboard());
  }

  private handleProjectsLoaded(projectList: string[]): void {
    this.projects.set(projectList);
    if (projectList.length > 0) {
      this.selectedProject.set(projectList[0]);
    }
  }

  private refreshCurrentDashboard(): void {
    const projectId = this.selectedProject();
    if (projectId !== null) {
      this.loadDashboard(projectId);
    }
  }

  private loadDashboard(projectId: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.projectApiService
      .getDashboard(projectId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.dashboard.set(data);
          this.lastRefreshed.set(new Date());
          this.loading.set(false);
        },
        error: () => {
          this.error.set(`Impossible de charger le dashboard du projet "${projectId}".`);
          this.loading.set(false);
        },
      });
  }
}
