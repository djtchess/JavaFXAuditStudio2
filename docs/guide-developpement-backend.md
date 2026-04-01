# Guide de developpement backend

## Objectif

Ce guide decrit comment travailler dans `backend/` sans casser l'architecture
hexagonale du projet ni les contrats consommes par le frontend Angular.

## Structure cible

- `domain/` : modeles purs, regles et invariants
- `application/` : use cases, orchestration, ports entrants et sortants
- `adapters/` : REST, persistence, IA, fichiers, restitution, analyse
- `configuration/` : assemblage Spring, securite, observabilite et proprietes

## Regles de travail

- ne pas importer Spring, JPA ou DTO REST dans `domain`
- ne pas mettre de logique metier centrale dans les controllers ou repositories
- garder les methodes courtes, sans `continue`, avec un maximum de quatre parametres
- justifier explicitement tout retour `null`
- exposer un contrat REST stable avant de faire evoluer le frontend

## Parcours standard d'une fonctionnalite

1. definir ou etendre un port dans `application/ports`
2. implementer le use case dans `application/service`
3. brancher l'adapter technique dans `adapters/out` ou `adapters/in`
4. assembler le bean dans `configuration/`
5. tester domaine, service et adapter au bon niveau
6. mettre a jour la doc technique si le contrat ou l'exploitation changent

## Configuration et variables principales

- `DB_URL`, `DB_USER`, `DB_PASSWORD` : datasource PostgreSQL
- `FRONTEND_ORIGIN` : origine CORS autorisee
- `AI_ENRICHMENT_ENABLED`, `AI_ENRICHMENT_PROVIDER` : activation et provider IA
- `CLAUDE_API_KEY`, `OPENAI_API_KEY` : credentials API des providers HTTP
- `APP_SECURITY_API_KEY_ENABLED`, `APP_SECURITY_API_KEY` : bearer token applicatif opt-in
- `app.security.token-query-parameter` : nom du query parameter de secours pour les flux SSE limites

Le backend expose :

- OpenAPI via `swagger-ui.html`
- Actuator via `/actuator/health`, `/actuator/info`, `/actuator/ai-health`, `/actuator/metrics/**`
- Prometheus via `/actuator/prometheus`

## Socle securite minimal

Le backend embarque un premier socle Spring Security stateless.

- mecanisme retenu : bearer token applicatif
- mode local par defaut : permissif avec `app.security.api-key-enabled=false`
- activation reseau partage : `APP_SECURITY_API_KEY_ENABLED=true` + `APP_SECURITY_API_KEY`
- header attendu : `Authorization: Bearer <APP_SECURITY_API_KEY>`
- fallback reserve aux clients SSE limites : `?apiKey=<APP_SECURITY_API_KEY>`

Quand la cle est activee, le socle protege notamment :

- `/api/v1/ai-enrichment/**`
- `/api/v1/ai/reference-patterns/**`
- `/api/v1/projects/analysis/**`
- audit LLM, enrichissement IA, generation IA, artefacts IA et preview sanitisee
- reclassification manuelle
- `/actuator/**` hors `health` et `info`
- `/api-docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`

Le point d'entree 401 retourne un JSON normalise contenant un `correlationId`.

Important pour les parcours navigateur :

- le frontend Angular n'envoie pas encore automatiquement ce bearer token
- pour le cockpit local, laisser ce mode desactive
- avant activation en environnement partage, prevoir une propagation du token cote proxy ou frontend

## Observabilite

Le backend transporte `correlationId` via `MDC` et `X-Correlation-Id`.

Conventions :

- aucun code source analyse ni secret ne doit apparaitre dans les logs
- `sessionId` est injecte dans le MDC quand l'URL contient `/analysis/sessions/{sessionId}`
- les logs prod peuvent etre structures via `logback-spring.xml`

Metriques exposees :

- `jas.analysis.sessions`
- `jas.analysis.pipeline.count`
- `jas.analysis.pipeline.duration`
- `jas.analysis.pipeline.stage.duration`
- `jas.llm.enrichment.count`
- `jas.llm.enrichment.duration`
- `llm.requests.duration`

Consommation visible :

- le frontend lit les endpoints `/actuator` via `MetricsApiService`
- la page `/monitoring` combine ces metriques backend avec le monitoring HTTP local du navigateur

Reference detaillee : `docs/jas-52-observabilite.md`

## Recette locale backend

### 1. Suite backend standard

```powershell
.\scripts\test.ps1 -Target backend
```

Cette commande est la reference locale standard :

- profil Spring `test`
- H2 en memoire
- Flyway desactive
- enrichissement IA neutralise pour les tests
- depot Maven local par defaut dans `.m2/repository`

Equivalent direct :

```powershell
backend\mvnw.cmd -f backend\pom.xml test
```

### 2. Smoke PostgreSQL aligne sur la CI

```powershell
.\scripts\test.ps1 -Target backend-postgres-smoke
```

Equivalent shell :

```bash
./scripts/test.sh backend-postgres-smoke
```

Cette recette :

- demarre `postgres` via `docker compose up -d postgres`
- exporte `CI_POSTGRES_ENABLED=true`
- utilise `DB_URL`, `DB_USER` et `DB_PASSWORD`
- execute `ff.ss.javaFxAuditStudio.integration.PostgresServiceContainerIT`

### 3. Lancement applicatif local

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/javafx_audit'
$env:DB_USER='javafx_audit'
$env:DB_PASSWORD='changeme'
$env:AI_ENRICHMENT_ENABLED='false'
backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Preconditions :

- PostgreSQL accessible
- schema compatible avec `spring.jpa.hibernate.ddl-auto=validate`
- Flyway actif en runtime
- provider IA executable si l'IA reste active
- si la securite applicative est activee, `APP_SECURITY_API_KEY` doit etre renseigne

## Scripts du repo

- `.\scripts\dev-start.ps1` et `./scripts/dev-start.sh` demarrent PostgreSQL puis backend et frontend
- `.\scripts\test.ps1 -Target backend` et `./scripts/test.sh backend` rejouent la suite backend standard
- `.\scripts\test.ps1 -Target backend-postgres-smoke` et `./scripts/test.sh backend-postgres-smoke` rejouent le smoke PostgreSQL
- `MAVEN_REPO_LOCAL` permet de surcharger le depot Maven local

## Ecarts local / CI fermes ou explicites

- la suite backend standard locale et la CI backend reposent toutes deux sur le Maven Wrapper
- la CI d'integration PostgreSQL passe par `./scripts/test.sh backend-postgres-smoke`, comme la recette locale shell
- l'ecart restant est volontaire :
  - la suite locale standard tourne sur H2 avec le profil `test`
  - le smoke d'integration tourne sur PostgreSQL avec le profil `ci-postgres`
  - le runtime applicatif local utilise `application.properties` et requiert PostgreSQL

## Checklist avant merge

- le domaine reste pur
- les DTO REST ne traversent pas la couche `application`
- les tests couvrent le comportement metier et pas seulement les codes HTTP
- la configuration Spring assemble des beans ; elle ne porte pas le metier
- la doc est mise a jour si le contrat REST, la securite ou l'exploitation changent
