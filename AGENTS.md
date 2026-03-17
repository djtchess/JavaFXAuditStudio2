# Configuration Multi-Agent

## Source de verite

La source de verite pour cette application est desormais composee de :

- `guide_generique_refactoring_controller_javafx_spring.md` pour la vision produit, les modules de la feature GPT-like, les lots de migration et l'architecture cible `Controller + ViewModel + UseCases + Policies + Gateways + Assemblers + Strategies`.
- `AGENTS.md` pour les regles de collaboration et de gouvernance.
- `backend/pom.xml` pour la stack backend effective deja initialisee : JDK 21 et Spring Boot `4.0.3`.
- le dossier `backend/` comme point d'ancrage technique reel du produit.
- les sources officielles Angular a revalider avant bootstrap frontend. A la date du 16 mars 2026, la cible retenue est Angular `21.x`, dernier majeur stable publie.

Cette configuration couvre deux familles d'agents :

1. Les agents de pilotage et de construction du produit.
2. Les agents specialises du moteur d'analyse statique et de refactoring.

## Regles structurantes non negociables

- Backend strictement hexagonal : `domain`, `application`, `ports`, `adapters`, `configuration`.
- `domain` pur : aucune dependance Spring, JPA, PostgreSQL, DTO REST, parseurs, moteurs PDF/DOCX, JavaFX ou Angular.
- Les adapters encapsulent la technique ; aucune logique metier centrale dans REST, persistance, parseurs, generateurs documentaires, clients HTTP ou integration IA.
- Les agents de code consomment les contrats valides ; ils ne redefinissent ni l'architecture ni les contrats.
- Toute sortie inter-agent doit etre structuree, tracable et indiquer faits, interpretations, hypotheses et incertitudes.
- Qualite Java obligatoire : pas de `continue`, un seul `return` par defaut, methodes <= 30 lignes sauf derogation justifiee, imbrication <= 3, maximum 4 parametres, pas de `null` retourne sans justification.
- Spring Boot `4.0.3` sert a assembler et exposer les adapters ; il ne contourne jamais l'hexagone.
- PostgreSQL est au service du domaine ; Angular `21.x` consomme les contrats valides sans inventer l'API.
- Le frontend doit rester client du backend et de ses contrats valides ; aucune logique de refactoring experte ne doit etre dupliquee cote Angular.
- Les lots frontend et backend doivent prevoir des logs debug activables par configuration, non sensibles, correles a un contexte fonctionnel et suffisamment explicites pour diagnostiquer les workflows critiques sans polluer la production.
- Les skills et prompts dans `/.codex` sont des agents specialises reutilisables ; ils nourrissent les agents produit definis dans `agents/catalog.md`.
- Le choix entre `GPT-5.4` et `GPT-5.3-codex` est centralise dans `agents/model-selection.md`.
- La transparence des donnees susceptibles d'etre envoyees a OpenAI doit pouvoir etre documentee par `transparence-openai`, sans jamais exposer une chaine de pensee interne brute.

## Inventaire des agents

### Agents explicitement demandes pour construire le projet

- `architecture-applicative`
- `product-owner-fonctionnel`
- `analyste-regles-metier`
- `api-contrats`
- `securite`
- `transparence-openai`
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

### Agents explicitement demandes pour industrialiser le moteur d'analyse

- `source-ingestion`
- `fxml-analysis`
- `controller-analysis`
- `inheritance-analysis`
- `spring-analysis`
- `pdf-analysis`
- `dynamic-ui-analysis`
- `consolidation`
- `restitution`

## Hypotheses documentees

- La mention historique Angular 19 est remplacee par Angular `21.x`, dernier majeur stable connu au 16 mars 2026. Toute initialisation frontend doit toutefois revalider la version stable au moment du bootstrap.
- Le backend JDK 21 / Spring Boot `4.0.3` est deja initialise dans `backend/` et sert de base obligatoire pour la suite.
- Le dossier `frontend/` est reserve a l'application Angular de pilotage conversationnel et de visualisation des diagnostics.
- Le guide `guide_generique_refactoring_controller_javafx_spring.md` devient la specification fonctionnelle, architecturale et methodologique principale de la nouvelle application.
- Les agents `devops-ci-cd`, `revue-code`, `qa-backend`, `qa-frontend`, `observabilite-exploitation` et `documentation-technique` doivent maintenant couvrir a la fois le produit Angular/Spring Boot et le moteur specialise d'analyse/refactoring.
- Les agents analytiques de la section 16 sont conserves comme sous-systeme specialise du produit cible ; ils ne remplacent pas les agents de pilotage et de construction de la section 18.
- L'agent `audit-qualite-analyse` reste un garde-fou transverse du moteur pour reevaluer regulierement la fiabilite des regles extraites sans se substituer a `gouvernance`.
- L'agent `transparence-openai` produit une trace observable et auditable des donnees, fichiers et prompts exposes a OpenAI ; il ne revendique jamais un acces a une chaine de pensee interne brute.

## Artefacts

- Contrat standard : [agents/contracts.md](agents/contracts.md)
- Orchestration : [agents/orchestration.md](agents/orchestration.md)
- Selection de modeles : [agents/model-selection.md](agents/model-selection.md)
- Catalogue des agents : [agents/catalog.md](agents/catalog.md)
- Specification transparence OpenAI : [agents/transparence-openai.md](agents/transparence-openai.md)
- Skills et prompts specialises : dossier `/.codex`

## Regle d'utilisation

1. Commencer par `architecture-applicative`.
2. Stabiliser besoins, regles metier, contrats, securite et transparence OpenAI avant toute production de code.
3. Cadrer ensuite l'architecture du moteur d'analyse a partir du guide et des exemples JavaFX.
4. Lancer les agents de production en parallele uniquement sur base de contrats valides.
5. Faire relire chaque lot par `revue-code`, puis par les agents QA/Gouvernance prevus.
6. Remonter toute ambiguite ou conflit vers `architecture-applicative` ou `gouvernance`.
7. Revalider la version Angular cible avant tout bootstrap ou upgrade du frontend.
