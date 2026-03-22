# Rapport de transparence OpenAI

## Synthese

- **Date du rapport** : 2026-03-22
- **Perimetre** : JavaFXAuditStudio2 — agents produit et moteur d'analyse, skills `.codex`, prompts du repo
- **Conclusion** : Aucun appel OpenAI n'est implemente dans le code source du repo (backend Java, frontend Angular). La surface OpenAI est exclusivement documentaire et operatoire : elle correspond aux prompts des agents, aux skills `.codex`, et aux fichiers lus par les agents lors de leur execution dans Codex. Les donnees les plus sensibles sont les extraits de fichiers Java/FXML soumis a l'analyse et les contrats inter-agents.

---

## Surface de donnees exposee a OpenAI

| Categorie | Element | Source | Statut | Sensibilite | Justification |
| --- | --- | --- | --- | --- | --- |
| Instructions systeme | `AGENTS.md` — regles de gouvernance | `.codex/skills/*/SKILL.md` + `agents/catalog.md` | potentiel | moyenne | Charge en contexte par tous les agents qui citent `AGENTS.md` comme appui |
| Instructions systeme | `AGENTS-codex.md` — regles structurantes, stack cible | agents catalog | potentiel | moyenne | Document de gouvernance explicitement charge par les agents Codex |
| Contrats inter-agents | `agents/contracts.md`, `agents/orchestration.md` | catalog, orchestration | potentiel | moyenne | Injectes comme reference par les agents de cadrage |
| Catalogue d'agents | `agents/catalog.md` | tous les agents | potentiel | faible | Metadonnees d'agents : noms, missions, modeles |
| Selection modeles | `agents/model-selection.md` | tous les agents | potentiel | faible | Regles de selection GPT-5.4 / GPT-5.3-codex |
| Prompts repo | `.codex/prompts/analyze-one-screen.md` | invocation manuelle | potentiel | moyenne | Prompt d'analyse d'ecran JavaFX, injecte dans GPT lors d'une invocation |
| Prompts repo | `.codex/prompts/analyze-multiple-screens.md` | invocation manuelle | potentiel | moyenne | Idem, perimetre multi-ecrans |
| Prompts repo | `.codex/prompts/refactoring-focused-review.md` | invocation manuelle | potentiel | moyenne | Prompt de revue orientee refactoring |
| Prompts repo | `.codex/prompts/openai-transparency-report.md` | invocation manuelle | potentiel | faible | Prompt de generation du rapport de transparence lui-meme |
| Skills `.codex` | `javafx-screen-cartographer/SKILL.md` et all `javafx-*` | agents specialises | potentiel | moyenne | Instructions metier, objectifs, structures de sortie attendues |
| Skills `.codex` | `openai-transparency-reporter/SKILL.md` | `transparence-openai` | potentiel | faible | Instructions du present skill |
| Fichiers sources JavaFX | Fichiers `.java` et `.fxml` soumis via `SubmitAnalysisRequest` | `source-ingestion`, `fxml-analysis`, `controller-analysis` | potentiel | elevee | Extraits de code metier potentiellement proprietaires |
| Guide de refactoring | `guide_generique_refactoring_controller_javafx_spring.md` | agents d'architecture et de cadrage | potentiel | elevee | Specification fonctionnelle et architecturale principale, extraits longs |
| Backlog et estimation | `jira/backlog-phase-2.md`, `jira/refactoring-app-initial-backlog.md` | `jira-estimation` | potentiel | moyenne | Perimetre projet, stories, estimations |
| Parcours utilisateur | `docs/jas-01-parcours-utilisateur.md` | `product-owner-fonctionnel` | potentiel | faible | Description des parcours UX, non sensible |
| Sorties inter-agents | Cartographies, plans de migration, artefacts generes | agents d'analyse et de restitution | potentiel | elevee | Contenu derive des sources JavaFX analysees |

---

## Fichiers impliques

| Fichier | Role | Granularite | Statut | Agent(s) | Justification |
| --- | --- | --- | --- | --- | --- |
| `AGENTS.md` | Gouvernance generale | integral | potentiel | tous | Charge comme appui par la majorite des agents |
| `AGENTS-codex.md` | Gouvernance Codex + stack cible | integral | potentiel | agents Codex | Reference principale pour les agents Codex |
| `agents/catalog.md` | Catalogue des agents | integral | potentiel | tous | Repertoire de reference des missions et modeles |
| `agents/contracts.md` | Contrat de sortie inter-agent | integral | potentiel | tous | Structure les sorties de chaque agent |
| `agents/orchestration.md` | Sequencement des agents | integral | potentiel | gouvernance, architecture | Ordre et dependances d'execution |
| `agents/model-selection.md` | Regles de selection GPT | integral | potentiel | tous | Choix entre GPT-5.4 et GPT-5.3-codex |
| `agents/transparence-openai.md` | Mission de l'agent de transparence | integral | observe | transparence-openai | Lu pour produire ce rapport |
| `.codex/skills/openai-transparency-reporter/SKILL.md` | Instructions du skill | integral | observe | transparence-openai | Lu pour produire ce rapport |
| `.codex/skills/javafx-*/SKILL.md` (x9) | Instructions des skills JavaFX | integral | potentiel | agents moteur | Injectes lors des analyses JavaFX |
| `.codex/prompts/*.md` (x4) | Prompts d'invocation | integral | potentiel | selon invocation | Transmis integralement a GPT lors de l'invocation |
| `guide_generique_refactoring_controller_javafx_spring.md` | Spec fonctionnelle principale | integral (long) | potentiel | architecture-applicative, product-owner, analyste-regles | Document long, potentiellement transmis par extraits |
| `jira/backlog-phase-2.md` | Backlog phase 2 | integral | potentiel | jira-estimation, gouvernance | Perimetre projet detaille |
| Fichiers `.java` soumis | Sources JavaFX analysees | extraits ou integral | potentiel | source-ingestion, controller-analysis | Contenu metier proprietaire |
| Fichiers `.fxml` soumis | UI JavaFX analysee | integral | potentiel | fxml-analysis | Structure d'ecran |

---

## Raisonnement observable des agents

### transparence-openai

- **mission** : Auditer la surface de donnees susceptibles d'etre envoyees a OpenAI et produire ce rapport Markdown.
- **entrees observees** : `agents/transparence-openai.md`, `.codex/skills/openai-transparency-reporter/SKILL.md`, `agents/catalog.md`, `agents/model-selection.md`, `AGENTS.md`, `AGENTS-codex.md`, `.claude/settings.local.json`.
- **faits** : Aucun appel HTTP vers l'API OpenAI n'est present dans `backend/` ni `frontend/`. Tous les agents du catalog sont affectes a `GPT-5.4` ou `GPT-5.3-codex` — ces appels se font dans l'environnement Codex, pas dans le code du depot.
- **interpretations** : La surface OpenAI est le contexte injecte dans chaque agent Codex : prompts des skills, contrats, fichiers sources fournis par l'utilisateur.
- **hypotheses** : Les agents Codex transmettent integralement les fichiers SKILL.md et les fichiers lus depuis le repo dans leur fenetre de contexte.
- **incertitudes** : La taille exacte de la fenetre de contexte transmise par Codex a GPT n'est pas verifiable sans logs reseau. On ne sait pas si le guide complet est tronque ou transmis en entier.
- **decisions** : Classer les fichiers de gouvernance (`AGENTS.md`, contrats) comme `potentiel/moyenne` et les fichiers sources JavaFX comme `potentiel/elevee`.
- **sorties** : Le present rapport `TransparenceOpenAI/rapport-transparence-openai-2026-03-22.md`.

### source-ingestion

- **mission** : Lire et normaliser les fichiers sources JavaFX soumis par l'utilisateur.
- **entrees observees** : Chemins de fichiers `.java` et `.fxml` passes via `SubmitAnalysisRequest`.
- **faits** : L'adapter `FilesystemSourceReaderAdapter` lit les fichiers du systeme de fichiers local.
- **interpretations** : Le contenu integral des fichiers est lu et passe aux agents d'analyse suivants, qui le transmettent a GPT.
- **hypotheses** : Les fichiers sources peuvent contenir du code metier proprietaire.
- **incertitudes** : La granularite de transmission (extrait vs. integral) depend de l'implementation de chaque agent.
- **decisions** : Classement `potentiel/elevee` pour les fichiers sources JavaFX.
- **sorties** : Texte normalise transmis aux agents `fxml-analysis`, `controller-analysis`, `spring-analysis`.

---

## Limites et zones non observables

- Aucun log reseau de Codex n'est disponible pour confirmer le perimetre exact transmis a GPT.
- La troncature eventuelle du guide `guide_generique_refactoring_controller_javafx_spring.md` (document long) est inconnue.
- Les appels effectivement passes lors des sessions Codex precedentes (lots A, B, C) ne sont pas retraçables sans historique de session Codex.
- La politique de retention des donnees OpenAI/Codex (usage pour entrainement, conservation des prompts) est regie par les CGU OpenAI et n'est pas auditee ici.

---

## Recommandations

1. **Ne pas inclure de secrets ou credentials dans les fichiers sources JavaFX soumis a l'analyse.** Le `FilesystemSourceReaderAdapter` lit le contenu integral.
2. **Limiter la taille des fichiers transmis** aux agents d'analyse : privilegier des extraits cibles plutot que des fichiers sources complets si le code est sensible.
3. **Documenter dans chaque SKILL.md la granularite d'exposition attendue** (extrait / integral / metadonnees seulement).
4. **Renouveler ce rapport a chaque lot majeur** (lot B, C, D...) car la surface augmente avec chaque nouvel adapter et endpoint.
5. **Verifier les CGU OpenAI** concernant l'utilisation des prompts et du contexte pour l'entrainement, surtout si les sources JavaFX contiennent du code proprietaire.

---

## Verifications

- [x] Aucun appel OpenAI implemente dans `backend/` ni `frontend/` : confirme par inspection des sources Java.
- [x] Tous les fichiers lus pour produire ce rapport sont listes dans "Fichiers impliques".
- [x] Les niveaux de preuve (`observe` / `potentiel` / `hors-perimetre`) sont appliques.
- [x] Les agents sont nommes exactement comme dans `agents/catalog.md`.
- [x] Aucun appel reseau non verifie n'est invente.
- [x] Aucune chaine de pensee interne brute n'est presentee.
