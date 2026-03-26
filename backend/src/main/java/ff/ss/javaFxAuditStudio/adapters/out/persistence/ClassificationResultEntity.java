package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.rules.ParsingMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classification_result")
public class ClassificationResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 36, nullable = false, unique = true)
    private String sessionId;

    @Column(name = "controller_ref", length = 512)
    private String controllerRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "parsing_mode", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ParsingMode parsingMode = ParsingMode.AST;

    @Column(name = "parsing_fallback_reason")
    private String parsingFallbackReason;

    @Column(name = "excluded_lifecycle_methods_count", nullable = false)
    private int excludedLifecycleMethodsCount = 0;

    @OneToMany(mappedBy = "classification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BusinessRuleEntity> rules = new ArrayList<>();

    /** Constructeur no-arg requis par JPA. */
    protected ClassificationResultEntity() {
    }

    public ClassificationResultEntity(
            final String sessionId,
            final String controllerRef,
            final Instant createdAt) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.createdAt = createdAt;
    }

    public ClassificationResultEntity(
            final String sessionId,
            final String controllerRef,
            final Instant createdAt,
            final ParsingMode parsingMode,
            final String parsingFallbackReason) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.createdAt = createdAt;
        this.parsingMode = parsingMode != null ? parsingMode : ParsingMode.AST;
        this.parsingFallbackReason = parsingFallbackReason;
    }

    public ClassificationResultEntity(
            final String sessionId,
            final String controllerRef,
            final Instant createdAt,
            final ParsingMode parsingMode,
            final String parsingFallbackReason,
            final int excludedLifecycleMethodsCount) {
        this.sessionId = sessionId;
        this.controllerRef = controllerRef;
        this.createdAt = createdAt;
        this.parsingMode = parsingMode != null ? parsingMode : ParsingMode.AST;
        this.parsingFallbackReason = parsingFallbackReason;
        this.excludedLifecycleMethodsCount = excludedLifecycleMethodsCount;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getControllerRef() {
        return controllerRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ParsingMode getParsingMode() {
        return parsingMode;
    }

    public void setParsingMode(final ParsingMode parsingMode) {
        this.parsingMode = parsingMode != null ? parsingMode : ParsingMode.AST;
    }

    public String getParsingFallbackReason() {
        return parsingFallbackReason;
    }

    public void setParsingFallbackReason(final String parsingFallbackReason) {
        this.parsingFallbackReason = parsingFallbackReason;
    }

    public int getExcludedLifecycleMethodsCount() {
        return excludedLifecycleMethodsCount;
    }

    public void setExcludedLifecycleMethodsCount(final int excludedLifecycleMethodsCount) {
        this.excludedLifecycleMethodsCount = excludedLifecycleMethodsCount;
    }

    public List<BusinessRuleEntity> getRules() {
        return rules;
    }
}
