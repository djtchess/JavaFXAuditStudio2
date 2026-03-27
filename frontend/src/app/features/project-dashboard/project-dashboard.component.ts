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
      width: min(1240px, calc(100% - 2rem));
      margin: 0 auto;
      padding: 2rem 0 4rem;
      display: grid;
      gap: 1.15rem;
      color: #d6e4f5;
    }

    .pd-hero {
      position: relative;
      overflow: hidden;
      border-radius: 28px;
      border: 1px solid rgba(117, 148, 188, 0.28);
      background:
        radial-gradient(circle at 84% 18%, rgba(64, 216, 255, 0.26), transparent 34%),
        radial-gradient(circle at 12% 82%, rgba(255, 170, 72, 0.2), transparent 37%),
        linear-gradient(140deg, rgba(7, 18, 33, 0.96), rgba(7, 24, 44, 0.9));
      box-shadow:
        0 28px 48px rgba(2, 8, 22, 0.48),
        inset 0 0 0 1px rgba(183, 214, 255, 0.08);
      padding: 1.45rem 1.4rem;
      animation: pdFadeIn 420ms ease-out;
    }

    .pd-hero::before {
      content: '';
      position: absolute;
      inset: 0;
      background-image:
        linear-gradient(to right, rgba(140, 188, 245, 0.05) 1px, transparent 1px),
        linear-gradient(to bottom, rgba(140, 188, 245, 0.05) 1px, transparent 1px);
      background-size: 28px 28px;
      pointer-events: none;
    }

    .pd-header {
      position: relative;
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 1.15rem;
    }

    .pd-title {
      margin: 0.25rem 0 0;
      font-family: "Segoe UI Variable Display", "Bahnschrift", "Trebuchet MS", sans-serif;
      font-size: clamp(1.6rem, 3.8vw, 2.85rem);
      font-weight: 700;
      letter-spacing: 0.04em;
      color: #f6fbff;
    }

    .pd-meta {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font-size: 0.75rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: rgba(185, 213, 244, 0.82);
    }

    .pd-meta::before {
      content: '';
      width: 0.45rem;
      height: 0.45rem;
      border-radius: 50%;
      background: #4ce0c3;
      box-shadow: 0 0 0.65rem rgba(76, 224, 195, 0.72);
    }

    .pd-kicker {
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.17em;
      font-size: 0.7rem;
      color: rgba(173, 205, 241, 0.86);
    }

    .pd-subtitle {
      position: relative;
      z-index: 1;
      margin: 0;
      color: rgba(208, 226, 248, 0.9);
      max-width: 78ch;
      line-height: 1.58;
      font-size: 0.94rem;
    }

    .pd-project-selector {
      position: relative;
      z-index: 1;
      display: flex;
      flex-wrap: wrap;
      gap: 0.62rem;
      margin-top: 1rem;
    }

    .pd-project-btn {
      padding: 0.4rem 0.86rem;
      border-radius: 999px;
      border: 1px solid rgba(120, 152, 194, 0.48);
      background: rgba(13, 36, 62, 0.7);
      color: #cfe4fb;
      font-size: 0.8rem;
      letter-spacing: 0.02em;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s, border-color 0.2s, color 0.2s, transform 0.2s;
    }

    .pd-project-btn.active {
      background: linear-gradient(130deg, rgba(80, 212, 255, 0.26), rgba(255, 173, 75, 0.2));
      color: #f6fbff;
      border-color: rgba(135, 225, 255, 0.75);
      box-shadow: 0 0 0 1px rgba(135, 225, 255, 0.25) inset;
    }

    .pd-project-btn:not(.active):hover {
      background: rgba(16, 47, 78, 0.86);
      border-color: rgba(146, 212, 247, 0.72);
      transform: translateY(-1px);
    }

    .pd-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
      gap: 1rem;
    }

    .pd-card {
      padding: 1.12rem 1.04rem 1.16rem;
      border-radius: 20px;
      border: 1px solid rgba(109, 140, 180, 0.26);
      background:
        linear-gradient(145deg, rgba(7, 22, 41, 0.95), rgba(6, 17, 32, 0.92));
      box-shadow:
        inset 0 0 0 1px rgba(175, 215, 255, 0.06),
        0 16px 26px rgba(3, 9, 23, 0.38);
      animation: pdSlideUp 420ms ease-out both;
    }

    .pd-card-value {
      margin: 0;
      font-family: "Georgia", "Times New Roman", serif;
      font-size: clamp(1.65rem, 4.5vw, 2.35rem);
      font-weight: 700;
      color: #f6fbff;
      line-height: 1.1;
      font-variant-numeric: tabular-nums;
    }

    .pd-card-label {
      margin-top: 0.32rem;
      font-size: 0.75rem;
      color: rgba(173, 203, 236, 0.83);
      text-transform: uppercase;
      letter-spacing: 0.1em;
    }

    .pd-section {
      border-radius: 22px;
      border: 1px solid rgba(111, 149, 189, 0.25);
      padding: 1.2rem 1.1rem;
      background:
        radial-gradient(circle at top right, rgba(64, 216, 255, 0.11), transparent 38%),
        linear-gradient(135deg, rgba(8, 24, 43, 0.95), rgba(9, 18, 35, 0.95));
      box-shadow:
        inset 0 0 0 1px rgba(168, 206, 247, 0.05),
        0 18px 32px rgba(2, 8, 22, 0.35);
    }

    .pd-section-title {
      font-size: 0.94rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.13em;
      color: rgba(189, 215, 242, 0.92);
      margin: 0 0 1.05rem;
    }

    .pd-lot-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.45rem;
    }

    .pd-lot-item {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      font-size: 0.86rem;
      color: #dbe9f8;
      border: 1px solid rgba(111, 149, 188, 0.23);
      border-radius: 14px;
      padding: 0.5rem 0.6rem;
      background: rgba(10, 31, 54, 0.64);
    }

    .pd-lot-index {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.55rem;
      height: 1.55rem;
      border-radius: 50%;
      background: linear-gradient(120deg, rgba(87, 221, 255, 0.33), rgba(255, 178, 83, 0.26));
      font-size: 0.72rem;
      font-weight: 700;
      color: #f4f9ff;
      flex-shrink: 0;
    }

    .pd-error {
      padding: 0.85rem 1rem;
      background: rgba(78, 23, 21, 0.7);
      border: 1px solid rgba(252, 136, 122, 0.45);
      border-radius: 14px;
      color: #ffd1c8;
      font-size: 0.86rem;
    }

    .pd-loading {
      padding: 1.6rem 1.1rem;
      color: #cadef4;
      font-size: 0.9rem;
      border-radius: 16px;
      border: 1px solid rgba(111, 149, 189, 0.26);
      background: rgba(7, 24, 44, 0.84);
    }

    .pd-empty {
      text-align: center;
      padding: 2.8rem 1.3rem;
      border-radius: 18px;
      border: 1px dashed rgba(116, 155, 195, 0.48);
      background: rgba(7, 24, 44, 0.8);
      color: rgba(193, 218, 245, 0.87);
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
      color: rgba(199, 222, 247, 0.88);
      white-space: nowrap;
    }

    .pd-session-input {
      flex: 1;
      min-width: 220px;
      max-width: 400px;
      padding: 0.5rem 0.86rem;
      border: 1px solid rgba(113, 148, 189, 0.45);
      border-radius: 10px;
      font-size: 0.85rem;
      font-family: monospace;
      color: #f6fbff;
      background: rgba(10, 31, 54, 0.72);
      outline: none;
      transition: border-color 0.2s, box-shadow 0.2s;
    }

    .pd-session-input:focus {
      border-color: rgba(136, 223, 255, 0.9);
      box-shadow: 0 0 0 2px rgba(101, 214, 255, 0.2);
    }

    .pd-session-hint {
      font-size: 0.78rem;
      color: rgba(188, 214, 241, 0.77);
      font-style: italic;
    }

    @keyframes pdFadeIn {
      from {
        opacity: 0;
        transform: translateY(12px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes pdSlideUp {
      from {
        opacity: 0;
        transform: translateY(8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @media (max-width: 900px) {
      .pd-container {
        width: min(1240px, calc(100% - 1rem));
      }

      .pd-hero {
        padding: 1.15rem 1rem;
      }

      .pd-card {
        border-radius: 16px;
      }
    }
  `,
  template: `
    <div class="pd-container">
      <section class="pd-hero">
        <div class="pd-header">
          <div>
            <p class="pd-kicker">Migration Command Center</p>
            <h1 class="pd-title">Dashboard de progression</h1>
          </div>
          @if (lastRefreshed()) {
            <span class="pd-meta">
              Actualise a {{ lastRefreshed() | date:'HH:mm:ss' }}
            </span>
          }
        </div>

        <p class="pd-subtitle">
          Visualisez les signaux de migration, priorisez les lots recommandes et lancez les revues
          de session depuis un cockpit unifie.
        </p>

        @if (projects().length > 1) {
          <div class="pd-project-selector" role="group" aria-label="Selection du projet">
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
      </section>

      @if (error()) {
        <div class="pd-error" role="alert">{{ error() }}</div>
      }

      @if (loading()) {
        <div class="pd-loading">Chargement en cours...</div>
      } @else if (!dashboard() && !error()) {
        <div class="pd-empty">
          <p class="pd-empty-title">Aucun projet disponible</p>
          <p>Lancez une premiere analyse pour voir apparaitre les donnees ici.</p>
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
            <div class="pd-card-label">Completees</div>
          </div>
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.uncertainCount }}</div>
            <div class="pd-card-label">Incertaines</div>
          </div>
          <div class="pd-card">
            <div class="pd-card-value">{{ dash.reclassifiedCount }}</div>
            <div class="pd-card-label">Reclassifiees</div>
          </div>
        </div>

        <div class="pd-section">
          <h2 class="pd-section-title">Regles par categorie</h2>
          <jas-category-bar-chart [data]="dash.rulesByCategory" />
        </div>

        @if (dash.recommendedLotOrder.length > 0) {
          <div class="pd-section">
            <h2 class="pd-section-title">Ordre recommande des lots</h2>
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
              <span class="pd-session-hint">
                Entrez un identifiant de session pour lancer la revue
              </span>
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
