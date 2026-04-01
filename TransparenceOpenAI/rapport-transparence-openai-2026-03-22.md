# Rapport de transparence OpenAI

## Synthese

- **Date du rapport** : 2026-04-01
- **Perimetre** : JavaFXAuditStudio2 - agents Codex, skills `.codex`, prompts du repo, backend d'enrichissement IA et fichiers relus pour `JAS-ACT-006`
- **Conclusion** : Le repo n'est plus dans un etat "surface OpenAI uniquement documentaire". Deux chemins runtime OpenAI existent dans le backend : `OpenAiGpt54AiEnrichmentAdapter` pour l'API HTTP OpenAI et `OpenAiCodexCliAiEnrichmentAdapter` pour le CLI Codex local. Le provider par defaut de `application.properties` reste `claude-code-cli`, donc la surface OpenAI backend est implementee mais non active par defaut dans la configuration locale observee. A cela s'ajoute la surface OpenAI deja observee cote Codex : instructions de gouvernance, backlog, skills, prompts, fichiers lus et sorties d'outils reprises dans la session.

---

## Surface de donnees exposee a OpenAI

| Categorie | Element | Source | Statut | Sensibilite | Justification |
| --- | --- | --- | --- | --- | --- |
| Demande utilisateur | Conversation courante autour de `JAS-ACT-006` | session Codex | observe | moyenne | Reprise dans le contexte de travail du modele |
| Instructions systeme | `AGENTS.md` | repo | observe | moyenne | Lu dans la session et source de gouvernance principale |
| Instructions systeme | `AGENTS-claude.md` | repo | observe | moyenne | Lu pendant le realignement de gouvernance de `JAS-ACT-006` |
| Contrats inter-agents | `agents/catalog.md`, `agents/contracts.md`, `agents/orchestration.md`, `agents/model-selection.md` | repo | observe | moyenne | Pilotage des agents, contrats de sortie et selection de modeles |
| Skills et prompts Codex | `.codex/skills/*/SKILL.md`, `.codex/prompts/*.md` | `.codex/` | potentiel | moyenne | Transmis quand un skill ou un prompt est invoque |
| Prompt templates backend | `backend/src/main/resources/prompts/*.mustache` | backend | observe | elevee | Rendues par le backend puis transmises au fournisseur IA actif |
| Configuration runtime IA | `backend/src/main/resources/application.properties`, `AiEnrichmentProperties.java` | backend | observe | moyenne | Definissent le provider, les timeouts, les budgets et les credentials via variables d'environnement |
| Chemin runtime OpenAI HTTP | `OpenAiGpt54AiEnrichmentAdapter.java` | backend | observe | elevee | Poste le prompt vers `https://api.openai.com/v1/chat/completions` |
| Chemin runtime OpenAI CLI | `OpenAiCodexCliAiEnrichmentAdapter.java` | backend | observe | elevee | Passe le prompt a `codex exec` et recupere la derniere reponse du CLI |
| Sources metier analysees | Fichiers `.java`, `.fxml` et artefacts derives lus pour l'enrichissement | runtime backend | potentiel | elevee | Contenu proprietaire potentiellement sanitise puis envoye au fournisseur actif |
| Sorties d'outils | `git diff`, `git status`, resultats de tests et recherches `rg` repris dans la session | session Codex | observe | faible | Metadonnees de build et de travail integrees au contexte |
| Backlog et gouvernance documentaire | `jira/backlog-restant-a-faire.md`, `docs/sanitization-audit-gouvernance.md` | repo | observe | moyenne | Utilises pour realigner le ticket `JAS-ACT-006` |

---

## Fichiers impliques

| Fichier | Role | Granularite | Statut | Agent(s) | Justification |
| --- | --- | --- | --- | --- | --- |
| `AGENTS.md` | Gouvernance generale | integral | observe | tous | Lu pour cadrer les regles du depot |
| `AGENTS-claude.md` | Gouvernance Claude partagee | integral | observe | transparence-openai, transparence-claude | Lu pour corriger les references de pilotage |
| `agents/catalog.md` | Catalogue des agents | integral | observe | transparence-openai | Confirme les noms, roles et appuis |
| `agents/contracts.md` | Contrat de sortie inter-agent | integral | observe | transparence-openai | Fixe la structure de trace observable |
| `agents/orchestration.md` | Sequencement des agents | integral | observe | transparence-openai | Confirme quand rejouer la transparence |
| `agents/model-selection.md` | Selection GPT-5.4 / GPT-5.3-codex | integral | observe | transparence-openai | Confirme l'usage OpenAI cote agents Codex |
| `.codex/skills/openai-transparency-reporter/SKILL.md` | Skill de transparence OpenAI | integral | observe | transparence-openai | Lu pour rejouer le rapport |
| `.codex/prompts/*.md` (x4) | Prompts repo | integral | potentiel | selon invocation | Peuvent etre injectes tels quels dans Codex |
| `backend/src/main/resources/prompts/*.mustache` (x7) | Prompts backend rendus avant appel LLM | integral par template | observe | backend-hexagonal | Surface de prompt runtime vers le fournisseur actif |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/ai/OpenAiGpt54AiEnrichmentAdapter.java` | Appel HTTP OpenAI | integral | observe | backend-hexagonal | Chemin OpenAI explicite implemente dans le code |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/ai/OpenAiCodexCliAiEnrichmentAdapter.java` | Appel Codex CLI local | integral | observe | backend-hexagonal | Chemin OpenAI local sans cle API dans le code |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/AiEnrichmentOrchestraConfiguration.java` | Assemblage des adapters IA | integral | observe | backend-hexagonal | Montre que les adapters OpenAI et Claude sont tous relies |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/AiEnrichmentProperties.java` | Configuration provider/credentials/budgets | integral | observe | backend-hexagonal | Definit les fournisseurs supportes et les credentials attendus |
| `backend/src/main/resources/application.properties` | Configuration runtime par defaut | integral | observe | backend-hexagonal, gouvernance | Montre que le provider local par defaut est `claude-code-cli` |
| `jira/backlog-restant-a-faire.md` | Backlog Jira consolide | integral | observe | gouvernance, transparence-openai | Source unique du ticket `JAS-ACT-006` |
| `docs/sanitization-audit-gouvernance.md` | Trace documentaire de sanitisation | integral | observe | gouvernance, transparence-openai | Reaiguillage vers le backlog unique et les rapports de transparence |
| Fichiers `.java` / `.fxml` soumis | Sources JavaFX analysees | extraits ou integral | potentiel | source-ingestion, controller-analysis, fxml-analysis | Contenu metier proprietaire potentiellement transmis apres sanitisation |

---

## Raisonnement observable des agents

### transparence-openai

- **mission** : Rejouer le rapport de transparence OpenAI dans le cadre de `JAS-ACT-006`.
- **entrees observees** : `AGENTS.md`, `AGENTS-claude.md`, `agents/catalog.md`, `agents/contracts.md`, `agents/orchestration.md`, `agents/model-selection.md`, `.codex/skills/openai-transparency-reporter/SKILL.md`, `jira/backlog-restant-a-faire.md`, `backend/src/main/resources/application.properties`, `AiEnrichmentProperties.java`, `AiEnrichmentOrchestraConfiguration.java`, `OpenAiGpt54AiEnrichmentAdapter.java`, `OpenAiCodexCliAiEnrichmentAdapter.java`, `backend/src/main/resources/prompts/*.mustache`.
- **faits** : Un chemin HTTP OpenAI et un chemin CLI Codex existent dans le backend. Le frontend n'implemente pas de client OpenAI direct observe. Le provider par defaut dans `application.properties` est `claude-code-cli`.
- **interpretations** : La surface OpenAI combine une exposition de session Codex deja observee et une exposition runtime backend qui depend du provider choisi.
- **hypotheses** : Si le provider est bascule vers `openai-gpt54` ou `openai-codex-cli`, les bundles sanitises, prompts rendus et metadonnees associees seront transmis a OpenAI ou au CLI Codex.
- **incertitudes** : Le provider reel en environnement partage ou production n'est pas observable depuis le repo seul. Aucun log reseau Codex/OpenAI n'est disponible dans le depot.
- **decisions** : Classer les adapters OpenAI, la config IA et les templates Mustache comme `observe`. Conserver les sources JavaFX comme `potentiel/elevee` car leur transmission depend du workflow runtime.
- **sorties** : Le present rapport `TransparenceOpenAI/rapport-transparence-openai-2026-03-22.md`, remis a jour sans changer son chemin.

### backend-hexagonal

- **mission** : Assembler le sous-systeme d'enrichissement IA et router les demandes vers le bon fournisseur.
- **entrees observees** : `AiEnrichmentOrchestraConfiguration.java`, `AiEnrichmentProperties.java`, `application.properties`, prompt templates Mustache, adapters OpenAI/Claude.
- **faits** : Le routage backend instancie `ClaudeCodeAiEnrichmentAdapter`, `OpenAiGpt54AiEnrichmentAdapter`, `ClaudeCodeCliAiEnrichmentAdapter` et `OpenAiCodexCliAiEnrichmentAdapter`. Les templates backend sont rendus avant l'appel fournisseur.
- **interpretations** : Le meme contexte fonctionnel sanitise peut partir vers OpenAI ou vers Claude selon la configuration effective du provider.
- **hypotheses** : Les fichiers sources et artefacts lus pour l'enrichissement peuvent contenir de la logique metier proprietaire a forte sensibilite.
- **incertitudes** : Les variables d'environnement reelles (`OPENAI_API_KEY`, login Codex CLI) ne sont pas observables depuis le repo.
- **decisions** : Garder en scope de transparence la configuration provider, les templates Mustache et les adapters de transport eux-memes.
- **sorties** : Chemins de transport LLM relies a l'hexagone backend et repertories dans ce rapport.

### source-ingestion

- **mission** : Lire les sources JavaFX qui nourrissent ensuite l'enrichissement et la generation.
- **entrees observees** : chemins de fichiers `.java` et `.fxml` fournis au backend, references de lecture source dans l'orchestration IA.
- **faits** : Les flux d'enrichissement et de generation s'appuient sur une lecture de fichiers source et sur la sanitisation de contexte avant appel fournisseur.
- **interpretations** : Les donnees les plus sensibles cote OpenAI restent les extraits de code et d'artefacts derives, pas les metadonnees de gouvernance.
- **hypotheses** : La granularite d'exposition depend du cas d'usage appele (`enrich`, `review`, `generate`, `refine`, `coherence`).
- **incertitudes** : Le volume exact transmis par tache n'est pas observable sans traces runtime detaillees.
- **decisions** : Maintenir la classification `potentiel/elevee` pour les sources JavaFX et artefacts fonctionnels.
- **sorties** : Donnees techniques et metier susceptibles d'alimenter un appel OpenAI si le provider correspondant est actif.

---

## Limites et zones non observables

- Aucun log reseau Codex ni OpenAI n'est disponible dans le repo pour prouver la taille exacte du contexte transmis.
- Le provider `ai.enrichment.provider` peut etre surcharge par profil ou variable d'environnement hors du depot.
- La politique de retention des conversations Codex/OpenAI n'est pas auditable depuis ces sources.
- Le chemin de fichier du rapport n'a pas ete renomme ; seule sa date interne et son contenu ont ete rejoues.
- Les volumes exacts de code JavaFX transmis par cas d'usage restent non verifiables sans instrumentation runtime supplementaire.

---

## Recommandations

1. Rejouer ce rapport a chaque evolution de `ai.enrichment.provider`, de la liste des providers supportes ou des templates `backend/src/main/resources/prompts/*.mustache`.
2. Documenter explicitement, par environnement, si le provider actif est `claude-code-cli`, `claude-code`, `openai-gpt54` ou `openai-codex-cli`.
3. Conserver les prompts complets hors des logs en environnement partage, meme quand un niveau DEBUG local est active.
4. Eviter tout secret ou extrait de donnees sensibles dans les sources JavaFX soumises a l'enrichissement ; la sanitisation doit rester un garde-fou, pas un permis d'exposition.
5. Maintenir la protection `JAS-ACT-005` sur les endpoints sensibles quand l'application est exposee hors localhost, afin de reduire l'exposition passive des flux IA et d'observabilite.

---

## Verifications

- [x] Un chemin OpenAI HTTP existe dans le backend (`OpenAiGpt54AiEnrichmentAdapter`).
- [x] Un chemin OpenAI CLI existe dans le backend (`OpenAiCodexCliAiEnrichmentAdapter`).
- [x] Aucun client OpenAI direct n'a ete observe dans le frontend Angular.
- [x] Les templates backend `backend/src/main/resources/prompts/*.mustache` sont integres au perimetre du rapport.
- [x] Le provider par defaut observe dans `application.properties` est `claude-code-cli`, pas un provider OpenAI.
- [x] Les niveaux de preuve `observe` / `potentiel` restent distingues.
- [x] Aucune chaine de pensee interne brute n'est exposee.
