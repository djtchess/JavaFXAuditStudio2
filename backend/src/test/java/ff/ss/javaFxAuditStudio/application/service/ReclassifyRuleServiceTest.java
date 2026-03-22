package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.application.ports.in.GenerateArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ClassificationResult;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ReclassificationAuditEntry;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;
import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReclassifyRuleServiceTest {

    @Mock
    private AnalysisSessionPort analysisSessionPort;

    @Mock
    private ClassificationPersistencePort classificationPersistencePort;

    @Mock
    private ReclassificationAuditPort reclassificationAuditPort;

    @Mock
    private GenerateArtifactsUseCase generateArtifactsUseCase;

    private ReclassifyRuleService service;

    private static final String ANALYSIS_ID = "session-abc";
    private static final String RULE_ID = "RG-001";
    private static final String CONTROLLER_REF = "com/example/MyController.java";

    @BeforeEach
    void setUp() {
        service = new ReclassifyRuleService(
                analysisSessionPort,
                classificationPersistencePort,
                reclassificationAuditPort,
                generateArtifactsUseCase);
    }

    @Test
    void reclassify_updatesRuleAndPersistsAudit() {
        BusinessRule original = buildRule(RULE_ID, ResponsibilityClass.UI);
        AnalysisSession session = buildSession(ANALYSIS_ID, AnalysisStatus.COMPLETED);
        ClassificationResult classification = buildClassification(original);

        when(analysisSessionPort.findById(ANALYSIS_ID)).thenReturn(Optional.of(session));
        when(classificationPersistencePort.findBySessionId(ANALYSIS_ID))
                .thenReturn(Optional.of(classification));
        when(classificationPersistencePort.save(eq(ANALYSIS_ID), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(reclassificationAuditPort.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        BusinessRule result = service.reclassify(ANALYSIS_ID, RULE_ID, ResponsibilityClass.APPLICATION, "raison test");

        assertThat(result.ruleId()).isEqualTo(RULE_ID);
        assertThat(result.responsibilityClass()).isEqualTo(ResponsibilityClass.APPLICATION);

        ArgumentCaptor<ReclassificationAuditEntry> auditCaptor =
                ArgumentCaptor.forClass(ReclassificationAuditEntry.class);
        verify(reclassificationAuditPort).save(auditCaptor.capture());
        ReclassificationAuditEntry audit = auditCaptor.getValue();
        assertThat(audit.fromCategory()).isEqualTo(ResponsibilityClass.UI);
        assertThat(audit.toCategory()).isEqualTo(ResponsibilityClass.APPLICATION);
        assertThat(audit.reason()).isEqualTo("raison test");
        assertThat(audit.analysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(audit.ruleId()).isEqualTo(RULE_ID);
    }

    @Test
    void reclassify_throwsIllegalStateException_whenSessionIsLocked() {
        AnalysisSession lockedSession = buildSession(ANALYSIS_ID, AnalysisStatus.LOCKED);
        when(analysisSessionPort.findById(ANALYSIS_ID)).thenReturn(Optional.of(lockedSession));

        assertThatThrownBy(() ->
                service.reclassify(ANALYSIS_ID, RULE_ID, ResponsibilityClass.APPLICATION, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCKED");

        verify(classificationPersistencePort, never()).save(any(), any());
        verify(reclassificationAuditPort, never()).save(any());
    }

    @Test
    void reclassify_throwsNoSuchElementException_whenSessionNotFound() {
        when(analysisSessionPort.findById(ANALYSIS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.reclassify(ANALYSIS_ID, RULE_ID, ResponsibilityClass.APPLICATION, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void reclassify_throwsNoSuchElementException_whenRuleNotFound() {
        AnalysisSession session = buildSession(ANALYSIS_ID, AnalysisStatus.COMPLETED);
        BusinessRule otherRule = buildRule("RG-999", ResponsibilityClass.UI);
        ClassificationResult classification = buildClassification(otherRule);

        when(analysisSessionPort.findById(ANALYSIS_ID)).thenReturn(Optional.of(session));
        when(classificationPersistencePort.findBySessionId(ANALYSIS_ID))
                .thenReturn(Optional.of(classification));

        assertThatThrownBy(() ->
                service.reclassify(ANALYSIS_ID, RULE_ID, ResponsibilityClass.APPLICATION, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void reclassify_triggersRegeneration_afterSuccessfulUpdate() {
        BusinessRule original = buildRule(RULE_ID, ResponsibilityClass.UI);
        AnalysisSession session = buildSession(ANALYSIS_ID, AnalysisStatus.COMPLETED);
        ClassificationResult classification = buildClassification(original);

        when(analysisSessionPort.findById(ANALYSIS_ID)).thenReturn(Optional.of(session));
        when(classificationPersistencePort.findBySessionId(ANALYSIS_ID))
                .thenReturn(Optional.of(classification));
        when(classificationPersistencePort.save(eq(ANALYSIS_ID), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(reclassificationAuditPort.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        service.reclassify(ANALYSIS_ID, RULE_ID, ResponsibilityClass.BUSINESS, null);

        verify(generateArtifactsUseCase).handle(ANALYSIS_ID, CONTROLLER_REF);
    }

    private static BusinessRule buildRule(final String ruleId, final ResponsibilityClass cls) {
        return new BusinessRule(
                ruleId,
                "Description " + ruleId,
                "com/example/MyController.java",
                10,
                cls,
                ExtractionCandidate.POLICY,
                false);
    }

    private static AnalysisSession buildSession(final String sessionId, final AnalysisStatus status) {
        return new AnalysisSession(
                sessionId,
                CONTROLLER_REF,
                null,
                status,
                Instant.now());
    }

    private static ClassificationResult buildClassification(final BusinessRule rule) {
        return new ClassificationResult(CONTROLLER_REF, List.of(rule), List.of());
    }
}
