package ff.ss.javaFxAuditStudio.adapters.in.rest.dto;

import java.util.Objects;

public record AgentOverviewResponse(
        String id,
        String label,
        String responsibility,
        String preferredModel) {

    public AgentOverviewResponse {
        Objects.requireNonNull(id, "id est obligatoire");
        Objects.requireNonNull(label, "label est obligatoire");
        Objects.requireNonNull(responsibility, "responsibility est obligatoire");
        Objects.requireNonNull(preferredModel, "preferredModel est obligatoire");
    }
}
