# JAS-01 — Formaliser le parcours utilisateur du cockpit de refactoring

Epic : JAS-EPIC-01
Statut : CADRAGE
Date : 2026-03-16
Agent : product-owner-fonctionnel

---

## objectif

Documenter le parcours utilisateur complet du cockpit Angular de refactoring progressif, en couvrant les quatre grandes phases : analyser, diagnostiquer, planifier, générer. Identifier les actions conversationnelles prioritaires et les écrans Angular cibles pour guider les agents de design et d'implémentation des epics suivants.

---

## perimetre

- Parcours utilisateur du frontend Angular 21.x uniquement (pas d'implémentation backend à ce stade)
- Quatre phases fonctionnelles : analyser → diagnostiquer → planifier → générer
- Interactions conversationnelles GPT-like pilotées par prompts
- Écrans Angular identifiés comme cibles de conception (sans maquettes détaillées)
- Hors périmètre : authentification utilisateur, persistance PostgreSQL, génération effective du code refactoré (traitées dans les epics suivants)

---

## faits

- Le guide (`guide_generique_refactoring_controller_javafx_spring.md`) définit une feature GPT-like en 7 modules : ingestion, analyse structurelle, classification des responsabilités, stratégie de refactoring, génération de code, explication/raisonnement, module conversationnel.
- La stratégie de migration est découpée en 5 lots : Lot 1 Diagnostic, Lot 2 ViewModel + Use Cases, Lot 3 Flux chargés, Lot 4 Adaptateurs Spring, Lot 5 Assemblers + Stratégies.
- Le backend expose déjà `GET /api/v1/workbench/overview` (Spring Boot 4.0.3, architecture hexagonale).
- L'objet domaine `WorkbenchOverview` agrège `RefactoringLot` et `AgentOverview`.
- Le frontend Angular 21.x est le cockpit de pilotage conversationnel.
- Le catalogue des agents (`agents/catalog.md`) liste les agents `product-owner-fonctionnel`, `api-contrats`, `frontend-angular`, `backend-hexagonal` comme consommateurs directs de ce livrable.

---

## interpretations

- L'interaction conversationnelle ne signifie pas un LLM embarqué côté Angular. Angular orchestre les appels vers le backend, qui pilote le moteur d'analyse. Angular est le terminal conversationnel.
- Les quatre phases (analyser, diagnostiquer, planifier, générer) correspondent directement aux 5 lots du guide : Lot 1 couvre analyser + diagnostiquer, Lots 2-3 couvrent planifier, Lots 4-5 couvrent générer.
- L'endpoint `GET /api/v1/workbench/overview` suffit pour alimenter le cockpit initial (vue synthèse). Les autres phases nécessiteront de nouveaux endpoints définis en JAS-02.
- Le "mode conversationnel" se traduit côté Angular par un composant de chat/prompt avec historique de session, pas nécessairement une fonctionnalité LLM native.

---

## hypotheses

- L'utilisateur est un développeur Java connaissant JavaFX et Spring. L'UX est orientée expert, pas grand public.
- Une session de refactoring correspond à un controller source unique soumis en entrée. La navigation multi-controllers est hors périmètre de l'Epic 01.
- Le frontend communique exclusivement via l'API REST du backend (pas de WebSocket pour cet epic).
- L'état de session (controller courant, lot en cours) est géré côté Angular (service ou store) pour l'Epic 01, sans persistance serveur.

---

## incertitudes

- Le format d'import du controller source côté Angular n'est pas fixé : upload de fichier `.java` vs copier-coller de code brut. À arbitrer en JAS-02.
- La profondeur de l'interaction conversationnelle côté Angular (historique, raffinements successifs) doit être précisée lors de la conception du composant chat.
- L'intégration du moteur d'analyse au backend est hors périmètre de cet epic. La phase "analyser" de ce parcours s'appuie donc sur le comportement futur du moteur (stub ou mock acceptable en Epic 01).

---

## decisions

- Le parcours est structuré en quatre phases séquentielles mais navigables librement une fois une phase franchie.
- Les actions conversationnelles prioritaires sont celles permettant de couvrir le Lot 1 du guide (diagnostic complet) dès l'Epic 02.
- Chaque phase possède un écran Angular dédié identifié ci-dessous.
- Le composant conversationnel (zone de prompt + réponses) est un composant transverse réutilisé dans les écrans des phases 2, 3 et 4.

---

## livrables

### Parcours utilisateur : analyser → diagnostiquer → planifier → générer

```
[1. ANALYSER]
  L'utilisateur soumet un controller JavaFX (.java, texte brut ou fichier)
  → Le backend reçoit la source et déclenche l'ingestion + l'analyse structurelle
  → L'Angular affiche : confirmation de réception, métriques structurelles initiales
    (nb champs FXML, nb handlers, nb services injectés, taille estimée)

[2. DIAGNOSTIQUER]
  L'utilisateur consulte le diagnostic produit par le moteur
  → Angular affiche : cartographie des responsabilités (UI / état / orchestration / métier / technique)
  → L'utilisateur peut poser des questions conversationnelles sur le diagnostic
    Exemples : "quels sont les handlers les plus chargés ?" / "quelle dette domine ?"
  → Angular affiche les réponses dans le composant chat

[3. PLANIFIER]
  L'utilisateur sélectionne un ou plusieurs lots de migration (Lots 1 à 5)
  → Angular affiche : architecture cible proposée, tableau "reste dans le controller / sort du controller"
  → L'utilisateur peut demander des ajustements conversationnels
    Exemples : "ne touche pas aux services legacy" / "génère seulement le ViewModel"
  → Angular affiche le plan de migration détaillé par lot sélectionné

[4. GÉNÉRER]
  L'utilisateur déclenche la génération pour le lot sélectionné
  → Angular affiche : liste des fichiers générés, contenu de chaque fichier, document de raisonnement
  → L'utilisateur peut demander des raffinements
    Exemples : "réécris seulement le controller" / "branche les adaptateurs Spring"
  → Angular permet l'export ou la copie des artefacts générés
```

### Actions conversationnelles prioritaires (Phase 1 de construction, Epic 02)

Niveau 1 — Actions de pilotage de base (indispensables pour le Lot 1) :
- `analyser ce controller` — déclenche l'ingestion et l'analyse structurelle
- `fais le lot 1` — produit diagnostic + cartographie + proposition d'architecture cible
- `donne le diagnostic` — restitue la cartographie des responsabilités
- `quels sont les handlers les plus chargés ?` — interroge la classification

Niveau 2 — Actions de planification (nécessaires pour les Lots 2-3) :
- `génère le ViewModel` — produit la classe `XxxViewModel`
- `fais la migration des flux d'enregistrement` — cible le flux de sauvegarde
- `liste les use cases` — énumère les intentions utilisateur identifiées

Niveau 3 — Actions de génération et de raffinement (Lots 4-5) :
- `branche les adaptateurs Spring` — génère les gateways
- `génère les strategies` — produit les classes de variantes de workflow
- `donne le raisonnement détaillé en markdown` — restitue le document d'explication
- `produis un plan de migration compilable` — génère le plan exportable

### Écrans Angular cibles

| Écran | Route Angular | Phase | Composants clés |
|---|---|---|---|
| Cockpit / Workbench Overview | `/workbench` | Transverse | Synthèse produit, liste des lots, agents disponibles |
| Soumission de source | `/workbench/session/new` | Analyser | Zone d'upload/paste, bouton d'analyse, retour métriques |
| Diagnostic | `/workbench/session/:id/diagnostic` | Diagnostiquer | Carte des responsabilités, composant chat, métriques |
| Plan de migration | `/workbench/session/:id/plan` | Planifier | Sélecteur de lots, tableau architecture cible, chat |
| Génération | `/workbench/session/:id/generate` | Générer | Liste des fichiers générés, viewer de code, chat, export |

### Composants transverses identifiés

- `ConversationalPanelComponent` : zone de prompt + historique des échanges (réutilisé dans phases 2, 3, 4)
- `WorkbenchHeaderComponent` : état de la session courante (controller chargé, lot actif)
- `LotSelectorComponent` : sélection des lots de migration avec état (disponible / actif / terminé)
- `CodeViewerComponent` : affichage de code généré avec coloration syntaxique

---

## dependances

- `backend/docs/architecture.md` : contrat initial du workbench
- `guide_generique_refactoring_controller_javafx_spring.md` : référence des 5 lots et du modèle de packages
- `agents/catalog.md` : identification des agents consommateurs
- JAS-02 (dépendance sortante) : doit définir les endpoints correspondant aux quatre phases

---

## verifications

- [ ] Le parcours en quatre phases couvre les 5 lots du guide sans omission ni incohérence
- [ ] Chaque action conversationnelle prioritaire est associable à un endpoint backend futur
- [ ] Les cinq écrans Angular identifiés sont cohérents avec les routes standard Angular 21.x
- [ ] Aucun écran n'implémente de logique métier centrale (contraire au contrat inter-agent)
- [ ] Les composants transverses sont suffisamment génériques pour être réutilisés dans les epics suivants

---

## handoff

```
handoff:
  vers: api-contrats (agent JAS-02)
  preconditions:
    - Ce document est validé (parcours, actions conversationnelles, écrans cibles)
    - L'architecture hexagonale backend est confirmée dans backend/docs/architecture.md
  points_de_vigilance:
    - Les quatre phases doivent avoir une correspondance endpoint dans JAS-02 (pas seulement GET /workbench/overview)
    - L'action "analyser ce controller" implique un endpoint POST avec le source en corps de requête
    - L'état de session (controller courant, lot en cours) doit être modélisé côté backend ou côté Angular : décision à prendre en JAS-02
    - Les formats d'import du source Java (fichier binaire vs texte brut) impactent le DTO de soumission
  artefacts_a_consulter:
    - jira/epic-01/JAS-01-parcours-utilisateur.md (ce fichier)
    - backend/docs/architecture.md
    - guide_generique_refactoring_controller_javafx_spring.md sections 5, 8, 11
    - agents/catalog.md (agent api-contrats)
```
