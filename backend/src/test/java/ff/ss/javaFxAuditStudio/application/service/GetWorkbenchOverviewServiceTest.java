package ff.ss.javaFxAuditStudio.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.application.ports.out.WorkbenchCatalogPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AgentOverview;
import ff.ss.javaFxAuditStudio.domain.workbench.RefactoringLot;
import ff.ss.javaFxAuditStudio.domain.workbench.WorkbenchOverview;

class GetWorkbenchOverviewServiceTest {

    @Test
    void shouldReturnOverviewFromCatalogPort() {
        WorkbenchCatalogPort catalogPort;
        GetWorkbenchOverviewService service;
        WorkbenchOverview result;

        catalogPort = () -> new WorkbenchOverview(
                "Produit",
                "Resume",
                "Angular 21.2.x",
                "Spring Boot 4.0.3",
                List.of(new RefactoringLot(1, "Diagnostic", "Objectif", "Livrable")),
                List.of(new AgentOverview("architecture-applicative", "Architecture", "Pilotage", "claude-opus-4-6")));
        service = new GetWorkbenchOverviewService(catalogPort);

        result = service.handle();

        assertThat(result.productName()).isEqualTo("Produit");
        assertThat(result.lots()).hasSize(1);
        assertThat(result.agents()).extracting(AgentOverview::preferredModel).containsExactly("claude-opus-4-6");
    }
}
