package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.MigrationPlanPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionFormatterPort;
import ff.ss.javaFxAuditStudio.application.ports.out.RestitutionPersistencePort;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.migration.MigrationPlan;
import ff.ss.javaFxAuditStudio.domain.restitution.ConfidenceLevel;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;
import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionSummary;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduceRestitutionServiceTest {

    @Mock
    private RestitutionPersistencePort restitutionPersistencePort;

    @Mock
    private ClassificationPersistencePort classificationPersistencePort;

    @Mock
    private ArtifactPersistencePort artifactPersistencePort;

    @Mock
    private CartographyPersistencePort cartographyPersistencePort;

    @Mock
    private MigrationPlanPersistencePort migrationPlanPersistencePort;

    @Mock
    private RestitutionFormatterPort restitutionFormatterPort;

    @InjectMocks
    private ProduceRestitutionService service;

    @Test
    void handle_shouldGenerateMarkdownAndReturnEnrichedReport_whenNoCacheIsPresent() {
        ClassificationResult classification = new ClassificationResult(
                "SampleController",
                List.of(),
                List.of());
        GenerationResult generationResult = new GenerationResult(
                "SampleController",
                List.of(new CodeArtifact(
                        "artifact-1",
                        ArtifactType.VIEW_MODEL,
                        1,
                        "SampleViewModel",
                        "class SampleViewModel {}",
                        false)),
                List.of());
        RestitutionSummary summary = new RestitutionSummary(
                "SampleController",
                0,
                0,
                1,
                0,
                ConfidenceLevel.LOW,
                false);
        RestitutionReport savedReport = new RestitutionReport(
                summary,
                List.of(),
                List.of(),
                List.of(),
                "# Restitution\n\n## Synthese");

        when(restitutionPersistencePort.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(cartographyPersistencePort.findBySessionId("session-1")).thenReturn(Optional.of(new ControllerCartography(
                "SampleController",
                "Sample.fxml",
                List.of(),
                List.of(),
                List.of())));
        when(classificationPersistencePort.findBySessionId("session-1")).thenReturn(Optional.of(classification));
        when(migrationPlanPersistencePort.findBySessionId("session-1")).thenReturn(Optional.of(new MigrationPlan(
                "SampleController",
                List.of(),
                true)));
        when(artifactPersistencePort.findBySessionId("session-1")).thenReturn(Optional.of(generationResult));
        when(restitutionPersistencePort.save(anyString(), any(RestitutionReport.class))).thenReturn(savedReport);
        when(restitutionFormatterPort.formatAsMarkdown(any(RestitutionReport.class))).thenReturn("# Restitution\n\n## Synthese");

        RestitutionReport result = service.handle("session-1", "SampleController");

        ArgumentCaptor<RestitutionReport> reportCaptor = ArgumentCaptor.forClass(RestitutionReport.class);
        verify(restitutionPersistencePort).save(anyString(), reportCaptor.capture());
        verify(restitutionFormatterPort).formatAsMarkdown(any(RestitutionReport.class));

        assertThat(reportCaptor.getValue().markdown()).contains("# Restitution");
        assertThat(result.markdown()).contains("# Restitution");
        assertThat(result.summary().controllerRef()).isEqualTo("SampleController");
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void handle_shouldPersistMarkdownWhenCachedReportIsMissingIt() {
        RestitutionSummary summary = new RestitutionSummary(
                "CachedController",
                2,
                1,
                1,
                0,
                ConfidenceLevel.MEDIUM,
                false);
        RestitutionReport cachedReport = new RestitutionReport(
                summary,
                List.of("Contradiction"),
                List.of("Unknown"),
                List.of("Finding"),
                "");

        when(restitutionPersistencePort.findBySessionId("session-2")).thenReturn(Optional.of(cachedReport));
        when(restitutionPersistencePort.save(anyString(), any(RestitutionReport.class))).thenAnswer(invocation -> invocation.getArgument(1));
        when(restitutionFormatterPort.formatAsMarkdown(cachedReport)).thenReturn("# Restitution\n\n## Synthese");

        RestitutionReport result = service.handle("session-2", "CachedController");

        verify(restitutionPersistencePort).save(anyString(), any(RestitutionReport.class));
        verify(restitutionFormatterPort).formatAsMarkdown(cachedReport);

        assertThat(result.markdown()).contains("## Synthese");
        assertThat(result.contradictions()).containsExactly("Contradiction");
        assertThat(result.unknowns()).containsExactly("Unknown");
    }

    @Test
    void handle_shouldReuseCachedReportWhenMarkdownAlreadyExists() {
        RestitutionSummary summary = new RestitutionSummary(
                "CachedController",
                2,
                1,
                1,
                0,
                ConfidenceLevel.MEDIUM,
                false);
        RestitutionReport cachedReport = new RestitutionReport(
                summary,
                List.of(),
                List.of(),
                List.of("Finding"),
                "# Restitution\n\n## Synthese");

        when(restitutionPersistencePort.findBySessionId("session-3")).thenReturn(Optional.of(cachedReport));

        RestitutionReport result = service.handle("session-3", "CachedController");

        verify(restitutionPersistencePort, never()).save(anyString(), any(RestitutionReport.class));
        verify(restitutionFormatterPort, never()).formatAsMarkdown(any(RestitutionReport.class));
        assertThat(result).isEqualTo(cachedReport);
    }

}
