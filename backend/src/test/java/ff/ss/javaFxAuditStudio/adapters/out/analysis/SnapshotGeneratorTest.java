package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.AssemblerGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.BridgeGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.GatewayGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.PolicyGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.SlimControllerGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.UseCaseGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.ViewModelGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotGeneratorTest {

    private static final String BASE = "Patient";
    private static final String PKG  = "com.example.patient";

    private static List<BusinessRule> useCaseRules() {
        return List.of(
            rule("RG-001", "Methode handler onSave : responsabilite APPLICATION detectee",
                 ResponsibilityClass.APPLICATION, ExtractionCandidate.USE_CASE),
            rule("RG-002", "Methode handler onDelete : responsabilite APPLICATION detectee",
                 ResponsibilityClass.APPLICATION, ExtractionCandidate.USE_CASE)
        );
    }

    private static List<BusinessRule> viewModelRules() {
        return List.of(
            rule("RG-003", "Champ FXML Label statusLabel : liaison UI directe detectee",
                 ResponsibilityClass.UI, ExtractionCandidate.VIEW_MODEL),
            rule("RG-004", "Champ FXML Button saveBtn : liaison UI directe detectee",
                 ResponsibilityClass.UI, ExtractionCandidate.VIEW_MODEL)
        );
    }

    private static List<BusinessRule> policyRules() {
        return List.of(
            rule("RG-005", "Regle metier isEligible : decision BUSINESS detectee",
                 ResponsibilityClass.BUSINESS, ExtractionCandidate.POLICY),
            rule("RG-006", "Regle metier canDelete : validation BUSINESS detectee",
                 ResponsibilityClass.BUSINESS, ExtractionCandidate.POLICY)
        );
    }

    private static List<BusinessRule> gatewayRules() {
        return List.of(
            rule("RG-007", "Appel HTTP fetchPatientData : acces TECHNICAL detecte",
                 ResponsibilityClass.TECHNICAL, ExtractionCandidate.GATEWAY),
            rule("RG-008", "Impression printReport : acces TECHNICAL detecte",
                 ResponsibilityClass.TECHNICAL, ExtractionCandidate.GATEWAY)
        );
    }

    private static List<BusinessRule> bridgeRules() {
        return List.of(
            rule("RG-009", "Methode handleMisc : responsabilite UNKNOWN indeterminee",
                 ResponsibilityClass.UNKNOWN, ExtractionCandidate.NONE),
            rule("RG-010", "Methode processData : responsabilite UNKNOWN indeterminee",
                 ResponsibilityClass.UNKNOWN, ExtractionCandidate.NONE)
        );
    }

    private static BusinessRule rule(
            final String id,
            final String desc,
            final ResponsibilityClass rc,
            final ExtractionCandidate ec) {
        return new BusinessRule(id, desc, "Patient.java", 0, rc, ec, false);
    }

    // ------------------------------------------------------------------ snapshots

    @Test
    void slimController_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new SlimControllerGenerator().generate(BASE, PKG, useCaseRules());
        assertMatchesSnapshot("SlimController", artifact.content());
    }

    @Test
    void viewModel_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new ViewModelGenerator().generate(BASE, PKG, viewModelRules());
        assertMatchesSnapshot("ViewModel", artifact.content());
    }

    @Test
    void useCase_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new UseCaseGenerator().generate(BASE, PKG, useCaseRules());
        assertMatchesSnapshot("UseCase", artifact.content());
    }

    @Test
    void policy_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new PolicyGenerator().generate(BASE, PKG, policyRules());
        assertMatchesSnapshot("Policy", artifact.content());
    }

    @Test
    void gateway_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new GatewayGenerator().generate(BASE, PKG, gatewayRules());
        assertMatchesSnapshot("Gateway", artifact.content());
    }

    @Test
    void bridge_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new BridgeGenerator().generate(BASE, PKG, bridgeRules());
        assertMatchesSnapshot("Bridge", artifact.content());
    }

    @Test
    void assembler_matchesSnapshot() throws Exception {
        CodeArtifact artifact = new AssemblerGenerator().generate(BASE, PKG, List.of());
        assertMatchesSnapshot("Assembler", artifact.content());
    }

    // ------------------------------------------------------------------ helpers

    private void assertMatchesSnapshot(final String name, final String actual) throws IOException {
        Path snapshotPath = snapshotPath(name);
        String updateSnapshots = System.getProperty("update-snapshots");
        if ("true".equals(updateSnapshots)) {
            Files.createDirectories(snapshotPath.getParent());
            Files.writeString(snapshotPath, actual);
            return;
        }
        if (!Files.exists(snapshotPath)) {
            Files.createDirectories(snapshotPath.getParent());
            Files.writeString(snapshotPath, actual);
            return; // Premiere execution : cree le snapshot
        }
        String expected = Files.readString(snapshotPath);
        assertThat(actual)
                .as("Snapshot %s a change — relancer avec -Dupdate-snapshots=true pour mettre a jour", name)
                .isEqualTo(expected);
    }

    /**
     * Resout le chemin du snapshot dans src/test/resources/snapshots/.
     *
     * <p>Maven execute les tests avec le repertoire de travail positionne a la racine du module
     * (backend/), donc le chemin relatif "src/test/resources/snapshots" est correct.
     */
    private Path snapshotPath(final String name) {
        return Path.of("src/test/resources/snapshots").resolve(name + ".java.snapshot");
    }
}
