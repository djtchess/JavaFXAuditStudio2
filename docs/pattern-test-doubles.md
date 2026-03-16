# Pattern test doubles — JavaFXAuditStudio2

## Objet

Ce document definit le pattern de test recommande pour les ports du backend hexagonal.
Il repond a la lacune 2 identifiee par `qa-backend` : les stubs de production (`@Component`) ne sont
pas controlables depuis les tests unitaires et ne doivent pas l'etre.

---

## 1. Tests unitaires de services : passer le port comme lambda ou classe anonyme

Les services applicatifs recoivent leurs ports par injection dans le constructeur.
En test unitaire, on n'instancie aucun contexte Spring.
On passe directement une implementation ad hoc du port, sous forme de lambda (si le port est une
interface fonctionnelle) ou de classe anonyme.

Ce pattern est deja applique dans `IngestSourcesServiceTest` et `ClassifyResponsibilitiesServiceTest`.

Regle : ne jamais injecter un stub `@Component` dans un test unitaire de service.
Les stubs `@Component` sont reserves au wiring Spring (tests d'integration ou demarrage de
l'application).

### Exemple tire de `IngestSourcesServiceTest`

```java
@Test
void handle_returnsInputResult_whenRefFound() {
    SourceInput sourceInput;
    SourceReaderPort port;
    IngestSourcesService service;
    IngestionResult result;

    sourceInput = new SourceInput(
            "com/example/MyController.java",
            SourceInputType.JAVA_CONTROLLER,
            "class MyController {}");
    port = ref -> Optional.of(sourceInput);   // lambda — aucun Spring, aucun stub
    service = new IngestSourcesService(port);

    result = service.handle(List.of("com/example/MyController.java"));

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.inputs()).hasSize(1);
}
```

Le lambda `ref -> Optional.of(sourceInput)` est la doublure de test.
Il retourne exactement ce que le test a besoin de controler, sans depend d'aucun fichier systeme,
d'aucun contexte Spring, ni d'aucun stub partage.

---

## 2. Tests d'integration : les stubs `@Component` sont les doublures d'integration

Les stubs fournis dans `adapters/out/analysis/` (`StubRuleExtractionAdapter`,
`StubMigrationPlannerAdapter`, `StubCodeGenerationAdapter`, `StubCartographyAnalysisAdapter`) sont
des `@Component` qui implementent les ports sortants avec des donnees vides ou generiques.

Ces stubs servent a un seul usage : permettre au contexte Spring de demarrer et au wiring d'etre
valide sans implementer le parsing reel.

Ils produisent des donnees vides et previsibles, ce qui est suffisant pour :
- verifier que le contexte Spring se charge sans erreur (`@SpringBootTest`) ;
- verifier que les adapters REST repondent avec un code HTTP correct et un corps coherent ;
- valider le wiring entre les ports et les services sans declencher de logique metier.

Regle : ne pas chercher a injecter un comportement specifique dans un stub `@Component` depuis un
test. Si le test a besoin d'un comportement precis, il doit etre un test unitaire utilisant un
lambda (voir section 1).

---

## 3. Quand remplacer un stub

Un stub `@Component` est temporaire par construction.
Il doit etre retire et remplace quand une implementation reelle du port est disponible.

Procedure de remplacement :

1. Creer la classe d'implementation reelle du port (`@Component`).
2. Retirer l'annotation `@Component` du stub correspondant (ou supprimer le stub).
3. Ne jamais laisser deux classes `@Component` implementer le meme port sans `@Qualifier`.
   Spring refuserait de demarrer avec une `NoUniqueBeanDefinitionException`.

Exemple : quand `RealRuleExtractionAdapter implements RuleExtractionPort` est cree avec
`@Component`, retirer `@Component` de `StubRuleExtractionAdapter`.
Le stub peut etre conserve comme classe de test (dans `src/test/`) s'il reste utile pour
des tests d'integration a faible cout.

---

## 4. Recap des regles

| Contexte                        | Doublure recommandee                        | Interdit                                      |
|---------------------------------|---------------------------------------------|-----------------------------------------------|
| Test unitaire de service        | Lambda ou classe anonyme inline             | Stub `@Component`, Mockito, contexte Spring   |
| Test d'integration Spring       | Stub `@Component` existant dans `src/main/` | Lambda inline non instanciable par Spring     |
| Remplacement d'un stub          | Implementation reelle + retrait `@Component` du stub | Deux `@Component` sur le meme port sans `@Qualifier` |

---

## References

- `backend/src/test/.../application/service/IngestSourcesServiceTest.java`
- `backend/src/test/.../application/service/ClassifyResponsibilitiesServiceTest.java`
- `backend/src/main/.../adapters/out/analysis/StubRuleExtractionAdapter.java`
- `backend/src/main/.../adapters/out/analysis/StubCartographyAnalysisAdapter.java`
- `backend/src/main/.../adapters/out/analysis/StubMigrationPlannerAdapter.java`
- `backend/src/main/.../adapters/out/analysis/StubCodeGenerationAdapter.java`
