package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.Objects;

public record RefactoringLotResponse(
        int number,
        String title,
        String objective,
        String primaryOutcome) {

    public RefactoringLotResponse {
        Objects.requireNonNull(title, "title est obligatoire");
        Objects.requireNonNull(objective, "objective est obligatoire");
        Objects.requireNonNull(primaryOutcome, "primaryOutcome est obligatoire");
    }
}
