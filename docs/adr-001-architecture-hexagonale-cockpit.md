# ADR-001 - Architecture hexagonale du cockpit de refactoring

## Statut

Accepted

## Contexte

Le projet pilote un moteur d'analyse et de refactoring de controllers JavaFX sans dupliquer la logique experte dans le frontend. Le backend Spring Boot 4.0.3 est deja structure en architecture hexagonale. Le frontend Angular 21 doit rester un client de contrats REST valides.

## Decision

Nous gardons un backend hexagonal strict et un frontend thin client.

- Le domaine ne depend ni de Spring, ni de JPA, ni de DTO REST.
- Les cas d'utilisation vivent dans `application`.
- Les dependances techniques sont encapsulees dans `adapters`.
- Les contrats HTTP sont la seule frontiere partagee avec Angular.
- Les workflows d'analyse sont exposes par sessions et statuts, pas par logique embarquee dans le frontend.

## Consequences

- Le code metier reste testable sans Spring.
- Le frontend peut evoluer sans connaitre les details internes du moteur.
- Les changements de persistence ou de fournisseur IA restent localises aux adapters.
- Les contrats REST et les DTO doivent etre maintenus explicitement, sinon les ecrans Angular se decalent du backend.

## Alternatives ecartees

- Mettre la logique d'analyse dans Angular: refuse, car cela dupliquerait le metier.
- Exposer des entites JPA directement: refuse, car cela couplerait l'API a la persistence.
- Construire un monolithe de services techniques sans ports: refuse, car cela casserait la testabilite et la separation des responsabilites.

## Regles associees

- Toute nouvelle fonctionnalite backend commence par un port.
- Tout endpoint REST mappe un use case ou un composeur d'application.
- Les logs debug restent non sensibles et correles via `X-Correlation-Id`.
- Les modifications frontend suivent toujours un contrat backend deja stabilise.
