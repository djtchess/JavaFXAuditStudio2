# Configuration Multi-Agent Claude Code

## Objet

Ce document adapte la gouvernance multi-agent du depot pour Claude Code sans remplacer `AGENTS.md` ni les artefacts existants dans `/.codex`.

Il complete les sources de verite existantes et sert de reference commune aux sous-agents declares dans `/.claude/agents/`.

## Compatibilite Claude Code

- Au 16 mars 2026, Claude Code supporte les sous-agents projet via des fichiers Markdown avec frontmatter YAML places dans `/.claude/agents/`.
- Ce depot utilise donc `/.claude/agents/*.md` pour les definitions executables par Claude Code.
- `AGENTS-claude.md` reste un document de gouvernance partage. Il ne remplace pas `AGENTS.md`.
- Si l'equipe souhaite plus tard un chargement automatique de cette gouvernance comme memoire projet Claude Code, elle pourra ajouter un `CLAUDE.md` qui reference ce fichier. Ce n'est pas fait ici pour respecter la demande initiale.

## Source de verite

La source de verite pour les sous-agents Claude Code est composee de :

- `guide_generique_refactoring_controller_javafx_spring.md`
- `AGENTS.md`
- `AGENTS-claude.md`
- `agents/contracts.md`
- `agents/orchestration.md`
- `agents/model-selection.md`
- `agents/catalog.md`
- `backend/pom.xml`
- `backend/`
- les contrats et schemas deja stabilises dans le depot

Les fichiers `/.codex` restent des references metier et operatoires existantes. Ils ne doivent pas etre modifies sauf demande explicite.

## Regles structurantes non negociables

- Backend strictement hexagonal : `domain`, `application`, `ports`, `adapters`, `configuration`.
- `domain` pur : aucune dependance Spring, JPA, PostgreSQL, DTO REST, parseurs, moteurs PDF/DOCX, JavaFX ou Angular.
- Les adapters encapsulent la technique ; aucune logique metier centrale dans REST, persistance, parseurs, generateurs documentaires, clients HTTP ou integration IA.
- Le frontend reste client du backend et de ses contrats valides ; aucune logique de refactoring experte ne doit etre dupliquee cote Angular.
- Toute sortie inter-agent doit distinguer `faits`, `interpretations`, `hypotheses` et `incertitudes`.
- Qualite Java obligatoire : pas de `continue`, un seul `return` par defaut, methodes <= 30 lignes sauf derogation justifiee, imbrication <= 3, maximum 4 parametres, pas de `null` retourne sans justification.
- Spring Boot `4.0.3` assemble les adapters et expose les interfaces sans contourner l'hexagone.
- PostgreSQL est au service du domaine.
- Angular `21.x` doit etre revalide avant bootstrap ou upgrade.
- Les lots frontend et backend doivent prevoir des logs debug activables par configuration, non sensibles et correles a un contexte fonctionnel.
- Aucune modification de `AGENTS.md` ni du contenu de `/.codex` sans demande explicite.

## Contrat de sortie inter-agent

Chaque sous-agent Claude doit suivre la structure definie dans `agents/contracts.md` :

1. `objectif`
2. `perimetre`
3. `faits`
4. `interpretations`
5. `hypotheses`
6. `incertitudes`
7. `decisions`
8. `livrables`
9. `dependances`
10. `verifications`
11. `handoff`

## Mapping de modeles Claude

- Utiliser `opus` pour les taches de cadrage, arbitrage, gouvernance, securite, synthese transverse, consolidation et restitution.
- Utiliser `sonnet` pour les taches de generation de code, wiring, tests, scripts, analyse structurelle et implementation pas a pas.
- Utiliser `haiku` seulement pour du triage leger ou des classifications simples ; aucun sous-agent principal du depot n'en depend par defaut.

Ce mapping remplace la logique historique `GPT-5.4` / `GPT-5.3-codex` sans changer les responsabilites fonctionnelles.

## Arborescence Claude Code

- `/.claude/agents/` contient les sous-agents Claude Code equivalents aux agents du produit, du moteur d'analyse et aux agents specialises historiques de `/.codex`.
- Les sous-agents `javafx-*` sont les equivalences Claude des skills specialises de `/.codex/skills/`.
- `jira-estimation` couvre a la fois l'agent produit et le skill historique du meme nom.

## Inventaire des sous-agents Claude

### Agents de pilotage et de construction

- `architecture-applicative`
- `product-owner-fonctionnel`
- `analyste-regles-metier`
- `api-contrats`
- `securite`
- `architecture-moteur-analyse`
- `audit-qualite-analyse`
- `jira-estimation`
- `gouvernance`
- `backend-hexagonal`
- `database-postgres`
- `frontend-angular`
- `implementation-moteur-analyse`
- `devops-ci-cd`
- `revue-code`
- `qa-backend`
- `qa-frontend`
- `test-automation`
- `observabilite-exploitation`
- `documentation-technique`

### Agents du moteur d'analyse

- `source-ingestion`
- `fxml-analysis`
- `controller-analysis`
- `inheritance-analysis`
- `spring-analysis`
- `pdf-analysis`
- `dynamic-ui-analysis`
- `consolidation`
- `restitution`

### Agents specialises de support

- `javafx-orchestrator`
- `javafx-screen-cartographer`
- `javafx-controller-flow-analyzer`
- `javafx-business-rules-extractor`
- `javafx-quality-smell-detector`
- `javafx-best-practices-researcher`
- `javafx-test-strategy-advisor`
- `javafx-refactoring-planner`
- `javafx-report-writer`

## Orchestration recommandee

### Phase 1 - Cadrage produit

- `architecture-applicative`
- `product-owner-fonctionnel`
- `analyste-regles-metier`
- `api-contrats`
- `securite`

### Phase 2 - Architecture du moteur

- `architecture-moteur-analyse`
- `audit-qualite-analyse`
- `gouvernance`

### Phase 3 - Implementation produit

- `backend-hexagonal`
- `database-postgres`
- `frontend-angular`
- `implementation-moteur-analyse`
- `devops-ci-cd`
- `observabilite-exploitation`
- `documentation-technique`

### Phase 4 - Industrialisation du moteur

- `source-ingestion`
- `fxml-analysis`
- `controller-analysis`
- `inheritance-analysis`
- `spring-analysis`
- `pdf-analysis`
- `dynamic-ui-analysis`
- `consolidation`
- `restitution`

### Phase 5 - Relecture et qualite

- `revue-code`
- `qa-backend`
- `qa-frontend`
- `test-automation`
- `gouvernance`

## Regles d'utilisation dans Claude Code

- Employer les sous-agents de `/.claude/agents/` comme delegues specialises.
- Demarrer par `architecture-applicative` pour tout nouveau lot structurant.
- Ne lancer les agents de production qu'apres stabilisation des besoins, des regles, des contrats et des contraintes de securite.
- Pour un controller JavaFX reel, privilegier `javafx-orchestrator` ou la chaine `source-ingestion` -> `fxml-analysis` -> `controller-analysis` -> `dynamic-ui-analysis` -> `consolidation` -> `restitution`.
- Toujours remonter les ambiguites vers `architecture-applicative` ou `gouvernance`.
- Revalider la version Angular cible avant tout bootstrap frontend.

## Politique de lecture et de modification

- Lire d'abord les artefacts de gouvernance avant toute conclusion.
- Modifier le code produit uniquement dans le perimetre explicitement demande.
- Ne pas editer `AGENTS.md` ni `/.codex` sans demande explicite.
- Quand un sous-agent Claude s'appuie sur un skill historique de `/.codex`, il le traite comme reference documentaire.

## Exemples d'invocation

- "Utilise le sous-agent `architecture-applicative` pour cadrer le prochain lot backend."
- "Utilise `backend-hexagonal` pour implementer ce cas d'usage en respectant le contrat API."
- "Utilise `javafx-orchestrator` pour analyser cet ecran JavaFX et preparer une restitution consolidee."
- "Utilise `revue-code` pour relire ce lot sous l'angle risques, regressions et tests manquants."

