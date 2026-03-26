package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RealCodeGenerationAdapterTest {

    private RealCodeGenerationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RealCodeGenerationAdapter();
    }

    @Test
    void shouldGenerateAtLeastSlimControllerAndViewModel() {
        List<CodeArtifact> artifacts = adapter.generate("/path/to/SampleController.java", "");

        // Sans regles classifiees : SlimController (lot 1) + ViewModel (lot 2)
        assertThat(artifacts).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldHaveLotNumbersBetween1And5() {
        List<CodeArtifact> artifacts = adapter.generate("/path/to/SampleController.java", "");

        assertThat(artifacts).allSatisfy(artifact ->
                assertThat(artifact.lotNumber()).isBetween(1, 5)
        );
    }

    @Test
    void shouldMarkBridgeAsTransitional() {
        List<CodeArtifact> artifacts = adapter.generate("/path/to/SampleController.java", "");

        List<CodeArtifact> lot1Artifacts = artifacts.stream()
                .filter(a -> a.lotNumber() == 1)
                .toList();
        assertThat(lot1Artifacts).isNotEmpty();
        assertThat(lot1Artifacts).allSatisfy(artifact ->
                assertThat(artifact.transitionalBridge()).isTrue()
        );
    }

    @Test
    void shouldHandleNullControllerRef() {
        assertThatNoException().isThrownBy(() -> {
            List<CodeArtifact> artifacts = adapter.generate(null, "");
            assertThat(artifacts).isNotEmpty();
        });
    }

    @Test
    void shouldGenerateNonEmptyContent() {
        List<CodeArtifact> artifacts = adapter.generate("/path/to/SampleController.java", "");

        assertThat(artifacts).allSatisfy(artifact ->
                assertThat(artifact.content()).isNotBlank()
        );
    }

    @Test
    void shouldProvideGenerationStatusOnEachArtifact() {
        List<CodeArtifact> artifacts = adapter.generate("/path/to/SampleController.java", "");

        assertThat(artifacts).allSatisfy(artifact -> {
            assertThat(artifact.generationStatus()).isNotBlank();
            assertThat(artifact.generationWarnings()).isNotNull();
        });
    }

    @Test
    void shouldPopulateGenerationStatusOkOrWarning() {
        List<CodeArtifact> artifacts = adapter.generate("/path/to/SampleController.java", "");

        assertThat(artifacts).allSatisfy(artifact ->
                assertThat(artifact.generationStatus()).isIn("OK", "WARNING")
        );
    }

    @Test
    void shouldContainRealImportAfterHintPromotion() {
        // JAS-009 — non-regression : un UseCase genere avec un parametre de type domaine (Patient)
        // doit avoir "import Patient;" (reel) et non "// import Patient;" (commentaire hint)
        MethodSignature sig = MethodSignature.of(
                "void",
                List.of(MethodParameter.known("Patient", "patient"))
        );
        BusinessRule rule = new BusinessRule(
                "RG-001",
                "void savePatient(Patient patient)",
                "PatientController.java",
                42,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false,
                sig
        );

        List<CodeArtifact> artifacts = adapter.generate(
                "/path/to/PatientController.java",
                "package com.example;",
                List.of(rule)
        );

        CodeArtifact useCase = artifacts.stream()
                .filter(a -> a.type() == ArtifactType.USE_CASE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun artefact USE_CASE genere"));

        assertThat(useCase.content())
                .as("Le hint // import Patient; doit etre promu en import reel")
                .contains("import Patient;");
        assertThat(useCase.content())
                .as("Le commentaire hint ne doit plus etre present dans le contenu")
                .doesNotContain("// import Patient;");
    }

    // --- JAS-027 : artefact TEST_SKELETON ---

    @Test
    void shouldContainTestSkeletonArtifactWhenUseCaseRulesPresent() {
        // JAS-027 — un USE_CASE declenche la generation d'un TEST_SKELETON
        MethodSignature sig = MethodSignature.of(
                "void",
                List.of(MethodParameter.known("String", "name"))
        );
        BusinessRule rule = new BusinessRule(
                "RG-027a",
                "Methode handler onSave : sauvegarde du formulaire",
                "PatientController.java",
                10,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false,
                sig
        );

        List<CodeArtifact> artifacts = adapter.generate(
                "/path/to/PatientController.java",
                "package com.example;",
                List.of(rule)
        );

        assertThat(artifacts)
                .as("Un artefact TEST_SKELETON doit etre genere quand des regles USE_CASE sont presentes")
                .anyMatch(a -> a.type() == ArtifactType.TEST_SKELETON);
    }

    @Test
    void shouldNotContainTestSkeletonWhenOnlyViewModelRules() {
        // JAS-027 — VIEW_MODEL seul ne doit pas produire de TEST_SKELETON
        BusinessRule rule = new BusinessRule(
                "RG-027b",
                "Champ FXML Label nameLabel : affichage du nom",
                "PatientController.java",
                5,
                ResponsibilityClass.PRESENTATION,
                ExtractionCandidate.VIEW_MODEL,
                false
        );

        List<CodeArtifact> artifacts = adapter.generate(
                "/path/to/PatientController.java",
                "package com.example;",
                List.of(rule)
        );

        assertThat(artifacts)
                .as("TEST_SKELETON ne doit pas etre genere quand les regles sont uniquement VIEW_MODEL")
                .noneMatch(a -> a.type() == ArtifactType.TEST_SKELETON);
    }

    @Test
    void shouldContainTestSkeletonArtifactWhenPolicyRulesPresent() {
        // JAS-027 — une POLICY seule doit aussi declencher la generation du TEST_SKELETON
        MethodSignature sig = MethodSignature.of("boolean", List.of());
        BusinessRule rule = new BusinessRule(
                "RG-027c",
                "Methode garde isFormValid : validation du formulaire",
                "PatientController.java",
                20,
                ResponsibilityClass.BUSINESS,
                ExtractionCandidate.POLICY,
                false,
                sig
        );

        List<CodeArtifact> artifacts = adapter.generate(
                "/path/to/PatientController.java",
                "package com.example;",
                List.of(rule)
        );

        assertThat(artifacts)
                .as("Un artefact TEST_SKELETON doit etre genere quand des regles POLICY sont presentes")
                .anyMatch(a -> a.type() == ArtifactType.TEST_SKELETON);
    }
}
