# Contrat Inter-Agent

## Objet

Ce contrat standardise les echanges entre les agents qui concoivent, implementent et valident la nouvelle application de refactoring progressif de controllers JavaFX + Spring.

Le produit cible combine :

- un frontend Angular `21.x` de pilotage conversationnel et de restitution ;
- un backend hexagonal JDK 21 / Spring Boot `4.0.3` deja initialise dans `backend/` ;
- un moteur d'analyse/refactoring capable de produire diagnostic, architecture cible, lots de migration, code genere et explication detaillee.

## Structure minimale obligatoire

Chaque agent doit produire une sortie avec les rubriques suivantes :

1. `objectif`
2. `perimetre`
3. `faits`
4. `interpretations`
5. `hypotheses`
6. `incertitudes`
7. `decisions`
8. `livrables`
9. `dependances`
10. `verifications`
11. `handoff`

## Regles d'ecriture

- `faits` contient uniquement ce qui est observe dans les fichiers, contrats, prompts, schemas ou versions verifiees.
- `interpretations` explicite les deductions utiles a la suite des travaux.
- `hypotheses` liste ce qui n'est pas confirme mais retenu pour avancer.
- `incertitudes` signale ce qui doit etre arbitre ou verifie avant implementation.
- `decisions` contient les choix engages par l'agent dans son perimetre.
- `livrables` liste les artefacts concretement attendus ou produits.
- `handoff` identifie l'agent suivant, les preconditions et les points de vigilance.

## Contraintes produit

- Aucun agent ne redefinit seul l'architecture hexagonale backend.
- Aucun agent frontend ne cree de logique metier centrale cote Angular.
- Aucun agent backend n'introduit de dependance JavaFX dans `domain` ou `application`.
- Les agents de generation de code se calent sur les contrats valides et la selection de modele documentee.
- Toute proposition de lot doit rester compatible avec la migration progressive en 5 lots du guide.

## Granularite attendue par type d'agent

### Agents de cadrage

Ils doivent produire :

- objectifs cibles ;
- limites de perimetre ;
- arbitrages ;
- risques ;
- criteres d'acceptation.

### Agents de design technique

Ils doivent produire :

- architecture cible ;
- packages ;
- ports ;
- contrats d'entree/sortie ;
- decisions de migration ;
- impact backend/frontend.

### Agents d'implementation

Ils doivent produire :

- fichiers a creer ou modifier ;
- sequence d'implementation ;
- hypotheses de compilation ;
- strategie de logs ;
- strategie de tests.

### Agents de revue et QA

Ils doivent produire :

- findings classes par severite ;
- risques de regression ;
- manques de tests ;
- ecarts au contrat ;
- conditions de sortie.

## Format de handoff recommande

```text
handoff:
  vers: <agent-cible>
  preconditions:
    - ...
  points_de_vigilance:
    - ...
  artefacts_a_consulter:
    - ...
```
