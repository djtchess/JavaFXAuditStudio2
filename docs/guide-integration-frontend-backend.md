# Guide d'integration frontend/backend

## Objectif

Ce guide explique comment le frontend Angular consomme le backend Spring Boot sans
reimplementer la logique de refactoring ni contourner les contrats exposes.

## Regle principale

Le frontend est un client de contrats. Toute regle metier, toute decision
d'analyse et toute transformation experte restent cote backend.

## Services frontend relies

- `WorkbenchApiService` consomme `/api/v1/workbench/overview`
- `AnalysisApiService` consomme les endpoints de session d'analyse
- `AiEnrichmentApiService` consomme les endpoints IA, audit LLM et artefacts IA
- `ProjectApiService` consomme les endpoints projet
- `ReclassificationApiService` consomme la reclassification manuelle
- `MetricsApiService` consomme les endpoints `/actuator`
- `FrontendMonitoringService` agregre les signaux HTTP locaux

## Interceptors HTTP

L'ordre est defini dans `frontend/src/app/app.config.ts` :

1. `correlationIdInterceptor`
2. `frontendMonitoringInterceptor`
3. `errorInterceptor`

Effets attendus :

- chaque appel sortant porte `X-Correlation-Id`
- le monitoring frontend mesure duree, statut, correlation ID et succes/echec
- les erreurs HTTP sont normalisees avant affichage UI

## Endpoints backend relies par le frontend

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

### IA, audit et artefacts IA

- `GET /api/v1/ai-enrichment/status`
- `POST /api/v1/analysis/sessions/{sessionId}/enrich`
- `GET /api/v1/analysis/sessions/{sessionId}/llm-audit`
- `POST /api/v1/analysis/sessions/{sessionId}/review`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai`
- `GET /api/v1/analysis/sessions/{sessionId}/generate/ai/stream`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai/refine`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai/export/zip`
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts/ai`
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts/ai/{artifactType}/versions`
- `POST /api/v1/analysis/sessions/{sessionId}/artifacts/ai/coherence`
- `POST /api/v1/analysis/sessions/{sessionId}/preview-sanitized`

### Reclassification

- `PATCH /api/v1/analysis/sessions/{sessionId}/rules/{ruleId}/classification`
- `GET /api/v1/analysis/sessions/{sessionId}/rules/{ruleId}/classification/history`

### Projets et analyses avancees

- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}/dashboard`
- `GET /api/v1/analysis/sessions/{sessionId}/flow`
- `POST /api/v1/projects/analysis/dependencies`
- `POST /api/v1/projects/analysis/delta`

### Observabilite backend

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/ai-health`
- `GET /actuator/metrics`
- `GET /actuator/metrics/{metricName}`

## Prefixes a connaitre

- la convention canonique est `/api/v1/analysis/sessions/...`
- les anciens endpoints `/api/v1/analyses/...` restent acceptes temporairement cote backend comme alias legacy
- le frontend doit consommer uniquement la convention canonique

## Routage frontend

- `/` affiche le dashboard global
- `/analysis` affiche la soumission d'une session
- `/analysis/:sessionId` affiche le detail de la session
- `/projects` affiche le dashboard projet
- `/monitoring` affiche le dashboard de monitoring backend + frontend

## Proxy local

Le frontend route :

- `/api` vers `http://localhost:8080`
- `/actuator` vers `http://localhost:8080`

Reference : `frontend/proxy.conf.json`

## Observabilite frontend visible

La page `/monitoring` combine :

- les metriques backend agregees par `MetricsApiService`
- les signaux HTTP locaux exposes par `FrontendMonitoringService`

La section frontend affiche :

- requetes en vol
- requetes completees
- echecs
- taux de succes
- latence moyenne
- echecs recents avec correlation ID

## Regles d'echange

- les listes JSON doivent etre retournees en `[]`, jamais en `null`
- les structures de reponse restent alignees sur les modeles TypeScript du frontend
- `X-Correlation-Id` doit rester present sur les appels et sur les erreurs
- Angular affiche les donnees ; il ne recalcule pas la logique d'analyse

## Securite des endpoints sensibles

Le backend peut activer un socle minimal via `APP_SECURITY_API_KEY_ENABLED=true`.

- header attendu : `Authorization: Bearer <APP_SECURITY_API_KEY>`
- fallback reserve aux clients limites sur SSE : `?apiKey=<APP_SECURITY_API_KEY>`

### Endpoints publics conserves

- `GET /api/v1/workbench/overview`
- endpoints coeur du workbench et de l'analyse non classes sensibles
- `GET /actuator/health`
- `GET /actuator/info`

### Endpoints proteges quand le mode est actif

- `GET /api/v1/ai-enrichment/status`
- `/api/v1/ai/reference-patterns/**`
- `/api/v1/projects/analysis/**`
- `GET /api/v1/analysis/sessions/{sessionId}/llm-audit`
- `POST /api/v1/analysis/sessions/{sessionId}/enrich`
- `POST /api/v1/analysis/sessions/{sessionId}/review`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai`
- `GET /api/v1/analysis/sessions/{sessionId}/generate/ai/stream`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai/refine`
- `POST /api/v1/analysis/sessions/{sessionId}/generate/ai/export/zip`
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts/ai`
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts/ai/{artifactType}/versions`
- `POST /api/v1/analysis/sessions/{sessionId}/artifacts/ai/coherence`
- `POST /api/v1/analysis/sessions/{sessionId}/preview-sanitized`
- `PATCH /api/v1/analysis/sessions/{sessionId}/rules/{ruleId}/classification`
- `GET /api/v1/analysis/sessions/{sessionId}/rules/{ruleId}/classification/history`
- `/actuator/**` hors `health` et `info`
- `/api-docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`

### Impact frontend actuel

- le frontend navigateur ne propage pas encore automatiquement le bearer token applicatif
- en developpement local standard, laisser la securite backend desactivee
- en environnement partage, prevoir une propagation du bearer token cote proxy ou frontend avant activation

## Processus d'ajout d'un nouvel ecran ou endpoint

1. Stabiliser le contrat backend.
2. Ajouter ou mettre a jour le modele TypeScript.
3. Ajouter la methode dans le service Angular concerne.
4. Ajouter ou mettre a jour le test du service.
5. Brancher le composant et son test.
6. Mettre a jour ce guide si le contrat ou le routage visible change.

## Validation

```powershell
backend\mvnw.cmd -f backend\pom.xml test
```

```powershell
Set-Location frontend
npm run typecheck
npm test -- --watch=false
npm run build
```

## Points d'attention

- ne pas dupliquer dans Angular les calculs de statut, de classification, de lot ou de plan
- ne pas inventer de chemins API ; suivre les services frontend et les controllers backend existants
- l'alias legacy `/api/v1/analyses/...` peut encore exister cote backend, mais il n'est plus une reference de developpement
- toute evolution de contrat impose la mise a jour conjointe des DTO/backend, modeles TypeScript, services et specs
