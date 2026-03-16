# Architecture Backend Initiale

## Contexte

Le backend implemente le socle hexagonal de la nouvelle application de refactoring progressif de controllers JavaFX + Spring.

Stack :

- JDK 21
- Spring Boot `4.0.3`
- architecture hexagonale stricte

## Objectif du premier increment

Poser un backend executable qui :

- expose un premier endpoint REST de workbench ;
- assemble un use case via la couche `configuration` ;
- conserve un domaine pur ;
- prepare l'integration du moteur d'analyse et du frontend Angular.

## Packages

```text
ff.ss.javaFxAuditStudio
  configuration/
  domain/
    workbench/
  application/
    ports/
      in/
      out/
    service/
  adapters/
    in/
      rest/
        dto/
        mapper/
    out/
      catalog/
```

## Repartition des responsabilites

### domain

- porte les objets `WorkbenchOverview`, `RefactoringLot` et `AgentOverview`
- aucune annotation Spring
- aucune dependance REST ou persistence

### application

- expose `GetWorkbenchOverviewUseCase`
- depend du port sortant `WorkbenchCatalogPort`
- orchestre la recuperation des donnees sans logique technique

### adapters.in.rest

- expose `/api/v1/workbench/overview`
- mappe le domaine vers des DTO REST
- ne contient pas de logique metier transverse

### adapters.out.catalog

- fournit un catalogue statique de depart
- servira de point de remplacement futur pour les donnees de configuration, la persistence ou le moteur

### configuration

- assemble les use cases
- configure le CORS de developpement pour Angular

## Endpoint initial

- `GET /api/v1/workbench/overview`

Usage :

- alimenter le cockpit Angular ;
- afficher la synthese produit ;
- visualiser les lots et les agents/modeles cibles.

## Evolution prevue

### Court terme

- sessions d'analyse ;
- import de sources Java/FXML ;
- lancement de diagnostics ;
- stockage des resultats en PostgreSQL.

### Moyen terme

- endpoints de diagnostic par ecran/controller ;
- endpoints de plan de migration ;
- endpoints de restitution et generation de code ;
- observabilite de workflow et correlation.

## Decisions

- Le premier port sortant est volontairement statique pour garder le socle simple et testable.
- Le frontend consomme un contrat deja oriente produit, pas une structure interne du moteur.
- Le backend reste le point de verite pour les capacites exposees a Angular.
