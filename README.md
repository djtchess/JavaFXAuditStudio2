# JavaFXAuditStudio2

JavaFXAuditStudio2 est le cockpit de pilotage du refactoring progressif de
controllers JavaFX vers une architecture cible hexagonale. Le projet combine un
backend Spring Boot `4.0.3` en Java `21` et une interface Angular `21.x` qui
consomme uniquement les contrats exposes par le backend.

## Vue d'ensemble

- backend hexagonal : `domain`, `application`, `ports`, `adapters`, `configuration`
- frontend Angular : client mince oriente visualisation et orchestration utilisateur
- domaine principal : analyse de screens JavaFX, cartographie, classification,
  plan de migration, artefacts et restitution
- base technique : PostgreSQL, Flyway, OpenAPI, Actuator, tests backend et frontend

## Structure du depot

- `backend/` : application Spring Boot, API REST, domaine, use cases et adapters
- `frontend/` : cockpit Angular
- `docs/` : guides, ADR et notes de travail
- `jira/` : backlog et lots de livraison
- `guide_generique_refactoring_controller_javafx_spring.md` : reference produit et architecture cible

## Demarrage rapide

### Prerequis

- JDK 21
- Node.js 22 et npm
- Docker Desktop ou Docker Engine avec `docker compose`
- PostgreSQL uniquement si vous ne passez pas par Docker pour la base

### Backend

Tests backend locaux, sans PostgreSQL :

```powershell
.\scripts\test.ps1 -Target backend
```

Equivalent direct :

```powershell
backend\mvnw.cmd -f backend\pom.xml test
```

Smoke PostgreSQL local, aligne sur la CI d'integration :

```powershell
.\scripts\test.ps1 -Target backend-postgres-smoke
```

Lancement applicatif backend en local :

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/javafx_audit'
$env:DB_USER='javafx_audit'
$env:DB_PASSWORD='changeme'
$env:AI_ENRICHMENT_ENABLED='false'
backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

### Frontend

```powershell
Set-Location frontend
npm install
npm run typecheck
npm test -- --watch=false
npm run build
npm run start
```

Le frontend utilise `frontend/proxy.conf.json` pour proxifier :

- `/api` vers `http://localhost:8080`
- `/actuator` vers `http://localhost:8080`

### Scripts racine

```powershell
.\scripts\build.ps1 -Target all
.\scripts\test.ps1 -Target all
.\scripts\test.ps1 -Target backend-postgres-smoke
.\scripts\docker-start.ps1
.\scripts\dev-start.ps1
.\scripts\db-reset.ps1 -RestartPostgres
```

Equivalents shell :

```bash
./scripts/build.sh all
./scripts/test.sh all
./scripts/test.sh backend-postgres-smoke
./scripts/run.sh --detached
./scripts/docker-start.sh
./scripts/dev-start.sh
./scripts/db-reset.sh --restart-postgres
```

## Docker

- `backend/Dockerfile` build l'application Spring Boot
- `frontend/Dockerfile` build le cockpit Angular puis le sert via Nginx
- `docker-compose.yml` orchestre `postgres`, `backend` et `frontend`
- `.env.example` documente les variables attendues par `docker-compose`

Validation rapide :

```powershell
Copy-Item .env.example .env
docker compose config
docker compose up --build -d
```

## Configuration

Variables principales utilisees par le backend :

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `FRONTEND_ORIGIN`
- `AI_ENRICHMENT_ENABLED`
- `AI_ENRICHMENT_PROVIDER`
- `CLAUDE_API_KEY`
- `OPENAI_API_KEY`
- `APP_SECURITY_API_KEY_ENABLED`
- `APP_SECURITY_API_KEY`

Notes utiles :

- pour un demarrage Docker local, `DB_PASSWORD=changeme` et `AI_ENRICHMENT_ENABLED=false` suffisent
- si vous activez le socle de securite minimal backend, le navigateur Angular ne propage pas encore automatiquement le bearer token
- en developpement local standard, laissez `APP_SECURITY_API_KEY_ENABLED=false`

## Exploitation locale

- `.\scripts\dev-start.ps1` et `./scripts/dev-start.sh` demarrent PostgreSQL via Docker puis lancent backend et frontend
- `.\scripts\test.ps1 -Target backend-postgres-smoke` et `./scripts/test.sh backend-postgres-smoke` reproduisent le smoke PostgreSQL de la CI d'integration
- `.\scripts\docker-start.ps1`, `./scripts/run.sh` et `./scripts/docker-start.sh` lancent la stack complete conteneurisee
- `.\scripts\db-reset.ps1` et `./scripts/db-reset.sh` suppriment les volumes Docker de la base ; c'est destructif

## Monitoring, logs et healthchecks

- backend health : `http://localhost:8080/actuator/health`
- backend info : `http://localhost:8080/actuator/info`
- backend AI health : `http://localhost:8080/actuator/ai-health`
- frontend : `http://localhost:4200`
- monitoring UI : `http://localhost:4200/monitoring`
- logs backend : `docker compose logs -f backend`
- logs frontend : `docker compose logs -f frontend`

La page `/monitoring` combine :

- les metriques backend lues sur `/actuator`
- les signaux HTTP locaux du frontend (correlation IDs, latence, echecs recents)

## CI

- `.github/workflows/backend.yml` execute la suite backend et publie un resume de couverture sur PR
- `.github/workflows/integration.yml` execute le smoke PostgreSQL via `./scripts/test.sh backend-postgres-smoke` puis valide `docker compose`
- `.github/workflows/frontend.yml` execute `npm run typecheck`, `npm run test:coverage`, `npm run build` et le build Docker frontend

## Endpoints principaux

### Workbench et sessions

- `GET /api/v1/workbench/overview`
- `POST /api/v1/analysis/sessions`
- `GET /api/v1/analysis/sessions/{sessionId}`
- `GET /api/v1/analysis/sessions/{sessionId}/cartography`
- `GET /api/v1/analysis/sessions/{sessionId}/classification`
- `GET /api/v1/analysis/sessions/{sessionId}/plan`
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts`
- `GET /api/v1/analysis/sessions/{sessionId}/report`
- `POST /api/v1/analysis/sessions/{sessionId}/run`
- `POST /api/v1/analysis/sessions/{sessionId}/artifacts/export`

### IA, projets et monitoring

- `GET /api/v1/ai-enrichment/status`
- `GET /api/v1/analysis/sessions/{sessionId}/llm-audit`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}/dashboard`
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/ai-health`
- `GET /actuator/metrics/{metricName}`

## Documentation utile

- [Guide developpement backend](docs/guide-developpement-backend.md)
- [Guide integration frontend/backend](docs/guide-integration-frontend-backend.md)
- [Frontend README](frontend/README.md)
- [ADR architecture hexagonale](docs/adr-001-architecture-hexagonale-cockpit.md)
- [Strategie de tests](docs/jas-51-strategie-tests.md)
- [Observabilite](docs/jas-52-observabilite.md)
- [Arbitrage des modules specialises du moteur](docs/jas-act-011-arbitrage-modules-specialises.md)
- [Audit sanitization et gouvernance](docs/sanitization-audit-gouvernance.md)
- [Contrat API](docs/jas-02-contrat-api.md)

## Notes

- `docker-compose.yml` fournit un scaffold d'integration complet pour PostgreSQL, le backend et le frontend
- les regles d'architecture et de collaboration vivent dans `AGENTS.md` et les artefacts du dossier `agents/`

