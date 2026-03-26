package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Lot de refactoring planifie dans la migration hexagonale")
public record RefactoringLotResponse(
        @Schema(description = "Numero du lot (1 a 5)")
        int number,
        @Schema(description = "Titre court du lot")
        String title,
        @Schema(description = "Objectif principal du lot")
        String objective,
        @Schema(description = "Resultat primaire attendu a la fin du lot")
        String primaryOutcome) {

    public RefactoringLotResponse {
        Objects.requireNonNull(title, "title est obligatoire");
        Objects.requireNonNull(objective, "objective est obligatoire");
        Objects.requireNonNull(primaryOutcome, "primaryOutcome est obligatoire");
    }
}
