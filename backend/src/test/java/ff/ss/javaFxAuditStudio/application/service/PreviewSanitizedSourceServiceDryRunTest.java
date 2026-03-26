package ff.ss.javaFxAuditStudio.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationReport;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de PreviewSanitizedSourceService en mode dry-run (AI-2).
 */
@ExtendWith(MockitoExtension.class)
class PreviewSanitizedSourceServiceDryRunTest {

    @Mock
    private AnalysisSessionPort sessionPort;

    @Mock
    private SanitizationPort sanitizationPort;

    @Mock
    private SourceFileReaderPort sourceFileReaderPort;

    private PreviewSanitizedSourceService service;

    @BeforeEach
    void setUp() {
        service = new PreviewSanitizedSourceService(sessionPort, sanitizationPort, sourceFileReaderPort);
    }

    private static AnalysisSession sessionWith(final String sessionId, final String controllerRef) {
        return new AnalysisSession(sessionId, controllerRef, null, AnalysisStatus.COMPLETED, Instant.now());
    }

    @Test
    void previewDryRun_should_delegate_to_port_previewTransformations() {
        AnalysisSession session = sessionWith("sess-dr-1", "C:/tmp/MyController.java");
        SanitizationReport expectedReport = SanitizationReport.refused(
                "bundle-dry", "1.0", List.of());

        when(sessionPort.findById("sess-dr-1")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("void init() { String password = null; }"));
        when(sanitizationPort.previewTransformations(any(), any(), any())).thenReturn(expectedReport);

        SanitizationReport result = service.previewDryRun("sess-dr-1");

        assertThat(result).isNotNull();
        assertThat(result.sensitiveMarkersFound()).isTrue();
        assertThat(result.approved()).isFalse();
        verify(sanitizationPort).previewTransformations(
                any(),
                eq("void init() { String password = null; }"),
                eq("C:/tmp/MyController.java"));
    }

    @Test
    void previewDryRun_should_return_approved_empty_report_when_sanitization_port_absent() {
        PreviewSanitizedSourceService serviceNoPort = new PreviewSanitizedSourceService(
                sessionPort, null, sourceFileReaderPort);
        AnalysisSession session = sessionWith("sess-dr-2", "C:/tmp/MyController.java");

        when(sessionPort.findById("sess-dr-2")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("public class MyController {}"));

        SanitizationReport result = serviceNoPort.previewDryRun("sess-dr-2");

        assertThat(result).isNotNull();
        assertThat(result.approved()).isTrue();
        assertThat(result.sensitiveMarkersFound()).isFalse();
        assertThat(result.transformations()).isEmpty();
    }

    @Test
    void previewDryRun_should_throw_when_session_is_missing() {
        when(sessionPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.previewDryRun("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }
}
