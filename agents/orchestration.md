# Orchestration Des Agents

## Cadre

Cette orchestration sert a construire la nouvelle application de refactoring progressif de controllers JavaFX + Spring a partir du guide `guide_generique_refactoring_controller_javafx_spring.md`.

Date de reference : `2026-03-16`

Stack de reference :

- frontend : Angular `21.x` a revalider avant bootstrap ;
- backend : JDK 21 + Spring Boot `4.0.3` dans `backend/` ;
- persistance cible : PostgreSQL ;
- architecture backend : hexagonale stricte ;
- moteur de refactoring : analyse structurelle + classification des responsabilites + strategie de migration + generation de code + explication.

## Phases

### Phase 1 - Cadrage produit

Agents :

- `architecture-applicative`
- `product-owner-fonctionnel`
- `analyste-regles-metier`
- `api-contrats`
- `securite`
- `transparence-openai`

Sorties attendues :

- vision produit consolidee ;
- module mappee sur le guide ;
- contrats d'API et DTO candidats ;
- contraintes de securite ;
- rapport initial de transparence OpenAI ;
- definition de l'interaction conversationnelle.

## Phase 2 - Architecture du moteur

Agents :

- `architecture-moteur-analyse`
- `audit-qualite-analyse`
- `gouvernance`

Sorties attendues :

- decomposition en modules `ingestion`, `analyse structurelle`, `classification`, `strategie`, `generation`, `explication`, `conversation` ;
- schema des entrees/sorties du moteur ;
- criteres de qualite et de confiance ;
- regles d'audit des analyses.

## Phase 3 - Implementation produit

Agents :

- `backend-hexagonal`
- `database-postgres`
- `frontend-angular`
- `implementation-moteur-analyse`
- `devops-ci-cd`
- `observabilite-exploitation`
- `documentation-technique`

Sorties attendues :

- backend structure par couches et cas d'usage ;
- contrats REST exposes ;
- frontend Angular de pilotage et de visualisation ;
- composants du moteur relies au backend ;
- pipelines et conventions d'observabilite ;
- documentation d'integration.

## Phase 4 - Industrialisation du moteur d'analyse

Agents :

- `source-ingestion`
- `fxml-analysis`
- `controller-analysis`
- `inheritance-analysis`
- `spring-analysis`
- `pdf-analysis`
- `dynamic-ui-analysis`
- `consolidation`
- `restitution`

Sorties attendues :

- extraction des sources et artefacts lies ;
- cartographie FXML/controller/services ;
- reconstitution des flux et des regles ;
- consolidation des evidences ;
- restitution exploitable par le backend et le frontend.

## Phase 5 - Relecture et qualite

Agents :

- `revue-code`
- `qa-backend`
- `qa-frontend`
- `test-automation`
- `gouvernance`

Sorties attendues :

- findings et regressions potentielles ;
- couverture de tests cible ;
- ecarts aux contrats ;
- validation de sortie de lot.

## Ordre d'execution recommande

1. `architecture-applicative`
2. `product-owner-fonctionnel`, `analyste-regles-metier`, `api-contrats`, `securite`, `transparence-openai`
3. `architecture-moteur-analyse`, `audit-qualite-analyse`
4. `backend-hexagonal`, `frontend-angular`, `database-postgres`, `implementation-moteur-analyse`
5. agents specialises du moteur en parallele sur echantillons reels
6. `devops-ci-cd`, `observabilite-exploitation`, `documentation-technique`
7. `revue-code`, `qa-backend`, `qa-frontend`, `test-automation`, `gouvernance`

## Appui des skills `.codex`

Les skills `.codex` servent de sous-agents specialises pendant les phases 2 a 5.

- `javafx-orchestrator` coordonne les analyses d'ecrans/controllers et consolide les sorties.
- `javafx-screen-cartographer` alimente `fxml-analysis`.
- `javafx-controller-flow-analyzer` alimente `controller-analysis`.
- `javafx-business-rules-extractor` alimente `analyste-regles-metier` et `consolidation`.
- `javafx-quality-smell-detector` alimente `audit-qualite-analyse` et `revue-code`.
- `javafx-best-practices-researcher` alimente `architecture-moteur-analyse` et `gouvernance`.
- `javafx-refactoring-planner` alimente `implementation-moteur-analyse` et `documentation-technique`.
- `javafx-test-strategy-advisor` alimente `test-automation`, `qa-backend` et `qa-frontend`.
- `javafx-report-writer` alimente `restitution` et la documentation de sortie.
- `openai-transparency-reporter` alimente `transparence-openai`.

## Regles de parallelisation

- Ne lancer les agents de production qu'apres validation des contrats et de la securite.
- Rejouer `transparence-openai` des qu'un nouveau prompt, skill ou flux de fichiers est introduit.
- Les agents analytiques peuvent travailler en parallele sur un meme controller seulement si `consolidation` reste unique.
- Toute ambiguite fonctionnelle remonte vers `architecture-applicative` et `gouvernance`.
- Toute ambiguite de version frontend remonte avant generation du squelette Angular.
