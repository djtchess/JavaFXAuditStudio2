import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, interval, startWith } from 'rxjs';

import { FrontendMonitoringService } from '../../core/services/frontend-monitoring.service';
import { MetricsApiService } from '../../core/services/metrics-api.service';
import { MonitoringSnapshot } from '../../core/models/monitoring.model';

@Component({
  selector: 'jas-monitoring-dashboard',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .page-shell {
      width: min(1240px, calc(100% - 2rem));
      margin: 0 auto;
      padding: 2rem 0 4rem;
      display: grid;
      gap: 1rem;
      color: #d8e7f8;
    }

    .hero {
      position: relative;
      overflow: hidden;
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(0, 1.5fr) minmax(240px, 0.5fr);
      align-items: start;
      padding: 1.35rem 1.35rem 1.45rem;
      border: 1px solid rgba(118, 154, 198, 0.26);
      border-radius: 28px;
      background:
        radial-gradient(circle at 86% 15%, rgba(67, 214, 255, 0.24), transparent 34%),
        radial-gradient(circle at 16% 86%, rgba(255, 174, 74, 0.19), transparent 36%),
        linear-gradient(145deg, rgba(7, 22, 41, 0.95), rgba(6, 16, 30, 0.95));
      box-shadow:
        0 26px 46px rgba(3, 9, 23, 0.44),
        inset 0 0 0 1px rgba(172, 209, 250, 0.06);
      animation: mdFadeIn 420ms ease-out;
    }

    .hero::before {
      content: '';
      position: absolute;
      inset: 0;
      background-image:
        linear-gradient(to right, rgba(145, 192, 245, 0.045) 1px, transparent 1px),
        linear-gradient(to bottom, rgba(145, 192, 245, 0.045) 1px, transparent 1px);
      background-size: 28px 28px;
      pointer-events: none;
    }

    .hero-copy,
    .hero-meta {
      position: relative;
      z-index: 1;
    }

    .eyebrow {
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.17em;
      font-size: 0.7rem;
      color: rgba(181, 212, 246, 0.82);
    }

    h1 {
      margin: 0.35rem 0 0.65rem;
      font-family: "Segoe UI Variable Display", "Bahnschrift", "Trebuchet MS", sans-serif;
      font-size: clamp(1.7rem, 4vw, 2.9rem);
      letter-spacing: 0.04em;
      line-height: 1.06;
      color: #f6fbff;
    }

    .lead {
      margin: 0;
      color: rgba(205, 226, 249, 0.92);
      max-width: 68ch;
      line-height: 1.56;
      font-size: 0.95rem;
    }

    .hero-meta {
      display: grid;
      gap: 0.62rem;
      border: 1px solid rgba(108, 143, 183, 0.35);
      border-radius: 16px;
      background: rgba(9, 30, 53, 0.66);
      padding: 0.85rem 0.9rem;
      color: rgba(194, 219, 247, 0.85);
      font-size: 0.81rem;
      align-content: start;
    }

    .hero-meta strong {
      display: block;
      color: #f2f8ff;
      font-family: "Georgia", "Times New Roman", serif;
      font-size: 1.02rem;
      letter-spacing: 0.03em;
      margin-bottom: 0.1rem;
    }

    .status-panel {
      padding: 0.8rem 0.95rem;
      border-radius: 14px;
      border: 1px solid rgba(110, 147, 189, 0.34);
      background: rgba(8, 28, 50, 0.8);
      color: rgba(200, 224, 249, 0.9);
      font-size: 0.88rem;
      animation: mdSlideUp 360ms ease-out;
    }

    .status-panel.error {
      border-color: rgba(251, 134, 120, 0.52);
      color: #ffd6cd;
      background: rgba(77, 21, 19, 0.7);
    }

    .section {
      display: grid;
      gap: 0.9rem;
      border-radius: 22px;
      border: 1px solid rgba(108, 146, 187, 0.24);
      padding: 1.1rem;
      background:
        radial-gradient(circle at top right, rgba(65, 214, 255, 0.11), transparent 34%),
        linear-gradient(140deg, rgba(8, 24, 43, 0.93), rgba(9, 18, 35, 0.94));
      box-shadow:
        inset 0 0 0 1px rgba(171, 208, 248, 0.05),
        0 14px 32px rgba(3, 9, 23, 0.32);
    }

    .summary-grid,
    .stage-grid {
      display: grid;
      gap: 0.8rem;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
    }

    .card {
      padding: 0.95rem 0.92rem 1rem;
      border-radius: 16px;
      border: 1px solid rgba(116, 151, 191, 0.26);
      background: rgba(9, 31, 54, 0.72);
      box-shadow: inset 0 0 0 1px rgba(176, 213, 251, 0.05);
    }

    .card-label {
      margin: 0 0 0.32rem;
      font-size: 0.72rem;
      text-transform: uppercase;
      letter-spacing: 0.09em;
      color: rgba(178, 209, 243, 0.82);
    }

    .card-value {
      margin: 0;
      font-size: clamp(1.5rem, 4vw, 2.15rem);
      line-height: 1.05;
      font-weight: 700;
      color: #f6fbff;
      font-family: "Georgia", "Times New Roman", serif;
      font-variant-numeric: tabular-nums;
    }

    .card-subtitle {
      margin: 0.42rem 0 0;
      color: rgba(194, 219, 246, 0.8);
      font-size: 0.8rem;
    }

    .section-title {
      margin: 0;
      font-size: 0.9rem;
      text-transform: uppercase;
      letter-spacing: 0.13em;
      color: rgba(188, 216, 245, 0.93);
    }

    .status-list {
      display: grid;
      gap: 0.62rem;
    }

    .status-row {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 0.68rem;
      align-items: center;
      padding: 0.8rem 0.9rem;
      border-radius: 14px;
      border: 1px solid rgba(116, 151, 192, 0.27);
      background: rgba(9, 31, 54, 0.72);
    }

    .status-name {
      font-weight: 600;
      color: #e2effd;
    }

    .status-count {
      font-variant-numeric: tabular-nums;
      font-size: 1.06rem;
      font-weight: 700;
      color: #f6fbff;
      font-family: "Georgia", "Times New Roman", serif;
    }

    .status-hint {
      grid-column: 1 / -1;
      font-size: 0.76rem;
      color: rgba(188, 214, 243, 0.75);
      margin-top: -0.2rem;
      letter-spacing: 0.02em;
    }

    .empty-state {
      padding: 0.95rem 1rem;
      border-radius: 14px;
      border: 1px dashed rgba(117, 154, 196, 0.44);
      color: rgba(190, 216, 245, 0.84);
      background: rgba(8, 28, 49, 0.64);
    }

    @keyframes mdFadeIn {
      from {
        opacity: 0;
        transform: translateY(12px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes mdSlideUp {
      from {
        opacity: 0;
        transform: translateY(8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @media (max-width: 920px) {
      .page-shell {
        width: min(1240px, calc(100% - 1rem));
      }

      .hero {
        grid-template-columns: 1fr;
      }
    }
  `,
  template: `
    <main class="page-shell">
      <section class="hero">
        <div class="hero-copy">
          <p class="eyebrow">Observabilite</p>
          <h1>Monitoring d'exploitation</h1>
          <p class="lead">
            Le tableau de bord lit directement les metriques exposees par Actuator pour suivre le
            volume de sessions, la sante IA, les resultats du pipeline et les temps moyens des
            etapes critiques.
          </p>
        </div>

        <div class="hero-meta">
          <div>
            <strong>Rafraichissement</strong>
            Toutes les 30 secondes
          </div>
          @if (lastRefreshed(); as refreshed) {
            <div>
              <strong>Derniere mise a jour</strong>
              {{ refreshed | date:'HH:mm:ss' }}
            </div>
          }
        </div>
      </section>

      @if (error()) {
        <section class="status-panel error" role="alert">
          {{ error() }}
        </section>
      }

      @if (loading()) {
        <section class="status-panel">
          Chargement des metriques Actuator...
        </section>
      } @else if (refreshing()) {
        <section class="status-panel">
          Actualisation des metriques en cours...
        </section>
      }

      <section class="section">
        <h2 class="section-title">Observabilite frontend</h2>
        <div class="summary-grid">
          <article class="card">
            <p class="card-label">Requetes en vol</p>
            <p class="card-value">{{ frontendSummary().inflightRequests }}</p>
            <p class="card-subtitle">Requetes HTTP actuellement ouvertes</p>
          </article>
          <article class="card">
            <p class="card-label">Requetes completees</p>
            <p class="card-value">{{ frontendSummary().totalCompletedRequests }}</p>
            <p class="card-subtitle">Depuis le chargement de la session frontend</p>
          </article>
          <article class="card">
            <p class="card-label">Echecs</p>
            <p class="card-value">{{ frontendSummary().failedRequests }}</p>
            <p class="card-subtitle">Requetes en erreur cote navigateur</p>
          </article>
          <article class="card">
            <p class="card-label">Succes</p>
            <p class="card-value">{{ frontendSummary().successRate | number:'1.0-1' }} %</p>
            <p class="card-subtitle">Taux de succes frontend observe</p>
          </article>
          <article class="card">
            <p class="card-label">Latence moyenne</p>
            <p class="card-value">{{ frontendSummary().averageDurationMs | number:'1.0-0' }} ms</p>
            <p class="card-subtitle">Duree moyenne des appels HTTP</p>
          </article>
        </div>

        @if (recentFrontendFailures().length > 0) {
          <div class="status-list">
            @for (failure of recentFrontendFailures(); track failure.completedAt + failure.url + failure.correlationId) {
              <article class="status-row">
                <div class="status-name">{{ failure.method }} {{ failure.url }}</div>
                <div class="status-count">{{ failure.status }}</div>
                <div class="status-hint">
                  correlationId = {{ failure.correlationId }} | {{ failure.durationMs }} ms | {{ failure.completedAt | date:'HH:mm:ss' }}
                </div>
                @if (failure.message) {
                  <div class="status-hint">{{ failure.message }}</div>
                }
              </article>
            }
          </div>
        } @else {
          <div class="empty-state">Aucun echec frontend recent n'a ete observe.</div>
        }
      </section>

      @if (snapshot(); as data) {
        <section class="section">
          <h2 class="section-title">Volume global</h2>
          <div class="summary-grid">
            <article class="card">
              <p class="card-label">Sessions totales</p>
              <p class="card-value">{{ data.totalSessions }}</p>
              <p class="card-subtitle">Metrique jas.analysis.sessions</p>
            </article>
          </div>
        </section>

        <section class="section">
          <h2 class="section-title">Sante IA</h2>
          @if (data.aiHealth; as aiHealth) {
            <div class="summary-grid">
              <article class="card">
                <p class="card-label">Etat IA</p>
                <p class="card-value">{{ aiHealth.status }}</p>
                <p class="card-subtitle">{{ aiHealth.provider }}</p>
              </article>
              <article class="card">
                <p class="card-label">Circuit breaker</p>
                <p class="card-value">{{ formatCircuitBreakerState(aiHealth.circuitBreakerState) }}</p>
                <p class="card-subtitle">{{ aiHealth.totalRequests }} appel(s)</p>
              </article>
              <article class="card">
                <p class="card-label">Succes</p>
                <p class="card-value">{{ aiHealth.successRate | number:'1.0-1' }} %</p>
                <p class="card-subtitle">Taux de succes LLM</p>
              </article>
              <article class="card">
                <p class="card-label">P95 latence</p>
                <p class="card-value">{{ aiHealth.p95LatencyMs | number:'1.0-0' }} ms</p>
                <p class="card-subtitle">Metric llm.requests.duration</p>
              </article>
              <article class="card">
                <p class="card-label">Tokens</p>
                <p class="card-value">{{ aiHealth.totalTokens | number:'1.0-0' }}</p>
                <p class="card-subtitle">Consommation cumulee</p>
              </article>
            </div>
            @if (describeOutcomes(data.llmOutcomes).length > 0) {
              <div class="status-list">
                @for (metric of describeOutcomes(data.llmOutcomes); track metric.key) {
                  <article class="status-row">
                    <div class="status-name">{{ metric.label }}</div>
                    <div class="status-count">{{ metric.value }}</div>
                    <div class="status-hint">outcome = {{ metric.key }}</div>
                  </article>
                }
              </div>
            } @else {
              <div class="empty-state">Aucun appel LLM observe pour le moment.</div>
            }
          } @else {
            <div class="empty-state">Aucune synthese IA disponible pour le moment.</div>
          }
        </section>

        <section class="section">
          <h2 class="section-title">Sessions par statut</h2>
          @if (data.statusMetrics.length > 0) {
            <div class="status-list">
              @for (metric of data.statusMetrics; track metric.status) {
                <article class="status-row">
                  <div class="status-name">{{ metric.label }}</div>
                  <div class="status-count">{{ metric.count }}</div>
                  <div class="status-hint">status = {{ metric.status }}</div>
                </article>
              }
            </div>
          } @else {
            <div class="empty-state">Aucune valeur de statut disponible pour le moment.</div>
          }
        </section>

        <section class="section">
          <h2 class="section-title">Resultats pipeline</h2>
          @if (describeOutcomes(data.pipelineOutcomes).length > 0) {
            <div class="status-list">
              @for (metric of describeOutcomes(data.pipelineOutcomes); track metric.key) {
                <article class="status-row">
                  <div class="status-name">{{ metric.label }}</div>
                  <div class="status-count">{{ metric.value }}</div>
                  <div class="status-hint">outcome = {{ metric.key }}</div>
                </article>
              }
            </div>
          } @else {
            <div class="empty-state">Aucun resultat de pipeline disponible pour le moment.</div>
          }
        </section>

        <section class="section">
          <h2 class="section-title">Temps moyen par etape</h2>
          @if (data.stageMetrics.length > 0) {
            <div class="stage-grid">
              @for (metric of data.stageMetrics; track metric.stage) {
                <article class="card">
                  <p class="card-label">{{ metric.label }}</p>
                  <p class="card-value">{{ metric.averageMs | number:'1.0-0' }} ms</p>
                  <p class="card-subtitle">{{ metric.count }} echantillon(s)</p>
                </article>
              }
            </div>
          } @else {
            <div class="empty-state">Aucune mesure de duree par etape disponible pour le moment.</div>
          }
        </section>

        <section class="section">
          <h2 class="section-title">Sante backend</h2>
          <div class="summary-grid">
            <article class="card">
              <p class="card-label">Etat global</p>
              <p class="card-value">{{ data.healthStatus }}</p>
              <p class="card-subtitle">Health Actuator</p>
            </article>
          </div>
          @if (data.healthComponents.length > 0) {
            <div class="status-list">
              @for (component of data.healthComponents; track component.name) {
                <article class="status-row">
                  <div class="status-name">{{ formatHealthComponentName(component.name) }}</div>
                  <div class="status-count">{{ component.status }}</div>
                  <div class="status-hint">component = {{ component.name }}</div>
                </article>
              }
            </div>
          } @else {
            <div class="empty-state">Aucun composant de sante detaille n'est expose.</div>
          }
        </section>
      }
    </main>
  `,
})
export class MonitoringDashboardComponent implements OnInit {
  private readonly metricsApi = inject(MetricsApiService);
  private readonly frontendMonitoring = inject(FrontendMonitoringService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly snapshot = signal<MonitoringSnapshot | null>(null);
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly lastRefreshed = signal<Date | null>(null);
  protected readonly frontendSummary = this.frontendMonitoring.summary;
  protected readonly recentFrontendFailures = this.frontendMonitoring.recentFailures;
  protected readonly outcomeLabels: Record<string, string> = {
    success: 'Succes',
    degraded: 'Degrade',
    failure: 'Echec',
    blocked: 'Bloque',
    circuit_open: 'Circuit ouvert',
    not_found: 'Introuvable',
  };
  protected readonly outcomeOrder = ['success', 'degraded', 'failure', 'blocked', 'circuit_open', 'not_found'];

  ngOnInit(): void {
    interval(30_000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }

  private reload(): void {
    if (this.snapshot() === null) {
      this.loading.set(true);
    } else {
      this.refreshing.set(true);
    }
    this.error.set(null);

    this.metricsApi.loadMonitoringSnapshot()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.loading.set(false);
          this.refreshing.set(false);
        })
      )
      .subscribe({
        next: (snapshot: MonitoringSnapshot) => {
          this.snapshot.set(snapshot);
          this.lastRefreshed.set(new Date());
        },
        error: (err: unknown) => {
          this.error.set(this.resolveErrorMessage(err));
        },
      });
  }

  private resolveErrorMessage(err: unknown): string {
    let message = 'Impossible de charger les metriques de monitoring.';
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const errorPayload = (err as { error?: { message?: string } }).error;
      if (errorPayload?.message) {
        message = errorPayload.message;
      }
    }
    return message;
  }

  protected describeOutcomes(outcomes: Record<string, number>): Array<{ key: string; label: string; value: number }> {
    return Object.entries(outcomes)
      .sort(([left], [right]) => this.compareByKnownOrder(left, right))
      .map(([key, value]) => ({
        key,
        label: this.outcomeLabels[key] ?? this.formatLabel(key),
        value,
      }));
  }

  protected formatCircuitBreakerState(state: string): string {
    const labels: Record<string, string> = {
      CLOSED: 'Ferme',
      OPEN: 'Ouvert',
      HALF_OPEN: 'Semi-ouvert',
    };
    return labels[state] ?? state;
  }

  protected formatHealthComponentName(name: string): string {
    return this.formatLabel(name);
  }

  private formatLabel(value: string): string {
    return value
      .split(/(?=[A-Z])|_/)
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }

  private compareByKnownOrder(left: string, right: string): number {
    const leftIndex = this.outcomeOrder.indexOf(left);
    const rightIndex = this.outcomeOrder.indexOf(right);
    if (leftIndex >= 0 && rightIndex >= 0) {
      return leftIndex - rightIndex;
    }
    if (leftIndex >= 0) {
      return -1;
    }
    if (rightIndex >= 0) {
      return 1;
    }
    return left.localeCompare(right);
  }
}
