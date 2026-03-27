# Backlog maitre - reste a faire

## objectif

Centraliser le reste a faire reel du produit a partir des sources de verite du repo, des backlogs existants et de l'etat actuel du code, afin d'eviter le double pilotage entre documents historiques.

## perimetre

- produit Angular + Spring Boot du dossier `frontend/` et `backend/`
- moteur d'analyse/refactoring, pipeline IA et sanitisation
- backlog restant au `2026-03-26`
- hors perimetre : replanifier ce qui est deja livre et valide dans le repo

## faits

- Les sources de verite declarees par `AGENTS.md` sont le guide produit, `AGENTS.md`, `backend/pom.xml`, `backend/` et la cible Angular `21.x`.
- Le backend cible `Spring Boot 4.0.3` et `Java 21` dans `backend/pom.xml`.
- Le frontend cible `Angular 21.2.x` dans `frontend/package.json`.
- La commande `backend\\mvnw.cmd -f backend\\pom.xml test` a passe le `2026-03-26` avec `522` tests verts.
- La commande `Set-Location frontend; npm run build` a passe le `2026-03-26`.
- Les endpoints principaux du pipeline existent deja dans `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/in/rest/AnalysisController.java`.
- Les adapters reels de cartographie, classification, plan de migration et generation existent deja dans `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/analysis/`.
- La persistance JPA/Flyway existe deja dans `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/persistence/` et `backend/src/main/resources/db/migration/`.
- La reclassification manuelle, le dashboard projet, l'OpenAPI, l'audit LLM et les vues Angular de diagnostic existent deja dans le repo.
- `jira/refactoring-app-initial-backlog.md`, `jira/backlog-phase-2.md` et `docs/phase3-jira-tickets.md` se recouvrent partiellement et ne refletent plus l'etat reel du code.

## interpretations

- Une grande partie des backlogs "phase 2" et "phase 3" est deja livree dans le code.
- Le reste a faire ne porte plus principalement sur le socle CRUD/pipeline, mais sur l'alignement documentaire, l'observabilite, le DevOps, la finition IA/sanitisation et quelques manques de conformite architecturale.
- Les backlogs specialises `jira/backlog-ia-refactoring-pipeline.md` et `jira/backlog-sanitisation-audit.md` restent utiles, mais ne doivent plus servir de vue globale a eux seuls.

## hypotheses

- Les identifiants historiques (`JAS-*`, `IAP-*`, `QW-*`, `AI-*`) sont conserves pour la tracabilite.
- Les fichiers non committes autour de la sanitisation correspondent a du travail en cours et ne doivent pas etre comptes comme livres tant qu'ils ne sont pas stabilises.
- L'equipe prefere une vue maitre du reste a faire plutot qu'une renumerotation complete du programme.

## incertitudes

- Le choix entre "rebaser les docs sur l'implementation actuelle" et "refactorer le code pour coller aux docs historiques" n'est pas encore arbitre.
- Certains chantiers IA avances sont partiellement presents en squelette ou en service local, mais pas encore assez stabilises pour etre marques DONE.
- Les livrables DevOps/documentation peuvent exister hors repo, sans etre visibles ici.

## decisions

- Ce fichier devient la vue maitre du reste a faire.
- `jira/refactoring-app-initial-backlog.md` conserve l'historique du backlog initial.
- `jira/backlog-phase-2.md` conserve l'historique du backlog socle produit.
- `docs/phase3-jira-tickets.md` conserve l'historique du backlog intermediaire phase 3.
- `jira/backlog-ia-refactoring-pipeline.md` et `jira/backlog-sanitisation-audit.md` restent les sous-backlogs detailles de reference pour l'IA et la sanitisation.

## liste priorisee du reste a faire

### P0 - socle produit a terminer ou realigner

- `P0-01` Exposer `GET /api/v1/analysis/sessions/{sessionId}` et trancher l'alignement API/use cases entre le design documentaire et l'implementation actuelle. References : `JAS-57`, `JAS-55` a `JAS-61`.
- `P0-02` Rendre les statuts d'analyse fins, persistants et tracables sur tout le pipeline, puis aligner `AnalysisStatus`, l'orchestrateur et la base. References : `JAS-86`.
- `P0-03` Brancher reellement la restitution markdown dans la chaine applicative au lieu de laisser seulement l'adapter present. References : `JAS-74`, `JAS-605`.
- `P0-04` Renforcer les tests backend pour verifier le contenu metier des reponses, pas seulement les codes HTTP et les statuts finaux. References : `JAS-89`, `JAS-90`, `JAS-92`.
- `P0-05` Completer la couverture frontend sur les composants et services encore non testes ou peu testes : `analysis-submit`, `analysis-detail`, `cartography-view`, `migration-plan-view`, `artifacts-view`, `report-view`, `analysis-api`, `ai-enrichment-api`, interceptors.
- `P0-06` Finaliser l'observabilite d'exploitation : logs structures production, health checks, metriques pipeline et metriques LLM. References : `JAS-801`, `JAS-802`, `JAS-803`, `IAP-9`, `IAP-11`.
- `P0-07` Produire le lot DevOps manquant : `Dockerfile` backend, `Dockerfile` frontend, workflows CI, scripts de build/run et README racine exploitable. References : `JAS-701` a `JAS-704`.
- `P0-08` Produire la documentation technique manquante : ADR, guide developpeur backend, guide d'integration frontend/backend. References : `JAS-901` a `JAS-903`.
- `P0-09` Fermer les fuites potentielles cote IA/sanitisation, en particulier les logs DEBUG du prompt complet et les ecarts de sanitisation encore ouverts. References : `AI-1`, `QW-3`, `AI-4`.

### P1 - qualite moteur et IA

- `P1-01` Finaliser la qualite AST/regex : dictionnaire configurable externe, validation robuste des reponses LLM, comptage de tokens fiable, elimination des constantes residuelles. References : `JAS-002`, `IAP-3`, `IAP-8`, `IAP-12`.
- `P1-02` Finaliser la qualite des artefacts generes : composants JavaFX custom, nommage semantique, verification compilabilite, tests snapshots et tests generes plus riches. References : `JAS-007`, `JAS-008`, `JAS-009`, `JAS-011`, `JAS-027`, `IAP-14` a `IAP-19`.
- `P1-03` Finaliser l'analyse metier avancee : detection state machine, extraction des gardes policy, analyse multi-controllers, dependances inter-controllers, delta analysis. References : `JAS-019`, `JAS-020`, `JAS-023`, `JAS-024`, `JAS-025`.
- `P1-04` Finaliser l'experience IA cote produit : streaming SSE, vue diff template/IA, raffinement multi-tour, feedback visuel de progression, copie/telechargement unitaire cote UI. References : `IAP-20` a `IAP-24`.
- `P1-05` Finaliser la persistance/versioning des artefacts IA, y compris historique et export ZIP. References : `IAP-25` a `IAP-28`.
- `P1-06` Finaliser le contexte IA enrichi : contexte ecran complet, injection du feedback de reclassification, verification de coherence inter-artefacts et apprentissage de patterns projet. References : `IAP-29` a `IAP-32`.

### P2 - industrialisation et extensions produit

- `P2-01` Industrialiser l'export des artefacts vers une structure cible Maven/Gradle plutot qu'un export a plat. Reference : `JAS-026`.
- `P2-02` Etendre le dashboard actuel vers un vrai dashboard de monitoring technique frontend. Reference : `JAS-804`.
- `P2-03` Formaliser un audit documentaire clair de l'architecture de sanitisation et sa trace de gouvernance si l'equipe souhaite conserver `QW-1` comme DONE. Reference : `QW-1`.
- `P2-04` Enrichir et durcir le corpus et les regles Semgrep au-dela du jeu minimal actuel. Reference : `AI-3`.

## plan de sprints recommande

### Sprint 1 - realignement socle et garde-fous

- Objectif : remettre le socle pipeline en conformite avant toute extension.
- Contenu : `P0-01`, `P0-02`, `P0-03`, `P0-09`.
- Sortie attendue : API/session coherente, statuts de pipeline fins, restitution markdown branchee, logs IA nettoyes.

### Sprint 2 - qualite executable et couverture

- Objectif : rendre le socle vraiment robuste a la regression.
- Contenu : `P0-04`, `P0-05`.
- Sortie attendue : tests backend sur le contenu metier, couverture frontend et services critiques nettement montee.

### Sprint 3 - observabilite, livraison et docs

- Objectif : rendre le produit exploitable par une equipe et un CI reel.
- Contenu : `P0-06`, `P0-07`, `P0-08`.
- Sortie attendue : health checks, metriques, packaging, CI, docs d'architecture et d'integration.

### Sprint 4 - qualite moteur

- Objectif : fiabiliser le moteur d'analyse et la generation avant d'aller plus loin cote IA.
- Contenu : `P1-01`, `P1-02`, `P1-03`.
- Sortie attendue : meilleur AST/regex, artefacts generes plus fiables, heuristiques metier avancees stabilisees.

### Sprint 5 - IA produit

- Objectif : passer d'une IA utile a une IA exploitable en workflow complet.
- Contenu : `P1-04`, `P1-05`, `P1-06`.
- Sortie attendue : UX IA plus riche, persistence/versioning des generations, contexte et coherence inter-artefacts.

### Sprint 6 - extensions et durcissement final

- Objectif : terminer les sujets d'industrialisation non bloquants et les extensions produit.
- Contenu : `P2-01`, `P2-02`, `P2-03`, `P2-04`.
- Sortie attendue : export structure, monitoring frontend, audit sanitisation formalise, Semgrep enrichi.

## dependances inter-sprints

- `Sprint 1` est prerequis pour tous les autres sprints.
- `Sprint 2` et `Sprint 3` peuvent se derouler en parallele apres `Sprint 1` si l'equipe est suffisante.
- `Sprint 4` doit attendre la fin de `Sprint 1` et beneficie fortement des sorties de `Sprint 2`.
- `Sprint 5` doit demarrer apres `Sprint 4`.
- `Sprint 6` peut commencer apres `Sprint 3`, mais sa cloture ideale vient apres `Sprint 5`.

## tickets par sprint

### Sprint 1 - tickets

| Ticket | Contenu | Agent lead | Appuis | Estimation |
| --- | --- | --- | --- | --- |
| `P0-01` | Endpoint session + alignement API/use cases | `backend-hexagonal` | `api-contrats`, `gouvernance` | 5 SP |
| `P0-02` | Statuts de pipeline fins et persistants | `backend-hexagonal` | `database-postgres`, `observabilite-exploitation` | 8 SP |
| `P0-03` | Restitution markdown branchee dans la chaine applicative | `backend-hexagonal` | `implementation-moteur-analyse` | 3 SP |
| `P0-09` | Nettoyage logs IA et ecarts de sanitisation critiques | `securite` | `transparence-openai`, `backend-hexagonal` | 5 SP |
| **Total sprint 1** |  |  |  | **21 SP** |

### Sprint 2 - tickets

| Ticket | Contenu | Agent lead | Appuis | Estimation |
| --- | --- | --- | --- | --- |
| `P0-04` | Tests backend sur contenu metier | `qa-backend` | `test-automation`, `backend-hexagonal` | 5 SP |
| `P0-05` | Couverture frontend et services critiques | `qa-frontend` | `frontend-angular`, `test-automation` | 8 SP |
| **Total sprint 2** |  |  |  | **13 SP** |

### Sprint 3 - tickets

| Ticket | Contenu | Agent lead | Appuis | Estimation |
| --- | --- | --- | --- | --- |
| `P0-06` | Observabilite prod et metriques pipeline/LLM | `observabilite-exploitation` | `backend-hexagonal`, `frontend-angular` | 8 SP |
| `P0-07` | Dockerfiles, CI et scripts de build/run | `devops-ci-cd` | `backend-hexagonal`, `frontend-angular` | 8 SP |
| `P0-08` | ADR, guide backend, guide integration | `documentation-technique` | `gouvernance`, `backend-hexagonal`, `frontend-angular` | 5 SP |
| **Total sprint 3** |  |  |  | **21 SP** |

### Sprint 4 - tickets

| Ticket | Contenu | Agent lead | Appuis | Estimation |
| --- | --- | --- | --- | --- |
| `P1-01` | Qualite AST/regex et validation LLM | `implementation-moteur-analyse` | `backend-hexagonal`, `securite` | 8 SP |
| `P1-02` | Qualite des artefacts generes | `implementation-moteur-analyse` | `test-automation`, `qa-backend` | 8 SP |
| `P1-03` | Analyse metier avancee | `implementation-moteur-analyse` | `analyste-regles-metier`, `architecture-moteur-analyse` | 13 SP |
| **Total sprint 4** |  |  |  | **29 SP** |

### Sprint 5 - tickets

| Ticket | Contenu | Agent lead | Appuis | Estimation |
| --- | --- | --- | --- | --- |
| `P1-04` | UX IA produit | `frontend-angular` | `backend-hexagonal`, `api-contrats` | 13 SP |
| `P1-05` | Persistance/versioning des artefacts IA | `backend-hexagonal` | `database-postgres`, `implementation-moteur-analyse` | 8 SP |
| `P1-06` | Contexte IA enrichi et coherence inter-artefacts | `implementation-moteur-analyse` | `backend-hexagonal`, `analyste-regles-metier` | 13 SP |
| **Total sprint 5** |  |  |  | **34 SP** |

### Sprint 6 - tickets

| Ticket | Contenu | Agent lead | Appuis | Estimation |
| --- | --- | --- | --- | --- |
| `P2-01` | Export structure Maven/Gradle | `backend-hexagonal` | `devops-ci-cd` | 5 SP |
| `P2-02` | Monitoring technique frontend | `frontend-angular` | `observabilite-exploitation` | 5 SP |
| `P2-03` | Audit documentaire sanitisation | `gouvernance` | `documentation-technique`, `transparence-openai` | 3 SP |
| `P2-04` | Durcissement corpus et regles Semgrep | `securite` | `implementation-moteur-analyse` | 5 SP |
| **Total sprint 6** |  |  |  | **18 SP** |

## charge consolidee

- `Sprint 1` : 21 SP
- `Sprint 2` : 13 SP
- `Sprint 3` : 21 SP
- `Sprint 4` : 29 SP
- `Sprint 5` : 34 SP
- `Sprint 6` : 18 SP
- **Total programme restant** : **136 SP**

## lecture de capacite

- Avec une seule equipe fullstack, `Sprint 4` et `Sprint 5` sont les sprints les plus charges et devront sans doute etre recoupes.
- Avec deux equipes, il est pertinent de faire tourner `Sprint 2` et `Sprint 3` en parallele apres `Sprint 1`.
- Si la capacite est faible, la premiere coupe naturelle consiste a repousser `P2-01` a `P2-04`, puis a fractionner `P1-04` et `P1-06`.

## sous-backlogs actifs

- IA avancee detaillee : `jira/backlog-ia-refactoring-pipeline.md`
- Sanitisation detaillee : `jira/backlog-sanitisation-audit.md`
- Historique backlog initial : `jira/refactoring-app-initial-backlog.md`
- Historique socle produit : `jira/backlog-phase-2.md`
- Historique phase 3 : `docs/phase3-jira-tickets.md`

## livrables

- `jira/backlog-restant-a-faire.md`
- mise a jour du statut documentaire des anciens backlogs globaux

## dependances

- `AGENTS.md`
- `guide_generique_refactoring_controller_javafx_spring.md`
- `backend/pom.xml`
- `backend/`
- `jira/backlog-ia-refactoring-pipeline.md`
- `jira/backlog-sanitisation-audit.md`

## verifications

- Validation backend executee le `2026-03-26` : `522` tests verts.
- Validation frontend executee le `2026-03-26` : build production OK.
- Les priorites ci-dessus ne reouvrent pas les chantiers deja livres dans les adapters reels, la persistance, l'orchestration de base, la reclassification, le dashboard projet et l'OpenAPI.
- Les tickets listes comme restants ont ete retenus soit par absence de code/artefact visible, soit parce que l'implementation actuelle reste partielle au regard des criteres du backlog historique.

## handoff

```text
handoff:
  vers: gouvernance
  preconditions:
    - backlog maitre cree
    - backlogs historiques explicitement declasses comme vues secondaires
  points_de_vigilance:
    - arbitrer rapidement l'ecart entre design documentaire et code reel sur les endpoints/use cases
    - ne pas relancer de chantier produit sans traiter P0-06, P0-07 et P0-08
    - garder l'IA et la sanitisation comme sous-backlogs specialises, pas comme backlog maitre
  artefacts_a_consulter:
    - jira/backlog-restant-a-faire.md
    - jira/backlog-ia-refactoring-pipeline.md
    - jira/backlog-sanitisation-audit.md
    - AGENTS.md
```
