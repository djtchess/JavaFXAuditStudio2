import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'jas-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: block;
      min-height: 100vh;
    }

    .app-shell {
      min-height: 100vh;
      padding: 1.1rem;
    }

    .app-header {
      position: sticky;
      top: 0;
      z-index: 10;
      width: min(1320px, 100%);
      margin: 0 auto;
      padding: 1rem 1.2rem;
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
      align-items: center;
      border: 1px solid var(--line-strong);
      border-radius: 28px;
      background:
        linear-gradient(135deg, rgba(7, 18, 29, 0.9), rgba(10, 24, 39, 0.88)),
        linear-gradient(120deg, rgba(101, 223, 255, 0.08), rgba(255, 154, 77, 0.08));
      backdrop-filter: blur(18px);
      box-shadow: var(--glow);
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 1rem;
      min-width: 0;
      text-decoration: none;
    }

    .brand-mark {
      display: grid;
      place-items: center;
      width: 3.4rem;
      height: 3.4rem;
      border-radius: 1.15rem;
      border: 1px solid rgba(101, 223, 255, 0.24);
      background:
        linear-gradient(145deg, rgba(101, 223, 255, 0.16), rgba(255, 154, 77, 0.18)),
        rgba(255, 255, 255, 0.04);
      color: var(--ink-strong);
      font-family: var(--font-display);
      font-size: 1.15rem;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
    }

    .brand-copy {
      display: grid;
      gap: 0.25rem;
      min-width: 0;
    }

    .brand-copy strong {
      font-family: var(--font-display);
      font-weight: 700;
      font-size: 1.05rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .brand-copy small {
      color: var(--ink-soft);
      font-size: 0.82rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .header-side {
      display: grid;
      gap: 0.85rem;
      justify-items: end;
    }

    .nav-links {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 0.6rem;
    }

    .nav-links a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      text-decoration: none;
      font-weight: 700;
      font-size: 0.82rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--ink-soft);
      padding: 0.68rem 1rem;
      border-radius: 999px;
      border: 1px solid rgba(157, 190, 218, 0.18);
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(8px);
    }

    .nav-links a:hover {
      background: rgba(101, 223, 255, 0.1);
      color: var(--ink-strong);
      transform: translateY(-1px);
    }

    .nav-links a.active {
      border-color: rgba(101, 223, 255, 0.36);
      background:
        linear-gradient(135deg, rgba(101, 223, 255, 0.18), rgba(255, 154, 77, 0.16)),
        rgba(255, 255, 255, 0.06);
      color: white;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
    }

    .signal-strip {
      display: flex;
      justify-content: flex-end;
      flex-wrap: wrap;
      gap: 0.55rem;
    }

    .signal-strip span {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      padding: 0.45rem 0.72rem;
      border-radius: 999px;
      border: 1px solid rgba(157, 190, 218, 0.14);
      background: rgba(255, 255, 255, 0.04);
      color: white;
      font-size: 0.72rem;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .signal-strip span::before {
      content: "";
      width: 0.45rem;
      height: 0.45rem;
      border-radius: 50%;
      background: var(--cyan);
      box-shadow: 0 0 12px rgba(101, 223, 255, 0.7);
    }

    .route-stage {
      width: min(1320px, 100%);
      margin: 1.2rem auto 0;
    }

    @media (max-width: 980px) {
      .app-header {
        grid-template-columns: 1fr;
      }

      .header-side {
        justify-items: start;
      }

      .nav-links,
      .signal-strip {
        justify-content: flex-start;
      }
    }

    @media (max-width: 640px) {
      .app-shell {
        padding: 0.75rem;
      }

      .app-header {
        padding: 0.95rem;
        border-radius: 22px;
      }

      .brand {
        align-items: flex-start;
      }

      .brand-mark {
        width: 2.9rem;
        height: 2.9rem;
      }
    }
  `,
  template: `
    <div class="app-shell">
      <header class="app-header">
        <a class="brand" routerLink="/">
          <span class="brand-mark">JAS</span>
          <span class="brand-copy">
            <strong>JavaFX Audit Studio</strong>
            <small>Cockpit Angular pour refactoring guide de controllers JavaFX</small>
          </span>
        </a>

        <div class="header-side">
          <div class="signal-strip" aria-label="Signaux de plateforme">
            <span>Angular 21</span>
            <span>Spring Boot 4</span>
            <span>Observabilite active</span>
          </div>

          <nav class="nav-links" aria-label="Navigation principale">
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
            <a routerLink="/analysis" routerLinkActive="active">Analyse</a>
            <a routerLink="/projects" routerLinkActive="active">Projets</a>
            <a routerLink="/monitoring" routerLinkActive="active">Monitoring</a>
          </nav>
        </div>
      </header>

      <main class="route-stage">
        <router-outlet />
      </main>
    </div>
  `
})
export class AppComponent {
}
