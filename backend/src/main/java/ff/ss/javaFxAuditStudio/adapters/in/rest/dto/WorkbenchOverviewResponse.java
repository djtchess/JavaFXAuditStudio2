package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Vue d'ensemble du workbench JavaFX Audit Studio")
public record WorkbenchOverviewResponse(
        @Schema(description = "Nom du produit")
        String productName,
        @Schema(description = "Description courte du produit")
        String summary,
        @Schema(description = "Technologie cible frontend (ex: Angular)")
        String frontendTarget,
        @Schema(description = "Technologie cible backend (ex: Spring Boot)")
        String backendTarget,
        @Schema(description = "Liste des lots de refactoring planifies")
        List<RefactoringLotResponse> lots,
        @Schema(description = "Liste des agents disponibles dans le workbench")
        List<AgentOverviewResponse> agents) {

    public WorkbenchOverviewResponse {
        Objects.requireNonNull(productName, "productName est obligatoire");
        Objects.requireNonNull(summary, "summary est obligatoire");
        Objects.requireNonNull(frontendTarget, "frontendTarget est obligatoire");
        Objects.requireNonNull(backendTarget, "backendTarget est obligatoire");
        lots = (lots != null) ? List.copyOf(lots) : List.of();
        agents = (agents != null) ? List.copyOf(agents) : List.of();
    }
}
