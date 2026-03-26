package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entite JPA pour la table llm_audit_log (JAS-029).
 */
@Entity
@Table(name = "llm_audit_log")
public class LlmAuditEntity {

    @Id
    @Column(name = "audit_id", length = 36, nullable = false)
    private String auditId;

    @Column(name = "session_id", length = 255, nullable = false)
    private String sessionId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Column(name = "task_type", length = 50, nullable = false)
    private String taskType;

    @Column(name = "sanitization_version", length = 20)
    private String sanitizationVersion;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "prompt_tokens_estimate")
    private Integer promptTokensEstimate;

    @Column(name = "degraded", nullable = false)
    private boolean degraded;

    @Column(name = "degradation_reason", length = 500)
    private String degradationReason;

    /** Constructeur no-arg requis par JPA. */
    protected LlmAuditEntity() {
    }

    public LlmAuditEntity(
            String auditId,
            String sessionId,
            Instant timestamp,
            String provider,
            String taskType,
            String sanitizationVersion,
            String payloadHash,
            Integer promptTokensEstimate,
            boolean degraded,
            String degradationReason) {
        this.auditId = auditId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.provider = provider;
        this.taskType = taskType;
        this.sanitizationVersion = sanitizationVersion;
        this.payloadHash = payloadHash;
        this.promptTokensEstimate = promptTokensEstimate;
        this.degraded = degraded;
        this.degradationReason = degradationReason;
    }

    public String getAuditId() {
        return auditId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getProvider() {
        return provider;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getSanitizationVersion() {
        return sanitizationVersion;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public Integer getPromptTokensEstimate() {
        return promptTokensEstimate;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public String getDegradationReason() {
        return degradationReason;
    }
}
