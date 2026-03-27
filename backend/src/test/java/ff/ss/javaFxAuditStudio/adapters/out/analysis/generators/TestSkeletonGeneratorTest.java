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
 * JAS-027 â€” Tests unitaires du generateur de squelettes JUnit 5.
 */
class TestSkeletonGeneratorTest {

    private final TestSkeletonGenerator generator = new TestSkeletonGenerator();

    @Test
    void generate_useCaseRuleWithPatientParam_containsAnonymousUseCaseDouble() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde du patient",
                MethodSignature.of("void", List.of(MethodParameter.known("Patient", "patient")))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("useCase = new PatientUseCase() {");
        assertThat(artifact.content()).contains("public void save(final Patient patient)");
        assertThat(artifact.content()).contains("final Patient patient = null; // TODO: fournir une valeur metier");
        assertThat(artifact.content()).contains("assertThatCode(() -> useCase.save(patient)).doesNotThrowAnyException();");
    }

    @Test
    void generate_policyRuleWithBooleanReturn_containsIsFalseAssertion() {
        BusinessRule rule = policyRule(
                "Methode garde isFormValid : validation du formulaire",
                MethodSignature.of("boolean", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("policy = new PatientPolicy();");
        assertThat(artifact.content()).contains("assertThat(result).isFalse();");
    }

    @Test
    void generate_useCaseRuleWithoutSignature_containsAssertThatCode() {
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

        assertThat(artifact.content()).contains("assertThatCode(() -> useCase.load()).doesNotThrowAnyException();");
    }

    @Test
    void generate_useCaseRuleWithDtoReturnType_containsNotNullAssertion() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSearch : recherche patient",
                MethodSignature.of("PatientDto", List.of(MethodParameter.known("Long", "id")))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("PatientDto result = useCase.search(id);");
        assertThat(artifact.content()).contains("assertThat(result).isNotNull();");
    }

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

        assertThat(artifact.content()).doesNotContain("@Test");
        assertThat(artifact.content()).contains("class PatientTest");
    }

    @Test
    void generate_twoRulesWithSameMethodName_onlyOneTestMethod() {
        BusinessRule rule1 = useCaseRule(
                "Methode handler onSave : premiere sauvegarde",
                MethodSignature.of("void", List.of())
        );
        BusinessRule rule2 = useCaseRule(
                "Methode handler onSave : deuxieme sauvegarde",
                MethodSignature.of("void", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule1, rule2));

        assertThat(countOccurrences(artifact.content(), "@Test")).isEqualTo(1);
    }

    @Test
    void generate_ruleWithNonStandardType_containsImportHint() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde du patient",
                MethodSignature.of("void", List.of(MethodParameter.known("Patient", "patient")))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("// import Patient;");
    }

    @Test
    void generate_useCaseRuleWithActionEventParam_actionEventFilteredOut() {
        BusinessRule rule = useCaseRule(
                "Methode handler onSave : sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event"),
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("ActionEvent");
        assertThat(artifact.content()).contains("final Patient patient = null; // TODO: fournir une valeur metier");
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
        assertThat(artifact.type()).isEqualTo(ArtifactType.TEST_SKELETON);
        assertThat(artifact.lotNumber()).isEqualTo(2);
    }

    private BusinessRule useCaseRule(final String description, final MethodSignature sig) {
        return new BusinessRule(
                "RG-001",
                description,
                "PatientController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false,
                sig);
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
                sig);
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
