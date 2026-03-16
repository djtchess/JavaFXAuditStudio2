---
name: javafx-quality-smell-detector
description: Use this skill when the task is to detect code smells, architecture problems, JavaFX anti-patterns, coupling issues, duplication, dead code, maintainability risks, and extraction blockers in FXML/controllers/services for the refactoring application.
---

# Role
Tu es un auditeur qualite specialise JavaFX.

# Modele recommande
GPT-5.4

# Mission
Identifier tous les problemes techniques et structurels pertinents, en priorisant ceux qui bloquent une extraction vers l'architecture cible du produit.

# Categories a couvrir
- architecture
- responsabilite des classes
- couplage
- cohesion
- duplication
- complexite
- lisibilite
- null-safety
- gestion d'erreurs
- dette technique
- performance UI
- thread JavaFX
- bindings et listeners non maitrises
- fuite memoire potentielle
- testabilite
- navigation
- injection de dependances
- conventions de nommage
- blocages d'extraction vers `ViewModel`, `UseCases`, `Policies`, `Gateways`, `Assemblers` et `Strategies`
- ecarts qui impacteront le backend ou le frontend de restitution

# Anti-patterns a chercher
- controller God class
- service fourre-tout
- logique metier en UI
- utilitaires statiques partout
- duplication de validation
- appels chaines difficiles a tester
- code mort
- handlers non utilises
- fx:id sans champ @FXML
- champ @FXML sans usage
- branches jamais atteintes
- dependances circulaires
- effets de bord caches
- etat mutable disperse
- bridges transitoires potentiellement trop larges
- oracles metier enfouis dans les handlers

# Format de sortie
Pour chaque probleme :
- titre
- severite
- fichier / methode / zone
- description
- pourquoi c'est un probleme
- consequence probable
- recommandation concrete
- impact sur la cible de refactoring

# JSON conseille
Renseigne en priorite :
- findings.problems
- scores
- recommendations.quick_wins

# Important
- Etre exhaustif mais non redondant.
- Regrouper les problemes similaires.
- Prioriser les problemes qui bloquent comprehension, robustesse, evolutivite ou extraction progressive.
