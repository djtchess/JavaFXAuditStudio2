# JAS-03 — Poser les exigences de sécurité et d'exposition

Epic : JAS-EPIC-01
Statut : CADRAGE
Date : 2026-03-16
Agent : securite
Dépend de : JAS-02

---

## objectif

Identifier les flux sensibles exposés par l'API du workbench (JAS-02), fixer les principes de journalisation non-sensible, et lister les exigences de configuration de sécurité à respecter pour les epics d'implémentation (Epic 02 backend, Epic 03 frontend). Ce document ne constitue pas une analyse de menaces exhaustive mais pose les garde-fous fondamentaux pour un démarrage sûr.

---

## perimetre

- Sécurité des endpoints REST définis en JAS-02
- Principe de journalisation applicable au backend Spring Boot 4.0.3
- Configuration CORS, validation des entrées, exposition réseau
- Hors périmètre : authentification/autorisation utilisateur (traitée dans un epic dédié), sécurité de l'infrastructure réseau, chiffrement au repos PostgreSQL

---

## faits

- Le backend Spring Boot 4.0.3 expose des endpoints sur `/api/v1/workbench/`.
- Les endpoints reçoivent du code source Java brut (`controllerSourceCode` dans `SubmitSourceRequest`) : contenu arbitraire non contrôlé.
- Les endpoints reçoivent des prompts conversationnels libres (`ChatRequest.prompt`) : contenu arbitraire.
- Les endpoints produisent du code Java généré (`GeneratedFileResponse.fileContent`) : artefacts potentiellement exécutables.
- La configuration CORS de développement pour Angular existe déjà dans le package `configuration/`.
- L'architecture hexagonale isole le domaine : aucune dépendance Spring dans `domain/` ou `application/`.
- Spring Boot 4.0.3 inclut Spring Security 7.x. Aucune configuration de sécurité custom n'est documentée à ce stade.
- Les identifiants de session sont des UUID (JAS-02).
- Le contexte est un outil de développement (usage interne par des développeurs Java), pas une application grand public.

---

## interpretations

- Le flux "soumission de source Java" est le flux le plus sensible : un code source volumineux, potentiellement confidentiel, est transmis en clair dans le corps d'une requête HTTP. En développement local ce risque est limité ; en déploiement partagé il exige HTTPS obligatoire.
- Le flux "génération de code" produit du code exécutable. Le backend ne doit pas exécuter ce code. La génération doit rester confinée à la production de texte/JSON, sans évaluation dynamique ni écriture sur le système de fichiers du serveur en dehors d'un répertoire contrôlé.
- Les prompts libres (`ChatRequest.prompt`) ne sont pas directement injectés dans un LLM par le backend dans l'Epic 01 (le moteur est un stub). Cependant les règles de validation doivent être posées dès maintenant.
- L'absence d'authentification est un choix explicite pour l'Epic 01 (outil de dev local). La surface d'exposition doit néanmoins être restreinte au minimum.
- Les logs doivent être utiles pour le débogage sans jamais contenir de code source soumis par l'utilisateur ni de code généré (potentiellement confidentiel).

---

## hypotheses

- L'outil est déployé en développement local ou sur un réseau interne contrôlé pour l'Epic 01. L'exposition Internet n'est pas dans le périmètre de cet epic.
- Les développeurs utilisateurs font confiance au code source qu'ils soumettent (pas de sandbox d'exécution nécessaire pour l'Epic 01).
- Spring Security est présent en dépendance mais aucune règle d'authentification n'est activée pour l'Epic 01.
- La taille maximale du corps de requête est limitée à 500 Ko (source Java de contrôleurs volumineux).

---

## incertitudes

- Le déploiement multi-utilisateurs (plusieurs développeurs sur un serveur partagé) n'est pas documenté. Si ce scénario arrive avant l'ajout d'une auth, il faut isoler les sessions par utilisateur via un mécanisme de token simple.
- La rétention des sessions en mémoire (Epic 01) n'a pas de durée de vie définie. Une session orpheline contenant du code source confidentiel doit avoir un TTL.
- La communication entre le backend et le moteur d'analyse (futur) pourrait exposer du code source vers un processus externe. Ce flux devra être sécurisé dans l'epic moteur.

---

## decisions

- HTTPS est obligatoire dès le premier déploiement hors machine locale. En local (localhost) HTTP est toléré.
- La taille maximale du corps de requête est plafonnée à 512 Ko via la configuration Spring Boot.
- Aucun contenu de code source ni de code généré n'est écrit dans les logs applicatifs.
- Les sessions ont un TTL de 2 heures en mémoire (configurable via propriété `jas.session.ttl-minutes`).
- Le CORS est configuré strictement : seul l'origine Angular de développement (`http://localhost:4200`) est autorisé en Epic 01. L'origine est externalisée en propriété de configuration.
- Les UUID de session sont générés par `UUID.randomUUID()` (cryptographiquement sûr via JDK 21).
- Spring Security est configuré en mode permissif explicite pour l'Epic 01 avec un commentaire de marquage `// TODO: activer l'authentification en Epic-AUTH`.
- Toute erreur de validation d'entrée retourne HTTP 400 avec un message générique (sans echo du contenu soumis).

---

## livrables

### Flux sensibles identifiés et mesures associées

| Flux | Endpoint | Sensibilité | Mesure obligatoire |
|---|---|---|---|
| Soumission du source Java | `POST /sessions` | HAUTE — code source potentiellement confidentiel | Taille max 512 Ko ; pas de log du contenu ; HTTPS hors localhost |
| Prompt conversationnel | `POST /sessions/{id}/chat` | MOYENNE — texte libre arbitraire | Taille max 2 Ko ; caractères de contrôle rejetés ; pas de log du prompt complet |
| Code généré | `POST /sessions/{id}/generate` | HAUTE — artefacts exécutables | Pas d'exécution serveur ; pas de log du contenu généré ; stockage en mémoire uniquement (Epic 01) |
| Identifiants de session | Tous les endpoints `/{sessionId}` | MOYENNE — prédictibilité = accès croisé | UUID v4 aléatoire ; validation format UUID en entrée ; retour 404 si session inconnue |
| Réponses d'erreur | Tous les endpoints | FAIBLE — fuite d'informations internes | Messages d'erreur génériques ; stack trace non exposée en production |

---

### Principes de journalisation non-sensible

#### Ce qui DOIT être logué (niveau INFO)

```
- Création de session : sessionId + controllerFileName + timestamp (pas le code source)
- Lancement d'une analyse : sessionId + analysisOptions + timestamp
- Fin d'une analyse : sessionId + durationMs + metricsCount
- Sélection des lots : sessionId + selectedLots
- Déclenchement d'une génération : sessionId + targetLot + artefactTypes
- Fin d'une génération : sessionId + durationMs + fileCount (pas le contenu des fichiers)
- Réception d'un prompt chat : sessionId + resolvedIntent + timestamp (pas le texte du prompt)
- Suppression / expiration de session : sessionId + reason
```

#### Ce qui DOIT être logué (niveau WARN)

```
- Taille de corps de requête dépassant 80% du plafond configuré
- Session non trouvée pour un sessionId valide (UUID valide mais inconnu)
- Rejet d'une requête pour format UUID invalide
- Timeout d'analyse ou de génération
```

#### Ce qui NE DOIT JAMAIS être logué

```
- controllerSourceCode (texte brut du source Java soumis)
- fileContent (code Java généré)
- ChatRequest.prompt (texte du prompt utilisateur)
- ChatResponse.responseText (réponse textuelle du backend)
- Tout contenu de GeneratedFileResponse
- Stack traces complètes en réponse HTTP (OK en log interne niveau DEBUG)
```

#### Format de log recommandé (Logback / SLF4J)

```
[JAS][{sessionId}][{useCase}] {event} - {metadata}
Exemple :
[JAS][550e8400-e29b-41d4-a716-446655440000][AnalyzeControllerUseCase] analysis.completed - durationMs=1240 zones=14
```

---

### Exigences de configuration

#### application.properties / application.yml

```yaml
# Taille maximale des requêtes
spring:
  servlet:
    multipart:
      max-request-size: 512KB
      max-file-size: 512KB

# Gestion des sessions en mémoire
jas:
  session:
    ttl-minutes: 120
    max-active-sessions: 50   # protection ressource mémoire

# CORS — origine Angular externalisée
jas:
  cors:
    allowed-origin: http://localhost:4200

# Sécurité — niveau Epic 01
spring:
  security:
    # Désactivé explicitement pour Epic 01 — TODO: activer en Epic-AUTH
    enabled: false
```

#### Classe de configuration CORS (à maintenir dans `configuration/`)

```java
// Dans configuration/WebSecurityConfiguration.java ou CorsConfiguration.java
@Bean
CorsConfigurationSource corsConfigurationSource(
    @Value("${jas.cors.allowed-origin}") String allowedOrigin) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigin));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "Accept"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

#### Validation des entrées (dans `adapters.in.rest`)

Chaque contrôleur REST doit valider :
- `sessionId` : format UUID v4 strict (`[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}`)
- `controllerSourceCode` : non nul, non vide, taille ≤ 512 000 caractères
- `prompt` dans `ChatRequest` : non nul, taille ≤ 2 000 caractères, rejet des caractères de contrôle ASCII (0x00–0x1F sauf 0x0A, 0x0D, 0x09)
- `targetLot` dans `GenerateRequest` : valeur entre 1 et 5 inclus
- `selectedLots` dans `SelectLotsRequest` : liste non nulle, valeurs entre 1 et 5, pas de doublons

Utiliser Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Min`, `@Max`) avec un `@RestControllerAdvice` global retournant HTTP 400 avec un message générique.

#### Gestion des erreurs — structure uniforme

```java
record ApiErrorResponse(
    String errorCode,    // ex: VALIDATION_ERROR, SESSION_NOT_FOUND, INTERNAL_ERROR
    String message,      // message générique, jamais le détail technique
    Instant timestamp
) {}
```

Codes HTTP utilisés :
- `400 Bad Request` : validation d'entrée
- `404 Not Found` : session inconnue
- `413 Payload Too Large` : dépassement de taille
- `500 Internal Server Error` : erreur interne (message générique uniquement)

---

### Checklist de sécurité pour les agents d'implémentation

Avant de merger chaque lot d'implémentation backend :

- [ ] Aucun `controllerSourceCode`, `fileContent` ou `prompt` n'apparaît dans les logs (revue de code obligatoire)
- [ ] Le `@RestControllerAdvice` global est en place et testé (HTTP 400 sur entrée invalide)
- [ ] La validation UUID est en place sur tous les endpoints `/{sessionId}`
- [ ] La configuration CORS est externalisée en propriété (pas de valeur hardcodée)
- [ ] La propriété `jas.session.ttl-minutes` est documentée dans `application.properties`
- [ ] Spring Security est configuré explicitement (même en mode permissif, pas de configuration par défaut implicite)
- [ ] Les tests unitaires des mappers ne logguent pas de contenu source
- [ ] Le handler `POST /sessions/{id}/generate` ne déclenche aucune écriture fichier sur le serveur hors répertoire dédié

---

## dependances

- JAS-01 : parcours utilisateur, phases et actions conversationnelles
- JAS-02 : endpoints, DTO, responsabilités backend/frontend
- `backend/docs/architecture.md` : packages, stack Spring Boot 4.0.3
- `agents/catalog.md` : agent `securite`, agent `observabilite-exploitation` (consommateur du format de log)

---

## verifications

- [ ] Les cinq flux sensibles sont identifiés et ont chacun une mesure associée
- [ ] Les principes "ne jamais logger" couvrent tous les champs de contenu des DTO JAS-02
- [ ] Les exigences de configuration sont directement utilisables dans `application.yml` sans transformation
- [ ] La structure `ApiErrorResponse` est cohérente avec les DTO JAS-02
- [ ] La checklist de sécurité est actionnable par l'agent `backend-hexagonal`

---

## handoff

```
handoff:
  vers: backend-hexagonal (Epic 02 — implémentation)
  preconditions:
    - JAS-01, JAS-02 et JAS-03 validés
    - backend/docs/architecture.md confirmé comme référence de packages
  points_de_vigilance:
    - La configuration CORS doit être refactorisée dès l'Epic 02 si elle est hardcodée actuellement
    - Le RestControllerAdvice global doit être créé avant les premiers endpoints de session (pas en fin de lot)
    - Le TTL de session (jas.session.ttl-minutes) doit être pris en charge dès la création du SessionRepository en mémoire
    - Tout ajout de log dans les use cases doit être relu contre la liste "ne jamais logger"
    - L'agent observabilite-exploitation doit être consulté pour aligner le format [JAS][sessionId][useCase] avec son schéma de corrélation
  artefacts_a_consulter:
    - jira/epic-01/JAS-01-parcours-utilisateur.md
    - jira/epic-01/JAS-02-contrat-api-workbench.md
    - jira/epic-01/JAS-03-securite-exposition.md (ce fichier)
    - backend/docs/architecture.md
    - agents/catalog.md (agents backend-hexagonal, observabilite-exploitation)
```
