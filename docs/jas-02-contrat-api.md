# JAS-02 - Contrat d'API Initial du Workbench

## Metadonnees

| Champ        | Valeur            |
|--------------|-------------------|
| Story        | JAS-02            |
| Epic         | JAS-EPIC-01       |
| Agent auteur | api-contrats      |
| Date         | 2026-03-16        |
| Version API  | v1                |
| Statut       | STABILISE         |

---

## Endpoint de synthese

### GET /api/v1/workbench/overview

**Responsabilite** : Retourner la vue de synthese du workbench, incluant les
informations produit, les lots de refactoring et les agents impliques.

**Methode HTTP** : GET
**URL** : `/api/v1/workbench/overview`
**Authentification** : a definir par l'agent `securite` (hors perimetre JAS-02)
**Content-Type reponse** : `application/json`

#### Codes HTTP

| Code | Signification                                    |
|------|--------------------------------------------------|
| 200  | Succes - corps JSON conforme a WorkbenchOverview |
| 500  | Erreur interne non anticipee                     |

> Les codes 401 et 403 seront ajoutes apres arbitrage securite.

---

## Schema de reponse

### WorkbenchOverview (racine)

Correspond au record Java `WorkbenchOverviewResponse` et a l'interface TypeScript `WorkbenchOverview`.

| Champ          | Type             | Nullable | Description                                        |
|----------------|------------------|----------|----------------------------------------------------|
| productName    | string           | non      | Nom du produit audite                              |
| summary        | string           | non      | Description generale du workbench                  |
| frontendTarget | string           | non      | Version cible du frontend (ex: "Angular 21.x")     |
| backendTarget  | string           | non      | Version cible du backend (ex: "Spring Boot 4.0.3") |
| lots           | RefactoringLot[] | non      | Liste des lots de refactoring (vide si aucun)      |
| agents         | AgentOverview[]  | non      | Liste des agents impliques (vide si aucun)         |

### RefactoringLot

Correspond au record Java `RefactoringLotResponse` et a l'interface TypeScript `RefactoringLot`.

| Champ          | Type    | Nullable | Description                                 |
|----------------|---------|----------|---------------------------------------------|
| number         | integer | non      | Numero ordinal du lot (commence a 1)        |
| title          | string  | non      | Intitule court du lot                       |
| objective      | string  | non      | Objectif fonctionnel du lot                 |
| primaryOutcome | string  | non      | Resultat principal attendu a l'issue du lot |

### AgentOverview

Correspond au record Java `AgentOverviewResponse` et a l'interface TypeScript `AgentOverview`.

| Champ          | Type   | Nullable | Description                                        |
|----------------|--------|----------|----------------------------------------------------|
| id             | string | non      | Identifiant technique de l'agent (slug kebab-case) |
| label          | string | non      | Libelle lisible de l'agent                         |
| responsibility | string | non      | Resume de la responsabilite de l'agent             |
| preferredModel | string | non      | Modele Claude recommande (opus / sonnet / haiku)   |

---

## Exemple de reponse JSON

```json
{
  "productName": "JavaFXAuditStudio",
  "summary": "Plateforme d'audit et de refactoring progressif de controllers JavaFX/Spring.",
  "frontendTarget": "Angular 21.x",
  "backendTarget": "Spring Boot 4.0.3 / JDK 21",
  "lots": [
    {
      "number": 1,
      "title": "Separation des responsabilites",
      "objective": "Identifier et isoler la logique metier dans les controllers",
      "primaryOutcome": "Controllers alleges, services extraits"
    }
  ],
  "agents": [
    {
      "id": "api-contrats",
      "label": "Agent API Contrats",
      "responsibility": "Definition et stabilisation des contrats d'echange entre frontend, backend et moteur",
      "preferredModel": "opus"
    }
  ]
}
```

---

## Responsabilites backend / frontend

### Backend (agent `backend-hexagonal`)

- Implementer `GetWorkbenchOverviewUseCase` dans la couche `application`.
- Le domaine ne connait pas les DTOs REST.
- Le mapper `WorkbenchOverviewResponseMapper` traduit les objets domaine vers les records DTO.
- Les records DTO ne contiennent aucune logique metier.
- Les listes `lots` et `agents` sont toujours serialisees comme tableaux JSON (jamais `null`).
- Les constructeurs canoniques compacts garantissent le non-null des champs obligatoires.

### Frontend (agent `frontend-angular`)

- Consommer l'endpoint via `WorkbenchApiService.loadOverview()` retournant `Observable<WorkbenchOverview>`.
- Traiter `lots` et `agents` comme des tableaux potentiellement vides, jamais `null`.
- Ne pas implementer de logique de refactoring ou de classification cote Angular.
- Utiliser les interfaces TypeScript comme seul contrat de typage : `WorkbenchOverview`, `RefactoringLot`, `AgentOverview`.
- Aucune transformation de nommage n'est necessaire (camelCase natif Jackson aligne avec TypeScript).

---

## Conventions d'echange

| Convention             | Valeur retenue                                       |
|------------------------|------------------------------------------------------|
| Serialisation          | JSON camelCase (Jackson natif Spring Boot)           |
| Versioning             | Prefixe URL `/api/v1/`                               |
| Listes vides           | `[]` - jamais `null`                                 |
| Champs obligatoires    | Valides par constructeur canonique compact du record |
| Encodage               | UTF-8                                                |
| Negociation de contenu | `application/json` uniquement pour ce lot            |

---

## Invariants du contrat

1. L'URL `/api/v1/workbench/overview` est stable pour tout le lot JAS-EPIC-01.
2. Aucun champ existant ne peut etre supprime ou renomme sans incrementer la version d'API (`/v2/`).
3. Des champs peuvent etre ajoutes en v1 si leur absence ne rompt pas les consommateurs existants.
4. Le corps de reponse ne contiendra jamais `null` pour une valeur de liste.

---

## Verifications de conformite

- [x] Les trois records Java utilisent un constructeur canonique compact avec gardes non-null
- [x] `lots` et `agents` serialises en `[]` quand vides, jamais en `null`
- [x] Le modele TypeScript a les memes champs que les records Java (camelCase aligne)
- [x] `WorkbenchApiService` appelle `/api/v1/workbench/overview` en GET
- [ ] `WorkbenchController` n'implemente aucune logique metier (a verifier)
- [x] Aucun DTO ne contient de dependance Spring, JPA ou logique domaine
