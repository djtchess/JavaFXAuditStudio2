# JAS-22 - Compatibilité technique Angular

## Metadonnees

| Champ        | Valeur                  |
|--------------|-------------------------|
| Story        | JAS-22                  |
| Epic         | JAS-EPIC-03             |
| Date         | 2026-03-16              |
| Statut       | VERIFIE                 |

---

## Versions en place (package.json)

| Outil / Lib             | Version déclarée | Type        |
|-------------------------|------------------|-------------|
| @angular/core           | 21.2.4           | dependency  |
| @angular/common         | 21.2.4           | dependency  |
| @angular/router         | 21.2.4           | dependency  |
| @angular/forms          | 21.2.4           | dependency  |
| @angular/animations     | 21.2.4           | dependency  |
| @angular/platform-browser | 21.2.4         | dependency  |
| rxjs                    | ^7.8.0           | dependency  |
| zone.js                 | ^0.16.1          | dependency  |
| @angular/cli            | 21.2.2           | devDependency |
| @angular/build          | 21.2.2           | devDependency |
| @angular/compiler-cli   | 21.2.4           | devDependency |
| typescript              | ~5.9.3           | devDependency |
| @types/node             | ^22.13.0         | devDependency |
| vitest                  | ^4.1.0           | devDependency |
| jsdom                   | ^26.0.0          | devDependency |

---

## Exigences minimales runtime

| Composant  | Version minimale requise | Justification                                   |
|------------|--------------------------|-------------------------------------------------|
| Node.js    | >= 20.19.0 LTS           | Exigence Angular CLI 21.x (LTS 20 ou LTS 22)   |
| Node.js    | >= 22.0.0 (recommandé)   | LTS actif au 2026-03-16, aligné @types/node 22  |
| npm        | >= 10.x                  | Inclus avec Node 20/22 LTS                      |
| TypeScript | >= 5.9.x                 | Contraint par ~5.9.3 dans package.json          |

> **Vérification Node.js** : Angular 21 requiert Node.js `^20.19.0 || ^22.0.0`.
> La présence de `@types/node ^22.13.0` confirme que Node 22 est la cible de développement.

---

## Choix techniques notables

### Standalone components
Tous les composants utilisent l'API standalone (pas de `NgModule`). Prérequis : Angular 14+, stable depuis Angular 17.

### Control flow syntax (@if, @for)
Le template HTML utilise la syntaxe de flux de contrôle native (`@if`, `@for`, `@else if`). Prérequis : Angular 17+.

### HttpClient fonctionnel
`provideHttpClient()` est utilisé dans `app.config.ts` (API fonctionnelle, sans `HttpClientModule`). Prérequis : Angular 15+.

### ChangeDetectionStrategy.OnPush
Tous les composants déclarent `OnPush`. Compatible Angular 21 sans restriction.

### Vitest (pas Karma/Jasmine)
Le projet utilise `vitest ^4.1.0` + `jsdom ^26.0.0` à la place de Karma. Le script `test` invoque `ng test --watch=false`, qui doit être configuré pour déléguer à Vitest.

> **Point d'attention** : vérifier que `angular.json` configure bien le builder Vitest pour le target `test`. Si `@angular/build` est configuré avec `karma`, les tests ne s'exécuteront pas.

---

## Ce qui est explicitement exclu

| Fonctionnalité           | Statut         | Raison                                          |
|--------------------------|----------------|-------------------------------------------------|
| NgModule                 | Exclu          | API standalone retenue                          |
| Karma / Jasmine          | Exclu          | Remplacé par Vitest                             |
| Server-Side Rendering    | Hors périmètre | Non requis pour ce cockpit dev-tool             |
| @angular/material        | Hors périmètre | CSS custom retenu pour le cockpit               |

---

## Sources officielles

- Angular 21 release notes : https://angular.dev/reference/releases
- Node.js compatibility : https://angular.dev/reference/versions
- TypeScript compatibility : https://angular.dev/reference/versions

> Date de vérification : **2026-03-16**. Revérifier avant tout upgrade Angular ou changement de Node LTS.
