# Regles de reporting — Claude Code

## Niveaux de preuve

| Niveau | Definition | Quand l'utiliser |
| --- | --- | --- |
| `observe` | La donnee ou le fichier apparait explicitement dans les fichiers lus ou dans la session | Quand une preuve directe existe |
| `potentiel` | La donnee pourrait etre transmise selon le workflow ou le skill, sans preuve d'execution dans la session | Quand la surface est plausible mais non constatee |
| `hors-perimetre` | La donnee n'est ni observee ni raisonnablement inferee | Quand il faut borner le rapport |

## Niveaux de sensibilite

| Niveau | Definition |
| --- | --- |
| `faible` | Metadonnees, identifiants techniques, noms de fichiers sans contenu |
| `moyenne` | Extraits de code, descriptions de workflow, prompts non sensibles |
| `elevee` | Regles metier detaillees, logs riches, extraits longs de fichiers |
| `critique` | Secrets, donnees personnelles, tokens, credentials, documents confidentiels, cles API |

## Specificites Claude Code a verifier

| Element | Risque | Action recommandee |
| --- | --- | --- |
| `settings.local.json` permissions larges (`Bash(*)`, `Write(*)`) | Execution non supervisee de commandes systeme | Restreindre aux commandes necessaires |
| Worktrees residuels dans `.claude/worktrees/` | Contenu de branche isole potentiellement non revise | Nettoyer les worktrees non utilises |
| Memoire persistante `~/.claude/projects/` | Accumulation de donnees cross-session | Auditer les fichiers memory periodiquement |
| Hooks configures dans settings.json | Execution automatique de commandes a chaque evenement | Documenter et minimaliser les hooks |
| Fichiers lus via Read/Grep/Glob | Contenu integral transmis a Claude | Inventorier les fichiers lus a chaque session |
| Sorties Bash reprises dans la conversation | Logs, erreurs, donnees systeme potentiellement sensibles | Eviter de repasser des sorties avec credentials |

## Trace de raisonnement observable

Pour chaque agent, documenter uniquement ce qui est explicable et verifiable :

- `mission` : ce que l'agent cherche a produire ;
- `entrees observees` : fichiers, prompts, contrats, artefacts lus ;
- `faits` : ce qui est present dans les sources ;
- `interpretations` : deductions utiles et bornees ;
- `hypotheses` : suppositions necessaires pour avancer ;
- `incertitudes` : ce qui manque ou reste ambigu ;
- `decisions` : choix engages dans le perimetre ;
- `sorties` : artefacts, rapport, handoff.

Ne pas presenter comme "raisonnement detaille" :

- une chaine de pensee interne brute ;
- des motifs non observables ;
- des appels reseau non verifies ;
- des fichiers non lus.

## Checklist minimale

1. Confirmer si le code contient un appel Claude API implemente ou seulement une surface documentaire.
2. Lister les prompts du repo et les skills susceptibles d'etre injectes dans le contexte Claude.
3. Inventorier les fichiers effectivement lus dans la session (Read, Grep, Glob, Bash cat).
4. Distinguer les extraits de contenu des simples chemins ou metadonnees.
5. Identifier les agents cites dans `agents/catalog.md`.
6. Verifier les permissions `settings.local.json` et signaler celles qui sont trop larges.
7. Lister les worktrees actifs et evaluer leur surface d'exposition.
8. Produire un rapport Markdown autonome et datable.
