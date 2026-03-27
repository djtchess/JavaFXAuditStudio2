package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

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
 * JAS-020 — Tests du PolicyGenerator pour la generation correcte des methodes garde.
 * Verifie que les signatures reelles sont utilisees quand disponibles,
 * et que le fallback Object context est conserve pour les regles sans signature.
 */
class PolicyGeneratorGuardTest {

    private final PolicyGenerator generator = new PolicyGenerator();

    @Test
    void generate_gardeAvecSignatureSansParametre_doitUtiliserVraieSignature() {
        BusinessRule rule = ruleGardeAvecSignature(
                "Methode garde isFormValid : decision metier BUSINESS detectee [complexite=3]",
                MethodSignature.of("boolean", List.of())
        );

        CodeArtifact artifact = generator.generate("Form", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("public boolean isFormValid()");
        assertThat(artifact.content()).contains("return false;");
    }

    @Test
    void generate_gardeAvecParametrePatient_doitInclureParametre() {
        BusinessRule rule = ruleGardeAvecSignature(
                "Methode garde canSave : decision metier BUSINESS detectee [complexite=2]",
                MethodSignature.of("boolean", List.of(
                        MethodParameter.known("Patient", "patient")
                ))
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("public boolean canSave(final Patient patient)");
    }

    @Test
    void generate_gardeSansSignature_doitUtiliserFallbackObjectContext() {
        // Regle extraite par regex — pas de signature AST disponible
        BusinessRule rule = ruleGardeSansSignature(
                "Methode garde isFormValid : decision metier BUSINESS detectee"
        );

        CodeArtifact artifact = generator.generate("Form", "com.example", List.of(rule));

        // Fallback : boolean + Object context
        assertThat(artifact.content()).contains("public boolean isFormValid(final Object context)");
    }

    @Test
    void generate_ruleBusinessSansSignature_retrocCompatibiliteFallback() {
        // Regle BUSINESS classique sans signature (ancien comportement)
        BusinessRule rule = new BusinessRule(
                "RG-001",
                "Methode handler onSave : responsabilite BUSINESS detectee",
                "SomeController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false
        );

        CodeArtifact artifact = generator.generate("Some", "com.example", List.of(rule));

        // L'ancien comportement doit etre preserve : boolean + Object context
        assertThat(artifact.content()).contains("public boolean onSave(final Object context)");
    }

    @Test
    void generate_semanticBusinessRule_exposesSemanticMethodName() {
        BusinessRule rule = new BusinessRule(
                "RG-002",
                "Regle metier isEligible : decision BUSINESS detectee",
                "PatientController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false,
                MethodSignature.of("boolean", List.of())
        );

        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("public boolean isEligible()");
    }

    // --- helpers ---

    private BusinessRule ruleGardeAvecSignature(final String description, final MethodSignature sig) {
        return new BusinessRule(
                "RG-001",
                description,
                "GuardController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false,
                sig);
    }

    private BusinessRule ruleGardeSansSignature(final String description) {
        return new BusinessRule(
                "RG-001",
                description,
                "GuardController.java",
                0,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false);
    }
}
