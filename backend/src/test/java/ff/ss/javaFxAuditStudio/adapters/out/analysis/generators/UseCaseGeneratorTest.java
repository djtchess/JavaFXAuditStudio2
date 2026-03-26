package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAS-008 — Tests du filtrage des types JavaFX UI dans l'interface UseCase generee.
 */
class UseCaseGeneratorTest {

    private final UseCaseGenerator generator = new UseCaseGenerator();

    // --- JAS-008 : filtrage des types JavaFX UI ---

    @Test
    void generate_ruleWithOnlyActionEvent_useCaseMethodHasNoParams() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("ActionEvent");
        // la methode doit etre presente mais sans parametre
        assertThat(artifact.content()).contains("void save()");
    }

    @Test
    void generate_ruleWithPatientParam_useCaseMethodContainsPatient() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("Patient patient");
        assertThat(artifact.content()).doesNotContain("ActionEvent");
    }

    @Test
    void generate_ruleWithActionEventAndPatient_useCaseHasOnlyPatient() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event"),
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("ActionEvent");
        assertThat(artifact.content()).contains("Patient patient");
    }

    @Test
    void generate_typeHintGeneratedForPatient() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Le hint d'import doit etre genere pour Patient (type non standard)
        assertThat(artifact.content()).contains("// import Patient;");
    }

    @Test
    void generate_noHintForStandardJavaTypes() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSearch : recherche par identifiant",
                MethodSignature.of("String", List.of(
                        MethodParameter.known("Long", "id"),
                        MethodParameter.known("String", "name")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Long et String sont des types connus — pas de hint d'import
        assertThat(artifact.content()).doesNotContain("// import Long;");
        assertThat(artifact.content()).doesNotContain("// import String;");
        // void ne doit jamais apparaitre en hint
        assertThat(artifact.content()).doesNotContain("// import void;");
    }

    @Test
    void generate_noHintForJavaFxUiTypes() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // ActionEvent est filtre — pas de hint d'import non plus
        assertThat(artifact.content()).doesNotContain("// import ActionEvent;");
    }

    // --- helpers ---

    private BusinessRule ruleWithSignature(final String description, final MethodSignature sig) {
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
}
