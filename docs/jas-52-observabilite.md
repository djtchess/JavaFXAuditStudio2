# JAS-52 - Strategie d'observabilite et de logs de workflow

Story : JAS-52 | Epic : JAS-EPIC-06
Date : 2026-03-16

---

## 1. Points de logs critiques par service

### IngestSourcesService

| Moment | Niveau | Message |
|---|---|---|
| Debut de handle() | DEBUG | `Ingestion demarree - {n} refs` |
| Reference absente (FILE_NOT_FOUND) | WARN | `Reference non trouvee - ref masquee par securite` |
| Fin de handle() | DEBUG | `Ingestion terminee - {n} inputs, {n} erreurs` |

Regles specifiques :
- La valeur brute de la reference fichier n'est jamais incluse dans le message de log.
- Le WARN FILE_NOT_FOUND est toujours emis, meme en profil INFO, car WARN >= INFO.

### CartographyService

| Moment | Niveau | Message |
|---|---|---|
| Debut de handle() | DEBUG | `Cartographie demarree - controllerRef masquee` |
| Fin de handle() | DEBUG | `Cartographie terminee - {n} composants, {n} handlers, {n} inconnues` |

Regles specifiques :
- `controllerRef` et `fxmlRef` ne sont jamais interpolees dans les messages.
- Les compteurs (composants, handlers, inconnues) sont des entiers sans semantique metier sensible.

### RestExceptionHandler (existant, JAS-03)

- Log le nom de classe de l'exception uniquement (pas le message).
- La stack trace n'est jamais incluse dans la reponse HTTP (`server.error.include-stacktrace=never`).

### CorrelationFilter (existant, JAS-03)

- Injecte `correlationId` dans le MDC Logback a chaque requete entrante.
- Le correlationId est retire du MDC en fin de requete (finally).

---

## 2. Conventions : ce qui ne doit JAMAIS apparaitre dans les logs

Les interdictions suivantes s'appliquent a tous les services et adaptateurs, sans exception :

- **Contenu source** : le code Java ou FXML analyse (corps des fichiers lus via `SourceReaderPort`) ne doit jamais etre logue, ni en entier ni en extrait.
- **Chemins absolus** : les chemins systeme complets (ex. `C:\Users\...`) ne doivent pas apparaitre. Logguer uniquement des compteurs ou des identifiants opaques.
- **References metier brutes** : les `controllerRef`, `fxmlRef`, `sourceRef` sont des valeurs fournies par l'utilisateur ; elles peuvent contenir des informations de chemin. Ne jamais les interpoler dans un message de log.
- **Stack traces** : interdites dans les reponses HTTP. Dans les logs serveur, reservees au niveau ERROR avec un identifiant de correlation exploitable.
- **Donnees d'authentification / jetons** : hors perimetre actuel mais interdit par principe.
- **Messages d'exception Spring internes** : le champ `message` des exceptions techniques ne doit pas etre expose (il peut contenir des chemins ou des details d'infrastructure).

---

## 3. Activation du niveau DEBUG

### Via profil Spring (recommande en developpement local)

```bash
# Au lancement Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou variable d'environnement
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Ou argument JVM
java -jar app.jar --spring.profiles.active=dev
```

### Via variable d'environnement sans profil (surcharge ponctuelle)

```bash
LOGGING_LEVEL_FF_SS_JAVAFXAUDITSTUDIO=DEBUG java -jar app.jar
```

### En production

Le profil `default` est actif. Le niveau `ff.ss.javaFxAuditStudio` reste a INFO.
Les messages DEBUG ne sont jamais emis. Aucune configuration supplementaire n'est requise.

---

## 4. Format du correlationId dans les logs

Le pattern Logback defini dans `logback-spring.xml` est :

```
%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-no-corr}] %logger{36} - %msg%n
```

Exemple de ligne produite avec correlationId present :

```
14:32:07.421 [http-nio-8080-exec-3] DEBUG [a1b2c3d4-e5f6-7890-abcd-ef1234567890] f.s.j.a.s.IngestSourcesService - Ingestion demarree - 3 refs
```

Exemple sans correlationId (appel hors contexte HTTP) :

```
14:32:07.422 [main] DEBUG [no-corr] f.s.j.a.s.CartographyService - Cartographie demarree - controllerRef masquee
```

Le champ `[%X{correlationId:-no-corr}]` utilise la valeur MDC injectee par `CorrelationFilter`. La valeur de repli `no-corr` signale qu'aucune requete HTTP n'est a l'origine de l'appel (batch, test, demarrage).

---

## 5. Fichiers crees ou modifies par cette story

| Fichier | Action |
|---|---|
| `backend/src/main/java/.../service/IngestSourcesService.java` | Logger SLF4J + 3 points de log |
| `backend/src/main/java/.../service/CartographyService.java` | Logger SLF4J + 2 points de log |
| `backend/src/main/resources/logback-spring.xml` | Cree : configuration Logback par profil |
| `backend/src/main/resources/application.properties` | Section JAS-52 ajoutee |

---

## 6. Handoffs

- **securite** : valider que les conventions d'interdiction de log (section 2) sont couvertes par une revue de code ou un test statique.
- **devops-ci-cd** : confirmer que le profil `dev` n'est pas active dans les pipelines CI (seul `default` doit etre actif en integration).
- **backend-hexagonal** : appliquer les memes conventions aux futurs adaptateurs (ports out) qui liront ou ecriront des fichiers source.
- **frontend-angular** : les correlationIds sont transmis dans l'en-tete `X-Correlation-Id` ; le frontend peut les conserver pour les inclure dans ses propres rapports d'erreur.
