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
 * JAS-008 — Tests du SlimControllerGenerator : le handler @FXML conserve les types JavaFX UI
 * mais l'appel au UseCase les exclut.
 */
class SlimControllerGeneratorTest {

    private final SlimControllerGenerator generator = new SlimControllerGenerator();

    @Test
    void generate_handlerFxml_keepsActionEventInSignature() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event"),
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Le handler @FXML doit conserver ActionEvent dans sa propre signature
        assertThat(artifact.content()).contains("public void save(final ActionEvent event, final Patient patient)");
    }

    @Test
    void generate_useCaseCall_doesNotContainActionEventArg() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSave : gestion de sauvegarde",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event"),
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // L'appel useCase.save(...) ne doit pas passer "event" (type JavaFX UI filtre)
        assertThat(artifact.content()).contains("useCase.save(patient)");
        assertThat(artifact.content()).doesNotContain("useCase.save(event");
    }

    @Test
    void generate_handlerFxml_onlyDomainParamsCallsUseCase() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onDelete : suppression d'un element",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("ActionEvent", "event")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Signature @FXML conserve ActionEvent
        assertThat(artifact.content()).contains("public void delete(final ActionEvent event)");
        // Appel useCase sans argument (ActionEvent filtre)
        assertThat(artifact.content()).contains("useCase.delete()");
    }

    @Test
    void generate_ruleWithoutJavaFxParams_useCaseCallPassesAllArgs() {
        BusinessRule rule = ruleWithSignature(
                "Methode handler onSearch : recherche par id",
                MethodSignature.of("void", List.of(
                        MethodParameter.known("Long", "id"),
                        MethodParameter.known("String", "query")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Tous les params sont domaine — l'appel useCase conserve tous les arguments
        assertThat(artifact.content()).contains("useCase.search(id, query)");
    }

    // --- helper ---

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
