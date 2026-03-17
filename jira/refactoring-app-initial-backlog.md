# Backlog JIRA - JavaFXAuditStudio2 : Genericite et Qualite des Artefacts

## Metadonnees

- Date de generation : 2026-03-17
- Derniere mise a jour : 2026-03-17
- Agent : `jira-estimation`
- Version : 2.1
- Perimetre : amelioration de la genericite du moteur d'analyse et de la qualite des artefacts generes
- Source de verite : `guide_generique_refactoring_controller_javafx_spring.md`, `AGENTS.md`, `agents/contracts.md`, `agents/orchestration.md`
- Stack cible : Spring Boot 4.0.3 / Java 21 / Angular 19 / PostgreSQL / JavaParser

---

## État d'avancement global

| Statut | Légende |
|--------|---------|
| ✅ DONE | Implémenté, testé, 104 tests verts |
| 🔄 PARTIAL | Partiellement implémenté — critères d'acceptation non tous couverts |
| ⏳ TODO | Pas encore commencé |

| ID | Titre (court) | Statut | Notes |
|----|---------------|--------|-------|
| JAS-001 | Indicateur mode parsing AST vs regex dans l'UI | ⏳ TODO | |
| JAS-002 | Dictionnaire regex étendu (SERVICE_CALL_PATTERN) | 🔄 PARTIAL | `xxxService.` détecté, YAML config non faite |
| JAS-003 | Filtrage méthodes lifecycle | ⏳ TODO | |
| JAS-004 | Suite tests paramétrée corpus samples/ | ⏳ TODO | |
| JAS-005 | Log cause JavaParser silencieux | ⏳ TODO | |
| JAS-006 | Signatures méthodes (params + types retour) | ⏳ TODO | |
| JAS-007 | Composants JavaFX custom dans ViewModel | 🔄 PARTIAL | Types standard mappés, héritage non résolu |
| JAS-008 | Nommage sémantique UseCase | 🔄 PARTIAL | Services → GATEWAY (non USE_CASE), pas de LLM |
| JAS-009 | Validation compilabilité artefacts | ⏳ TODO | Déduplication LinkedHashSet déjà présente |
| JAS-010 | Refactorisation Strategy pattern | ⏳ TODO | Générateur actuel monolithique |
| JAS-011 | Tests snapshot | ⏳ TODO | |
| JAS-012 | UI reclassification manuelle | ⏳ TODO | |
| JAS-013 | API REST PATCH reclassification | ⏳ TODO | |
| JAS-014 | Dashboard progression migration | ⏳ TODO | |
| JAS-015 | API métriques projet | ⏳ TODO | |
| JAS-016 | ClassificationBadgeComponent Angular | ⏳ TODO | |
| JAS-017 | Intégration Claude API (circuit breaker) | ⏳ TODO | |
| JAS-018 | Nommage sémantique UseCase par LLM | ⏳ TODO | |
| JAS-019 | Détection state machines | ⏳ TODO | |
| JAS-020 | Extraction gardes habilitation en Policy | ⏳ TODO | |
| JAS-021 | Prompt engineering corpus référence IA | ⏳ TODO | |
| JAS-022 | Config sécurisée clé API Claude | ⏳ TODO | |
| JAS-029 | Audit trail données LLM + UI transparence | ⏳ TODO | |
| JAS-030 | Visualisation raisonnement LLM (chain-of-thought) | ⏳ TODO | |
| JAS-023 | Analyse projet complet multi-controllers | ⏳ TODO | |
| JAS-024 | Dépendances inter-controllers + graphe | ⏳ TODO | |
| JAS-025 | Pipeline différentiel delta analysis | ⏳ TODO | |
| JAS-026 | Export artefacts structure Maven/Gradle | ⏳ TODO | |
| JAS-027 | Génération squelettes tests JUnit 5 | ⏳ TODO | |
| JAS-028 | Documentation OpenAPI 3.1 | ⏳ TODO | |

---

## Fonctionnalités livrées hors backlog (2026-03-17)

Ces fonctionnalités ont été implémentées avant ou pendant la création du backlog. Elles ne correspondent à aucun ticket existant mais constituent des fondations du pipeline opérationnel.

### [LIVRÉ] Pipeline complet 5 étapes fonctionnel
**Fichiers** : `GenerationConfiguration.java`, `RestitutionConfiguration.java`, `MigrationPlanConfiguration.java`, `AnalysisController.java`
- Endpoints `GET /sessions/{id}/plan`, `GET /sessions/{id}/artifacts`, `GET /sessions/{id}/report` opérationnels
- Chaque étape charge la session par ID et utilise `session.controllerName()` (corrigé — était sessionId à la place)
- `ExportArtifactsUseCase` + `ExportArtifactsService` + endpoint `POST /sessions/{id}/artifacts/export`

### [LIVRÉ] Fix contrainte unique `code_artifact` (Flyway V7)
**Fichier** : `V7__fix_code_artifact_unique_constraint.sql`
- Contrainte `UNIQUE (artifact_id)` → `UNIQUE (session_id, artifact_id)` pour permettre le multi-session
- Résout le `DataIntegrityViolationException` en mode régénération

### [LIVRÉ] Fix rapport "Non actionnable" malgré HIGH confidence
**Fichier** : `ProduceRestitutionService.java`
- Correction : 7ème param du constructeur `RestitutionSummary` est `hasContradictions` (pas `actionnable`) — `false` au lieu de `true`

### [LIVRÉ] Contenu du code généré exposé dans l'UI (viewer de code)
**Fichiers** : `ArtifactsResponse.java` (champ `content`), `artifacts-view.component.ts` (complet)
- Bouton "Voir le code" / "Masquer" par artefact
- Bloc `<pre class="code-block">` avec police monospace Cascadia Code
- Onglets par lot (Lot 1, Lot 2…)

### [LIVRÉ] Export `.java` vers le système de fichiers
**Fichiers** : `ExportArtifactsUseCase`, `ExportArtifactsService`, `ExportArtifactsRequest`, `ExportArtifactsResponse`, `artifacts-view.component.ts` (panneau export)
- Champ texte pour le répertoire cible
- `Files.writeString()` avec création du dossier si absent
- Retour : liste des fichiers exportés + erreurs

### [LIVRÉ] RealCodeGenerationAdapter — génération de vrai code Java
**Fichier** : `RealCodeGenerationAdapter.java`
- Extraction du package depuis les 30 premières lignes
- `methodNameFromRule()` avec 4 patterns ordonnés (handler, service, champ FXML, signature, fallback camelCase)
- Déduplication via `LinkedHashSet<String>` pour les interfaces
- Artefacts conditionnels (ne génère pas de stubs vides si aucune règle)

### [LIVRÉ] JavaParserRuleExtractionAdapter — classification @FXML améliorée
**Fichier** : `JavaParserRuleExtractionAdapter.java`
- `addFxmlFieldRules()` : les champs `@FXML` (composants UI) génèrent des règles `VIEW_MODEL` avec description `"Champ FXML TypeName fieldName"`
- `classifyMethod()` révisé : `@FXML` + appel service → APPLICATION (plus UI par défaut)
- `containsServiceCalls()` : détecte `fieldHint.toLowerCase() + "."`, `service.`, `usecase.`, `repository.`, `gateway.`
- `buildDescription()` : méthodes `@FXML` → format `"Methode handler X : responsabilite Y"` aligné sur le fallback regex

### [LIVRÉ] JavaControllerRuleExtractionAdapter — services → GATEWAY
**Fichier** : `JavaControllerRuleExtractionAdapter.java`
- `SERVICE_CALL_PATTERN = Pattern.compile("\\w+[Ss]ervice\\.")` ajouté
- `classifyByKeywords()` : appel `xxxService.` → APPLICATION (résout les 12 handlers UNKNOWN du Bridge)
- `extractInjectedServices()` : services injectés → `TECHNICAL / GATEWAY` au lieu de `APPLICATION / USE_CASE` (les handlers @FXML sont les vraies intentions utilisateur)
- `isInfrastructureType()` remplace `isApplicationLayerService()`

### [LIVRÉ] RealCodeGenerationAdapter — ViewModel avec propriétés typées
**Fichier** : `RealCodeGenerationAdapter.java`
- `extractFxmlType()` : extrait le type JavaFX depuis la description `"Champ FXML TypeName fieldName"`
- `fxmlTypeToProperty()` : mapping sémantique type → propriété :
  - Conteneurs (VBox, HBox, GridPane…) → `BooleanProperty xxxVisible`
  - Boutons (Button, Btn…) → `BooleanProperty xxxEnabled`
  - Cases à cocher (CheckBox, Chcbx…) → `BooleanProperty xxxSelected`
  - Labels, TextFields → `StringProperty xxxText`
  - TableView, ListView → `// TODO: ObservableList`
  - Types custom → heuristique par nom de champ
- Handlers `"Methode handler"` filtrés du ViewModel (plus de fuite d'EVENT_HANDLER dans VIEW_MODEL)
- `ViewModelProperty` record + `PropertyType` enum

### [LIVRÉ] SlimController avec handlers @FXML réels
**Fichier** : `RealCodeGenerationAdapter.java` — `buildControllerSlim()`
- Un stub `@FXML void handlerName() { useCase.handlerName(); }` généré pour chaque règle USE_CASE identifiée
- Déduplication des handlers via `LinkedHashSet`

---

## Definition of Done (globale)

Tout ticket est considered DONE lorsque les conditions suivantes sont toutes satisfaites :

**Qualite code**
- [ ] Le code backend respecte l'architecture hexagonale stricte (domain, application, ports, adapters, configuration)
- [ ] Aucune dependance Spring ou JPA dans le package `domain`
- [ ] Methodes <= 30 lignes sauf derogation commentee dans le ticket
- [ ] Imbrication <= 3 niveaux
- [ ] Maximum 4 parametres par methode
- [ ] Pas de `null` retourne sans justification documentee
- [ ] Pas de `continue` dans les boucles
- [ ] Un seul `return` par defaut dans chaque methode

**Qualite tests**
- [ ] Couverture de tests unitaires >= 80 % sur les classes nouvelles ou modifiees
- [ ] Tests d'integration valides sur les endpoints REST concernes
- [ ] Aucune regression sur les tests existants

**Qualite frontend**
- [ ] Composant Angular avec `ChangeDetectionStrategy.OnPush`
- [ ] Utilisation des signals Angular 19 pour l'etat reactif
- [ ] Aucune logique metier de refactoring dans le composant Angular

**Livraison**
- [ ] PR revue et approuvee par au moins un pair
- [ ] Migration Flyway fournie si schema modifie
- [ ] Logs debug activables par configuration, non sensibles
- [ ] Documentation du comportement dans le ticket ferme
- [ ] Handoff inter-agent documente si applicable

---

## Risques globaux

| ID | Risque | Probabilite | Impact | Mitigation |
|----|--------|-------------|--------|------------|
| R-01 | JavaParser instable sur code legacy anonymise | Haute | Eleve | Fallback regex robuste + indicateur de confiance explicite |
| R-02 | Enrichissement LLM introduit des latences > 30s | Moyenne | Eleve | Circuit breaker, timeout configurable, mode degraded sans LLM |
| R-03 | Generation de code non compilable non detectee | Haute | Critique | Validation syntaxique JavaParser sur artefacts generes avant persistance |
| R-04 | Base de donnees saturee par projets multi-controllers | Faible | Moyen | Pagination, archivage et purge configurable par projet |
| R-05 | Reclassification manuelle incohérente entre sessions | Moyenne | Moyen | Audit log des reclassifications, verrouillage optimiste |
| R-06 | Plugin IDE depend d'APIs privees changeantes | Haute | Moyen | Abstraction du protocole d'export, decouplage par contrat fichier |
| R-07 | Detection des state machines fausse-positive | Moyenne | Moyen | Seuil de confiance configurable, validation humaine obligatoire |
| R-08 | Regression silencieuse en mode regex fallback | Haute | Eleve | Metriques comparatives AST vs regex loggees et exposees API |

---

## EPIC 1 - Robustesse de l'extraction

**ID Epic** : JAS-EPIC-01
**Description** : Fiabiliser le pipeline d'extraction pour garantir une classification correcte meme sur du code Java invalide, anonymise ou partiellement parsable. Couvre les limites 1, 2 et 5.
**Composant** : Backend / Infra
**Priorite** : Critical
**Estimation globale** : 42 points

---

### JAS-001

**Type** : Story
**Titre** : Indicateur de mode parsing (AST vs regex) expose dans le rapport et l'API
**Description** : Actuellement, quand JavaParser echoue et que le fallback regex prend le relais, aucune indication n'est donnee ni dans les artefacts generes, ni dans l'UI, ni dans la reponse API. L'utilisateur recoit des artefacts de qualite degradee sans en etre informe. Il faut creer un champ `parsingMode` (valeurs : `AST`, `REGEX_FALLBACK`) dans le DTO de reponse et le persister en base. L'UI doit afficher une banniere d'avertissement visible quand le mode REGEX_FALLBACK est actif.
**Criteres d'acceptation** :
- [ ] Given un fichier Java avec `import .business.*`, When l'analyse est lancee, Then le champ `parsingMode` de la reponse vaut `REGEX_FALLBACK`
- [ ] Given un fichier Java valide, When l'analyse est lancee, Then le champ `parsingMode` vaut `AST`
- [ ] Given une analyse en mode `REGEX_FALLBACK`, When l'utilisateur consulte le rapport Angular, Then une banniere orange avec le texte "Analyse en mode regex - precision reduite" est visible
- [ ] Given une analyse en mode `REGEX_FALLBACK`, When l'utilisateur consulte le detail d'un handler, Then le badge de confiance de chaque regle classifiee affiche "Faible"
- [ ] La valeur `parsingMode` est persistee dans la table `analysis_result` et retournee par `GET /api/analyses/{id}`
**Estimation** : 5
**Priorite** : Critical
**Dependances** : aucune
**Composant** : Backend, Frontend

---

### JAS-002

**Type** : Story
**Statut** : 🔄 PARTIAL
**Titre** : Enrichissement du dictionnaire de mots-cles regex pour classification APPLICATION et BUSINESS
**Description** : Le fallback regex ne reconnait que 8 patterns (`setText`, `setVisible`, `service.save`, `repository`, `execute`, `invoke`, `xxxService.`, `restTemplate`). En consequence, des handlers tels que `calculateur.compute()`, `gestionnaire.traiter()`, `moteur.calculer()`, `processeur.executer()` sont classes UNKNOWN et genereront des Bridges inutiles. Il faut etendre le dictionnaire de patterns regex avec des families semantiques, configurables via un fichier YAML externe.

**Implémenté** :
- ✅ `SERVICE_CALL_PATTERN = Pattern.compile("\\w+[Ss]ervice\\.")` — détecte `xyzService.method()` → APPLICATION
- ✅ `classifyByKeywords()` étendu avec ce pattern (résout les 12 handlers UNKNOWN de `ExamenParacliniqueController`)
- ✅ `@FXML` handlers → APPLICATION par défaut dans `JavaParserRuleExtractionAdapter`

**Restant** :
- [ ] Given un handler contenant `calculateur.compute(`, When la classification regex est executee, Then la categorie est `APPLICATION` (non `UNKNOWN`)
- [ ] Given un handler contenant `gestionnaire.traiter(`, When la classification regex est executee, Then la categorie est `APPLICATION`
- [ ] Given un handler contenant `repository.findBy`, When la classification regex est executee, Then la categorie est `TECHNICAL`
- [ ] Le fichier de configuration `classification-patterns.yml` est charge au demarrage de l'application et rechargeable sans redemarrage (Actuator refresh)
- [ ] Le dictionnaire supporte au moins 5 familles : UI, APPLICATION, BUSINESS, TECHNICAL, INFRASTRUCTURE
- [x] La proportion de classifications UNKNOWN < 20% sur l'echantillon `ExamenParacliniqueController` fourni dans `samples/` *(12→0 UNKNOWN grâce à SERVICE_CALL_PATTERN)*
**Estimation** : 8 (reste ~5 pts)
**Priorite** : Critical
**Dependances** : JAS-001
**Composant** : Backend

---

### JAS-003

**Type** : Story
**Titre** : Filtrage automatique des methodes lifecycle JavaFX (initialize, dispose, stop)
**Description** : Les methodes `initialize()`, `dispose()` et `stop()` sont des methodes de cycle de vie JavaFX standard. Elles ne representent pas des intentions utilisateur et ne doivent pas generer de UseCase. Actuellement elles sont traitees comme des handlers et produisent des UseCases vides. Il faut creer une liste de methodes exclues configurable et l'appliquer avant la phase de classification.
**Criteres d'acceptation** :
- [ ] Given un controller avec une methode `initialize()`, When l'analyse est lancee, Then `initialize` n'apparait pas dans la liste des handlers du rapport
- [ ] Given un controller avec une methode `dispose()`, When l'analyse est lancee, Then aucun UseCase nomme `dispose` n'est genere
- [ ] Given un controller avec une methode `stop()`, When l'analyse est lancee, Then `stop` est absent de la cartographie des responsabilites
- [ ] La liste des methodes filtrees est configurable via propriete `analysis.lifecycle-methods.excluded` dans `application.yml`
- [ ] Une methode `initializeSpecifique()` (prefixe mais differente) n'est PAS filtree
- [ ] Le rapport de diagnostic affiche un compteur "X methodes lifecycle exclues de l'analyse"
**Estimation** : 3
**Priorite** : High
**Dependances** : aucune
**Composant** : Backend

---

### JAS-004

**Type** : Task
**Titre** : Tests unitaires couvrant les deux modes de parsing (AST et regex) sur le corpus samples/
**Description** : Creer une suite de tests paramétrée qui execute l'analyse sur les fichiers du repertoire `samples/` en mode AST et en mode regex force, puis compare les taux de classification par categorie. Cette suite servira de filet de securite pour toute evolution du moteur.
**Criteres d'acceptation** :
- [ ] La suite de tests est executable avec `./mvnw test -Dtest=ParsingModeSuiteTest`
- [ ] Les resultats AST vs regex sont loggues dans un rapport HTML Maven Surefire
- [ ] Un test echoue si le taux UNKNOWN en mode regex depasse 25% sur un sample
- [ ] Les tests sont integres dans le pipeline CI (JAS-EPIC-01 prerequis de merge)
**Estimation** : 3
**Priorite** : High
**Dependances** : JAS-001, JAS-002
**Composant** : Backend

---

### JAS-005

**Type** : Bug
**Titre** : JavaParser plante silencieusement sur import syntaxiquement invalide sans logger la cause
**Description** : Quand JavaParser rencontre `import .business.*` (point initial), il leve une `ParseProblemException` qui est avalee et le fallback est active sans tracer la cause dans les logs. En production, il est impossible de savoir si le fallback a ete declenche par ce cas ou par un autre. Il faut capturer et logger la cause au niveau WARN avec le nom du fichier et la ligne fautive, et l'inclure dans les metadonnees de l'analyse.
**Criteres d'acceptation** :
- [ ] Given un fichier avec `import .business.*`, When l'analyse est lancee, Then un log WARN contient le nom du fichier et le message de l'exception JavaParser
- [ ] La cause de l'echec AST est stockee dans le champ `parsingFallbackReason` du DTO de reponse
- [ ] Le champ `parsingFallbackReason` est retourne par `GET /api/analyses/{id}`
- [ ] Aucun log de niveau ERROR n'est emis pour ce cas (c'est un cas attendu, non une erreur)
**Estimation** : 2
**Priorite** : High
**Dependances** : JAS-001
**Composant** : Backend

---

## EPIC 2 - Qualite des artefacts generes

**ID Epic** : JAS-EPIC-02
**Description** : Ameliorer la precision et la completude des artefacts Java generes (UseCase, Gateway, ViewModel, etc.) en preservant les signatures de methodes, en gerant les composants custom JavaFX et en validant la compilabilite du code produit. Couvre les limites 3, 4, 7 et 10.
**Composant** : Backend
**Priorite** : Critical
**Estimation globale** : 55 points

---

### JAS-006

**Type** : Story
**Titre** : Preservation des signatures de methodes (parametres et types de retour) dans les artefacts generes
**Description** : Actuellement tous les UseCases generes ont la signature `void method()` sans parametres, et tous les Gateways retournent `Object` avec `Object request`. Un handler `void lancerExamen(Long patientId, ExamenType type)` perd completement sa signature. Il faut extraire les parametres et les types de retour depuis l'AST JavaParser (ou depuis le regex si en mode fallback) et les propager dans les artefacts generes.
**Criteres d'acceptation** :
- [ ] Given un handler `void lancerExamen(Long patientId, ExamenType type)`, When le UseCase est genere, Then la methode generee est `void execute(Long patientId, ExamenType type)` (ou equivalent semantique)
- [ ] Given un handler `PatientDto chargerPatient(Long id)`, When le UseCase est genere, Then le type de retour `PatientDto` est preserve
- [ ] Given un Gateway issu d'un handler avec parametres, When le code est genere, Then l'interface du port contient les memes types (non `Object`)
- [ ] En mode regex fallback, les parametres sont marques `/* type inconnu */` et un commentaire l'indique
- [ ] La generation ne plante pas si le type est une classe interne ou un generique simple (`List<String>`)
- [ ] Les imports necessaires sont detectes et inclus dans le fichier genere
**Estimation** : 13
**Priorite** : Critical
**Dependances** : JAS-001
**Composant** : Backend

---

### JAS-007

**Type** : Story
**Statut** : 🔄 PARTIAL
**Titre** : Detection des composants JavaFX custom dans le ViewModel (heritage GridPane, TableView)
**Description** : Les composants custom tels que `ExamensRealisesGridPane` et `ExamensRealisesTableView` ne sont pas reconnus par le moteur car ils n'apparaissent pas dans la liste des types JavaFX standard. Le fallback heuristique par nom echoue souvent. Il faut implementer une analyse d'heritage : si un champ est d'un type qui etend `GridPane`, `TableView`, `VBox`, `HBox`, etc., il doit etre reconnu comme composant JavaFX et mapper vers la propriete ViewModel appropriee.

**Implémenté** :
- ✅ `extractFxmlType()` extrait le type JavaFX depuis la description `"Champ FXML TypeName fieldName"`
- ✅ `fxmlTypeToProperty()` : mapping exhaustif types standard → propriété sémantique
  - Conteneurs (VBox, HBox, GridPane, Pane, BorderPane…) → `BooleanProperty xxxVisible`
  - Boutons (Button, Btn, MenuItem, Hyperlink) → `BooleanProperty xxxEnabled`
  - Cases à cocher (CheckBox, Chcbx, RadioButton, ToggleButton) → `BooleanProperty xxxSelected`
  - Labels, TextField, TextArea, PasswordField → `StringProperty xxxText`
  - ComboBox, ChoiceBox, Spinner, Slider → `StringProperty xxxValue`
  - TableView, ListView, TreeView → `// TODO: ObservableList`
- ✅ Handlers `"Methode handler"` filtrés du ViewModel (plus de fuite EVENT_HANDLER→VIEW_MODEL)

**Restant** :
- [ ] Given `ExamensRealisesGridPane extends GridPane`, When l'analyse est lancee, Then le champ est reconnu comme type `GridPane` (résolution par héritage)
- [ ] La resolution d'heritage fonctionne meme si la classe parente n'est pas dans les sources analysees
- [ ] Le rapport cartographie signale les composants reconnus par heritage vs par type direct
- [ ] Un avertissement est logue si le type custom ne peut pas etre resolu
**Estimation** : 8 (reste ~5 pts — résolution héritage uniquement)
**Priorite** : High
**Dependances** : JAS-006
**Composant** : Backend

---

### JAS-008

**Type** : Story
**Statut** : 🔄 PARTIAL
**Titre** : Transformation semantique des noms de methodes UseCase (technique vers intention metier)
**Description** : Les noms de methodes UseCase sont actuellement derives des noms de champs de services injectes (`donneesEntretienMedicalConclusionService` → `executeDonneesEntretienMedicalConclusion`). Ils doivent exprimer une intention metier lisible (`chargerDonneesConclusion`, `lancerExamen`). Il faut implementer un pipeline de transformation de nom : extraction du verbe d'action depuis le handler source, normalisation camelCase, suppression des suffixes techniques (`Service`, `Repository`, `Gateway`).

**Implémenté** :
- ✅ `extractInjectedServices()` : services `@Autowired` → `TECHNICAL / GATEWAY` (non plus `APPLICATION / USE_CASE`) — les handlers @FXML sont désormais la source des méthodes UseCase
- ✅ `methodNameFromRule()` : 4 patterns ordonnés (handler, service, champ FXML, signature, fallback camelCase)
- ✅ `isInfrastructureType()` : détecte Factory, Utils, Config, Converter, Adapter, Helper → GATEWAY

**Restant** :
- [ ] Given un handler `onBtnLancerExamenClicked()`, When le UseCase est genere, Then le nom est `lancerExamen` (suppression préfixe `on`, suffixe `Clicked`, `Action`, `Btn`)
- [ ] Given un service injecte `donneesEntretienMedicalConclusionService`, When il est en Gateway, Then le nom de methode du Gateway ne contient pas le suffixe `Service`
- [ ] Given un handler `handleSaveButtonAction()`, When le UseCase est genere, Then le nom est `sauvegarder` ou `enregistrer`
- [ ] Les noms transformes sont affichés dans le rapport avant generation pour validation
**Estimation** : 8 (reste ~5 pts — suppression des préfixes/suffixes techniques)
**Priorite** : High
**Dependances** : JAS-006
**Composant** : Backend

---

### JAS-009

**Type** : Story
**Titre** : Validation de compilabilite des artefacts generes (detection doublons et imports manquants)
**Description** : Le code genere n'est pas verifie pour sa compilabilite. Des methodes dupliquees dans les interfaces, des imports manquants ou des types non resolus peuvent produire du code invalide silencieusement. Il faut ajouter une etape de validation post-generation qui utilise JavaParser pour re-parser le code genere et detecter les problemes structurels avant de le persister.
**Criteres d'acceptation** :
- [ ] Given un UseCase genere avec deux methodes de meme signature, When la validation est executee, Then une erreur `DUPLICATE_METHOD` est retournee dans le champ `generationWarnings` du DTO
- [ ] Given un artefact genere avec un type non importe, When la validation est executee, Then un avertissement `MISSING_IMPORT` est retourne
- [ ] Given un artefact valide, When la validation est executee, Then `generationWarnings` est vide et le statut est `VALID`
- [ ] La validation est non bloquante : les artefacts avec avertissements sont generes mais marques `NEEDS_REVIEW`
- [ ] L'UI affiche les avertissements de generation dans la vue detail de chaque artefact
- [ ] Les artefacts marques `NEEDS_REVIEW` sont comptes dans le resume du rapport
**Estimation** : 8
**Priorite** : High
**Dependances** : JAS-006, JAS-007
**Composant** : Backend, Frontend

---

### JAS-010

**Type** : Task
**Titre** : Refactorisation du generateur d'artefacts en Strategy pattern (un generateur par type d'artefact)
**Description** : Le generateur d'artefacts actuel est monolithique. Avant d'implémenter JAS-006 et JAS-007, il faut extraire chaque type de generateur (UseCase, Gateway, ViewModel, Policy, Bridge, Assembler, Strategy) dans sa propre classe implementant une interface `ArtifactGenerator`. Cela permettra d'ajouter la gestion des signatures et des composants custom sans risquer de regression sur les autres types.
**Criteres d'acceptation** :
- [ ] L'interface `ArtifactGenerator<T extends ArtifactResult>` est definie dans le package `application.generation`
- [ ] Chaque type d'artefact a sa propre implementation : `UseCaseGenerator`, `GatewayGenerator`, `ViewModelGenerator`, `PolicyGenerator`, `BridgeGenerator`, `AssemblerGenerator`, `StrategyGenerator`
- [ ] Les tests existants continuent de passer apres refactorisation
- [ ] Le generateur principal orchestre les sous-generateurs via injection Spring (`List<ArtifactGenerator>`)
- [ ] La refactorisation ne modifie pas le comportement observable depuis l'API REST
**Estimation** : 5
**Priorite** : Critical
**Dependances** : aucune
**Composant** : Backend

---

### JAS-011

**Type** : Task
**Titre** : Tests de snapshot pour les artefacts generes (reference de non-regression)
**Description** : Creer des tests de snapshot qui generent les artefacts pour les samples du repertoire `samples/` et comparent la sortie a une reference enregistree. Tout changement non intentionnel dans la generation sera detecte automatiquement.
**Criteres d'acceptation** :
- [ ] Un test de snapshot existe pour chaque type d'artefact (UseCase, Gateway, ViewModel, Policy, Bridge, Assembler)
- [ ] Le test echoue si la sortie generee differe de la reference stockee dans `src/test/resources/snapshots/`
- [ ] La reference peut etre mise a jour via `./mvnw test -Dupdate-snapshots=true`
- [ ] Les snapshots sont commites dans le depot Git
**Estimation** : 3
**Priorite** : Medium
**Dependances** : JAS-010
**Composant** : Backend

---

## EPIC 3 - Experience utilisateur et reclassification

**ID Epic** : JAS-EPIC-03
**Description** : Permettre a l'utilisateur de corriger les classifications erronees depuis le frontend Angular, et offrir un tableau de bord de progression de migration par projet. Couvre la limite 9 et l'amelioration D.
**Composant** : Frontend, Backend
**Priorite** : High
**Estimation globale** : 44 points

---

### JAS-012

**Type** : Story
**Titre** : Interface de reclassification manuelle des regles de gestion (UI Angular)
**Description** : L'utilisateur ne peut pas corriger une mauvaise classification depuis le frontend. Tout ce qui est UNKNOWN reste en Bridge sans possibilite de re-router. Il faut creer une interface de reclassification qui permet de modifier la categorie d'une regle (UI, APPLICATION, BUSINESS, TECHNICAL, UNKNOWN) et de declencher la regeneration des artefacts affectes.
**Criteres d'acceptation** :
- [ ] Given une regle classifiee UNKNOWN, When l'utilisateur clique sur "Reclassifier", Then une modale s'ouvre avec les 5 categories disponibles
- [ ] Given l'utilisateur selectionne APPLICATION, When il confirme, Then la regle est mise a jour en base et les artefacts affectes sont regeneres
- [ ] Given une reclassification realisee, When l'utilisateur consulte l'historique, Then la reclassification apparait avec la date, l'utilisateur et les valeurs avant/apres
- [ ] La reclassification ne peut pas etre realisee sur une analyse verrouilee (statut `LOCKED`)
- [ ] Le rapport regenere apres reclassification indique le nombre de regles modifiees manuellement
- [ ] Les regles reclassifiees manuellement ont un badge "Modifie manuellement" dans l'UI
**Estimation** : 13
**Priorite** : High
**Dependances** : JAS-001
**Composant** : Frontend, Backend

---

### JAS-013

**Type** : Story
**Titre** : API REST de reclassification avec audit log
**Description** : Exposer les endpoints REST necessaires a la reclassification manuelle depuis le frontend, avec audit log en base de donnees pour tracer toutes les modifications.
**Criteres d'acceptation** :
- [ ] `PATCH /api/analyses/{analysisId}/rules/{ruleId}/classification` accepte `{"category": "APPLICATION", "reason": "texte libre"}`
- [ ] La reponse inclut l'etat complet de la regle apres modification
- [ ] Chaque appel cree un enregistrement dans la table `rule_classification_audit` (userId, ruleId, fromCategory, toCategory, reason, timestamp)
- [ ] `GET /api/analyses/{analysisId}/rules/{ruleId}/classification/history` retourne l'historique complet
- [ ] Le endpoint retourne 409 si l'analyse est en statut `LOCKED`
- [ ] La migration Flyway pour `rule_classification_audit` est fournie
**Estimation** : 5
**Priorite** : High
**Dependances** : JAS-012
**Composant** : Backend

---

### JAS-014

**Type** : Story
**Titre** : Dashboard de progression de migration par projet
**Description** : Creer une vue tableau de bord qui affiche pour chaque projet : le nombre total de controllers detectes, le pourcentage analyse, le pourcentage migre (lots 1 a 5 valides), les risques identifies et une timeline de migration.
**Criteres d'acceptation** :
- [ ] Given un projet avec 10 controllers, When le dashboard est affiche, Then les compteurs "analyste", "en cours" et "migres" sont corrects
- [ ] Le dashboard affiche un graphe en barres (ou equivalent) du nb de regles par categorie (UI/APPLICATION/BUSINESS/TECHNICAL/UNKNOWN)
- [ ] Une timeline affiche l'ordre recommande des lots avec les dependances entre controllers
- [ ] Le dashboard est accessible depuis la navigation principale en < 2 clics
- [ ] Les donnees sont actualisees automatiquement toutes les 30 secondes via polling ou SSE
- [ ] Le dashboard est responsive et utilisable sur ecran 1280px minimum
**Estimation** : 13
**Priorite** : Medium
**Dependances** : JAS-013
**Composant** : Frontend, Backend

---

### JAS-015

**Type** : Story
**Titre** : API agregation des metriques de progression par projet
**Description** : Exposer les donnees necessaires au dashboard via une API d'agregation qui calcule les indicateurs de progression par projet.
**Criteres d'acceptation** :
- [ ] `GET /api/projects/{projectId}/dashboard` retourne : `totalControllers`, `analysedControllers`, `migratedControllers`, `rulesByCategory` (map), `riskSummary`, `recommendedLotOrder`
- [ ] La reponse est calculee en < 500ms pour un projet de 50 controllers
- [ ] Le calcul utilise des requetes agregees JPA (pas de chargement de toutes les entites en memoire)
- [ ] Un cache Spring avec TTL 60s est applique sur le calcul d'agregation
- [ ] La migration Flyway pour la table `project` (si non existante) est fournie
**Estimation** : 8
**Priorite** : Medium
**Dependances** : JAS-013
**Composant** : Backend

---

### JAS-016

**Type** : Task
**Titre** : Composant Angular badge de confiance de classification
**Description** : Creer un composant Angular reutilisable `ClassificationBadgeComponent` qui affiche la categorie d'une regle avec un code couleur, le score de confiance et le mode de parsing utilise. Ce composant sera utilise dans la liste des regles et dans la vue de reclassification.
**Criteres d'acceptation** :
- [ ] Le composant affiche la categorie avec un code couleur (UI=bleu, APPLICATION=vert, BUSINESS=orange, TECHNICAL=gris, UNKNOWN=rouge)
- [ ] Le score de confiance est affiche en pourcentage (0-100%)
- [ ] Le badge "REGEX" ou "AST" est affiche en sous-texte
- [ ] Le composant accepte un Input signal `rule: RuleClassification`
- [ ] Le composant utilise `ChangeDetectionStrategy.OnPush`
- [ ] Des tests unitaires Karma couvrent les 5 categories
**Estimation** : 3
**Priorite** : Medium
**Dependances** : JAS-001
**Composant** : Frontend

---

## EPIC 4 - Analyse enrichie par intelligence artificielle

**ID Epic** : JAS-EPIC-04
**Description** : Integrer Claude API comme couche d'enrichissement optionnelle pour nommer les UseCases selon l'intention metier, detecter les state machines et enrichir les descriptions des regles. Couvre l'amelioration A et les limites 6 et 7.
**Composant** : Backend
**Priorite** : Medium
**Estimation globale** : 47 points

---

### JAS-017

**Type** : Story
**Titre** : Integration Claude API comme service d'enrichissement optionnel (circuit breaker + mode degrade)
**Description** : Creer le port `AiEnrichmentPort` et son adaptateur `ClaudeApiAiEnrichmentAdapter` permettant d'envoyer des extraits de code au LLM pour enrichissement. L'integration doit etre totalement optionnelle : si le service est absent ou lent, l'application fonctionne en mode degrade sans aucune perte fonctionnelle.
**Criteres d'acceptation** :
- [ ] Le port `AiEnrichmentPort` est dans le package `ports.out` (hexagone respecte)
- [ ] L'adaptateur `ClaudeApiAiEnrichmentAdapter` est dans `adapters.out.ai`
- [ ] Si la propriete `ai.enrichment.enabled=false`, aucun appel reseau n'est effectue
- [ ] Si l'API Claude est indisponible, un circuit breaker (Resilience4j) declenche le mode degrade en < 5s
- [ ] Le timeout est configurable via `ai.enrichment.timeout-ms` (defaut : 10000)
- [ ] Un log INFO indique si l'enrichissement IA est actif ou desactive au demarrage
- [ ] Le cout estimé en tokens est logue au niveau DEBUG pour chaque appel
**Estimation** : 8
**Priorite** : Medium
**Dependances** : JAS-010
**Composant** : Backend

---

### JAS-018

**Type** : Story
**Titre** : Nommage semantique des UseCases par Claude API
**Description** : Utiliser Claude API pour transformer les noms techniques de methodes en noms metier semantiques. Le LLM recoit le handler source (signature + corps), le contexte du controller (domaine metier detecte) et les conventions de nommage du projet. Il retourne un nom de UseCase expressif.
**Criteres d'acceptation** :
- [ ] Given un handler `onBtnValiderResultatExamenClicked()` avec corps contenant des appels a `examenService`, When l'enrichissement IA est active, Then le nom genere est semantique (ex: `validerResultatExamen`)
- [ ] Given le meme handler avec enrichissement IA desactive, When le UseCase est genere, Then la transformation regle JAS-008 s'applique (non-bloquant)
- [ ] Le nom suggere par l'IA est soumis a validation utilisateur avant d'etre persiste (flag `aiSuggested: true`)
- [ ] L'utilisateur peut accepter ou rejeter la suggestion depuis l'UI
- [ ] Le prompt envoye a Claude ne contient pas de donnees patient ou de donnees metier sensibles (uniquement la structure de code)
- [ ] Un test d'integration avec mock Claude API valide le flux complet
**Estimation** : 8
**Priorite** : Medium
**Dependances** : JAS-017, JAS-008
**Composant** : Backend, Frontend

---

### JAS-019

**Type** : Story
**Titre** : Detection de la logique d'etat (state machine) dans les controllers
**Description** : Les controllers avec mode tripartite (ex: examen/interpretation/consultation) ou gestion de selection de ligne contiennent une logique d'etat implicite non detectee. Il faut implementer un detecteur de state machine base sur l'analyse des champs booléens, des patterns switch/if imbriques et des transitions conditionnelles entre methodes.
**Criteres d'acceptation** :
- [ ] Given un controller avec 3 modes d'ecran geres par des flags booleens (`isExamenMode`, `isInterpretationMode`), When l'analyse est lancee, Then le rapport contient une section "Logique d'etat detectee" avec les etats et transitions identifies
- [ ] Given une state machine detectee, When les artefacts sont generes, Then un fichier `XxxState.java` (enum ou sealed class) est genere dans le package `ui/xxx/`
- [ ] Given une methode `isXxxEnabled()` presente dans le controller, When l'analyse est lancee, Then elle est categorisee comme Policy candidate (non comme handler UI)
- [ ] Le detecteur a un seuil de confiance : en dessous de 0.6, la detection est signalee comme "possible" non "confirmee"
- [ ] La detection fonctionne en mode AST et en mode regex (avec une precision moindre en regex)
**Estimation** : 13
**Priorite** : Medium
**Dependances** : JAS-002
**Composant** : Backend

---

### JAS-020

**Type** : Story
**Titre** : Extraction automatique des gardes d'habilitation en Policy candidates
**Description** : Les methodes `isXxxEnabled()`, `canXxx()`, `hasRightTo()` dans les controllers representent des gardes d'habilitation qui devraient etre extraites en Policy. Actuellement elles sont traitees comme des handlers ou ignorees. Il faut creer un detecteur specifique pour ces methodes et les marquer comme Policy candidates dans le rapport.
**Criteres d'acceptation** :
- [ ] Given une methode `boolean isLancerExamenEnabled()`, When l'analyse est lancee, Then la methode est categorisee `POLICY_CANDIDATE` dans le rapport
- [ ] Given une `POLICY_CANDIDATE`, When les artefacts du lot 2 sont generes, Then une classe `XxxPolicy.java` est generee dans `domain/xxx/`
- [ ] La Policy generee a une methode `boolean isAllowed(XxxContext context)` avec les parametres preserves
- [ ] Le rapport liste toutes les Policy candidates avec leur complexite cyclomatique estimee
- [ ] Les methodes `isVisible()` et `isDisable()` purement UI sont exclues de la detection Policy
**Estimation** : 8
**Priorite** : Medium
**Dependances** : JAS-006, JAS-019
**Composant** : Backend

---

### JAS-021

**Type** : Task
**Titre** : Prompt engineering et tests de qualite sur corpus reference
**Description** : Definir et tester les prompts envoyes a Claude API pour le nommage semantique et l'enrichissement des descriptions. Creer un corpus de test avec les reponses attendues et mesurer la precision des suggestions.
**Criteres d'acceptation** :
- [ ] Un corpus de 20 handlers avec noms attendus est disponible dans `src/test/resources/ai-corpus/`
- [ ] Le taux de precision des suggestions IA est > 70% sur ce corpus (mesure par similarite semantique)
- [ ] Les prompts sont externalises dans des fichiers de template Mustache (non hardcodes)
- [ ] Un test de regression compare les suggestions IA aux valeurs du corpus lors de chaque build (avec mock Claude)
**Estimation** : 5
**Priorite** : Low
**Dependances** : JAS-017, JAS-018
**Composant** : Backend

---

### JAS-022

**Type** : Task
**Titre** : Configuration securisee de la cle API Claude (Vault ou secret management)
**Description** : La cle API Claude ne doit jamais etre committee ni loggee. Mettre en place la gestion securisee via variable d'environnement avec validation au demarrage.
**Criteres d'acceptation** :
- [ ] La cle est injectee via `CLAUDE_API_KEY` (variable d'environnement), jamais en dur dans le code
- [ ] Si `ai.enrichment.enabled=true` et que la cle est absente, l'application refuse de demarrer avec un message explicite
- [ ] La cle n'apparait jamais dans les logs (masquee par un filtre Logback)
- [ ] Le `docker-compose.yml` reference la variable sans la valeur
- [ ] La documentation d'installation est mise a jour dans `docs/`
**Estimation** : 2
**Priorite** : High
**Dependances** : JAS-017
**Composant** : Infra, Backend

---

### JAS-029

**Type** : Story
**Titre** : Audit trail des données envoyées au LLM — transparence et conformité RGPD
**Description** :
Avant tout appel au LLM (Claude API ou autre), l'utilisateur doit pouvoir savoir exactement quelles données sont transmises au serveur externe. Deux exigences : (1) les données envoyées sont loguées côté backend dans une table d'audit dédiée, (2) un panneau de visualisation dans l'UI permet d'inspecter le payload exact de chaque appel LLM lié à une session.

Cela répond à deux besoins : la conformité (l'utilisateur consent en connaissance de cause à l'envoi de code source potentiellement sensible vers un LLM externe) et le debug (l'équipe peut diagnostiquer pourquoi le LLM a produit un résultat inattendu).

**Critères d'acceptation** :
- [ ] Chaque appel LLM (JAS-017, JAS-018, JAS-019, JAS-020) persiste en base un enregistrement `llm_audit_log` avec : `session_id`, `timestamp`, `model`, `endpoint`, `prompt_tokens`, `completion_tokens`, `payload_hash` (SHA-256 du prompt), `payload_text` (texte complet du prompt envoyé), `response_summary`
- [ ] La table `llm_audit_log` est créée via migration Flyway (V8 ou suivante)
- [ ] Le `payload_text` est tronqué à 50 000 caractères avec indicateur `truncated=true` si dépassement
- [ ] Un endpoint `GET /api/v1/analysis/sessions/{sessionId}/llm-audit` retourne la liste des appels LLM de la session (paginée, 20 entrées/page)
- [ ] Le frontend affiche dans l'onglet "Analyse IA" un tableau des appels LLM avec colonnes : horodatage, modèle, tokens utilisés, bouton "Voir le prompt"
- [ ] Le bouton "Voir le prompt" ouvre un drawer/modal avec le texte complet du prompt envoyé, syntaxiquement mis en évidence (zone de code non éditable)
- [ ] Un bandeau d'avertissement RGPD s'affiche la première fois que l'enrichissement IA est activé : "Le code source sera transmis à [fournisseur LLM]. Continuer ?"
- [ ] Le consentement est persisté en session (pas de répétition à chaque appel)
- [ ] L'ensemble est désactivable via `ai.audit.enabled=false` (par défaut `true` si IA activée)

**Estimation** : 8
**Priorité** : High
**Dépendances** : JAS-017, JAS-022
**Composant** : Backend, Frontend

---

### JAS-030

**Type** : Story
**Titre** : Visualisation du raisonnement détaillé du LLM (chain-of-thought) dans l'UI
**Description** :
Quand le LLM enrichit une classification ou nomme un UseCase, son raisonnement intermédiaire (chain-of-thought) doit être visible dans l'interface. L'objectif est double : permettre à l'utilisateur de comprendre POURQUOI le LLM a pris une décision, et lui donner les moyens de la valider ou de la rejeter en connaissance de cause.

Le raisonnement doit être demandé explicitement dans le prompt (via le champ `thinking` de l'API Claude ou via un prompt structuré demandant un bloc `<reasoning>…</reasoning>` avant la réponse finale), stocké séparément du payload audit (JAS-029), et affiché dans l'UI sous forme structurée (étapes numérotées, pas un bloc texte brut).

**Critères d'acceptation** :
- [ ] Le prompt système envoyé au LLM inclut une instruction explicite demandant un raisonnement structuré avant la réponse finale (format `<thinking>…</thinking>` puis `<answer>…</answer>`)
- [ ] Le backend parse la réponse LLM pour séparer le bloc `<thinking>` de la réponse finale `<answer>`
- [ ] La table `llm_audit_log` (JAS-029) contient une colonne `reasoning_text` (TEXT, nullable) stockant le bloc de raisonnement extrait
- [ ] Un endpoint `GET /api/v1/analysis/sessions/{sessionId}/llm-audit/{auditId}/reasoning` retourne le texte de raisonnement complet pour un appel donné
- [ ] Dans l'UI, chaque ligne du tableau d'audit LLM (JAS-029) affiche un badge "Raisonnement disponible" si `reasoning_text` est non null
- [ ] Un clic sur ce badge ouvre un panneau latéral (side panel) affichant le raisonnement découpé en étapes numérotées (split sur les sauts de ligne ou les marqueurs `Step N:`)
- [ ] Chaque étape du raisonnement est affichée avec un niveau d'indentation visuel selon sa profondeur logique
- [ ] Si le raisonnement est absent (modèle ne supportant pas le chain-of-thought), le badge est remplacé par "Non disponible" en gris
- [ ] Le panneau de raisonnement inclut un bouton "Copier le raisonnement" (clipboard)
- [ ] Les performances ne sont pas dégradées : le raisonnement n'est chargé qu'à la demande (lazy loading via l'endpoint dédié), pas dans la liste paginée

**Estimation** : 8
**Priorité** : High
**Dépendances** : JAS-029
**Composant** : Backend, Frontend

---

## EPIC 5 - Scalabilite et integration

**ID Epic** : JAS-EPIC-05
**Description** : Etendre l'outil pour supporter l'analyse multi-controllers (projet complet), le pipeline differentiel (delta analysis), l'export vers les IDE et la generation de squelettes de tests. Couvre la limite 8 et les ameliorations B, C et E.
**Composant** : Backend, Frontend, Infra
**Priorite** : Medium / Low
**Estimation globale** : 76 points

---

### JAS-023

**Type** : Story
**Titre** : Ingestion et analyse d'un projet complet (multi-controllers)
**Description** : L'outil analyse actuellement un seul controller a la fois. Il faut permettre l'upload ou le reference d'un repertoire de projet complet, l'analyse en batch de tous les controllers detectes et la creation d'une vue projet consolidee.
**Criteres d'acceptation** :
- [ ] `POST /api/projects` accepte un archive ZIP contenant les sources Java et FXML
- [ ] L'API detecte automatiquement tous les fichiers `*Controller.java` dans l'archive
- [ ] L'analyse de chaque controller est executee de maniere asynchrone (Spring @Async)
- [ ] `GET /api/projects/{projectId}/status` retourne l'avancement en temps reel (analysed/total)
- [ ] Le frontend affiche une barre de progression pendant l'analyse batch
- [ ] L'analyse d'un projet de 50 controllers se termine en < 120 secondes sur un serveur standard
- [ ] Les erreurs sur un controller individuel n'arretent pas l'analyse des autres
**Estimation** : 21
**Priorite** : Medium
**Dependances** : JAS-015
**Composant** : Backend, Frontend

---

### JAS-024

**Type** : Story
**Titre** : Detection des dependances entre controllers (appels inter-controllers)
**Description** : Dans un projet multi-controllers, certains controllers en appelent d'autres ou partagent des services. Il faut detecter ces dependances et les visualiser dans le dashboard pour definir l'ordre optimal de migration.
**Criteres d'acceptation** :
- [ ] Given deux controllers partageant un service injecte `patientService`, When l'analyse projet est realisee, Then une dependance "shared-service" est enregistree entre eux
- [ ] Given un controller A qui instancie ou appelle un controller B, When l'analyse est realisee, Then une dependance directe A->B est detectee
- [ ] `GET /api/projects/{projectId}/dependency-graph` retourne le graphe de dependances au format JSON (noeuds + aretes)
- [ ] Le frontend affiche le graphe avec D3.js ou equivalent (pas de librairie proprietaire)
- [ ] L'ordre de migration recommande respecte les dependances detectees (topological sort)
**Estimation** : 13
**Priorite** : Medium
**Dependances** : JAS-023
**Composant** : Backend, Frontend

---

### JAS-025

**Type** : Story
**Titre** : Pipeline differentiel - ne generer que les artefacts manquants ou modifies
**Description** : Actuellement chaque analyse regenere tous les artefacts depuis zero. Si le projet cible existe deja partiellement, le pipeline doit comparer le code genere avec l'existant et ne produire que ce qui manque ou a change. Cela evite d'ecraser le travail manuel deja realise.
**Criteres d'acceptation** :
- [ ] `POST /api/analyses/{analysisId}/generate?mode=differential` accepte un ZIP du projet cible existant
- [ ] Le moteur compare chaque artefact genere avec l'existant via hash SHA-256
- [ ] Les artefacts identiques ne sont pas regéneres (status `UNCHANGED`)
- [ ] Les artefacts differents sont marques `MODIFIED` avec un diff textuel disponible
- [ ] Les artefacts absents du projet cible sont marques `NEW`
- [ ] Le rapport differentiel liste les artefacts par statut (UNCHANGED/MODIFIED/NEW)
- [ ] L'utilisateur peut selectionner quels artefacts MODIFIED il souhaite ecraser
**Estimation** : 13
**Priorite** : Low
**Dependances** : JAS-023
**Composant** : Backend, Frontend

---

### JAS-026

**Type** : Story
**Titre** : Export des artefacts vers une arborescence Maven/Gradle respectant la structure de packages
**Description** : Permettre l'export des artefacts generes directement dans l'arborescence d'un projet Maven ou Gradle cible, en respectant la structure de packages et les conventions du projet (groupId, basePackage).
**Criteres d'acceptation** :
- [ ] `GET /api/analyses/{analysisId}/export?format=maven-zip` retourne un ZIP avec la structure `src/main/java/{basePackage}/...`
- [ ] La configuration `groupId` et `basePackage` est definissable au niveau projet
- [ ] Les fichiers generes respectent le package declare en debut de fichier
- [ ] Le ZIP inclut les migrations Flyway si des entites sont generees
- [ ] Le format Gradle (`src/main/java/...` identique) est supporte
- [ ] L'export peut etre filtre par type d'artefact (usecase seulement, gateway seulement, etc.)
**Estimation** : 8
**Priorite** : Low
**Dependances** : JAS-009
**Composant** : Backend

---

### JAS-027

**Type** : Story
**Titre** : Generation de squelettes de tests JUnit 5 pour chaque artefact produit
**Description** : Pour chaque artefact genere (UseCase, Policy, Gateway, Assembler), generer automatiquement un squelette de test JUnit 5 avec Mockito pour les dependances. Les squelettes doivent compiler et inclure au moins un test de cas nominal et un test de cas d'erreur.
**Criteres d'acceptation** :
- [ ] Given un UseCase `LancerExamenUseCase` genere, When la generation de tests est activee, Then `LancerExamenUseCaseTest.java` est genere dans `src/test/java/`
- [ ] Le test genere contient un `@Test void shouldExecuteNominalCase()` et un `@Test void shouldThrowWhenPreconditionFails()`
- [ ] Les dependances du UseCase sont mockees avec Mockito (`@Mock`, `@InjectMocks`)
- [ ] Given une Policy generee, When les tests sont generes, Then un test verifie le cas `isAllowed=true` et un autre `isAllowed=false`
- [ ] Les tests squelettes compilent sans modification (les corps de test ont des `// TODO: implement assertion`)
- [ ] Le rapport de generation indique combien de tests squelettes ont ete produits
**Estimation** : 13
**Priorite** : Low
**Dependances** : JAS-009, JAS-010
**Composant** : Backend

---

### JAS-028

**Type** : Task
**Titre** : Documentation API OpenAPI 3.1 pour tous les nouveaux endpoints
**Description** : Documenter tous les endpoints introduits par les epics 1 a 5 avec des annotations SpringDoc OpenAPI 3.1. La documentation doit etre disponible via Swagger UI integre.
**Criteres d'acceptation** :
- [ ] Chaque endpoint a une description `@Operation`, des `@Parameter` et des `@ApiResponse` documentes
- [ ] Swagger UI est accessible a `/swagger-ui.html` en profil `dev`
- [ ] Les DTOs ont des annotations `@Schema` avec description et exemple
- [ ] La documentation est generee automatiquement au build via plugin Maven springdoc
- [ ] Le fichier `openapi.yaml` genere est commite dans `docs/api/`
**Estimation** : 5
**Priorite** : Medium
**Dependances** : JAS-006, JAS-012, JAS-013, JAS-015, JAS-017, JAS-023
**Composant** : Backend

---

## Recapitulatif du backlog

| ID | Type | Titre (abrege) | Epic | Points | Priorite | Composant |
|----|------|----------------|------|--------|----------|-----------|
| JAS-001 | Story | Indicateur mode parsing AST vs regex | EPIC-01 | 5 | Critical | Back+Front |
| JAS-002 | Story | Enrichissement dictionnaire regex classification | EPIC-01 | 8 | Critical | Backend |
| JAS-003 | Story | Filtrage methodes lifecycle JavaFX | EPIC-01 | 3 | High | Backend |
| JAS-004 | Task | Tests unitaires corpus samples/ | EPIC-01 | 3 | High | Backend |
| JAS-005 | Bug | JavaParser plante silencieusement sur import invalide | EPIC-01 | 2 | High | Backend |
| JAS-006 | Story | Preservation signatures methodes dans artefacts | EPIC-02 | 13 | Critical | Backend |
| JAS-007 | Story | Detection composants JavaFX custom (heritage) | EPIC-02 | 8 | High | Backend |
| JAS-008 | Story | Transformation semantique noms UseCase | EPIC-02 | 8 | High | Backend |
| JAS-009 | Story | Validation compilabilite artefacts generes | EPIC-02 | 8 | High | Back+Front |
| JAS-010 | Task | Refactorisation generateur en Strategy pattern | EPIC-02 | 5 | Critical | Backend |
| JAS-011 | Task | Tests snapshot artefacts generes | EPIC-02 | 3 | Medium | Backend |
| JAS-012 | Story | Interface reclassification manuelle Angular | EPIC-03 | 13 | High | Front+Back |
| JAS-013 | Story | API REST reclassification avec audit log | EPIC-03 | 5 | High | Backend |
| JAS-014 | Story | Dashboard progression migration par projet | EPIC-03 | 13 | Medium | Front+Back |
| JAS-015 | Story | API agregation metriques par projet | EPIC-03 | 8 | Medium | Backend |
| JAS-016 | Task | Composant Angular badge confiance classification | EPIC-03 | 3 | Medium | Frontend |
| JAS-017 | Story | Integration Claude API optionnelle (circuit breaker) | EPIC-04 | 8 | Medium | Backend |
| JAS-018 | Story | Nommage semantique UseCases par Claude API | EPIC-04 | 8 | Medium | Back+Front |
| JAS-019 | Story | Detection logique d'etat (state machine) | EPIC-04 | 13 | Medium | Backend |
| JAS-020 | Story | Extraction gardes d'habilitation en Policy candidates | EPIC-04 | 8 | Medium | Backend |
| JAS-021 | Task | Prompt engineering et tests corpus reference IA | EPIC-04 | 5 | Low | Backend |
| JAS-022 | Task | Configuration securisee cle API Claude | EPIC-04 | 2 | High | Infra+Back |
| JAS-029 | Story | Audit trail données envoyées au LLM + UI transparence | EPIC-04 | 8 | High | Back+Front |
| JAS-030 | Story | Visualisation raisonnement détaillé LLM (chain-of-thought) | EPIC-04 | 8 | High | Back+Front |
| JAS-023 | Story | Ingestion et analyse projet complet multi-controllers | EPIC-05 | 21 | Medium | Back+Front |
| JAS-024 | Story | Detection dependances inter-controllers | EPIC-05 | 13 | Medium | Back+Front |
| JAS-025 | Story | Pipeline differentiel delta analysis | EPIC-05 | 13 | Low | Back+Front |
| JAS-026 | Story | Export artefacts structure Maven/Gradle | EPIC-05 | 8 | Low | Backend |
| JAS-027 | Story | Generation squelettes tests JUnit 5 | EPIC-05 | 13 | Low | Backend |
| JAS-028 | Task | Documentation OpenAPI 3.1 nouveaux endpoints | EPIC-05 | 5 | Medium | Backend |

**Total estimé : 280 story points**

---

## Ordonnancement recommande par lot progressif

### Lot A - Stabilisation fondamentale (prerequis de tout le reste)
Tickets : JAS-005, JAS-003, JAS-010, JAS-001
Total : 15 points
Objectif : fiabiliser la base de parsing et restructurer le generateur avant d'ajouter des fonctionnalites.

### Lot B - Qualite critique de classification et d'artefacts
Tickets : JAS-002, JAS-004, JAS-006, JAS-011
Total : 27 points
Objectif : que les artefacts generes soient corrects en termes de signatures et de classification.

### Lot C - Qualite avancee des artefacts et feedback utilisateur
Tickets : JAS-007, JAS-008, JAS-009, JAS-016, JAS-022
Total : 28 points
Objectif : artefacts semantiquement corrects, composants custom geres, compilabilite validee, UI informee.

### Lot D - Reclassification manuelle et metriques projet
Tickets : JAS-013, JAS-012, JAS-015, JAS-014
Total : 39 points
Objectif : l'utilisateur peut corriger l'outil et voir la progression globale.

### Lot E - Enrichissement IA
Tickets : JAS-017, JAS-022 (si pas fait), JAS-021, JAS-018, JAS-019, JAS-020, JAS-029, JAS-030
Total : 60 points
Objectif : le LLM enrichit les noms et detecte les patterns avances. Chaque appel LLM est auditable et son raisonnement visible dans l'UI.

### Lot F - Scalabilite et integration
Tickets : JAS-023, JAS-024, JAS-027, JAS-028, JAS-025, JAS-026
Total : 73 points
Objectif : l'outil passe a l'echelle sur un projet complet avec export IDE.

---

## Handoff inter-agents

```text
handoff:
  vers: gouvernance
  preconditions:
    - Backlog valide par product-owner-fonctionnel
    - Risques R-01 a R-08 arbitres et mitigation assignee
    - Lot A confirme comme prerequis non negociable
  points_de_vigilance:
    - JAS-010 (refactorisation Strategy) doit etre merge avant JAS-006 et JAS-007
    - JAS-017 (Claude API) necessite une decision securite sur les donnees envoyees au LLM — couverte par JAS-029 (audit trail) et JAS-030 (chain-of-thought)
    - JAS-023 (multi-controllers) necessite une decision sur le stockage des ZIP uploadés
    - La version Angular cible (19 ou 21.x) doit etre revalidee avant JAS-012 et JAS-014
  artefacts_a_consulter:
    - jira/refactoring-app-initial-backlog.md (ce document)
    - agents/contracts.md
    - agents/orchestration.md
    - guide_generique_refactoring_controller_javafx_spring.md

handoff:
  vers: backend-hexagonal
  preconditions:
    - Lot A complete (JAS-005, JAS-003, JAS-010, JAS-001)
    - Contrats API valides pour les nouveaux endpoints
  points_de_vigilance:
    - Le port AiEnrichmentPort (JAS-017) doit rester dans ports.out sans dependance Spring
    - Les migrations Flyway pour rule_classification_audit et project doivent etre sequentielles
    - Les generateurs Strategy (JAS-010) doivent etre enregistres via List<ArtifactGenerator> Spring
  artefacts_a_consulter:
    - backend/pom.xml
    - jira/refactoring-app-initial-backlog.md

handoff:
  vers: frontend-angular
  preconditions:
    - JAS-001 complete (parsingMode disponible dans l'API)
    - JAS-013 complete (API reclassification disponible)
    - Version Angular revalidee
  points_de_vigilance:
    - Tous les composants en ChangeDetectionStrategy.OnPush
    - Utiliser les signals Angular 19 pour l'etat des formulaires de reclassification
    - Le graphe de dependances (JAS-024) ne doit pas introduire de dependance non validee (D3.js a confirmer)
  artefacts_a_consulter:
    - frontend/ (structure existante)
    - jira/refactoring-app-initial-backlog.md

handoff:
  vers: implementation-moteur-analyse
  preconditions:
    - JAS-010 complete (architecture Strategy du generateur)
    - JAS-002 complete (dictionnaire YAML configurable)
  points_de_vigilance:
    - La detection des state machines (JAS-019) doit avoir un seuil de confiance configurable
    - L'extraction des Policy candidates (JAS-020) doit exclure les methodes purement UI
    - Le fallback regex enrichi (JAS-002) ne doit pas casser le mode AST
  artefacts_a_consulter:
    - backend/src/ (moteur actuel)
    - samples/ (corpus de test)
    - jira/refactoring-app-initial-backlog.md
```

---

## Annexe - Contrat de sortie de l'agent jira-estimation

### objectif
Transformer les 10 limites et 5 ameliorations identifiees en backlog JIRA structuré, estime et ordonnance.

### perimetre
28 tickets couvrant les 5 epics definis. Exclus : la refonte de l'architecture backend existante, les migrations de donnees de production, la securite des acces utilisateurs (hors perimetre non exprime dans le scope initial).

### faits
- 10 limites documentees avec exemples concrets (ExamenParacliniqueController, 12/15 handlers en Bridge)
- 5 ameliorations identifiees (A a E)
- Stack : Spring Boot 4.0.3 / Java 21 / Angular 19 / JavaParser / PostgreSQL
- Corpus de samples existant dans `samples/`
- Architecture hexagonale stricte deja en place dans `backend/`

### interpretations
- La limite 3 (signatures perdues) est la plus impactante pour la qualite des artefacts : priorite Critical
- La limite 2 (dictionnaire regex etroit) explique directement le probleme des 12/15 handlers en Bridge
- L'amelioration A (LLM) doit etre strictement optionnelle pour ne pas bloquer les lots precedents
- JAS-010 (refactorisation Strategy) est un prerequis technique non visible par l'utilisateur mais critique pour la maintenabilite

### hypotheses
- La version Angular 19 est confirmee (a revalider si upgrade vers 21.x prevu)
- JavaParser peut re-parser le code genere pour la validation de compilabilite (JAS-009)
- Claude API (Anthropic) est le LLM cible pour l'amelioration A
- Un corpus de samples adequat existe dans `samples/` pour les tests de regression

### incertitudes
- La version exacte d'Angular cible (19 mentionnee dans le prompt, 21.x dans AGENTS.md) doit etre arbitree
- Le choix de la librairie de visualisation pour le graphe de dependances (JAS-024) est ouvert
- Le stockage des ZIPs uploades pour l'analyse multi-controllers necessite une decision d'infrastructure
- Le modele Claude exact (Sonnet, Opus, Haiku) et le schema de tarification pour l'amelioration A sont a confirmer

### decisions
- Lot A defini comme prerequis non negociable (JAS-010 en premier)
- Enrichissement IA totalement optionnel avec circuit breaker Resilience4j
- Validation de compilabilite non bloquante (avertissements, pas erreurs)
- Reclassification manuelle avec audit log obligatoire

### livrables
- Ce fichier : `jira/refactoring-app-initial-backlog.md`
- 28 tickets structures avec criteres d'acceptation Given/When/Then
- 6 lots progressifs d'implementation (A a F)
- 8 risques identifies et mitigations proposees
- Handoffs vers gouvernance, backend-hexagonal, frontend-angular, implementation-moteur-analyse

### dependances
- `agents/contracts.md` respecte
- `guide_generique_refactoring_controller_javafx_spring.md` comme reference metier
- `AGENTS.md` pour les conventions d'orchestration

### verifications
- [ ] Chaque ticket a un ID unique (JAS-001 a JAS-028)
- [ ] Chaque ticket a au moins 3 criteres d'acceptation verifiables
- [ ] Les estimations sont en Fibonacci (1, 2, 3, 5, 8, 13, 21)
- [ ] Les dependances sont coherentes (pas de dependance circulaire)
- [ ] Le handoff vers les 4 agents prioritaires est documente

### handoff
Voir section "Handoff inter-agents" ci-dessus.
