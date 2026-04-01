# Rapport de transparence Claude Code

## Synthese

- **Date du rapport** : 2026-04-01
- **Perimetre** : JavaFXAuditStudio2 - session Claude Code/Codex partagee, sous-agents `.claude/agents/`, skills `.codex`, settings, memoire persistante et chemins runtime Claude du backend
- **Conclusion** : La surface Claude n'est plus limitee a l'usage operatoire du client Claude Code. Le repo embarque aussi deux chemins runtime Claude dans le backend : `ClaudeCodeAiEnrichmentAdapter` pour l'API HTTP Anthropic et `ClaudeCodeCliAiEnrichmentAdapter` pour le CLI `claude` local. Le provider par defaut dans `application.properties` est `claude-code-cli`, donc une exposition Claude runtime est active par defaut dans la configuration locale observee. A la date du `2026-04-01`, aucun worktree residuel n'est detecte sous `.claude/worktrees/`. Les permissions de `.claude/settings.local.json` restent bornees, meme si des `additionalDirectories` externes existent encore.

---

## Surface de donnees exposee a Claude Code

| Categorie | Element | Source | Statut | Sensibilite | Justification |
| --- | --- | --- | --- | --- | --- |
| Demande utilisateur | Conversation courante autour de `JAS-ACT-006` | session | observe | moyenne | Historique repris dans le contexte de l'agent |
| Instructions systeme | `AGENTS.md` | repo | observe | moyenne | Gouvernance principale lue dans la session |
| Instructions systeme | `AGENTS-claude.md` | repo | observe | moyenne | Gouvernance Claude partagee reellement presente dans le repo |
| Sous-agents Claude | `.claude/agents/*.md` (x40) | `.claude/agents/` | observe | moyenne | Definitions completes des sous-agents disponibles dans le repo |
| Configuration permissions | `.claude/settings.local.json` | `.claude/` | observe | faible | Permissions bornees et `additionalDirectories` explicites |
| Memoire persistante | `~/.claude/projects/c--Programmation-IA-JavaFXAuditStudio-JavaFXAuditStudio2/memory/` | environnement Claude | potentiel | moyenne | Peut etre remontee dans le contexte si Claude la charge |
| Contrats inter-agents | `agents/catalog.md`, `agents/contracts.md`, `agents/orchestration.md`, `agents/model-selection.md` | repo | observe | moyenne | Pilotage, structure des sorties et selection des modeles |
| Skills et prompts | `.codex/skills/*/SKILL.md` (x13), `.codex/prompts/*.md` (x4) | `.codex/` | potentiel | moyenne | Peuvent etre charges par Claude Code ou reutilises depuis le repo |
| Prompt templates backend | `backend/src/main/resources/prompts/*.mustache` | backend | observe | elevee | Rendus puis transmis au fournisseur Claude actif |
| Chemin runtime Anthropic HTTP | `ClaudeCodeAiEnrichmentAdapter.java` | backend | observe | elevee | Appel HTTP explicite vers l'API Anthropic Messages |
| Chemin runtime Claude CLI | `ClaudeCodeCliAiEnrichmentAdapter.java` | backend | observe | elevee | Appel local au CLI `claude` avec prompt sur stdin |
| Configuration runtime IA | `backend/src/main/resources/application.properties`, `AiEnrichmentProperties.java` | backend | observe | moyenne | Definissent le provider par defaut et la strategie d'authentification |
| Worktrees Claude | `.claude/worktrees/` | `.claude/` | observe | faible | Aucun repertoire agent detecte au `2026-04-01` |
| Code source et artefacts | `backend/src/main/java/**/*.java`, fichiers `.java` / `.fxml` soumis au runtime | repo et runtime | observe/potentiel | elevee | Code lu en session et sources metier potentiellement transmises apres sanitisation |
| Sorties d'outils | `git status`, `git diff`, recherches `rg`, logs de test repris dans la session | session | observe | faible | Sorties techniques integrees a la conversation |
| Backlog et rapports | `jira/backlog-restant-a-faire.md`, rapports `TransparenceOpenAI/` et `TransparenceClaude/` | repo | observe | moyenne | Artefacts de gouvernance et de transparence rejoues dans `JAS-ACT-006` |

---

## Fichiers impliques

| Fichier | Role | Granularite | Statut | Agent(s) | Justification |
| --- | --- | --- | --- | --- | --- |
| `AGENTS.md` | Gouvernance generale | integral | observe | tous | Lu pour appliquer les regles du depot |
| `AGENTS-claude.md` | Gouvernance Claude partagee | integral | observe | transparence-claude | Lu et realigne dans ce ticket |
| `.claude/agents/*.md` (x40) | Definitions des sous-agents Claude | integral par fichier | observe | selon invocation | Surface directe de contexte pour les sous-agents Claude |
| `.claude/settings.local.json` | Permissions et repertoires additionnels | integral | observe | transparence-claude | Audite pour qualifier la surface d'exposition |
| `agents/catalog.md` | Catalogue des agents | integral | observe | transparence-claude | Confirme noms, roles et appuis |
| `agents/contracts.md` | Contrat de sortie inter-agent | integral | observe | transparence-claude | Structure la trace observable |
| `agents/orchestration.md` | Orchestration des agents | integral | observe | transparence-claude | Confirme quand rejouer la transparence |
| `agents/model-selection.md` | Mapping des modeles | integral | observe | transparence-claude | Confirme l'usage Claude cote gouvernance |
| `.codex/skills/claude-transparency-reporter/SKILL.md` | Skill de transparence Claude | integral | observe | transparence-claude | Lu pour rejouer ce rapport |
| `.codex/prompts/*.md` (x4) | Prompts repo | integral | potentiel | selon invocation | Peuvent etre injectes dans une session Claude |
| `backend/src/main/resources/prompts/*.mustache` (x7) | Prompts backend | integral par template | observe | backend-hexagonal | Surface de prompt vers le fournisseur Claude actif |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/ai/ClaudeCodeAiEnrichmentAdapter.java` | Appel HTTP Anthropic | integral | observe | backend-hexagonal | Chemin runtime Claude explicite |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/ai/ClaudeCodeCliAiEnrichmentAdapter.java` | Appel CLI Claude local | integral | observe | backend-hexagonal | Chemin runtime Claude local sans SDK |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/AiEnrichmentOrchestraConfiguration.java` | Assemblage des adapters IA | integral | observe | backend-hexagonal | Montre que Claude HTTP et Claude CLI sont relies |
| `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/AiEnrichmentProperties.java` | Configuration provider/credentials | integral | observe | backend-hexagonal | Definit les providers et credentials attendus |
| `backend/src/main/resources/application.properties` | Configuration runtime par defaut | integral | observe | backend-hexagonal, gouvernance | Montre que `claude-code-cli` est le provider local par defaut |
| `jira/backlog-restant-a-faire.md` | Backlog Jira consolide | integral | observe | gouvernance, transparence-claude | Point d'entree unique de `JAS-ACT-006` |
| `TransparenceClaude/rapport-transparence-claude-2026-03-22.md` | Rapport Claude rejoue | integral | observe | transparence-claude | Artefact final du present ticket |
| `TransparenceOpenAI/rapport-transparence-openai-2026-03-22.md` | Rapport OpenAI rejoue | integral | observe | transparence-openai, transparence-claude | Utilise pour coherer la gouvernance de transparence |
| `.claude/worktrees/` | Repertoire de worktrees agents | structure | observe | transparence-claude | Verifie vide au `2026-04-01` |

---

## Raisonnement observable des agents

### transparence-claude

- **mission** : Rejouer le rapport de transparence Claude dans le cadre de `JAS-ACT-006`.
- **entrees observees** : `AGENTS.md`, `AGENTS-claude.md`, `.claude/settings.local.json`, `.claude/agents/`, `agents/catalog.md`, `agents/contracts.md`, `agents/orchestration.md`, `agents/model-selection.md`, `jira/backlog-restant-a-faire.md`, `application.properties`, `AiEnrichmentProperties.java`, `AiEnrichmentOrchestraConfiguration.java`, traces de recherche sur les adapters Claude/OpenAI.
- **faits** : Le repo contient 40 sous-agents Claude, 13 skills `.codex`, 4 prompts `.codex`, aucun worktree residuel sous `.claude/worktrees/`, un chemin HTTP Anthropic et un chemin CLI Claude dans le backend, et un provider par defaut `claude-code-cli` dans `application.properties`.
- **interpretations** : La surface Claude est double : contexte de session/outillage Claude Code d'une part, flux runtime backend par defaut vers Claude d'autre part.
- **hypotheses** : Les bundles sanitises et les artefacts d'analyse peuvent contenir de la logique metier proprietaire meme apres reduction de surface.
- **incertitudes** : La memoire persistante exacte chargee par Claude Code et la retention de conversation cote Anthropic ne sont pas auditables depuis le repo.
- **decisions** : Corriger les references de gouvernance vers `AGENTS-claude.md`, reclasser les worktrees en "aucun residuel observe" et maintenir les adapters Claude backend en `observe/elevee`.
- **sorties** : Le present rapport `TransparenceClaude/rapport-transparence-claude-2026-03-22.md`, rejoue sans changer son chemin.

### backend-hexagonal

- **mission** : Assembler le sous-systeme d'enrichissement IA et router les cas d'usage vers le bon fournisseur.
- **entrees observees** : `AiEnrichmentOrchestraConfiguration.java`, `AiEnrichmentProperties.java`, `application.properties`, templates backend, classes d'adapters Claude/OpenAI.
- **faits** : Le routage backend relie simultanement les adapters Claude HTTP, Claude CLI, OpenAI HTTP et Codex CLI. Les prompts backend sont rendus depuis `backend/src/main/resources/prompts/*.mustache` avant l'appel fournisseur.
- **interpretations** : Le meme socle de donnees sanitisees peut etre dirige vers Claude ou OpenAI ; la configuration courante favorise Claude.
- **hypotheses** : Les cas d'usage `enrich`, `review`, `generate`, `refine` et `coherence` reexposent des artefacts fonctionnels a forte sensibilite.
- **incertitudes** : Les variables d'environnement reelles, credentials et surcharges de profil ne sont pas observables depuis le repo.
- **decisions** : Inclure la configuration provider, les templates et les adapters backend dans le coeur du rapport de transparence Claude.
- **sorties** : Cartographie des flux Claude runtime exposes par le backend.

### gouvernance

- **mission** : Maintenir un pilotage documentaire coherent avec le depot reel.
- **entrees observees** : `jira/backlog-restant-a-faire.md`, `docs/sanitization-audit-gouvernance.md`, rapports de transparence, `agents/catalog.md`, `agents/transparence-claude.md`.
- **faits** : Le backlog Jira est devenu la seule source Markdown conservee sous `./jira`. Les rapports de transparence precedents etaient en retard sur les providers, les prompts backend, les comptes d'agents/skills et les worktrees Claude.
- **interpretations** : Sans mise a jour conjointe du backlog et des rapports de transparence, `JAS-ACT-006` resterait incomplet et laisserait des contre-verites dans la doc de pilotage.
- **hypotheses** : Les prochains tickets frontend et observabilite s'appuieront sur ces artefacts comme reference de depart.
- **incertitudes** : D'autres documents historiques hors perimetre du ticket peuvent encore contenir des references anciennes.
- **decisions** : Marquer `JAS-ACT-006` comme cloture apres realignement du backlog, des rapports OpenAI/Claude et des references `AGENTS-claude.md`.
- **sorties** : Base documentaire coherente pour enchainer sur `JAS-ACT-007`.

---

## Limites et zones non observables

- Aucun log reseau Anthropic/Claude Code n'est disponible pour mesurer le contexte exact transmis.
- La memoire persistante `~/.claude/projects/.../memory/` n'a pas ete lue dans ce ticket ; elle reste une surface `potentiel`.
- Le provider effectif peut etre surcharge hors du repo par profil ou variables d'environnement.
- Le chemin de fichier du rapport n'a pas ete renomme ; seule sa date interne et son contenu ont ete rejoues.
- L'existence d'`additionalDirectories` dans `.claude/settings.local.json` ne prouve pas qu'ils sont tous utilises dans chaque session.

---

## Recommandations

1. Maintenir `.claude/worktrees/` vide entre les lots ou documenter explicitement toute exception temporaire.
2. Rejouer ce rapport a chaque ajout de sous-agent `.claude/agents/`, de skill `.codex`, de prompt backend ou de changement de provider IA.
3. Verifier regulierement la pertinence des `additionalDirectories` dans `.claude/settings.local.json`, en particulier les chemins externes `e:\...`.
4. Ne jamais inclure de secrets ni de donnees non sanitisees dans les fichiers lus ou resumes en session Claude.
5. Documenter par environnement si l'application utilise `claude-code-cli` ou `claude-code` afin de distinguer clairement l'exposition locale CLI et l'exposition HTTP Anthropic.

---

## Verifications

- [x] Le repo contient 40 sous-agents sous `.claude/agents/`.
- [x] Le repo contient 13 skills sous `.codex/skills/` et 4 prompts sous `.codex/prompts/`.
- [x] Aucun worktree residuel n'a ete detecte sous `.claude/worktrees/` au `2026-04-01`.
- [x] Un chemin Claude HTTP et un chemin Claude CLI existent dans le backend.
- [x] Aucun client Anthropic direct n'a ete observe dans le frontend Angular.
- [x] Les permissions de `.claude/settings.local.json` ont ete relues et restent bornees.
- [x] Les niveaux de preuve `observe` / `potentiel` sont distingues.
- [x] Aucune chaine de pensee interne brute n'est exposee.
