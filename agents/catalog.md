# Catalogue Des Agents

## Agents de pilotage et de construction

| Agent | Mission | Livrables principaux | Modele | Appuis |
| --- | --- | --- | --- | --- |
| architecture-applicative | Cadrer la solution cible a partir du guide et des contraintes du repo | architecture cible, modules, roadmap, limites de perimetre | GPT-5.4 | guide, `backend/`, `agents/contracts.md` |
| product-owner-fonctionnel | Transformer le guide en parcours utilisateur et backlog fonctionnel | parcours, user stories, criteres d'acceptation | GPT-5.4 | guide, `agents/orchestration.md` |
| analyste-regles-metier | Extraire et formuler les regles stables du refactoring | catalogue de regles, invariants, decisions metier | GPT-5.4 | guide, `javafx-business-rules-extractor` |
| api-contrats | Definir les contrats echanges front/back et moteur/backend | endpoints, DTO, schemas, versioning | GPT-5.4 | `backend/`, `agents/contracts.md` |
| securite | Poser les controles de securite et d'exposition | menaces, exigences, controles, journalisation | GPT-5.4 | contrats, backend |
| transparence-openai | Auditer la surface de donnees, prompts et fichiers exposes a OpenAI | rapport Markdown, inventaire de fichiers, trace agent observable | GPT-5.4 | `AGENTS.md`, `AGENTS-claude.md`, `agents/`, `.codex/`, prompts du repo |
| transparence-claude | Auditer la surface de donnees, prompts, permissions et fichiers exposes a Claude Code | rapport Markdown, inventaire de fichiers, trace agent observable | claude-sonnet-4-6 | `AGENTS.md`, `AGENTS-claude.md`, `agents/`, `.claude/`, `.codex/`, prompts du repo |
| architecture-moteur-analyse | Decouper le moteur d'analyse/refactoring en composants implementables | architecture du moteur, interfaces, flux de donnees | GPT-5.4 | guide, `.codex` skills |
| audit-qualite-analyse | Evaluer la fiabilite des analyses et garde-fous | score de confiance, alertes, checklist de qualite | GPT-5.4 | `javafx-quality-smell-detector` |
| jira-estimation | Transformer le scope en epics/stories/tasks | backlog estime, dependances, risques | GPT-5.4 | guide, architecture cible |
| gouvernance | Arbitrer les ecarts et valider la coherence globale | decisions, validations, ecarts, non-regressions | GPT-5.4 | tous les artefacts |
| backend-hexagonal | Implementer le backend autour des cas d'usage et ports | code backend, packages, adapters, endpoints | GPT-5.3-codex | `backend/`, contrats |
| database-postgres | Concevoir la persistence et les adaptateurs de donnees | schema, migrations, repositories/adapters | GPT-5.3-codex | backend, contrats |
| frontend-angular | Construire l'interface conversationnelle Angular | shell Angular, pages, services API, etats UI | GPT-5.3-codex | contrats, Angular 21.x |
| implementation-moteur-analyse | Coder les modules du moteur d'analyse et de generation | ingestion, analyse, consolidation, generation | GPT-5.3-codex | guide, `.codex` skills |
| devops-ci-cd | Industrialiser build, tests et livraison | pipelines, scripts, packaging, environnements | GPT-5.3-codex | backend, frontend |
| revue-code | Revoir les lots sous angle risques et regressions | findings classes, recommandations, gaps | GPT-5.4 | code produit |
| qa-backend | Definir et verifier la qualite backend | plan de tests backend, risques, criteres de sortie | GPT-5.4 | backend, contrats |
| qa-frontend | Definir et verifier la qualite frontend | plan de tests frontend, risques, criteres de sortie | GPT-5.4 | frontend, contrats |
| test-automation | Ecrire et maintenir les tests automatises | TU, TI, tests Angular, fixtures | GPT-5.3-codex | skill `test-automation-agent` |
| observabilite-exploitation | Poser la journalisation et la correlation | logs, traces, configs debug, dashboards | GPT-5.3-codex | backend, frontend |
| documentation-technique | Documenter architecture, lots et exploitation | guides dev, ADR, docs d'integration | GPT-5.4 | tous les artefacts |

## Agents du moteur d'analyse

| Agent | Mission | Livrables principaux | Modele | Appuis |
| --- | --- | --- | --- | --- |
| source-ingestion | Lire et normaliser fichiers sources et artefacts lies | inventaire, texte prepare, liens entre fichiers | GPT-5.3-codex | echantillons source |
| fxml-analysis | Cartographier FXML, fx:id, evenements et zones UI | carte d'ecran, composants, handlers lies | GPT-5.3-codex | `javafx-screen-cartographer` |
| controller-analysis | Reconstituer les flux du controller et ses dependances | flux, etats, handlers, appels externes | GPT-5.3-codex | `javafx-controller-flow-analyzer` |
| inheritance-analysis | Evaluer les heritages quand une hierarchie non triviale est detectee | hierarchies utiles, effets de bord, contraintes implicites | GPT-5.4 | code Java |
| spring-analysis | Evaluer injections, services Spring et coutures techniques | carte Spring, points d'integration, dependances | GPT-5.3-codex | code Java, contexte Spring |
| pdf-analysis | Extraire et relier les contenus PDF/DOCX quand des documents sont fournis | evidences documentaires optionnelles, limites, sources | GPT-5.3-codex | documents fournis |
| dynamic-ui-analysis | Reconstituer les comportements UI non declares quand le FXML statique ne suffit pas | flux dynamiques utiles, listeners, zones invisibles | GPT-5.4 | controller, FXML |
| consolidation | Fusionner les analyses et arbitrer les contradictions ; module obligatoire du MVP | sortie consolidee, scores, alertes, arbitrage final | GPT-5.4 | toutes les analyses |
| restitution | Produire une restitution exploitable par le produit | rapport final, JSON, priorites, risques | GPT-5.4 | consolidation, `javafx-report-writer` |

## Skills `.codex` de support

| Skill | Usage principal | Modele | Consomme |
| --- | --- | --- | --- |
| javafx-orchestrator | Orchestrer les analyses d'ecrans/controllers | GPT-5.4 | tous les skills JavaFX |
| javafx-screen-cartographer | Cartographier un ecran FXML | GPT-5.3-codex | FXML |
| javafx-controller-flow-analyzer | Reconstituer les flux d'un controller | GPT-5.3-codex | controller Java |
| javafx-business-rules-extractor | Extraire les regles de gestion | GPT-5.4 | controller, services, validations |
| javafx-quality-smell-detector | Detecter dette et anti-patterns | GPT-5.4 | code JavaFX |
| javafx-best-practices-researcher | Comparer a la stack cible | GPT-5.4 | code, architecture |
| javafx-test-strategy-advisor | Prioriser les tests et preconditions | GPT-5.4 | code, plan de refactoring |
| javafx-refactoring-planner | Produire la migration par lots | GPT-5.4 | analyses consolidees |
| javafx-report-writer | Rediger la restitution finale | GPT-5.4 | sorties consolidees |
| openai-transparency-reporter | Produire un rapport Markdown de transparence OpenAI | GPT-5.4 | `agents/`, `.codex/prompts`, fichiers lus dans la session |
| claude-transparency-reporter | Produire un rapport Markdown de transparence Claude Code | claude-sonnet-4-6 | `agents/`, `.claude/settings.local.json`, `.codex/prompts`, fichiers lus dans la session |



