# Frontend Angular

## Cible technique

- Angular `21.2.4`
- TypeScript `5.9.x`
- RxJS `7.8.x`
- Node.js `22` recommande (`>= 20.19.0` minimum)

## Scripts utiles

```bash
npm install
npm run typecheck
npm test -- --watch=false
npm run build
npm run start
```

- `npm run start` lance `ng serve --proxy-config proxy.conf.json`
- `npm run typecheck` couvre `tsconfig.app.json` et `tsconfig.spec.json`
- `npm run test:coverage` est le point d'entree utilise par la CI frontend

## Proxy local

Le frontend ne code pas d'URL absolue vers le backend.

- `/api` est proxifie vers `http://localhost:8080`
- `/actuator` est proxifie vers `http://localhost:8080`

Fichier de reference : `frontend/proxy.conf.json`

## Routage principal

- `/` : dashboard workbench
- `/analysis` : creation d'une session d'analyse
- `/analysis/:sessionId` : detail de session et orchestration du pipeline
- `/projects` : dashboard projet
- `/monitoring` : dashboard de monitoring backend + frontend

## Pipeline HTTP frontend

L'ordre des interceptors HTTP est defini dans `src/app/app.config.ts` :

1. `correlationIdInterceptor`
2. `frontendMonitoringInterceptor`
3. `errorInterceptor`

Effets attendus :

- chaque requete sortante porte `X-Correlation-Id`
- les durees, statuts et correlations sont agreges dans `FrontendMonitoringService`
- les erreurs HTTP sont normalisees pour l'UI et loguees avec correlation ID

## Observabilite visible

La page `/monitoring` combine deux sources :

- les metriques backend lues via `MetricsApiService` sur `/actuator`
- les signaux HTTP locaux exposes par `FrontendMonitoringService`

La section frontend affiche :

- requetes en vol
- requetes completees
- echecs
- taux de succes
- latence moyenne
- echecs recents avec correlation ID

## Securite

Le backend peut activer un bearer token applicatif via `APP_SECURITY_API_KEY_ENABLED=true`.

Etat actuel du frontend :

- le navigateur ajoute `X-Correlation-Id`
- le frontend ne propage pas encore automatiquement `Authorization: Bearer ...`
- le fallback `?apiKey=...` reste reserve aux flux SSE/browser limites

Consequences pratiques :

- en developpement local standard, laisser la securite applicative backend desactivee
- en environnement partage, prevoir une propagation du bearer token cote proxy ou frontend avant activation

## Validation locale

```bash
npm run typecheck
npm test -- --watch=false
npm run build
```
