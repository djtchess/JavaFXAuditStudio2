# Backlog Jira Initial - Refactoring App

## Contexte

Application cible :

- frontend Angular `21.2.x` de pilotage conversationnel et de restitution ;
- backend hexagonal JDK 21 / Spring Boot `4.0.3` ;
- moteur d'analyse/refactoring progressif de controllers JavaFX + Spring.

Source de verite :

- `guide_generique_refactoring_controller_javafx_spring.md`
- `AGENTS.md`
- `agents/orchestration.md`
- `agents/model-selection.md`

## Epic JAS-EPIC-01 - Cadrage produit et contrats

Lead agents :

- `architecture-applicative`
- `product-owner-fonctionnel`
- `api-contrats`
- `securite`

### Story JAS-01

- Titre : Formaliser le parcours utilisateur du cockpit de refactoring
- Description : Transformer le guide en parcours utilisateur actionnables pour l'application Angular et les cas d'usage backend.
- Story points : 5
- Dependances : aucune
- Risques : derive fonctionnelle si le vocabulaire du guide n'est pas stabilise
- Criteres d'acceptation :
  - un parcours "analyser -> diagnostiquer -> planifier -> generer" est documente ;
  - les actions conversationnelles prioritaires sont listees ;
  - les ecrans Angular cibles sont identifies.

### Story JAS-02

- Titre : Definir le contrat d'API initial du workbench
- Description : Stabiliser le premier contrat backend/frontend pour exposer l'etat du produit, les lots, les agents et les capacites du moteur.
- Story points : 3
- Dependances : JAS-01
- Risques : contrat trop faible pour le frontend ou trop technique pour le produit
- Criteres d'acceptation :
  - un endpoint de synthese est defini ;
  - les DTO initiaux sont nommes ;
  - les responsabilites backend/frontend sont explicites.

### Story JAS-03

- Titre : Poser les exigences de securite et d'exposition
- Description : Definir les garde-fous d'exposition REST, de journalisation et de gestion des fichiers source analyses.
- Story points : 3
- Dependances : JAS-02
- Risques : exposition de code source sensible, logs trop bavards
- Criteres d'acceptation :
  - les flux sensibles sont identifies ;
  - les principes de logs non sensibles sont fixes ;
  - les exigences de configuration sont listees.

## Epic JAS-EPIC-02 - Backend hexagonal socle

Lead agents :

- `backend-hexagonal`
- `database-postgres`
- `observabilite-exploitation`

### Story JAS-10

- Titre : Structurer le backend selon l'hexagone
- Description : Creer les couches `domain`, `application`, `ports`, `adapters`, `configuration` et un premier use case de workbench.
- Story points : 5
- Dependances : JAS-02
- Risques : derivation vers un backend CRUD sans cas d'usage
- Criteres d'acceptation :
  - les packages hexagonaux existent ;
  - le domaine ne depend pas de Spring ;
  - un endpoint REST appelle un use case assemble en configuration.

### Story JAS-11

- Titre : Exposer un endpoint de synthese du workbench
- Description : Fournir au frontend un endpoint REST renvoyant la vue d'ensemble du produit, des lots et des agents.
- Story points : 3
- Dependances : JAS-10
- Risques : contrat instable, mapping DTO fragile
- Criteres d'acceptation :
  - l'endpoint repond en JSON ;
  - le mapping domaine -> DTO est teste ;
  - le contrat est documente.

### Story JAS-12

- Titre : Preparer la persistance PostgreSQL pour les sessions d'analyse
- Description : Concevoir les premiers agregats persistables, les tables candidates et les ports de persistence sans coupler le domaine a PostgreSQL.
- Story points : 5
- Dependances : JAS-10
- Risques : schema premature, fuite d'infrastructure dans le domaine
- Criteres d'acceptation :
  - les ports de persistence cibles sont identifies ;
  - un schema conceptuel minimal existe ;
  - les objets du domaine restent purs.

## Epic JAS-EPIC-03 - Frontend Angular cockpit

Lead agents :

- `frontend-angular`
- `documentation-technique`

### Story JAS-20

- Titre : Initialiser le workspace Angular 21
- Description : Poser le squelette Angular, la configuration de build, les scripts et le proxy de developpement.
- Story points : 5
- Dependances : JAS-02
- Risques : incompatibilite de versions Node/TypeScript, configuration CLI incomplete
- Criteres d'acceptation :
  - le workspace Angular existe dans `frontend/` ;
  - les scripts `start`, `build` et `test` sont declares ;
  - le proxy `/api` vers le backend est configure.

### Story JAS-21

- Titre : Construire l'ecran cockpit initial
- Description : Afficher la synthese produit, les lots de migration et la repartition des agents/modele IA.
- Story points : 5
- Dependances : JAS-11, JAS-20
- Risques : UI purement decorative, absence de gestion d'erreur backend
- Criteres d'acceptation :
  - le frontend consomme l'endpoint du workbench ;
  - un etat loading et un etat erreur sont geres ;
  - les lots et agents sont visibles dans l'interface.

### Story JAS-22

- Titre : Documenter la compatibilite technique Angular
- Description : Capturer dans le repo les contraintes Node.js, TypeScript et RxJS liees a Angular 21.
- Story points : 2
- Dependances : JAS-20
- Risques : bootstrap non reproductible sur poste dev
- Criteres d'acceptation :
  - les versions minimales sont documentees ;
  - la date de verification est indiquee ;
  - les sources officielles sont referencees.

## Epic JAS-EPIC-04 - Ingestion et analyse structurelle

Lead agents :

- `source-ingestion`
- `fxml-analysis`
- `controller-analysis`
- `spring-analysis`

### Story JAS-30

- Titre : Concevoir le module d'ingestion de sources
- Description : Preparer la lecture des controllers Java, FXML et fichiers associes.
- Story points : 5
- Dependances : JAS-10
- Risques : pipeline d'entree trop couple au filesystem local
- Criteres d'acceptation :
  - les types d'entree sont listes ;
  - les erreurs d'ingestion sont classees ;
  - les sorties normalisees sont definies.

### Story JAS-31

- Titre : Cartographier les ecrans et controllers JavaFX
- Description : Produire les artefacts de structure, composants, handlers et dependances techniques.
- Story points : 8
- Dependances : JAS-30
- Risques : cartographie inexacte des liaisons FXML/controller
- Criteres d'acceptation :
  - les composants FXML sont extraits ;
  - les handlers et injections sont reconstitues ;
  - les inconnues sont signalees explicitement.

## Epic JAS-EPIC-05 - Strategie de refactoring et generation

Lead agents :

- `analyste-regles-metier`
- `architecture-moteur-analyse`
- `implementation-moteur-analyse`
- `javafx-refactoring-planner`

### Story JAS-40

- Titre : Extraire les regles de gestion et classifier les responsabilites
- Description : Identifier les regles stables et distinguer UI, presentation, application, metier et technique.
- Story points : 8
- Dependances : JAS-31
- Risques : confusion entre regle metier et detail UI
- Criteres d'acceptation :
  - les regles sont tracees a leurs sources ;
  - les zones incertaines sont marquees ;
  - les candidats `Policy` et `UseCase` sont identifies.

### Story JAS-41

- Titre : Produire le plan de migration en 5 lots
- Description : Generer la strategie progressive alignee sur le guide pour chaque controller analyse.
- Story points : 5
- Dependances : JAS-40
- Risques : plan trop theorique ou non compilable
- Criteres d'acceptation :
  - les lots 1 a 5 sont restitues ;
  - les risques de regression sont explicites ;
  - les extractions candidates sont priorisees.

### Story JAS-42

- Titre : Generer les artefacts de code cibles
- Description : Produire controllers amincis, ViewModels, UseCases, Policies, Gateways, Assemblers et Strategies.
- Story points : 13
- Dependances : JAS-41
- Risques : code genere non coherent avec l'hexagone ou le legacy
- Criteres d'acceptation :
  - les artefacts generes sont classes par lot ;
  - les bridges transitoires sont marques ;
  - le code cible est testable hors JavaFX quand applicable.

## Epic JAS-EPIC-06 - Restitution, QA et industrialisation

Lead agents :

- `restitution`
- `revue-code`
- `qa-backend`
- `qa-frontend`
- `test-automation`
- `gouvernance`

### Story JAS-50

- Titre : Produire la restitution finale lisible par dev et PO
- Description : Construire les rapports markdown et JSON consolides, avec priorisation et confiance.
- Story points : 5
- Dependances : JAS-31, JAS-40, JAS-41
- Risques : rapport redondant ou insuffisamment actionnable
- Criteres d'acceptation :
  - un rapport complet est genere ;
  - la synthese courte est produite ;
  - les contradictions et inconnues sont visibles.

### Story JAS-51

- Titre : Poser la strategie de tests transverses
- Description : Definir et implementer les premiers tests backend et frontend qui securisent la fondation.
- Story points : 5
- Dependances : JAS-11, JAS-21
- Risques : couverture superficielle, outils non compatibles
- Criteres d'acceptation :
  - des tests backend du workbench existent ;
  - la strategie frontend est documentee ;
  - les lacunes de testabilite sont listees.

### Story JAS-52

- Titre : Ajouter observabilite et logs de workflow
- Description : Ajouter les conventions de logs activables, correlation fonctionnelle et points de diagnostic.
- Story points : 3
- Dependances : JAS-10, JAS-21
- Risques : logs sensibles ou trop verbeux
- Criteres d'acceptation :
  - les points de logs critiques sont identifies ;
  - le niveau DEBUG est activable par configuration ;
  - aucune donnee sensible n'est journalisee par defaut.

## Dependances globales

- JAS-01 -> JAS-02 -> JAS-10 -> JAS-11 -> JAS-21
- JAS-10 -> JAS-30 -> JAS-31 -> JAS-40 -> JAS-41 -> JAS-42
- JAS-20 -> JAS-21
- JAS-11 et JAS-21 alimentent JAS-51

## Risques transverses

- Le moteur peut deriver vers un simple parseur sans explicabilite.
- Le frontend peut dupliquer de la logique experte si le contrat n'est pas stable.
- Le backend peut perdre l'hexagone si les adapters REST et de persistence prennent la main sur le metier.
- Les versions Angular peuvent evoluer rapidement ; la cible doit etre reverifiee avant bootstrap ou upgrade.

## Estimation macro

- Epic 01 : 11 SP
- Epic 02 : 13 SP
- Epic 03 : 12 SP
- Epic 04 : 13 SP
- Epic 05 : 26 SP
- Epic 06 : 13 SP

Total initial : 88 SP
