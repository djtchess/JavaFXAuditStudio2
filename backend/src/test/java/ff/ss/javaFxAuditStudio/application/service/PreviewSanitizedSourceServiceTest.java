package ff.ss.javaFxAuditStudio.application.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.ai.SanitizedSourcePreviewResult;
import ff.ss.javaFxAuditStudio.domain.ai.TokenEstimator;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewSanitizedSourceServiceTest {

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
    void preview_should_read_raw_source_via_source_file_reader_port_when_available() {
        AnalysisSession session = sessionWith("sess-1", "C:/tmp/MyController.java");
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-1",
                "C:/tmp/MyController.java",
                "sanitized source",
                10,
                "1.0");

        when(sessionPort.findById("sess-1")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("public class MyController {}"));
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle);

        SanitizedSourcePreviewResult result = service.preview("sess-1");

        assertThat(result.sanitized()).isTrue();
        assertThat(result.bundle().sanitizedSource()).isEqualTo("sanitized source");
        verify(sourceFileReaderPort).read("C:/tmp/MyController.java");
        verify(sanitizationPort).sanitize(
                any(),
                eq("public class MyController {}"),
                eq("C:/tmp/MyController.java"));
    }

    @Test
    void preview_should_fallback_to_controller_ref_when_source_file_reader_returns_empty() {
        AnalysisSession session = sessionWith("sess-2", "C:/tmp/MissingController.java");
        SanitizedBundle bundle = new SanitizedBundle(
                "bundle-2",
                "C:/tmp/MissingController.java",
                "sanitized source",
                10,
                "1.0");

        when(sessionPort.findById("sess-2")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MissingController.java"))
                .thenReturn(Optional.empty());
        when(sanitizationPort.sanitize(any(), any(), any())).thenReturn(bundle);

        SanitizedSourcePreviewResult result = service.preview("sess-2");

        assertThat(result.sanitized()).isTrue();
        verify(sanitizationPort).sanitize(
                any(),
                eq("C:/tmp/MissingController.java"),
                eq("C:/tmp/MissingController.java"));
    }

    @Test
    void preview_should_return_raw_bundle_when_sanitization_is_disabled() {
        PreviewSanitizedSourceService serviceWithoutSanitization = new PreviewSanitizedSourceService(
                sessionPort,
                null,
                sourceFileReaderPort);
        AnalysisSession session = sessionWith("sess-3", "C:/tmp/MyController.java");

        when(sessionPort.findById("sess-3")).thenReturn(Optional.of(session));
        when(sourceFileReaderPort.read("C:/tmp/MyController.java"))
                .thenReturn(Optional.of("public class MyController {}"));

        SanitizedSourcePreviewResult result = serviceWithoutSanitization.preview("sess-3");

        assertThat(result.sanitized()).isFalse();
        assertThat(result.bundle().controllerRef()).isEqualTo("C:/tmp/MyController.java");
        assertThat(result.bundle().sanitizedSource()).isEqualTo("public class MyController {}");
        assertThat(result.bundle().estimatedTokens())
                .isEqualTo(TokenEstimator.estimate("public class MyController {}"));
    }

    @Test
    void preview_should_throw_when_session_is_missing() {
        when(sessionPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }
}
