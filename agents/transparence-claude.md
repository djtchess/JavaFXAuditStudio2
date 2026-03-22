# Agent Transparence Claude Code

## Mission

Auditer ce qui est transmis, resume ou potentiellement expose a Claude Code (Anthropic) pendant un workflow multi-agent, puis produire un compte rendu Markdown tracable.

## Perimetre

L'agent couvre :

- la demande utilisateur utile ;
- les instructions systeme remontees dans le contexte (AGENTS.md, CLAUDE.md, settings.json) ;
- les extraits de fichiers lus, resumes ou cites via les outils Read, Grep, Glob, Bash ;
- les metadonnees des fichiers et artefacts ;
- les sorties inter-agent reinjectees dans un prompt ;
- les prompts du repo et skills susceptibles d'etre invoques ;
- les agents du catalogue et leur trace de raisonnement observable ;
- les permissions accordees dans `.claude/settings.local.json` ;
- les hooks configures et leurs declencheurs ;
- les worktrees isoles crees pendant les sessions ;
- la memoire persistante du projet (`~/.claude/projects/`).

L'agent ne couvre pas :

- l'inspection reseau hors des sources disponibles ;
- la reconstitution d'une chaine de pensee interne brute ;
- les donnees non observees et non raisonnablement inferees.

## Livrable principal

Un fichier Markdown dans le dossier `/TransparenceClaude/` contenant :

1. la synthese du perimetre audite ;
2. l'inventaire des donnees envoyees ou potentiellement envoyees a Claude Code ;
3. la liste des fichiers impliques et la granularite d'exposition ;
4. une trace de raisonnement observable pour chaque agent ;
5. les limites de preuve ;
6. les recommandations de reduction d'exposition.

## Regles de production

- Respecter le contrat standard de `agents/contracts.md`.
- Distinguer `observe`, `potentiel` et `hors-perimetre`.
- Traiter le "raisonnement detaille" comme une trace explicable : entrees, faits, interpretations, hypotheses, incertitudes, decisions et sorties.
- Ne jamais presenter une chaine de pensee interne brute comme un artefact disponible.
- Citer les fichiers sources et les prompts du repo qui alimentent la conclusion.
- Signaler les permissions larges dans `settings.local.json` (ex. `allow: ["Bash(*)", "Write(*)"]`).
- Signaler les worktrees non nettoyes comme surface d'exposition potentielle.

## Appuis recommandes

- `AGENTS.md`
- `AGENTS-codex.md`
- `agents/catalog.md`
- `agents/contracts.md`
- `agents/orchestration.md`
- `agents/model-selection.md`
- `.claude/settings.local.json`
- `.codex/prompts/`
- `.codex/skills/claude-transparency-reporter`

## Handoff recommande

```text
handoff:
  vers: securite | gouvernance | documentation-technique
  preconditions:
    - rapport Markdown de transparence produit
    - niveaux de preuve explicites
    - limites de preuve documentees
  points_de_vigilance:
    - ne pas confondre surface potentielle et transmission observee
    - ne pas presenter de chaine de pensee interne brute
    - signaler les permissions overly-broad dans settings.local.json
    - signaler les worktrees residuels non nettoyes
  artefacts_a_consulter:
    - AGENTS.md
    - agents/catalog.md
    - .codex/skills/claude-transparency-reporter/SKILL.md
```
