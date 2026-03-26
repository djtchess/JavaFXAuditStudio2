package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Presentation d'un agent disponible dans le workbench")
public record AgentOverviewResponse(
        @Schema(description = "Identifiant technique de l'agent")
        String id,
        @Schema(description = "Nom lisible de l'agent")
        String label,
        @Schema(description = "Description de la responsabilite principale de l'agent")
        String responsibility,
        @Schema(description = "Modele LLM prefere pour cet agent")
        String preferredModel) {

    public AgentOverviewResponse {
        Objects.requireNonNull(id, "id est obligatoire");
        Objects.requireNonNull(label, "label est obligatoire");
        Objects.requireNonNull(responsibility, "responsibility est obligatoire");
        Objects.requireNonNull(preferredModel, "preferredModel est obligatoire");
    }
}
