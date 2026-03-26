package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de reponse pour une entree d'audit LLM (JAS-029).
 *
 * <p>Ne contient jamais le contenu sanitise brut — uniquement le hash SHA-256.
 *
 * @param auditId               UUID de l'entree
 * @param sessionId             reference a la session
 * @param timestamp             horodatage ISO-8601
 * @param provider              fournisseur IA utilise
 * @param taskType              type de tache
 * @param sanitizationVersion   version du profil de desensibilisation
 * @param payloadHash           SHA-256 hex du payload sanitise
 * @param promptTokensEstimate  tokens estimes avant appel
 * @param degraded              vrai si mode degrade
 * @param degradationReason     raison si mode degrade, vide sinon
 */
@Schema(description = "Entree de journal d'appel LLM")
public record LlmAuditEntryResponse(
        @Schema(description = "UUID de l'entree d'audit LLM")
        String auditId,
        @Schema(description = "Identifiant de la session d'analyse associee")
        String sessionId,
        @Schema(description = "Horodatage de l'appel LLM au format ISO-8601")
        String timestamp,
        @Schema(description = "Fournisseur IA utilise pour cet appel")
        String provider,
        @Schema(description = "Type de tache traitee par le LLM")
        String taskType,
        @Schema(description = "Version du profil de desensibilisation utilise")
        String sanitizationVersion,
        @Schema(description = "Hash SHA-256 hexadecimal du payload sanitise transmis au LLM")
        String payloadHash,
        @Schema(description = "Estimation du nombre de tokens du prompt avant l'appel")
        int promptTokensEstimate,
        @Schema(description = "Vrai si le mode degrade etait actif lors de cet appel")
        boolean degraded,
        @Schema(description = "Raison du mode degrade, chaine vide si mode nominal")
        String degradationReason) {
}
