package ff.ss.javaFxAuditStudio.domain.ai;

import java.time.Instant;
import java.util.Objects;

/**
 * Entrée d'audit d'un appel LLM sanitisé (JAS-029 / IAP-2).
 * Ne contient ni code brut, ni secret, ni table de renommage.
 *
 * @param auditId              UUID de l'entrée
 * @param sessionId            référence à la session d'analyse
 * @param timestamp            horodatage de l'appel
 * @param provider             fournisseur LLM utilisé (enum typesafe)
 * @param taskType             type de tâche IA (enum typesafe)
 * @param sanitizationVersion  version du profil de désensibilisation
 * @param payloadHash          SHA-256 hex du sanitizedSource (jamais la valeur)
 * @param promptTokensEstimate tokens estimés avant appel
 * @param degraded             vrai si mode dégradé
 * @param degradationReason    raison si degraded, vide sinon
 */
public record LlmAuditEntry(
        String auditId,
        String sessionId,
        Instant timestamp,
        LlmProvider provider,
        TaskType taskType,
        String sanitizationVersion,
        String payloadHash,
        int promptTokensEstimate,
        boolean degraded,
        String degradationReason) {

    public LlmAuditEntry {
        Objects.requireNonNull(auditId, "auditId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        degradationReason = degradationReason != null ? degradationReason : "";
    }
}
