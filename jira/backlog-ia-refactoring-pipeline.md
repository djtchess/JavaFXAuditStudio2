# Backlog IA Refactoring Pipeline -- JavaFXAuditStudio2

Date de reference : 2026-03-26
Agent : `jira-estimation`
Modele : `opus`

---

## Contrat inter-agent (structure contracts.md)

### objectif

Transformer le scope d'amelioration du pipeline IA de refactorisation (enrichissement,
generation, review) en epics, stories et taches estimables, structures en lots progressifs
executables. Couvrir les 6 axes : robustesse, observabilite, qualite du code genere,
experience utilisateur, persistance/versioning, enrichissement contextuel.

### perimetre

Prefixe de numerotation : IAP (IA Pipeline).
Tickets IAP-1 a IAP-32 repartis en 6 epics (A a F).
Perimetre : les 3 services IA (`EnrichAnalysisService`, `AiSpringBootGenerationService`,
`ReviewArtifactsService`), les adapters IA (`adapters/out/ai/*`), les generateurs
template (`adapters/out/analysis/generators/*`), le parser de reponse LLM
(`LlmResponseParser`), la configuration (`AiEnrichmentProperties`, `AiEnrichmentOrchestraConfiguration`),
les prompts Mustache (`resources/prompts/`), et les composants Angular associes.

Hors perimetre : le module de sanitisation (couvert par `backlog-sanitisation-audit.md`),
l'analyse statique et la classification (pipeline amont stable).

### faits

- 3 services applicatifs IA partagent du code duplique : `buildBundle()`, `readSourceFile()`, `formatRules()`.
- `readSourceFile()` fait du I/O direct via `Files.readString(Path.of(filePath))` dans la couche service.
- `LlmResponseParser` accepte silencieusement toute reponse JSON invalide via fallback texte brut.
- `provider` et `taskType` sont des `String` partout, sans enum ni validation.
- L'estimation de tokens est `source.length() / 4` sans comptage reel.
- `max_tokens=1024` est hardcode dans les adapters HTTP (insuffisant pour la generation multi-classes).
- Le routage dans `RoutingAiEnrichmentAdapter` utilise des comparaisons de String sans enum.
- Aucune metrique Micrometer n'est exposee pour les appels LLM.
- Aucun retry avec backoff n'existe ; seul le circuit breaker protege les appels.
- Le frontend n'offre ni streaming, ni diff template/IA, ni raffinement interactif.
- Les artefacts IA generes ne sont pas persistes en base.
- Les reclassifications manuelles ne sont pas injectees comme feedback dans le pipeline IA.

### interpretations

- L'epic A (robustesse) est le socle technique : le code duplique, les types non-typesafe
  et l'absence de validation des reponses LLM fragilisent toute evolution ulterieure.
- L'epic B (observabilite) est independant de A mais critique pour le pilotage en production.
- Les epics C et D dependent de A pour la stabilite du pipeline.
- L'epic E (persistance) est un prerequis de F (apprentissage contextuel).
- L'epic F est le plus a forte valeur metier mais aussi le plus risque.

### hypotheses

- Spring Boot Actuator et Micrometer sont disponibles dans le `pom.xml` existant.
- SSE (Server-Sent Events) est supporte par Spring Boot 4.0.3 pour le streaming.
- Les providers LLM (Claude, OpenAI) supportent le streaming via leur API REST.
- Le schema PostgreSQL peut etre etendu pour persister les artefacts IA generes.
- Angular 21.x supporte les EventSource natifs pour SSE.

### incertitudes

- La compatibilite exacte de Micrometer avec Spring Boot 4.0.3 doit etre verifiee.
- Le support du streaming cote Claude Code CLI n'est pas confirme.
- La taille maximale des reponses LLM avant OOM n'est pas mesuree.
- Le volume de tokens reel pour une generation multi-classes n'est pas calibre.

### decisions

- Numerotation : prefixe IAP (IA Pipeline), IAP-1 a IAP-32.
- Estimation : suite Fibonacci (1, 2, 3, 5, 8, 13).
- Priorites : P0 (bloquant production), P1 (important), P2 (amelioration), P3 (nice-to-have).
- Les epics sont ordonnes par dependances techniques, pas par valeur metier seule.
- `taskType` et `provider` seront convertis en enums dans le domaine.

### livrables

- `jira/backlog-ia-refactoring-pipeline.md` (le present document)

### dependances

- `backlog-sanitisation-audit.md` : les stories de sanitisation ne sont pas dupliquees ici.
- `backlog-phase-2.md` : le pipeline d'analyse statique est considere comme stable.
- `agents/contracts.md` : structure de sortie inter-agent respectee.

### verifications

- Chaque ticket a : ID, titre, epic, description, criteres d'acceptation, SP, dependances, risques, approche technique.
- Les dependances inter-tickets sont coherentes.
- Aucun ticket ne viole l'architecture hexagonale.
- Le total SP est dans la fourchette 120-170 pour 32 stories.

### handoff

```text
handoff:
  vers: gouvernance
  preconditions:
    - backlog-ia-refactoring-pipeline.md produit et lisible
    - dependances inter-tickets verifiees
    - coherence avec backlog-sanitisation-audit.md confirmee
  points_de_vigilance:
    - IAP-1 (extraction utilitaire) impacte 3 services applicatifs simultanement
    - IAP-10 (streaming SSE) necessite validation archi frontend + backend
    - IAP-25 (persistance artefacts) modifie le schema PostgreSQL
    - IAP-30 (feedback reclassification) cree un couplage entre pipeline manuel et IA
  artefacts_a_consulter:
    - jira/backlog-ia-refactoring-pipeline.md
    - agents/contracts.md
    - backend/src/main/java/ff/ss/javaFxAuditStudio/application/service/
    - backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/ai/
```

---

## Resume des epics et estimation macro

| Epic | Tickets    | Titre                                        | SP total |
|------|------------|----------------------------------------------|----------|
| A    | IAP-1..8   | Robustesse et qualite du pipeline IA         | 34       |
| B    | IAP-9..13  | Observabilite et tracabilite IA              | 21       |
| C    | IAP-14..19 | Amelioration de la qualite du code genere    | 29       |
| D    | IAP-20..24 | Experience utilisateur IA                    | 26       |
| E    | IAP-25..28 | Persistance et versioning des artefacts IA   | 21       |
| F    | IAP-29..32 | Enrichissement contextuel et apprentissage   | 21       |
| **Total** | **32 tickets** |                                     | **152**  |

---

## Tableau synthetique

| ID | Epic | Titre | Description | Priorite | SP | Dependances | Tags |
|----|------|-------|-------------|----------|----|-------------|------|
| IAP-1 | A | Extraction utilitaire LLM commun | Extraire `buildBundle`, `readSourceFile`, `formatRules` dans `LlmServiceSupport` | P0 | 5 | - | refactoring, backend |
| IAP-2 | A | Enums `TaskType` et `LlmProvider` dans le domaine | Remplacer les String par des enums typesafe | P0 | 3 | - | domaine, typesafety |
| IAP-3 | A | Validation schema reponse LLM | Valider la structure JSON des reponses LLM avant traitement | P0 | 5 | - | robustesse, backend |
| IAP-4 | A | Retry avec backoff exponentiel | Ajouter une politique de retry configurable aux appels LLM | P0 | 5 | IAP-1 | resilience, backend |
| IAP-5 | A | Port de lecture source fichier | Deplacer le I/O fichier hors de la couche service vers un port | P1 | 3 | IAP-1 | hexagonal, backend |
| IAP-6 | A | Configuration dynamique `max_tokens` | Rendre `max_tokens` configurable par type de tache IA | P1 | 3 | IAP-2 | config, backend |
| IAP-7 | A | Limite de taille reponse LLM | Limiter la taille des reponses LLM acceptees (anti-OOM) | P1 | 3 | IAP-3 | securite, backend |
| IAP-8 | A | Elimination des constantes magiques | Extraire `length/4`, `31`, `1024` dans la configuration | P2 | 2 | IAP-6 | cleanup, backend |
| IAP-9 | B | Metriques Micrometer appels LLM | Compteurs succes/echec, latence, tokens par provider et task | P0 | 5 | - | observabilite, backend |
| IAP-10 | B | Tracing distribue X-Request-ID | Propager le requestId aux headers des appels LLM | P1 | 3 | - | tracing, backend |
| IAP-11 | B | Dashboard sante pipeline IA | Endpoint Actuator + widget Angular de sante du pipeline IA | P1 | 5 | IAP-9 | observabilite, fullstack |
| IAP-12 | B | Comptage reel de tokens | Remplacer `length/4` par un estimateur calibre (cl100k_base) | P2 | 5 | - | precision, backend |
| IAP-13 | B | Alerting Semgrep defaillances | Logger et compter les echecs Semgrep au lieu de les ignorer | P2 | 3 | - | observabilite, backend |
| IAP-14 | C | Prompt UseCase avec corps logique | Enrichir le prompt pour generer des methodes avec logique metier | P1 | 5 | IAP-6 | qualite-code, prompts |
| IAP-15 | C | Prompt Policy avec regles reelles | Remplacer `UnsupportedOperationException` par des regles deduites | P1 | 5 | IAP-14 | qualite-code, prompts |
| IAP-16 | C | Prompt Assembler type | Generer des Assemblers avec types reels au lieu de Object->Object | P1 | 5 | IAP-14 | qualite-code, prompts |
| IAP-17 | C | Prompt tests avec assertions | Generer des tests avec assertions significatives, pas que des stubs | P1 | 5 | IAP-14 | qualite-code, prompts |
| IAP-18 | C | Gestion d'erreurs dans artefacts | Ajouter try-catch, validation, et exceptions metier dans le code genere | P2 | 5 | IAP-14 | qualite-code, prompts |
| IAP-19 | C | Feature flags par type d'artefact | Permettre d'activer/desactiver la generation IA par type d'artefact | P2 | 3 | IAP-2 | config, backend |
| IAP-20 | D | Streaming SSE reponses LLM | Streamer les reponses LLM en temps reel via SSE | P1 | 8 | IAP-4 | ux, fullstack |
| IAP-21 | D | Vue diff template vs IA | Composant Angular de comparaison cote-a-cote template/IA | P1 | 5 | - | ux, frontend |
| IAP-22 | D | Raffinement interactif multi-tour | Permettre a l'utilisateur de demander des corrections sur un artefact | P2 | 8 | IAP-20 | ux, fullstack |
| IAP-23 | D | Feedback visuel progression generation | Barre de progression avec etapes pendant la generation IA | P2 | 3 | IAP-20 | ux, frontend |
| IAP-24 | D | Copier/telecharger artefact individuel | Boutons copier et telecharger par artefact dans le frontend | P2 | 2 | - | ux, frontend |
| IAP-25 | E | Schema et entite persistance artefacts IA | Migration Flyway + entite JPA pour les artefacts generes | P1 | 5 | - | persistance, backend |
| IAP-26 | E | Service de sauvegarde artefacts IA | Use case et adapter JPA pour persister les generations | P1 | 5 | IAP-25 | persistance, backend |
| IAP-27 | E | Historique et versioning des artefacts | Sauvegarder plusieurs versions par artefact, comparer | P2 | 8 | IAP-26 | persistance, backend |
| IAP-28 | E | Export ZIP des artefacts generes | Endpoint REST + bouton Angular d'export groupé en ZIP | P2 | 3 | IAP-26 | export, fullstack |
| IAP-29 | F | Contexte ecran complet pour le LLM | Envoyer toutes les regles de l'ecran (pas juste le controller) au LLM | P1 | 5 | IAP-1 | contexte, backend |
| IAP-30 | F | Injection feedback reclassification | Utiliser les reclassifications manuelles comme signal pour le LLM | P2 | 5 | IAP-29 | apprentissage, backend |
| IAP-31 | F | Verification coherence inter-artefacts | LLM valide la coherence entre les artefacts generes d'un meme ecran | P2 | 8 | IAP-29, IAP-26 | qualite, backend |
| IAP-32 | F | Apprentissage patterns projet cible | Memoriser les patterns valides du projet pour guider les futures generations | P3 | 3 | IAP-30, IAP-27 | apprentissage, backend |

---

## EPIC A -- Robustesse et qualite du pipeline IA

**Lead agent** : `backend-hexagonal`
**Objectif** : Corriger les gaps techniques critiques qui fragilisent le pipeline IA existant.
**SP total** : 34
**Dependances de l'epic** : Aucune (fondation)

---

### IAP-1 -- Extraction utilitaire LLM commun

**Epic** : A
**Priorite** : P0
**Description** :
Les 3 services IA (`EnrichAnalysisService`, `AiSpringBootGenerationService`,
`ReviewArtifactsService`) dupliquent `buildBundle()`, `readSourceFile()` et `formatRules()`.
Extraire ces methodes dans une classe utilitaire `LlmServiceSupport` dans `application/service/`
ou dans un port dedie.

**Approche technique** : Creer `LlmServiceSupport` avec les 3 methodes extraites. Injecter
dans les 3 services via constructeur. Supprimer les methodes dupliquees.

**Criteres d'acceptation** :
- CA-1 : Les 3 services delegent a `LlmServiceSupport` pour `buildBundle`, `readSourceFile`, `formatRules`.
- CA-2 : Aucune duplication de ces methodes dans les services.
- CA-3 : `LlmServiceSupport` a ses propres tests unitaires (>= 5 cas).
- CA-4 : `mvn test` passe sans regression.
- CA-5 : Les signatures des 3 use cases publics ne changent pas.

**Story points** : 5
**Dependances** : Aucune
**Risques** : Moyen -- 3 services modifies simultanement. Risque de regression si les comportements
de fallback (I/O, sanitisation absente) ne sont pas strictement preserves.
**Agent lead** : `backend-hexagonal`

---

### IAP-2 -- Enums `TaskType` et `LlmProvider` dans le domaine

**Epic** : A
**Priorite** : P0
**Description** :
`taskType` est un `String` libre (`"ARTIFACT_REVIEW"`, `"SPRING_BOOT_GENERATION"`, etc.)
et `provider` est un `String` (`"claude-code"`, `"openai-gpt54"`, `"claude-code-cli"`).
Ces valeurs doivent etre des enums du domaine pour la typesafety et la validation.

**Approche technique** : Creer `TaskType` et `LlmProvider` dans `domain/ai/`. Mettre a jour
`AiEnrichmentRequest`, `AiEnrichmentProperties`, `RoutingAiEnrichmentAdapter` et les services.

**Criteres d'acceptation** :
- CA-1 : `TaskType` enum existe dans `domain/ai/` avec toutes les valeurs actuelles.
- CA-2 : `LlmProvider` enum existe dans `domain/ai/` avec `CLAUDE_CODE`, `OPENAI_GPT54`, `CLAUDE_CODE_CLI`.
- CA-3 : `RoutingAiEnrichmentAdapter.route()` utilise un `switch` sur l'enum au lieu de `String.equals()`.
- CA-4 : La deserialisation Spring des properties fonctionne avec les enums.
- CA-5 : `mvn test` passe.

**Story points** : 3
**Dependances** : Aucune
**Risques** : Faible -- changement structurel mais localize. Risque de casse de la configuration
`application.properties` si les noms enum ne correspondent pas aux valeurs existantes.
**Agent lead** : `backend-hexagonal`

---

### IAP-3 -- Validation schema reponse LLM

**Epic** : A
**Priorite** : P0
**Description** :
`LlmResponseParser` accepte silencieusement toute reponse malformee via fallback texte brut.
Cela masque les erreurs de prompt et les reponses tronquees. Le parser doit valider la structure
attendue et signaler explicitement les reponses invalides.

**Approche technique** : Ajouter une validation stricte du schema JSON (presence de `"suggestions"`,
types corrects). Logger un warning en cas de fallback. Ajouter un compteur Micrometer
`llm.response.parse.fallback` pour detecter les degradations silencieuses.

**Criteres d'acceptation** :
- CA-1 : `LlmResponseParser` valide la presence de la cle `"suggestions"` dans le JSON.
- CA-2 : En cas de fallback texte brut, un `WARN` est emis avec le `requestId`.
- CA-3 : Les reponses tronquees (JSON incomplet) sont detectees et signalees.
- CA-4 : Un test couvre les cas : JSON valide, JSON sans `suggestions`, JSON tronque, texte brut, null.
- CA-5 : `mvn test` passe.

**Story points** : 5
**Dependances** : Aucune
**Risques** : Moyen -- les prompts actuels pourraient produire des reponses que le nouveau
validateur rejette. Necessaire de tester avec des reponses reelles de chaque provider.
**Agent lead** : `backend-hexagonal`

---

### IAP-4 -- Retry avec backoff exponentiel

**Epic** : A
**Priorite** : P0
**Description** :
Les appels LLM n'ont aucun retry. Le circuit breaker protege contre les pannes prolongees
mais pas contre les erreurs transitoires (timeout ponctuel, rate limit 429). Ajouter une
politique de retry configurable avec backoff exponentiel.

**Approche technique** : Ajouter un `RetryPolicy` dans `adapters/out/ai/` configurable
via `AiEnrichmentProperties` (`maxRetries`, `initialBackoffMs`, `multiplier`).
Integrer dans `RoutingAiEnrichmentAdapter` avant le circuit breaker.

**Criteres d'acceptation** :
- CA-1 : Les appels LLM sont reessayes jusqu'a `maxRetries` fois (defaut 2) avec backoff.
- CA-2 : Les erreurs 429 (rate limit) et 503 (service unavailable) declenchent un retry.
- CA-3 : Les erreurs 400 (bad request) ne declenchent pas de retry.
- CA-4 : Le backoff double a chaque tentative (defaut 500ms, 1000ms, 2000ms).
- CA-5 : La configuration est externalisable dans `application.properties`.

**Story points** : 5
**Dependances** : IAP-1 (pour ne pas dupliquer la logique de retry dans 3 services)
**Risques** : Moyen -- le retry peut augmenter la latence percue et le cout tokens.
Necessaire de plafonner le nombre de retries et le timeout total.
**Agent lead** : `backend-hexagonal`

---

### IAP-5 -- Port de lecture source fichier

**Epic** : A
**Priorite** : P1
**Description** :
`readSourceFile()` fait du `Files.readString(Path.of(filePath))` directement dans la couche
service applicatif. Cela viole l'architecture hexagonale (I/O dans `application`).
Extraire vers un port `SourceFileReaderPort` dans `application/ports/out/`.

**Approche technique** : Creer `SourceFileReaderPort` avec `Optional<String> read(String path)`.
Implementer dans `adapters/out/` via `FilesystemSourceFileReader`. Injecter dans
`LlmServiceSupport` (IAP-1).

**Criteres d'acceptation** :
- CA-1 : `SourceFileReaderPort` existe dans `application/ports/out/`.
- CA-2 : Aucun appel `Files.*` dans `application/service/`.
- CA-3 : Le fallback (chemin invalide -> nom du controller) est preserve dans l'adapter.
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dependances** : IAP-1
**Risques** : Faible -- refactoring d'extraction sans changement de comportement.
**Agent lead** : `backend-hexagonal`

---

### IAP-6 -- Configuration dynamique `max_tokens`

**Epic** : A
**Priorite** : P1
**Description** :
`max_tokens=1024` est hardcode dans les adapters HTTP LLM. Ce plafond est trop bas pour
generer plusieurs classes Java completes. La valeur doit etre configurable par type de tache.

**Approche technique** : Ajouter dans `AiEnrichmentProperties` un map
`maxTokensByTask` (cle = `TaskType`, valeur = int). Defaut : `ENRICHMENT=1024`,
`SPRING_BOOT_GENERATION=4096`, `ARTIFACT_REVIEW=2048`. Les adapters lisent la config.

**Criteres d'acceptation** :
- CA-1 : `max_tokens` est configurable dans `application.properties` par type de tache.
- CA-2 : Les adapters HTTP utilisent la valeur configuree au lieu du hardcode.
- CA-3 : Un defaut raisonnable est applique si la config est absente.
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dependances** : IAP-2 (pour `TaskType` enum)
**Risques** : Faible -- les providers ont leurs propres limites de tokens qui seront respectees.
**Agent lead** : `backend-hexagonal`

---

### IAP-7 -- Limite de taille reponse LLM

**Epic** : A
**Priorite** : P1
**Description** :
Aucune limite n'est imposee sur la taille des reponses LLM acceptees. Une reponse
anormalement grande pourrait causer un OOM. Ajouter un plafond configurable.

**Approche technique** : Ajouter `maxResponseSizeBytes` dans `AiEnrichmentProperties`
(defaut 512 KB). Les adapters HTTP tronquent la reponse si elle depasse et signalent
un mode degrade.

**Criteres d'acceptation** :
- CA-1 : Les reponses LLM depassant `maxResponseSizeBytes` sont tronquees.
- CA-2 : Une reponse tronquee est signalee comme degradee avec un message explicite.
- CA-3 : La limite est configurable dans `application.properties`.
- CA-4 : Un test verifie le comportement avec une reponse de taille excessive.

**Story points** : 3
**Dependances** : IAP-3
**Risques** : Faible -- protection defensive sans changement du flux nominal.
**Agent lead** : `backend-hexagonal`

---

### IAP-8 -- Elimination des constantes magiques

**Epic** : A
**Priorite** : P2
**Description** :
Les constantes `source.length() / 4` (estimation tokens), `31` (troncature nom),
`1024` (max_tokens), `200` (troncature message erreur) sont dispersees dans le code.
Les centraliser dans la configuration.

**Approche technique** : Extraire dans `AiEnrichmentProperties` ou dans des constantes
nommees explicites. Remplacer `length/4` par un appel a l'estimateur (IAP-12 si disponible,
sinon constante nommee `TOKEN_ESTIMATION_DIVISOR`).

**Criteres d'acceptation** :
- CA-1 : Aucune constante numerique magique dans les services et adapters IA.
- CA-2 : Chaque constante a un nom semantique ou est dans la configuration.
- CA-3 : `mvn test` passe.

**Story points** : 2
**Dependances** : IAP-6
**Risques** : Aucun -- refactoring cosmetique.
**Agent lead** : `backend-hexagonal`

---

## EPIC B -- Observabilite et tracabilite IA

**Lead agent** : `observabilite-exploitation`
**Objectif** : Instrumenter le pipeline IA pour le pilotage en production.
**SP total** : 21
**Dependances de l'epic** : Aucune (parallelisable avec A)

---

### IAP-9 -- Metriques Micrometer appels LLM

**Epic** : B
**Priorite** : P0
**Description** :
Aucune metrique request-level n'est exposee. Il est impossible de connaitre le taux
de succes, la latence moyenne ou la consommation de tokens sans lire les logs.

**Approche technique** : Ajouter dans `RoutingAiEnrichmentAdapter` des compteurs Micrometer :
`llm.requests.total` (tags: provider, taskType, status), `llm.requests.duration` (timer),
`llm.tokens.used` (distribution summary). Exposer via Actuator `/actuator/prometheus`.

**Criteres d'acceptation** :
- CA-1 : Le compteur `llm.requests.total` est incremente a chaque appel LLM avec tags provider/taskType/status.
- CA-2 : Le timer `llm.requests.duration` mesure la latence de chaque appel.
- CA-3 : Les metriques sont accessibles via `/actuator/prometheus`.
- CA-4 : Un test unitaire verifie l'incrementation des compteurs.
- CA-5 : `mvn test` passe.

**Story points** : 5
**Dependances** : Aucune
**Risques** : Faible -- ajout de metriques sans impact fonctionnel. Verifier la compatibilite
Micrometer / Spring Boot 4.0.3.
**Agent lead** : `observabilite-exploitation`

---

### IAP-10 -- Tracing distribue X-Request-ID

**Epic** : B
**Priorite** : P1
**Description** :
Le `requestId` genere dans les services n'est pas propage aux appels HTTP vers les providers LLM.
Cela empeche la correlation des logs cote provider.

**Approche technique** : Ajouter le header `X-Request-ID` dans les adapters HTTP
(`ClaudeCodeAiEnrichmentAdapter`, `OpenAiGpt54AiEnrichmentAdapter`). Propager le
`requestId` depuis `AiEnrichmentRequest`.

**Criteres d'acceptation** :
- CA-1 : Chaque appel HTTP au LLM inclut le header `X-Request-ID`.
- CA-2 : Le `requestId` dans les logs backend correspond a celui envoye au provider.
- CA-3 : Un test verifie la presence du header dans les requetes sortantes.

**Story points** : 3
**Dependances** : Aucune
**Risques** : Aucun -- ajout d'un header HTTP sans impact fonctionnel.
**Agent lead** : `backend-hexagonal`

---

### IAP-11 -- Dashboard sante pipeline IA

**Epic** : B
**Priorite** : P1
**Description** :
Aucun dashboard ne permet de visualiser l'etat du pipeline IA : taux de succes,
latence, circuit breaker ouvert/ferme, tokens consommes.

**Approche technique** : Backend : endpoint Actuator custom `/actuator/ai-health` aggregeant
les metriques. Frontend : widget Angular sur le dashboard projet affichant les KPIs IA.

**Criteres d'acceptation** :
- CA-1 : `/actuator/ai-health` retourne le taux de succes, la latence P95 et l'etat du circuit breaker.
- CA-2 : Un composant Angular `AiHealthWidgetComponent` affiche les KPIs.
- CA-3 : Le widget se rafraichit toutes les 30 secondes.
- CA-4 : `mvn test` et `ng test` passent.

**Story points** : 5
**Dependances** : IAP-9
**Risques** : Moyen -- necessite coordination backend/frontend.
**Agent lead** : `observabilite-exploitation` + `frontend-angular`

---

### IAP-12 -- Comptage reel de tokens

**Epic** : B
**Priorite** : P2
**Description** :
L'estimation actuelle (`source.length() / 4`) est imprecise. Pour un code Java type,
le ratio reel est plus proche de `length / 3.5` pour GPT-4, et different pour Claude.
Une estimation calibree est necessaire pour le budget tokens et les alertes.

**Approche technique** : Implementer un `TokenEstimator` dans `adapters/out/ai/` utilisant
un algorithme de tokenization simplifie (regex sur les mots/symboles Java). Calibrer
sur un corpus de 10 fichiers Java reels et comparer avec les comptages providers.

**Criteres d'acceptation** :
- CA-1 : `TokenEstimator` existe avec une methode `int estimate(String source, LlmProvider provider)`.
- CA-2 : L'ecart avec le comptage reel provider est < 15% sur le corpus de calibration.
- CA-3 : Les 3 services utilisent `TokenEstimator` au lieu de `length / 4`.
- CA-4 : Un test de calibration documente les ecarts mesures.

**Story points** : 5
**Dependances** : Aucune
**Risques** : Moyen -- la precision depend du modele de tokenization choisi. Accepter
une approximation raisonnable plutot que la precision absolue.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-13 -- Alerting Semgrep defaillances

**Epic** : B
**Priorite** : P2
**Description** :
Semgrep fonctionne en mode "best-effort" : les defaillances sont silencieusement ignorees.
En production, une defaillance Semgrep signifie que du code non-sanitise pourrait etre
envoye au LLM sans detection.

**Approche technique** : Ajouter un compteur Micrometer `semgrep.failures.total` dans
`SemgrepScanSanitizer`. Logger les echecs en `WARN` (pas en `DEBUG`). Ajouter un health
indicator Actuator pour Semgrep.

**Criteres d'acceptation** :
- CA-1 : Les echecs Semgrep sont loggues en `WARN` avec le type d'erreur.
- CA-2 : Un compteur Micrometer `semgrep.failures.total` est incremente.
- CA-3 : Un health indicator `/actuator/health` signale Semgrep `DOWN` apres N echecs consecutifs.
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dependances** : Aucune
**Risques** : Faible -- amelioration de la visibilite sans changement fonctionnel.
**Agent lead** : `observabilite-exploitation`

---

## EPIC C -- Amelioration de la qualite du code genere

**Lead agent** : `implementation-moteur-analyse`
**Objectif** : Faire generer par l'IA du code plus complet, plus correct, plus maintenable.
**SP total** : 28
**Dependances de l'epic** : IAP-6 (max_tokens configurable) pour les prompts plus longs

---

### IAP-14 -- Prompt UseCase avec corps logique

**Epic** : C
**Priorite** : P1
**Description** :
Les UseCases generes contiennent des methodes vides ou avec `// TODO`. Le prompt
doit guider le LLM a generer la logique metier deduite des regles classifiees.

**Approche technique** : Enrichir le template Mustache `spring-boot-generation` avec une
section par UseCase incluant les regles associees, les dependances inferees et un
exemple de corps de methode attendu.

**Criteres d'acceptation** :
- CA-1 : Les UseCases generes contiennent au moins une methode avec un corps logique non-vide.
- CA-2 : Les dependances (ports, services) sont injectees dans le constructeur UseCase.
- CA-3 : Le prompt est versionne dans `resources/prompts/`.
- CA-4 : Un test avec un corpus de regles echantillon verifie la qualite de la generation.

**Story points** : 5
**Dependances** : IAP-6 (max_tokens suffisant pour des corps de methode complets)
**Risques** : Moyen -- la qualite depend fortement du prompt et du modele. Iterer sur le prompt.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-15 -- Prompt Policy avec regles reelles

**Epic** : C
**Priorite** : P1
**Description** :
Les Policies generees contiennent `throw new UnsupportedOperationException()`. Le LLM
doit deduire les regles de validation depuis les regles classifiees POLICY.

**Approche technique** : Enrichir le prompt avec les regles POLICY et leur contexte
(champs valides, messages d'erreur, conditions). Fournir un exemple de Policy complete.

**Criteres d'acceptation** :
- CA-1 : Les Policies generees contiennent des conditions de validation reelles.
- CA-2 : Les messages d'erreur sont significatifs (pas de `UnsupportedOperationException`).
- CA-3 : Le prompt inclut au moins 2 exemples de Policy pour guider le LLM.
- CA-4 : Un test verifie l'absence de `UnsupportedOperationException` dans les Policies generees.

**Story points** : 5
**Dependances** : IAP-14
**Risques** : Moyen -- le LLM peut halluciner des regles inexistantes. Necessaire de
valider avec review IA (endpoint `/review`).
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-16 -- Prompt Assembler type

**Epic** : C
**Priorite** : P1
**Description** :
Les Assemblers generent des methodes `Object toDto(Object entity)` sans types reels.
Le LLM doit inferer les types depuis les regles et le contexte du controller.

**Approche technique** : Enrichir le prompt avec les types extraits du controller (champs
FXML, DTOs references, services injectes). Fournir un exemple d'Assembler avec types concrets.

**Criteres d'acceptation** :
- CA-1 : Les Assemblers generent des methodes avec des types concrets (pas Object).
- CA-2 : Les noms de classes DTO et Entity sont deduits des regles.
- CA-3 : Le prompt inclut la liste des types references dans le controller.
- CA-4 : Un test verifie que les types `Object` ne sont pas utilises dans les signatures.

**Story points** : 5
**Dependances** : IAP-14
**Risques** : Moyen -- les types reels ne sont pas toujours disponibles dans le contexte
d'un seul controller. Fallback : types inferres avec commentaire `// TODO: verify type`.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-17 -- Prompt tests avec assertions

**Epic** : C
**Priorite** : P1
**Description** :
Les squelettes de test generes contiennent des `@Test void should...() {}` vides ou avec
`// TODO: add assertions`. Le LLM doit generer des tests avec des assertions significatives.

**Approche technique** : Enrichir le prompt `test-skeleton` avec les signatures des methodes
a tester, les types de retour, et des exemples d'assertions JUnit 5 / Mockito. Demander
au LLM de generer au moins un `given-when-then` par methode publique.

**Criteres d'acceptation** :
- CA-1 : Chaque methode de test generee contient au moins une assertion `assertThat`/`assertEquals`/`verify`.
- CA-2 : Les mocks Mockito sont configures avec des `when().thenReturn()`.
- CA-3 : Le pattern given-when-then est visible dans la structure du test.
- CA-4 : Un test avec un UseCase echantillon verifie la presence d'assertions.

**Story points** : 5
**Dependances** : IAP-14
**Risques** : Moyen -- les assertions generees peuvent etre incorrectes. Le but est d'avoir
un point de depart meilleur que des stubs vides.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-18 -- Gestion d'erreurs dans artefacts generes

**Epic** : C
**Priorite** : P2
**Description** :
Les artefacts generes ne contiennent aucune gestion d'erreurs : pas de try-catch, pas de
validation d'entree, pas d'exceptions metier. Le code genere devrait inclure des patterns
defensifs basiques.

**Approche technique** : Ajouter dans les prompts une instruction systematique de gestion
d'erreurs : validation des parametres d'entree, try-catch sur les appels externes,
exceptions metier nommees.

**Criteres d'acceptation** :
- CA-1 : Les UseCases generes valident les parametres d'entree (`Objects.requireNonNull`).
- CA-2 : Les Gateways generes incluent un try-catch pour les appels externes.
- CA-3 : Au moins une exception metier custom est generee par controller.
- CA-4 : Un test verifie la presence de validations dans les artefacts generes.

**Story points** : 5
**Dependances** : IAP-14
**Risques** : Faible -- amelioration de prompt sans impact structurel.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-19 -- Feature flags par type d'artefact

**Epic** : C
**Priorite** : P2
**Description** :
Actuellement, la generation IA produit tous les types d'artefacts. Il n'est pas possible
de generer uniquement les UseCases sans les Policies, ou uniquement les tests.

**Approche technique** : Ajouter dans `AiEnrichmentProperties` un map
`enabledArtifactTypes` (Set<ArtifactType>). Filtrer les artefacts demandes au LLM
dans le prompt. Exposer comme parametre dans l'endpoint REST.

**Criteres d'acceptation** :
- CA-1 : La configuration `ai.enrichment.enabled-artifact-types` existe.
- CA-2 : L'endpoint `/generate/ai` accepte un parametre optionnel `artifactTypes`.
- CA-3 : Seuls les types demandes sont generes et retournes.
- CA-4 : Par defaut, tous les types sont actifs.
- CA-5 : `mvn test` passe.

**Story points** : 3
**Dependances** : IAP-2 (pour les enums)
**Risques** : Faible -- filtrage additif sans modification du pipeline.
**Agent lead** : `backend-hexagonal`

---

## EPIC D -- Experience utilisateur IA

**Lead agent** : `frontend-angular`
**Objectif** : Rendre l'experience de generation IA interactive, informative et fluide.
**SP total** : 26
**Dependances de l'epic** : IAP-4 (retry) pour la fiabilite du streaming

---

### IAP-20 -- Streaming SSE reponses LLM

**Epic** : D
**Priorite** : P1
**Description** :
Les reponses LLM sont recues en bloc. L'utilisateur attend sans feedback jusqu'a la fin
de la generation. Le streaming via SSE (Server-Sent Events) permet d'afficher le code
genere progressivement.

**Approche technique** : Backend : nouveau endpoint SSE `GET /api/v1/analyses/{sessionId}/generate/stream`
retournant un `Flux<ServerSentEvent<String>>`. Adapter les adapters LLM pour lire la reponse
en streaming. Frontend : `EventSource` Angular avec affichage incremental du code.

**Criteres d'acceptation** :
- CA-1 : L'endpoint SSE retourne les fragments de code au fur et a mesure de la generation.
- CA-2 : Le frontend affiche le code progressivement (pas d'attente du bloc complet).
- CA-3 : En cas d'erreur LLM pendant le stream, un event `error` est emis proprement.
- CA-4 : L'endpoint non-streaming (`POST /generate/ai`) continue de fonctionner.
- CA-5 : `mvn test` et `ng test` passent.

**Story points** : 8
**Dependances** : IAP-4
**Risques** : Eleve -- necessite que les providers LLM supportent le streaming. Claude Code
CLI pourrait ne pas supporter le streaming. Prevoir un fallback vers le mode bloc.
**Agent lead** : `frontend-angular` + `backend-hexagonal`

---

### IAP-21 -- Vue diff template vs IA

**Epic** : D
**Priorite** : P1
**Description** :
L'utilisateur ne peut pas comparer les artefacts generes par template (sans IA) avec
ceux generes par l'IA. Une vue diff cote-a-cote est necessaire pour evaluer la
valeur ajoutee de l'IA.

**Approche technique** : Composant Angular `DiffViewComponent` utilisant une librairie
diff (ex: `ngx-diff`, ou diff maison avec `diff-match-patch`). Appeler les deux endpoints
(template + IA) et afficher les differences.

**Criteres d'acceptation** :
- CA-1 : Un composant `DiffViewComponent` affiche les artefacts template vs IA cote-a-cote.
- CA-2 : Les differences sont surlignees (ajouts en vert, suppressions en rouge).
- CA-3 : L'utilisateur peut selectionner le type d'artefact a comparer.
- CA-4 : `ng test` passe.

**Story points** : 5
**Dependances** : Aucune (les deux endpoints existent deja)
**Risques** : Faible -- composant frontend isole. La librairie diff doit etre compatible
Angular 21.x.
**Agent lead** : `frontend-angular`

---

### IAP-22 -- Raffinement interactif multi-tour

**Epic** : D
**Priorite** : P2
**Description** :
L'utilisateur ne peut pas demander de corrections sur un artefact genere. Il doit
relancer toute la generation. Un mode multi-tour permet de raffiner un artefact specifique.

**Approche technique** : Backend : nouveau endpoint `POST /api/v1/analyses/{sessionId}/refine`
acceptant `{artifactType, instruction, previousCode}`. Le prompt inclut le code precedent
et l'instruction de l'utilisateur. Frontend : bouton "Raffiner" par artefact avec un
champ de texte pour l'instruction.

**Criteres d'acceptation** :
- CA-1 : L'endpoint `/refine` accepte un artefact existant et une instruction utilisateur.
- CA-2 : Le LLM recoit le code precedent et l'instruction dans le prompt.
- CA-3 : Le code raffine est retourne et affiche a la place du precedent.
- CA-4 : L'historique des raffinements est conserve (au moins en session).
- CA-5 : `mvn test` et `ng test` passent.

**Story points** : 8
**Dependances** : IAP-20 (pour le streaming des raffinements)
**Risques** : Moyen -- la qualite du raffinement depend de la capacite du LLM a respecter
les instructions. Le contexte multi-tour peut depasser les limites de tokens.
**Agent lead** : `frontend-angular` + `backend-hexagonal`

---

### IAP-23 -- Feedback visuel progression generation

**Epic** : D
**Priorite** : P2
**Description** :
Pendant la generation IA (qui peut durer 10-30 secondes), l'utilisateur n'a aucun
feedback visuel. Une barre de progression avec les etapes est necessaire.

**Approche technique** : Utiliser les events SSE (IAP-20) pour emettre des events de
progression (`sanitizing`, `sending_to_llm`, `parsing_response`, `validating`).
Composant Angular `GenerationProgressComponent` avec barre de progression et etapes.

**Criteres d'acceptation** :
- CA-1 : Un composant affiche les etapes de progression de la generation.
- CA-2 : Chaque etape est mise a jour en temps reel via SSE.
- CA-3 : En cas d'erreur, l'etape fautive est visuellement identifiee.
- CA-4 : `ng test` passe.

**Story points** : 3
**Dependances** : IAP-20
**Risques** : Faible -- depend du SSE (IAP-20). Sans IAP-20, un spinner simple suffit.
**Agent lead** : `frontend-angular`

---

### IAP-24 -- Copier/telecharger artefact individuel

**Epic** : D
**Priorite** : P2
**Description** :
Les artefacts generes ne peuvent etre ni copies ni telecharges individuellement depuis
l'interface. L'utilisateur doit selectionner et copier manuellement le code.

**Approche technique** : Ajouter un bouton "Copier" (clipboard API) et "Telecharger" (.java)
par artefact dans le composant de visualisation des artefacts.

**Criteres d'acceptation** :
- CA-1 : Un bouton "Copier" copie le code de l'artefact dans le presse-papier.
- CA-2 : Un bouton "Telecharger" telecharge le fichier `.java` avec le bon nom de classe.
- CA-3 : Un feedback visuel confirme la copie ("Copie !").
- CA-4 : `ng test` passe.

**Story points** : 2
**Dependances** : Aucune
**Risques** : Aucun -- fonctionnalite frontend isolee.
**Agent lead** : `frontend-angular`

---

## EPIC E -- Persistance et versioning des artefacts IA

**Lead agent** : `backend-hexagonal` + `database-postgres`
**Objectif** : Sauvegarder, versionner, comparer et exporter les artefacts generes par l'IA.
**SP total** : 21
**Dependances de l'epic** : Aucune (parallelisable avec C et D)

---

### IAP-25 -- Schema et entite persistance artefacts IA

**Epic** : E
**Priorite** : P1
**Description** :
Les artefacts IA generes ne sont pas persistes. Ils sont perdus a chaque nouvelle generation.
Creer le schema PostgreSQL et l'entite JPA pour les persister.

**Approche technique** : Migration Flyway `V13__create_ai_generated_artifact.sql`.
Table `ai_generated_artifact` avec colonnes : `id`, `session_id`, `artifact_type`,
`class_name`, `java_source`, `provider`, `version`, `created_at`, `parent_version_id`.
Entite JPA `AiGeneratedArtifactEntity`.

**Criteres d'acceptation** :
- CA-1 : La migration Flyway cree la table `ai_generated_artifact`.
- CA-2 : L'entite JPA est mappee et compilable.
- CA-3 : La table a un index sur `session_id`.
- CA-4 : Le schema supporte le versioning (colonne `version` + `parent_version_id`).
- CA-5 : `mvn test` passe.

**Story points** : 5
**Dependances** : Aucune
**Risques** : Faible -- ajout de table sans impact sur le schema existant.
**Agent lead** : `database-postgres`

---

### IAP-26 -- Service de sauvegarde artefacts IA

**Epic** : E
**Priorite** : P1
**Description** :
Creer le use case et l'adapter JPA pour persister les artefacts generes apres chaque
generation IA. Modifier les services existants pour sauvegarder automatiquement.

**Approche technique** : Creer `SaveAiArtifactsUseCase` dans `application/ports/in/`.
Creer `AiArtifactPersistencePort` dans `application/ports/out/`. Implementer
`JpaAiArtifactPersistenceAdapter`. Appeler depuis `AiSpringBootGenerationService`.

**Criteres d'acceptation** :
- CA-1 : Les artefacts generes sont persistes en base apres chaque generation reussie.
- CA-2 : Les artefacts degrades ne sont pas persistes.
- CA-3 : Le port et l'adapter JPA existent et sont testes.
- CA-4 : L'endpoint `GET /api/v1/analyses/{sessionId}/artifacts/ai` retourne les artefacts persistes.
- CA-5 : `mvn test` passe.

**Story points** : 5
**Dependances** : IAP-25
**Risques** : Moyen -- la sauvegarde automatique augmente la charge PostgreSQL.
Prevoir un mecanisme de cleanup pour les sessions anciennes.
**Agent lead** : `backend-hexagonal`

---

### IAP-27 -- Historique et versioning des artefacts

**Epic** : E
**Priorite** : P2
**Description** :
Permettre de sauvegarder plusieurs versions d'un meme artefact (apres raffinement ou
regeneration) et de comparer les versions.

**Approche technique** : Utiliser `parent_version_id` dans la table pour chainer les versions.
Endpoint `GET /api/v1/analyses/{sessionId}/artifacts/ai/{artifactType}/versions`.
Composant Angular de comparaison de versions.

**Criteres d'acceptation** :
- CA-1 : Chaque regeneration cree une nouvelle version liee a la precedente.
- CA-2 : L'endpoint retourne l'historique ordonne des versions.
- CA-3 : Le frontend permet de comparer deux versions d'un meme artefact.
- CA-4 : `mvn test` et `ng test` passent.

**Story points** : 8
**Dependances** : IAP-26
**Risques** : Moyen -- le volume de stockage peut croitre rapidement. Prevoir une politique
de retention (garder les N dernieres versions).
**Agent lead** : `backend-hexagonal` + `frontend-angular`

---

### IAP-28 -- Export ZIP des artefacts generes

**Epic** : E
**Priorite** : P2
**Description** :
Permettre a l'utilisateur d'exporter tous les artefacts generes d'une session
dans une archive ZIP organisee par package Java.

**Approche technique** : Endpoint `GET /api/v1/analyses/{sessionId}/artifacts/ai/export`
retournant un `application/zip`. Organiser les fichiers par package Java infere.
Bouton "Exporter ZIP" dans le composant Angular.

**Criteres d'acceptation** :
- CA-1 : L'endpoint retourne un ZIP valide avec les fichiers `.java`.
- CA-2 : Les fichiers sont organises par package (repertoire = package).
- CA-3 : Le bouton Angular declenche le telechargement du ZIP.
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dependances** : IAP-26
**Risques** : Faible -- fonctionnalite d'export isolee.
**Agent lead** : `backend-hexagonal`

---

## EPIC F -- Enrichissement contextuel et apprentissage

**Lead agent** : `implementation-moteur-analyse`
**Objectif** : Utiliser le contexte metier complet et les retours utilisateur pour
ameliorer la qualite de la generation IA.
**SP total** : 21
**Dependances de l'epic** : IAP-1 (utilitaire commun), IAP-26 (persistance artefacts)

---

### IAP-29 -- Contexte ecran complet pour le LLM

**Epic** : F
**Priorite** : P1
**Description** :
Le LLM recoit uniquement le code du controller et ses regles classifiees. Il manque
le contexte de l'ecran complet : FXML, services injectes, DTOs references, relations
avec d'autres ecrans. Ce contexte enrichirait significativement la generation.

**Approche technique** : Enrichir le `SanitizedBundle` avec les metadonnees de l'ecran
(structure FXML resume, liste des services injectes, types references).
Modifier `buildBundle()` dans `LlmServiceSupport` pour inclure ces metadonnees.
Enrichir le prompt avec une section "Screen Context".

**Criteres d'acceptation** :
- CA-1 : Le prompt inclut une section "Screen Context" avec les metadonnees de l'ecran.
- CA-2 : Les services injectes sont listes avec leur type complet.
- CA-3 : Les composants FXML sont resumes (type + fx:id).
- CA-4 : La taille du contexte est plafonnee pour rester dans la fenetre de tokens.
- CA-5 : `mvn test` passe.

**Story points** : 5
**Dependances** : IAP-1
**Risques** : Moyen -- le contexte elargi peut depasser la fenetre de tokens du LLM.
Necessaire de prioriser et tronquer intelligemment.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-30 -- Injection feedback reclassification

**Epic** : F
**Priorite** : P2
**Description** :
Les reclassifications manuelles (endpoint `/reclassify`) ne sont pas utilisees comme
feedback pour la generation IA. Si un utilisateur reclassifie une regle de USE_CASE vers
POLICY, la prochaine generation devrait en tenir compte.

**Approche technique** : Avant la generation, interroger `ClassificationPersistencePort`
pour recuperer les reclassifications manuelles. Les inclure dans le prompt comme
"User corrections" avec le detail du changement.

**Criteres d'acceptation** :
- CA-1 : Les reclassifications manuelles sont incluses dans le prompt de generation.
- CA-2 : Le LLM recoit le format `[RULE-ID] reclassified: OLD -> NEW (reason)`.
- CA-3 : La generation sans reclassification n'est pas impactee.
- CA-4 : Un test verifie l'inclusion des reclassifications dans le prompt.

**Story points** : 5
**Dependances** : IAP-29
**Risques** : Faible -- ajout d'information dans le prompt sans modification structurelle.
**Agent lead** : `backend-hexagonal`

---

### IAP-31 -- Verification coherence inter-artefacts

**Epic** : F
**Priorite** : P2
**Description** :
Les artefacts generes (UseCase, Policy, ViewModel, Controller, Test) ne sont pas verifies
pour leur coherence mutuelle. Un UseCase pourrait referencer un port que le Controller
n'injecte pas, ou un test pourrait mocker un service absent.

**Approche technique** : Nouveau endpoint `POST /api/v1/analyses/{sessionId}/verify-coherence`
qui soumet tous les artefacts generes au LLM pour verification croisee. Le LLM retourne
les incoherences detectees. Reutiliser le prompt `artifact-review` enrichi.

**Criteres d'acceptation** :
- CA-1 : L'endpoint retourne une liste d'incoherences entre artefacts.
- CA-2 : Les types d'incoherences couverts : imports manquants, dependances croisees, signatures incompatibles.
- CA-3 : Chaque incoherence inclut les deux artefacts concernes et une suggestion de correction.
- CA-4 : `mvn test` passe.

**Story points** : 8
**Dependances** : IAP-29, IAP-26
**Risques** : Eleve -- necessite un prompt sophistique et une fenetre de tokens large pour
soumettre tous les artefacts. Risque de faux positifs.
**Agent lead** : `implementation-moteur-analyse`

---

### IAP-32 -- Apprentissage patterns projet cible

**Epic** : F
**Priorite** : P3
**Description** :
La generation IA ne tient pas compte des conventions et patterns du projet cible.
Si le projet utilise un pattern specifique de validation, les Policies generees
devraient le reproduire.

**Approche technique** : Permettre a l'utilisateur de fournir des "exemples reference"
(classes Java validees) qui sont persistes et inclus dans les prompts de generation
comme contexte de style. Stocker dans une table `project_reference_patterns`.

**Criteres d'acceptation** :
- CA-1 : L'utilisateur peut uploader des fichiers de reference via un endpoint REST.
- CA-2 : Les fichiers de reference sont persistes en base.
- CA-3 : Le prompt de generation inclut les references pertinentes (par type d'artefact).
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dependances** : IAP-30, IAP-27
**Risques** : Moyen -- les fichiers de reference augmentent le contexte et consomment
des tokens. Necessaire de selectionner les plus pertinents.
**Agent lead** : `implementation-moteur-analyse`

---

## Plan de lots progressifs

### Lot 1 -- Fondations (Sprint 1-2)

**Objectif** : Stabiliser le pipeline IA existant. Aucune nouvelle fonctionnalite.

| Ticket | Titre | SP | Epic |
|--------|-------|----|------|
| IAP-1  | Extraction utilitaire LLM commun | 5 | A |
| IAP-2  | Enums TaskType et LlmProvider | 3 | A |
| IAP-3  | Validation schema reponse LLM | 5 | A |
| IAP-5  | Port de lecture source fichier | 3 | A |
| IAP-9  | Metriques Micrometer appels LLM | 5 | B |
| IAP-10 | Tracing distribue X-Request-ID | 3 | B |

**Total lot 1** : 24 SP
**Agents leads** : `backend-hexagonal`, `observabilite-exploitation`

---

### Lot 2 -- Resilience et configuration (Sprint 3)

**Objectif** : Rendre le pipeline resilient aux erreurs et configurable.

| Ticket | Titre | SP | Epic |
|--------|-------|----|------|
| IAP-4  | Retry avec backoff exponentiel | 5 | A |
| IAP-6  | Configuration dynamique max_tokens | 3 | A |
| IAP-7  | Limite de taille reponse LLM | 3 | A |
| IAP-8  | Elimination constantes magiques | 2 | A |
| IAP-13 | Alerting Semgrep defaillances | 3 | B |

**Total lot 2** : 16 SP
**Agents leads** : `backend-hexagonal`, `observabilite-exploitation`

---

### Lot 3 -- Qualite du code genere (Sprint 4-5)

**Objectif** : Ameliorer significativement la qualite des artefacts IA.

| Ticket | Titre | SP | Epic |
|--------|-------|----|------|
| IAP-14 | Prompt UseCase avec corps logique | 5 | C |
| IAP-15 | Prompt Policy avec regles reelles | 5 | C |
| IAP-16 | Prompt Assembler type | 5 | C |
| IAP-17 | Prompt tests avec assertions | 5 | C |
| IAP-19 | Feature flags par type d'artefact | 3 | C |
| IAP-12 | Comptage reel de tokens | 5 | B |

**Total lot 3** : 28 SP
**Agents leads** : `implementation-moteur-analyse`, `backend-hexagonal`

---

### Lot 4 -- UX et persistance (Sprint 6-7)

**Objectif** : Ameliorer l'experience utilisateur et persister les artefacts.

| Ticket | Titre | SP | Epic |
|--------|-------|----|------|
| IAP-20 | Streaming SSE reponses LLM | 8 | D |
| IAP-21 | Vue diff template vs IA | 5 | D |
| IAP-24 | Copier/telecharger artefact | 2 | D |
| IAP-25 | Schema persistance artefacts IA | 5 | E |
| IAP-26 | Service sauvegarde artefacts IA | 5 | E |

**Total lot 4** : 25 SP
**Agents leads** : `frontend-angular`, `backend-hexagonal`, `database-postgres`

---

### Lot 5 -- Interactivite et versioning (Sprint 8)

**Objectif** : Ajouter le raffinement interactif et le versioning.

| Ticket | Titre | SP | Epic |
|--------|-------|----|------|
| IAP-18 | Gestion erreurs dans artefacts | 5 | C |
| IAP-22 | Raffinement interactif multi-tour | 8 | D |
| IAP-23 | Feedback visuel progression | 3 | D |
| IAP-11 | Dashboard sante pipeline IA | 5 | B |
| IAP-27 | Historique et versioning artefacts | 8 | E |

**Total lot 5** : 29 SP
**Agents leads** : `frontend-angular`, `backend-hexagonal`, `observabilite-exploitation`

---

### Lot 6 -- Enrichissement contextuel (Sprint 9-10)

**Objectif** : Exploiter le contexte metier et les retours utilisateur.

| Ticket | Titre | SP | Epic |
|--------|-------|----|------|
| IAP-28 | Export ZIP artefacts | 3 | E |
| IAP-29 | Contexte ecran complet | 5 | F |
| IAP-30 | Injection feedback reclassification | 5 | F |
| IAP-31 | Verification coherence inter-artefacts | 8 | F |
| IAP-32 | Apprentissage patterns projet | 3 | F |

**Total lot 6** : 24 SP
**Agents leads** : `implementation-moteur-analyse`, `backend-hexagonal`

---

## Risques globaux

| Risque | Impact | Probabilite | Mitigation |
|--------|--------|-------------|------------|
| Fenetre de tokens insuffisante pour le contexte enrichi (IAP-29, IAP-31) | Eleve | Elevee | Plafonner et tronquer intelligemment le contexte. Tester avec les limites reelles. |
| Incompatibilite Micrometer / Spring Boot 4.0.3 (IAP-9) | Moyen | Faible | Verifier dans le pom.xml avant le lot 1. |
| Streaming non supporte par Claude Code CLI (IAP-20) | Moyen | Moyenne | Prevoir un fallback vers le mode bloc pour le provider CLI. |
| Qualite des prompts insuffisante pour la generation (IAP-14..18) | Eleve | Moyenne | Iterer sur les prompts avec des corpus de test reels. Budget de 2-3 iterations par prompt. |
| Volume de stockage artefacts IA (IAP-27) | Moyen | Moyenne | Politique de retention configurable (N dernieres versions). |
| Regression des 3 services lors de l'extraction utilitaire (IAP-1) | Eleve | Faible | Tests de non-regression complets avant et apres extraction. |
| Faux positifs de la verification de coherence (IAP-31) | Moyen | Elevee | Seuil de confiance configurable. Afficher comme suggestions, pas comme erreurs. |
| Cout tokens en augmentation avec le contexte enrichi (IAP-29, IAP-30) | Moyen | Elevee | Metriques de tokens (IAP-9) + alertes de depassement budget. |

---

## Estimation consolidee

| Epic | Tickets | SP total | Lots concernes |
|------|---------|----------|----------------|
| A -- Robustesse | IAP-1 a IAP-8 | 34 | Lot 1, 2 |
| B -- Observabilite | IAP-9 a IAP-13 | 21 | Lot 1, 2, 3, 5 |
| C -- Qualite code genere | IAP-14 a IAP-19 | 28 | Lot 3, 5 |
| D -- Experience utilisateur | IAP-20 a IAP-24 | 26 | Lot 4, 5 |
| E -- Persistance/versioning | IAP-25 a IAP-28 | 21 | Lot 4, 5, 6 |
| F -- Enrichissement contextuel | IAP-29 a IAP-32 | 21 | Lot 6 |
| **Total** | **32 tickets** | **152 SP** | **6 lots** |

Velocite estimee par sprint : 13-16 SP.
Duree estimee : 10 sprints (5 mois a 2 semaines/sprint).

---

*Document produit par l'agent `jira-estimation` le 2026-03-26.*
*References : `agents/contracts.md`, `backlog-sanitisation-audit.md`, `backlog-phase-2.md`, analyse directe des services IA.*
