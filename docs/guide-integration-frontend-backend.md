# Guide d'integration frontend/backend

## Objectif

Ce guide explique comment le frontend Angular consomme le backend Spring Boot du projet sans reimplementer la logique de refactoring.

## Regle principale

Le frontend est un client de contrats. Toute regle metier, toute decision d'analyse et toute transformation experte restent cote backend.

## Cartographie des appels

- `frontend/src/app/core/services/analysis-api.service.ts` consomme les endpoints d'analyse.
- `frontend/src/app/core/services/ai-enrichment-api.service.ts` consomme les endpoints IA et audit.
- `frontend/src/app/core/services/project-api.service.ts` consomme le dashboard projet.
- `frontend/src/app/core/services/reclassification-api.service.ts` consomme la reclassification.
- `frontend/src/app/core/interceptors/correlation-id.interceptor.ts` ajoute `X-Correlation-Id`.
- `frontend/src/app/core/interceptors/error.interceptor.ts` trace les erreurs HTTP avec correlation ID.

## Endpoints backend deja relies

- `GET /api/v1/workbench/overview`
- `POST /api/v1/analysis/sessions`
- `GET /api/v1/analysis/sessions/{sessionId}`
- `GET /api/v1/analysis/sessions/{sessionId}/cartography`
- `GET /api/v1/analysis/sessions/{sessionId}/classification`
- `GET /api/v1/analysis/sessions/{sessionId}/plan`
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts`
- `GET /api/v1/analysis/sessions/{sessionId}/report`
- `POST /api/v1/analysis/sessions/{sessionId}/run`

## Prefixes a connaitre

- Le flux de pilotage et d'analyse utilise le prefixe `/api/v1/analysis/...`.
- Les appels IA existants utilisent encore `/api/v1/analyses/...` dans le code courant.
- Tant que ce split existe, le frontend doit suivre les contrats effectifs du backend et non une convention theorique.

## Routage frontend

- `/` affiche le dashboard global.
- `/analysis` affiche la soumission d'une session.
- `/analysis/:sessionId` affiche le detail de la session.
- `/projects` affiche le dashboard projet.

## Proxy local

Le frontend route `/api` vers `http://localhost:8080` via `frontend/proxy.conf.json`. Cela permet de developper sans coder les URLs absolues dans Angular.

## Regles d'echange

- Les listes JSON ne doivent jamais etre interpretees comme `null`.
- Les structures de reponse doivent etre alignees sur `frontend/src/app/core/models/analysis.model.ts`.
- Le `correlationId` doit rester present sur les appels et les retours d'erreur.
- Les composants Angular affichent les donnees, ils ne recalculent pas la logique d'analyse.

## Processus d'ajout d'un nouvel ecran ou endpoint

1. Stabiliser le contrat backend.
2. Ajouter le type TypeScript correspondant.
3. Ajouter la methode dans le service Angular concerne.
4. Ajouter le test du service.
5. Brancher le composant et son test.

## Validation

```powershell
backend\mvnw.cmd -f backend\pom.xml test
```

```powershell
Set-Location frontend
npm test
```

## Points d'attention

- Ne pas dupliquer les calculs de statut, de classification ou de plan dans Angular.
- Ne pas utiliser de chemin API invente: suivre les services existants et les contrats publies.
- Pour les evolutions de contrat, mettre a jour les DTO backend, le modele TS et les specs ensemble.
