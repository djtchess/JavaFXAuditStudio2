package ff.ss.javaFxAuditStudio.adapters.in.rest;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiEnrichmentStatusResponse;
import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST pour le statut de la configuration IA (JAS-022).
 *
 * <p>Expose :
 * <ul>
 *   <li>{@code GET /api/v1/ai-enrichment/status}</li>
 * </ul>
 *
 * <p>Regles de securite :
 * - La valeur de l'API key n'est JAMAIS retournee dans la reponse.
 * - Seul le champ {@code credentialPresent} indique la presence d'un credential.
 * - Ce controller lit uniquement {@link AiEnrichmentProperties} — il n'appelle pas {@code EnrichAnalysisUseCase}.
 */
@Tag(name = "Enrichissement IA")
@RestController
@RequestMapping("/api/v1/ai-enrichment")
public class AiEnrichmentStatusController {

    private final AiEnrichmentProperties aiEnrichmentProperties;

    public AiEnrichmentStatusController(final AiEnrichmentProperties aiEnrichmentProperties) {
        this.aiEnrichmentProperties = aiEnrichmentProperties;
    }

    /**
     * Retourne le statut de la configuration de l'enrichissement IA.
     *
     * @return 200 avec {@link AiEnrichmentStatusResponse} — jamais la valeur des credentials
     */
    @Operation(summary = "Statut du service IA", description = "Retourne la configuration du service d'enrichissement IA : activation, fournisseur, presence des credentials et timeout.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut disponible")
    })
    @GetMapping("/status")
    public ResponseEntity<AiEnrichmentStatusResponse> status() {
        // Pour les fournisseurs sans credential (CLI), credentialPresent = true :
        // l'authentification est geree localement par le CLI, pas par une API key.
        boolean credentialPresent = !aiEnrichmentProperties.isCredentialRequired()
                || (aiEnrichmentProperties.activeApiKey() != null
                        && !aiEnrichmentProperties.activeApiKey().isBlank());

        AiEnrichmentStatusResponse response = new AiEnrichmentStatusResponse(
                aiEnrichmentProperties.enabled(),
                aiEnrichmentProperties.provider(),
                credentialPresent,
                aiEnrichmentProperties.effectiveTimeoutMs());

        return ResponseEntity.ok(response);
    }
}
