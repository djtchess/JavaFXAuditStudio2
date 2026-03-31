# Backlog Sanitisation Audit â€” JavaFXAuditStudio2

Date de reference : 2026-03-30
Agent : `jira-estimation`
Modele : `GPT-5.4`

---

## Contrat inter-agent (structure contracts.md)

### objectif

Transformer le scope d'audit et d'amÃ©lioration du module de sanitisation (OpenRewrite + Semgrep)
en Ã©pics, stories et tÃ¢ches estimables, structurÃ©s en deux Ã©pics progressifs : QW (Quality
of sanitization Workbench) et AI (AI enrichment improvements).

### perimetre

Mise a jour 2026-03-30 :
- Tickets cibles : QW-1 a QW-5 et AI-1 a AI-8 (13 tickets, 61 SP).
- Le perimetre couvre maintenant aussi `adapters/out/ai/` et `application/service/LlmServiceSupport`
  car les ecarts restants se situent dans l'assemblage des prompts, les contrats de sortie
  fournisseur et le chemin Claude CLI.
- Hors perimetre : UX frontend IA, changement de modele LLM, evolution fonctionnelle des use cases metier.

### faits

- `OpenAiGpt54AiEnrichmentAdapter` et `ClaudeCodeAiEnrichmentAdapter` envoient un unique
  message `user` contenant a la fois consignes et donnees rendues par Mustache.
- `OpenAiHttpDtos.ChatRequest` et `ClaudeHttpDtos.MessagesRequest` n'exposent pas de schema
  de sortie strict ni de canal `system`.
- `AiEnrichmentRequest.extraContext` est injecte tel quel dans le rendu de prompt via `context.putAll(...)`.
- `LlmServiceSupport.formatGeneratedArtifactDetails()` et `formatProjectReferencePatterns()`
  injectent du contenu brut d'artefacts et de patterns dans les prompts.
- Le budget token de la sanitisation couvre le `sanitizedSource`, pas le prompt rendu complet.
- L'audit LLM persiste le hash du `sanitizedSource`, pas le hash du prompt complet ni de ses sections.
- `ClaudeCodeCliAiEnrichmentAdapter` retire deux variables d'environnement sensibles, mais
  conserve le repertoire de travail courant et le reste de l'environnement parent.

- Pipeline de sanitisation : 6 sanitizers en sÃ©quence + SensitiveMarkerDetector post-chain.
- OpenRewrite branchÃ© via `OpenRewriteIdentifierSanitizer` (mode AST + fallback regex).
- Semgrep dÃ©sactivÃ© par dÃ©faut (`enabled=false`, `failOnFindings=false`).
- RÃ¨gles Semgrep hardcodÃ©es dans `buildRulesYaml()` (StringBuilder Java).
- Suffixes mÃ©tier dupliquÃ©s dans `IdentifierSanitizer` ET `OpenRewriteIdentifierSanitizer`.
- `SanitizationReport` construit mais jamais retournÃ© par le port ni persistÃ©.
- `CommentSanitizer` et `DataSubstitutionSanitizer` sans test unitaire dÃ©diÃ©.
- `SensitiveMarkerDetector` sans test dÃ©diÃ©.
- `PreviewSanitizedSourceController` + `PreviewSanitizedSourceService` sans test.
- `SemgrepFinding.snippet` loggÃ© en INFO/WARN â€” fuite potentielle de code source.
- `SensitiveMarkerDetector` logue les 12 premiers chars d'un candidat token.

### interpretations

- La frontiere actuelle de sanitisation s'arrete trop tot : le `sanitizedSource` est traite,
  mais le contexte auxiliaire re-injecte dans les prompts contourne encore cette protection.
- Le durcissement a plus forte valeur se situe maintenant dans l'assemblage du prompt,
  le contrat de sortie fournisseur et la gouvernance du chemin CLI.

- QW-2 (centralisation du dictionnaire) est le prÃ©requis technique de la majoritÃ© des
  autres tickets : AI-1, AI-2, AI-3 en dÃ©pendent.
- QW-5 (fichiers non-Java) est le ticket Ã  plus fort impact mais aussi le plus risquÃ©
  car il nÃ©cessite une modification du port `SanitizationPort`.
- AI-3 (rÃ¨gles Semgrep externalisÃ©es) est indÃ©pendant du port mais dÃ©pend de QW-2
  pour la cohÃ©rence du dictionnaire mÃ©tier.
- AI-4 (tests chaÃ®ne complÃ¨te) valide la premiÃ¨re vague de durcissement.
- AI-5 Ã  AI-8 couvrent le durcissement fournisseur observÃ© aprÃ¨s l'extension du pipeline :
  contexte promptable complet, assemblage de prompt, sorties structurÃ©es et budgets de contexte.

### hypotheses

- Les API OpenAI et Claude retenues supportent encore des mecanismes de structuration
  de sortie et de separation instructions/donnees exploitables depuis le backend.

- La version OpenRewrite compatible avec Spring Boot 4.0.3 est disponible dans `pom.xml`
  (Ã  vÃ©rifier avant QW-3).
- Semgrep peut Ãªtre installÃ© optionnellement sur les postes de dÃ©veloppement pour
  les tests locaux (AI-3, AI-4) ; les tickets AI-5 Ã  AI-8 restent eux testables sans dÃ©pendance
  externe obligatoire hors fournisseurs LLM mockÃ©s.
- Le port `SanitizationPort` peut Ãªtre Ã©tendu sans casser les consommateurs existants
  si l'extension est addititive (surcharge ou nouvelle mÃ©thode default).

### incertitudes

- Le niveau exact d'isolation possible avec `claude --print` depend des options disponibles
  sur la version de CLI installee en environnement cible.

- La version exacte d'OpenRewrite compatible Spring Boot 4.0.3 n'est pas vÃ©rifiÃ©e.
- L'extension du port pour multi-fichiers (QW-5) nÃ©cessite un arbitrage architecture
  avant implÃ©mentation.
- La performance de Semgrep sur un corpus multi-fichiers n'est pas mesurÃ©e.

### decisions

- Les nouveaux tickets AI-5 a AI-8 ciblent specifiquement la surface fournisseur
  (OpenAI, Claude HTTP, Claude CLI) sans reouvrir les tickets deja couverts par AI-1 a AI-4.

- La numÃ©rotation utilise les prÃ©fixes QW (Quality Workbench) et AI (AI improvements).
- L'estimation suit la suite de Fibonacci : 1, 2, 3, 5, 8, 13.
- QW-1 est la story de cartographie/audit, dÃ©jÃ  rÃ©alisÃ©e dans cette session â€” statut DONE.
- Les stories sont indÃ©pendantes au sein d'un epic sauf dÃ©pendances explicites.

### livrables

- `jira/backlog-sanitisation-audit.md` (le prÃ©sent document)

### dependances

- JAS-018 (pipeline sanitisation) : prÃ©requis pour tous les tickets ci-dessous.
- `agents/contracts.md` : structure de sortie inter-agent respectÃ©e.
- `jira/backlog-phase-2.md` : cohÃ©rence de numÃ©rotation avec les lots existants.

### verifications

- Chaque ticket a : ID, titre, epic, description, critÃ¨res d'acceptation, SP, dÃ©pendances, risques, agent lead.
- Les dÃ©pendances inter-tickets sont cohÃ©rentes avec la logique d'implÃ©mentation.
- Aucun ticket ne modifie le domaine hexagonal sans justification.

### handoff

```text
handoff:
  vers: gouvernance
  preconditions:
    - backlog-sanitisation-audit.md produit et lisible
    - dependances inter-tickets verifiees
  points_de_vigilance:
    - QW-5 modifie le port SanitizationPort : valider l'architecture avant implÃ©mentation
    - AI-3 externalise les rÃ¨gles Semgrep : valider le format YAML et le chemin classpath
    - AI-4 dÃ©pend de Semgrep installÃ© sur l'environnement de test
    - AI-6 change le montage des prompts et la collision des variables reservees : verifier tous les templates Mustache
    - AI-7 et AI-8 doivent conserver la parite OpenAI / Claude HTTP et documenter les ecarts inevitables du chemin CLI
  artefacts_a_consulter:
    - jira/backlog-sanitisation-audit.md
    - agents/contracts.md
    - backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/sanitization/
```

---

## RÃ©sumÃ© des Ã©pics et estimation macro

| Epic | Tickets | Titre                                 | SP total |
|------|---------|---------------------------------------|----------|
| QW   | QW-1..5 | Quality of sanitization Workbench     | 24       |
| AI   | AI-1..8 | AI enrichment improvements            | 37       |
| **Total** |    |                                       | **61**   |

---

## EPIC QW â€” Quality of sanitization Workbench

**Lead agent** : `backend-hexagonal`
**Objectif** : Auditer, consolider et renforcer le module de sanitisation existant
(dictionnaire centralisÃ©, pÃ©rimÃ¨tre, reporting, couverture multi-fichiers).

**DÃ©pendances de l'epic** : JAS-018 (pipeline existant)

---

### QW-1 â€” Cartographie et audit de l'architecture de sanitisation existante

**Epic** : QW
**Statut** : DONE (rÃ©alisÃ© dans la session du 2026-03-25)
**Description** :
Lire, cartographier et auditer l'intÃ©gralitÃ© du module de sanitisation :
pipeline, domaine, configuration, ports, tests, prompts Mustache.
Identifier les Ã©carts par rapport aux bonnes pratiques (pÃ©rimÃ¨tre, dictionnaire,
OpenRewrite, Semgrep, logs, reporting, testabilitÃ©, maintenabilitÃ©).

**CritÃ¨res d'acceptation** :
- CA-1 : Tous les fichiers du module sanitisation sont lus et documentÃ©s.
- CA-2 : L'ordre exact du pipeline est identifiÃ© et confirmÃ©.
- CA-3 : Les fichiers non testÃ©s sont listÃ©s.
- CA-4 : Un tableau d'audit par axe (10 axes) est produit.
- CA-5 : Un plan de remÃ©diation priorisÃ© est disponible.

**Story points** : 3
**DÃ©pendances** : Aucune
**Risques** : Aucun (lecture seule)
**Agent lead** : `revue-code`

---

### QW-2 â€” Centralisation du dictionnaire de neutralisation

**Epic** : QW
**Description** :
CrÃ©er une source unique de vÃ©ritÃ© pour les suffixes mÃ©tier utilisÃ©s par
`IdentifierSanitizer` et `OpenRewriteIdentifierSanitizer`. Actuellement ces
suffixes sont dupliquÃ©s dans deux constantes distinctes. Un oubli de synchronisation
entraÃ®ne une couverture asymÃ©trique entre la passe AST et la passe regex.

CrÃ©er une classe partagÃ©e `BusinessTermDictionary` dans `adapters/out/sanitization/`
(ou dans la configuration) exposant une constante `BUSINESS_SUFFIXES` et Ã©ventuellement
une liste de termes additionnels configurables.

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| QW-2-T1 | CrÃ©er `BusinessTermDictionary` avec la liste centralisÃ©e des suffixes | 1 |
| QW-2-T2 | Mettre Ã  jour `IdentifierSanitizer` pour utiliser `BusinessTermDictionary` | 1 |
| QW-2-T3 | Mettre Ã  jour `OpenRewriteIdentifierSanitizer` pour utiliser `BusinessTermDictionary` | 1 |
| QW-2-T4 | Ajouter un test vÃ©rifiant la cohÃ©rence du dictionnaire entre les deux sanitizers | 1 |
| QW-2-T5 | Documenter le dictionnaire dans le README backend | 0.5 |

**CritÃ¨res d'acceptation** :
- CA-1 : Un seul endroit dÃ©finit les suffixes mÃ©tier.
- CA-2 : `IdentifierSanitizer` et `OpenRewriteIdentifierSanitizer` importent la mÃªme source.
- CA-3 : `mvn test` passe aprÃ¨s modification.
- CA-4 : Un test vÃ©rifie que les deux sanitizers produisent le mÃªme rÃ©sultat sur un cas nominal.

**Story points** : 5
**DÃ©pendances** : QW-1
**Risques** : Faible â€” refactoring de constantes uniquement. Risque de rÃ©gression si les
deux patterns ne sont pas strictement Ã©quivalents aprÃ¨s centralisation.
**Agent lead** : `backend-hexagonal`

---

### QW-3 â€” Renforcement du pÃ©rimÃ¨tre et isolation

**Epic** : QW
**Description** :
Renforcer l'isolation du pipeline sanitisation :
1. Valider que `PreviewSanitizedSourceService` gÃ¨re correctement un `controllerRef`
   invalide (chemin inexistant ou nom seul) sans fuite silencieuse.
2. Ajouter une validation explicite en entrÃ©e du port (`bundleId`, `rawSource` non vide
   au-delÃ  du `Objects.requireNonNull` actuel).
3. Documenter les limites du pÃ©rimÃ¨tre : 1 fichier Java par appel, pas de classpath complet.
4. Ajouter un test pour le fallback `PreviewSanitizedSourceService` quand `controllerRef`
   est un nom simple sans chemin valide.

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| QW-3-T1 | Ajouter validation `rawSource` non blank dans `SanitizationPipelineAdapter` | 0.5 |
| QW-3-T2 | Tester `PreviewSanitizedSourceService` avec controllerRef inexistant | 1 |
| QW-3-T3 | Documenter les limites (1 fichier, pas de classpath) dans `SanitizationPort` Javadoc | 0.5 |
| QW-3-T4 | Ajouter `PreviewSanitizedSourceServiceTest` (cas nominal + cas fallback) | 2 |

**CritÃ¨res d'acceptation** :
- CA-1 : `SanitizationPipelineAdapter.sanitize()` rejette un `rawSource` blank avec message explicite.
- CA-2 : `PreviewSanitizedSourceService` retourne `sanitized=false` (bundle brut) si le chemin est invalide.
- CA-3 : Tests unitaires dÃ©diÃ©s pour `PreviewSanitizedSourceService`.
- CA-4 : `mvn test` passe.

**Story points** : 5
**DÃ©pendances** : QW-2
**Risques** : Faible â€” ajout de validations sans modification du comportement nominal.
**Agent lead** : `backend-hexagonal`

---

### QW-4 â€” AmÃ©lioration du reporting sanitisation

**Epic** : QW
**Description** :
`SanitizationReport` est construit dans le pipeline mais jamais retournÃ© ni persistÃ©.
Le port `SanitizationPort` retourne uniquement `SanitizedBundle`.

AmÃ©lioration : retourner Ã©galement le `SanitizationReport` depuis le pipeline, soit en
Ã©tendant `SanitizationPort` (option A : nouveau type de retour `SanitizationResult`
encapsulant bundle + report), soit en exposant le rapport via le `SanitizedBundle` lui-mÃªme
(option B : enrichir `SanitizedBundle` avec un champ `report`).

Exposer ensuite ce rapport dans l'endpoint `preview-sanitized` (dÃ©tail des transformations
appliquÃ©es, nombre d'occurrences par rÃ¨gle, statut approved/refused).

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| QW-4-T1 | Arbitrer l'option d'architecture (A ou B) â€” dÃ©cision en configuration | 0.5 |
| QW-4-T2 | Modifier `SanitizationPort` ou `SanitizedBundle` pour exposer le rapport | 1 |
| QW-4-T3 | Enrichir `SanitizedSourcePreviewResponse` avec les transformations du rapport | 1 |
| QW-4-T4 | Ajouter test de l'endpoint preview incluant la vÃ©rification du rapport | 0.5 |

**CritÃ¨res d'acceptation** :
- CA-1 : `SanitizationReport` est accessible aprÃ¨s un appel Ã  `SanitizationPort.sanitize()`.
- CA-2 : L'endpoint `POST /api/v1/analyses/{sessionId}/preview-sanitized` retourne les transformations.
- CA-3 : Le rapport indique le nombre d'occurrences par `SanitizationRuleType`.
- CA-4 : `mvn test` passe.

**Story points** : 3
**DÃ©pendances** : QW-2, QW-3
**Risques** : Moyen â€” modification du port `SanitizationPort` impacte 4 consommateurs
(EnrichAnalysisService, ReviewArtifactsService, AiSpringBootGenerationService,
PreviewSanitizedSourceService). PrÃ©fÃ©rer l'option B (enrichissement de `SanitizedBundle`)
pour minimiser l'impact.
**Agent lead** : `backend-hexagonal`

---

### QW-5 â€” Couverture des fichiers non-Java

**Epic** : QW
**Description** :
Actuellement le port ne reÃ§oit qu'une `String` (un seul fichier Java). Les fichiers
YAML, properties, SQL, tests et docs ne sont jamais sanitisÃ©s avant envoi au LLM.

Extension du port ou crÃ©ation d'un port secondaire `MultiFileSanitizationPort` permettant
de soumettre une liste de fichiers avec leur type. Le `SemgrepScanSanitizer` serait Ã©tendu
pour scanner des extensions non-Java (avec des rÃ¨gles adaptÃ©es).

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| QW-5-T1 | DÃ©finir l'architecture : nouveau port vs extension du port existant | 1 |
| QW-5-T2 | CrÃ©er `SanitizableFile` record (content, fileName, fileType) dans le domaine | 0.5 |
| QW-5-T3 | CrÃ©er ou Ã©tendre le port pour accepter une liste de `SanitizableFile` | 2 |
| QW-5-T4 | Adapter `SemgrepScanSanitizer` pour accepter les extensions YAML/properties/SQL | 2 |
| QW-5-T5 | Ajouter des rÃ¨gles Semgrep pour les fichiers non-Java (secrets dans properties, etc.) | 1 |
| QW-5-T6 | Tests unitaires couvrant le scan multi-fichiers | 1 |

**CritÃ¨res d'acceptation** :
- CA-1 : Un fichier `.yaml` ou `.properties` peut Ãªtre soumis au pipeline sanitisation.
- CA-2 : Les secrets dans les fichiers properties sont dÃ©tectÃ©s par Semgrep.
- CA-3 : Les consommateurs existants (1 fichier Java) ne sont pas cassÃ©s.
- CA-4 : `mvn test` passe.

**Story points** : 8
**DÃ©pendances** : QW-2, QW-3
**Risques** : Ã‰levÃ© â€” modification de l'architecture du port. DÃ©cision d'architecture
obligatoire avant implÃ©mentation. Risque de rÃ©gression sur les consommateurs existants.
**Agent lead** : `backend-hexagonal`

---

## EPIC AI â€” AI enrichment improvements

**Lead agent** : `securite` + `backend-hexagonal`
**Objectif** : SÃ©curiser le pipeline AI qui dÃ©pend de la sanitisation :
sÃ©curisation des logs, mode dry-run, rÃ¨gles Semgrep externalisÃ©es, tests de la chaÃ®ne,
sanitisation du contexte promptable, cloisonnement des prompts et sorties structurÃ©es strictes.

**DÃ©pendances de l'epic** : QW-2

---

### AI-1 â€” SÃ©curisation des logs LLM

**Epic** : AI
**Description** :
Deux points de fuite de donnÃ©es sensibles dans les logs ont Ã©tÃ© identifiÃ©s :

1. `SensitiveMarkerDetector` logue les 12 premiers chars d'un candidat token :
   `candidate.substring(0, Math.min(12, candidate.length()))` â†’ fuite partielle possible.
2. `SemgrepScanSanitizer.logFindings()` logue `finding.message` ET `finding.snippet` â†’
   le snippet peut contenir du code source non-sanitisÃ©.

Corriger ces deux points pour ne jamais logger de donnÃ©es potentiellement sensibles.

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| AI-1-T1 | Remplacer le log des 12 chars par un indicateur gÃ©nÃ©rique dans `SensitiveMarkerDetector` | 0.5 |
| AI-1-T2 | Supprimer le log de `finding.snippet` dans `SemgrepScanSanitizer.logFindings()` | 0.5 |
| AI-1-T3 | Ajouter `SensitiveMarkerDetectorTest` couvrant les cas URL, email, token, keyword | 2 |
| AI-1-T4 | VÃ©rifier les logs de `EnrichAnalysisService` (aucun log du source brut ni sanitisÃ©) | 1 |
| AI-1-T5 | Ajouter un test de rÃ©gression vÃ©rifiant qu'aucun message de log ne contient le source | 1 |

**CritÃ¨res d'acceptation** :
- CA-1 : `SensitiveMarkerDetector` ne logue jamais de contenu partiel de la source.
- CA-2 : `SemgrepScanSanitizer` ne logue jamais `finding.snippet`.
- CA-3 : Un test dÃ©diÃ© `SensitiveMarkerDetectorTest` existe avec â‰¥ 5 cas.
- CA-4 : `mvn test` passe.

**Story points** : 5
**DÃ©pendances** : QW-2
**Risques** : Faible sur le comportement. Moyen sur la dÃ©tectabilitÃ© des incidents si
les logs deviennent moins verbeux â€” compenser par des compteurs mÃ©triques.
**Agent lead** : `securite`

---

### AI-2 â€” Mode dry-run pour la sanitisation

**Epic** : AI
**Description** :
Ajouter un mode dry-run au pipeline sanitisation permettant de prÃ©visualiser les
transformations qui seraient appliquÃ©es, sans modifier le source ni dÃ©clencher le
`SensitiveMarkerDetector` de refus.

Ce mode est utile pour :
- Valider la configuration du dictionnaire avant dÃ©ploiement.
- Tester de nouvelles rÃ¨gles sans risque de blocage.
- Exposer via l'endpoint `preview-sanitized` (dÃ©jÃ  existant) en mode dry-run optionnel.

ImplÃ©mentation : ajouter un paramÃ¨tre `dryRun` Ã  `SanitizationPort.sanitize()` ou
crÃ©er une mÃ©thode `previewTransformations()` distincte.

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| AI-2-T1 | Ajouter la mÃ©thode `previewTransformations(bundleId, rawSource, controllerRef)` au port | 1 |
| AI-2-T2 | ImplÃ©menter dans `SanitizationPipelineAdapter` : applique les sanitizers, collecte les rapports, ne refuse pas | 1 |
| AI-2-T3 | Exposer le mode dry-run via `PreviewSanitizedSourceController` (param `?dryRun=true`) | 0.5 |
| AI-2-T4 | Tester le dry-run : une source avec marqueur sensible ne doit pas lever d'exception | 0.5 |

**CritÃ¨res d'acceptation** :
- CA-1 : `previewTransformations()` retourne un rapport sans lancer de `SanitizationRefusedException`.
- CA-2 : L'endpoint `preview-sanitized?dryRun=true` retourne le rapport complet des transformations.
- CA-3 : Le mode dry-run n'envoie jamais le source au LLM.
- CA-4 : `mvn test` passe.

**Story points** : 3
**DÃ©pendances** : QW-2
**Risques** : Faible â€” mÃ©thode additive au port. S'assurer que le dry-run ne court-circuite
pas accidentellement le `SensitiveMarkerDetector` sur les chemins existants.
**Agent lead** : `backend-hexagonal`

---

### AI-3 â€” AmÃ©lioration des rÃ¨gles Semgrep

**Epic** : AI
**Description** :
Les rÃ¨gles Semgrep sont actuellement hardcodÃ©es dans `buildRulesYaml()` (StringBuilder Java).
Elles ne sont pas versionnables, non lisibles par l'Ã©quipe sÃ©curitÃ© et non testables
unitairement. Semgrep est dÃ©sactivÃ© par dÃ©faut et `failOnFindings=false`.

AmÃ©liorations :
1. Externaliser les rÃ¨gles Semgrep dans un fichier YAML classpath
   (`resources/semgrep/sanitization-rules.yaml`).
2. Ajouter une `denylist` configurable dans `SemgrepScanProperties`
   (termes mÃ©tier Ã  bloquer, en plus des `businessTerms` dÃ©tectables).
3. Documenter les conditions recommandÃ©es pour activer Semgrep et `failOnFindings`.
4. Ajouter des rÃ¨gles pour les fichiers non-Java si QW-5 est livrÃ©.

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| AI-3-T1 | CrÃ©er `resources/semgrep/sanitization-rules.yaml` avec les 3 rÃ¨gles existantes + termes mÃ©tier | 1 |
| AI-3-T2 | Modifier `SemgrepScanSanitizer` pour charger les rÃ¨gles depuis le classpath | 1 |
| AI-3-T3 | Ajouter `denylist` configurable dans `SemgrepScanProperties` | 0.5 |
| AI-3-T4 | Ajouter une rÃ¨gle Semgrep pour les domaines internes (pattern domaine configurable) | 0.5 |
| AI-3-T5 | Documenter dans `application.properties` les conditions d'activation Semgrep | 0.5 |
| AI-3-T6 | Test vÃ©rifiant que le fichier YAML est chargÃ© et parsable au dÃ©marrage | 0.5 |

**CritÃ¨res d'acceptation** :
- CA-1 : Les rÃ¨gles Semgrep sont dans `resources/semgrep/sanitization-rules.yaml`.
- CA-2 : `SemgrepScanSanitizer` charge les rÃ¨gles depuis le classpath (pas de StringBuilder).
- CA-3 : La `denylist` est configurable via `ai.sanitization.semgrep.denylist`.
- CA-4 : Le fichier YAML compile et est validable avec `semgrep --validate` (si Semgrep disponible).
- CA-5 : `mvn test` passe sans Semgrep sur PATH (mode gracieux conservÃ©).

**Story points** : 5
**DÃ©pendances** : QW-2, QW-3
**Risques** : Moyen â€” le chargement depuis classpath peut Ã©chouer si le chemin est mal configurÃ©.
PrÃ©voir un fallback sur les rÃ¨gles inline si le fichier est absent.
**Agent lead** : `securite`

---

### AI-4 â€” Tests et validation de la chaÃ®ne complÃ¨te sanitisation â†’ LLM

**Epic** : AI
**Description** :
Aucun test ne couvre la chaÃ®ne complÃ¨te :
- Tous les sanitizers (OpenRewrite + 4 regex + Semgrep activÃ©) en sÃ©quence.
- Un corpus de code mÃ©tier rÃ©el (avec noms, secrets, emails, URLs).
- La vÃ©rification que la sortie ne contient plus de marqueurs sensibles.
- Le cas de refus (marqueur rÃ©siduel aprÃ¨s pipeline).

CrÃ©er un test d'intÃ©gration `SanitizationPipelineFullIT` avec des fixtures reprÃ©sentatives.
CrÃ©er Ã©galement les tests unitaires manquants : `CommentSanitizerTest`, `DataSubstitutionSanitizerTest`.

**TÃ¢ches techniques** :
| ID | TÃ¢che | SP |
|----|-------|----|
| AI-4-T1 | CrÃ©er `CommentSanitizerTest` (â‰¥ 5 cas : block, single-line sensible, single-line technique) | 0.5 |
| AI-4-T2 | CrÃ©er `DataSubstitutionSanitizerTest` (â‰¥ 5 cas : email, numÃ©ro long, chaine longue, Java pattern) | 0.5 |
| AI-4-T3 | CrÃ©er `SensitiveMarkerDetectorTest` (si non crÃ©Ã© dans AI-1) | 0.5 |
| AI-4-T4 | CrÃ©er fixture `SampleBusinessController.java` dans `test/resources/ai-corpus-sanitized/` | 0.5 |
| AI-4-T5 | CrÃ©er `SanitizationPipelineFullIT` : pipeline complet avec fixture rÃ©elle | 1 |
| AI-4-T6 | VÃ©rifier dans `SanitizationPipelineFullIT` qu'aucun nom mÃ©tier ne subsiste aprÃ¨s pipeline | 0.5 |

**CritÃ¨res d'acceptation** :
- CA-1 : `CommentSanitizerTest` et `DataSubstitutionSanitizerTest` existent et passent.
- CA-2 : `SanitizationPipelineFullIT` passe sur la fixture `SampleBusinessController.java`.
- CA-3 : La sortie du pipeline ne contient pas de suffixes mÃ©tier (Service, Manager, etc.).
- CA-4 : Le cas de refus (source avec mot-clÃ© `password`) est couvert.
- CA-5 : `mvn test` passe.

**Story points** : 3
**DÃ©pendances** : QW-3, QW-5, AI-1, AI-2, AI-3
**Risques** : Faible sur le code â€” Semgrep non requis sur CI pour les tests unitaires.
Moyen sur la fixture : un fichier de test trop proche du code mÃ©tier rÃ©el
pourrait exposer des informations confidentielles dans le repo.
**Agent lead** : `test-automation`

---

### AI-5 - Sanitisation de tout le contexte promptable

**Epic** : AI
**Description** :
Le pipeline sanitize surtout `rawSource` vers `SanitizedBundle`. En revanche, plusieurs
champs injectes via `extraContext` restent bruts avant envoi aux fournisseurs :

- `previousCode` dans `RefineArtifactService`
- `currentArtifactCode` dans `RefineAiArtifactService`
- `generatedArtifactDetails` dans `VerifyAiArtifactCoherenceService`
- `projectReferencePatterns` dans `AiSpringBootGenerationService` et `VerifyAiArtifactCoherenceService`

Ce ticket etend la sanitisation a tout contenu code ou texte riche reinjecte dans un prompt,
avec un assembleur dedie qui applique le bon profil par type de fragment avant rendu Mustache.

**Taches techniques** :
| ID | Tache | SP |
|----|-------|----|
| AI-5-T1 | Introduire un assembleur `PromptContextSanitizer` ou equivalent pour les champs `extraContext` promptables | 2 |
| AI-5-T2 | Sanitiser `previousCode` / `currentArtifactCode` avant injection dans les prompts de raffinement | 2 |
| AI-5-T3 | Sanitiser ou reduire `generatedArtifactDetails` avant injection dans la verification de coherence | 1 |
| AI-5-T4 | Sanitiser les `projectReferencePatterns` avant injection dans les prompts de generation et coherence | 1 |
| AI-5-T5 | Ajouter des tests d'integration verifiant qu'aucun bloc de contexte brut ne fuit vers les adaptateurs Claude/OpenAI | 2 |

**Criteres d'acceptation** :
- CA-1 : Aucun champ contenant du code ou du texte libre n'est injecte brut dans `extraContext`.
- CA-2 : `previousCode`, `currentArtifactCode`, `generatedArtifactDetails` et `projectReferencePatterns` passent par une etape de sanitisation ou de reduction explicite.
- CA-3 : Les adaptateurs OpenAI, Claude HTTP et Claude CLI ne recoivent plus de contexte promptable brut pour ces champs.
- CA-4 : Une suite de tests couvre au minimum les flux review, generation, refine et coherence.

**Story points** : 8
**Dependances** : QW-5, AI-4
**Risques** : Eleve - si la sanitisation du contexte est trop aggressive, la qualite des suggestions peut baisser.
Il faut mesurer le compromis entre protection et utilite metier.
**Agent lead** : `backend-hexagonal`

---

### AI-6 - Isolation des variables reservees et cloisonnement instruction/donnee

**Epic** : AI
**Description** :
Les adaptateurs fusionnent aujourd'hui le contexte de base puis `extraContext` avec `context.putAll(...)`.
Un champ externe peut donc ecraser `sanitizedSource`, `controllerRef`, `taskType` ou `estimatedTokens`.
En parallele, les prompts sont envoyes comme un seul bloc `user`, sans separation provider-native
entre instructions stables et donnees a analyser.

Ce ticket ajoute :
1. une allowlist de cles autorisees dans `extraContext`,
2. le rejet explicite des collisions avec les variables reservees,
3. un cloisonnement instructions/donnees pour OpenAI et Claude,
4. un balisage clair des zones de donnees non fiables dans les prompts.

**Taches techniques** :
| ID | Tache | SP |
|----|-------|----|
| AI-6-T1 | Definir la liste des variables reservees et faire echouer toute collision `extraContext` | 1 |
| AI-6-T2 | Introduire une allowlist de cles `extraContext` par template ou par `TaskType` | 1 |
| AI-6-T3 | Faire evoluer l'adaptateur OpenAI pour separer instructions stables et donnees a analyser | 1 |
| AI-6-T4 | Faire evoluer l'adaptateur Claude HTTP avec une separation equivalente et un balisage explicite des donnees | 1 |
| AI-6-T5 | Adapter le prompt Claude CLI pour conserver le meme cloisonnement logique | 0.5 |
| AI-6-T6 | Ajouter des tests de regression sur collision de cles reservees et prompt assembly | 0.5 |

**Criteres d'acceptation** :
- CA-1 : Une cle `extraContext` ne peut plus ecraser `sanitizedSource`, `controllerRef`, `taskType` ou `estimatedTokens`.
- CA-2 : Les adaptateurs OpenAI et Claude distinguent explicitement instructions stables et donnees a analyser.
- CA-3 : Les templates utilisent un balisage explicite pour les sections de donnees non fiables.
- CA-4 : Une collision de variable reservee provoque un echec explicite, trace et testable.

**Story points** : 5
**Dependances** : AI-5
**Risques** : Moyen - changement transverse du montage des prompts, avec impact possible sur tous les cas d'usage IA.
**Agent lead** : `securite`

---

### AI-7 - Sorties structurees strictes cote fournisseurs

**Epic** : AI
**Description** :
Le parseur actuel accepte du texte autour du JSON et retombe sur un fallback texte brut si la
reponse n'est pas conforme. Pour des usages outilles, cela laisse trop de place aux sorties
partiellement valides ou detournees.

Ce ticket durcit le contrat fournisseur :
1. schema de sortie strict cote OpenAI,
2. schema ou mode structure equivalent cote Claude,
3. validation stricte avant mapping metier,
4. refus explicite plutot que fallback silencieux vers du texte libre.

**Taches techniques** :
| ID | Tache | SP |
|----|-------|----|
| AI-7-T1 | Definir les schemas de sortie attendus par `TaskType` | 1 |
| AI-7-T2 | Activer un mode de sortie structuree strict cote OpenAI | 1.5 |
| AI-7-T3 | Activer un mode ou garde-fou equivalent cote Claude HTTP et documenter le cas CLI | 1.5 |
| AI-7-T4 | Faire evoluer `LlmResponseParser` pour refuser les sorties hors schema plutot que fallback texte brut | 0.5 |
| AI-7-T5 | Ajouter des tests de contrat par fournisseur sur reponse nominale, refus et JSON invalide | 0.5 |

**Criteres d'acceptation** :
- CA-1 : Chaque `TaskType` critique dispose d'un schema de sortie explicite.
- CA-2 : OpenAI et Claude HTTP utilisent un mode de sortie structuree adapte au fournisseur.
- CA-3 : Une sortie hors schema produit un resultat degrade explicite, sans reinjection du texte brut en metier.
- CA-4 : Les tests couvrent au minimum OpenAI, Claude HTTP et le comportement degrade du CLI.

**Story points** : 5
**Dependances** : AI-6
**Risques** : Moyen - les differences de capacites entre fournisseurs peuvent imposer des comportements asymetriques.
**Agent lead** : `backend-hexagonal`

---

### AI-8 - Budget de contexte et minimisation des donnees envoyees

**Epic** : AI
**Description** :
Plusieurs cas d'usage injectent aujourd'hui de gros blocs de contexte (`sanitizedSource`,
`generatedArtifactDetails`, patterns de reference, extraits de regles) sans budget explicite
par type de tache. Cela augmente la surface de fuite et le risque de dilution des instructions.

Ce ticket introduit des budgets de contexte par `TaskType` et des strategies de reduction :
top-N patterns, snippets centres sur les regles, plafonds par champ, et suppression des blocs
non necessaires pour une tache donnee.

**Taches techniques** :
| ID | Tache | SP |
|----|-------|----|
| AI-8-T1 | Definir un budget de contexte par `TaskType` (chars, lignes ou tokens) | 1 |
| AI-8-T2 | Limiter `generatedArtifactDetails` et `projectReferencePatterns` selon le budget de la tache | 1 |
| AI-8-T3 | Ajouter des metriques ou logs techniques non sensibles sur le budget applique et les champs elides | 0.5 |
| AI-8-T4 | Ajouter des tests verifiant la reduction deterministe du contexte par tache | 0.5 |

**Criteres d'acceptation** :
- CA-1 : Un budget de contexte explicite existe pour les taches IA principales.
- CA-2 : Les prompts n'embarquent plus integralement les gros blocs de contexte quand une reduction equivalente existe.
- CA-3 : Le budget applique est observable sans logger le contenu des donnees.
- CA-4 : Les tests prouvent que la reduction est stable et compatible avec les prompts existants.

**Story points** : 3
**Dependances** : AI-5, AI-7
**Risques** : Faible a moyen - trop reduire peut degrader la qualite des suggestions si le budget n'est pas ajuste par tache.
**Agent lead** : `observabilite-exploitation`

---

## Plan de lots progressifs

### Sprint 1 â€” Fondations qualitÃ©

| Ticket | Titre | SP |
|--------|-------|----|
| QW-1   | Cartographie et audit (DONE) | 3 |
| QW-2   | Centralisation dictionnaire | 5 |
| AI-1   | SÃ©curisation logs LLM | 5 |
| AI-5   | Sanitisation de tout le contexte promptable | 8 |

**Objectif** : Ã‰liminer les fuites les plus directes, unifier le dictionnaire et supprimer le contexte promptable brut.
**Total sprint 1** : 21 SP

### Sprint 2 â€” Robustesse et reporting

| Ticket | Titre | SP |
|--------|-------|----|
| QW-3   | PÃ©rimÃ¨tre et isolation | 5 |
| QW-4   | Reporting sanitisation | 3 |
| AI-2   | Mode dry-run | 3 |
| AI-6   | Isolation des variables reservees et cloisonnement instruction/donnee | 5 |

**Objectif** : Renforcer la robustesse du pipeline, exposer le rapport, ajouter dry-run et fiabiliser l'assemblage des prompts.
**Total sprint 2** : 16 SP

### Sprint 3 â€” Couverture et industrialisation

| Ticket | Titre | SP |
|--------|-------|----|
| QW-5   | Fichiers non-Java | 8 |
| AI-3   | RÃ¨gles Semgrep externalisÃ©es | 5 |
| AI-4   | Tests chaÃ®ne complÃ¨te | 3 |
| AI-7   | Sorties structurees strictes cote fournisseurs | 5 |
| AI-8   | Budget de contexte et minimisation des donnees envoyees | 3 |

**Objectif** : Couvrir les fichiers non-Java, externaliser les rÃ¨gles, durcir les contrats fournisseurs et valider la chaÃ®ne complÃ¨te.
**Total sprint 3** : 24 SP

## Risques globaux

| Risque | Impact | ProbabilitÃ© | Mitigation |
|--------|--------|-------------|------------|
| IncompatibilitÃ© OpenRewrite / Spring Boot 4.0.3 | Ã‰levÃ© | Faible | VÃ©rifier la version dans `pom.xml` avant QW-3 |
| Performance Semgrep sur gros corpus multi-fichiers | Moyen | Ã‰levÃ©e | QW-5 : prÃ©voir un timeout configurable par fichier |
| Fuite de termes sensibles dans les logs LLM | Ã‰levÃ© | Moyenne | AI-1 : ticket prioritaire sprint 1 |
| RÃ©gression de la sanitisation sur les consommateurs existants | Ã‰levÃ© | Moyenne | QW-4 / QW-5 : tests de non-rÃ©gression obligatoires |
| RÃ¨gles Semgrep YAML invalides au chargement | Moyen | Faible | AI-3 : fallback sur rÃ¨gles inline + test de dÃ©marrage |
| Contexte promptable brut injecte via `extraContext` | Ã‰levÃ© | Ã‰levÃ©e | AI-5 : sanitiser tous les champs promptables avant rendu Mustache |
| Collision de variables reservees dans les templates | Ã‰levÃ© | Moyenne | AI-6 : allowlist + rejet explicite des collisions |
| Divergence de comportement entre OpenAI, Claude HTTP et Claude CLI | Moyen | Moyenne | AI-7 : tests de contrat fournisseurs et mode degrade explicite |
| Budget prompt reel sous-estime par rapport au `sanitizedSource` seul | Moyen | Haute | AI-8 : budget de contexte par tache et minimisation des donnees |

---

## Estimation consolidÃ©e

| Epic | Tickets | SP total |
|------|---------|----------|
| QW   | QW-1 Ã  QW-5 | 24 |
| AI   | AI-1 Ã  AI-8 | 37 |
| **Total** | **13 tickets** | **61 SP** |

---

*Document produit par l'agent `jira-estimation` le 2026-03-30.*
*RÃ©fÃ©rence : `agents/contracts.md`, `jira/backlog-phase-2.md`, analyse directe du module sanitisation.*

