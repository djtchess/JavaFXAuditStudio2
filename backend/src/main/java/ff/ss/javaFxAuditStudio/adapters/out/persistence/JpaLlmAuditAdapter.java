package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import java.util.List;
import java.util.Objects;

import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.domain.ai.LlmAuditEntry;
import ff.ss.javaFxAuditStudio.domain.ai.LlmProvider;
import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

/**
 * Adaptateur JPA implémentant le port LlmAuditPort (JAS-029 / IAP-2).
 *
 * <p>Convertit les enums {@link LlmProvider} et {@link TaskType} en String pour
 * la persistance JPA, et inversement lors de la lecture.
 *
 * <p>Assemble via {@code LlmAuditConfiguration} — pas de {@code @Repository}.
 */
public class JpaLlmAuditAdapter implements LlmAuditPort {

    private final LlmAuditRepository repository;

    public JpaLlmAuditAdapter(final LlmAuditRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(final LlmAuditEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        repository.save(toEntity(entry));
    }

    @Override
    public List<LlmAuditEntry> findBySessionId(final String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return repository.findBySessionId(sessionId)
                .stream()
                .map(JpaLlmAuditAdapter::toDomain)
                .toList();
    }

    private static LlmAuditEntity toEntity(final LlmAuditEntry entry) {
        return new LlmAuditEntity(
                entry.auditId(),
                entry.sessionId(),
                entry.timestamp(),
                entry.provider().value(),       // LlmProvider → String pour la BD
                entry.taskType().name(),         // TaskType → String pour la BD
                entry.sanitizationVersion(),
                entry.payloadHash(),
                entry.promptTokensEstimate(),
                entry.degraded(),
                entry.degradationReason());
    }

    private static LlmAuditEntry toDomain(final LlmAuditEntity entity) {
        return new LlmAuditEntry(
                entity.getAuditId(),
                entity.getSessionId(),
                entity.getTimestamp(),
                LlmProvider.fromString(entity.getProvider()),   // String → LlmProvider
                taskTypeFromString(entity.getTaskType()),        // String → TaskType
                entity.getSanitizationVersion(),
                entity.getPayloadHash(),
                entity.getPromptTokensEstimate() != null ? entity.getPromptTokensEstimate() : 0,
                entity.isDegraded(),
                entity.getDegradationReason());
    }

    private static TaskType taskTypeFromString(final String value) {
        if (value == null || value.isBlank()) {
            return TaskType.NAMING;
        }
        try {
            return TaskType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TaskType.NAMING; // fallback gracieux pour les entrées historiques inconnues
        }
    }
}
