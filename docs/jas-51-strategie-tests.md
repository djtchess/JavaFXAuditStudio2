# JAS-51 — Strategie de tests transverses

## objectif

Poser la strategie de tests pour JavaFXAuditStudio2 : backend hexagonal, frontend Angular et moteur d'analyse. Identifier les lacunes de testabilite et les dettes a traiter.

## perimetre

- Tests unitaires domaine : records ingestion, cartographie, classification, plan migration
- Tests unitaires service : ingestion sources, classification responsabilites
- Tests integration existants : workbench controller (WorkbenchControllerIT)
- Strategie frontend : dashboard Angular
- Lacunes de testabilite transverses

## faits

- Stack backend : JDK 21, Spring Boot 4.0.3, JUnit 5, AssertJ
- Les records de domaine appliquent `List.copyOf` dans leur constructeur compact pour garantir l'immutabilite
- `IngestionResult`, `ControllerCartography`, `ClassificationResult`, `MigrationPlan` exposent des methodes derivees testables directement (`hasErrors`, `hasUnknowns`, `hasUncertainties`, `lotByNumber`)
- `PlannedLot` valide que `lotNumber` est dans [1, 5] et leve `IllegalArgumentException` sinon
- `MigrationPlan` trie les lots par `lotNumber` dans le constructeur compact
- `IngestSourcesService` et `ClassifyResponsibilitiesService` dependent de ports fonctionnels substituables par lambda
- Le test existant `GetWorkbenchOverviewServiceTest` utilise "GPT-5.4" dans une assertion — dette identifiee
- Aucun framework de mock (Mockito) n'est utilise dans les tests unitaires existants

## interpretations

- L'architecture hexagonale facilite les tests unitaires purs : les services ne dependent que de ports (interfaces), remplacables par lambdas ou classes internes anonymes
- La presence de stubs (`StubCartographyAnalysisAdapter`, `StubMigrationPlannerAdapter`, `StubRuleExtractionAdapter`, `StubCodeGenerationAdapter`) indique que les adapters d'analyse reelle ne sont pas encore implementes — les tests de service dependent donc de comportements fictifs stables
- L'absence de `@SpringBootTest` dans les tests unitaires est un choix delibere conforme a l'architecture : aucune dependance Spring dans `domain` ou `application`

## hypotheses

- Le frontend utilise Angular 21.x avec Vitest comme runner de tests unitaires et jsdom comme environnement DOM
- Les composants Angular du dashboard suivent une separation claire entre le composant de presentation et le service HTTP
- Les stubs actuels seront remplaces par des adapters reels lors des lots d'implementation moteur

## incertitudes

- La version Angular cible n'a pas ete revalidee (cf. JAS-22)
- Le framework de test frontend (Vitest vs Jest vs Karma) n'est pas encore confirme dans le projet
- L'adapter `FilesystemSourceReaderAdapter` n'est pas couvert — sa testabilite sans acces au systeme de fichiers reel est non resolue
- La configuration des tests d'integration (`WorkbenchControllerIT`) avec Spring Boot 4.0.3 utilise `@SpringBootTest` — compatibilite avec les changements de Spring Boot 4.x a verifier

## decisions

- Tests unitaires domaine : instanciation directe, pas de Spring, pas de Mockito
- Ports remplacables par lambdas quand l'interface est fonctionnelle (un seul methode abstraite)
- Nommage uniforme : `methodName_expectedBehavior_whenCondition`
- Variables locales declarees avant usage, conformement au style de `GetWorkbenchOverviewServiceTest`
- La dette "GPT-5.4" dans `GetWorkbenchOverviewServiceTest` est conservee en l'etat et tracee comme dette technique

---

## Strategie frontend

### Environnement cible

- Runner : **Vitest**
- Environnement DOM : **jsdom**
- Framework de composants Angular : tests via `TestBed` Angular ou, pour les composants purs, isolation directe avec des doubles de services

### Cas a couvrir en priorite : dashboard workbench

Le dashboard est le point d'entree principal de l'application. Trois etats critiques doivent etre couverts :

#### Etat chargement (loading)

```
Etant donne que le service HTTP n'a pas encore repondu
Quand le composant est initialise
Alors un indicateur de chargement est visible
Et aucune donnee de workbench n'est affichee
```

Test suggere : injecter un service stub dont `getOverview()` retourne un `Observable` qui ne complete pas immediatement. Verifier la presence du selecteur de chargement dans le DOM.

#### Etat erreur (error)

```
Etant donne que le service HTTP retourne une erreur HTTP 500
Quand le composant reçoit l'erreur
Alors un message d'erreur est affiche
Et l'indicateur de chargement est masque
```

Test suggere : injecter un service stub dont `getOverview()` retourne `throwError(() => new HttpErrorResponse({ status: 500 }))`. Verifier le message d'erreur dans le DOM.

#### Etat donnees (data)

```
Etant donne que le service HTTP retourne un WorkbenchOverviewResponse valide
Quand le composant reçoit les donnees
Alors le nom du produit est affiche
Et la liste des lots de refactoring est rendue
Et le nombre d'agents est correct
```

Test suggere : injecter un service stub dont `getOverview()` retourne `of(fixtureWorkbenchOverview)`. Verifier les selecteurs de contenu dans le DOM.

### Fixture de reference

```typescript
export const fixtureWorkbenchOverview = {
  productName: 'JavaFXAuditStudio',
  summary: 'Outil de refactoring progressif de controllers JavaFX',
  frontendVersion: 'Angular 21.x',
  backendVersion: 'Spring Boot 4.0.3',
  lots: [
    { lotNumber: 1, title: 'Diagnostic', objective: 'Cartographier', deliverable: 'Rapport' }
  ],
  agents: [
    { name: 'architecture-applicative', role: 'Architecture', scope: 'Pilotage', preferredModel: 'claude-opus' }
  ]
};
```

### Autres composants a couvrir (priorite secondaire)

- Composant liste des agents : rendu conditionnel selon le champ `preferredModel`
- Composant liste des lots : tri par numero de lot, affichage du deliverable
- Pipe ou transformer de statut d'analyse : `AnalysisStatus` -> libelle lisible

---

## Lacunes de testabilite

### 1. FilesystemSourceReaderAdapter non testable sans filesystem

**Localisation** : `adapters/out/ingestion/FilesystemSourceReaderAdapter.java`

**Probleme** : L'adapter lit directement le systeme de fichiers via `java.nio.file.Files`. Aucun test unitaire ne peut le couvrir sans acces au filesystem reel ou a un filesystem en memoire (ex. Jimfs).

**Risque** : Chemin non couvert en cas d'erreur de lecture, d'encodage non UTF-8 ou de fichier vide.

**Resolution recommandee** : Introduire une abstraction `FileSystemPort` (ou `PathReader`) injectable, implementee par l'adapter reel et remplacable par un double de test. Alternativement, utiliser Jimfs dans un test d'integration leger sans Spring.

### 2. Stubs d'analyse a remplacer par des adapters testables

**Localisation** :
- `adapters/out/analysis/StubCartographyAnalysisAdapter.java`
- `adapters/out/analysis/StubMigrationPlannerAdapter.java`
- `adapters/out/analysis/StubRuleExtractionAdapter.java`
- `adapters/out/analysis/StubCodeGenerationAdapter.java`

**Probleme** : Ces stubs retournent des donnees fictives codees en dur. Ils ne sont pas des doubles de test controlables depuis les tests — ils sont des implementations de production provisoires.

**Risque** : Quand les vrais adapters seront implementes, les services en aval n'auront pas de tests avec des doubles controlables. Les regressions comportementales ne seront pas detectees.

**Resolution recommandee** : Creer des doubles de test parametrables (classes internes de test ou lambdas) pour chaque port, distincts des stubs de production.

### 3. Dette "GPT-5.4" dans GetWorkbenchOverviewServiceTest

**Localisation** : `application/service/GetWorkbenchOverviewServiceTest.java`, ligne 35

**Probleme** : L'assertion `containsExactly("GPT-5.4")` encode en dur un nom de modele obsolete (GPT-5.4 n'est pas le modele cible du projet — Claude est utilise).

**Risque** : Le test passe mais valide un comportement incorrect au regard de la gouvernance du projet (AGENTS.md precise Claude opus/sonnet/haiku).

**Resolution recommandee** : Aligner la valeur de `preferredModel` dans le test avec les noms de modeles reels du catalogue (`claude-opus`, `claude-sonnet`). Tracker comme dette technique JAS.

### 4. WorkbenchControllerIT dependant du contexte Spring complet

**Localisation** : `adapters/in/rest/WorkbenchControllerIT.java`

**Probleme** : Le test d'integration charge le contexte Spring complet avec `@SpringBootTest`. En Spring Boot 4.x, des changements de comportement des filtres de securite ou des auto-configurations peuvent casser ce test sans regression fonctionnelle.

**Risque** : Fragilite du test face aux evolutions de configuration Spring Boot 4.

**Resolution recommandee** : Envisager une slice `@WebMvcTest` pour les tests de controller, en isolant la couche REST du reste du contexte. Conserver `@SpringBootTest` uniquement pour les tests de smoke end-to-end.

### 5. Absence de tests pour les mappers REST

**Localisation** : `adapters/in/rest/mapper/WorkbenchOverviewResponseMapper.java`

**Probleme** : Le mapper traduit les objets de domaine en DTOs REST. Aucun test unitaire ne verifie que tous les champs sont correctement mappe, que les listes nulles sont gerees ou que les types sont preserves.

**Risque** : Regression silencieuse lors d'ajout de champs dans le domaine ou le DTO.

**Resolution recommandee** : Ajouter `WorkbenchOverviewResponseMapperTest` avec une fixture domaine complete et une assertion sur chaque champ du DTO de sortie.

### 6. Absence de tests pour les filtres HTTP

**Localisation** :
- `adapters/in/rest/CorrelationFilter.java`
- `adapters/in/rest/SecurityHeadersFilter.java`

**Probleme** : Ces filtres ne sont pas testes unitairement. Leur comportement (ajout d'en-tetes, correlation ID) n'est verifie que de facon implicite via `WorkbenchControllerIT`.

**Risque** : Un filtre mal configure ne serait pas detecte avant integration.

**Resolution recommandee** : Tester chaque filtre avec `MockHttpServletRequest` / `MockHttpServletResponse` de Spring Test, sans charger le contexte complet.

---

## livrables

- `/backend/src/test/java/ff/ss/javaFxAuditStudio/domain/ingestion/IngestionResultTest.java`
- `/backend/src/test/java/ff/ss/javaFxAuditStudio/domain/cartography/ControllerCartographyTest.java`
- `/backend/src/test/java/ff/ss/javaFxAuditStudio/domain/rules/ClassificationResultTest.java`
- `/backend/src/test/java/ff/ss/javaFxAuditStudio/domain/migration/MigrationPlanTest.java`
- `/backend/src/test/java/ff/ss/javaFxAuditStudio/application/service/IngestSourcesServiceTest.java`
- `/backend/src/test/java/ff/ss/javaFxAuditStudio/application/service/ClassifyResponsibilitiesServiceTest.java`
- `/docs/jas-51-strategie-tests.md` (ce document)

## dependances

- Tests existants conserves sans modification : `JavaFxAuditStudioApplicationTests`, `GetWorkbenchOverviewServiceTest`, `WorkbenchControllerIT`
- Les tests de service dependent des ports definis dans `application/ports/out/`
- La strategie frontend depend de la confirmation de la version Angular cible (JAS-22)

## verifications

- [ ] Tous les nouveaux tests compilent sans dependance Spring dans les packages `domain` et `application/service`
- [ ] Aucun import Mockito dans les nouveaux fichiers de test
- [ ] Les noms de methodes respectent la convention `methodName_expectedBehavior_whenCondition`
- [ ] Les variables locales sont declarees avant usage
- [ ] `mvn test` passe en incluant les nouveaux tests

## handoff

```text
handoff:
  vers: qa-backend
  preconditions:
    - Les 6 nouveaux fichiers de test sont crees et compilables
    - La strategie de tests est documentee dans docs/jas-51-strategie-tests.md
  points_de_vigilance:
    - Verifier que Spring Boot 4.0.3 ne casse pas WorkbenchControllerIT (slice WebMvcTest recommandee)
    - La dette "GPT-5.4" dans GetWorkbenchOverviewServiceTest doit etre traitee dans un lot dedie
    - FilesystemSourceReaderAdapter n'a aucun test — risque eleve avant les lots moteur
  artefacts_a_consulter:
    - backend/src/test/java/ff/ss/javaFxAuditStudio/domain/
    - backend/src/test/java/ff/ss/javaFxAuditStudio/application/service/
    - docs/jas-51-strategie-tests.md

handoff:
  vers: qa-frontend
  preconditions:
    - La strategie frontend est documentee dans ce document (section "Strategie frontend")
    - La version Angular cible est confirmee (JAS-22)
  points_de_vigilance:
    - Confirmer le runner de test frontend (Vitest vs Jest vs Karma)
    - Les fixtures de reference doivent etre alignees avec le contrat API valide (docs/jas-02-contrat-api.md)
  artefacts_a_consulter:
    - docs/jas-51-strategie-tests.md (section strategie frontend)
    - docs/jas-02-contrat-api.md

handoff:
  vers: devops-ci-cd
  preconditions:
    - Les tests backend compilent et passent
  points_de_vigilance:
    - Integrer les nouveaux packages de test dans le rapport de couverture
    - Separer les tests unitaires (domaine, service) des tests d'integration (WorkbenchControllerIT)
      dans le pipeline CI pour un feedback plus rapide
  artefacts_a_consulter:
    - backend/pom.xml
    - docs/jas-51-strategie-tests.md

handoff:
  vers: revue-code
  preconditions:
    - Tous les nouveaux fichiers de test sont crees
  points_de_vigilance:
    - Verifier l'absence d'import Spring dans les tests domaine
    - Verifier la conformite du nommage avec la convention du projet
    - Verifier que les lacunes listees sont bien traitees en tant que stories de backlog
  artefacts_a_consulter:
    - backend/src/test/java/ff/ss/javaFxAuditStudio/domain/
    - backend/src/test/java/ff/ss/javaFxAuditStudio/application/service/
    - docs/jas-51-strategie-tests.md
```
