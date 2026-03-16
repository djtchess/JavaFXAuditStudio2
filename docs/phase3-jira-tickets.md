# Phase 3 - Implementation Produit : Decoupage JIRA Complet

Date de reference : 2026-03-16
Agent : `jira-estimation`
Modele : `opus`

---

## Sommaire

- [Vue d'ensemble](#vue-densemble)
- [EPIC-01 Backend Hexagonal - Remplacement des stubs](#epic-01-backend-hexagonal---remplacement-des-stubs)
- [EPIC-02 Database PostgreSQL - Enrichissement du schema](#epic-02-database-postgresql---enrichissement-du-schema)
- [EPIC-03 Frontend Angular - UX enrichie](#epic-03-frontend-angular---ux-enrichie)
- [EPIC-04 Moteur d'analyse - Implementation reelle](#epic-04-moteur-danalyse---implementation-reelle)
- [EPIC-05 DevOps CI/CD](#epic-05-devops-cicd)
- [EPIC-06 Observabilite et exploitation](#epic-06-observabilite-et-exploitation)
- [EPIC-07 Documentation technique](#epic-07-documentation-technique)
- [Dependances inter-epics](#dependances-inter-epics)
- [Risques globaux](#risques-globaux)
- [Plan de lots progressifs](#plan-de-lots-progressifs)
- [Totaux](#totaux)

---

## Vue d'ensemble

| Epic | Stories | Story Points | Priorite dominante |
|------|---------|-------------|-------------------|
| EPIC-01 Backend hexagonal | 6 | 39 | P0 |
| EPIC-02 Database PostgreSQL | 5 | 29 | P0 |
| EPIC-03 Frontend Angular | 6 | 34 | P1 |
| EPIC-04 Moteur d'analyse | 5 | 37 | P1 |
| EPIC-05 DevOps CI/CD | 4 | 21 | P2 |
| EPIC-06 Observabilite | 4 | 18 | P2 |
| EPIC-07 Documentation | 3 | 10 | P3 |
| **Total** | **33** | **188** | |

---

## EPIC-01 Backend Hexagonal - Remplacement des stubs

**Objectif** : Remplacer les 4 stubs Profile("stub") par des adapters connectes a de vraies implementations, et consolider le wiring Spring Boot.

**Etat actuel observe** :
- `StubCartographyAnalysisAdapter` : retourne `List.of()` -- **deja remplace** par `FxmlCartographyAnalysisAdapter` (parsing DOM XML + regex Java). Le stub reste present sous profil "stub".
- `StubRuleExtractionAdapter` : retourne `List.of()` -- **deja remplace** par `JavaControllerRuleExtractionAdapter` (regex multi-patterns). Le stub reste sous profil "stub".
- `StubCodeGenerationAdapter` : retourne `List.of()` -- **deja remplace** par `RealCodeGenerationAdapter` (generation de squelettes). Le stub reste sous profil "stub".
- `StubMigrationPlannerAdapter` : retourne 5 lots generiques -- **deja remplace** par `RealMigrationPlannerAdapter` (lots contextuels). Le stub reste sous profil "stub".
- Les adapters reels sont instancies via les `*Configuration` classes sans annotation Spring directe -- conforme au pattern hexagonal.
- Les adapters reels produisent des squelettes de code et des analyses par regex. Ils ne font pas de vraie analyse AST.

### JAS-301 : Activer les adapters reels par defaut et deprecier les stubs
**Type** : Story
**Priorite** : P0
**Estimation** : 3 SP
**Dependances** : Aucune

**Description** : Configurer le profil Spring par defaut pour utiliser les adapters reels. Marquer les stubs comme `@Deprecated`. Ajouter un profil "test" qui conserve les stubs pour les tests d'integration rapides.

**Criteres d'acceptation** :
- CA-1 : Le profil par defaut (`application.yml`) active les adapters reels sans specification de profil explicite.
- CA-2 : Chaque stub est annote `@Deprecated(forRemoval = true)` avec commentaire renvoyant au ticket de suppression.
- CA-3 : Les tests d'integration existants passent avec le profil "stub".
- CA-4 : Un test d'integration verifie que le profil par defaut instancie les adapters reels.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-301-T1 | Modifier `application.yml` : profil actif par defaut = production | 1 SP |
| JAS-301-T2 | Ajouter `@Deprecated` aux 4 classes Stub* | 0.5 SP |
| JAS-301-T3 | Ecrire un test d'integration verifiant le bean inject par profil | 1 SP |
| JAS-301-T4 | Documenter le switch de profils dans le README backend | 0.5 SP |

---

### JAS-302 : Persister les resultats de cartographie en base
**Type** : Story
**Priorite** : P0
**Estimation** : 8 SP
**Dependances** : EPIC-02 (JAS-401, JAS-402)

**Description** : Creer un adapter de persistence pour sauvegarder les resultats de cartographie (composants FXML, handlers) lies a une session d'analyse, et les relire a la demande.

**Criteres d'acceptation** :
- CA-1 : Un nouveau port sortant `CartographyPersistencePort` est defini dans `application.ports.out`.
- CA-2 : Un adapter JPA `JpaCartographyAdapter` implemente ce port.
- CA-3 : Les resultats de cartographie sont persistes a chaque execution reussie du use case `CartographyUseCase`.
- CA-4 : `AnalysisController.getCartography()` relit les resultats persistes si deja calcules.
- CA-5 : Les entities JPA respectent le schema Flyway (JAS-402).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-302-T1 | Creer `CartographyPersistencePort` (save, findBySessionId) | 1 SP |
| JAS-302-T2 | Creer les entities JPA : `CartographyResultEntity`, `FxmlComponentEntity`, `HandlerBindingEntity` | 2 SP |
| JAS-302-T3 | Creer le repository Spring Data `CartographyResultRepository` | 1 SP |
| JAS-302-T4 | Implementer `JpaCartographyAdapter` | 2 SP |
| JAS-302-T5 | Modifier `CartographyService` pour persister apres analyse | 1 SP |
| JAS-302-T6 | Tests unitaires du mapping entity/domain | 1 SP |

---

### JAS-303 : Persister les resultats de classification en base
**Type** : Story
**Priorite** : P0
**Estimation** : 8 SP
**Dependances** : EPIC-02 (JAS-401, JAS-403)

**Description** : Persister les regles metier extraites et la classification des responsabilites liees a une session.

**Criteres d'acceptation** :
- CA-1 : Un port `ClassificationPersistencePort` (save, findBySessionId) est defini.
- CA-2 : Un adapter JPA sauvegarde les `BusinessRule` en base.
- CA-3 : Le `ClassifyResponsibilitiesService` persiste les resultats apres classification.
- CA-4 : Relecture possible via `AnalysisController.getClassification()`.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-303-T1 | Creer `ClassificationPersistencePort` | 1 SP |
| JAS-303-T2 | Creer les entities JPA : `ClassificationResultEntity`, `BusinessRuleEntity` | 2 SP |
| JAS-303-T3 | Implementer `JpaClassificationAdapter` | 2 SP |
| JAS-303-T4 | Modifier `ClassifyResponsibilitiesService` | 1 SP |
| JAS-303-T5 | Tests unitaires | 2 SP |

---

### JAS-304 : Persister les plans de migration en base
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : EPIC-02 (JAS-404)

**Description** : Persister les `MigrationPlan` et `PlannedLot` generes pour une session d'analyse.

**Criteres d'acceptation** :
- CA-1 : Port `MigrationPlanPersistencePort` defini.
- CA-2 : Adapter JPA implemente.
- CA-3 : `ProduceMigrationPlanService` persiste apres generation.
- CA-4 : Relecture via le controller REST existant.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-304-T1 | Creer port et entities (MigrationPlanEntity, PlannedLotEntity, RegressionRiskEntity) | 2 SP |
| JAS-304-T2 | Implementer JpaAdapter | 1.5 SP |
| JAS-304-T3 | Tests unitaires | 1.5 SP |

---

### JAS-305 : Persister les artefacts generes en base
**Type** : Story
**Priorite** : P1
**Estimation** : 8 SP
**Dependances** : EPIC-02 (JAS-405)

**Description** : Persister les `CodeArtifact` generes pour une session, y compris le contenu source genere (stocke en `TEXT` ou `CLOB`).

**Criteres d'acceptation** :
- CA-1 : Port `ArtifactPersistencePort` defini.
- CA-2 : La colonne `content` est de type `TEXT` pour stocker du code Java genere.
- CA-3 : Les artefacts sont lies a une session et un lot de migration.
- CA-4 : Relecture paginee possible (un controller peut generer 6+ artefacts).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-305-T1 | Port sortant + entity `CodeArtifactEntity` | 2 SP |
| JAS-305-T2 | Repository + adapter JPA | 2 SP |
| JAS-305-T3 | Modifier `GenerateArtifactsService` pour persister | 1 SP |
| JAS-305-T4 | Tests + validation mapping content TEXT | 2 SP |
| JAS-305-T5 | Endpoint REST pagination artefacts par lot | 1 SP |

---

### JAS-306 : Persister les rapports de restitution en base
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : EPIC-02 (JAS-406)

**Description** : Persister le `RestitutionReport` complet (summary, contradictions, unknowns, findings) pour relecture ulterieure.

**Criteres d'acceptation** :
- CA-1 : Port `RestitutionPersistencePort` defini.
- CA-2 : Les listes (contradictions, unknowns, findings) sont stockees comme `JSONB` ou table enfant.
- CA-3 : Relecture via controller REST.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-306-T1 | Port + entity `RestitutionReportEntity` | 1.5 SP |
| JAS-306-T2 | Adapter JPA avec mapping JSONB pour listes | 2 SP |
| JAS-306-T3 | Tests unitaires | 1.5 SP |

---

### Risques EPIC-01

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-01 | Le switch de profils casse les tests existants | Moyen | Moyenne | JAS-301-T3 : test d'integration multi-profils |
| R-02 | Les entities JPA divergent du domain model (mapping lourd) | Eleve | Moyenne | Utiliser des mappers explicites, pas de heritage entity/domain |
| R-03 | Performance de la colonne TEXT pour le contenu code | Faible | Faible | Indexation partielle si necessaire, pagination des lectures |

---

## EPIC-02 Database PostgreSQL - Enrichissement du schema

**Objectif** : Faire evoluer le schema de 1 table (`analysis_session`) vers un modele complet supportant la persistance de tous les resultats d'analyse.

**Etat actuel observe** :
- 1 seule migration Flyway : `V1__create_analysis_session.sql`
- 1 seule table : `analysis_session` (5 colonnes)
- DDL validate actif
- PostgreSQL cible

### JAS-401 : Migration V2 - Table cartography_result
**Type** : Story
**Priorite** : P0
**Estimation** : 5 SP
**Dependances** : Aucune (prerequis de JAS-302)

**Description** : Creer les tables pour stocker les resultats de cartographie FXML et les handler bindings.

**Criteres d'acceptation** :
- CA-1 : Migration `V2__create_cartography_tables.sql` cree 3 tables : `cartography_result`, `fxml_component`, `handler_binding`.
- CA-2 : FK vers `analysis_session(session_id)` sur `cartography_result`.
- CA-3 : `fxml_component` reference `cartography_result` avec FK.
- CA-4 : `handler_binding` reference `cartography_result` avec FK.
- CA-5 : Commentaires SQL sur chaque table et colonne.
- CA-6 : Flyway validate passe sans erreur.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-401-T1 | Ecrire V2 SQL : DDL + FK + index + commentaires | 3 SP |
| JAS-401-T2 | Tester la migration sur base vierge et sur base V1 existante | 1 SP |
| JAS-401-T3 | Valider la coherence entity JPA / DDL (ddl-auto=validate) | 1 SP |

**Schema propose** :
```
cartography_result
  id              BIGSERIAL PK
  session_id      VARCHAR(36) FK -> analysis_session NOT NULL
  controller_ref  VARCHAR(512) NOT NULL
  fxml_ref        VARCHAR(512)
  created_at      TIMESTAMPTZ NOT NULL

fxml_component
  id                  BIGSERIAL PK
  cartography_id      BIGINT FK -> cartography_result NOT NULL
  fx_id               VARCHAR(256) NOT NULL
  component_type      VARCHAR(256) NOT NULL
  event_handler       VARCHAR(256)

handler_binding
  id                  BIGSERIAL PK
  cartography_id      BIGINT FK -> cartography_result NOT NULL
  method_name         VARCHAR(256) NOT NULL
  event_type          VARCHAR(128)
  return_type         VARCHAR(128)
```

---

### JAS-402 : Migration V3 - Tables classification et business_rule
**Type** : Story
**Priorite** : P0
**Estimation** : 5 SP
**Dependances** : JAS-401

**Description** : Creer les tables pour stocker les resultats de classification des responsabilites.

**Criteres d'acceptation** :
- CA-1 : Migration V3 cree `classification_result` et `business_rule`.
- CA-2 : Enums `responsibility_class` et `extraction_candidate` stockes comme VARCHAR.
- CA-3 : FK vers `analysis_session`.
- CA-4 : Flyway validate passe.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-402-T1 | Ecrire V3 SQL | 2 SP |
| JAS-402-T2 | Tests migration + coherence entity | 2 SP |
| JAS-402-T3 | Index sur session_id | 1 SP |

**Schema propose** :
```
classification_result
  id              BIGSERIAL PK
  session_id      VARCHAR(36) FK -> analysis_session NOT NULL
  controller_ref  VARCHAR(512) NOT NULL
  created_at      TIMESTAMPTZ NOT NULL

business_rule
  id                      BIGSERIAL PK
  classification_id       BIGINT FK -> classification_result NOT NULL
  rule_id                 VARCHAR(64) NOT NULL
  description             TEXT NOT NULL
  source_ref              VARCHAR(512) NOT NULL
  source_line             INT NOT NULL DEFAULT 0
  responsibility_class    VARCHAR(32) NOT NULL
  extraction_candidate    VARCHAR(32) NOT NULL
  uncertain               BOOLEAN NOT NULL DEFAULT FALSE
```

---

### JAS-403 : Migration V4 - Tables migration_plan et planned_lot
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-402

**Description** : Tables pour les plans de migration et leurs lots.

**Criteres d'acceptation** :
- CA-1 : Migration V4 cree `migration_plan`, `planned_lot`, `regression_risk`.
- CA-2 : Les `actions` de chaque lot sont stockees en `JSONB` (liste de strings).
- CA-3 : FK correctes.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-403-T1 | Ecrire V4 SQL | 2 SP |
| JAS-403-T2 | Tests migration | 2 SP |
| JAS-403-T3 | Coherence entity | 1 SP |

**Schema propose** :
```
migration_plan
  id              BIGSERIAL PK
  session_id      VARCHAR(36) FK -> analysis_session NOT NULL
  controller_ref  VARCHAR(512) NOT NULL
  compilable      BOOLEAN NOT NULL DEFAULT FALSE
  created_at      TIMESTAMPTZ NOT NULL

planned_lot
  id              BIGSERIAL PK
  plan_id         BIGINT FK -> migration_plan NOT NULL
  lot_number      INT NOT NULL
  title           VARCHAR(256) NOT NULL
  description     TEXT
  actions         JSONB NOT NULL DEFAULT '[]'

regression_risk
  id              BIGSERIAL PK
  lot_id          BIGINT FK -> planned_lot NOT NULL
  description     TEXT NOT NULL
  risk_level      VARCHAR(16) NOT NULL
  mitigation      TEXT
```

---

### JAS-404 : Migration V5 - Table code_artifact
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-403

**Description** : Table pour les artefacts de code generes.

**Criteres d'acceptation** :
- CA-1 : Migration V5 cree `code_artifact`.
- CA-2 : La colonne `content` est de type `TEXT` (code Java genere).
- CA-3 : FK vers `analysis_session`.
- CA-4 : Colonne `artifact_type` stockee en VARCHAR (enum).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-404-T1 | Ecrire V5 SQL | 2 SP |
| JAS-404-T2 | Tests migration + coherence entity | 2 SP |
| JAS-404-T3 | Index sur session_id + artifact_type | 1 SP |

**Schema propose** :
```
code_artifact
  id                  BIGSERIAL PK
  session_id          VARCHAR(36) FK -> analysis_session NOT NULL
  artifact_id         VARCHAR(256) NOT NULL
  artifact_type       VARCHAR(32) NOT NULL
  lot_number          INT NOT NULL
  class_name          VARCHAR(256) NOT NULL
  content             TEXT NOT NULL
  transitional_bridge BOOLEAN NOT NULL DEFAULT FALSE
  created_at          TIMESTAMPTZ NOT NULL
```

---

### JAS-405 : Migration V6 - Table restitution_report
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-404

**Description** : Table pour le rapport de restitution avec colonnes JSONB pour les listes.

**Criteres d'acceptation** :
- CA-1 : Migration V6 cree `restitution_report`.
- CA-2 : Les colonnes `contradictions`, `unknowns`, `findings` sont de type `JSONB`.
- CA-3 : La synthese (titre, description, confidence_level) est stockee en colonnes scalaires.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-405-T1 | Ecrire V6 SQL | 2 SP |
| JAS-405-T2 | Tests migration | 2 SP |
| JAS-405-T3 | Coherence entity (mapping JSONB -> List<String> via converter JPA) | 1 SP |

**Schema propose** :
```
restitution_report
  id                  BIGSERIAL PK
  session_id          VARCHAR(36) FK -> analysis_session NOT NULL
  summary_title       VARCHAR(512)
  summary_description TEXT
  confidence_level    VARCHAR(16) NOT NULL
  has_contradictions  BOOLEAN NOT NULL DEFAULT FALSE
  contradictions      JSONB NOT NULL DEFAULT '[]'
  unknowns            JSONB NOT NULL DEFAULT '[]'
  findings            JSONB NOT NULL DEFAULT '[]'
  created_at          TIMESTAMPTZ NOT NULL
```

---

### JAS-406 : Migration V7 - Index et contraintes transverses
**Type** : Story
**Priorite** : P2
**Estimation** : 3 SP
**Dependances** : JAS-405

**Description** : Ajouter des index composites et des contraintes d'integrite transverses.

**Criteres d'acceptation** :
- CA-1 : Index unique `(session_id)` sur chaque table de resultat (1 resultat par session par type).
- CA-2 : Index sur `analysis_session.status` pour les requetes de listing.
- CA-3 : Contrainte CHECK sur `analysis_session.status` limitee aux valeurs valides.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-406-T1 | Ecrire V7 SQL | 1.5 SP |
| JAS-406-T2 | Tests de migration + performance queries | 1.5 SP |

---

### Risques EPIC-02

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-04 | Migration Flyway echoue sur base existante avec donnees de test | Moyen | Moyenne | Scripts de migration testes sur base V1 avec donnees |
| R-05 | JSONB complique le mapping JPA | Moyen | Faible | Utiliser un `AttributeConverter<List<String>, String>` |
| R-06 | Schema trop normalise impacte les performances de lecture | Faible | Faible | Monitoring requetes, denormaliser si necessaire |

---

## EPIC-03 Frontend Angular - UX enrichie

**Objectif** : Remplacer l'affichage JSON brut par des composants structures et lisibles pour chaque etape d'analyse.

**Etat actuel observe** :
- `analysis-detail.component.ts` affiche les 5 etapes avec `{{ data | json }}` dans des blocs `<pre>`.
- Composant monolithique (392 lignes) avec 5 signaux et 5 methodes `run*()`.
- Design system en CSS custom variables, pas de framework CSS.
- Standalone components, OnPush strategy.

### JAS-501 : Composant cartography-view - affichage structure de la cartographie
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-302 (donnees reelles disponibles)

**Description** : Creer un composant standalone qui affiche la cartographie sous forme de tableau avec composants FXML et handlers.

**Criteres d'acceptation** :
- CA-1 : Tableau des composants FXML avec colonnes : fx:id, type, event handler.
- CA-2 : Tableau des handlers avec colonnes : methode, event type, return type.
- CA-3 : Badge de comptage (nombre de composants, nombre de handlers).
- CA-4 : OnPush change detection.
- CA-5 : Input signal : `CartographyResponse`.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-501-T1 | Creer `cartography-view.component.ts` avec template et styles | 3 SP |
| JAS-501-T2 | Integrer dans `analysis-detail.component.ts` en remplacement du JSON | 1 SP |
| JAS-501-T3 | Tests unitaires du composant | 1 SP |

---

### JAS-502 : Composant classification-view - affichage des regles metier
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-303

**Description** : Afficher les regles metier classifiees avec indicateurs visuels par type de responsabilite.

**Criteres d'acceptation** :
- CA-1 : Liste des regles avec badge colore par `responsibilityClass` (UI=bleu, BUSINESS=orange, APPLICATION=vert, TECHNICAL=gris, UNKNOWN=rouge).
- CA-2 : Indicateur "uncertain" visible.
- CA-3 : Filtre par type de responsabilite.
- CA-4 : Compteur par categorie.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-502-T1 | Creer `classification-view.component.ts` | 3 SP |
| JAS-502-T2 | Integration + remplacement du JSON | 1 SP |
| JAS-502-T3 | Tests unitaires | 1 SP |

---

### JAS-503 : Composant migration-plan-view - affichage des lots de migration
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-304

**Description** : Afficher le plan de migration sous forme de timeline ou stepper avec les 5 lots.

**Criteres d'acceptation** :
- CA-1 : Affichage en stepper vertical avec numero de lot, titre, description.
- CA-2 : Liste des actions par lot.
- CA-3 : Indicateur de risque de regression par lot (couleur par RiskLevel).
- CA-4 : Description de la mitigation au survol ou en expansion.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-503-T1 | Creer `migration-plan-view.component.ts` | 3 SP |
| JAS-503-T2 | Integration + remplacement du JSON | 1 SP |
| JAS-503-T3 | Tests unitaires | 1 SP |

---

### JAS-504 : Composant artifacts-view - affichage du code genere
**Type** : Story
**Priorite** : P1
**Estimation** : 8 SP
**Dependances** : JAS-305

**Description** : Afficher les artefacts de code generes avec coloration syntaxique et navigation par lot.

**Criteres d'acceptation** :
- CA-1 : Navigation par onglets (1 onglet par lot de migration).
- CA-2 : Chaque artefact affiche : nom de classe, type (badge), code avec coloration syntaxique.
- CA-3 : Indicateur "bridge transitoire" visible.
- CA-4 : Bouton de copie du code dans le presse-papier.
- CA-5 : Coloration syntaxique Java (via Prism.js ou highlight.js integre en standalone).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-504-T1 | Evaluer et integrer une librairie de coloration syntaxique (Prism.js) | 2 SP |
| JAS-504-T2 | Creer `artifacts-view.component.ts` avec onglets par lot | 3 SP |
| JAS-504-T3 | Creer `code-block.component.ts` reutilisable (coloration + copie) | 2 SP |
| JAS-504-T4 | Tests unitaires | 1 SP |

---

### JAS-505 : Composant report-view - affichage du rapport de restitution
**Type** : Story
**Priorite** : P1
**Estimation** : 5 SP
**Dependances** : JAS-306

**Description** : Afficher le rapport de restitution avec synthese, contradictions, inconnues et findings.

**Criteres d'acceptation** :
- CA-1 : Carte de synthese en tete avec titre, description, badge de confiance.
- CA-2 : Section "Contradictions" avec liste a puces (masquee si vide).
- CA-3 : Section "Inconnues" avec liste a puces.
- CA-4 : Section "Findings" avec liste a puces.
- CA-5 : Indicateur "rapport actionnable" (isActionable) visible.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-505-T1 | Creer `report-view.component.ts` | 3 SP |
| JAS-505-T2 | Integration + remplacement du JSON | 1 SP |
| JAS-505-T3 | Tests unitaires | 1 SP |

---

### JAS-506 : Refactoring de analysis-detail en composant orchestrateur
**Type** : Story
**Priorite** : P2
**Estimation** : 5 SP
**Dependances** : JAS-501 a JAS-505

**Description** : Simplifier `analysis-detail.component.ts` en delegant chaque etape a un composant enfant. Ajouter un bouton "Executer tout le pipeline" et un indicateur de progression global.

**Criteres d'acceptation** :
- CA-1 : Le composant parent ne contient plus de template d'affichage de resultats -- il delegue.
- CA-2 : Un bouton "Executer le pipeline complet" declenche les 5 etapes en sequence.
- CA-3 : Une barre de progression indique l'etape en cours.
- CA-4 : Les erreurs d'une etape sont visibles sans bloquer l'affichage des etapes precedentes.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-506-T1 | Extraire les 5 blocs step-card vers les composants enfants | 2 SP |
| JAS-506-T2 | Implementer le bouton pipeline complet avec orchestration sequentielle | 2 SP |
| JAS-506-T3 | Barre de progression et gestion d'erreurs partielle | 1 SP |

---

### Risques EPIC-03

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-07 | La librairie de coloration syntaxique alourdit le bundle | Moyen | Moyenne | Lazy loading + tree-shaking, n'inclure que le langage Java |
| R-08 | Composants structures trop rigides si le modele domain evolue | Moyen | Faible | Inputs typesafe + tests de regression |
| R-09 | Compatibilite Angular 21.x non verifiee pour certaines deps | Eleve | Moyenne | Revalider avant integration (cf. jas-22) |

---

## EPIC-04 Moteur d'analyse - Implementation reelle

**Objectif** : Ameliorer les adapters d'analyse au-dela du regex pour produire des resultats plus precis et contextuels.

**Etat actuel observe** :
- `FxmlCartographyAnalysisAdapter` : parsing DOM XML fonctionnel, extraction handlers par regex `@FXML void xxx(`.
- `JavaControllerRuleExtractionAdapter` : classification par mots-cles dans le body des methodes (regex). Limite documentee : `sourceLine` toujours 0.
- `RealCodeGenerationAdapter` : genere 6 squelettes (controller slim, viewmodel, usecase, policy, gateway, assembler) avec des TODO.
- `RealMigrationPlannerAdapter` : 5 lots statiques avec risque contextuel (High si nom contient "Complex").

### JAS-601 : Enrichir le parsing FXML - gestion des imports et des inclusions
**Type** : Story
**Priorite** : P1
**Estimation** : 8 SP
**Dependances** : JAS-302

**Description** : Ameliorer `FxmlCartographyAnalysisAdapter` pour extraire les imports de classes, les inclusions `fx:include`, les bindings de proprietes et les CSS stylesheets references.

**Criteres d'acceptation** :
- CA-1 : Les processing instructions `<?import ...?>` sont extraites et listees.
- CA-2 : Les `fx:include` sont detectes et la reference au fichier inclus est capturee.
- CA-3 : Les bindings de type `${...}` dans les attributs sont detectes.
- CA-4 : Les `stylesheets` references sont listees.
- CA-5 : Le domain model `FxmlComponent` est enrichi ou un nouveau record `FxmlImport`, `FxmlInclude` est cree.
- CA-6 : Aucune dependance externe ajoutee (JDK DOM suffit).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-601-T1 | Enrichir ou creer les records domain : `FxmlImport`, `FxmlInclude`, `FxmlBinding` | 2 SP |
| JAS-601-T2 | Modifier `FxmlCartographyAnalysisAdapter.parseXmlComponents()` pour les imports et includes | 3 SP |
| JAS-601-T3 | Detecter les bindings `${...}` | 1 SP |
| JAS-601-T4 | Tests unitaires avec FXML reels contenant includes, bindings, imports | 2 SP |

---

### JAS-602 : Enrichir l'extraction de regles - analyse AST Java via JavaParser
**Type** : Story
**Priorite** : P1
**Estimation** : 13 SP
**Dependances** : JAS-303

**Description** : Remplacer l'analyse regex de `JavaControllerRuleExtractionAdapter` par une analyse AST via JavaParser pour extraire les numeros de ligne, la complexite cyclomatique, les appels de methodes et les annotations.

**Criteres d'acceptation** :
- CA-1 : Dependance `com.github.javaparser:javaparser-core` ajoutee au `pom.xml`.
- CA-2 : Chaque `BusinessRule` a un `sourceLine` correct (pas 0).
- CA-3 : La complexite cyclomatique de chaque methode est calculee.
- CA-4 : Les appels de methodes sont traces (qui appelle quoi).
- CA-5 : L'adapter reste un adaptateur hexagonal (pas de logique metier dedans, juste extraction).
- CA-6 : Fallback sur l'extraction regex si le parsing AST echoue.
- CA-7 : Performance acceptable : un fichier Java de 5000 lignes parse en moins de 2 secondes.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-602-T1 | Ajouter JavaParser au pom.xml (scope adapters uniquement) | 1 SP |
| JAS-602-T2 | Creer `JavaParserRuleExtractionAdapter` implementant `RuleExtractionPort` | 5 SP |
| JAS-602-T3 | Visiteur AST pour extraction des handlers, injections, champs FXML | 3 SP |
| JAS-602-T4 | Calcul de complexite cyclomatique par methode | 2 SP |
| JAS-602-T5 | Tests sur fichiers Java representatifs (controller simple, controller complexe) | 2 SP |

---

### JAS-603 : Generation de code contextuelle basee sur la classification
**Type** : Story
**Priorite** : P1
**Estimation** : 8 SP
**Dependances** : JAS-602, JAS-305

**Description** : Faire evoluer `RealCodeGenerationAdapter` pour generer du code base sur les regles metier extraites au lieu de squelettes generiques.

**Criteres d'acceptation** :
- CA-1 : Le port `CodeGenerationPort` recoit aussi la `ClassificationResult` (ou un enrichissement du contrat).
- CA-2 : Les use cases generes correspondent aux handlers identifies comme APPLICATION ou BUSINESS.
- CA-3 : Les policies generees correspondent aux regles classifiees BUSINESS.
- CA-4 : Les gateways generees correspondent aux regles classifiees TECHNICAL.
- CA-5 : Le code genere compile (validation syntaxique minimale).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-603-T1 | Modifier le port `CodeGenerationPort` : enrichir la signature ou creer un VO d'entree | 2 SP |
| JAS-603-T2 | Refactorer `RealCodeGenerationAdapter` pour utiliser la classification | 3 SP |
| JAS-603-T3 | Generateur de ViewModel base sur les champs FXML detectes | 1 SP |
| JAS-603-T4 | Tests avec classification reelle en entree | 2 SP |

---

### JAS-604 : Planification de migration contextuelle
**Type** : Story
**Priorite** : P2
**Estimation** : 5 SP
**Dependances** : JAS-602

**Description** : Faire evoluer `RealMigrationPlannerAdapter` pour adapter les lots et les niveaux de risque en fonction de la complexite reelle du controller (nombre de handlers, complexite cyclomatique, nombre de services injectes).

**Criteres d'acceptation** :
- CA-1 : Le port `MigrationPlannerPort` recoit la `ControllerCartography` et/ou la `ClassificationResult`.
- CA-2 : Le nombre de lots peut varier (3 a 5) selon la complexite.
- CA-3 : Les niveaux de risque sont calcules en fonction de metriques reelles.
- CA-4 : Les actions par lot sont specifiques au controller analyse.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-604-T1 | Enrichir le port `MigrationPlannerPort` | 1 SP |
| JAS-604-T2 | Implementer la logique d'adaptation des lots | 2 SP |
| JAS-604-T3 | Tests avec differents niveaux de complexite | 2 SP |

---

### JAS-605 : Restitution enrichie avec markdown et metriques
**Type** : Story
**Priorite** : P2
**Estimation** : 3 SP
**Dependances** : JAS-602, JAS-603

**Description** : Enrichir `MarkdownRestitutionFormatterAdapter` pour produire un rapport markdown avec metriques, graphiques textuels et liens vers les artefacts generes.

**Criteres d'acceptation** :
- CA-1 : Le rapport inclut un tableau de metriques (nombre de composants, handlers, regles, artefacts).
- CA-2 : Le rapport inclut un diagramme de repartition des responsabilites (texte).
- CA-3 : Le rapport est structurable en sections navigables.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-605-T1 | Enrichir le formatteur markdown | 2 SP |
| JAS-605-T2 | Tests unitaires du rapport genere | 1 SP |

---

### Risques EPIC-04

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-10 | JavaParser non compatible JDK 21 records ou sealed classes | Eleve | Faible | Verifier la compatibilite avant integration, version >= 3.25 |
| R-11 | Modification du port `CodeGenerationPort` casse le contrat existant | Eleve | Moyenne | Versionner le port ou utiliser surcharge + default method |
| R-12 | Performance de l'AST parsing sur gros fichiers | Moyen | Moyenne | Benchmark + fallback regex |
| R-13 | Le code genere ne compile pas | Moyen | Haute | Validation syntaxique via JavaParser.parseCompilationUnit() |

---

## EPIC-05 DevOps CI/CD

**Objectif** : Pipeline CI/CD, containerisation backend, scripts de build.

### JAS-701 : Dockerfile backend multi-stage
**Type** : Story
**Priorite** : P2
**Estimation** : 5 SP
**Dependances** : Aucune

**Description** : Creer un Dockerfile multi-stage pour le backend Spring Boot avec JDK 21.

**Criteres d'acceptation** :
- CA-1 : Stage build : Maven + JDK 21, compilation + tests.
- CA-2 : Stage runtime : JRE 21 slim, healthcheck integre.
- CA-3 : Image finale < 300 Mo.
- CA-4 : Variables d'environnement pour la config PostgreSQL.
- CA-5 : Non-root user dans l'image finale.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-701-T1 | Ecrire le Dockerfile multi-stage | 2 SP |
| JAS-701-T2 | docker-compose.yml avec PostgreSQL et backend | 2 SP |
| JAS-701-T3 | Documentation du build local | 1 SP |

---

### JAS-702 : Pipeline CI GitHub Actions
**Type** : Story
**Priorite** : P2
**Estimation** : 8 SP
**Dependances** : JAS-701

**Description** : Pipeline CI pour build, tests, analyse de qualite.

**Criteres d'acceptation** :
- CA-1 : Declenchement sur push main et PR.
- CA-2 : Job backend : compile, test unitaire, test integration avec PostgreSQL (service container).
- CA-3 : Job frontend : npm install, lint, build, test.
- CA-4 : Cache Maven et npm.
- CA-5 : Rapport de couverture de tests publie en commentaire PR.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-702-T1 | Workflow backend (.github/workflows/backend.yml) | 3 SP |
| JAS-702-T2 | Workflow frontend (.github/workflows/frontend.yml) | 2 SP |
| JAS-702-T3 | Workflow integration (PostgreSQL service container) | 2 SP |
| JAS-702-T4 | Publication couverture tests | 1 SP |

---

### JAS-703 : Dockerfile frontend (Nginx)
**Type** : Story
**Priorite** : P2
**Estimation** : 3 SP
**Dependances** : Aucune

**Description** : Dockerfile Angular avec build prod et serving Nginx.

**Criteres d'acceptation** :
- CA-1 : Stage build : Node + Angular CLI, build production.
- CA-2 : Stage runtime : Nginx alpine, config reverse proxy vers API backend.
- CA-3 : Image < 100 Mo.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-703-T1 | Dockerfile frontend multi-stage | 1.5 SP |
| JAS-703-T2 | Config Nginx avec reverse proxy /api | 1 SP |
| JAS-703-T3 | Integration dans docker-compose | 0.5 SP |

---

### JAS-704 : Scripts de build et de demarrage local
**Type** : Story
**Priorite** : P2
**Estimation** : 3 SP
**Dependances** : JAS-701, JAS-703

**Description** : Scripts shell pour demarrage local (dev et docker).

**Criteres d'acceptation** :
- CA-1 : `scripts/dev-start.sh` : lance PostgreSQL Docker + backend Maven + frontend ng serve.
- CA-2 : `scripts/docker-start.sh` : lance tout via docker-compose.
- CA-3 : `scripts/db-reset.sh` : reinitialise la base de donnees.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-704-T1 | Scripts dev et docker | 2 SP |
| JAS-704-T2 | Documentation dans README racine | 1 SP |

---

### Risques EPIC-05

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-14 | Temps de build CI trop long (> 10 min) | Faible | Moyenne | Caching agressif Maven/npm, parallelisation jobs |
| R-15 | Incompatibilite version JDK image Docker vs projet | Moyen | Faible | Fixer la version JDK dans le Dockerfile |

---

## EPIC-06 Observabilite et exploitation

**Objectif** : Logs structures, correlation, metriques, health checks.

**Etat actuel observe** :
- `CorrelationFilter` existant (injecte un correlation-id dans les headers).
- `SecurityHeadersFilter` existant.
- Logs via SLF4J/Logback.
- Pas de metriques Micrometer/Actuator visibles.

### JAS-801 : Logs structures JSON en production
**Type** : Story
**Priorite** : P2
**Estimation** : 3 SP
**Dependances** : Aucune

**Description** : Configurer Logback pour produire des logs JSON structures en profil production, avec correlation-id, session-id et timestamp ISO.

**Criteres d'acceptation** :
- CA-1 : En profil `prod`, les logs sont en JSON (logstash-logback-encoder).
- CA-2 : En profil `dev`, les logs restent en texte lisible.
- CA-3 : Le correlation-id du header HTTP est inclus dans chaque ligne de log.
- CA-4 : Le session-id de l'analyse en cours est inclus quand disponible (MDC).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-801-T1 | Ajouter logstash-logback-encoder au pom.xml | 0.5 SP |
| JAS-801-T2 | Configurer logback-spring.xml avec profils | 1.5 SP |
| JAS-801-T3 | Enrichir `CorrelationFilter` pour pousser session-id en MDC | 1 SP |

---

### JAS-802 : Health checks Actuator
**Type** : Story
**Priorite** : P2
**Estimation** : 5 SP
**Dependances** : Aucune

**Description** : Activer Spring Boot Actuator avec health, info, metrics. Creer un health indicator custom pour la base de donnees et le moteur d'analyse.

**Criteres d'acceptation** :
- CA-1 : `/actuator/health` retourne UP/DOWN avec detail base de donnees.
- CA-2 : `/actuator/info` retourne version, git commit, build timestamp.
- CA-3 : Health indicator custom `AnalysisEngineHealthIndicator` verifie que les ports sortants sont fonctionnels.
- CA-4 : Endpoints actuator securises (pas exposes publiquement).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-802-T1 | Ajouter spring-boot-starter-actuator | 0.5 SP |
| JAS-802-T2 | Configurer les endpoints exposes | 1 SP |
| JAS-802-T3 | Creer `AnalysisEngineHealthIndicator` | 2 SP |
| JAS-802-T4 | Configurer info (git, build) | 1 SP |
| JAS-802-T5 | Securiser les endpoints actuator | 0.5 SP |

---

### JAS-803 : Metriques Micrometer pour les etapes d'analyse
**Type** : Story
**Priorite** : P2
**Estimation** : 5 SP
**Dependances** : JAS-802

**Description** : Instrumenter le pipeline d'analyse avec des metriques Micrometer (timers, compteurs).

**Criteres d'acceptation** :
- CA-1 : Timer sur chaque etape du pipeline (ingestion, cartographie, classification, plan, generation, restitution).
- CA-2 : Compteur de sessions par statut (CREATED, IN_PROGRESS, COMPLETED, FAILED).
- CA-3 : Metriques exposees via `/actuator/prometheus` (si Prometheus est configure) ou `/actuator/metrics`.
- CA-4 : Les metriques sont injectees via le `MeterRegistry`, pas de couplage direct.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-803-T1 | Ajouter micrometer-registry-prometheus au pom.xml | 0.5 SP |
| JAS-803-T2 | Instrumenter `AnalysisOrchestrationService` avec des timers | 2 SP |
| JAS-803-T3 | Compteur de sessions par statut dans `JpaAnalysisSessionAdapter` | 1 SP |
| JAS-803-T4 | Tests d'integration verifiant l'emission des metriques | 1.5 SP |

---

### JAS-804 : Dashboard de monitoring frontend
**Type** : Story
**Priorite** : P3
**Estimation** : 5 SP
**Dependances** : JAS-803

**Description** : Page Angular affichant les metriques cles du backend (sessions actives, taux de succes, temps moyen par etape).

**Criteres d'acceptation** :
- CA-1 : Page `/monitoring` accessible depuis le menu.
- CA-2 : Affiche : sessions totales, sessions par statut, temps moyen par etape.
- CA-3 : Appel a `/actuator/metrics` via service Angular.
- CA-4 : Rafraichissement automatique toutes les 30 secondes.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-804-T1 | Service Angular `MetricsApiService` | 1 SP |
| JAS-804-T2 | Composant `monitoring-dashboard.component.ts` | 3 SP |
| JAS-804-T3 | Route + navigation | 1 SP |

---

### Risques EPIC-06

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-16 | Les logs JSON augmentent significativement le volume de stockage | Faible | Moyenne | Rotation de logs, niveaux configurables |
| R-17 | Actuator expose des informations sensibles | Eleve | Moyenne | Securiser via Spring Security, limiter les endpoints |

---

## EPIC-07 Documentation technique

**Objectif** : ADR, guides developpeur, docs d'integration.

### JAS-901 : ADR pour les decisions d'architecture Phase 3
**Type** : Story
**Priorite** : P3
**Estimation** : 3 SP
**Dependances** : Aucune

**Description** : Documenter les decisions d'architecture prises en Phase 3 sous forme d'Architecture Decision Records.

**Criteres d'acceptation** :
- CA-1 : ADR-001 : Choix JavaParser pour l'analyse AST.
- CA-2 : ADR-002 : Schema de persistance normalise vs JSONB.
- CA-3 : ADR-003 : Strategie de profils Spring (stub/prod/test).
- CA-4 : ADR-004 : Choix de la librairie de coloration syntaxique frontend.
- CA-5 : Format standard : contexte, decision, consequences, statut.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-901-T1 | Ecrire les 4 ADR dans docs/adr/ | 3 SP |

---

### JAS-902 : Guide developpeur backend
**Type** : Story
**Priorite** : P3
**Estimation** : 3 SP
**Dependances** : JAS-701, JAS-301

**Description** : Guide pour les developpeurs rejoignant le projet : setup local, conventions, architecture hexagonale, profils, tests.

**Criteres d'acceptation** :
- CA-1 : Instructions de setup local (JDK, Maven, PostgreSQL, Docker).
- CA-2 : Explication de l'architecture hexagonale avec schema.
- CA-3 : Conventions de code (methodes <= 30 lignes, imbrication <= 3, etc.).
- CA-4 : Guide des profils Spring.
- CA-5 : Guide des tests (unitaire, integration, pattern test doubles).

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-902-T1 | Ecrire docs/developer-guide-backend.md | 3 SP |

---

### JAS-903 : Guide d'integration frontend-backend
**Type** : Story
**Priorite** : P3
**Estimation** : 3 SP
**Dependances** : JAS-501 a JAS-505

**Description** : Documenter les contrats d'API, les modeles TypeScript, les conventions d'appel et la gestion d'erreurs entre Angular et Spring Boot.

**Criteres d'acceptation** :
- CA-1 : Mapping complet endpoints REST <-> services Angular.
- CA-2 : Schema des DTOs avec exemples JSON.
- CA-3 : Gestion du correlation-id cote frontend.
- CA-4 : Pattern de gestion d'erreurs HTTP.

**Taches techniques** :
| ID | Tache | Estimation |
|----|-------|-----------|
| JAS-903-T1 | Ecrire docs/integration-guide.md | 3 SP |

---

### Risques EPIC-07

| ID | Risque | Impact | Probabilite | Mitigation |
|----|--------|--------|-------------|-----------|
| R-18 | Documentation obsolete rapidement si le code evolue | Moyen | Haute | Lier les docs aux tickets, revue a chaque sprint |

---

## Dependances inter-epics

```
EPIC-02 (Database)
  |
  +-- JAS-401 ------> JAS-302 (EPIC-01 : Persist cartographie)
  +-- JAS-402 ------> JAS-303 (EPIC-01 : Persist classification)
  +-- JAS-403 ------> JAS-304 (EPIC-01 : Persist migration plan)
  +-- JAS-404 ------> JAS-305 (EPIC-01 : Persist artifacts)
  +-- JAS-405 ------> JAS-306 (EPIC-01 : Persist restitution)
  |
EPIC-01 (Backend) + EPIC-02 (Database)
  |
  +-- JAS-302 ------> JAS-501 (EPIC-03 : Cartography view)
  +-- JAS-303 ------> JAS-502 (EPIC-03 : Classification view)
  +-- JAS-304 ------> JAS-503 (EPIC-03 : Migration plan view)
  +-- JAS-305 ------> JAS-504 (EPIC-03 : Artifacts view)
  +-- JAS-306 ------> JAS-505 (EPIC-03 : Report view)
  |
EPIC-01 (Backend)
  |
  +-- JAS-302/303 --> JAS-601/602 (EPIC-04 : Enrichir analyse)
  +-- JAS-602 ------> JAS-603 (EPIC-04 : Generation contextuelle)
  +-- JAS-602 ------> JAS-604 (EPIC-04 : Planification contextuelle)
  |
EPIC-05 (DevOps) : independant, peut demarrer en parallele
EPIC-06 (Observabilite) : independant, peut demarrer en parallele
EPIC-07 (Documentation) : en fin de phase, depend de la stabilisation
```

### Matrice de dependances critiques (chemin critique)

```
JAS-401 -> JAS-302 -> JAS-501
                   -> JAS-601
JAS-402 -> JAS-303 -> JAS-502
                   -> JAS-602 -> JAS-603
                              -> JAS-604
JAS-403 -> JAS-304 -> JAS-503
JAS-404 -> JAS-305 -> JAS-504
JAS-405 -> JAS-306 -> JAS-505
```

Le chemin critique le plus long est :
**JAS-402 -> JAS-303 -> JAS-602 -> JAS-603** (5 + 8 + 13 + 8 = 34 SP)

---

## Risques globaux

| ID | Risque | Impact | Probabilite | Mitigation | Owner |
|----|--------|--------|-------------|-----------|-------|
| R-G1 | Les adapters reels sont deja partiellement implementes mais pas testes en conditions reelles | Eleve | Haute | Ecrire des tests d'integration avec des fichiers JavaFX reels en priorite | backend-hexagonal |
| R-G2 | Le schema PostgreSQL etendu cree un couplage fort avec le domain model | Eleve | Moyenne | Mapper explicites entity/domain, pas de partage de classes | database-postgres |
| R-G3 | Angular 21.x n'est pas valide pour les dependances tierces (Prism.js, etc.) | Moyen | Moyenne | Valider la compatibilite Angular avant JAS-504 | frontend-angular |
| R-G4 | JavaParser ajoute une dependance lourde au projet | Moyen | Faible | Scope compile uniquement dans le module adapters | implementation-moteur-analyse |
| R-G5 | Le pipeline d'orchestration (`AnalysisOrchestrationService`) n'a pas de gestion de reprise | Eleve | Moyenne | Ajouter un mecanisme de reprise par etape (hors scope Phase 3, a anticiper) | gouvernance |

---

## Plan de lots progressifs

### Lot A - Fondations (Sprint 1-2)
**Objectif** : Schema de base + profils actifs + CI

| Ticket | Epic | SP | Parallele avec |
|--------|------|----|---------------|
| JAS-401 | DB | 5 | JAS-701, JAS-801, JAS-802 |
| JAS-402 | DB | 5 | JAS-701, JAS-801, JAS-802 |
| JAS-301 | Backend | 3 | JAS-401 |
| JAS-701 | DevOps | 5 | JAS-401, JAS-402 |
| JAS-801 | Observabilite | 3 | tout |
| JAS-802 | Observabilite | 5 | tout |
| **Sous-total** | | **26** | |

### Lot B - Persistance complete (Sprint 3-4)
**Objectif** : Tous les resultats d'analyse persistes en base

| Ticket | Epic | SP | Prerequis |
|--------|------|----|----------|
| JAS-302 | Backend | 8 | JAS-401 |
| JAS-303 | Backend | 8 | JAS-402 |
| JAS-403 | DB | 5 | JAS-402 |
| JAS-404 | DB | 5 | JAS-403 |
| JAS-405 | DB | 5 | JAS-404 |
| JAS-702 | DevOps | 8 | JAS-701 |
| **Sous-total** | | **39** | |

### Lot C - Persistance restante + debut UX (Sprint 5-6)
**Objectif** : Derniers adapters de persistance + premiers composants Angular

| Ticket | Epic | SP | Prerequis |
|--------|------|----|----------|
| JAS-304 | Backend | 5 | JAS-403 |
| JAS-305 | Backend | 8 | JAS-404 |
| JAS-306 | Backend | 5 | JAS-405 |
| JAS-406 | DB | 3 | JAS-405 |
| JAS-501 | Frontend | 5 | JAS-302 |
| JAS-502 | Frontend | 5 | JAS-303 |
| JAS-703 | DevOps | 3 | - |
| **Sous-total** | | **34** | |

### Lot D - UX + Moteur enrichi (Sprint 7-8)
**Objectif** : Composants Angular restants + analyse AST

| Ticket | Epic | SP | Prerequis |
|--------|------|----|----------|
| JAS-503 | Frontend | 5 | JAS-304 |
| JAS-504 | Frontend | 8 | JAS-305 |
| JAS-505 | Frontend | 5 | JAS-306 |
| JAS-601 | Moteur | 8 | JAS-302 |
| JAS-602 | Moteur | 13 | JAS-303 |
| JAS-803 | Observabilite | 5 | JAS-802 |
| **Sous-total** | | **44** | |

### Lot E - Finalisation (Sprint 9-10)
**Objectif** : Generation contextuelle + refactoring frontend + docs

| Ticket | Epic | SP | Prerequis |
|--------|------|----|----------|
| JAS-506 | Frontend | 5 | JAS-501..505 |
| JAS-603 | Moteur | 8 | JAS-602 |
| JAS-604 | Moteur | 5 | JAS-602 |
| JAS-605 | Moteur | 3 | JAS-602, JAS-603 |
| JAS-704 | DevOps | 3 | JAS-701, JAS-703 |
| JAS-804 | Observabilite | 5 | JAS-803 |
| JAS-901 | Doc | 3 | - |
| JAS-902 | Doc | 3 | JAS-701 |
| JAS-903 | Doc | 3 | JAS-501..505 |
| **Sous-total** | | **38** | |

---

## Totaux

| Metrique | Valeur |
|----------|--------|
| Nombre d'epics | 7 |
| Nombre de stories | 33 |
| Nombre de taches techniques | ~90 |
| Story points totaux | 188 |
| Lots progressifs | 5 (A a E) |
| Sprints estimes | 10 (2 semaines chacun) |
| Chemin critique (SP) | 34 (JAS-402 -> JAS-303 -> JAS-602 -> JAS-603) |

---

## Contrat inter-agent (sortie jira-estimation)

### objectif
Produire le decoupage JIRA complet de la Phase 3 avec estimation, dependances et risques.

### perimetre
7 axes de la Phase 3 : backend hexagonal, database, frontend, moteur d'analyse, devops, observabilite, documentation.

### faits
- 4 stubs existent sous profil "stub", 4 adapters reels existent deja mais sans persistance en base.
- 1 seule table Flyway existe (`analysis_session`).
- Le frontend affiche tout en JSON brut.
- Les adapters reels utilisent du regex/DOM XML, pas d'analyse AST.
- Le `RealCodeGenerationAdapter` genere des squelettes avec TODO.
- Le `AnalysisOrchestrationService` orchestre les 6 etapes sans persistance intermediaire.

### interpretations
- La Phase 3 est principalement un travail de persistance (base + adapters JPA) et d'enrichissement de l'existant.
- Le chemin critique passe par le schema de base de donnees et la persistance des classifications avant de pouvoir enrichir le moteur d'analyse.
- Les adapters reels sont fonctionnels mais superficiels (regex). Le saut qualitatif majeur est l'introduction de JavaParser (JAS-602, 13 SP).

### hypotheses
- H-1 : JavaParser >= 3.25 est compatible JDK 21 et Spring Boot 4.0.3.
- H-2 : Angular 21.x supporte les dependances de coloration syntaxique visees.
- H-3 : Les sprints sont de 2 semaines avec une velocite de 15-20 SP par sprint.
- H-4 : PostgreSQL JSONB est le bon choix pour les listes de strings (contradictions, findings, etc.).

### incertitudes
- I-1 : La strategie exacte de reprise du pipeline apres echec partiel n'est pas definie (hors scope Phase 3).
- I-2 : Le besoin d'un cache de resultats d'analyse n'est pas arbitre.
- I-3 : La necessite de pagination sur les endpoints de listing de sessions n'est pas specifiee.

### decisions
- D-1 : Les migrations Flyway sont sequentielles (V2 a V7) pour assurer la reproductibilite.
- D-2 : Les listes dans le rapport de restitution sont stockees en JSONB (pas en tables enfants).
- D-3 : JavaParser est introduit comme dependance dans le module adapters uniquement.
- D-4 : Les stubs sont conserves sous profil "stub" pour les tests rapides, pas supprimes.
- D-5 : Chaque composant Angular d'affichage de resultat est standalone et reutilisable.

### livrables
- `docs/phase3-jira-tickets.md` (ce document)

### dependances
- Prerequis : Phase 2 terminee (confirme par les commits existants).
- Co-requis : Aucun blocage inter-phase.

### verifications
- Le decoupage couvre les 7 axes de la Phase 3.
- Chaque story a des criteres d'acceptation verifiables.
- Les dependances forment un DAG sans cycle.
- Le plan de lots progressifs respecte les dependances.

### handoff
```
handoff:
  vers: gouvernance
  preconditions:
    - Decoupage JIRA valide et arbitre par l'equipe
    - Risques globaux revus et acceptes
  points_de_vigilance:
    - Le chemin critique (34 SP) doit etre lance en priorite
    - La compatibilite JavaParser / JDK 21 doit etre verifiee avant JAS-602
    - La compatibilite Angular 21.x / Prism.js doit etre verifiee avant JAS-504
  artefacts_a_consulter:
    - docs/phase3-jira-tickets.md
    - agents/contracts.md
    - agents/orchestration.md

handoff secondaires:
  vers: backend-hexagonal
  preconditions:
    - JAS-301 et JAS-401/402 prets a demarrer
  artefacts_a_consulter:
    - Sections EPIC-01 et EPIC-02 de ce document

  vers: frontend-angular
  preconditions:
    - JAS-302/303 termines (donnees reelles disponibles)
  artefacts_a_consulter:
    - Section EPIC-03 de ce document

  vers: implementation-moteur-analyse
  preconditions:
    - JAS-602 (JavaParser) arbitre et pom.xml valide
  artefacts_a_consulter:
    - Section EPIC-04 de ce document
```
