# Audit Sanitization et Gouvernance

Date de reference : 2026-03-27

## Objet

Ce document formalise la trace attendue par `P2-03` pour conserver `QW-1` comme
livre. Il synthesise l'architecture de sanitisation actuellement en place,
les decisions de gouvernance associees et les points restant sous surveillance.

## Perimetre audite

- `backend/src/main/java/ff/ss/javaFxAuditStudio/adapters/out/sanitization/`
- `backend/src/main/java/ff/ss/javaFxAuditStudio/domain/sanitization/`
- `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/SemgrepScanProperties.java`
- `backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/AiEnrichmentOrchestraConfiguration.java`
- `backend/src/main/resources/semgrep/sanitization-rules.yaml`
- les tests `backend/src/test/java/.../sanitization/`

## Architecture en place

Le pipeline de sanitisation reste un sous-systeme technique de l'hexagone backend.
Le domaine ne transporte pas de dependance Spring ou Semgrep ; il expose seulement
les concepts `SanitizedBundle`, `SanitizationReport`, `SanitizationTransformation`
et `SanitizationRefusedException`.

L'assemblage concret est realise dans `AiEnrichmentOrchestraConfiguration`.
L'ordre courant du pipeline est :

1. neutralisation des identifiants metier
2. suppression ou masquage des secrets
3. nettoyage des commentaires
4. substitutions de donnees
5. scan Semgrep post-sanitisation
6. detecteur final de marqueurs sensibles residuels

Le scan Semgrep reste optionnel et degradable. En cas d'absence de la CLI, de
timeout ou d'erreur d'execution, le pipeline emet un warning et bascule en mode
gracieux sans casser les autres traitements.

## Garanties de gouvernance

- Le code source sanitise n'est jamais expose dans les logs applicatifs.
- L'audit LLM ne conserve qu'un hash du payload sanitise, jamais le contenu brut.
- Les prompts et bundles transmis au fournisseur IA restent precedes par la
  sanitisation backend.
- Les regles Semgrep versionnees sur le classpath sont lisibles et auditables par
  l'equipe securite sans recompilation du code Java.
- La `denylist` Semgrep est configurable par proprietes pour ajouter des termes
  internes sensibles sans modifier le code source.

## Etat du lot sprint 6

Les points suivants sont consideres stabilises pour la cloture documentaire :

- la cartographie QW-1 est consolidee par cette note de reference
- le chargement des regles Semgrep n'est plus limite au jeu minimal historique
- le corpus de tests couvre les regles statiques, les regles generiques et la
  denylist configurable
- la documentation operative indique ou se trouvent les artefacts de gouvernance

## Risques encore ouverts

- Semgrep reste desactive par defaut ; une activation production doit etre
  accompagnee d'un calibrage sur un corpus reel de projet.
- Le mode gracieux protege la disponibilite mais peut masquer une degradation
  de couverture securite si les warnings ne sont pas suivis.
- Les fichiers non-Java sont mieux couverts qu'auparavant, mais le portefeuille
  de regles doit continuer a evoluer avec les formats effectivement rencontres.

## Trace observable a conserver

- backlog de reference : `jira/backlog-sanitisation-audit.md`
- configuration runtime : `backend/src/main/resources/application.properties`
- regles de scan : `backend/src/main/resources/semgrep/sanitization-rules.yaml`
- tests de non-regression : `backend/src/test/java/ff/ss/javaFxAuditStudio/adapters/out/sanitization/`

Cette note sert de point d'entree unique pour les revues `gouvernance`,
`documentation-technique` et `transparence-openai` lorsqu'il faut prouver ce qui
est sanitise, ou le controle est applique, et quelle evidence technique permet
de le verifier.
