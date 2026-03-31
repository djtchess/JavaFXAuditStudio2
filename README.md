# JavaFXAuditStudio2

JavaFXAuditStudio2 est le cockpit de pilotage du refactoring progressif de controllers JavaFX vers une architecture cible hexagonale. Le projet combine un backend Spring Boot 4.0.3 en Java 21 et une interface Angular 21.x qui consomme uniquement les contrats exposes par le backend.

## Vue d'ensemble

- Backend hexagonal: `domain`, `application`, `ports`, `adapters`, `configuration`.
- Frontend Angular: client mince, oriente visualisation et orchestration utilisateur.
- Domaine principal: analyse de screens JavaFX, cartographie, classification, plan de migration, artefacts et restitution.
- Base technique: PostgreSQL, Flyway, OpenAPI, tests backend et frontend.

## Structure du depot

- `backend/` : application Spring Boot, API REST, domaine, use cases et adapters.
- `frontend/` : cockpit Angular.
- `docs/` : guides, ADR et notes de travail.
- `jira/` : backlog et lots de livraison.
- `guide_generique_refactoring_controller_javafx_spring.md` : reference produit et architecture cible.

## Demarrage rapide

### Prerequis

- JDK 21
- Node.js 22 et npm
- Docker Desktop ou Docker Engine avec `docker compose`
- PostgreSQL uniquement si tu ne passes pas par Docker pour la base

### Backend

```powershell
Set-Location backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

### Frontend

```powershell
Set-Location frontend
npm install
npm run build
npm test
npm run start
```

Le frontend est configure pour parler au backend via le proxy local `frontend/proxy.conf.json` sur `/api`.

### Scripts racine

```powershell
.\scripts\build.ps1 -Target all
.\scripts\test.ps1 -Target all
.\scripts\docker-start.ps1
.\scripts\dev-start.ps1
.\scripts\db-reset.ps1 -RestartPostgres
```

Equivalents shell :

```bash
./scripts/build.sh all
./scripts/test.sh all
./scripts/run.sh --detached
./scripts/docker-start.sh
./scripts/dev-start.sh
./scripts/db-reset.sh --restart-postgres
```

## Docker

- `backend/Dockerfile` build l'application Spring Boot.
- `frontend/Dockerfile` build le cockpit Angular puis le sert via Nginx.
- `docker-compose.yml` orchestre `postgres`, `backend` et `frontend`.
- `.env.example` documente les variables attendues par `docker-compose`.

Validation rapide :

```powershell
Copy-Item .env.example .env
docker compose config
docker compose up --build -d
```

Si tu lances le backend hors Docker, adapte `DB_URL` vers `jdbc:postgresql://localhost:5432/javafx_audit`.

## Configuration

Variables principales utilisees par le backend:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `FRONTEND_ORIGIN`
- `AI_ENRICHMENT_ENABLED`
- `AI_ENRICHMENT_PROVIDER`
- `CLAUDE_API_KEY`
- `OPENAI_API_KEY`

Pour un demarrage Docker local, les valeurs par defaut `DB_PASSWORD=changeme` et `AI_ENRICHMENT_ENABLED=false` sont suffisantes.
Si tu veux activer l'IA dans Docker, utilise un provider API (`openai-gpt54` ou `claude-code`) avec la cle correspondante. Le provider CLI local n'est pas embarque dans l'image backend.

## Exploitation locale

- `.\scripts\dev-start.ps1` et `./scripts/dev-start.sh` demarrent PostgreSQL via Docker puis lancent le backend avec `DB_URL=jdbc:postgresql://localhost:5432/javafx_audit`.
- `.\scripts\docker-start.ps1`, `./scripts/run.sh` et `./scripts/docker-start.sh` lancent la stack complete conteneurisee.
- `.\scripts\db-reset.ps1` et `./scripts/db-reset.sh` suppriment les volumes Docker de la base. C'est destructif.

## Arret, logs et healthchecks

- Arret de la stack Docker : `docker compose down`
- Arret avec purge de volume PostgreSQL : `.\scripts\db-reset.ps1` ou `./scripts/db-reset.sh`
- Logs backend : `docker compose logs -f backend`
- Logs frontend : `docker compose logs -f frontend`
- Health backend : `http://localhost:8080/actuator/health`
- Health IA : `http://localhost:8080/actuator/ai-health`
- Frontend : `http://localhost:4200`

## CI

- `.github/workflows/backend.yml` execute la suite backend avec cache Maven, generation JaCoCo et commentaire de couverture sur PR.
- `.github/workflows/integration.yml` execute un smoke PostgreSQL realiste sur service container puis valide `docker compose`.
- `.github/workflows/frontend.yml` execute les checks statiques frontend, les tests avec couverture, le build Angular et le build de l'image frontend.
- Les workflows publient un commentaire de couverture sur les PR quand l'execution vient d'un `pull_request`.

## Endpoints principaux

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

## Documentation utile

- [ADR architecture hexagonale](docs/adr-001-architecture-hexagonale-cockpit.md)
- [Guide developpement backend](docs/guide-developpement-backend.md)
- [Guide integration frontend/backend](docs/guide-integration-frontend-backend.md)
- [Strategie de tests](docs/jas-51-strategie-tests.md)
- [Observabilite](docs/jas-52-observabilite.md)
- [Audit sanitization et gouvernance](docs/sanitization-audit-gouvernance.md)
- [Contrat API](docs/jas-02-contrat-api.md)

## Notes

- `docker-compose.yml` fournit un scaffold d'integration complet pour PostgreSQL, le backend et le frontend.
- `frontend/README.md` contient les details Angular si tu veux aller plus loin cote UI.
- Les regles d'architecture et de collaboration vivent dans `AGENTS.md` et les artefacts du dossier `agents/`.
