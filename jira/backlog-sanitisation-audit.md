# Backlog Sanitisation Audit — JavaFXAuditStudio2

Date de reference : 2026-03-25
Agent : `jira-estimation`
Modele : `opus`

---

## Contrat inter-agent (structure contracts.md)

### objectif

Transformer le scope d'audit et d'amélioration du module de sanitisation (OpenRewrite + Semgrep)
en épics, stories et tâches estimables, structurés en deux épics progressifs : QW (Quality
of sanitization Workbench) et AI (AI enrichment improvements).

### perimetre

Tickets QW-1 à QW-5 et AI-1 à AI-4 (9 tickets, 40 SP).
Périmètre : module de sanitisation existant dans `adapters/out/sanitization/`, `domain/sanitization/`,
`configuration/Sanitization*`, `configuration/Semgrep*`, `configuration/AiEnrichmentOrchestraConfiguration`.
Hors périmètre : le pipeline AI (EnrichAnalysisService, AiEnrichmentPort, LLM adapters),
déjà en production.

### faits

- Pipeline de sanitisation : 6 sanitizers en séquence + SensitiveMarkerDetector post-chain.
- OpenRewrite branché via `OpenRewriteIdentifierSanitizer` (mode AST + fallback regex).
- Semgrep désactivé par défaut (`enabled=false`, `failOnFindings=false`).
- Règles Semgrep hardcodées dans `buildRulesYaml()` (StringBuilder Java).
- Suffixes métier dupliqués dans `IdentifierSanitizer` ET `OpenRewriteIdentifierSanitizer`.
- `SanitizationReport` construit mais jamais retourné par le port ni persisté.
- `CommentSanitizer` et `DataSubstitutionSanitizer` sans test unitaire dédié.
- `SensitiveMarkerDetector` sans test dédié.
- `PreviewSanitizedSourceController` + `PreviewSanitizedSourceService` sans test.
- `SemgrepFinding.snippet` loggé en INFO/WARN — fuite potentielle de code source.
- `SensitiveMarkerDetector` logue les 12 premiers chars d'un candidat token.

### interpretations

- QW-2 (centralisation du dictionnaire) est le prérequis technique de la majorité des
  autres tickets : AI-1, AI-2, AI-3 en dépendent.
- QW-5 (fichiers non-Java) est le ticket à plus fort impact mais aussi le plus risqué
  car il nécessite une modification du port `SanitizationPort`.
- AI-3 (règles Semgrep externalisées) est indépendant du port mais dépend de QW-2
  pour la cohérence du dictionnaire métier.
- AI-4 (tests chaîne complète) doit être le dernier ticket du lot.

### hypotheses

- La version OpenRewrite compatible avec Spring Boot 4.0.3 est disponible dans `pom.xml`
  (à vérifier avant QW-3).
- Semgrep peut être installé optionnellement sur les postes de développement pour
  les tests locaux (AI-3, AI-4).
- Le port `SanitizationPort` peut être étendu sans casser les consommateurs existants
  si l'extension est addititive (surcharge ou nouvelle méthode default).

### incertitudes

- La version exacte d'OpenRewrite compatible Spring Boot 4.0.3 n'est pas vérifiée.
- L'extension du port pour multi-fichiers (QW-5) nécessite un arbitrage architecture
  avant implémentation.
- La performance de Semgrep sur un corpus multi-fichiers n'est pas mesurée.

### decisions

- La numérotation utilise les préfixes QW (Quality Workbench) et AI (AI improvements).
- L'estimation suit la suite de Fibonacci : 1, 2, 3, 5, 8, 13.
- QW-1 est la story de cartographie/audit, déjà réalisée dans cette session — statut DONE.
- Les stories sont indépendantes au sein d'un epic sauf dépendances explicites.

### livrables

- `jira/backlog-sanitisation-audit.md` (le présent document)

### dependances

- JAS-018 (pipeline sanitisation) : prérequis pour tous les tickets ci-dessous.
- `agents/contracts.md` : structure de sortie inter-agent respectée.
- `jira/backlog-phase-2.md` : cohérence de numérotation avec les lots existants.

### verifications

- Chaque ticket a : ID, titre, epic, description, critères d'acceptation, SP, dépendances, risques, agent lead.
- Les dépendances inter-tickets sont cohérentes avec la logique d'implémentation.
- Aucun ticket ne modifie le domaine hexagonal sans justification.

### handoff

```text
handoff:
  vers: gouvernance
  preconditions:
    - backlog-sanitisation-audit.md produit et lisible
    - dependances inter-tickets verifiees
  points_de_vigilance:
    - QW-5 modifie le port SanitizationPort : valider l'architecture avant implémentation
    - AI-3 externalise les règles Semgrep : valider le format YAML et le chemin classpath
    - AI-4 dépend de Semgrep installé sur l'environnement de test
  artefacts_a_consulter:
    - jira/backlog-sanitisation-audit.md
    - agents/contracts.md
    - backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/sanitization/
```

---

## Résumé des épics et estimation macro

| Epic | Tickets | Titre                                 | SP total |
|------|---------|---------------------------------------|----------|
| QW   | QW-1..5 | Quality of sanitization Workbench     | 24       |
| AI   | AI-1..4 | AI enrichment improvements            | 16       |
| **Total** |    |                                       | **40**   |

---

## EPIC QW — Quality of sanitization Workbench

**Lead agent** : `backend-hexagonal`
**Objectif** : Auditer, consolider et renforcer le module de sanitisation existant
(dictionnaire centralisé, périmètre, reporting, couverture multi-fichiers).

**Dépendances de l'epic** : JAS-018 (pipeline existant)

---

### QW-1 — Cartographie et audit de l'architecture de sanitisation existante

**Epic** : QW
**Statut** : DONE (réalisé dans la session du 2026-03-25)
**Description** :
Lire, cartographier et auditer l'intégralité du module de sanitisation :
pipeline, domaine, configuration, ports, tests, prompts Mustache.
Identifier les écarts par rapport aux bonnes pratiques (périmètre, dictionnaire,
OpenRewrite, Semgrep, logs, reporting, testabilité, maintenabilité).

**Critères d'acceptation** :
- CA-1 : Tous les fichiers du module sanitisation sont lus et documentés.
- CA-2 : L'ordre exact du pipeline est identifié et confirmé.
- CA-3 : Les fichiers non testés sont listés.
- CA-4 : Un tableau d'audit par axe (10 axes) est produit.
- CA-5 : Un plan de remédiation priorisé est disponible.

**Story points** : 3
**Dépendances** : Aucune
**Risques** : Aucun (lecture seule)
**Agent lead** : `revue-code`

---

### QW-2 — Centralisation du dictionnaire de neutralisation

**Epic** : QW
**Description** :
Créer une source unique de vérité pour les suffixes métier utilisés par
`IdentifierSanitizer` et `OpenRewriteIdentifierSanitizer`. Actuellement ces
suffixes sont dupliqués dans deux constantes distinctes. Un oubli de synchronisation
entraîne une couverture asymétrique entre la passe AST et la passe regex.

Créer une classe partagée `BusinessTermDictionary` dans `adapters/out/sanitization/`
(ou dans la configuration) exposant une constante `BUSINESS_SUFFIXES` et éventuellement
une liste de termes additionnels configurables.

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| QW-2-T1 | Créer `BusinessTermDictionary` avec la liste centralisée des suffixes | 1 |
| QW-2-T2 | Mettre à jour `IdentifierSanitizer` pour utiliser `BusinessTermDictionary` | 1 |
| QW-2-T3 | Mettre à jour `OpenRewriteIdentifierSanitizer` pour utiliser `BusinessTermDictionary` | 1 |
| QW-2-T4 | Ajouter un test vérifiant la cohérence du dictionnaire entre les deux sanitizers | 1 |
| QW-2-T5 | Documenter le dictionnaire dans le README backend | 0.5 |

**Critères d'acceptation** :
- CA-1 : Un seul endroit définit les suffixes métier.
- CA-2 : `IdentifierSanitizer` et `OpenRewriteIdentifierSanitizer` importent la même source.
- CA-3 : `mvn test` passe après modification.
- CA-4 : Un test vérifie que les deux sanitizers produisent le même résultat sur un cas nominal.

**Story points** : 5
**Dépendances** : QW-1
**Risques** : Faible — refactoring de constantes uniquement. Risque de régression si les
deux patterns ne sont pas strictement équivalents après centralisation.
**Agent lead** : `backend-hexagonal`

---

### QW-3 — Renforcement du périmètre et isolation

**Epic** : QW
**Description** :
Renforcer l'isolation du pipeline sanitisation :
1. Valider que `PreviewSanitizedSourceService` gère correctement un `controllerRef`
   invalide (chemin inexistant ou nom seul) sans fuite silencieuse.
2. Ajouter une validation explicite en entrée du port (`bundleId`, `rawSource` non vide
   au-delà du `Objects.requireNonNull` actuel).
3. Documenter les limites du périmètre : 1 fichier Java par appel, pas de classpath complet.
4. Ajouter un test pour le fallback `PreviewSanitizedSourceService` quand `controllerRef`
   est un nom simple sans chemin valide.

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| QW-3-T1 | Ajouter validation `rawSource` non blank dans `SanitizationPipelineAdapter` | 0.5 |
| QW-3-T2 | Tester `PreviewSanitizedSourceService` avec controllerRef inexistant | 1 |
| QW-3-T3 | Documenter les limites (1 fichier, pas de classpath) dans `SanitizationPort` Javadoc | 0.5 |
| QW-3-T4 | Ajouter `PreviewSanitizedSourceServiceTest` (cas nominal + cas fallback) | 2 |

**Critères d'acceptation** :
- CA-1 : `SanitizationPipelineAdapter.sanitize()` rejette un `rawSource` blank avec message explicite.
- CA-2 : `PreviewSanitizedSourceService` retourne `sanitized=false` (bundle brut) si le chemin est invalide.
- CA-3 : Tests unitaires dédiés pour `PreviewSanitizedSourceService`.
- CA-4 : `mvn test` passe.

**Story points** : 5
**Dépendances** : QW-2
**Risques** : Faible — ajout de validations sans modification du comportement nominal.
**Agent lead** : `backend-hexagonal`

---

### QW-4 — Amélioration du reporting sanitisation

**Epic** : QW
**Description** :
`SanitizationReport` est construit dans le pipeline mais jamais retourné ni persisté.
Le port `SanitizationPort` retourne uniquement `SanitizedBundle`.

Amélioration : retourner également le `SanitizationReport` depuis le pipeline, soit en
étendant `SanitizationPort` (option A : nouveau type de retour `SanitizationResult`
encapsulant bundle + report), soit en exposant le rapport via le `SanitizedBundle` lui-même
(option B : enrichir `SanitizedBundle` avec un champ `report`).

Exposer ensuite ce rapport dans l'endpoint `preview-sanitized` (détail des transformations
appliquées, nombre d'occurrences par règle, statut approved/refused).

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| QW-4-T1 | Arbitrer l'option d'architecture (A ou B) — décision en configuration | 0.5 |
| QW-4-T2 | Modifier `SanitizationPort` ou `SanitizedBundle` pour exposer le rapport | 1 |
| QW-4-T3 | Enrichir `SanitizedSourcePreviewResponse` avec les transformations du rapport | 1 |
| QW-4-T4 | Ajouter test de l'endpoint preview incluant la vérification du rapport | 0.5 |

**Critères d'acceptation** :
- CA-1 : `SanitizationReport` est accessible après un appel à `SanitizationPort.sanitize()`.
- CA-2 : L'endpoint `POST /api/v1/analyses/{sessionId}/preview-sanitized` retourne les transformations.
- CA-3 : Le rapport indique le nombre d'occurrences par `SanitizationRuleType`.
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dépendances** : QW-2, QW-3
**Risques** : Moyen — modification du port `SanitizationPort` impacte 4 consommateurs
(EnrichAnalysisService, ReviewArtifactsService, AiSpringBootGenerationService,
PreviewSanitizedSourceService). Préférer l'option B (enrichissement de `SanitizedBundle`)
pour minimiser l'impact.
**Agent lead** : `backend-hexagonal`

---

### QW-5 — Couverture des fichiers non-Java

**Epic** : QW
**Description** :
Actuellement le port ne reçoit qu'une `String` (un seul fichier Java). Les fichiers
YAML, properties, SQL, tests et docs ne sont jamais sanitisés avant envoi au LLM.

Extension du port ou création d'un port secondaire `MultiFileSanitizationPort` permettant
de soumettre une liste de fichiers avec leur type. Le `SemgrepScanSanitizer` serait étendu
pour scanner des extensions non-Java (avec des règles adaptées).

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| QW-5-T1 | Définir l'architecture : nouveau port vs extension du port existant | 1 |
| QW-5-T2 | Créer `SanitizableFile` record (content, fileName, fileType) dans le domaine | 0.5 |
| QW-5-T3 | Créer ou étendre le port pour accepter une liste de `SanitizableFile` | 2 |
| QW-5-T4 | Adapter `SemgrepScanSanitizer` pour accepter les extensions YAML/properties/SQL | 2 |
| QW-5-T5 | Ajouter des règles Semgrep pour les fichiers non-Java (secrets dans properties, etc.) | 1 |
| QW-5-T6 | Tests unitaires couvrant le scan multi-fichiers | 1 |

**Critères d'acceptation** :
- CA-1 : Un fichier `.yaml` ou `.properties` peut être soumis au pipeline sanitisation.
- CA-2 : Les secrets dans les fichiers properties sont détectés par Semgrep.
- CA-3 : Les consommateurs existants (1 fichier Java) ne sont pas cassés.
- CA-4 : `mvn test` passe.

**Story points** : 8
**Dépendances** : QW-2, QW-3
**Risques** : Élevé — modification de l'architecture du port. Décision d'architecture
obligatoire avant implémentation. Risque de régression sur les consommateurs existants.
**Agent lead** : `backend-hexagonal`

---

## EPIC AI — AI enrichment improvements

**Lead agent** : `securite` + `backend-hexagonal`
**Objectif** : Sécuriser le pipeline AI qui dépend de la sanitisation :
sécurisation des logs, mode dry-run, règles Semgrep externalisées, tests de la chaîne.

**Dépendances de l'epic** : QW-2

---

### AI-1 — Sécurisation des logs LLM

**Epic** : AI
**Description** :
Deux points de fuite de données sensibles dans les logs ont été identifiés :

1. `SensitiveMarkerDetector` logue les 12 premiers chars d'un candidat token :
   `candidate.substring(0, Math.min(12, candidate.length()))` → fuite partielle possible.
2. `SemgrepScanSanitizer.logFindings()` logue `finding.message` ET `finding.snippet` →
   le snippet peut contenir du code source non-sanitisé.

Corriger ces deux points pour ne jamais logger de données potentiellement sensibles.

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| AI-1-T1 | Remplacer le log des 12 chars par un indicateur générique dans `SensitiveMarkerDetector` | 0.5 |
| AI-1-T2 | Supprimer le log de `finding.snippet` dans `SemgrepScanSanitizer.logFindings()` | 0.5 |
| AI-1-T3 | Ajouter `SensitiveMarkerDetectorTest` couvrant les cas URL, email, token, keyword | 2 |
| AI-1-T4 | Vérifier les logs de `EnrichAnalysisService` (aucun log du source brut ni sanitisé) | 1 |
| AI-1-T5 | Ajouter un test de régression vérifiant qu'aucun message de log ne contient le source | 1 |

**Critères d'acceptation** :
- CA-1 : `SensitiveMarkerDetector` ne logue jamais de contenu partiel de la source.
- CA-2 : `SemgrepScanSanitizer` ne logue jamais `finding.snippet`.
- CA-3 : Un test dédié `SensitiveMarkerDetectorTest` existe avec ≥ 5 cas.
- CA-4 : `mvn test` passe.

**Story points** : 5
**Dépendances** : QW-2
**Risques** : Faible sur le comportement. Moyen sur la détectabilité des incidents si
les logs deviennent moins verbeux — compenser par des compteurs métriques.
**Agent lead** : `securite`

---

### AI-2 — Mode dry-run pour la sanitisation

**Epic** : AI
**Description** :
Ajouter un mode dry-run au pipeline sanitisation permettant de prévisualiser les
transformations qui seraient appliquées, sans modifier le source ni déclencher le
`SensitiveMarkerDetector` de refus.

Ce mode est utile pour :
- Valider la configuration du dictionnaire avant déploiement.
- Tester de nouvelles règles sans risque de blocage.
- Exposer via l'endpoint `preview-sanitized` (déjà existant) en mode dry-run optionnel.

Implémentation : ajouter un paramètre `dryRun` à `SanitizationPort.sanitize()` ou
créer une méthode `previewTransformations()` distincte.

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| AI-2-T1 | Ajouter la méthode `previewTransformations(bundleId, rawSource, controllerRef)` au port | 1 |
| AI-2-T2 | Implémenter dans `SanitizationPipelineAdapter` : applique les sanitizers, collecte les rapports, ne refuse pas | 1 |
| AI-2-T3 | Exposer le mode dry-run via `PreviewSanitizedSourceController` (param `?dryRun=true`) | 0.5 |
| AI-2-T4 | Tester le dry-run : une source avec marqueur sensible ne doit pas lever d'exception | 0.5 |

**Critères d'acceptation** :
- CA-1 : `previewTransformations()` retourne un rapport sans lancer de `SanitizationRefusedException`.
- CA-2 : L'endpoint `preview-sanitized?dryRun=true` retourne le rapport complet des transformations.
- CA-3 : Le mode dry-run n'envoie jamais le source au LLM.
- CA-4 : `mvn test` passe.

**Story points** : 3
**Dépendances** : QW-2
**Risques** : Faible — méthode additive au port. S'assurer que le dry-run ne court-circuite
pas accidentellement le `SensitiveMarkerDetector` sur les chemins existants.
**Agent lead** : `backend-hexagonal`

---

### AI-3 — Amélioration des règles Semgrep

**Epic** : AI
**Description** :
Les règles Semgrep sont actuellement hardcodées dans `buildRulesYaml()` (StringBuilder Java).
Elles ne sont pas versionnables, non lisibles par l'équipe sécurité et non testables
unitairement. Semgrep est désactivé par défaut et `failOnFindings=false`.

Améliorations :
1. Externaliser les règles Semgrep dans un fichier YAML classpath
   (`resources/semgrep/sanitization-rules.yaml`).
2. Ajouter une `denylist` configurable dans `SemgrepScanProperties`
   (termes métier à bloquer, en plus des `businessTerms` détectables).
3. Documenter les conditions recommandées pour activer Semgrep et `failOnFindings`.
4. Ajouter des règles pour les fichiers non-Java si QW-5 est livré.

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| AI-3-T1 | Créer `resources/semgrep/sanitization-rules.yaml` avec les 3 règles existantes + termes métier | 1 |
| AI-3-T2 | Modifier `SemgrepScanSanitizer` pour charger les règles depuis le classpath | 1 |
| AI-3-T3 | Ajouter `denylist` configurable dans `SemgrepScanProperties` | 0.5 |
| AI-3-T4 | Ajouter une règle Semgrep pour les domaines internes (pattern domaine configurable) | 0.5 |
| AI-3-T5 | Documenter dans `application.properties` les conditions d'activation Semgrep | 0.5 |
| AI-3-T6 | Test vérifiant que le fichier YAML est chargé et parsable au démarrage | 0.5 |

**Critères d'acceptation** :
- CA-1 : Les règles Semgrep sont dans `resources/semgrep/sanitization-rules.yaml`.
- CA-2 : `SemgrepScanSanitizer` charge les règles depuis le classpath (pas de StringBuilder).
- CA-3 : La `denylist` est configurable via `ai.sanitization.semgrep.denylist`.
- CA-4 : Le fichier YAML compile et est validable avec `semgrep --validate` (si Semgrep disponible).
- CA-5 : `mvn test` passe sans Semgrep sur PATH (mode gracieux conservé).

**Story points** : 5
**Dépendances** : QW-2, QW-3
**Risques** : Moyen — le chargement depuis classpath peut échouer si le chemin est mal configuré.
Prévoir un fallback sur les règles inline si le fichier est absent.
**Agent lead** : `securite`

---

### AI-4 — Tests et validation de la chaîne complète sanitisation → LLM

**Epic** : AI
**Description** :
Aucun test ne couvre la chaîne complète :
- Tous les sanitizers (OpenRewrite + 4 regex + Semgrep activé) en séquence.
- Un corpus de code métier réel (avec noms, secrets, emails, URLs).
- La vérification que la sortie ne contient plus de marqueurs sensibles.
- Le cas de refus (marqueur résiduel après pipeline).

Créer un test d'intégration `SanitizationPipelineFullIT` avec des fixtures représentatives.
Créer également les tests unitaires manquants : `CommentSanitizerTest`, `DataSubstitutionSanitizerTest`.

**Tâches techniques** :
| ID | Tâche | SP |
|----|-------|----|
| AI-4-T1 | Créer `CommentSanitizerTest` (≥ 5 cas : block, single-line sensible, single-line technique) | 0.5 |
| AI-4-T2 | Créer `DataSubstitutionSanitizerTest` (≥ 5 cas : email, numéro long, chaine longue, Java pattern) | 0.5 |
| AI-4-T3 | Créer `SensitiveMarkerDetectorTest` (si non créé dans AI-1) | 0.5 |
| AI-4-T4 | Créer fixture `SampleBusinessController.java` dans `test/resources/ai-corpus-sanitized/` | 0.5 |
| AI-4-T5 | Créer `SanitizationPipelineFullIT` : pipeline complet avec fixture réelle | 1 |
| AI-4-T6 | Vérifier dans `SanitizationPipelineFullIT` qu'aucun nom métier ne subsiste après pipeline | 0.5 |

**Critères d'acceptation** :
- CA-1 : `CommentSanitizerTest` et `DataSubstitutionSanitizerTest` existent et passent.
- CA-2 : `SanitizationPipelineFullIT` passe sur la fixture `SampleBusinessController.java`.
- CA-3 : La sortie du pipeline ne contient pas de suffixes métier (Service, Manager, etc.).
- CA-4 : Le cas de refus (source avec mot-clé `password`) est couvert.
- CA-5 : `mvn test` passe.

**Story points** : 3
**Dépendances** : QW-3, QW-5, AI-1, AI-2, AI-3
**Risques** : Faible sur le code — Semgrep non requis sur CI pour les tests unitaires.
Moyen sur la fixture : un fichier de test trop proche du code métier réel
pourrait exposer des informations confidentielles dans le repo.
**Agent lead** : `test-automation`

---

## Plan de lots progressifs

### Sprint 1 — Fondations qualité

| Ticket | Titre | SP |
|--------|-------|----|
| QW-1   | Cartographie et audit (DONE) | 3 |
| QW-2   | Centralisation dictionnaire | 5 |
| AI-1   | Sécurisation logs LLM | 5 |

**Objectif** : Éliminer les fuites de logs, unifier le dictionnaire. Pas de modification de port.
**Total sprint 1** : 13 SP

### Sprint 2 — Robustesse et reporting

| Ticket | Titre | SP |
|--------|-------|----|
| QW-3   | Périmètre et isolation | 5 |
| QW-4   | Reporting sanitisation | 3 |
| AI-2   | Mode dry-run | 3 |

**Objectif** : Renforcer la robustesse du pipeline, exposer le rapport, ajouter dry-run.
**Total sprint 2** : 11 SP

### Sprint 3 — Couverture et industrialisation

| Ticket | Titre | SP |
|--------|-------|----|
| QW-5   | Fichiers non-Java | 8 |
| AI-3   | Règles Semgrep externalisées | 5 |
| AI-4   | Tests chaîne complète | 3 |

**Objectif** : Couvrir les fichiers non-Java, externaliser les règles, valider la chaîne complète.
**Total sprint 3** : 16 SP

---

## Risques globaux

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| Incompatibilité OpenRewrite / Spring Boot 4.0.3 | Élevé | Faible | Vérifier la version dans `pom.xml` avant QW-3 |
| Performance Semgrep sur gros corpus multi-fichiers | Moyen | Élevée | QW-5 : prévoir un timeout configurable par fichier |
| Fuite de termes sensibles dans les logs LLM | Élevé | Moyenne | AI-1 : ticket prioritaire sprint 1 |
| Régression de la sanitisation sur les consommateurs existants | Élevé | Moyenne | QW-4 / QW-5 : tests de non-régression obligatoires |
| Règles Semgrep YAML invalides au chargement | Moyen | Faible | AI-3 : fallback sur règles inline + test de démarrage |

---

## Estimation consolidée

| Epic | Tickets | SP total |
|------|---------|----------|
| QW   | QW-1 à QW-5 | 24 |
| AI   | AI-1 à AI-4 | 16 |
| **Total** | **9 tickets** | **40 SP** |

---

*Document produit par l'agent `jira-estimation` le 2026-03-25.*
*Référence : `agents/contracts.md`, `jira/backlog-phase-2.md`, analyse directe du module sanitisation.*
