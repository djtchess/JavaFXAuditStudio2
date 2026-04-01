# JAS-52 - Strategie d'observabilite et de logs de workflow

Story : JAS-52 | Realignement : JAS-ACT-009 / JAS-ACT-010
Date : 2026-04-01

---

## 1. Observabilite exposee

### Backend

Les endpoints Actuator exposes par le backend sont :

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/metrics/{metricName}`
- `GET /actuator/prometheus`
- `GET /actuator/ai-health`

Quand `APP_SECURITY_API_KEY_ENABLED=false`, ces endpoints restent accessibles comme en developpement local.

Quand `APP_SECURITY_API_KEY_ENABLED=true` :

- `GET /actuator/health`
- `GET /actuator/info`

restent publics, et le reste de `/actuator/**` devient protege par le bearer token applicatif.

La health globale agrege notamment :

- `analysisWorkflow`
- `llmEnrichment`

Les principales familles de metriques exposees sont :

- `jas.analysis.sessions`
- `jas.analysis.pipeline.count`
- `jas.analysis.pipeline.duration`
- `jas.analysis.pipeline.stage.count`
- `jas.analysis.pipeline.stage.duration`
- `jas.llm.enrichment.count`
- `jas.llm.enrichment.duration`
- `llm.requests.duration`

Tags principaux utilises :

- `status`
- `stage`
- `outcome`
- `provider`
- `taskType`

### Frontend

Le frontend expose une observabilite visible dans la route :

- `/monitoring`

Cette page combine deux sources :

- les metriques backend lues via `MetricsApiService`
- un resume local en memoire des appels HTTP du navigateur via `FrontendMonitoringService`

Le resume frontend affiche :

- le nombre de requetes en vol
- le nombre total de requetes completees
- le nombre d'echecs
- le taux de succes
- la latence moyenne
- les echecs recents avec `correlationId`, statut HTTP, duree et message

Important :

- ces donnees frontend sont locales a l'onglet navigateur courant
- elles ne sont pas persistees cote backend
- elles servent au diagnostic d'exploitation et de developpement, pas a l'audit metier

---

## 2. Chaine d'observabilite frontend

L'ordre des interceptors HTTP Angular est le suivant :

1. `correlationIdInterceptor`
2. `frontendMonitoringInterceptor`
3. `errorInterceptor`

### `correlationIdInterceptor`

- ajoute `X-Correlation-Id` a chaque requete sortante
- genere un UUID quand le client n'en fournit pas

### `frontendMonitoringInterceptor`

- ouvre un compteur de requete en vol au depart
- mesure la duree reelle de l'appel
- enregistre pour chaque reponse :
  - methode HTTP
  - URL
  - statut
  - duree
  - `correlationId`
  - succes ou echec
  - message d'erreur eventuel
  - horodatage de fin
- ferme le compteur via `finalize`
- conserve jusqu'a `20` evenements recents

### `errorInterceptor`

- journalise les erreurs HTTP avec le `correlationId`
- normalise les erreurs de proxy local Angular quand le backend Spring Boot est indisponible
- preserve le `correlationId` de la reponse ou replie sur celui de la requete

---

## 3. Correlation et contexte de session

### Backend

`CorrelationFilter` applique les regles suivantes :

- lit `X-Correlation-Id` si le client le fournit
- sinon genere un UUID
- renvoie toujours `X-Correlation-Id` dans la reponse HTTP
- injecte `correlationId` dans le MDC
- injecte aussi `sessionId` dans le MDC quand l'URL matche :
  - `/analysis/sessions/{sessionId}`
  - `/analyses/{sessionId}`

Le `sessionId` MDC permet de corriger rapidement le diagnostic des workflows d'analyse dans les logs backend.

### Refus d'authentification

`ApiAuthenticationEntryPoint` renvoie un `401` JSON standardise sur les endpoints proteges quand la cle API est activee et absente ou invalide.

Forme de la reponse :

```json
{"status":401,"error":"Authentification requise","correlationId":"..."}
```

La reponse renvoie aussi :

- `X-Correlation-Id`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Cache-Control: no-store`

---

## 4. Points de logs critiques

### IngestSourcesService

| Moment | Niveau | Message |
|---|---|---|
| Debut de `handle()` | DEBUG | `Ingestion demarree - {n} refs` |
| Reference absente | WARN | `Reference non trouvee - ref masquee par securite` |
| Fin de `handle()` | DEBUG | `Ingestion terminee - {n} inputs, {n} erreurs` |

Regles :

- ne jamais logguer une reference brute de fichier
- conserver uniquement des compteurs ou identifiants opaques

### CartographyService

| Moment | Niveau | Message |
|---|---|---|
| Debut de `handle()` | DEBUG | `Cartographie demarree - controllerRef masquee` |
| Fin de `handle()` | DEBUG | `Cartographie terminee - {n} composants, {n} handlers, {n} inconnues` |

### RestExceptionHandler

- journalise le type d'exception serveur
- n'expose pas de stack trace dans la reponse HTTP

### Frontend

Le frontend journalise uniquement des erreurs HTTP techniques cote navigateur.

Format actuel :

```text
[HTTP Error] <status> <statusText> | Correlation-Id: <id> | URL: <url>
```

Ce log ne doit jamais contenir :

- de code source Java ou FXML
- de contenu de prompt
- de secret ou de jeton
- de payload metier sensible en clair

---

## 5. Ce qui ne doit jamais apparaitre dans les logs

Interdictions communes backend/frontend :

- code source Java analyse
- contenu FXML brut
- chemins absolus machine
- references utilisateur brutes (`controllerRef`, `fxmlRef`, `sourceRef`)
- prompts complets ou contexte sanitise complet hors debug local explicitement active
- stack traces dans les reponses HTTP
- jetons, credentials, `APP_SECURITY_API_KEY`, `CLAUDE_API_KEY`, `OPENAI_API_KEY`
- messages techniques Spring ou proxy exposes tels quels a l'utilisateur final

---

## 6. Activation du diagnostic

### Backend

Via profil Spring :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Ou :

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Ou ponctuellement :

```bash
LOGGING_LEVEL_FF_SS_JAVAFXAUDITSTUDIO=DEBUG java -jar app.jar
```

### Frontend

Le diagnostic frontend repose surtout sur :

- la page `/monitoring`
- la console navigateur en developpement
- les erreurs HTTP normalisees avec `correlationId`

Le frontend ne doit pas surjournaliser. Les erreurs console restent reservees aux anomalies techniques utiles au diagnostic.

---

## 7. Fichiers pivots

| Fichier | Role |
|---|---|
| `backend/src/main/resources/application.properties` | exposition Actuator, tags metrics et options de securite minimale |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/in/rest/CorrelationFilter.java` | correlationId et sessionId dans le MDC |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/in/rest/ApiAuthenticationEntryPoint.java` | reponse `401` JSON standardisee |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/ApiSecurityEndpointCatalog.java` | perimetre public/protege |
| `frontend/src/app/core/interceptors/correlation-id.interceptor.ts` | injection de `X-Correlation-Id` cote Angular |
| `frontend/src/app/core/interceptors/frontend-monitoring.interceptor.ts` | instrumentation HTTP frontend |
| `frontend/src/app/core/interceptors/error.interceptor.ts` | journalisation et normalisation des erreurs |
| `frontend/src/app/core/services/frontend-monitoring.service.ts` | agregats frontend en memoire |
| `frontend/src/app/features/monitoring/monitoring-dashboard.component.ts` | exposition visible du monitoring |

---

## 8. Handoffs

- `documentation-technique` : garder ce document aligne avec les interceptors frontend et les endpoints Actuator reels
- `observabilite-exploitation` : verifier toute evolution de tags, histogrammes ou endpoints exposes
- `securite` : revalider l'impact de `APP_SECURITY_API_KEY_ENABLED` sur `/actuator/**`, Swagger et les endpoints IA
- `frontend-angular` : conserver le dashboard `/monitoring` comme vue de diagnostic, sans deplacer la logique experte hors backend
