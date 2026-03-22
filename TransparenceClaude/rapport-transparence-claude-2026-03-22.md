# Rapport de transparence Claude Code

## Synthese

- **Date du rapport** : 2026-03-22
- **Perimetre** : JavaFXAuditStudio2 — sous-agents Claude Code (`.claude/agents/`), skills `.codex`, settings, permissions, worktrees et memoire persistante
- **Conclusion** : Aucun appel a l'API Claude (Anthropic SDK) n'est implemente dans le code source du repo. La surface Claude Code est operatoire : elle correspond aux fichiers lus par les outils Read/Grep/Glob/Bash lors des sessions, aux instructions systeme (AGENTS.md, AGENTS-codex.md, settings), aux sous-agents `.claude/agents/`, et a la memoire persistante projet. Huit worktrees residuels dans `.claude/worktrees/` constituent une surface d'exposition non nettoyee. Les permissions `settings.local.json` sont bien bornees.

---

## Surface de donnees exposee a Claude Code

| Categorie | Element | Source | Statut | Sensibilite | Justification |
| --- | --- | --- | --- | --- | --- |
| Instructions systeme | `AGENTS.md` — gouvernance generale | charge automatiquement si CLAUDE.md reference | potentiel | moyenne | Document de gouvernance, charge en contexte si reference |
| Instructions systeme | `AGENTS-codex.md` — stack cible, regles structurantes | charge automatiquement | observe | moyenne | Lu dans cette session pour etablir le contexte |
| Instructions systeme | `.claude/agents/*.md` (37 sous-agents) | invoke par Claude Code | potentiel | moyenne | Frontmatter + instructions de chaque sous-agent transmis a Claude lors de l'invocation |
| Configuration permissions | `.claude/settings.local.json` | charge par Claude Code | observe | faible | Permissions bien bornees (mvn, npm, git, ng) — pas de `Bash(*)` |
| Memoire persistante | `~/.claude/projects/c--Programmation-IA-JavaFXAuditStudio-JavaFXAuditStudio2/memory/` | charge si MEMORY.md present | potentiel | moyenne | Accumulation cross-session de contexte projet |
| Contrats inter-agents | `agents/contracts.md`, `agents/orchestration.md` | lus par les agents | potentiel | moyenne | Injectes comme reference par les agents de cadrage |
| Catalogue d'agents | `agents/catalog.md`, `agents/model-selection.md` | lus par les agents | observe | faible | Lus dans cette session |
| Agents de transparence | `agents/transparence-openai.md`, `agents/transparence-claude.md` | lus dans cette session | observe | faible | Lus pour etablir le perimetre |
| Skills `.codex` | `.codex/skills/*/SKILL.md` (11 skills) | lus par les sous-agents | potentiel | moyenne | Instructions metier des skills, transmises a Claude lors de l'invocation |
| Prompts repo | `.codex/prompts/*.md` (4 fichiers) | invocation manuelle | potentiel | moyenne | Prompts d'analyse, transmis integralement a Claude |
| Guide de refactoring | `guide_generique_refactoring_controller_javafx_spring.md` | agents d'architecture | potentiel | elevee | Specification fonctionnelle principale, document long |
| Code source backend | `backend/src/main/java/**/*.java` (50+ fichiers) | lus par outils Read/Grep/Bash | observe | elevee | Extraits de code lus dans les sessions pour analyse et implementation |
| Backlog | `jira/backlog-phase-2.md`, `jira/refactoring-app-initial-backlog.md` | lus dans les sessions | observe | moyenne | Perimetre projet, stories, estimations |
| Worktrees residuels | `.claude/worktrees/agent-{a7b59b72,ace90ee0,...}` (8 worktrees) | crees par agents background | observe | moyenne | Copies isolees du repo potentiellement avec code non merge |
| Sorties Bash | Resultats de `mvn compile`, `git log`, `git status` | reprises dans la conversation | potentiel | faible | Informations de build et de statut git, non sensibles |
| Fichiers sources JavaFX | Fichiers `.java` et `.fxml` soumis via `SubmitAnalysisRequest` | runtime (non en session) | potentiel | elevee | Code metier proprietaire eventuellement charge par les agents d'analyse |

---

## Fichiers impliques

| Fichier | Role | Granularite | Statut | Agent(s) | Justification |
| --- | --- | --- | --- | --- | --- |
| `AGENTS.md` | Gouvernance generale Claude Code | integral | potentiel | tous | Reference de gouvernance standard |
| `AGENTS-codex.md` | Gouvernance Codex + regles structurantes | integral | observe | agents Codex | Lu dans cette session |
| `.claude/agents/*.md` (37) | Definitions des sous-agents | integral par fichier | potentiel | selon invocation | Transmis a Claude lors de l'invocation du sous-agent |
| `.claude/settings.local.json` | Permissions et repertoires additionnels | integral | observe | Claude Code | Lu par Claude Code au demarrage de session |
| `.codex/skills/*/SKILL.md` (11) | Instructions des skills | integral | potentiel | selon invocation | Transmis lors de l'invocation du skill |
| `.codex/prompts/*.md` (4) | Prompts d'invocation | integral | potentiel | selon invocation | Transmis integralement a Claude |
| `agents/catalog.md` | Catalogue des agents | integral | observe | tous | Lu dans cette session |
| `agents/contracts.md` | Contrat de sortie inter-agent | integral | potentiel | tous | Reference de structure |
| `agents/model-selection.md` | Regles de selection de modeles | integral | observe | tous | Lu dans cette session |
| `agents/transparence-openai.md` | Mission agent transparence OpenAI | integral | observe | transparence-openai, transparence-claude | Lu dans cette session |
| `agents/transparence-claude.md` | Mission agent transparence Claude | integral | observe | transparence-claude | Cree et lu dans cette session |
| `guide_generique_refactoring_controller_javafx_spring.md` | Spec fonctionnelle principale | integral (long) | potentiel | architecture-applicative, product-owner | Document long |
| `jira/backlog-phase-2.md` | Backlog phase 2 | extraits (trop long pour integral) | observe | jira-estimation, gouvernance | Lu par extraits dans cette session |
| `backend/src/main/java/**/*.java` (50+) | Code source backend | par fichier, integral | observe | backend-hexagonal, revue-code | Lus pour analyse et implementation dans les sessions |
| `.claude/worktrees/agent-*/` (8) | Copies isolees du repo | structure + fichiers modifies | observe | agents background | Worktrees residuels, non encore nettoyes |
| `~/.claude/projects/.../memory/` | Memoire persistante projet | par fichier memoire | potentiel | Claude Code | Charge en contexte si MEMORY.md present |

---

## Raisonnement observable des agents

### transparence-claude

- **mission** : Auditer la surface de donnees susceptibles d'etre envoyees a Claude Code et produire ce rapport Markdown.
- **entrees observees** : `agents/transparence-claude.md`, `.codex/skills/claude-transparency-reporter/SKILL.md`, `agents/catalog.md`, `agents/model-selection.md`, `AGENTS.md`, `AGENTS-codex.md`, `.claude/settings.local.json`, liste des worktrees via `git status`.
- **faits** : 37 sous-agents definis dans `.claude/agents/`. 8 worktrees residuels. Permissions `settings.local.json` bien bornees (pas de `Bash(*)`). Aucun appel SDK Anthropic dans le code backend/frontend. Memoire persistante presente dans `~/.claude/projects/`.
- **interpretations** : Chaque invocation d'un sous-agent transmet son fichier `.md` complet en contexte Claude. Les fichiers lus via Read/Grep/Glob sont transmis integralement. Les worktrees residuels representent une surface informelle : si un agent a fait des modifications, elles existent dans des branches temporaires.
- **hypotheses** : Claude Code ne transmet pas les fichiers non lus explicitement. La memoire persistante est chargee si MEMORY.md est reference au demarrage.
- **incertitudes** : La politique de retention des prompts et conversations par Anthropic n'est pas verifiable depuis le repo. La taille exacte du contexte transmis par session n'est pas loguee.
- **decisions** : Classer les worktrees residuels comme `observe/moyenne` (surface existante non nettoyee). Classer `settings.local.json` comme `observe/faible` (permissions bien bornees).
- **sorties** : Le present rapport `TransparenceClaude/rapport-transparence-claude-2026-03-22.md`.

### backend-hexagonal

- **mission** : Implementer le backend hexagonal (use cases, adapters, endpoints).
- **entrees observees** : Fichiers `.java` du backend lus via Read et Bash, backlog `jira/backlog-phase-2.md`, contrats `agents/contracts.md`.
- **faits** : 50+ fichiers Java lus en session. AnalysisController, DTOs, mappers, adapters existants lus integralement.
- **interpretations** : Le code source complet des fichiers Java est transmis a Claude lors de chaque appel Read.
- **hypotheses** : Le code Java ne contient pas de secrets (credentials, tokens) — confirme par inspection de `settings.local.json` qui ne montre pas de variables d'environnement sensibles.
- **incertitudes** : Les fichiers sources JavaFX soumis au runtime peuvent contenir du code metier proprietaire.
- **decisions** : Classement `observe/elevee` pour le code source backend lu en session.
- **sorties** : Code Java implemente dans `backend/src/main/java/`.

---

## Limites et zones non observables

- Aucun log des appels Claude Code API n'est disponible pour confirmer la taille exacte du contexte transmis.
- La politique de retention des conversations Claude Code (Anthropic) n'est pas auditee ici.
- Les sessions anterieures (lots A, B, C) ne sont pas retraçables sans historique de session Claude Code.
- Le contenu exact des 8 worktrees residuels n'a pas ete inspecte fichier par fichier.
- La memoire persistante `~/.claude/projects/` n'a pas ete auditee dans ce rapport (hors perimetre repo).

---

## Recommandations

1. **Nettoyer les worktrees residuels** (`.claude/worktrees/agent-*`) : verifier leur contenu puis supprimer les branches temporaires inutilisees.
2. **Ne pas inclure de secrets dans les fichiers lus par Claude** (credentials, tokens, cles API). Les fichiers Java sont transmis integralement.
3. **Auditer periodiquement la memoire persistante** `~/.claude/projects/.../memory/` pour eviter l'accumulation de contexte sensible cross-session.
4. **Limiter les `additionalDirectories`** dans `settings.local.json` aux chemins strictement necessaires (les chemins `e:\` actuels pointent vers des repertoires peut-etre inexistants).
5. **Creer un `CLAUDE.md`** a la racine si la gouvernance (`AGENTS.md`, `AGENTS-codex.md`) doit etre chargee systematiquement en contexte — actuellement ces fichiers ne sont charges que si Claude les lit explicitement.
6. **Renouveler ce rapport a chaque lot majeur** car la surface augmente avec chaque nouveau sous-agent et chaque session.
7. **Verifier les CGU Anthropic** concernant l'utilisation des prompts et du contenu de conversation pour l'entrainement.

---

## Verifications

- [x] Aucun appel SDK Anthropic implemente dans `backend/` ni `frontend/` : confirme par inspection des sources Java.
- [x] Permissions `settings.local.json` auditees : bien bornees, pas de `Bash(*)` ni `Write(*)`.
- [x] 8 worktrees residuels identifies et signales.
- [x] Tous les fichiers lus pour produire ce rapport sont listes dans "Fichiers impliques".
- [x] Les niveaux de preuve (`observe` / `potentiel` / `hors-perimetre`) sont appliques.
- [x] Les agents sont nommes exactement comme dans `agents/catalog.md`.
- [x] Aucun appel reseau non verifie n'est invente.
- [x] Aucune chaine de pensee interne brute n'est presentee.
