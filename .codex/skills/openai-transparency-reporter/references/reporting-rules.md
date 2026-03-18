# Regles de reporting

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
| `critique` | Secrets, donnees personnelles, tokens, credentials, documents confidentiels |

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

1. Confirmer si le code contient un appel OpenAI implemente ou seulement une surface documentaire.
2. Lister les prompts du repo et les skills susceptibles d'etre injectes.
3. Inventorier les fichiers effectivement lus dans la session.
4. Distinguer les extraits de contenu des simples chemins ou metadonnees.
5. Identifier les agents cites dans `agents/catalog.md`.
6. Produire un rapport Markdown autonome et datable.
