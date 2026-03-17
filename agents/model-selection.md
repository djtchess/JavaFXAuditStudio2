# Selection Des Modeles

## Principe

Utiliser `GPT-5.4` pour les taches a forte ambiguite, de synthese, d'arbitrage, de gouvernance, de securite, d'architecture ou d'explication.

Utiliser `GPT-5.3-codex` pour les taches de generation de code, de transformation structurelle, de wiring technique, de tests, de scripts et d'implementation pas a pas.

## Regles

### Preferer GPT-5.4 quand il faut

- consolider plusieurs sources contradictoires ;
- extraire ou arbitrer des regles metier ;
- definir des contrats ;
- decider d'une architecture cible ;
- produire une synthese lisible pour plusieurs publics ;
- prioriser des lots et evaluer le risque.

### Preferer GPT-5.3-codex quand il faut

- creer ou modifier des fichiers ;
- decliner un plan en classes, packages et endpoints ;
- produire des tests ;
- brancher des adaptateurs Spring, PostgreSQL ou Angular ;
- transformer du code existant en suivant une cible deja validee ;
- implementer des parseurs, assembleurs, strategies ou bridges transitoires.

## Affectation par agent produit

| Agent | Modele principal | Justification |
| --- | --- | --- |
| architecture-applicative | GPT-5.4 | Vision, arbitrages, decomposition produit |
| product-owner-fonctionnel | GPT-5.4 | Clarification des besoins et scenarios conversationnels |
| analyste-regles-metier | GPT-5.4 | Extraction et formulation des regles |
| api-contrats | GPT-5.4 | Conception des contrats et compatibilite front/back |
| securite | GPT-5.4 | Menaces, controles, contraintes transverses |
| transparence-openai | GPT-5.4 | Audit de donnees exposees, synthese et explicabilite |
| architecture-moteur-analyse | GPT-5.4 | Decomposition modulaire et decisions structurelles |
| audit-qualite-analyse | GPT-5.4 | Evaluation de fiabilite, scoring et garde-fous |
| jira-estimation | GPT-5.4 | Decoupage, estimation, dependances |
| gouvernance | GPT-5.4 | Validation transverse et arbitrage |
| backend-hexagonal | GPT-5.3-codex | Implementation backend conforme a l'architecture validee |
| database-postgres | GPT-5.3-codex | DDL, mapping, migrations, adaptateurs |
| frontend-angular | GPT-5.3-codex | Implementation Angular et integration API |
| implementation-moteur-analyse | GPT-5.3-codex | Code du moteur, parseurs, generateurs, services |
| devops-ci-cd | GPT-5.3-codex | Pipelines, scripts, packaging, automatisation |
| revue-code | GPT-5.4 | Revue critique et priorisation des risques |
| qa-backend | GPT-5.4 | Strategie de couverture et risques backend |
| qa-frontend | GPT-5.4 | Strategie de couverture et risques frontend |
| test-automation | GPT-5.3-codex | Ecriture des tests et fixtures |
| observabilite-exploitation | GPT-5.3-codex | Logs, traces, correlation, configuration |
| documentation-technique | GPT-5.4 | Synthese, guides d'usage, explications |

## Affectation par agent du moteur d'analyse

| Agent | Modele principal | Justification |
| --- | --- | --- |
| source-ingestion | GPT-5.3-codex | Lecture structuree, pre-traitement, normalisation |
| fxml-analysis | GPT-5.3-codex | Cartographie structurelle precise |
| controller-analysis | GPT-5.3-codex | Traçage des handlers, appels et etats |
| inheritance-analysis | GPT-5.4 | Raisonnement sur hierarchies et effets implicites |
| spring-analysis | GPT-5.3-codex | Detection des injections et des liens techniques |
| pdf-analysis | GPT-5.3-codex | Extraction technique de contenu PDF/DOCX |
| dynamic-ui-analysis | GPT-5.4 | Reconstitution de comportements dynamiques |
| consolidation | GPT-5.4 | Fusion, arbitrage, gestion des contradictions |
| restitution | GPT-5.4 | Narration, priorisation, restitution exploitable |

## Affectation par skill `.codex`

| Skill | Modele principal | Raison |
| --- | --- | --- |
| javafx-orchestrator | GPT-5.4 | Coordination, arbitrage, consolidation |
| javafx-screen-cartographer | GPT-5.3-codex | Cartographie structurelle de FXML |
| javafx-controller-flow-analyzer | GPT-5.3-codex | Lecture de code et reconstruction de flux |
| javafx-business-rules-extractor | GPT-5.4 | Interpretation des regles et conditions |
| javafx-quality-smell-detector | GPT-5.4 | Evaluation de risques et de maintainabilite |
| javafx-best-practices-researcher | GPT-5.4 | Mise en perspective avec la stack cible |
| javafx-test-strategy-advisor | GPT-5.4 | Strategie de couverture et priorisation |
| javafx-refactoring-planner | GPT-5.4 | Plan de migration progressif |
| javafx-report-writer | GPT-5.4 | Synthese finale actionnable |
| openai-transparency-reporter | GPT-5.4 | Audit Markdown des donnees, fichiers et traces agent |
