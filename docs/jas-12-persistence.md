# JAS-12 - Persistance PostgreSQL pour les sessions d'analyse

## objectif

Preparer la couche de persistance PostgreSQL pour les sessions d'analyse dans le respect strict de l'architecture hexagonale. Le domaine reste pur, sans aucune annotation technique. Les adapters encapsulent tout le detail PostgreSQL et JPA.

## perimetre

- Creation du port de persistence sortant `AnalysisSessionPort`
- Creation de l'objet domaine `AnalysisSession` (record Java immuable)
- Creation de l'enum domaine `AnalysisStatus`
- Creation du schema SQL conceptuel de reference
- Exclusion explicite : adapter JPA, migrations Flyway, tests de persistence

## faits

- Le package racine est `ff.ss.javaFxAuditStudio`.
- Le backend cible JDK 21 et Spring Boot 4.0.3.
- L'architecture hexagonale est strictement appliquee : `domain`, `application`, `ports`, `adapters`, `configuration`.
- Les ports sortants existants (`WorkbenchCatalogPort`) sont de simples interfaces Java sans annotation.
- Les records domaine existants (`WorkbenchOverview`, `RefactoringLot`, `AgentOverview`) ne portent aucune annotation JPA ni Spring.
- Le repertoire `backend/src/main/resources/db/` a ete cree pour accueillir le schema conceptuel.

## interpretations

- `AnalysisSession` est un agregat racine minimal pour le suivi du cycle de vie d'une session.
- `sourceSnippetRef` est une reference (chemin, URI, identifiant de stockage) et non le contenu brut, conformement au principe de separation des responsabilites de stockage.
- Le port `AnalysisSessionPort` est un port secondaire (driven) oriente persistence, coherent avec le pattern hexagonal.
- `findById` retourne `Optional<AnalysisSession>` pour interdire explicitement le retour de null.
- `findAll` retourne `List<AnalysisSession>`, liste vide si aucun resultat.

## hypotheses

- La colonne `status` est stockee en `VARCHAR` en base pour eviter la dependance a un type enum PostgreSQL natif qui compliquerait les migrations evolutives.
- La valeur `source_snippet_ref` pointe vers un systeme de stockage externe (systeme de fichiers, stockage objet) dont le schema n'est pas dans le perimetre JAS-12.
- L'identifiant `session_id` est genere par la couche application (UUID) avant appel au port de persistence.

## incertitudes

- La strategie de generation de `session_id` (UUID v4, UUID v7, sequence) n'est pas encore arretee. A confirmer lors de l'implementation de l'adapter JPA.
- La strategie de pagination pour `findAll` n'est pas specifiee. Un `findAll(Pageable)` devra probablement etre ajoute lors de l'implementation reelle.
- Le systeme de stockage cible pour les extraits de source (reference par `sourceSnippetRef`) reste a definir.

## decisions

| Decision | Justification |
|---|---|
| `sourceSnippetRef` reference externe plutot que contenu brut | Evite le stockage de texte volumineux en base, simplifie les indexes, preserve la separation des responsabilites |
| `Optional<AnalysisSession>` sur `findById` | Interdit le retour de null, conforme aux regles qualite Java du projet |
| `List<AnalysisSession>` vide sur `findAll` | Conforme a la regle "pas de null retourne" |
| `status` en `VARCHAR` | Flexibilite d'evolution sans migration de type enum PostgreSQL |
| `created_at TIMESTAMP WITH TIME ZONE` | Toujours UTC, evite les ambiguites de fuseau horaire |
| Constructeur compact avec gardes `requireNonNull` | Assure l'invariant metier sans annotation de validation externe dans le domaine |

## livrables

| Artefact | Chemin |
|---|---|
| Enum domaine | `backend/src/main/java/ff/ss/javaFxAuditStudio/domain/workbench/AnalysisStatus.java` |
| Record domaine | `backend/src/main/java/ff/ss/javaFxAuditStudio/domain/workbench/AnalysisSession.java` |
| Port sortant | `backend/src/main/java/ff/ss/javaFxAuditStudio/application/ports/out/AnalysisSessionPort.java` |
| Schema conceptuel SQL | `backend/src/main/resources/db/schema-conceptuel.sql` |
| Document de decisions | `docs/jas-12-persistence.md` (ce fichier) |

## schema conceptuel

```sql
CREATE TABLE analysis_session (
    session_id      VARCHAR                  NOT NULL,  -- identifiant metier (UUID)
    controller_name VARCHAR                  NOT NULL,  -- nom du controller analyse
    source_snippet_ref VARCHAR,                         -- reference externe (pas le contenu brut)
    status          VARCHAR                  NOT NULL,  -- PENDING | RUNNING | COMPLETED | FAILED
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,  -- toujours UTC
    CONSTRAINT pk_analysis_session PRIMARY KEY (session_id)
);
```

## ports de persistence identifies

### Port sortant : `AnalysisSessionPort`

Package : `ff.ss.javaFxAuditStudio.application.ports.out`

```
AnalysisSession save(AnalysisSession session)
Optional<AnalysisSession> findById(String sessionId)
List<AnalysisSession> findAll()
```

Ce port sera implemente par un adapter JPA dans `adapters/out/persistence/` lors d'une story ulterieure.

## ce qui reste a faire (hors JAS-12)

- [ ] Implementer `AnalysisSessionJpaAdapter` dans `adapters/out/persistence/`
- [ ] Creer l'entite JPA `AnalysisSessionEntity` (annotations JPA uniquement dans l'adapter)
- [ ] Creer le mapper domaine <-> entite JPA
- [ ] Creer la premiere migration Flyway (`V1__create_analysis_session.sql`)
- [ ] Ecrire les tests d'integration de persistence (`@DataJpaTest` ou Testcontainers)
- [ ] Definir la strategie de pagination pour `findAll`
- [ ] Confirmer la strategie de generation de `session_id`

## dependances

- `backend-hexagonal` : doit creer le cas d'usage qui consomme `AnalysisSessionPort`
- `qa-backend` : validera les tests d'integration de persistence
- `test-automation` : couvrira les scenarios de bout en bout impliquant les sessions
- `devops-ci-cd` : integrera Flyway et la base de test dans le pipeline

## verifications

- [x] `AnalysisSession` : aucune annotation Spring, JPA ou PostgreSQL
- [x] `AnalysisStatus` : enum pur sans dependance externe
- [x] `AnalysisSessionPort` : interface pure sans annotation Spring
- [x] `findById` retourne `Optional`, pas de null
- [x] `findAll` retourne `List`, pas de null
- [x] Constructeur compact avec gardes `requireNonNull` sur les champs obligatoires
- [x] `sourceSnippetRef` nullable (reference optionnelle, pas le contenu brut)
- [x] Schema SQL avec commentaires sur chaque colonne
- [x] Aucune migration Flyway creee (hors perimetre)

## handoff

```text
handoff:
  vers: backend-hexagonal
  preconditions:
    - AnalysisSession, AnalysisStatus et AnalysisSessionPort sont stables et compiles
    - Le schema conceptuel est valide
  points_de_vigilance:
    - Ne pas importer de types JPA ou Spring dans domain/workbench
    - sourceSnippetRef est nullable (Optional non utilise dans le record pour rester simple)
    - La pagination de findAll est a prevoir des la conception du cas d'usage
  artefacts_a_consulter:
    - backend/src/main/java/ff/ss/javaFxAuditStudio/application/ports/out/AnalysisSessionPort.java
    - backend/src/main/java/ff/ss/javaFxAuditStudio/domain/workbench/AnalysisSession.java
    - backend/src/main/resources/db/schema-conceptuel.sql
    - docs/jas-12-persistence.md

handoff:
  vers: qa-backend
  preconditions:
    - L'adapter JPA est implemente (story suivante)
    - Une base PostgreSQL de test est disponible (Testcontainers ou H2)
  points_de_vigilance:
    - Verifier que le mapper JPA ne laisse pas fuiter d'entites JPA hors de l'adapter
    - Verifier les contraintes NOT NULL en base vs les gardes domaine
  artefacts_a_consulter:
    - backend/src/main/resources/db/schema-conceptuel.sql
    - docs/jas-12-persistence.md

handoff:
  vers: devops-ci-cd
  preconditions:
    - La premiere migration Flyway est creee
  points_de_vigilance:
    - Le schema-conceptuel.sql n'est PAS une migration Flyway executable
    - Les migrations Flyway doivent etre dans db/migration/ avec versionning strict
  artefacts_a_consulter:
    - backend/src/main/resources/db/schema-conceptuel.sql
```
