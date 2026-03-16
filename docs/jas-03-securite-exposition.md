# JAS-03 - Exigences de securite et d'exposition

Agent: `securite`
Story: JAS-03
Epic: JAS-EPIC-01
Date: 2026-03-16
Statut: VALIDE

---

## objectif

Poser les exigences de securite, de journalisation et de controle d'acces pour
JavaFXAuditStudio2 avant toute implementation des flux d'ingestion de sources,
d'analyse et de restitution.

---

## perimetre

- Backend hexagonal JDK 21 / Spring Boot 4.0.3
- Frontend Angular 21.x (client REST uniquement)
- Flux : upload de fichiers source Java/FXML, lancement d'analyse, restitution
  de diagnostics, telechargement de rapports
- Hors perimetre : authentification utilisateur (non prevue a ce stade),
  chiffrement au repos (defere a une iteration ulterieure)

---

## faits

Observes directement dans les fichiers du depot :

1. `RestApiConfiguration.java` configure CORS uniquement sur `/api/**` avec
   `allowedMethods("GET")` et `allowedOrigins` injecte depuis
   `app.frontend.origin`.
2. `application.properties` contient trois proprietes :
   `spring.application.name`, `app.frontend.origin=http://localhost:4200`,
   `logging.level.ff.ss.javaFxAuditStudio=INFO`.
3. `WorkbenchController` n'expose qu'un endpoint `GET /api/v1/workbench/overview`
   sans traitement de fichier pour l'instant.
4. La methode CORS `allowedMethods("GET")` est trop restrictive : les flux
   d'ingestion de sources necessiteront `POST` et `DELETE` au minimum.
5. Aucun en-tete de securite HTTP n'est configure (pas de Spring Security dans
   pom.xml, pas de filtre de reponse personnalise).
6. Aucune gestion centralisee des erreurs REST (`@ControllerAdvice`) n'est
   presente ; les stack traces Spring Boot peuvent etre exposees par defaut.
7. `spring-boot-starter-security` est absent du pom.xml.
8. Le niveau de log global est INFO ; DEBUG est desactivable/activable par
   configuration mais aucune propriete nominative n'est documentee.

---

## interpretations

- La configuration CORS actuelle bloque tous les futurs appels POST du frontend
  (upload, lancement d'analyse, operations PATCH/DELETE) ; elle doit etre
  elargie aux methodes requises tout en restant strictement limitee a l'origine
  declaree.
- L'absence de Spring Security est acceptable en phase initiale si l'application
  reste sur reseau local isole, mais les en-tetes de securite HTTP doivent etre
  poses des maintenant via un filtre ou une configuration WebMvc pour ne pas
  creer de dette.
- L'exposition des stack traces par defaut de Spring Boot (`server.error.include-
  stacktrace`) est un risque d'information disclosure a corriger en prod.
- La propriete `app.frontend.origin` existante est une bonne pratique ; elle doit
  etre etendue a un profil prod avec validation stricte.

---

## hypotheses

- L'application s'execute en environnement local ou intranet fermé en phase de
  developpement ; aucun acces public n'est prevu dans les premiers lots.
- L'upload de fichiers source sera introduit dans un lot ulterieur (probablement
  JAS-EPIC-01 lot 2 ou 3) ; les exigences s'y appliquent par avance.
- Un profil Spring `prod` sera cree par l'agent `devops-ci-cd` ; les proprietes
  sensibles y seront externalisees via variables d'environnement.
- Les fichiers Java/FXML uploades sont potentiellement proprietaires et
  confidentiels ; ils ne doivent jamais apparaitre dans les logs.

---

## incertitudes

- Le domaine cible de production n'est pas encore connu ; `app.frontend.origin`
  devra etre fourni par l'exploitant lors du deploiement.
- La politique de retention des fichiers uploades (stockage temporaire vs
  persistant) n'est pas arbitree ; elle conditionne les exigences de purge et
  d'isolation.
- L'activation ou non d'une authentification (JWT, session) est hors perimetre
  JAS-03 mais devra etre decidee avant la mise en production reelle.
- Spring Boot 4.0.3 est une version recente ; la compatibilite des proprietes
  `server.error.*` doit etre verifiee lors de l'implementation.

---

## decisions

D1. CORS elargi aux methodes `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
    sur `/api/**`, origine strictement contrôlee par propriete externalisee,
    credentials non autorises par defaut.

D2. En-tetes de securite HTTP obligatoires sur toutes les reponses REST :
    `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control` pour les
    reponses contenant des donnees d'analyse.

D3. Aucune stack trace dans les reponses REST en profil prod ; un
    `@ControllerAdvice` centralise retourne un corps d'erreur normalise sans
    detail technique.

D4. Regles de logs non sensibles : le contenu des fichiers source uploades,
    les chemins absolus des fichiers clients et les tokens eventuels ne
    paraissent JAMAIS dans les logs a quelque niveau que ce soit.

D5. Le niveau DEBUG est activable par propriete de configuration
    (`logging.level.ff.ss.javaFxAuditStudio=DEBUG`) sans redemarrage en profil
    dev via Spring Boot Actuator si Actuator est ajoute, ou par rechargement de
    configuration.

D6. Chaque entree de log doit porter un contexte fonctionnel minimal :
    identifiant de session d'analyse (correlationId), nom de l'operation,
    horodatage. Ce contexte est transmis via MDC (Mapped Diagnostic Context).

D7. La propriete `app.frontend.origin` est la seule source de verite pour
    l'origine CORS autorisee ; toute valeur wildcardee (`*`) est interdite
    en profil prod.

---

## 1. Flux sensibles identifies

### 1.1 Flux de haute sensibilite

| Flux | Nature | Risque si expose |
|------|--------|-----------------|
| Upload de fichiers source Java/FXML | Donnees propriétaires du client | Fuite de propriete intellectuelle |
| Contenu brut des fichiers analyses | Code source complet | Idem |
| Resultats d'analyse detailles | Diagnostics, regles metier extraites | Revelation d'architecture interne |
| Rapports PDF/DOCX generes | Synthese du diagnostic complet | Idem |
| Chemins absolus des fichiers clients | Metadonnees systeme | Reconnaissance d'infrastructure |

### 1.2 Flux de sensibilite moderee

| Flux | Nature | Risque |
|------|--------|--------|
| Metadonnees de session d'analyse (nom de projet, nombre de fichiers) | Donnees descriptives | Enumeration de projets clients |
| Statut d'avancement d'analyse | Donnee operationnelle | Faible, sauf agregation |
| Liste des agents actifs | Configuration interne | Reconnaissance de surface |

### 1.3 Flux non sensibles (logs et monitoring autorises)

- Identifiants de session d'analyse (correlationId opaques, UUIDs)
- Codes de statut HTTP
- Durees d'execution des operations (sans detail metier)
- Nombre de fichiers traites (sans noms ni contenu)
- Erreurs techniques normalisees (code erreur, message generique)

---

## 2. Regles de logs non sensibles

### 2.1 Interdictions absolues (niveau log quelconque, y compris DEBUG)

Les elements suivants ne doivent JAMAIS apparaitre dans les logs :

- Contenu de fichiers source Java ou FXML (meme un extrait)
- Noms de classes, methodes ou champs extraits des fichiers analyses
- Chemins absolus de fichiers fournis par le client
- Contenu de rapports generes
- Tokens d'authentification ou cles d'API (future implementation)
- Parametres de requete contenant des donnees metier (noms de projets clients,
  noms de controllers analyses)

### 2.2 Ce qui est autorise en log INFO (production)

```
[correlationId=<uuid>] [operation=ANALYSE_START] duree=0ms fichiers=12
[correlationId=<uuid>] [operation=ANALYSE_COMPLETE] duree=4521ms statut=SUCCESS
[correlationId=<uuid>] [operation=UPLOAD_RECEIVED] taille=48320 type=application/zip
[correlationId=<uuid>] [operation=RAPPORT_GENERE] format=PDF statut=SUCCESS
```

### 2.3 Ce qui est autorise en log DEBUG (dev uniquement, non sensible)

```
[correlationId=<uuid>] [operation=PARSE_CONTROLLER] nbControllers=3 statut=OK
[correlationId=<uuid>] [operation=FXML_RESOLUTION] nbFichiers=5 statut=OK
[correlationId=<uuid>] [operation=RULE_EXTRACT] nbRegles=14 duree=120ms
```

### 2.4 Correlation par contexte fonctionnel

- Chaque requete REST entrant genere un `correlationId` (UUID v4).
- Le `correlationId` est propage via MDC (`org.slf4j.MDC`) pour toute la duree
  du traitement.
- Il est retourne dans l'en-tete de reponse `X-Correlation-Id` pour faciliter
  le diagnostic en support.
- Il n'est jamais lie a des donnees identifiantes du client.

### 2.5 Activation du niveau DEBUG par configuration

```properties
# Profil dev : activable sans recompilation
logging.level.ff.ss.javaFxAuditStudio=DEBUG

# Profil prod : INFO par defaut
logging.level.ff.ss.javaFxAuditStudio=INFO
```

Avec Spring Boot Actuator (si ajoute), le niveau peut etre modifie a chaud via
`POST /actuator/loggers/ff.ss.javaFxAuditStudio` sans redemarrage.

---

## 3. Exigences CORS

### 3.1 Profil developpement

```properties
app.frontend.origin=http://localhost:4200
app.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
app.cors.max-age=3600
```

### 3.2 Profil production

```properties
# Fourni par variable d'environnement ; pas de valeur par defaut permissive
app.frontend.origin=${FRONTEND_ORIGIN}
app.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
app.cors.max-age=86400
```

### 3.3 Regles CORS non negociables

- Interdiction absolue de `allowedOrigins("*")` en profil prod.
- `allowCredentials(true)` n'est autorise que si une session HTTP est
  explicitement mise en place ; desactive par defaut.
- Le preflight `OPTIONS` doit etre couvert pour eviter les echecs silencieux.
- Les en-tetes autorises sont limites aux en-tetes applicatifs necessaires :
  `Content-Type`, `Accept`, `X-Correlation-Id`, `Authorization` (reserve).
- Les en-tetes exposes (visibles cote client) : `X-Correlation-Id`.

### 3.4 Defauts de la configuration actuelle et corrections requises

Defauts observes dans `RestApiConfiguration.java` :

| Defaut | Impact | Correction |
|--------|--------|------------|
| `allowedMethods("GET")` uniquement | Bloque POST/PUT/DELETE/OPTIONS | Etendre aux methodes necessaires |
| Pas de `allowedHeaders` explicite | Comportement implicite variable selon navigateur | Declarer explicitement |
| Pas de `exposedHeaders` | `X-Correlation-Id` non lisible par Angular | Ajouter `exposedHeaders` |
| Pas de `maxAge` | Preflight rejoue a chaque requete | Fixer a 3600s en dev |

---

## 4. Exigences d'en-tetes de securite HTTP

Les en-tetes suivants doivent etre poses sur toutes les reponses REST :

| En-tete | Valeur recommandee | Objectif |
|---------|--------------------|----------|
| `X-Content-Type-Options` | `nosniff` | Empeche le MIME sniffing |
| `X-Frame-Options` | `DENY` | Empeche le clickjacking |
| `Cache-Control` | `no-store` sur reponses avec donnees d'analyse | Empeche la mise en cache de donnees sensibles |
| `X-Correlation-Id` | UUID de correlation (genere par filtre) | Tracabilite des requetes |
| `Content-Security-Policy` | `default-src 'none'` (API JSON, pas de HTML) | Defense en profondeur |

Ces en-tetes peuvent etre poses via un `OncePerRequestFilter` Spring ou via
Spring Security (`HttpSecurity.headers()`) si Security est ajoute ulterieurement.

En-tetes a NE PAS exposer :

- `X-Powered-By` : doit etre supprime ou masque
- `Server` : valeur generique uniquement, pas de version Spring/Tomcat
- Detail de version dans les corps d'erreur

---

## 5. Gestion des erreurs REST

### 5.1 Exigences

- Un `@ControllerAdvice` centralise (`RestExceptionHandler`) intercepte toutes
  les exceptions non gerees.
- En profil prod, le corps de reponse d'erreur est limite a :
  ```json
  {
    "correlationId": "<uuid>",
    "code": "ERREUR_TECHNIQUE",
    "message": "Une erreur est survenue. Contactez le support avec l'identifiant fourni."
  }
  ```
- La stack trace n'est JAMAIS incluse dans le corps de reponse quel que soit le
  profil.
- La propriete `server.error.include-stacktrace=never` doit etre posee dans
  `application.properties`.
- La propriete `server.error.include-message=never` doit etre posee en profil
  prod pour eviter les messages Spring internes.

### 5.2 Codes HTTP utilises

| Situation | Code HTTP |
|-----------|-----------|
| Requete invalide (validation) | 400 |
| Ressource non trouvee | 404 |
| Erreur fonctionnelle metier | 422 |
| Erreur technique interne | 500 |
| Fichier trop volumineux | 413 |
| Type de fichier non accepte | 415 |

---

## 6. Proprietes de configuration a externaliser

Toutes les proprietes suivantes doivent etre externalisees (variables
d'environnement ou secrets manager) et ne jamais etre committees avec des
valeurs de production :

| Propriete | Variable d'environnement | Valeur dev par defaut |
|-----------|-------------------------|-----------------------|
| `app.frontend.origin` | `FRONTEND_ORIGIN` | `http://localhost:4200` |
| `app.cors.allowed-methods` | `CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` |
| `app.cors.max-age` | `CORS_MAX_AGE` | `3600` |
| `server.error.include-stacktrace` | - | `never` (fixe) |
| `server.error.include-message` | - | `never` en prod, `always` en dev |
| `logging.level.ff.ss.javaFxAuditStudio` | `LOG_LEVEL_APP` | `INFO` |
| `app.upload.max-file-size` | `UPLOAD_MAX_SIZE` | `50MB` (a arbitrer) |
| `app.upload.allowed-extensions` | `UPLOAD_ALLOWED_EXT` | `java,fxml,zip` |

---

## 7. Exigences specifiques aux flux d'upload (lot futur)

A appliquer lors de l'implementation du flux d'ingestion :

- Taille maximale de fichier configurable via `app.upload.max-file-size`.
- Extensions autorisees declarees en liste blanche (`java`, `fxml`, `zip`
  uniquement) ; tout autre type est refuse avec HTTP 415.
- Le nom de fichier fourni par le client est sanitise avant tout usage (pas
  de path traversal).
- Le contenu des fichiers uploades est stocke en memoire ou disque temporaire
  isole ; il n'est jamais logué.
- Les fichiers temporaires sont purges en fin de session d'analyse ou apres
  un delai configurable.
- Le contenu du fichier n'est jamais retourne dans une reponse REST (seul le
  resultat d'analyse l'est).

---

## livrables

- [x] `docs/jas-03-securite-exposition.md` (ce document)
- [x] `RestApiConfiguration.java` corrige (voir section corrections ci-dessous
      et le fichier Java mis a jour)

---

## dependances

- `api-contrats` : les contrats d'API doivent integrer les codes d'erreur
  normalises et l'en-tete `X-Correlation-Id`.
- `backend-hexagonal` : implementation du `RestExceptionHandler`,
  du filtre MDC et du filtre d'en-tetes de securite.
- `observabilite-exploitation` : configuration des appenders de log avec
  masquage des champs sensibles, format structuré (JSON) en prod.
- `devops-ci-cd` : creation des profils Spring `dev` et `prod`, gestion
  des variables d'environnement, secrets manager.
- `frontend-angular` : prise en compte de `X-Correlation-Id` dans les
  interceptors HTTP Angular pour propagation et affichage en cas d'erreur.
- `gouvernance` : arbitrage sur l'activation ou non de l'authentification
  avant la mise en production.

---

## verifications

- [ ] `app.frontend.origin` n'a jamais la valeur `*` dans les fichiers commites
- [ ] `server.error.include-stacktrace=never` present dans application.properties
- [ ] Aucun test ne logue du contenu de fichier source
- [ ] La configuration CORS couvre bien OPTIONS pour le preflight
- [ ] `X-Correlation-Id` visible dans les reponses du backend (test curl)
- [ ] Le niveau DEBUG activable produit des logs sans donnee source

---

## handoff

```text
handoff:
  vers: api-contrats
  preconditions:
    - Ce document JAS-03 est considere comme valide par l'equipe
    - Les codes d'erreur normalises de la section 5.2 sont agrees
  points_de_vigilance:
    - Integrer X-Correlation-Id dans tous les contrats de reponse (succes et erreur)
    - Prevoir un schema d'erreur standard reference dans chaque endpoint OpenAPI
    - Les methodes CORS etendues (POST, PUT, PATCH, DELETE) doivent etre
      coherentes avec les verbes HTTP des futurs endpoints d'ingestion
  artefacts_a_consulter:
    - docs/jas-03-securite-exposition.md (ce fichier)
    - backend/src/main/java/ff/ss/javaFxAuditStudio/configuration/RestApiConfiguration.java

handoff:
  vers: backend-hexagonal
  preconditions:
    - Contrats API stabilises par api-contrats
    - JAS-03 valide
  points_de_vigilance:
    - Implementer RestExceptionHandler avant tout endpoint expose
    - Poser le filtre MDC (correlationId) sur toutes les requetes /api/**
    - Poser le filtre d'en-tetes de securite HTTP (section 4)
    - Ne jamais logguer de contenu de fichier source, meme en DEBUG
  artefacts_a_consulter:
    - docs/jas-03-securite-exposition.md
    - backend/src/main/resources/application.properties

handoff:
  vers: observabilite-exploitation
  preconditions:
    - Backend-hexagonal a pose le MDC et les filtres
  points_de_vigilance:
    - Configurer le masquage des champs sensibles dans l'appender de production
    - Format JSON structure recommande pour l'agregation de logs (ELK/Loki)
    - Le correlationId doit etre indexable dans le systeme de logs
  artefacts_a_consulter:
    - docs/jas-03-securite-exposition.md (section 2 et 6)
```
