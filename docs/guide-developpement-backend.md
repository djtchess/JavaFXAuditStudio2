# Guide de developpement backend

## Objectif

Ce guide decrit comment travailler dans `backend/` sans casser l'architecture hexagonale du projet.

## Structure cible

- `domain/`: modeles purs, regles et invariants
- `application/`: use cases, orchestration, ports entrant et sortant
- `adapters/`: REST, persistence, IA, fichiers, restitution, analyse
- `configuration/`: assemblage Spring et proprietes

## Regles de travail

- Ne pas importer Spring, JPA ou DTO REST dans `domain`.
- Ne pas mettre de logique metier centrale dans les controllers ou repositories.
- Conserver les methodes courtes, sans `continue`, avec un maximum de quatre parametres.
- Garder les retours `null` justifies et documentes.

## Parcours d'une fonctionnalite

1. Definir ou etendre un port dans `application/ports`.
2. Implementer le use case dans `application/service`.
3. Brancher l'adapter technique dans `adapters/out` ou `adapters/in`.
4. Ajouter la configuration Spring dans `configuration/`.
5. Tester le domaine, le service et l'adapter au bon niveau.

## Donnees et configuration

- Base PostgreSQL pilotee par `DB_URL`, `DB_USER` et `DB_PASSWORD`.
- CORS regle via `FRONTEND_ORIGIN`.
- Fournisseurs IA configures via `CLAUDE_API_KEY` et `OPENAI_API_KEY`.
- Le backend expose une documentation OpenAPI sur `swagger-ui.html`.

## Lancement et tests

```powershell
backend\mvnw.cmd -f backend\pom.xml test
```

```powershell
backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

## Observabilite

- Le `correlationId` est transporte via `MDC` et `X-Correlation-Id`.
- Les logs debug ne doivent jamais contenir de source brute ni de secret.
- Les details d'observabilite sont documentes dans `docs/jas-52-observabilite.md`.

## Checklist avant merge

- Le domaine reste pur.
- Les DTO REST ne traversent pas la couche `application`.
- Les tests couvrent le comportement metier, pas seulement les codes HTTP.
- La configuration Spring assemble des beans, elle ne porte pas le metier.
