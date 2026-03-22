import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'jas-root',
  imports: [RouterOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    nav {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      padding: 0.8rem 1.5rem;
      border-bottom: 1px solid var(--line);
      background: rgba(255, 255, 255, 0.85);
    }

    .brand {
      font-family: var(--font-display);
      font-weight: 700;
      font-size: 1rem;
      color: var(--slate);
      text-decoration: none;
    }

    .nav-links {
      display: flex;
      gap: 1rem;
      margin-left: auto;
    }

    .nav-links a {
      text-decoration: none;
      font-weight: 600;
      font-size: 0.88rem;
      color: var(--ink-soft);
      padding: 0.3rem 0.6rem;
      border-radius: 8px;
      transition: background 0.15s, color 0.15s;
    }

    .nav-links a:hover {
      background: var(--accent-soft);
      color: var(--slate);
    }
  `,
  template: `
    <nav>
      <a class="brand" routerLink="/">JavaFX Audit Studio</a>
      <div class="nav-links">
        <a routerLink="/">Dashboard</a>
        <a routerLink="/analysis">Analyse</a>
        <a routerLink="/projects">Projets</a>
      </div>
    </nav>
    <router-outlet />
  `
})
export class AppComponent {
}
