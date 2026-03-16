# JAS-02 — Définir le contrat d'API initial du workbench

Epic : JAS-EPIC-01
Statut : CADRAGE
Date : 2026-03-16
Agent : api-contrats
Dépend de : JAS-01

---

## objectif

Définir le contrat d'API REST initial du workbench en couvrant les quatre phases fonctionnelles identifiées en JAS-01 (analyser, diagnostiquer, planifier, générer). Nommer les DTO initiaux, fixer les responsabilités backend/frontend et préparer le terrain pour l'implémentation backend-hexagonal (Epic 02).

---

## perimetre

- Endpoints REST couvrant les phases analyser, diagnostiquer, planifier, générer
- DTO d'entrée et de sortie nommés et structurés (sans implémentation Java à ce stade)
- Responsabilités backend vs frontend explicites pour chaque échange
- Alignement sur l'architecture hexagonale : adapters.in.rest / ports.in / application / domain
- Hors périmètre : implémentation des use cases, intégration du moteur d'analyse, persistance PostgreSQL, WebSocket, authentification

---

## faits

- L'endpoint existant est `GET /api/v1/workbench/overview`, exposé par `adapters.in.rest`, mappé depuis le domaine `WorkbenchOverview` via `GetWorkbenchOverviewUseCase`.
- Les objets domaine actuels : `WorkbenchOverview`, `RefactoringLot`, `AgentOverview`.
- Le package `adapters.in.rest.dto` existe et contient déjà les DTO de `overview`.
- Le package `adapters.in.rest.mapper` existe pour le mapping domaine → DTO.
- Spring Boot 4.0.3 — les contrôleurs REST s'appuient sur `@RestController`, `@RequestMapping`.
- JDK 21 — les records Java sont disponibles et recommandés pour les DTO immuables.
- JAS-01 identifie cinq routes Angular et quatre phases fonctionnelles.
- Le guide définit les intentions conversationnelles comme actions utilisateur → use cases backend.

---

## interpretations

- Chaque phase du parcours JAS-01 correspond à un ou plusieurs endpoints REST. La granularité "une session" est le conteneur logique des échanges d'une analyse.
- Une "session" est une unité de travail liée à un controller source soumis. Elle porte le contexte (source ingérée, diagnostic, plan, générations). Elle doit avoir un identifiant stable retourné au frontend.
- Les actions conversationnelles (prompts) sont des requêtes POST vers un endpoint de commande. Le backend interprète l'intention et délègue au bon use case.
- Le frontend Angular est consommateur pur : il ne calcule pas de logique métier, il envoie des commandes et affiche les résultats.
- Pour l'Epic 01, le moteur d'analyse est un stub. Les réponses peuvent être simulées côté `adapters.out.catalog` existant.

---

## hypotheses

- Le source Java du controller est transmis comme texte brut (`String`) dans le corps JSON de la requête de soumission (pas d'upload multipart pour l'Epic 01).
- L'identifiant de session est un UUID généré par le backend à la création de session.
- Les réponses conversationnelles sont synchrones pour l'Epic 01 (pas de streaming SSE ni WebSocket).
- Les DTO utilisent des records Java 21 côté backend.
- Le versioning de l'API reste `/api/v1/` pour tout l'Epic 01.

---

## incertitudes

- La gestion de l'état de session (stockage en mémoire vs PostgreSQL) n'est pas tranchée. Pour l'Epic 01 : stockage en mémoire acceptable (Map<UUID, Session> dans l'adapter out). À formaliser en Epic 02.
- La taille maximale du source soumis (contrôleur avec 1000+ lignes) : limiter à 500 Ko pour l'Epic 01, à réviser.
- Le format de réponse pour la génération de code : liste de fichiers avec contenu ou archive ZIP ? Décision : liste JSON de fichiers pour l'Epic 01.
- La gestion des erreurs de parsing du source Java (source invalide) : codes HTTP et structure d'erreur à uniformiser.

---

## decisions

- La base de l'URL est `/api/v1/workbench/`.
- Une session est créée via `POST /api/v1/workbench/sessions` et identifiée par un UUID.
- Les phases (diagnostic, plan, génération) sont des sous-ressources de la session.
- Le prompt conversationnel est un endpoint dédié `POST /api/v1/workbench/sessions/{id}/chat`.
- Tous les DTO d'entrée/sortie sont des records Java 21 immuables.
- Les mappers restent dans `adapters.in.rest.mapper`. Les DTO restent dans `adapters.in.rest.dto`.
- Le domaine ne connaît aucun DTO REST.
- Les nouvelles interfaces de port entrant sont ajoutées dans `application.ports.in`.

---

## livrables

### Endpoints définis

#### Workbench Overview (existant, confirmé)

```
GET /api/v1/workbench/overview
  → WorkbenchOverviewResponse
  Responsabilité backend : GetWorkbenchOverviewUseCase → WorkbenchCatalogPort
  Responsabilité frontend : alimenter l'écran /workbench
```

#### Gestion des sessions (nouveaux)

```
POST /api/v1/workbench/sessions
  Corps : SubmitSourceRequest
  → SessionCreatedResponse
  Responsabilité backend : CreateAnalysisSessionUseCase (nouveau port in)
  Responsabilité frontend : envoyer le source, stocker le sessionId retourné

GET /api/v1/workbench/sessions/{sessionId}
  → SessionSummaryResponse
  Responsabilité backend : GetSessionUseCase
  Responsabilité frontend : afficher l'état courant de la session (écran WorkbenchHeader)
```

#### Phase Analyser

```
POST /api/v1/workbench/sessions/{sessionId}/analyze
  Corps : AnalyzeRequest (vide ou options d'analyse)
  → AnalysisResultResponse
  Responsabilité backend : AnalyzeControllerUseCase → StructuralAnalysisPort (moteur)
  Responsabilité frontend : afficher les métriques structurelles initiales sur /session/new
```

#### Phase Diagnostiquer

```
GET /api/v1/workbench/sessions/{sessionId}/diagnostic
  → DiagnosticResponse
  Responsabilité backend : GetDiagnosticUseCase → récupère le diagnostic produit par l'analyse
  Responsabilité frontend : alimenter l'écran /session/:id/diagnostic (carte des responsabilités)
```

#### Phase Planifier

```
GET /api/v1/workbench/sessions/{sessionId}/plan
  → MigrationPlanResponse
  Responsabilité backend : GetMigrationPlanUseCase → génère les lots de migration depuis le diagnostic
  Responsabilité frontend : alimenter l'écran /session/:id/plan (sélecteur de lots, architecture cible)

POST /api/v1/workbench/sessions/{sessionId}/plan/select
  Corps : SelectLotsRequest
  → MigrationPlanResponse (mis à jour)
  Responsabilité backend : UpdateSelectedLotsUseCase
  Responsabilité frontend : mettre à jour l'affichage du plan selon les lots sélectionnés
```

#### Phase Générer

```
POST /api/v1/workbench/sessions/{sessionId}/generate
  Corps : GenerateRequest
  → GenerationResultResponse
  Responsabilité backend : GenerateCodeUseCase → moteur de génération
  Responsabilité frontend : alimenter l'écran /session/:id/generate (liste fichiers, code viewer)
```

#### Interaction conversationnelle (transverse)

```
POST /api/v1/workbench/sessions/{sessionId}/chat
  Corps : ChatRequest
  → ChatResponse
  Responsabilité backend : HandleChatUseCase → interprète le prompt, route vers le bon use case, retourne réponse textuelle + artefacts éventuels
  Responsabilité frontend : alimenter le ConversationalPanelComponent dans les phases 2, 3, 4
```

---

### DTO initiaux nommés

#### DTO d'entrée

```java
// Soumission du source controller
record SubmitSourceRequest(
    String controllerSourceCode,   // texte brut du fichier .java
    String controllerFileName,     // nom du fichier (ex: PatientController.java)
    String projectContext          // contexte optionnel (ex: "module facturation")
) {}

// Analyse (peut rester vide pour l'Epic 01)
record AnalyzeRequest(
    List<String> analysisOptions   // ex: ["STRUCTURAL", "RESPONSIBILITY", "METRICS"]
) {}

// Sélection des lots de migration
record SelectLotsRequest(
    List<Integer> selectedLots,    // ex: [1, 2] pour Lot 1 + Lot 2
    Map<String, String> constraints // ex: {"preserve": "legacy-services"}
) {}

// Génération de code
record GenerateRequest(
    int targetLot,                 // lot à générer (1 à 5)
    List<String> artefactTypes,    // ex: ["VIEWMODEL", "USE_CASES", "POLICIES"]
    boolean includeExplanation     // inclure le document de raisonnement
) {}

// Prompt conversationnel
record ChatRequest(
    String prompt,                 // texte libre de l'utilisateur
    String currentPhase            // contexte de la phase active : ANALYZE|DIAGNOSE|PLAN|GENERATE
) {}
```

#### DTO de sortie

```java
// Vue synthèse workbench (existant, confirmé)
record WorkbenchOverviewResponse(
    String productName,
    String version,
    List<RefactoringLotResponse> lots,
    List<AgentOverviewResponse> agents
) {}

record RefactoringLotResponse(int number, String name, String objective) {}
record AgentOverviewResponse(String name, String mission, String model) {}

// Session créée
record SessionCreatedResponse(
    UUID sessionId,
    String controllerFileName,
    String status,                 // CREATED | ANALYZING | DIAGNOSED | PLANNED | GENERATED
    Instant createdAt
) {}

// Résumé de session
record SessionSummaryResponse(
    UUID sessionId,
    String controllerFileName,
    String status,
    StructuralMetricsResponse metrics  // peut être null si analyse non encore faite
) {}

// Métriques structurelles
record StructuralMetricsResponse(
    int totalLines,
    int fxmlComponentCount,
    int injectedServiceCount,
    int handlerCount,
    int privateMethodCount,
    int externalCallCount
) {}

// Résultat d'analyse
record AnalysisResultResponse(
    UUID sessionId,
    StructuralMetricsResponse metrics,
    List<ResponsibilityZoneResponse> responsibilityZones
) {}

record ResponsibilityZoneResponse(
    String methodName,
    String responsibilityType,     // UI | STATE | ORCHESTRATION | BUSINESS | TECHNICAL
    String summary,
    int complexityScore
) {}

// Diagnostic
record DiagnosticResponse(
    UUID sessionId,
    List<ResponsibilityZoneResponse> zones,
    List<String> dominantDebtTypes,  // ex: ["BUSINESS_DEBT", "COUPLING_DEBT"]
    List<String> majorHandlers,
    String architectureTargetSummary
) {}

// Plan de migration
record MigrationPlanResponse(
    UUID sessionId,
    List<Integer> selectedLots,
    List<MigrationLotDetailResponse> lotDetails,
    ArchitectureTargetResponse architectureTarget
) {}

record MigrationLotDetailResponse(
    int lotNumber,
    String objective,
    List<String> artefactsToCreate,
    List<String> artefactsToKeepInController
) {}

record ArchitectureTargetResponse(
    List<String> layersToCreate,   // ex: ["ViewModel", "UseCases", "Policies", "Gateways"]
    List<String> remainsInController,
    List<String> exitsController
) {}

// Résultat de génération
record GenerationResultResponse(
    UUID sessionId,
    int generatedLot,
    List<GeneratedFileResponse> files,
    String reasoningDocument        // markdown d'explication, nullable
) {}

record GeneratedFileResponse(
    String packagePath,
    String className,
    String fileContent,
    String artefactType            // CONTROLLER | VIEWMODEL | USE_CASE | POLICY | GATEWAY | ASSEMBLER | STRATEGY
) {}

// Réponse conversationnelle
record ChatResponse(
    String responseText,
    String resolvedIntent,         // intention détectée : ANALYZE | DIAGNOSE | PLAN | GENERATE | EXPLAIN
    GenerationResultResponse generatedArtefacts  // nullable, si la commande a produit du code
) {}
```

---

### Responsabilités backend / frontend résumées

| Responsabilité | Backend (hexagonal) | Frontend Angular |
|---|---|---|
| Réception et validation du source Java | SubmitSourceRequest → validation adapter | Envoi du texte brut, affichage confirmation |
| Ingestion et analyse structurelle | AnalyzeControllerUseCase → StructuralAnalysisPort | Affichage métriques (AnalysisResultResponse) |
| Diagnostic et cartographie | GetDiagnosticUseCase → domaine DiagnosticReport | Affichage carte des responsabilités |
| Construction du plan de migration | GetMigrationPlanUseCase → domaine MigrationPlan | Affichage lots, sélection interactive |
| Génération du code refactoré | GenerateCodeUseCase → moteur génération | Affichage fichiers, viewer, export |
| Interprétation des prompts chat | HandleChatUseCase → routing vers use case ciblé | Affichage réponse + artefacts dans ConversationalPanel |
| État de session | SessionRepository (mémoire en Epic 01) | Stockage du sessionId courant (service Angular) |
| Logique métier de refactoring | Domain pur (policies, assemblers, use cases) | Aucune |

---

### Nouveaux packages backend à créer

```text
application/ports/in/
  CreateAnalysisSessionUseCase.java
  AnalyzeControllerUseCase.java
  GetDiagnosticUseCase.java
  GetMigrationPlanUseCase.java
  UpdateSelectedLotsUseCase.java
  GenerateCodeUseCase.java
  HandleChatUseCase.java

application/ports/out/
  AnalysisSessionRepository.java    (port sortant, stockage de session)
  StructuralAnalysisPort.java       (port sortant, moteur d'analyse)
  CodeGenerationPort.java           (port sortant, moteur de génération)

domain/workbench/
  AnalysisSession.java              (entité domaine, agrège source + résultats)
  DiagnosticReport.java
  MigrationPlan.java
  GeneratedArtefact.java

adapters/in/rest/dto/
  [tous les records DTO listés ci-dessus]

adapters/in/rest/mapper/
  SessionMapper.java
  DiagnosticMapper.java
  MigrationPlanMapper.java
  GenerationResultMapper.java
```

---

## dependances

- JAS-01 : parcours utilisateur validé, écrans Angular et actions conversationnelles prioritaires
- `backend/docs/architecture.md` : packages existants, endpoint existant, objets domaine initiaux
- `agents/catalog.md` : agents `backend-hexagonal` et `frontend-angular` sont les consommateurs directs

---

## verifications

- [ ] Chaque phase du parcours JAS-01 a au moins un endpoint correspondant dans ce contrat
- [ ] Tous les DTO d'entrée et de sortie sont nommés et structurés
- [ ] Aucun DTO ne contient de logique métier (records immuables uniquement)
- [ ] Les nouveaux use cases sont dans `application.ports.in` (respect de l'hexagone)
- [ ] Le domaine (`domain/workbench/`) ne contient aucune annotation Spring ni import REST
- [ ] L'endpoint conversationnel `POST .../chat` permet de couvrir les actions conversationnelles prioritaires listées en JAS-01
- [ ] Les responsabilités backend/frontend sont clairement séparées sans ambiguïté

---

## handoff

```
handoff:
  vers: securite (agent JAS-03)
  preconditions:
    - Ce document est validé (endpoints, DTO, responsabilités)
    - JAS-01 est validé
  points_de_vigilance:
    - L'endpoint POST /sessions reçoit du code source Java : c'est un flux sensible à contrôler
    - L'endpoint POST .../chat reçoit des prompts libres : risque d'injection à traiter
    - L'endpoint POST .../generate produit du code exécutable : journalisation des artefacts produits requise
    - Les sessionId (UUID) doivent être opaques et non-prédictibles
    - Pas d'authentification dans cet epic mais la surface d'exposition doit être documentée
  artefacts_a_consulter:
    - jira/epic-01/JAS-01-parcours-utilisateur.md
    - jira/epic-01/JAS-02-contrat-api-workbench.md (ce fichier)
    - backend/docs/architecture.md
    - agents/catalog.md (agent securite)
```
