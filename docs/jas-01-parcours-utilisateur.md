# JAS-01 — Parcours utilisateur du cockpit de refactoring

## Contrat inter-agent (structure contracts.md)

### objectif

Formaliser le parcours utilisateur complet du cockpit Angular de pilotage du refactoring progressif
de controllers JavaFX + Spring, en derivant du guide `guide_generique_refactoring_controller_javafx_spring.md`
les etapes fonctionnelles, les actions conversationnelles prioritaires, les ecrans Angular cibles
et les cas d'usage backend associes.

### perimetre

- Parcours utilisateur de bout en bout : analyser -> diagnostiquer -> planifier -> generer.
- Actions conversationnelles que l'utilisateur peut declencher depuis le cockpit.
- Ecrans Angular 21 a implementer, ancres sur les contrats REST deja stabilises.
- Cas d'usage backend hexagonaux a creer ou a etendre.
- Hors perimetre : implementation technique des ecrans, definition des nouveaux endpoints REST,
  logique du moteur d'analyse (relevent de `api-contrats` et `backend-hexagonal`).

### faits

- Le backend expose `GET /api/v1/workbench/overview` retournant `WorkbenchOverview`
  (productName, summary, frontendTarget, backendTarget, lots : List<RefactoringLot>, agents : List<AgentOverview>).
- `RefactoringLot` contient : number, title, objective, primaryOutcome.
- `AgentOverview` contient : id, label, responsibility, preferredModel.
- Le frontend Angular possede un seul composant route : `DashboardComponent` sur le chemin `""`,
  consommant `WorkbenchApiService.loadOverview()`.
- `DashboardComponent` expose un `DashboardViewModel` avec isLoading, overview, errorMessage.
- Aucun ecran de soumission de controller, de conversation ou de visualisation de diagnostic n'existe encore.
- Le guide decrit 5 lots de migration et 7 modules fonctionnels : ingestion, analyse structurelle,
  classification des responsabilites, strategie, generation de code, explication/raisonnement, conversation GPT-like.
- L'architecture backend est hexagonale stricte : domain pur, application/ports, adapters, configuration.
- Angular 21.x est la cible frontend ; aucune logique experte de refactoring ne doit etre cote Angular.

### interpretations

- Le parcours "analyser -> diagnostiquer -> planifier -> generer" correspond directement aux lots 1 a 5
  du guide, regroupes en 4 etapes fonctionnelles coherentes pour l'utilisateur.
- L'interaction conversationnelle decrite dans le guide (section 11) implique un ecran dedie
  distinct du dashboard, capable d'envoyer des messages et d'afficher des reponses structurees.
- Le dashboard actuel est le point d'entree naturel pour l'etape "analyser" : il affiche
  l'apercu du workbench et oriente vers la soumission d'un controller.
- Les lots du backend (`RefactoringLot`) sont deja le modele de donnees central pour l'etape "planifier".
- La restitution du diagnostic et du plan de migration necessite des ecrans distincts orientant
  vers des endpoints backend a creer.

### hypotheses

- L'utilisateur est un developpeur Java qui connait le controller qu'il souhaite refactorer.
- La soumission du controller source se fait par upload de fichier(s) ou par saisie de texte.
- Le backend analysera le controller de facon asynchrone et retournera un identifiant de session/analyse.
- L'interaction conversationnelle est pilotee par un endpoint REST de type dialogue (requete/reponse),
  non par WebSocket dans un premier temps.
- Un controller soumis est associe a une "session d'analyse" persistee cote backend.

### incertitudes

- Le format exact de soumission du controller (upload multipart, JSON avec contenu base64, ou saisie texte)
  n'est pas encore arrete : a arbitrer par `api-contrats`.
- La gestion de l'asynchronisme (polling, webhook, Server-Sent Events) n'est pas definie :
  a arbitrer par `architecture-applicative`.
- Le perimetre exact des artefacts telechargeables (code genere, markdown, ZIP) n'est pas specifie.
- La persistance des sessions d'analyse (PostgreSQL) et leur cycle de vie sont a cadrer par `database-postgres`.

### decisions

- Le parcours utilisateur est formalise en 4 etapes lineaires avec retour possible a l'etape precedente.
- Chaque etape correspond a un ecran Angular distinct avec sa propre route.
- Le frontend reste client passif du backend : il n'embarque aucune logique d'analyse ou de classification.
- Les actions conversationnelles sont representees comme des commandes textuelles predefinies
  ou en saisie libre, envoyees au backend via un endpoint de dialogue.
- Le dashboard existant est enrichi (etape Analyser) sans etre remplace.

### livrables

- Le present document `docs/jas-01-parcours-utilisateur.md`.

### dependances

- `GET /api/v1/workbench/overview` (disponible).
- Endpoints a creer : soumission controller, lancement analyse, dialogue conversationnel,
  telechargement artefacts (a specifier par `api-contrats`).
- Persistance session d'analyse (a cadrer par `database-postgres`).
- Moteur d'analyse (a implementer par `implementation-moteur-analyse`).

### verifications

- Le parcours couvre les 4 etapes demandees : analyser, diagnostiquer, planifier, generer.
- Chaque etape liste des actions conversationnelles.
- Chaque ecran Angular est nomme, route et role fonctionnel precise.
- Chaque ecran est associe a un ou plusieurs cas d'usage backend.
- Aucune logique experte n'est attribuee au frontend.

### handoff

```text
handoff:
  vers: jira-estimation
  preconditions:
    - parcours utilisateur valide par product-owner-fonctionnel
    - ecrans cibles identifies et roles clarifies
  points_de_vigilance:
    - les endpoints backend associes aux etapes 2, 3 et 4 ne sont pas encore definis
    - l'asynchronisme de l'analyse est une incertitude structurante pour l'estimation
    - la story JAS-01 ne couvre que le parcours, pas l'implementation
  artefacts_a_consulter:
    - docs/jas-01-parcours-utilisateur.md
    - guide_generique_refactoring_controller_javafx_spring.md
    - agents/contracts.md

  vers: api-contrats
  preconditions:
    - parcours utilisateur stabilise
    - cas d'usage backend identifies par etape
  points_de_vigilance:
    - format de soumission du controller source a decider
    - mode d'asynchronisme a trancher avant contrat d'API
    - les artefacts generables (code, markdown, ZIP) ont besoin de contrats de telechargement
  artefacts_a_consulter:
    - docs/jas-01-parcours-utilisateur.md
    - backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/in/rest/WorkbenchController.java

  vers: frontend-angular
  preconditions:
    - contrats API stabilises par api-contrats
    - parcours utilisateur valide
  points_de_vigilance:
    - quatre nouvelles routes Angular a creer
    - WorkbenchApiService a etendre avec les nouveaux endpoints
    - aucune logique metier de refactoring dans les composants
    - gestion des etats asynchrones (loading, error) a prevoir sur chaque ecran
  artefacts_a_consulter:
    - docs/jas-01-parcours-utilisateur.md
    - frontend/src/app/app.routes.ts
    - frontend/src/app/core/services/workbench-api.service.ts

  vers: gouvernance
  preconditions:
    - parcours utilisateur publie
  points_de_vigilance:
    - verifier la coherence du parcours avec la roadmap en 5 lots du guide
    - valider que les ecrans identifies ne creent pas de logique experte cote Angular
  artefacts_a_consulter:
    - docs/jas-01-parcours-utilisateur.md
    - agents/orchestration.md
```

---

## Parcours utilisateur — cockpit de refactoring progressif JavaFX + Spring

### Vue d'ensemble

Le cockpit propose un parcours lineaire en quatre etapes, refletant la progression
du guide de refactoring :

```
[Analyser] --> [Diagnostiquer] --> [Planifier] --> [Generer]
```

L'utilisateur peut revenir a l'etape precedente a tout moment pour affiner sa soumission
ou modifier ses choix. Chaque etape est associee a un ecran Angular distinct.

---

## Etape 1 — Analyser

### Objectif fonctionnel

L'utilisateur soumet un controller JavaFX + Spring source pour initier une session d'analyse.
Le cockpit affiche l'apercu du workbench (lots disponibles, agents actifs) et permet
de charger le controller a analyser.

### Actions utilisateur

- Consulter l'apercu du workbench (lots de refactoring, agents disponibles).
- Soumettre un controller Java source (fichier ou saisie directe).
- Associer optionnellement des fichiers lies : FXML, services, DTO.
- Nommer la session d'analyse pour la retrouver ultérieurement.
- Declencher l'analyse.

### Actions conversationnelles prioritaires

- "Analyse ce controller"
- "Quels lots sont disponibles pour ce type de controller ?"
- "Quels agents vont intervenir sur cette analyse ?"

### Ecran Angular cible

**Nom du composant** : `AnalyzeComponent`
**Route** : `/analyze`
**Role fonctionnel** : Point d'entree de la session. Affiche l'apercu du workbench recupere
via `GET /api/v1/workbench/overview`. Permet la soumission du controller source et le
demarrage de l'analyse. Redirige vers l'ecran Diagnostiquer a la reception de l'identifiant
de session.

**Donnees affichees** :
- Liste des lots de refactoring (depuis `WorkbenchOverview.lots`).
- Liste des agents disponibles (depuis `WorkbenchOverview.agents`).
- Formulaire de soumission du controller source.

**Etat UI minimal** :
- isSubmitting : boolean
- errorMessage : string | null
- analysisSessionId : string | null (recue du backend au succes)

### Cas d'usage backend associes

- `GetWorkbenchOverviewUseCase` (existant) — charge l'apercu du workbench.
- `SubmitControllerAnalysisUseCase` (a creer) — recoit le controller source,
  cree une session d'analyse, declenche l'ingestion et retourne un identifiant de session.

---

## Etape 2 — Diagnostiquer

### Objectif fonctionnel

Le cockpit restitue le diagnostic produit par le moteur d'analyse pour le controller soumis.
L'utilisateur peut lire le diagnostic, poser des questions conversationnelles et comprendre
les responsabilites identifiees dans le controller.

### Actions utilisateur

- Consulter le diagnostic synthetique du controller (taille, handlers, services injectes,
  complexite, dette identifiee).
- Consulter la cartographie des responsabilites (ce qui reste dans le controller,
  ce qui doit en sortir).
- Interagir en langage naturel pour approfondir le diagnostic.
- Valider le diagnostic pour passer a l'etape de planification.

### Actions conversationnelles prioritaires

- "Explique-moi le diagnostic de ce controller"
- "Quelles sont les zones les plus chargees ?"
- "Pourquoi cette methode doit-elle sortir du controller ?"
- "Quel est le niveau de dette metier detecte ?"
- "Donne le raisonnement detaille en markdown"

### Ecran Angular cible

**Nom du composant** : `DiagnosticComponent`
**Route** : `/analyze/:sessionId/diagnostic`
**Role fonctionnel** : Affiche le diagnostic produit par le moteur pour la session courante.
Presente la cartographie des responsabilites sous forme structuree. Integre un panneau
conversationnel permettant d'envoyer une question et d'afficher la reponse du backend.

**Donnees affichees** :
- Diagnostic synthetique : metriques du controller (nombre de handlers, services injectes,
  methodes privees, complexite).
- Tableau de responsabilites : zone identifiee, classification (UI / etat / orchestration /
  metier / technique), recommandation.
- Historique de la conversation de la session.
- Zone de saisie du message conversationnel.

**Etat UI minimal** :
- isLoadingDiagnostic : boolean
- diagnostic : DiagnosticResult | null
- conversationHistory : ConversationTurn[]
- isWaitingResponse : boolean
- errorMessage : string | null

### Cas d'usage backend associes

- `GetAnalysisDiagnosticUseCase` (a creer) — retourne le diagnostic produit pour une session.
- `SendConversationMessageUseCase` (a creer) — recoit un message utilisateur dans le contexte
  d'une session, sollicite le module conversationnel du moteur, retourne la reponse structuree.

---

## Etape 3 — Planifier

### Objectif fonctionnel

Le cockpit presente le plan de migration en lots genere par le moteur pour le controller analyse.
L'utilisateur peut choisir les lots a executer, ajuster les priorites et valider le plan
avant de lancer la generation.

### Actions utilisateur

- Consulter le plan de migration en lots (lots 1 a 5 selon le guide).
- Lire le detail de chaque lot : objectif, artefacts a produire, dependances.
- Choisir de lancer un lot specifique ou la sequence complete.
- Poser des questions conversationnelles sur le plan propose.
- Valider le plan et passer a l'etape de generation.

### Actions conversationnelles prioritaires

- "Fais le lot 1"
- "Que produit le lot 2 ?"
- "Quels sont les risques du lot 3 ?"
- "Peut-on sauter le lot 4 ?"
- "Produis un plan de migration compilable"
- "Ne touche pas aux services legacy"

### Ecran Angular cible

**Nom du composant** : `MigrationPlanComponent`
**Route** : `/analyze/:sessionId/plan`
**Role fonctionnel** : Affiche le plan de migration en lots genere pour la session courante.
Permet la selection d'un ou plusieurs lots a executer. Expose un panneau conversationnel
partage avec l'etape Diagnostiquer. Les lots affiches sont distincts des `RefactoringLot`
du workbench overview : il s'agit ici des lots specifiques au controller analyse.

**Donnees affichees** :
- Liste des lots de migration specifiques au controller analyse.
  Pour chaque lot : numero, titre, objectif, artefacts attendus, classes a creer ou modifier,
  risques identifies.
- Selection des lots a executer (case a cocher ou selecteur de lot cible).
- Panneau conversationnel (meme service que l'etape Diagnostiquer, session identique).

**Etat UI minimal** :
- isLoadingPlan : boolean
- migrationPlan : MigrationPlan | null
- selectedLots : number[]
- isWaitingResponse : boolean
- errorMessage : string | null

### Cas d'usage backend associes

- `GetMigrationPlanUseCase` (a creer) — retourne le plan de migration en lots pour une session.
- `SendConversationMessageUseCase` (a creer, partage avec Diagnostiquer) — conversation contextuelle.
- `SelectMigrationLotsUseCase` (a creer) — enregistre les lots selectionnes par l'utilisateur
  pour la phase de generation.

---

## Etape 4 — Generer

### Objectif fonctionnel

Le cockpit lance la generation des artefacts pour les lots selectionnes et restitue
le code genere, les documents de raisonnement et les fichiers telechargeable.
L'utilisateur peut affiner par conversation et telecharger les artefacts produits.

### Actions utilisateur

- Lancer la generation pour les lots selectionnes.
- Consulter le code genere : controller aminci, ViewModel, use cases, policies,
  gateways, assemblers, strategies.
- Lire le document de raisonnement detaille.
- Affiner la generation par interaction conversationnelle.
- Telecharger les artefacts produits (fichiers Java, markdown).
- Revenir au plan pour modifier la selection de lots.

### Actions conversationnelles prioritaires

- "Genere le ViewModel"
- "Reecris seulement le controller"
- "Genere les strategies"
- "Branche les adaptateurs Spring"
- "Fais la migration des flux d'enregistrement et d'impression"
- "Donne le raisonnement detaille en markdown"
- "Produis un plan de migration compilable"

### Ecran Angular cible

**Nom du composant** : `GenerationComponent`
**Route** : `/analyze/:sessionId/generate`
**Role fonctionnel** : Affiche les artefacts generes par le moteur pour les lots selectionnes.
Presente le code genere dans un visualiseur de fichiers. Expose le document de raisonnement.
Permet le telechargement des artefacts. Integre le panneau conversationnel pour affiner
la generation de maniere iterative.

**Donnees affichees** :
- Statut de generation (en cours / termine / erreur).
- Liste des artefacts generes avec leur type (code Java, markdown, documentation).
- Visualiseur de contenu pour chaque artefact (affichage en lecture seule).
- Document de raisonnement detaille.
- Panneau conversationnel (meme service, session identique).
- Bouton de telechargement par artefact ou en archive.

**Etat UI minimal** :
- isGenerating : boolean
- generationStatus : 'idle' | 'running' | 'done' | 'error'
- artifacts : GeneratedArtifact[]
- selectedArtifact : GeneratedArtifact | null
- isWaitingResponse : boolean
- errorMessage : string | null

### Cas d'usage backend associes

- `LaunchGenerationUseCase` (a creer) — lance la generation des artefacts pour les lots
  selectionnes d'une session.
- `GetGenerationStatusUseCase` (a creer) — retourne le statut de generation et la liste
  des artefacts disponibles.
- `GetArtifactContentUseCase` (a creer) — retourne le contenu d'un artefact specifique.
- `DownloadArtifactsUseCase` (a creer) — produit une archive telechargeable des artefacts.
- `SendConversationMessageUseCase` (partage) — affinage conversationnel de la generation.

---

## Synthese des ecrans Angular cibles

| Composant              | Route                              | Etape        | Contrat backend principal              |
|------------------------|------------------------------------|--------------|----------------------------------------|
| DashboardComponent     | `/`                                | (accueil)    | GET /api/v1/workbench/overview         |
| AnalyzeComponent       | `/analyze`                         | Analyser     | GET /api/v1/workbench/overview         |
|                        |                                    |              | POST /api/v1/analysis/submit (a creer) |
| DiagnosticComponent    | `/analyze/:sessionId/diagnostic`   | Diagnostiquer| GET /api/v1/analysis/:id/diagnostic    |
|                        |                                    |              | POST /api/v1/analysis/:id/conversation |
| MigrationPlanComponent | `/analyze/:sessionId/plan`         | Planifier    | GET /api/v1/analysis/:id/plan          |
|                        |                                    |              | POST /api/v1/analysis/:id/lots/select  |
|                        |                                    |              | POST /api/v1/analysis/:id/conversation |
| GenerationComponent    | `/analyze/:sessionId/generate`     | Generer      | POST /api/v1/analysis/:id/generate     |
|                        |                                    |              | GET /api/v1/analysis/:id/artifacts     |
|                        |                                    |              | GET /api/v1/analysis/:id/artifacts/:aid|
|                        |                                    |              | POST /api/v1/analysis/:id/conversation |

Note : les routes backend prefixees `/api/v1/analysis/` sont des proposals fonctionnels
soumis a validation par `api-contrats`. Seul `GET /api/v1/workbench/overview` est stabilise.

---

## Synthese des cas d'usage backend par etape

| Etape        | Use case                          | Statut    | Port entrant                        |
|--------------|-----------------------------------|-----------|-------------------------------------|
| Analyser     | GetWorkbenchOverviewUseCase       | Existant  | GetWorkbenchOverviewUseCase         |
| Analyser     | SubmitControllerAnalysisUseCase   | A creer   | SubmitControllerAnalysisUseCase     |
| Diagnostiquer| GetAnalysisDiagnosticUseCase      | A creer   | GetAnalysisDiagnosticUseCase        |
| Diagnostiquer| SendConversationMessageUseCase    | A creer   | SendConversationMessageUseCase      |
| Planifier    | GetMigrationPlanUseCase           | A creer   | GetMigrationPlanUseCase             |
| Planifier    | SelectMigrationLotsUseCase        | A creer   | SelectMigrationLotsUseCase          |
| Generer      | LaunchGenerationUseCase           | A creer   | LaunchGenerationUseCase             |
| Generer      | GetGenerationStatusUseCase        | A creer   | GetGenerationStatusUseCase          |
| Generer      | GetArtifactContentUseCase         | A creer   | GetArtifactContentUseCase           |
| Generer      | DownloadArtifactsUseCase          | A creer   | DownloadArtifactsUseCase            |

---

## Criteres d'acceptation de la Story JAS-01

1. Le parcours "analyser -> diagnostiquer -> planifier -> generer" est documente avec
   un objectif fonctionnel, des actions utilisateur et des actions conversationnelles
   pour chacune des 4 etapes.

2. Les actions conversationnelles prioritaires sont listees par etape, derivees
   du vocabulaire stabilise du guide (section 11 : "analyse ce controller",
   "fais le lot N", "genere le ViewModel", etc.).

3. Les ecrans Angular cibles sont identifies avec :
   - leur nom de composant Angular,
   - leur route,
   - leur role fonctionnel,
   - les donnees affichees,
   - l'etat UI minimal necessaire.

4. Chaque ecran est associe a un ou plusieurs cas d'usage backend, en distinguant
   les cas d'usage existants des cas d'usage a creer.

5. Aucun ecran Angular ne porte de logique experte de refactoring : le frontend
   reste client du backend.

6. Le document est structure conformement au contrat inter-agent `agents/contracts.md`.

---

## Correspondance avec les 5 lots du guide

| Lot du guide | Titre                                            | Etape cockpit  |
|--------------|--------------------------------------------------|----------------|
| Lot 1        | Diagnostic et cible architecturale               | Diagnostiquer  |
| Lot 2        | Introduction du ViewModel, use cases, policy     | Planifier      |
| Lot 3        | Migration des flux les plus charges              | Planifier      |
| Lot 4        | Adaptateurs Spring branches sur l'existant       | Planifier      |
| Lot 5        | Assembleur de formulaire et strategies           | Planifier      |
| Tous lots    | Generation des artefacts de code                 | Generer        |

L'etape "Analyser" correspond a la phase d'ingestion (section 5.1 du guide) :
soumission des sources, normalisation, preparation pour l'analyse.

L'etape "Diagnostiquer" correspond aux etapes 7.1 a 7.7 du guide :
observation initiale, cartographie des responsabilites, identification des intentions
utilisateur, decisions stables, fabrication d'objets, variantes de workflow, existant a preserver.
