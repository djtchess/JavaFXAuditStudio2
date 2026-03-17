package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
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
}
