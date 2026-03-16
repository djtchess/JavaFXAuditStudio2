# Backlog Phase 2 — JavaFXAuditStudio2

## Contrat inter-agent (structure contracts.md)

### objectif

Transformer le scope cadre des lots A a F en epics, stories, tasks et sous-taches
estimables, structures en lots progressifs executables, avec dependances, risques
et criteres d'acceptation visibles.

### perimetre

Tickets JAS-53 a JAS-120 (numerotation depuis JAS-52 inclus comme dernier ticket phase 1).
Lots A a F : endpoints REST d'analyse, persistence PostgreSQL, implementeurs reels
des analyseurs, ecrans Angular du parcours, orchestration pipeline, tests d'integration.
Hors perimetre : ce qui est deja implemente (stubs, domaine, ports, dashboard, JAS-01 a JAS-52).

### faits

- Domaine hexagonal complet en stubs : ingestion, cartographie, regles, migration, generation, restitution.
- `AnalysisSession` et `AnalysisSessionPort` existent dans le domaine, sans adapter JPA.
- Aucun endpoint REST n'expose les use cases d'analyse (uniquement `GET /api/v1/workbench/overview`).
- Le frontend Angular expose un `DashboardComponent` et un `WorkbenchApiService`.
- Le parcours utilisateur est formalise dans `docs/jas-01-parcours-utilisateur.md`.
- Le schema conceptuel SQL est reference dans `db/schema-conceptuel.sql` (non encore cree dans le repo).
- Les 4 stubs a remplacer : `StubCartographyAnalysisAdapter`, `StubRuleExtractionAdapter`,
  `StubMigrationPlannerAdapter`, `StubCodeGenerationAdapter`.

### interpretations

- Les lots A, B et E forment le socle backend sans lequel les lots C, D et F ne peuvent pas
  etre integres ni testes de bout en bout.
- Le lot B (JPA) doit preceder le lot E (orchestration) car l'orchestrateur doit persister
  les etats de session.
- Le lot C (analyseurs reels) peut commencer en parallele du lot A apres stabilisation
  des contrats de port, puisqu'il ne touche qu'aux adapters `out`.
- Le lot D (Angular) peut demarrer sur maquette avec les endpoints stub du lot A,
  sans attendre le lot C.
- Le lot F (tests d'integration) necessite que les lots A, B, C et E soient au moins
  partiellement livres.

### hypotheses

- Le format de soumission des sources est un payload JSON avec chemins de fichiers
  (type `FilesystemSourceReaderAdapter` deja en place).
- L'analyse est synchrone dans un premier temps (pas de WebSocket, pas de SSE).
- La session d'analyse est identifiee par un UUID genere cote backend.
- Les analyseurs reels utilisent uniquement l'API standard JDK (pas de bibliotheque externe
  de parsing Java dans le lot C2 pour ce lot).
- PostgreSQL est disponible en environnement de developpement via Docker Compose
  (a confirmer par `devops-ci-cd`).

### incertitudes

- Le schema SQL exact de `db/schema-conceptuel.sql` n'est pas encore cree ; le lot B
  doit inclure sa creation.
- Le niveau de detail des artefacts generes (squelettes compilables vs. code vide)
  reste a arbitrer par `implementation-moteur-analyse`.
- La gestion de l'asynchronisme de l'analyse (polling du statut) est retenue comme
  synchrone pour la phase 2 ; a revoir si les temps de traitement l'exigent.
- La version exacte de Flyway compatible avec Spring Boot 4.0.3 est a verifier.

### decisions

- La numerotation commence a JAS-53.
- Les epics suivent la numerotation JAS-EPIC-07 a JAS-EPIC-12.
- Chaque epic correspond a un lot (A a F).
- Les stories sont granulaires (max 8 SP) ; au-dela, elles sont eclates en sous-stories.
- Les dependances inter-lots sont rendues explicites par les IDs de tickets.
- L'estimation suit la suite de Fibonacci : 1, 2, 3, 5, 8, 13.

### livrables

- `jira/backlog-phase-2.md` (le present document).

### dependances

- JAS-52 (dernier ticket phase 1, observabilite) : prerequis pour tous les tickets ci-dessous.
- `docs/jas-01-parcours-utilisateur.md` : reference fonctionnelle pour le lot D.
- `agents/contracts.md` : structure de sortie inter-agent respectee.

### verifications

- Chaque ticket a un ID, un titre, une epic, une description, des criteres d'acceptation,
  une estimation, des dependances, des risques et un agent lead.
- Les dependances inter-tickets sont coherentes avec la logique d'implementation progressive.
- Aucun ticket ne reticketise ce qui est deja implemente.

### handoff

```text
handoff:
  vers: gouvernance
  preconditions:
    - backlog-phase-2.md produit et lisible
    - dependances inter-lots verifiees
  points_de_vigilance:
    - valider que le lot A ne casse pas l'hexagone existant
    - verifier la compatibilite Flyway / Spring Boot 4.0.3 avant lot B
    - s'assurer que le lot C2 n'introduit pas de bibliotheque de parsing Java non approuvee
  artefacts_a_consulter:
    - jira/backlog-phase-2.md
    - agents/orchestration.md
    - docs/jas-01-parcours-utilisateur.md

  vers: backend-hexagonal
  preconditions:
    - backlog-phase-2.md valide par gouvernance
    - contrats de port existants non modifies sans accord
  points_de_vigilance:
    - le lot A cree des adapters REST in et des services application nouveaux
    - le lot B introduit JPA dans adapters/out uniquement
    - le lot E cree un use case d'orchestration dans application/service
  artefacts_a_consulter:
    - jira/backlog-phase-2.md (lot A, lot B, lot E)
    - backend/src/main/java/ff/ss/javaFxAuditStudio/

  vers: frontend-angular
  preconditions:
    - lot A (endpoints REST) au moins partiellement livre
    - contrats DTO documentes
  points_de_vigilance:
    - quatre ecrans Angular a creer (lot D)
    - routing Angular a etendre
    - aucune logique experte de refactoring dans les composants
  artefacts_a_consulter:
    - jira/backlog-phase-2.md (lot D)
    - docs/jas-01-parcours-utilisateur.md

  vers: implementation-moteur-analyse
  preconditions:
    - ports CartographyAnalysisPort, RuleExtractionPort, MigrationPlannerPort, CodeGenerationPort stabilises
    - lot A livre (les adapters out peuvent etre branches)
  points_de_vigilance:
    - lot C remplace les stubs sans changer les ports
    - le parsing FXML utilise javax.xml standard JDK uniquement
    - le parsing Java utilise analyse textuelle/regex dans ce lot
  artefacts_a_consulter:
    - jira/backlog-phase-2.md (lot C)
    - backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/analysis/
```

---

## Resume des epics et estimation macro

| Epic        | Lot | Titre                                         | SP total |
|-------------|-----|-----------------------------------------------|----------|
| JAS-EPIC-07 | A   | Endpoints REST d'analyse                      | 34       |
| JAS-EPIC-08 | B   | Persistence PostgreSQL (adapter JPA)          | 21       |
| JAS-EPIC-09 | C   | Implementeurs reels des analyseurs            | 42       |
| JAS-EPIC-10 | D   | Ecrans Angular parcours analyse               | 34       |
| JAS-EPIC-11 | E   | Orchestration pipeline bout-en-bout           | 21       |
| JAS-EPIC-12 | F   | Tests d'integration du pipeline               | 21       |
| **Total**   |     |                                               | **173**  |

Prerequis communs a tous les epics : JAS-52 (observabilite, phase 1).

---

## Epic JAS-EPIC-07 — Endpoints REST d'analyse (Lot A)

**Lead agent** : `backend-hexagonal`
**Objectif** : Exposer les use cases d'analyse via des endpoints REST conformes a
l'architecture hexagonale. Chaque endpoint dispose d'un DTO REST, d'un mapper
domaine vers DTO, et s'appuie sur le use case correspondant.

**Dependances de l'epic** : JAS-52

---

### JAS-53 — Cadrer les contrats DTO et mappers pour le lot A

**Epic** : JAS-EPIC-07
**Description** :
Avant de creer les controllers REST, definir les DTO de requete et de reponse pour
les 7 endpoints du lot A, et les interfaces de mapper domaine vers DTO. Ce ticket
produit les fichiers Java vides (interfaces et records DTO) sans logique.

Fichiers a creer dans `adapters/in/rest/dto/` :
- `SubmitAnalysisRequest` (record) : `List<String> sourceFilePaths`, `String sessionName`
- `AnalysisSessionResponse` (record) : `String sessionId`, `String status`, `String sessionName`
- `CartographyResponse` (record) : mapper de `ControllerCartography`
- `ClassificationResponse` (record) : mapper de `ClassificationResult`
- `MigrationPlanResponse` (record) : mapper de `MigrationPlan`
- `ArtifactsResponse` (record) : mapper de `GenerationResult`
- `RestitutionReportResponse` (record) : mapper de `RestitutionReport`

Fichiers a creer dans `adapters/in/rest/mapper/` :
- Interfaces de mapper (une par domaine -> DTO) avec signature uniquement.

**Criteres d'acceptation** :
- Tous les records DTO compilent sans dependance Spring ni domaine JPA.
- Les champs des DTO correspondent aux objets domaine existants.
- Les interfaces de mapper sont declarees avec les signatures `map(DomainObject) -> DtoType`.
- Aucune logique de mapping n'est implementee dans ce ticket.
- Un test de compilation passe (`mvn compile`).

**Story points** : 3
**Dependances** : JAS-52
**Risques** : Risque de sur-specification si les objets domaine evoluent avant la livraison du lot A. Garder les DTO minimalistes.
**Agent lead** : `backend-hexagonal`

---

### JAS-54 — Implementer les mappers domaine -> DTO pour le lot A

**Epic** : JAS-EPIC-07
**Description** :
Implementer les classes de mapper declarees dans JAS-53. Chaque mapper est une
classe dans `adapters/in/rest/mapper/` implementant l'interface correspondante,
sans logique Spring (pas de `@Component`, assemblage en configuration).

Mappers a implementer :
- `AnalysisSessionResponseMapper` : `AnalysisSession` -> `AnalysisSessionResponse`
- `CartographyResponseMapper` : `ControllerCartography` -> `CartographyResponse`
- `ClassificationResponseMapper` : `ClassificationResult` -> `ClassificationResponse`
- `MigrationPlanResponseMapper` : `MigrationPlan` -> `MigrationPlanResponse`
- `ArtifactsResponseMapper` : `GenerationResult` -> `ArtifactsResponse`
- `RestitutionReportResponseMapper` : `RestitutionReport` -> `RestitutionReportResponse`

**Criteres d'acceptation** :
- Chaque mapper a des tests unitaires couvrant le cas nominal et le cas objet vide/null-safe.
- Les mappers ne dependent pas de Spring, JPA ni d'autres adapters.
- Les mappers sont enregistres dans leurs classes `@Configuration` respectives.
- `mvn test` passe sur les tests des mappers.

**Story points** : 5
**Dependances** : JAS-53
**Risques** : Risque de champs domaine mal mappes si les objets domaine ont des types complexes. Verifier `ControllerCartography` (listes de `FxmlComponent` et `HandlerBinding`).
**Agent lead** : `backend-hexagonal`

---

### JAS-55 — Creer le use case et service de soumission de session d'analyse

**Epic** : JAS-EPIC-07
**Description** :
Creer le port entrant `SubmitAnalysisUseCase` et son service `SubmitAnalysisService`
dans `application/`. Ce service orchestre : creation de `AnalysisSession` avec UUID
genere, appel de `IngestSourcesUseCase`, persistence via `AnalysisSessionPort`,
retour de la session creee.

Fichiers a creer :
- `application/ports/in/SubmitAnalysisUseCase.java` : interface avec `submit(List<String> paths, String name) : AnalysisSession`
- `application/service/SubmitAnalysisService.java` : implementation

Note : `AnalysisSessionPort` existe mais sans adapter JPA (lot B). Ce ticket utilise
un stub en memoire (Map) pour la persistence, a remplacer par JAS-66.

**Criteres d'acceptation** :
- `SubmitAnalysisUseCase` est une interface dans `application/ports/in/`.
- `SubmitAnalysisService` implementee sans dependance Spring directe dans le constructeur (injection via configuration).
- Une `AnalysisSession` avec statut `CREATED` est retournee.
- Un UUID est genere pour `sessionId`.
- Tests unitaires du service avec mock de `IngestSourcesUseCase` et `AnalysisSessionPort`.
- `mvn test` passe.

**Story points** : 5
**Dependances** : JAS-52
**Risques** : Couplage premature avec la persistence si le stub est trop specifique. Utiliser l'interface `AnalysisSessionPort` uniquement.
**Agent lead** : `backend-hexagonal`

---

### JAS-56 — Endpoint POST /api/v1/analysis/sessions

**Epic** : JAS-EPIC-07
**Description** :
Creer `AnalysisController` dans `adapters/in/rest/` avec l'endpoint
`POST /api/v1/analysis/sessions`.

- Corps de requete : `SubmitAnalysisRequest`
- Reponse succes : `201 Created` avec `AnalysisSessionResponse`
- Reponse erreur : 400 si la liste de chemins est vide, 500 sur erreur d'ingestion

Sous-taches :
1. Creer `AnalysisController` avec le seul endpoint POST.
2. Utiliser `SubmitAnalysisUseCase` (JAS-55) et `AnalysisSessionResponseMapper` (JAS-54).
3. Enregistrer le controller et ses dependances dans `AnalysisConfiguration`.
4. Ajouter un log debug avec correlation ID (conformement a JAS-52).

**Criteres d'acceptation** :
- `POST /api/v1/analysis/sessions` repond `201` avec `sessionId` non null.
- `POST` avec liste vide repond `400`.
- Le controller ne contient aucune logique metier (delegation stricte au use case).
- Un test d'integration Spring Boot MockMvc valide les codes HTTP.
- Le log debug est produit avec le correlation ID du `CorrelationFilter`.

**Story points** : 3
**Dependances** : JAS-54, JAS-55
**Risques** : `RestExceptionHandler` doit couvrir les erreurs d'ingestion ; verifier la couverture existante.
**Agent lead** : `backend-hexagonal`

---

### JAS-57 — Endpoint GET /api/v1/analysis/sessions/{sessionId}

**Epic** : JAS-EPIC-07
**Description** :
Ajouter a `AnalysisController` l'endpoint `GET /api/v1/analysis/sessions/{sessionId}`.

- Reponse succes : `200 OK` avec `AnalysisSessionResponse`
- Reponse erreur : `404 Not Found` si la session n'existe pas

Creer le use case `GetAnalysisSessionUseCase` et son service `GetAnalysisSessionService`
dans `application/`. Le service interroge `AnalysisSessionPort`.

**Criteres d'acceptation** :
- `GET /api/v1/analysis/sessions/{id}` repond `200` avec le statut de la session.
- `GET` avec ID inconnu repond `404`.
- `GetAnalysisSessionUseCase` est une interface dans `application/ports/in/`.
- Tests unitaires du service avec mock de `AnalysisSessionPort`.
- Test MockMvc valide 200 et 404.

**Story points** : 3
**Dependances** : JAS-54, JAS-55, JAS-56
**Risques** : Le stub en memoire de JAS-55 doit exposer une methode `findById` coherente avec `AnalysisSessionPort`.
**Agent lead** : `backend-hexagonal`

---

### JAS-58 — Endpoint GET /api/v1/analysis/sessions/{sessionId}/cartography

**Epic** : JAS-EPIC-07
**Description** :
Ajouter l'endpoint `GET /api/v1/analysis/sessions/{sessionId}/cartography`.

Creer le use case `GetCartographyUseCase` et son service `GetCartographyService`.
Le service : recupere la session, appelle `CartographyUseCase` sur les sources
de la session, retourne `CartographyResponse`.

**Criteres d'acceptation** :
- `GET /cartography` repond `200` avec une `CartographyResponse` non nulle.
- `GET /cartography` sur session inconnue repond `404`.
- `CartographyResponse` contient `fxmlComponents` et `handlerBindings` (listes).
- Tests unitaires du service avec mocks.
- Test MockMvc valide le contrat.

**Story points** : 5
**Dependances** : JAS-54, JAS-57
**Risques** : `CartographyUseCase` retourne actuellement des donnees stub ; le test accepte les donnees stub.
**Agent lead** : `backend-hexagonal`

---

### JAS-59 — Endpoint GET /api/v1/analysis/sessions/{sessionId}/classification

**Epic** : JAS-EPIC-07
**Description** :
Ajouter l'endpoint `GET /api/v1/analysis/sessions/{sessionId}/classification`.

Creer `GetClassificationUseCase` et `GetClassificationService`. Le service appelle
`ClassifyResponsibilitiesUseCase` apres avoir recupere la cartographie de la session.

**Criteres d'acceptation** :
- `GET /classification` repond `200` avec `ClassificationResponse` contenant
  `businessRules` et `extractionCandidates`.
- Tests unitaires du service.
- Test MockMvc valide le contrat.

**Story points** : 5
**Dependances** : JAS-54, JAS-58
**Risques** : Dependance a l'ordre cartographie -> classification ; le service doit gerer l'absence de cartographie en amont.
**Agent lead** : `backend-hexagonal`

---

### JAS-60 — Endpoint GET /api/v1/analysis/sessions/{sessionId}/plan

**Epic** : JAS-EPIC-07
**Description** :
Ajouter l'endpoint `GET /api/v1/analysis/sessions/{sessionId}/plan`.

Creer `GetMigrationPlanUseCase` et `GetMigrationPlanService`. Le service appelle
`ProduceMigrationPlanUseCase` en utilisant la classification disponible pour la session.

**Criteres d'acceptation** :
- `GET /plan` repond `200` avec `MigrationPlanResponse` contenant `plannedLots` (liste de 5).
- Chaque `PlannedLotResponse` contient : `lotNumber`, `title`, `objective`, `riskLevel`.
- Tests unitaires et test MockMvc.

**Story points** : 5
**Dependances** : JAS-54, JAS-59
**Risques** : Le stub produit exactement 5 lots fixes ; le test ne doit pas etre trop specifique sur le contenu textuel.
**Agent lead** : `backend-hexagonal`

---

### JAS-61 — Endpoints GET /artifacts et GET /report

**Epic** : JAS-EPIC-07
**Description** :
Ajouter les deux derniers endpoints d'interrogation de session :
- `GET /api/v1/analysis/sessions/{sessionId}/artifacts`
- `GET /api/v1/analysis/sessions/{sessionId}/report`

Creer `GetArtifactsUseCase`, `GetArtifactsService`, `GetRestitutionUseCase`,
`GetRestitutionService`.

**Criteres d'acceptation** :
- `GET /artifacts` repond `200` avec `ArtifactsResponse` (liste de `CodeArtifact`).
- `GET /report` repond `200` avec `RestitutionReportResponse` (contenu markdown non vide).
- Les 404 sont geres pour les sessions inconnues.
- Tests unitaires et MockMvc.

**Story points** : 5
**Dependances** : JAS-54, JAS-60
**Risques** : Le rapport markdown produit par `MarkdownRestitutionFormatterAdapter` doit etre expose tel quel ; verifier l'encodage UTF-8.
**Agent lead** : `backend-hexagonal`

---

## Epic JAS-EPIC-08 — Persistence PostgreSQL - Adapter JPA (Lot B)

**Lead agent** : `backend-hexagonal` + `database-postgres`
**Objectif** : Fournir un vrai adapter JPA pour `AnalysisSessionPort` afin de remplacer
le stub en memoire du lot A, et poser les bases de la persistence pour l'orchestration (lot E).

**Dependances de l'epic** : JAS-52

---

### JAS-62 — Ajouter les dependances Maven JPA, PostgreSQL, Flyway

**Epic** : JAS-EPIC-08
**Description** :
Modifier `backend/pom.xml` pour ajouter :
- `spring-boot-starter-data-jpa`
- `postgresql` (driver JDBC)
- `flyway-core`

Verifier la compatibilite avec Spring Boot 4.0.3 et JDK 21 avant ajout.
Ajouter la configuration datasource dans `application.properties` avec des
valeurs parametrables via variables d'environnement.

Sous-taches :
1. Identifier les versions compatibles de chaque dependance.
2. Modifier `pom.xml`.
3. Ajouter dans `application.properties` :
   `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`,
   `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`.
4. Verifier que `mvn compile` passe sans erreur.

**Criteres d'acceptation** :
- `pom.xml` contient les 3 nouvelles dependances avec versions explicites.
- `application.properties` expose les proprietes datasource via `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`.
- `mvn compile` passe.
- Aucune variable sensible n'est committee en dur dans le fichier de proprietes.

**Story points** : 2
**Dependances** : JAS-52
**Risques** : Incompatibilite de version Flyway avec Spring Boot 4.0.3 (verifier le changelog de Spring Boot 4.0.x).
**Agent lead** : `backend-hexagonal`

---

### JAS-63 — Creer la migration Flyway V1 et le schema SQL

**Epic** : JAS-EPIC-08
**Description** :
Creer le fichier de migration Flyway `V1__create_analysis_session.sql` dans
`backend/src/main/resources/db/migration/`.

Schema de la table `analysis_session` :
```sql
CREATE TABLE analysis_session (
    id            UUID         PRIMARY KEY,
    session_name  VARCHAR(255) NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    source_paths  TEXT         NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

`source_paths` est une colonne TEXT contenant les chemins serialises (JSON array).

Creer egalement le fichier conceptuel `db/schema-conceptuel.sql` a la racine
du projet pour documenter le schema attendu hors migration Flyway.

**Criteres d'acceptation** :
- Le fichier `V1__create_analysis_session.sql` existe dans le dossier Flyway.
- Le script est idempotent (utilise `CREATE TABLE IF NOT EXISTS`).
- `db/schema-conceptuel.sql` documente le schema avec des commentaires.
- La migration s'execute correctement sur une base PostgreSQL de test.

**Story points** : 3
**Dependances** : JAS-62
**Risques** : Le type UUID peut necessite l'extension `pgcrypto` selon la version PostgreSQL ; tester sur PostgreSQL 15+.
**Agent lead** : `database-postgres`

---

### JAS-64 — Creer l'entite JPA AnalysisSessionEntity

**Epic** : JAS-EPIC-08
**Description** :
Creer `AnalysisSessionEntity` dans `adapters/out/persistence/` avec les annotations
JPA appropriees.

L'entite doit rester dans le package `adapters/out/persistence/` et ne doit pas
etre importee dans le `domain` ou `application`. Le mapping vers `AnalysisSession`
(domaine) se fait exclusivement dans l'adapter.

Champs : `id` (UUID), `sessionName` (String), `status` (String, enum serialise),
`sourcePaths` (String, JSON), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime).

**Criteres d'acceptation** :
- `AnalysisSessionEntity` est une classe JPA annotee `@Entity` uniquement dans `adapters/out/persistence/`.
- Aucun import Spring Data ni JPA n'apparait dans `domain` ou `application`.
- La classe compile sans erreur.
- Les champs correspondent au schema Flyway de JAS-63.

**Story points** : 3
**Dependances** : JAS-63
**Risques** : Tentation d'utiliser directement l'entite JPA comme objet domaine ; interdire strictement.
**Agent lead** : `backend-hexagonal`

---

### JAS-65 — Creer AnalysisSessionRepository et JpaAnalysisSessionAdapter

**Epic** : JAS-EPIC-08
**Description** :
Creer deux composants dans `adapters/out/persistence/` :
1. `AnalysisSessionRepository extends JpaRepository<AnalysisSessionEntity, UUID>`
2. `JpaAnalysisSessionAdapter implements AnalysisSessionPort`

`JpaAnalysisSessionAdapter` :
- Injecte `AnalysisSessionRepository`.
- Convertit `AnalysisSession` (domaine) <-> `AnalysisSessionEntity` (JPA) via un mapper prive.
- Implements les methodes de `AnalysisSessionPort` : `save`, `findById`, `updateStatus`.

**Criteres d'acceptation** :
- `JpaAnalysisSessionAdapter` implemente tous les methodes de `AnalysisSessionPort`.
- La conversion domaine <-> entite est testee en isolation.
- L'adapter est enregistre dans une classe `@Configuration` (pas de `@Component` direct
  si la configuration est centralisee dans `PersistenceConfiguration`).
- Un test d'integration `@DataJpaTest` valide les operations CRUD sur `H2` ou `Testcontainers PostgreSQL`.

**Story points** : 5
**Dependances** : JAS-64
**Risques** : La serialisation de `sourcePaths` en JSON dans un champ TEXT peut etre fragile ; utiliser `ObjectMapper` ou simple `String.join`.
**Agent lead** : `backend-hexagonal`

---

### JAS-66 — Brancher JpaAnalysisSessionAdapter dans la configuration

**Epic** : JAS-EPIC-08
**Description** :
Creer `PersistenceConfiguration` dans `configuration/` qui enregistre
`JpaAnalysisSessionAdapter` comme bean `AnalysisSessionPort`, remplacant le stub
en memoire introduit dans le lot A (JAS-55).

Supprimer le stub en memoire de `SubmitAnalysisService` et verifier que
tous les services qui dependent de `AnalysisSessionPort` fonctionnent
avec l'implementation JPA.

**Criteres d'acceptation** :
- `AnalysisSessionPort` est satisfait par `JpaAnalysisSessionAdapter` en contexte Spring.
- Le stub en memoire est supprime ou extrait en profil `test` uniquement.
- Les tests MockMvc du lot A (JAS-56 et JAS-57) continuent de passer (adaptes si besoin avec Testcontainers ou H2).
- `mvn test` passe.

**Story points** : 3
**Dependances** : JAS-55, JAS-57, JAS-65
**Risques** : Risque de regression sur les tests MockMvc si la datasource n'est pas configuree en contexte de test.
**Agent lead** : `backend-hexagonal`

---

### JAS-67 — Configuration Docker Compose pour PostgreSQL de developpement

**Epic** : JAS-EPIC-08
**Description** :
Creer un fichier `docker-compose.yml` a la racine du projet pour lancer PostgreSQL
en environnement de developpement local. Documenter dans `README.md` (ou dans un
fichier `docs/dev-setup.md`) la procedure de demarrage.

Services :
- `postgres` : image `postgres:15`, port `5432`, variables d'environnement `POSTGRES_DB`,
  `POSTGRES_USER`, `POSTGRES_PASSWORD`.

**Criteres d'acceptation** :
- `docker-compose up -d` lance PostgreSQL accessible sur `localhost:5432`.
- Les variables de connexion correspondent a celles de `application.properties`.
- Un fichier `docs/dev-setup.md` documente la procedure de demarrage.
- Aucune donnee sensible n'est committee (utiliser `.env.example`).

**Story points** : 2
**Dependances** : JAS-62
**Risques** : Divergence entre la version PostgreSQL locale et celle de production (a aligner avec `devops-ci-cd`).
**Agent lead** : `devops-ci-cd`

---

### JAS-68 — Migration Flyway V2 : champs de resultats intermediaires

**Epic** : JAS-EPIC-08
**Description** :
Creer `V2__add_analysis_results.sql` pour stocker les resultats intermediaires
du pipeline (cartographie, classification, plan, artefacts).

Colonnes a ajouter dans `analysis_session` :
- `cartography_json TEXT` — serialisation JSON de `ControllerCartography`
- `classification_json TEXT` — serialisation JSON de `ClassificationResult`
- `migration_plan_json TEXT` — serialisation JSON de `MigrationPlan`
- `generation_result_json TEXT` — serialisation JSON de `GenerationResult`
- `restitution_report_json TEXT` — serialisation JSON de `RestitutionReport`

Ces colonnes sont nullable pour permettre une persistance progressive au fil du pipeline.

**Criteres d'acceptation** :
- `V2__add_analysis_results.sql` s'execute sans erreur sur une base ayant deja le schema V1.
- Les colonnes sont nullable.
- `AnalysisSessionEntity` est mis a jour avec les nouveaux champs.
- `JpaAnalysisSessionAdapter` expose des methodes `saveCartography`, `saveClassification`,
  `saveMigrationPlan`, `saveGenerationResult`, `saveRestitutionReport`.
- Tests `@DataJpaTest` pour les nouvelles methodes.

**Story points** : 3
**Dependances** : JAS-65
**Risques** : Colonnes JSON en TEXT : serialisation/deserialisation a gerer proprement ; utiliser `ObjectMapper` injecte.
**Agent lead** : `database-postgres`

---

## Epic JAS-EPIC-09 — Implementeurs reels des analyseurs (Lot C)

**Lead agent** : `implementation-moteur-analyse`
**Objectif** : Remplacer les 4 stubs d'analyse par des implementations reelles
basees sur des parseurs legers, sans changer les ports ni le domaine.

**Dependances de l'epic** : JAS-52, JAS-56 (lot A minimal pour tester l'integration)

---

### JAS-69 — Analyseur FXML reel (C1)

**Epic** : JAS-EPIC-09
**Description** :
Creer `FxmlCartographyAnalysisAdapter` dans `adapters/out/analysis/` remplacant
`StubCartographyAnalysisAdapter`. Implementer `CartographyAnalysisPort`.

L'adapter utilise `javax.xml.parsers.DocumentBuilder` (API standard JDK 21) pour :
1. Lire le contenu FXML (String) et le parser en `Document` XML.
2. Extraire tous les elements avec attribut `fx:id` -> `FxmlComponent` (id, componentType).
3. Extraire les attributs `onAction` et leurs valeurs -> `HandlerBinding` (fxId, handlerMethod).
4. Identifier les inconnues (`CartographyUnknown`) si le FXML est malformed.

L'adapter ne doit pas appeler de services Spring ni de bases de donnees.

Sous-taches :
1. Implementer le parsing XML avec `javax.xml`.
2. Implementer l'extraction `fx:id`.
3. Implementer l'extraction `onAction`.
4. Gerer les cas d'erreur (FXML vide, XML invalide).

**Criteres d'acceptation** :
- `FxmlCartographyAnalysisAdapter` implemente `CartographyAnalysisPort`.
- Les tests unitaires couvrent : FXML avec 3 composants, FXML vide, FXML malformed.
- Les `FxmlComponent` retournes ont `fxId` et `componentType` non nuls.
- Les `HandlerBinding` retournes ont `handlerMethod` correspondant aux valeurs `onAction`.
- `StubCartographyAnalysisAdapter` est conserve comme bean de profil `stub` (ne pas supprimer).
- `CartographyConfiguration` est mis a jour pour utiliser le vrai adapter.

**Story points** : 8
**Dependances** : JAS-52
**Risques** : Variabilite du format FXML (namespaces XML, encoding). Tester avec des FXML JavaFX reels en fixtures.
**Agent lead** : `fxml-analysis`

---

### JAS-70 — Fixtures FXML et Java pour les tests

**Epic** : JAS-EPIC-09
**Description** :
Creer des fichiers de test dans `backend/src/test/resources/fixtures/` :
- `SampleController.java` : controller JavaFX fictif avec `@FXML`, handlers, injections Spring.
- `SampleView.fxml` : FXML correspondant avec `fx:id` et `onAction`.
- `ComplexController.java` : controller avec plus de 10 handlers et 3 services injectes.
- `ComplexView.fxml` : FXML correspondant plus riche.

Ces fixtures sont utilises par les tests des analyseurs C1, C2, C3, C4 et les tests
d'integration du lot F.

**Criteres d'acceptation** :
- `SampleController.java` contient au minimum : 3 champs `@FXML`, 2 services `@Autowired`, 3 methodes handler.
- `SampleView.fxml` est un FXML JavaFX valide avec 3 `fx:id` et 2 `onAction`.
- `ComplexController.java` contient au minimum : 10 champs `@FXML`, 4 services injectes, 8 handlers.
- Les fixtures compilent en isolation (pas de dependances JavaFX reelles, juste du texte Java).
- Les fixtures sont documentees avec des commentaires expliquant leur role.

**Story points** : 3
**Dependances** : JAS-52
**Risques** : Les fixtures Java ne doivent pas etre compilees dans le classpath de production ; les placer dans `src/test/resources` uniquement.
**Agent lead** : `implementation-moteur-analyse`

---

### JAS-71 — Analyseur Java controller reel (C2)

**Epic** : JAS-EPIC-09
**Description** :
Creer `JavaControllerRuleExtractionAdapter` dans `adapters/out/analysis/` remplacant
`StubRuleExtractionAdapter`. Implementer `RuleExtractionPort`.

L'adapter utilise une analyse textuelle par regex sur le contenu Java source (String) :
1. Detecter les champs annotes `@FXML` -> extraire le nom et le type.
2. Detecter les champs annotes `@Autowired` ou `@Inject` -> identifier les dependances.
3. Detecter les methodes handler (`@FXML` sur methode, prefixe `on`, retour `void`).
4. Classifier chaque methode selon `ResponsibilityClass` (heuristiques textuelles) :
   - Corps contenant `service.save`, `repository`, `persist` -> `APPLICATION` ou `BUSINESS`
   - Corps contenant appels UI (`setText`, `setVisible`, `getChildren`) -> `UI`
   - Corps contenant appels REST (`restTemplate`, `webClient`) -> `TECHNICAL`
5. Identifier les candidats `ExtractionCandidate` (Policy si decision, UseCase si action).

Sous-taches :
1. Implementer les regex de detection des annotations.
2. Implementer la classification par mots-cles.
3. Implementer la construction de `ClassificationResult`.
4. Gerer les cas d'erreur (source vide, classe non parseable).

**Criteres d'acceptation** :
- `JavaControllerRuleExtractionAdapter` implemente `RuleExtractionPort`.
- Tests unitaires avec `SampleController.java` (fixture JAS-70) : au moins 3 `BusinessRule` extraites.
- La classification distingue `UI`, `APPLICATION`, `BUSINESS`, `TECHNICAL`.
- Les candidats `ExtractionCandidate` identifient les handlers lourds.
- `StubRuleExtractionAdapter` est conserve comme bean de profil `stub`.
- `ClassificationConfiguration` est mis a jour.

**Story points** : 13
**Dependances** : JAS-70
**Risques** : L'analyse textuelle par regex est fragile sur du code reformate ; documenter les limites explicitement. Ne pas pretendre a une precision de compilateur.
**Agent lead** : `controller-analysis`

---

### JAS-72 — Planificateur de migration reel (C3)

**Epic** : JAS-EPIC-09
**Description** :
Creer `RealMigrationPlannerAdapter` dans `adapters/out/analysis/` remplacant
`StubMigrationPlannerAdapter`. Implementer `MigrationPlannerPort`.

L'adapter prend `ClassificationResult` en entree et genere un `MigrationPlan`
avec 5 `PlannedLot` bases sur les candidats d'extraction identifies :
- Lot 1 : diagnostic et cible (toujours genere avec les metriques de classification).
- Lot 2 : candidats `ViewModel` et premiers `UseCase`.
- Lot 3 : handlers lourds (`BUSINESS`, `APPLICATION`) -> migration effective.
- Lot 4 : candidats `Gateway` (appels REST, IO) -> adaptateurs.
- Lot 5 : candidats `Assembler`, `Strategy`.

Le `RiskLevel` de chaque lot est calcule selon le nombre de candidats et leur complexite.

**Criteres d'acceptation** :
- `RealMigrationPlannerAdapter` implemente `MigrationPlannerPort`.
- Les 5 lots sont toujours generes (meme si un lot est vide de candidats).
- Le `RiskLevel` varie selon le nombre de candidats (LOW < 3, MEDIUM < 7, HIGH >= 7).
- Tests unitaires avec un `ClassificationResult` de 5 candidats et de 10 candidats.
- `MigrationPlanConfiguration` est mis a jour.

**Story points** : 8
**Dependances** : JAS-71
**Risques** : Les heuristiques de risque sont arbitraires ; les documenter comme telles et les rendre configurables.
**Agent lead** : `implementation-moteur-analyse`

---

### JAS-73 — Generateur d'artefacts reel (C4)

**Epic** : JAS-EPIC-09
**Description** :
Creer `RealCodeGenerationAdapter` dans `adapters/out/analysis/` remplacant
`StubCodeGenerationAdapter`. Implementer `CodeGenerationPort`.

L'adapter prend `MigrationPlan` en entree et genere des `CodeArtifact` (squelettes
compilables) pour chaque `PlannedLot` :
- ViewModel : classe Java avec proprietes `ObjectProperty<T>` pour chaque champ d'etat identifie.
- UseCase : interface Java avec methode `execute(Input) : Output`.
- Policy : classe Java avec methode `decide(Context) : Decision`.
- Gateway : interface Java avec methodes correspondant aux appels externes.
- Assembler : classe Java avec methode `assemble(FormData) : DomainObject`.

Les squelettes contiennent des commentaires `// TODO : implementer` et les
imports Java corrects mais aucune logique metier.

Sous-taches :
1. Implementer la generation de squelette ViewModel.
2. Implementer la generation de squelette UseCase.
3. Implementer la generation de squelette Policy.
4. Implementer la generation de squelette Gateway.
5. Implementer la generation de squelette Assembler.

**Criteres d'acceptation** :
- `RealCodeGenerationAdapter` implemente `CodeGenerationPort`.
- Pour chaque `PlannedLot`, au moins un `CodeArtifact` est produit.
- Les squelettes generes sont syntaxiquement valides Java 21 (contenu text verifie).
- Le type `ArtifactType` est correctement assigne a chaque artefact.
- Tests unitaires avec un `MigrationPlan` de 5 lots.
- Les bridges transitoires sont marques avec un commentaire `// BRIDGE TRANSITOIRE`.
- `GenerationConfiguration` est mis a jour.

**Story points** : 8
**Dependances** : JAS-72
**Risques** : La generation de code syntaxiquement valide sans compilateur est fragile ; limiter les squelettes a des templates simples et testes.
**Agent lead** : `implementation-moteur-analyse`

---

### JAS-74 — Adapter MarkdownRestitutionFormatterAdapter au pipeline reel

**Epic** : JAS-EPIC-09
**Description** :
`MarkdownRestitutionFormatterAdapter` existe mais produit un rapport generique.
L'adapter doit etre enrichi pour inclure dans le rapport markdown :
- Un resume executif avec les metriques de classification.
- La liste des 5 lots avec leurs risques.
- La liste des artefacts generes par lot.
- Les inconnues signalees (`CartographyUnknown`).

**Criteres d'acceptation** :
- Le rapport markdown produit contient les 4 sections : resume, lots, artefacts, inconnues.
- Tests unitaires avec un `RestitutionReport` complet (tous champs non nuls).
- Le rapport est UTF-8 et ne depasse pas 50 ko pour un controller de 500 lignes.

**Story points** : 2
**Dependances** : JAS-73
**Risques** : Le rapport peut etre tronque si les listes de candidats sont tres longues ; prevoir une limite configurable.
**Agent lead** : `restitution`

---

## Epic JAS-EPIC-10 — Ecrans Angular parcours analyse (Lot D)

**Lead agent** : `frontend-angular`
**Objectif** : Implementer les 4 ecrans du parcours "analyser -> diagnostiquer ->
planifier -> generer" en Angular 21.2.x, consommant les endpoints du lot A.

**Dependances de l'epic** : JAS-56, JAS-57, JAS-58, JAS-59, JAS-60, JAS-61 (lot A)

---

### JAS-75 — Etendre WorkbenchApiService avec les endpoints d'analyse

**Epic** : JAS-EPIC-10
**Description** :
Etendre `WorkbenchApiService` (existant dans `frontend/src/app/core/services/`) avec
les methodes appellants les nouveaux endpoints REST du lot A :
- `submitAnalysis(request: SubmitAnalysisRequest): Observable<AnalysisSessionResponse>`
- `getSession(sessionId: string): Observable<AnalysisSessionResponse>`
- `getCartography(sessionId: string): Observable<CartographyResponse>`
- `getClassification(sessionId: string): Observable<ClassificationResponse>`
- `getMigrationPlan(sessionId: string): Observable<MigrationPlanResponse>`
- `getArtifacts(sessionId: string): Observable<ArtifactsResponse>`
- `getReport(sessionId: string): Observable<RestitutionReportResponse>`

Creer les interfaces TypeScript correspondantes dans `frontend/src/app/core/models/`.

**Criteres d'acceptation** :
- Toutes les methodes retournent des `Observable<T>`.
- Les interfaces TypeScript correspondent aux DTO du lot A.
- Les tests unitaires du service utilisent `HttpClientTestingModule`.
- Aucune logique metier dans le service (delegation stricte au backend).

**Story points** : 5
**Dependances** : JAS-56, JAS-57, JAS-58, JAS-59, JAS-60, JAS-61
**Risques** : Les endpoints du lot A doivent etre disponibles avant integration ; utiliser des mocks HTTP en test.
**Agent lead** : `frontend-angular`

---

### JAS-76 — Ajouter le routing Angular pour les ecrans d'analyse

**Epic** : JAS-EPIC-10
**Description** :
Etendre `frontend/src/app/app.routes.ts` avec les nouvelles routes :
- `/analyze` -> `AnalyzeComponent` (lazy-loaded)
- `/analyze/:sessionId/diagnostic` -> `DiagnosticComponent` (lazy-loaded)
- `/analyze/:sessionId/plan` -> `MigrationPlanComponent` (lazy-loaded)
- `/analyze/:sessionId/generate` -> `GenerationComponent` (lazy-loaded)

Ajouter un lien "Analyser un controller" dans la navigation du `DashboardComponent`.

**Criteres d'acceptation** :
- Les 4 routes sont declarees en `app.routes.ts` avec lazy-loading.
- La navigation depuis le dashboard vers `/analyze` fonctionne.
- Les tests de routing Angular valident les 4 routes.
- Aucune route ne charge de logique metier directement dans le module de routing.

**Story points** : 2
**Dependances** : JAS-75
**Risques** : Le lazy-loading requiert des modules Angular separement bundleables ; verifier la configuration Angular 21.
**Agent lead** : `frontend-angular`

---

### JAS-77 — Implementer AnalyzeComponent (route /analyze)

**Epic** : JAS-EPIC-10
**Description** :
Implementer `AnalyzeComponent` avec :
- Affichage de l'apercu du workbench (via `WorkbenchApiService.loadOverview()`).
- Formulaire de soumission avec champ "Chemin controller Java" et champ "Chemin FXML" (optionnel).
- Bouton "Analyser" -> appel `submitAnalysis()`.
- Etat loading avec indicateur visuel.
- Etat erreur avec message.
- Redirection automatique vers `/analyze/:sessionId/diagnostic` apres succes.

**Criteres d'acceptation** :
- Le formulaire est valide si le chemin controller Java n'est pas vide.
- Le bouton est desactive pendant la soumission.
- Une erreur HTTP est affichee lisiblement (pas de stack trace).
- La redirection vers `/diagnostic` se fait avec le `sessionId` recu.
- Tests unitaires couvrent : etat initial, etat loading, etat erreur, etat succes.
- `AnalyzeComponent` ne contient aucune logique de parsing de code source.

**Story points** : 5
**Dependances** : JAS-75, JAS-76
**Risques** : La soumission par chemin de fichier (pas d'upload) implique que les chemins sont valides sur le serveur backend ; documenter cette contrainte.
**Agent lead** : `frontend-angular`

---

### JAS-78 — Implementer DiagnosticComponent (route /analyze/:sessionId/diagnostic)

**Epic** : JAS-EPIC-10
**Description** :
Implementer `DiagnosticComponent` avec :
- Chargement automatique de la cartographie (`getCartography`) et de la classification (`getClassification`).
- Affichage de la liste des composants FXML (tableau : fx:id, type).
- Affichage de la liste des handlers (tableau : handler, composant source).
- Affichage des regles metier classifiees (certaines vs. incertaines).
- Navigation vers `/plan`.

**Criteres d'acceptation** :
- La cartographie et la classification sont chargees a l'initialisation du composant.
- Les erreurs HTTP sont gerees avec message d'erreur affiche.
- Les listes sont filtrees et affichees proprement si vides.
- Le bouton "Voir le plan" navigue vers `/analyze/:sessionId/plan`.
- Tests unitaires couvrent les etats loading, erreur, vide, rempli.

**Story points** : 8
**Dependances** : JAS-75, JAS-76, JAS-77
**Risques** : Les donnees retournees par les stubs du lot A peuvent etre vides au moment du dev frontend ; prevoir des fixtures de test Angular.
**Agent lead** : `frontend-angular`

---

### JAS-79 — Implementer MigrationPlanComponent (route /analyze/:sessionId/plan)

**Epic** : JAS-EPIC-10
**Description** :
Implementer `MigrationPlanComponent` avec :
- Chargement du plan de migration (`getMigrationPlan`).
- Affichage des 5 lots : numero, titre, objectif, niveau de risque (badge couleur).
- Pour chaque lot : liste des candidats d'extraction identifies.
- Navigation vers `/generate`.

**Criteres d'acceptation** :
- Les 5 lots sont affiches meme si certains sont vides de candidats.
- Le badge de risque est colore : vert (LOW), orange (MEDIUM), rouge (HIGH).
- Le bouton "Lancer la generation" navigue vers `/analyze/:sessionId/generate`.
- Tests unitaires couvrent les etats loading, erreur, plan vide, plan complet.

**Story points** : 5
**Dependances** : JAS-75, JAS-76, JAS-78
**Risques** : La correspondance entre `RiskLevel` backend et le badge couleur doit etre documentee cote frontend.
**Agent lead** : `frontend-angular`

---

### JAS-80 — Implementer GenerationComponent (route /analyze/:sessionId/generate)

**Epic** : JAS-EPIC-10
**Description** :
Implementer `GenerationComponent` avec :
- Chargement des artefacts (`getArtifacts`) et du rapport de restitution (`getReport`).
- Affichage de la liste des artefacts par lot : type, nom de classe, contenu (visualiseur text).
- Mise en evidence des bridges transitoires (marquage visuel distinct).
- Affichage du rapport markdown (rendu HTML basique ou pre-formate).
- Bouton "Retour au plan".

**Criteres d'acceptation** :
- Les artefacts sont groupes par lot dans l'interface.
- Les bridges transitoires sont visuellement distincts (badge ou icone).
- Le rapport markdown est affiche lisiblement.
- Le bouton "Retour au plan" navigue vers `/analyze/:sessionId/plan`.
- Tests unitaires couvrent les etats loading, erreur, liste vide, liste remplie.
- Aucune logique de generation de code dans le composant.

**Story points** : 5
**Dependances** : JAS-75, JAS-76, JAS-79
**Risques** : Le rendu markdown en HTML peut introduire des vulnerabilites XSS ; utiliser `DomSanitizer` Angular ou une bibliotheque safe.
**Agent lead** : `frontend-angular`

---

### JAS-81 — Tests de composants Angular (lot D)

**Epic** : JAS-EPIC-10
**Description** :
Completer la couverture de tests des 4 composants avec des tests Jasmine/Karma
(ou Jest selon la configuration du workspace) couvrant :
- Les etats loading, erreur, vide, rempli pour chaque composant.
- La navigation entre ecrans.
- La delegation stricte au service (pas de logique dans le composant).

**Criteres d'acceptation** :
- Chaque composant a au minimum 5 tests unitaires.
- Les tests utilisent `HttpClientTestingModule` et des mocks de service.
- `ng test` passe sans erreur.
- La couverture de branches est >= 70% pour chaque composant.

**Story points** : 5
**Dependances** : JAS-77, JAS-78, JAS-79, JAS-80
**Risques** : Les versions de Jasmine/Karma peuvent ne pas etre compatibles avec Angular 21 ; verifier avant demarrage.
**Agent lead** : `qa-frontend`

---

### JAS-82 — Logs debug frontend et correlation avec le backend

**Epic** : JAS-EPIC-10
**Description** :
Ajouter un intercepteur HTTP Angular qui :
- Ajoute l'en-tete `X-Correlation-Id` a chaque requete (genere cote client ou reuse depuis localStorage).
- Logue en `console.debug` les requetes et reponses HTTP quand l'environnement est `development`.
- Ne logue rien en `production`.

**Criteres d'acceptation** :
- L'intercepteur est enregistre dans `app.config.ts`.
- Les logs debug sont absents en build de production.
- Le `X-Correlation-Id` est present dans les requetes envoyees au backend.
- Tests unitaires de l'intercepteur.

**Story points** : 3
**Dependances** : JAS-77
**Risques** : La generation de correlation ID cote client doit etre UUID v4 ; utiliser `crypto.randomUUID()`.
**Agent lead** : `frontend-angular`

---

## Epic JAS-EPIC-11 — Orchestration pipeline bout-en-bout (Lot E)

**Lead agent** : `backend-hexagonal` + `implementation-moteur-analyse`
**Objectif** : Creer un orchestrateur qui enchaine les use cases d'analyse de facon
sequentielle, met a jour le statut de session et persiste les resultats intermediaires.

**Dependances de l'epic** : JAS-55, JAS-56, JAS-65, JAS-68 (lots A et B partiels)

---

### JAS-83 — Definir AnalysisOrchestrationUseCase et son contrat

**Epic** : JAS-EPIC-11
**Description** :
Definir l'interface `AnalysisOrchestrationUseCase` dans `application/ports/in/`.
Ce port entrant expose une seule methode :
`orchestrate(String sessionId) : OrchestratedAnalysisResult`

`OrchestratedAnalysisResult` (a creer dans le domaine) contient :
- `sessionId`
- `finalStatus` (`AnalysisStatus`)
- `cartography` (`ControllerCartography`)
- `classification` (`ClassificationResult`)
- `migrationPlan` (`MigrationPlan`)
- `generationResult` (`GenerationResult`)
- `restitutionReport` (`RestitutionReport`)
- `errors` (`List<String>`)

**Criteres d'acceptation** :
- `AnalysisOrchestrationUseCase` est dans `application/ports/in/`.
- `OrchestratedAnalysisResult` est dans `domain/` (classe immuable ou record).
- Aucune dependance Spring ni JPA dans ces deux artefacts.
- La compilation passe.

**Story points** : 2
**Dependances** : JAS-52
**Risques** : Le domaine doit rester pur ; `OrchestratedAnalysisResult` ne doit pas contenir de DTO REST.
**Agent lead** : `backend-hexagonal`

---

### JAS-84 — Implementer AnalysisOrchestrationService

**Epic** : JAS-EPIC-11
**Description** :
Creer `AnalysisOrchestrationService` dans `application/service/` implementant
`AnalysisOrchestrationUseCase`.

Sequence d'orchestration :
1. Recuperer la session via `AnalysisSessionPort.findById(sessionId)`.
2. Mettre le statut a `INGESTING` et persister.
3. Appeler `IngestSourcesUseCase.ingest(session.sourcePaths)`.
4. Mettre le statut a `CARTOGRAPHING` et persister.
5. Appeler `CartographyUseCase.cartography(ingestionResult)`.
6. Persister la cartographie via `AnalysisSessionPort.saveCartography(...)`.
7. Mettre le statut a `CLASSIFYING` et persister.
8. Appeler `ClassifyResponsibilitiesUseCase.classify(cartography)`.
9. Persister la classification.
10. Mettre le statut a `PLANNING` et persister.
11. Appeler `ProduceMigrationPlanUseCase.plan(classification)`.
12. Persister le plan.
13. Mettre le statut a `GENERATING` et persister.
14. Appeler `GenerateArtifactsUseCase.generate(plan)`.
15. Persister les artefacts.
16. Mettre le statut a `REPORTING` et persister.
17. Appeler `ProduceRestitutionUseCase.produce(cartography, classification, plan, generation)`.
18. Persister le rapport.
19. Mettre le statut a `COMPLETED` et persister.
20. Retourner `OrchestratedAnalysisResult`.

En cas d'erreur a n'importe quelle etape :
- Mettre le statut a `FAILED`.
- Ajouter le message d'erreur dans `OrchestratedAnalysisResult.errors`.
- Ne pas propager l'exception non checke vers le controller.

**Criteres d'acceptation** :
- Le service passe par chaque statut `AnalysisStatus` dans l'ordre.
- Chaque changement de statut est persiste avant l'appel du use case suivant.
- En cas d'erreur, le statut `FAILED` est persiste et `errors` est non vide.
- Tests unitaires avec mocks de tous les use cases et du port de persistence.
- Le service produit un log debug a chaque etape (avec sessionId).

**Story points** : 8
**Dependances** : JAS-55, JAS-57, JAS-65, JAS-68, JAS-83
**Risques** : La methode depasse 30 lignes ; decomposer en methodes privees (`processIngestion`, `processCartography`, etc.) de moins de 10 lignes chacune.
**Agent lead** : `backend-hexagonal`

---

### JAS-85 — Endpoint POST /api/v1/analysis/sessions/{sessionId}/run

**Epic** : JAS-EPIC-11
**Description** :
Ajouter a `AnalysisController` l'endpoint `POST /api/v1/analysis/sessions/{sessionId}/run`
qui declenche l'orchestration complete du pipeline.

- Reponse succes : `200 OK` avec `OrchestratedAnalysisResultResponse` (DTO REST du resultat final).
- Reponse erreur : `404` si session inconnue, `409` si session en cours (`status != CREATED`).

Creer `OrchestratedAnalysisResultResponse` et son mapper.

**Criteres d'acceptation** :
- `POST /run` sur une session `CREATED` retourne `200` avec le resultat complet.
- `POST /run` sur une session inconnue retourne `404`.
- `POST /run` sur une session `COMPLETED` retourne `409`.
- Tests MockMvc pour les 3 cas.
- Le log debug avec sessionId est produit.

**Story points** : 5
**Dependances** : JAS-56, JAS-57, JAS-84
**Risques** : L'orchestration synchrone peut etre lente pour de gros controllers ; documenter cette limitation et prevoir un timeout configurable.
**Agent lead** : `backend-hexagonal`

---

### JAS-86 — Ajouter les statuts AnalysisStatus manquants

**Epic** : JAS-EPIC-11
**Description** :
`AnalysisStatus` existe dans le domaine. Verifier et completer l'enum avec tous les
statuts necessaires au pipeline :
`CREATED`, `INGESTING`, `CARTOGRAPHING`, `CLASSIFYING`, `PLANNING`, `GENERATING`,
`REPORTING`, `COMPLETED`, `FAILED`.

Mettre a jour la migration Flyway si necessaire (contrainte CHECK sur la colonne `status`).

**Criteres d'acceptation** :
- `AnalysisStatus` contient les 9 valeurs.
- Si la colonne SQL a une contrainte CHECK, la migration V3 la met a jour.
- Aucune valeur n'est ajoutee dans le domaine sans justification fonctionnelle.
- La compilation passe.

**Story points** : 2
**Dependances** : JAS-63, JAS-83
**Risques** : Une contrainte CHECK sur `status` en base peut bloquer les migrations Flyway si elle n'est pas mise a jour.
**Agent lead** : `backend-hexagonal`

---

### JAS-87 — Enregistrer AnalysisOrchestrationService dans la configuration

**Epic** : JAS-EPIC-11
**Description** :
Creer `AnalysisOrchestrationConfiguration` dans `configuration/` qui :
- Declare le bean `AnalysisOrchestrationUseCase` pointe vers `AnalysisOrchestrationService`.
- Injecte tous les use cases dependants.
- Assemble le controller REST avec le nouveau use case.

**Criteres d'acceptation** :
- Le contexte Spring demarre sans erreur avec la nouvelle configuration.
- `AnalysisOrchestrationUseCase` est injectable dans `AnalysisController`.
- Test de contexte Spring (`@SpringBootTest`) passe.

**Story points** : 2
**Dependances** : JAS-84, JAS-85, JAS-86
**Risques** : Risque de circular dependency si l'orchestrateur injecte des services qui dependent eux-memes de l'orchestrateur.
**Agent lead** : `backend-hexagonal`

---

## Epic JAS-EPIC-12 — Tests d'integration du pipeline (Lot F)

**Lead agent** : `qa-backend` + `test-automation`
**Objectif** : Securiser le pipeline complet par des tests d'integration couvrant
chaque endpoint REST, le pipeline bout-en-bout et la persistence.

**Dependances de l'epic** : JAS-66, JAS-85, JAS-87 (lots B et E complets)

---

### JAS-88 — Strategie de test d'integration et outils

**Epic** : JAS-EPIC-12
**Description** :
Definir et documenter la strategie de tests d'integration pour le lot F :
- Choix entre `@SpringBootTest` + `MockMvc` + `H2` ou Testcontainers + PostgreSQL reel.
- Localisation des fixtures : `src/test/resources/fixtures/`.
- Convention de nommage des tests d'integration (suffixe `IT`).
- Configuration du profil de test Spring.

Produire `docs/strategie-tests-integration.md` documentant ces choix.

**Criteres d'acceptation** :
- `docs/strategie-tests-integration.md` existe et documente les 4 points ci-dessus.
- Un profil Spring `test` est configure dans `application-test.properties`.
- Les dependances de test (Testcontainers ou H2) sont ajoutees dans `pom.xml` en scope `test`.
- `mvn test` passe avec la nouvelle configuration.

**Story points** : 3
**Dependances** : JAS-62
**Risques** : Testcontainers peut alourdir le CI ; documenter le compromis et prevoir un switch de profil.
**Agent lead** : `qa-backend`

---

### JAS-89 — Tests d'integration des endpoints REST (lot A)

**Epic** : JAS-EPIC-12
**Description** :
Creer des tests d'integration `AnalysisControllerIT` couvrant :
- `POST /sessions` : cas succes, cas liste vide, cas chemin inexistant.
- `GET /sessions/{id}` : cas succes, cas 404.
- `GET /sessions/{id}/cartography` : cas succes avec fixture FXML.
- `GET /sessions/{id}/classification` : cas succes avec fixture Java.
- `GET /sessions/{id}/plan` : cas succes.
- `GET /sessions/{id}/artifacts` : cas succes.
- `GET /sessions/{id}/report` : cas succes, verification de l'encodage UTF-8.

**Criteres d'acceptation** :
- Chaque endpoint a au minimum 2 tests d'integration (cas nominal + cas erreur).
- Les tests utilisent les fixtures de JAS-70 (`SampleController.java`, `SampleView.fxml`).
- Les assertions verifient les codes HTTP, les champs JSON de reponse et les headers.
- `mvn verify -P integration-test` passe.

**Story points** : 5
**Dependances** : JAS-66, JAS-70, JAS-88
**Risques** : La dependance aux fichiers du filesystem dans les fixtures necessite une configuration de chemin absolu en test.
**Agent lead** : `qa-backend`

---

### JAS-90 — Tests d'integration du pipeline bout-en-bout

**Epic** : JAS-EPIC-12
**Description** :
Creer `AnalysisPipelineIT` qui teste le pipeline complet avec les analyseurs reels
(lots C) :
1. Soumettre les chemins de `SampleController.java` et `SampleView.fxml`.
2. Appeler `POST /sessions/{id}/run`.
3. Verifier le statut final `COMPLETED`.
4. Verifier que la cartographie contient les composants FXML attendus.
5. Verifier que la classification contient au moins une regle metier.
6. Verifier que le plan contient 5 lots.
7. Verifier que les artefacts contiennent au moins un `CodeArtifact` de type `VIEW_MODEL`.
8. Verifier que le rapport markdown n'est pas vide.

**Criteres d'acceptation** :
- Le test passe de bout en bout sur les fixtures de JAS-70.
- Le statut final est `COMPLETED`.
- Chaque etape du pipeline produit un resultat non vide verifie par assertion.
- Le test s'execute en moins de 30 secondes.
- `mvn verify -P integration-test` passe.

**Story points** : 8
**Dependances** : JAS-69, JAS-71, JAS-72, JAS-73, JAS-85, JAS-88, JAS-89
**Risques** : Le test bout-en-bout est lent si le parsing est intensif ; limiter les fixtures aux 2 controllers de test.
**Agent lead** : `qa-backend`

---

### JAS-91 — Tests d'integration de la persistence JPA

**Epic** : JAS-EPIC-12
**Description** :
Creer `JpaAnalysisSessionAdapterIT` avec des tests `@DataJpaTest` (H2 ou Testcontainers) :
- `save` : une session est persistee et retrouvee par ID.
- `updateStatus` : le statut est mis a jour et persiste.
- `saveCartography` : le JSON de cartographie est stocke et retrouve.
- `saveClassification`, `saveMigrationPlan`, `saveGenerationResult`, `saveRestitutionReport`.
- Recherche par ID inexistant retourne `Optional.empty()`.

**Criteres d'acceptation** :
- Chaque methode de `JpaAnalysisSessionAdapter` a au moins un test d'integration.
- Les assertions verifient la ronde-trip de serialisation JSON (persist + retrieve + deserialize).
- `mvn test` passe (les tests `@DataJpaTest` font partie du cycle `test` standard).

**Story points** : 5
**Dependances** : JAS-65, JAS-68, JAS-88
**Risques** : La serialisation JSON des objets domaine en TEXT peut introduire des decalages de types (LocalDateTime, enums) ; tester explicitement.
**Agent lead** : `qa-backend`

---

### JAS-92 — Fixtures complexes et test du controller complexe

**Epic** : JAS-EPIC-12
**Description** :
Creer un test d'integration `ComplexControllerPipelineIT` utilisant les fixtures
`ComplexController.java` et `ComplexView.fxml` (JAS-70) :
- Verifier que le pipeline traite correctement un controller avec 10+ handlers.
- Verifier que les 5 lots sont generes avec des risques differencies.
- Verifier que les artefacts couvrent au moins 3 types distincts (`VIEW_MODEL`, `USE_CASE`, `POLICY`).
- Verifier que le rapport markdown mentionne les inconnues detectives.

**Criteres d'acceptation** :
- Le test passe avec le controller complexe.
- Le plan contient au moins 7 candidats d'extraction.
- Le `RiskLevel` du lot 3 est `HIGH` ou `MEDIUM`.
- Les artefacts couvrent 3 types distincts minimum.
- `mvn verify -P integration-test` passe.

**Story points** : 5
**Dependances** : JAS-70, JAS-90
**Risques** : La precision de la classification sur le controller complexe depend de la qualite des heuristiques de JAS-71 ; tolerer des faux positifs documentes.
**Agent lead** : `test-automation`

---

## Synthese des dependances inter-lots

```
Lot A (JAS-53 a JAS-61)
  └─> Lot B (JAS-62 a JAS-68) : JPA remplace le stub en memoire du lot A
  └─> Lot D (JAS-75 a JAS-82) : le frontend consomme les endpoints du lot A

Lot B (JAS-62 a JAS-68)
  └─> Lot E (JAS-83 a JAS-87) : l'orchestrateur persiste via JPA

Lot C (JAS-69 a JAS-74)
  └─> Lot E (JAS-83 a JAS-87) : l'orchestrateur appelle les analyseurs reels
  └─> Lot F (JAS-88 a JAS-92) : les tests d'integration utilisent les analyseurs reels

Lot E (JAS-83 a JAS-87)
  └─> Lot F (JAS-88 a JAS-92) : les tests d'integration testent l'orchestration

Lot F (JAS-88 a JAS-92) : dernier lot, valide tout le pipeline
```

**Ordres d'execution recommandes par sprint :**

| Sprint | Tickets                                      | Objectif                                  |
|--------|----------------------------------------------|-------------------------------------------|
| S1     | JAS-53, JAS-54, JAS-55, JAS-62, JAS-63      | DTO, mappers, use case soumission, JPA socle |
| S2     | JAS-56, JAS-57, JAS-58, JAS-64, JAS-65      | Endpoints POST/GET session et cartographie, entite JPA |
| S3     | JAS-59, JAS-60, JAS-61, JAS-66, JAS-67      | Endpoints classification/plan/artefacts, branchement JPA |
| S4     | JAS-68, JAS-70, JAS-69, JAS-71              | Persistence resultats, fixtures, analyseurs C1 et C2 |
| S5     | JAS-72, JAS-73, JAS-74, JAS-75, JAS-76      | Analyseurs C3/C4, service Angular, routing |
| S6     | JAS-77, JAS-78, JAS-79, JAS-80, JAS-81, JAS-82 | Ecrans Angular, tests composants, logs |
| S7     | JAS-83, JAS-84, JAS-85, JAS-86, JAS-87      | Orchestration pipeline                    |
| S8     | JAS-88, JAS-89, JAS-90, JAS-91, JAS-92      | Tests d'integration complets              |

---

## Risques transverses phase 2

| Risque                                                                 | Impact | Probabilite | Mitigation                                                       |
|------------------------------------------------------------------------|--------|-------------|------------------------------------------------------------------|
| Incompatibilite Flyway / Spring Boot 4.0.3                             | Eleve  | Moyenne     | JAS-62 : verifier les versions avant ajout dans pom.xml          |
| Analyse Java par regex produit trop de faux positifs                  | Moyen  | Elevee      | JAS-71 : documenter les limites ; ne pas promettre la precision  |
| Serialisation JSON des objets domaine en TEXT produit des bugs         | Eleve  | Moyenne     | JAS-68 : tester la ronde-trip explicitement                      |
| Orchestration synchrone trop lente pour de gros controllers            | Moyen  | Elevee      | JAS-85 : documenter la limitation et prevoir timeout configurable |
| Rendu markdown en HTML introduit une vulnerabilite XSS                 | Eleve  | Faible      | JAS-80 : utiliser DomSanitizer Angular obligatoirement           |
| Derivation de l'hexagone si JPA entre dans application/ ou domain/     | Eleve  | Moyenne     | Revue systematique de chaque PR du lot B                         |
| Fixtures Java compilees accidentellement en production                 | Faible | Faible      | JAS-70 : confiner dans src/test/resources uniquement             |
| Correlation ID non transmis du frontend au backend                     | Faible | Faible      | JAS-82 : intercepteur HTTP avec X-Correlation-Id                 |

---

## Estimation consolidee

| Epic         | Lot | Tickets                        | SP   |
|--------------|-----|--------------------------------|------|
| JAS-EPIC-07  | A   | JAS-53 a JAS-61 (9 tickets)    | 34   |
| JAS-EPIC-08  | B   | JAS-62 a JAS-68 (7 tickets)    | 21   |
| JAS-EPIC-09  | C   | JAS-69 a JAS-74 (6 tickets)    | 42   |
| JAS-EPIC-10  | D   | JAS-75 a JAS-82 (8 tickets)    | 38   |
| JAS-EPIC-11  | E   | JAS-83 a JAS-87 (5 tickets)    | 19   |
| JAS-EPIC-12  | F   | JAS-88 a JAS-92 (5 tickets)    | 26   |
| **Total**    |     | **40 tickets**                 | **180** |

SP phase 1 (JAS-01 a JAS-52) : 88 SP
SP phase 2 (JAS-53 a JAS-92) : 180 SP
**Total projet : 268 SP**

---

*Document produit par l'agent `jira-estimation` le 2026-03-16.*
*Reference : `agents/contracts.md`, `docs/jas-01-parcours-utilisateur.md`,*
*`guide_generique_refactoring_controller_javafx_spring.md`, `agents/orchestration.md`.*
