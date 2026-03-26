package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAS-027 — Tests unitaires du generateur de squelettes JUnit 5.
 */
class TestSkeletonGeneratorTest {

    private final TestSkeletonGenerator generator = new TestSkeletonGenerator();

    // --- Test 1 : Regle USE_CASE avec signature (Patient patient) ---

    @Test
    void generate_useCaseRuleWithPatientParam_containsFinalPatientDeclaration() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde du patient",
                MethodSignature.of("void", List.of(MethodParameter.known("Patient", "patient")))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("final Patient patient = null;");
    }

    // --- Test 2 : Regle POLICY avec retour boolean -> assertThat(result).isFalse() ---

    @Test
    void generate_policyRuleWithBooleanReturn_containsIsFalseAssertion() {
        BusinessRule rule = policyRule(
                "Methode garde isFormValid : validation du formulaire",
                MethodSignature.of("boolean", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("assertThat(result).isFalse();");
    }

    // --- Test 3 : Regle USE_CASE sans signature -> "(aucun parametre)" ---

    @Test
    void generate_useCaseRuleWithoutSignature_containsNoParamsComment() {
        BusinessRule rule = new BusinessRule(
                "RG-003",
                "Methode handler onLoad : chargement",
                "PatientController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("// (aucun parametre)");
    }

    // --- Test 4 : Regle USE_CASE avec retour non-void (PatientDto) -> isNotNull ---

    @Test
    void generate_useCaseRuleWithDtoReturnType_containsIsNotNullAssertion() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSearch : recherche patient",
                MethodSignature.of("PatientDto", List.of(MethodParameter.known("Long", "id")))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("assertThat(result).isNotNull();");
        assertThat(artifact.content()).contains("PatientDto result = useCase.");
    }

    // --- Test 5 : Regle GATEWAY presente -> "@Mock\n    private XxxGateway gateway" ---

    @Test
    void generate_gatewayRulePresent_containsMockGatewayField() {
        BusinessRule gatewayRule = new BusinessRule(
                "RG-005",
                "Service injecte PatientRepository repo",
                "PatientController.java",
                0,
                ResponsibilityClass.TECHNICAL,
                ExtractionCandidate.GATEWAY,
                false
        );
        BusinessRule useCaseRule = useCaseRule(
                "Methode handler onSave : sauvegarde",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example",
                List.of(gatewayRule, useCaseRule));

        assertThat(artifact.content()).contains("@Mock\n    private PatientGateway gateway;");
    }

    // --- Test 6 : Regle GATEWAY absente -> pas de "@Mock" ---

    @Test
    void generate_noGatewayRule_doesNotContainMockAnnotation() {
        BusinessRule rule = useCaseRule(
                "Methode handler onLoad : chargement",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("@Mock");
    }

    // --- Test 7 : Deux regles avec le meme nom de methode -> deduplication (un seul @Test) ---

    @Test
    void generate_twoRulesWithSameMethodName_onlyOneTestMethod() {
        // Les deux regles produisent le meme nom semantique apres cleanMethodName
        BusinessRule rule1 = useCaseRule(
                "Methode handler onSave : premiere sauvegarde",
                MethodSignature.of("void", List.of())
        );
        BusinessRule rule2 = useCaseRule(
                "Methode handler onSave : deuxieme sauvegarde",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example",
                List.of(rule1, rule2));

        // On compte le nombre d'occurrences de "@Test"
        long testCount = countOccurrences(artifact.content(), "@Test");
        assertThat(testCount).isEqualTo(1);
    }

    // --- Test 8 : Hints d'imports pour types non-standards ---

    @Test
    void generate_ruleWithNonStandardType_containsImportHint() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde du patient",
                MethodSignature.of("void", List.of(MethodParameter.known("Patient", "patient")))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("// import Patient;");
    }

    // --- Test 9 : Regle VIEW_MODEL -> NE genere PAS de methode de test ---

    @Test
    void generate_viewModelRuleOnly_noTestMethodGenerated() {
        BusinessRule rule = new BusinessRule(
                "RG-009",
                "Champ FXML Label nameLabel : affichage du nom",
                "PatientController.java",
                0,
                ResponsibilityClass.PRESENTATION,
                ExtractionCandidate.VIEW_MODEL,
                false
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Pas de methode @Test puisque VIEW_MODEL est ignoree
        assertThat(artifact.content()).doesNotContain("@Test");
        // Mais la classe doit quand meme etre generee (squelette vide)
        assertThat(artifact.content()).contains("class PatientTest");
    }

    // --- Test 10 : Code genere commence par "@ExtendWith(MockitoExtension.class)" ---

    @Test
    void generate_generatedCode_startsWithExtendWithAnnotation() {
        BusinessRule rule = useCaseRule(
                "Methode handler onLoad : chargement",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", null, List.of(rule));

        assertThat(artifact.content()).contains("@ExtendWith(MockitoExtension.class)");
        // La declaration de classe doit suivre l'annotation
        int extendWithIdx = artifact.content().indexOf("@ExtendWith(MockitoExtension.class)");
        int classIdx = artifact.content().indexOf("class PatientTest");
        assertThat(extendWithIdx).isLessThan(classIdx);
    }

    // --- Tests complementaires ---

    @Test
    void generate_artifactType_isTestSkeleton() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.type()).isEqualTo(ArtifactType.TEST_SKELETON);
    }

    @Test
    void generate_lotNumber_isTwo() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.lotNumber()).isEqualTo(2);
    }

    @Test
    void generate_useCaseRuleWithActionEventParam_actionEventFilteredOut() {
        // JAS-008 : les types JavaFX UI doivent etre filtres des squelettes de test
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event"),
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("ActionEvent");
        assertThat(artifact.content()).contains("final Patient patient = null;");
    }

    @Test
    void generate_booleanBoxedReturn_containsIsFalseAssertion() {
        BusinessRule rule = useCaseRule(
                "Methode garde canDelete : autorisation de suppression",
                MethodSignature.of("Boolean", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("assertThat(result).isFalse();");
    }

    @Test
    void generate_className_isBaseNamePlusTest() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.className()).isEqualTo("PatientTest");
    }

    // --- helpers ---

    private BusinessRule useCaseRule(final String description, final MethodSignature sig) {
        return new BusinessRule(
                "RG-001",
                description,
                "PatientController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false,
                sig
        );
    }

    private BusinessRule policyRule(final String description, final MethodSignature sig) {
        return new BusinessRule(
                "RG-002",
                description,
                "PatientController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false,
                sig
        );
    }

    private long countOccurrences(final String text, final String pattern) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
