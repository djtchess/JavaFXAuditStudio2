# JAS-ACT-011 - Arbitrage des modules specialises du moteur

## objectif

Fermer l'ambiguite restante sur `pdf-analysis`, `inheritance-analysis`,
`dynamic-ui-analysis` et `consolidation` avant l'implementation du ticket
`JAS-ACT-012`.

## perimetre

- artefacts de gouvernance du moteur dans `agents/`
- backlog actif dans `jira/backlog-restant-a-faire.md`
- etat reel du code backend
- echantillons JavaFX sous `samples/`

## faits

- `agents/orchestration.md` place `pdf-analysis`, `inheritance-analysis`,
  `dynamic-ui-analysis` et `consolidation` en phase 4 comme modules specialises
  du moteur.
- `agents/catalog.md` decrit ces quatre agents, mais sans critere d'activation
  formel ni statut MVP/hors MVP.
- Le code backend expose deja un noyau de moteur autour de l'ingestion, de la
  cartographie, de la classification, du plan de migration, de la generation et
  de la restitution.
- Aucun package ou service backend ne materialise aujourd'hui un module dedie
  nomme `pdf-analysis`, `inheritance-analysis` ou `dynamic-ui-analysis`.
- Le depot contient en revanche une piste partielle de consolidation via
  `ProduceRestitutionUseCase`, `ProduceRestitutionService` et
  `domain/restitution`.
- Les echantillons JavaFX montrent une valeur immediate pour :
  - l'heritage implicite :
    `OperationTransitController extends AbstractExamenController`
  - les comportements UI dynamiques :
    listeners, bindings, `EventHandler`, `Property`, `managedProperty().bind(...)`
- Les echantillons montrent aussi des usages PDF, mais surtout comme artefacts
  metier ou de sortie, pas comme source d'entree structurelle du moteur.
- Les occurrences de `pdf-analysis`, `inheritance-analysis`,
  `dynamic-ui-analysis` et `consolidation` dans le repo sont quasi uniquement
  documentaires a ce stade.

## interpretations

- `consolidation` est le seul des quatre modules qui soit structurellement
  indispensable au moteur, car il doit fusionner les evidences de plusieurs
  analyses avant restitution.
- `inheritance-analysis` et `dynamic-ui-analysis` ont une vraie valeur
  fonctionnelle sur les echantillons actuels, mais uniquement quand certains
  signaux sont detectes.
- `pdf-analysis` n'est pas un besoin coeur du MVP moteur tant que l'entree
  principale reste la lecture de sources Java/FXML et que les documents PDF/DOCX
  ne sont pas fournis comme source d'analyse a part entiere.

## hypotheses

- `JAS-ACT-012` doit rester focalise sur les modules immediatement utiles au
  moteur specialise sans diluer le scope dans des integrations documentaires
  optionnelles.
- La restitution actuelle peut servir de point d'ancrage pour introduire une
  vraie consolidation, plutot que de creer un module parallele sans raccord.

## incertitudes

- La forme exacte du contrat de sortie consolide n'est pas encore stabilisee en
  DTO/JSON cible.
- Un futur lot produit peut rendre `pdf-analysis` plus prioritaire si l'entree
  du moteur s'ouvre explicitement aux pieces jointes documentaires.

## decisions

- `consolidation` est declare module obligatoire du MVP moteur, mais doit etre
  formalise dans la chaine existante orchestration/coherence/restitution plutot
  que cree comme sous-systeme autonome.
- `inheritance-analysis` est declare module conditionnel et fusionne a
  `controller-analysis` : activation seulement si une hierarchie non triviale ou
  un contrat implicite est detecte dans le controller ou ses dependances.
- `dynamic-ui-analysis` est declare module conditionnel, avec priorite MVP
  etroite sur les bindings, listeners, visibilites/manages et comportements
  runtime non expliques par le FXML statique.
- `pdf-analysis` est de-scope du coeur de `JAS-ACT-012` :
  il devient module optionnel, active uniquement quand l'entree contient des
  references PDF/DOCX ou des documents fournis explicitement pour l'analyse.
- `JAS-ACT-012` devra donc :
  - formaliser `consolidation` dans le noyau existant
  - etendre `controller-analysis` avec `inheritance-analysis` comme chemin
    conditionnel
  - implementer `dynamic-ui-analysis` comme chemin conditionnel prioritaire
  - laisser `pdf-analysis` hors MVP par defaut, avec de-scope explicite ou point
    d'extension trace

## livrables

- ce document d'arbitrage
- mise a jour de `agents/orchestration.md`
- mise a jour de `agents/catalog.md`
- mise a jour de `jira/backlog-restant-a-faire.md`

## dependances

- `agents/orchestration.md`
- `agents/catalog.md`
- `agents/contracts.md`
- `jira/backlog-restant-a-faire.md`
- `backend/src/main/java/ff/ss/javaFxAuditStudio/application/service/`
- `backend/src/main/java/ff/ss/javaFxAuditStudio/application/ports/in/`
- `samples/javafx/`

## verifications

- verification de l'absence de modules dedies via recherche textuelle dans
  `backend/`, `frontend/`, `docs/`, `agents/`, `samples/` et `scripts-python/`
- verification de la presence d'exemples d'heritage, bindings, listeners et PDF
  dans `samples/javafx/`
- verification de l'ancrage existant de la restitution/consolidation dans
  `ProduceRestitutionUseCase` et `ProduceRestitutionService`

## handoff

```text
handoff:
  vers: implementation-moteur-analyse
  preconditions:
    - considerer `consolidation` comme obligatoire dans JAS-ACT-012
    - traiter `inheritance-analysis` et `dynamic-ui-analysis` comme modules conditionnels
    - garder `pdf-analysis` hors MVP par defaut sauf entree documentaire explicite
  points_de_vigilance:
    - ne pas creer quatre pipelines lourds si trois d'entre eux doivent rester conditionnels
    - raccorder la consolidation au noyau existant de restitution au lieu de dupliquer la synthese
    - conserver des criteres d'activation explicites et testables
  artefacts_a_consulter:
    - docs/jas-act-011-arbitrage-modules-specialises.md
    - agents/orchestration.md
    - agents/catalog.md
    - jira/backlog-restant-a-faire.md
```

