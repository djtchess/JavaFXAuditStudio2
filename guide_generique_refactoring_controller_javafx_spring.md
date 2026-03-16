# Spécification complète pour Codex — feature GPT-like de refactoring de controllers JavaFX + Spring

## 1. Objet du document

Ce document est une **spécification d’architecture, de raisonnement et de transformation** destinée à être fournie à Codex afin qu’il développe une **nouvelle feature** capable de refactorer un controller JavaFX + Spring fourni en entrée.

Le but n’est pas seulement d’écrire quelques scripts de refactoring. Le but est de faire coder par Codex une **feature interactive de type GPT-like**, spécialisée dans l’analyse et la refactorisation progressive de controllers JavaFX + Spring trop chargés.

Cette feature devra :
- recevoir un controller source en entrée ;
- analyser sa structure et ses responsabilités ;
- produire un diagnostic détaillé ;
- proposer une architecture cible ;
- générer un plan de migration par étapes ;
- générer du code refactoré ;
- expliquer son raisonnement ;
- fonctionner comme un assistant expert de refacto, piloté par prompts et fichiers.

Le terme “GPT-like” signifie ici :
- interaction conversationnelle ;
- capacité à expliquer le diagnostic ;
- capacité à proposer plusieurs étapes ;
- capacité à générer du code cible ;
- capacité à conserver le contexte d’un controller donné ;
- capacité à produire des artefacts textuels et code de manière structurée.

Ce document doit donc être compris comme :
- une spécification fonctionnelle de la feature à développer ;
- une spécification métier de la logique de refactoring ;
- une spécification de raisonnement à suivre ;
- une spécification des transformations de code à appliquer.

---

## 2. Vision du produit à construire

### 2.1 Finalité

la feature à construire devra aider un développeur à transformer un controller JavaFX + Spring monolithique en architecture cible en couches.

Entrée principale :
- un ou plusieurs fichiers Java de controllers ;
- éventuellement des classes associées si disponibles ;
- éventuellement des classes services associées si disponibles ;
- éventuellement des fichiers FXML, services et DTO si présents.

Sorties attendues :
- diagnostic du controller ;
- cartographie des responsabilités ;
- plan de migration ;
- proposition d’architecture cible ;
- génération de nouvelles classes ;
- version refactorée partielle ou progressive du controller ;
- documentation du raisonnement.

### 2.2 Positionnement

Cette feature n’est pas un simple parseur de code. Elle doit se comporter comme un **assistant expert de refactoring guidé par architecture**.

Elle doit donc combiner :
- compréhension structurelle du code ;
- heuristiques d’architecture ;
- règles de transformation ;
- génération de code ;
- interaction textuelle explicative.

### 2.3 Cas d’usage principal

Un utilisateur fournit un controller JavaFX + Spring très chargé. la feature :
1. l’analyse ;
2. détecte les handlers et la logique métier cachée ;
3. identifie l’état de présentation ;
4. repère les appels externes ;
5. propose une cible `Controller + ViewModel + UseCases + Policies + Gateways + Assemblers + Strategies` ;
6. génère le plan de migration ;
7. génère les fichiers Java nécessaires ;
8. explique les choix réalisés.

---

## 3. Problème cible à résoudre

### 3.1 Nature des controllers à traiter

la feature doit être optimisée pour les controllers JavaFX + Spring qui présentent une ou plusieurs caractéristiques suivantes :
- beaucoup de composants `@FXML` ;
- beaucoup de services injectés ;
- logique métier dans les handlers ;
- calculs de statuts et de droits dans le controller ;
- construction d’objets métier depuis les champs UI ;
- appels à des services REST ;
- gestion de fichiers, pièces jointes, impressions ;
- lancement d’outils externes ;
- intégration à du matériel connecté ;
- grands fichiers avec de nombreuses méthodes privées.

### 3.2 Symptôme architectural fondamental

Le problème n’est pas seulement la taille du controller. Le vrai problème est le **mélange de responsabilités**.

Le controller devient simultanément :
- couche UI ;
- couche d’état ;
- couche d’orchestration ;
- couche métier ;
- couche technique.

Le refactoring doit donc être pensé comme une **séparation progressive des responsabilités**, et non comme une simple réduction du nombre de lignes.

---

## 4. Principe directeur de raisonnement

la feature que Codex doit construire devra appliquer le principe suivant :

> Chaque portion de logique doit être déplacée vers la couche qui porte naturellement cette responsabilité, sans réécriture brutale de l’existant.

### 4.1 Ce que le controller doit devenir

Le controller final doit être un **adaptateur FXML**.
Il doit principalement :
- binder les composants UI ;
- relayer les actions utilisateur ;
- déléguer vers un ViewModel ou un orchestrateur applicatif ;
- conserver seulement les détails purement visuels.

### 4.2 Ce que le controller ne doit plus porter

Le controller ne doit plus porter :
- les règles métier ;
- les calculs de statut métier ;
- la logique d’orchestration de bout en bout ;
- les appels directs aux services techniques ou REST ;
- la fabrication profonde d’objets métier ;
- les décisions de workflow complexes.

### 4.3 Règle de progressivité

la feature ne doit pas forcer une réécriture “big bang”. Elle doit produire une migration incrémentale.

Raison :
- les controllers réels ont des effets de bord ;
- les dépendances sont souvent implicites ;
- la préparation des données peut être imbriquée dans l’UI ;
- certains services historiques sont déjà stables en production.

L’outil doit donc savoir :
- diagnostiquer ;
- prioriser ;
- extraire progressivement ;
- réutiliser l’existant via adaptateurs.

---

## 5. Architecture logique de la feature GPT-like à développer

Codex doit développer une feature composée de modules spécialisés.

### 5.1 Module d’ingestion
Responsabilités : recevoir les fichiers source, lire un controller Java, éventuellement lire les fichiers liés, normaliser les entrées, préparer le texte et la structure pour analyse.

### 5.2 Module d’analyse structurelle
Responsabilités : identifier classes, champs, injections, méthodes, repérer les annotations `@FXML`, `@Component`, `@Autowired`, détecter les handlers, mesurer la complexité structurelle, cartographier les dépendances.

### 5.3 Module de classification des responsabilités
Responsabilités : distinguer UI, présentation, orchestration, métier, technique, repérer les méthodes qui construisent des objets métier, les méthodes à forte complexité conditionnelle, les zones appelant plusieurs services et les calculs de statut.

### 5.4 Module de stratégie de refactoring
Responsabilités : déterminer les lots de migration, proposer l’architecture cible, décider s’il faut un ViewModel, des use cases, des policies, un assembler, une strategy, un bridge temporaire.

### 5.5 Module de génération de code
Responsabilités : générer un controller aminci, un ViewModel, les use cases, les policies, les gateways/adaptateurs Spring, les assemblers, les stratégies, et si besoin les classes de bridge transitoire.

### 5.6 Module d’explication / raisonnement
Responsabilités : expliquer le diagnostic, expliquer pourquoi telle logique sort du controller, pourquoi telle classe est créée, expliquer l’ordre de migration, produire un document de raisonnement détaillé.

### 5.7 Module conversationnel GPT-like
Responsabilités : permettre une interaction par étape, conserver le contexte du controller courant, répondre à des demandes telles que “fais le lot 2”, “génère seulement le ViewModel”, “donne-moi le raisonnement détaillé”, “branche les adaptateurs Spring”, “produis un markdown pour Codex”, et permettre des raffinements successifs.

---

## 6. Architecture cible de refactoring que la feature doit viser

```text
FXML
  ↓
Controller JavaFX
  ↓
ViewModel / Presenter
  ↓
Use Cases / feature Services
  ↓
Policies / Validators / Domain Services
  ↓
Gateways / Adaptateurs Spring
  ↓
Services historiques / REST clients / impression / fichiers / hardware / launchers
```

### 6.1 Pourquoi cette cible

Cette cible est adaptée au contexte JavaFX + Spring existant, car elle :
- respecte la place naturelle de FXML et du controller ;
- sort le métier et l’orchestration du controller ;
- conserve l’existant via adaptateurs ;
- favorise une migration progressive ;
- rend les décisions métier testables hors UI.

### 6.2 Interprétation précise des couches

#### Controller JavaFX
Rôle : adaptateur UI. Conserve les bindings, les événements utilisateur, les détails visuels et les appels vers le ViewModel.

#### ViewModel
Rôle : état de présentation. Conserve la sélection courante, les modes écran, l’état loading, les flags boutons, les messages utilisateur, et une coordination légère avec les use cases.

#### Use Cases
Rôle : intentions utilisateur. Exemples : sauvegarder, supprimer, ajouter une pièce jointe, imprimer, lancer un examen, acquérir des résultats.

#### Policies / Validators
Rôle : décisions stables. Exemples : calcul de statut, faisabilité d’une action, règles de transition, règles métier spécifiques.

#### Gateways / Adaptateurs
Rôle : couture avec l’existant. Exemples : accès REST, mapping DTO, accès fichiers, impression, lancement outil, intégration matériel.

#### Assemblers
Rôle : transformer les données d’écran en commandes métier ou structures applicatives.

#### Strategies
Rôle : gérer les variantes de workflow lorsque plusieurs scénarios existent.

---

## 7. Raisonnement détaillé que la feature doit appliquer sur n’importe quel controller

### 7.1 Étape d’observation initiale
Pour tout controller fourni, la feature doit relever : taille du fichier, nombre de composants FXML, nombre de champs d’état, nombre de services injectés, nombre de handlers, nombre de méthodes privées complexes, nombre d’appels techniques externes.

Pourquoi : avant de refactorer, il faut qualifier la nature de la dette : dette UI, dette métier, dette de workflow, dette technique, dette de couplage.

### 7.2 Étape de cartographie des responsabilités
Pour chaque méthode, la feature doit se demander : est-ce de l’UI pure, de l’état de présentation, de l’orchestration applicative, une décision métier, ou de la technique ?

Pourquoi : le controller est souvent un mélange. Il faut transformer un fichier monolithique en carte de responsabilités.

### 7.3 Étape d’identification des intentions utilisateur
Pour chaque handler important, la feature doit identifier l’intention réelle.
Exemples : `onSave`, `onAddAttachment`, `onAcquire`, `onLaunch`, `onPrint`, `onDelete`.

Pourquoi : une intention utilisateur correspond presque toujours à un use case.

### 7.4 Étape d’identification des décisions stables
la feature doit repérer les endroits où le controller décide d’un statut, d’un droit d’action, d’une transition, d’une éligibilité ou d’une branche métier.

Pourquoi : ces décisions sont souvent stables, réutilisables et testables. Elles doivent devenir des policies.

### 7.5 Étape d’identification de la fabrication d’objets
la feature doit repérer les zones où le controller instancie un objet métier, remplit un DTO ou une commande, fusionne plusieurs champs UI, prépare une liste d’objets depuis des composants dynamiques, ou ajoute des métadonnées avant sauvegarde.

Pourquoi : ces zones signalent la nécessité d’un assembler ou d’un mapper de formulaire.

### 7.6 Étape d’identification des variantes de workflow
la feature doit repérer les `if/else` ou `switch` qui choisissent un traitement selon un type, un mode, un matériel, un protocole, un format ou une catégorie fonctionnelle.

Pourquoi : quand ces branches sont substantielles, il faut créer des stratégies.

### 7.7 Étape d’identification de l’existant à préserver
la feature doit repérer les services historiques utiles à conserver : services REST stables, services d’impression, clients de fichiers, launchers, services documentaires, intégration hardware.

Pourquoi : le refactoring doit les encapsuler, pas forcément les réécrire.

---

## 8. Stratégie de migration en 5 lots que la feature doit implémenter

### Lot 1 — Diagnostic et cible architecturale
Objectif : comprendre le controller sans modifier trop vite.

À produire : diagnostic synthétique, cartographie des responsabilités, liste des handlers majeurs, liste des zones à forte complexité, proposition d’architecture cible, tableau “reste dans le controller / sort du controller”.

Raisonnement : cette étape sert à éviter les refactorings aveugles.

### Lot 2 — Introduction du ViewModel, des use cases et de la première policy
Objectif : créer les premiers points d’appui de la future architecture.

À produire : `XxxViewModel`, premiers use cases, première policy structurante, souvent le statut ou l’éligibilité.

Raisonnement : on commence ici parce que les controllers surchargés mélangent presque toujours l’état de l’écran et les actions utilisateur. Sortir l’état vers le ViewModel et nommer les intentions via des use cases stabilise la migration.

### Lot 3 — Migration effective des flux les plus chargés
Objectif : sortir les gros flux métier hors du controller.

Flux typiquement ciblés : sauvegarde, pièces jointes, impression, acquisition, lancement, post-traitement, recalcul de statut.

À produire : controller aminci sur ces flux, use cases réellement appelés, ports de dépendance si nécessaire, policy de statut ou service équivalent réellement utilisée.

Raisonnement : les gros flux concentrent le plus de dette structurelle.

### Lot 4 — Adaptateurs Spring branchés sur l’existant
Objectif : reconnecter l’architecture cible aux services legacy.

À produire : gateways, adaptateurs Spring, éventuellement un bridge transitoire minimal.

Raisonnement : sans adaptateurs, les use cases restent théoriques.

### Lot 5 — Assembleur de formulaire et stratégies de workflow
Objectif : sortir les dernières dépendances structurelles au controller.

À produire : assembleur de formulaire, stratégies spécialisées, éventuellement un routeur de stratégies, suppression progressive du bridge.

Raisonnement : tant que la construction des données métier et les variantes de workflow restent dans le controller, la dépendance structurelle à l’écran persiste.

---

## 9. Règles de décision détaillées que la feature doit suivre

### 9.1 Quand créer un ViewModel
Créer un ViewModel si l’écran a plusieurs modes, plusieurs états de boutons, un état de sélection important, des messages à afficher, un état de chargement, un dirty state, ou un pilotage d’UI non trivial.

### 9.2 Quand créer un use case
Créer un use case dès qu’une action utilisateur appelle plusieurs services, modifie plusieurs objets, provoque plusieurs effets de bord, ou correspond à une intention fonctionnelle claire.

### 9.3 Quand créer une policy
Créer une policy quand la logique représente une décision, dépend d’états métier, peut être testée indépendamment, ou est susceptible d’être réutilisée.

### 9.4 Quand créer un assembler
Créer un assembler quand le controller lit beaucoup de champs UI, prépare une structure pour sauvegarde ou update, répète une logique de fabrication d’objet, ou mélange données d’écran et métadonnées système.

### 9.5 Quand créer une strategy
Créer une strategy quand le controller choisit plusieurs scénarios selon type, mode, matériel, protocole, format ou catégorie fonctionnelle.

### 9.6 Quand accepter un bridge temporaire
Accepter un bridge seulement si l’extraction immédiate serait trop risquée, si certaines données sont encore enfouies dans l’UI, si le bridge a une surface minimale et si sa suppression ultérieure est explicitement prévue.

---

## 10. Règles de transformation concrètes à implémenter dans la feature

### 10.1 Transformation d’un handler lourd
Avant : un handler lit plusieurs widgets, valide, appelle plusieurs services, décide du métier, rafraîchit l’UI et affiche un message.

Après :
- controller → délègue ;
- ViewModel → prépare et restitue l’état ;
- use case → orchestre ;
- policy → décide ;
- gateway → parle à l’existant.

### 10.2 Extraction des objets construits depuis l’écran
Quand un controller construit un objet métier ou un DTO à partir de l’écran, la feature doit générer un `FormDataAssembler`, un `CommandFactory` ou un `InputMapper`.

### 10.3 Encapsulation des appels externes
Les appels vers REST, impression, fichiers, hardware, launchers ne doivent pas rester dans le controller ni migrer vers le ViewModel. Ils doivent être encapsulés dans des gateways/adaptateurs.

### 10.4 Extraction de la logique de statut
Toute logique de statut ou d’habilitation très conditionnelle doit devenir une policy ou un service métier testable.

### 10.5 Gestion des variantes
Les variantes de traitement doivent devenir des stratégies lorsque les branches sont substantielles.

### 10.6 Marquage explicite du transitoire
Toute classe de bridge générée doit être clairement documentée comme temporaire.

---

## 11. Fonctionnement conversationnel GPT-like attendu

la feature devra permettre des interactions de ce type :
- “analyse ce controller” ;
- “fais le lot 1” ;
- “génère le ViewModel” ;
- “fais la migration des flux d’enregistrement et d’impression” ;
- “branche les adaptateurs Spring” ;
- “donne le raisonnement détaillé en markdown” ;
- “génère les strategies” ;
- “réécris seulement le controller” ;
- “ne touche pas aux services legacy” ;
- “produis un plan de migration compilable”.

### 11.1 Comportement attendu
la feature doit conserver le contexte du controller analysé, pouvoir travailler par lots, être capable de réviser sa proposition, produire des réponses structurées, générer du code et des documents.

### 11.2 Posture attendue
la feature doit se comporter comme un assistant d’architecture pragmatique. Elle ne doit pas forcer une pureté théorique. Elle doit arbitrer entre propreté cible, risque de migration, réutilisation de l’existant, testabilité et lisibilité.

---

## 12. Artefacts que la feature doit être capable de générer

### 12.1 Artefacts d’analyse
- diagnostic markdown ;
- cartographie des responsabilités ;
- tableau de migration ;
- checklist des risques.

### 12.2 Artefacts de code
- controller aminci ;
- ViewModel ;
- use cases ;
- policies ;
- gateways ;
- adaptateurs Spring ;
- assemblers ;
- stratégies ;
- bridges temporaires.

### 12.3 Artefacts d’explication
- document de raisonnement détaillé ;
- guide de migration ;
- consignes pour intégration progressive.

---

## 13. Contraintes de conception que Codex doit respecter en développant la feature

### 13.1 Priorité à la lisibilité
Le code généré doit être lisible et cohérent avec une architecture de couches.

### 13.2 Priorité à la progressivité
la feature doit proposer des lots de migration et non une refonte totale imposée.

### 13.3 Priorité à la réutilisation de l’existant
Les services legacy doivent être encapsulés avant d’être remplacés.

### 13.4 Priorité à l’explicabilité
la feature doit toujours pouvoir expliquer pourquoi une extraction est proposée.

### 13.5 Priorité à la testabilité
Les policies, assemblers et use cases générés doivent pouvoir être testés hors JavaFX.

---

## 14. Modèle de packages recommandé pour le code généré

```text
ui/
  xxx/
    XxxController.java
    XxxViewModel.java
    XxxState.java

feature/
  xxx/
    SaveXxxUseCase.java
    DeleteXxxUseCase.java
    PrintXxxUseCase.java
    LaunchXxxUseCase.java
    AcquireXxxUseCase.java
    AddAttachmentUseCase.java

feature/assembler/
    XxxFormDataAssembler.java

domain/
  xxx/
    XxxStatusPolicy.java
    XxxEligibilityPolicy.java
    XxxBusinessValidator.java

infrastructure/adapter/
    SpringLegacyXxxGatewayAdapter.java
    SpringLegacyXxxDocumentAdapter.java
    SpringLegacyXxxTechnicalAdapter.java

migration/bridge/
    XxxLegacyControllerBridge.java

feature/strategy/
    XxxWorkflowStrategy.java
    XxxTypeAStrategy.java
    XxxTypeBStrategy.java
```

---

## 15. Procédure détaillée que la feature doit suivre sur un controller donné

Pour chaque controller en entrée, la feature doit suivre la séquence suivante :
1. lire le controller ;
2. extraire la structure de la classe ;
3. lister les dépendances injectées ;
4. lister les composants FXML ;
5. lister les handlers publics ;
6. détecter les méthodes privées structurantes ;
7. classifier chaque zone par responsabilité ;
8. identifier les flux majeurs ;
9. identifier les décisions stables ;
10. identifier les objets construits depuis l’écran ;
11. identifier les variantes de workflow ;
12. identifier l’existant à préserver ;
13. construire l’architecture cible ;
14. proposer les lots de migration ;
15. générer les classes du lot demandé ;
16. produire l’explication détaillée.

---

## 16. Ce que la feature doit éviter

Elle doit éviter :
- de déplacer du code sans changer sa responsabilité ;
- de créer des abstractions inutiles ;
- de mettre du métier profond dans le ViewModel ;
- de faire des use cases qui ne sont que des wrappers vides ;
- de faire des adaptateurs qui redeviennent des monolithes ;
- de garder durablement un bridge trop large ;
- de prétendre supprimer tous les couplages en une seule étape.

---

## 17. Résultat attendu final

Si la feature est correctement développée, elle devra être capable, pour un controller JavaFX + Spring donné, de produire une transformation progressive vers un modèle de ce type :
- controller FXML aminci ;
- ViewModel portant l’état de présentation ;
- use cases correspondant aux intentions utilisateur ;
- policies pour les décisions métier stables ;
- gateways/adaptateurs encapsulant l’existant ;
- assemblers pour la fabrication des données ;
- stratégies pour les variantes de workflow ;
- explication détaillée de chaque étape.

Le résultat attendu n’est pas seulement du code refactoré. Le résultat attendu est une **feature experte** qui sait diagnostiquer, expliquer, transformer et générer une trajectoire de refactoring fiable.

---

## 18. Instruction finale à donner à Codex

Construis une feature GPT-like spécialisée dans le refactoring de controllers JavaFX + Spring.

Cette feature doit :
- accepter en entrée un controller Java source ;
- analyser ses responsabilités ;
- diagnostiquer les zones problématiques ;
- proposer une architecture cible en couches ;
- générer une migration progressive en plusieurs lots ;
- produire le code des nouvelles classes ;
- produire des documents de raisonnement détaillé ;
- permettre une interaction conversationnelle par étape ;
- privilégier une refactorisation pragmatique et progressive ;
- encapsuler l’existant avant de le remplacer ;
- viser un résultat de type `Controller + ViewModel + UseCases + Policies + Gateways + Assemblers + Strategies`.

Utilise ce document comme spécification fonctionnelle, architecturale et méthodologique principale.

