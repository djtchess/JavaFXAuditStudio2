# Backlog JIRA consolide - source unique

> Date de reference : 2026-04-01
> Statut documentaire : ce fichier est desormais l'unique backlog Markdown conserve dans `./jira`.
> Source de verite : `AGENTS.md`, `agents/orchestration.md`, `backend/`, `frontend/`, l'etat reel du depot et les validations locales realisees le `2026-04-01`.

## objectif

Centraliser dans un seul document :

- la priorisation active du produit
- l'ordre d'execution par lot et par agent
- le decoupage Jira exploitable
- la trace minimale des anciens backlogs consolides

## decisions de gouvernance

- `jira/backlog-restant-a-faire.md` devient la seule reference Markdown conservee dans `./jira`.
- Les anciens fichiers Jira sont consolides ici puis supprimes du dossier `jira/`.
- La numerotation active pour le pilotage courant est `JAS-ACT-*`.
- Les familles historiques `JAS-*`, `JAS-3xx/4xx/5xx/...`, `IAP-*`, `QW-*` et `AI-*` restent conservees ici sous forme de synthese d'archive.
- Le backlog reflote sur l'etat reel du code apres l'implementation de `JAS-ACT-001` a `JAS-ACT-011`.

## etat reel du depot

### faits verifies

- Le backend `Spring Boot 4.0.3 / Java 21` est revenu au vert.
- `backend\\mvnw.cmd -f backend\\pom.xml test` passe avec `667` tests, `0` failure, `0` error, `1` skipped.
- Le backend dispose maintenant d'un socle Spring Security minimal opt-in pour les endpoints sensibles, base sur un bearer token applicatif.
- Le frontend `Angular 21.2.x` est revenu au vert sur le perimetre principal.
- `npm test -- --watch=false` passe avec `104` tests verts.
- `npm run build` passe.
- Le build frontend ne remonte plus de warning `anyComponentStyle` sur `ai-enrichment-view`.
- L'observabilite frontend est maintenant branchee sur les parcours HTTP via un interceptor dedie, un suivi des correlation IDs et une exposition dans l'ecran de monitoring.
- La documentation technique front/back est maintenant realignee sur l'etat reel du depot pour la securite minimale, l'observabilite, les routes frontend et les validations locales.
- La convention API cote frontend est maintenant alignee sur `/api/v1/analysis/sessions/...`.
- Le backend conserve encore un alias legacy `/api/v1/analyses/...` pour transition.
- Les scripts de recette locale et la CI d'integration backend sont harmonises autour des scripts `scripts/test.*`.
- Le repo contient deja des workflows CI, des Dockerfiles, `docker-compose.yml`, des bases d'observabilite et de documentation technique.
- Les rapports `TransparenceOpenAI` et `TransparenceClaude` ont ete rejoues le `2026-04-01` sur l'etat reel du depot.
- L'arbitrage moteur est maintenant trace : `consolidation` est obligatoire dans le noyau existant, `inheritance-analysis` est absorbe comme chemin conditionnel, `dynamic-ui-analysis` devient le module conditionnel prioritaire, `pdf-analysis` sort du MVP par defaut.

### conclusions

- La phase de bootstrap est terminee.
- Les tickets `JAS-ACT-001` a `JAS-ACT-011` peuvent etre consideres comme traites.
- Le prochain ticket de code prioritaire est `JAS-ACT-012`.
- Le reste a faire porte surtout sur l'implementation du perimetre moteur arbitre puis l'industrialisation.

## tickets deja traites

| Ticket | Statut | Synthese |
| --- | --- | --- |
| `JAS-ACT-001` | DONE | Diagnostic et correction du faux etat rouge backend ; suite backend relancee proprement |
| `JAS-ACT-002` | DONE | Fermeture des ecarts hexagonaux REST prioritaires via use cases entrants |
| `JAS-ACT-003` | DONE | Stabilisation de l'execution locale backend et alignement CI/local |
| `JAS-ACT-004` | DONE | Harmonisation de la convention API `/analysis/sessions` avec alias legacy backend |
| `JAS-ACT-005` | DONE | Socle Spring Security minimal pose sur les endpoints sensibles, avec bearer token applicatif opt-in, classification documentaire et tests dedies |
| `JAS-ACT-006` | DONE | Gouvernance documentaire et rapports de transparence realignes sur l'etat reel du repo, avec references de pilotage corrigees |
| `JAS-ACT-007` | DONE | Suite frontend stabilisee durablement ; build sans warning de budget `anyComponentStyle` et validations locales repassees au vert |
| `JAS-ACT-008` | DONE | Dette de structure frontend reduite sur le flux IA via extraction de panneaux dedies pour l'audit, les artefacts persistes et la previsualisation sanitisee |
| `JAS-ACT-009` | DONE | Observabilite frontend branchee sur les parcours HTTP critiques avec correlation IDs, synthese locale et exposition dans le dashboard de monitoring |
| `JAS-ACT-010` | DONE | Guides techniques front/back, README et observabilite realignes sur les routes, la securite minimale, le monitoring et les validations locales |
| `JAS-ACT-011` | DONE | Arbitrage moteur formalise : consolidation obligatoire, inheritance-analysis absorbe comme chemin conditionnel, dynamic-ui-analysis conditionnel prioritaire, pdf-analysis optionnel hors MVP |

## prochain ticket a implementer

- `JAS-ACT-012` - Implementer ou de-scoper proprement les modules arbitres

Pourquoi maintenant :

- l'ambiguite sur les modules specialises du moteur est maintenant fermee
- `consolidation` doit etre formalise dans le noyau existant avant tout elargissement du moteur
- `dynamic-ui-analysis` est le premier slice conditionnel a forte valeur a implementer proprement

## priorisation active

### P0 - fermer le socle

- Aucun ticket `P0` ouvert apres la cloture de `JAS-ACT-008`.

### P1 - stabiliser le produit visible

- Aucun ticket `P1` ouvert apres la cloture de `JAS-ACT-010`.

### P1 - finaliser produit et moteur

| Ticket | Priorite | SP | Agent lead | Objet |
| --- | --- | --- | --- | --- |
| `JAS-ACT-012` | P1 | 13 | `implementation-moteur-analyse` | implementer ou de-scoper proprement les modules arbitres |
| `JAS-ACT-013` | P1 | 8 | `product-owner-fonctionnel` | cadrer l'evolution de l'ingestion et du module conversationnel |

### P2 - industrialiser

| Ticket | Priorite | SP | Agent lead | Objet |
| --- | --- | --- | --- | --- |
| `JAS-ACT-014` | P2 | 8 | `devops-ci-cd` | durcir la CI en vraies gates qualite |
| `JAS-ACT-015` | P2 | 8 | `devops-ci-cd` | completer la chaine de livraison et d'exploitation |
| `JAS-ACT-016` | P2 | 5 | `database-postgres` | durcir l'industrialisation data et moteur |

## ordre d'execution recommande

1. `implementation-moteur-analyse` sur `JAS-ACT-012`
2. `product-owner-fonctionnel` + `architecture-applicative` sur `JAS-ACT-013`
3. `devops-ci-cd` + `database-postgres` sur `JAS-ACT-014` a `JAS-ACT-016`

## lots recommandes

### Lot 1 - Securite et gouvernance

- Tickets : `JAS-ACT-005`, `JAS-ACT-006`
- Objectif : proteger le socle et remettre la gouvernance documentaire au meme niveau que le code
- Sortie attendue : endpoints sensibles classes, premier niveau de protection pose, transparence et doc de pilotage rejouees

### Lot 2 - Stabilisation produit visible

- Tickets : aucun ticket ouvert
- Objectif : lot cloture apres `JAS-ACT-010`
- Sortie attendue : docs a jour, parcours critiques traces proprement, references front/back realignees

### Lot 3 - Finalisation moteur et trajectoire produit

- Tickets : `JAS-ACT-012`, `JAS-ACT-013`
- Objectif : implementer le perimetre moteur arbitre puis cadrer l'evolution produit
- Sortie attendue : consolidation formalisee, modules conditionnels traces, trajectoire conversationnelle clarifiee

### Lot 4 - Industrialisation finale

- Tickets : `JAS-ACT-014`, `JAS-ACT-015`, `JAS-ACT-016`
- Objectif : rendre le produit durablement exploitable
- Sortie attendue : gates qualite, livraison outillee, runbooks, hygiene data et moteur renforcee

## backlog Jira exploitable

## EPIC JAS-ACT-EPIC-01 - Retour au vert backend

**Statut**

- Epic clos sur le perimetre prioritaire

### `JAS-ACT-001` - Diagnostiquer et corriger les regressions backend critiques

- Type : Story
- Statut : DONE
- Priorite : P0
- Estimation : 8 SP
- Lead : `backend-hexagonal`

### `JAS-ACT-002` - Refermer les ecarts hexagonaux prioritaires cote REST

- Type : Story
- Statut : DONE
- Priorite : P0
- Estimation : 5 SP
- Lead : `backend-hexagonal`

### `JAS-ACT-003` - Stabiliser l'execution locale backend

- Type : Story
- Statut : DONE
- Priorite : P0
- Estimation : 3 SP
- Lead : `test-automation`

## EPIC JAS-ACT-EPIC-02 - Contrats API, securite et gouvernance

**Objectif**

Figer les conventions d'API, poser le socle de securite et maintenir un pilotage documentaire aligne sur le repo reel.

### `JAS-ACT-004` - Trancher et harmoniser la convention `/analysis` vs `/analyses`

- Type : Story
- Statut : DONE
- Priorite : P0
- Estimation : 3 SP
- Lead : `api-contrats`

### `JAS-ACT-005` - Definir et poser le socle securite backend minimal

- Type : Story
- Statut : DONE
- Priorite : P0
- Estimation : 8 SP
- Lead : `securite`
- Appuis : `api-contrats`, `backend-hexagonal`

Criteres d'acceptation :

- les endpoints sensibles sont classes et documentes
- le modele d'authentification et d'autorisation cible du socle est defini
- une premiere implementation protege les endpoints critiques ou formalise explicitement leur exposition
- des tests de securite de base sont ajoutes ou realignes

Validation constatee le `2026-04-01` :

- socle Spring Security stateless ajoute au backend
- endpoints sensibles centralises dans `ApiSecurityEndpointCatalog`
- bearer token applicatif opt-in via `APP_SECURITY_API_KEY_ENABLED` et `APP_SECURITY_API_KEY`
- tests cibles de securite et d'observabilite verts sur le slice `JAS-ACT-005`

### `JAS-ACT-006` - Realigner backlog, doc de pilotage et transparence

- Type : Story
- Statut : DONE
- Priorite : P0
- Estimation : 5 SP
- Lead : `architecture-applicative`
- Appuis : `documentation-technique`, `transparence-openai`, `gouvernance`

Criteres d'acceptation :

- le backlog maitre reste coherent avec l'etat reel du repo
- les conventions de pilotage et d'execution sont a jour
- le rapport de transparence est rejoue si les changements de prompts ou de flux le necessitent

Validation constatee le `2026-04-01` :

- backlog Jira consolide aligne sur les tickets `JAS-ACT-001` a `JAS-ACT-006`
- references de gouvernance Claude corrigees vers `AGENTS-claude.md`
- rapports `TransparenceOpenAI` et `TransparenceClaude` rejoues sur l'etat reel du depot

## EPIC JAS-ACT-EPIC-03 - Stabilisation frontend et observabilite

**Objectif**

Conserver un frontend stable, plus lisible et reellement observable, sans deplacer la logique experte hors du backend.

### `JAS-ACT-007` - Remettre durablement la suite frontend au vert

- Type : Story
- Statut : DONE
- Validation : `npm run typecheck`, `npm run build`, `npm test -- --watch=false`
- Priorite : P1
- Estimation : 5 SP
- Lead : `frontend-angular`
- Appuis : `qa-frontend`, `test-automation`

### `JAS-ACT-008` - Reduire la dette de structure des composants frontend majeurs

- Type : Story
- Statut : DONE
- Validation : `npm run typecheck`, `npm run build`, `npm test -- --watch=false`
- Priorite : P1
- Estimation : 8 SP
- Lead : `frontend-angular`
- Appuis : `qa-frontend`, `revue-code`

### `JAS-ACT-009` - Brancher l'observabilite frontend reelle

- Type : Story
- Statut : DONE
- Validation : `npm run typecheck`, `npm run build`, `npm test -- --watch=false`
- Priorite : P1
- Estimation : 5 SP
- Lead : `observabilite-exploitation`
- Appuis : `frontend-angular`, `qa-frontend`

### `JAS-ACT-010` - Mettre a jour la documentation technique frontend/backend

- Type : Story
- Statut : DONE
- Validation : verification manuelle des guides, references et commandes documentees
- Priorite : P1
- Estimation : 5 SP
- Lead : `documentation-technique`
- Appuis : `backend-hexagonal`, `frontend-angular`, `gouvernance`

## EPIC JAS-ACT-EPIC-04 - Finalisation moteur specialise et trajectoire produit

**Objectif**

Fermer les ecarts encore ouverts entre la cible du moteur specialise et ce qui est effectivement implemente dans le depot.

### `JAS-ACT-011` - Arbitrer et fermer les modules specialises restants

- Type : Story
- Statut : DONE
- Validation : verification croisee de `agents/orchestration.md`, `agents/catalog.md`, `docs/jas-act-011-arbitrage-modules-specialises.md` et du code reel backend/samples
- Priorite : P1
- Estimation : 5 SP
- Lead : `architecture-moteur-analyse`
- Appuis : `implementation-moteur-analyse`, `gouvernance`

Decisions constatees :

- `consolidation` : obligatoire, a formaliser dans le noyau orchestration/coherence/restitution existant
- `inheritance-analysis` : conditionnel, a absorber dans `controller-analysis`
- `dynamic-ui-analysis` : conditionnel et prioritaire sur un MVP etroit
- `pdf-analysis` : optionnel hors MVP par defaut

### `JAS-ACT-012` - Implementer ou de-scoper proprement les modules arbitres

- Type : Story
- Statut : TODO
- Priorite : P1
- Estimation : 13 SP
- Lead : `implementation-moteur-analyse`
- Appuis : `analyste-regles-metier`, `test-automation`

Perimetre arbitre :

- formaliser `consolidation` dans le noyau existant
- etendre `controller-analysis` avec `inheritance-analysis` comme chemin conditionnel
- implementer `dynamic-ui-analysis` comme chemin conditionnel prioritaire
- laisser `pdf-analysis` hors MVP par defaut avec point d'extension ou de-scope explicite

### `JAS-ACT-013` - Cadrer l'evolution de l'ingestion et du module conversationnel

- Type : Story
- Statut : TODO
- Priorite : P1
- Estimation : 8 SP
- Lead : `product-owner-fonctionnel`
- Appuis : `architecture-applicative`, `api-contrats`, `frontend-angular`

## EPIC JAS-ACT-EPIC-05 - Industrialisation finale

**Objectif**

Transformer la base existante en produit durablement exploitable, avec qualite mesurable et chaine de livraison outillee.

### `JAS-ACT-014` - Durcir la CI en vraies gates qualite

- Type : Story
- Statut : TODO
- Priorite : P2
- Estimation : 8 SP
- Lead : `devops-ci-cd`
- Appuis : `qa-backend`, `qa-frontend`, `observabilite-exploitation`

### `JAS-ACT-015` - Completer la chaine de livraison et d'exploitation

- Type : Story
- Statut : TODO
- Priorite : P2
- Estimation : 8 SP
- Lead : `devops-ci-cd`
- Appuis : `observabilite-exploitation`, `documentation-technique`

### `JAS-ACT-016` - Durcir l'industrialisation data et moteur

- Type : Story
- Statut : TODO
- Priorite : P2
- Estimation : 5 SP
- Lead : `database-postgres`
- Appuis : `securite`, `implementation-moteur-analyse`

## estimation consolidee

| Epic | SP |
| --- | --- |
| `JAS-ACT-EPIC-01` | 16 SP |
| `JAS-ACT-EPIC-02` | 16 SP |
| `JAS-ACT-EPIC-03` | 23 SP |
| `JAS-ACT-EPIC-04` | 26 SP |
| `JAS-ACT-EPIC-05` | 21 SP |
| **Total historique du programme `JAS-ACT`** | **102 SP** |
| **Reste a faire apres `JAS-ACT-001` a `011`** | **42 SP** |

## synthese d'archive des anciens backlogs

### cadrage initial epic 01

Les anciens documents `JAS-01`, `JAS-02` et `JAS-03` ont fourni :

- le parcours utilisateur initial du cockpit
- le contrat d'API initial du workbench
- les exigences de securite et d'exposition de depart

Leur substance active est maintenant reprise par :

- `JAS-ACT-005` pour la securite
- `JAS-ACT-010` pour la documentation et les contrats techniques
- `JAS-ACT-013` pour la trajectoire produit et conversationnelle

### backlog produit et phase 2 / phase 3

Les anciens fichiers `refactoring-app-initial-backlog.md`, `backlog-phase-2.md` et `docs/phase3-jira-tickets.md` ont servi de reservoir historique pour :

- le remplacement progressif des stubs
- la persistence PostgreSQL
- l'UX Angular
- le moteur d'analyse
- DevOps, observabilite et documentation

Ils sont desormais resumes par les epics `JAS-ACT-001` a `JAS-ACT-016`, plus courts et aligns sur l'etat reel du depot.

### backlog IA pipeline

L'ancien backlog `IAP-*` couvrait surtout :

- robustesse du pipeline IA
- observabilite et metriques LLM
- qualite du code genere
- experience utilisateur IA
- persistance/versioning des artefacts
- enrichissement contextuel et feedback

Ces sujets restent utiles mais ne sont plus pilotes comme backlog separable dans `./jira`. Ils se redistribuent maintenant principalement dans :

- `JAS-ACT-006`
- `JAS-ACT-009`
- `JAS-ACT-010`
- `JAS-ACT-012`
- `JAS-ACT-016`

### backlog sanitisation et audit

Les anciennes familles `QW-*` et `AI-*` couvraient le durcissement de la sanitisation, de l'assemblage de prompts et de la gouvernance LLM.

Leur substance active est maintenant suivie via :

- `JAS-ACT-005` pour la classification et la protection des flux sensibles
- `JAS-ACT-006` pour la transparence et la gouvernance
- `JAS-ACT-012` pour les impacts moteur
- `JAS-ACT-016` pour l'industrialisation data/moteur

## definition de done minimale

- build ou execution locale reproductible sur le perimetre du lot
- tests critiques verts
- aucune regression connue non documentee sur le perimetre traite
- documentation mise a jour si le lot touche contrats, exploitation ou usages
- revue `revue-code` puis verification QA ciblee quand le lot le justifie

## handoff recommande

```text
handoff:
  vers: gouvernance
  preconditions:
    - considerer ce fichier comme l'unique backlog Markdown dans ./jira
    - lancer le prochain lot sur JAS-ACT-012
    - enchainer ensuite sur JAS-ACT-013 apres stabilisation du perimetre moteur arbitre
  points_de_vigilance:
    - ne pas reouvrir artificiellement des chantiers deja livres dans le depot
    - conserver /api/v1/analyses seulement comme alias legacy tant que la transition n'est pas fermee
    - rejouer la transparence a chaque evolution des prompts, providers ou donnees exposees
  artefacts_a_consulter:
    - AGENTS.md
    - agents/orchestration.md
    - backend/
    - frontend/
    - docs/
```




