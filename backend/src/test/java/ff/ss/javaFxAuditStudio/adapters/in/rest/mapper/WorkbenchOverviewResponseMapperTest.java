package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AgentOverviewResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.RefactoringLotResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.WorkbenchOverviewResponse;
import ff.ss.javaFxAuditStudio.domain.workbench.AgentOverview;
import ff.ss.javaFxAuditStudio.domain.workbench.RefactoringLot;
import ff.ss.javaFxAuditStudio.domain.workbench.WorkbenchOverview;

class WorkbenchOverviewResponseMapperTest {

    private WorkbenchOverviewResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorkbenchOverviewResponseMapper();
    }

    @Test
    void toResponse_mapsProductNameAndSummary() {
        WorkbenchOverview overview;
        WorkbenchOverviewResponse response;

        overview = new WorkbenchOverview(
                "JavaFX Audit Studio",
                "Un studio d'audit logiciel",
                "Angular 21",
                "Spring Boot 4",
                List.of(),
                List.of());

        response = mapper.toResponse(overview);

        assertThat(response.productName()).isEqualTo("JavaFX Audit Studio");
        assertThat(response.summary()).isEqualTo("Un studio d'audit logiciel");
        assertThat(response.frontendTarget()).isEqualTo("Angular 21");
        assertThat(response.backendTarget()).isEqualTo("Spring Boot 4");
    }

    @Test
    void toResponse_mapsLotsCorrectly() {
        RefactoringLot lot;
        WorkbenchOverview overview;
        WorkbenchOverviewResponse response;
        RefactoringLotResponse lotResponse;

        lot = new RefactoringLot(1, "Lot Socle", "Stabiliser l'architecture", "Architecture hexagonale en place");
        overview = new WorkbenchOverview(
                "Produit",
                "Résumé",
                "Frontend",
                "Backend",
                List.of(lot),
                List.of());

        response = mapper.toResponse(overview);

        assertThat(response.lots()).hasSize(1);
        lotResponse = response.lots().get(0);
        assertThat(lotResponse.number()).isEqualTo(1);
        assertThat(lotResponse.title()).isEqualTo("Lot Socle");
        assertThat(lotResponse.objective()).isEqualTo("Stabiliser l'architecture");
        assertThat(lotResponse.primaryOutcome()).isEqualTo("Architecture hexagonale en place");
    }

    @Test
    void toResponse_mapsAgentsCorrectly() {
        AgentOverview agent;
        WorkbenchOverview overview;
        WorkbenchOverviewResponse response;
        AgentOverviewResponse agentResponse;

        agent = new AgentOverview("agent-1", "Architecte", "Conception hexagonale", "claude-sonnet-4-6");
        overview = new WorkbenchOverview(
                "Produit",
                "Résumé",
                "Frontend",
                "Backend",
                List.of(),
                List.of(agent));

        response = mapper.toResponse(overview);

        assertThat(response.agents()).hasSize(1);
        agentResponse = response.agents().get(0);
        assertThat(agentResponse.id()).isEqualTo("agent-1");
        assertThat(agentResponse.label()).isEqualTo("Architecte");
        assertThat(agentResponse.responsibility()).isEqualTo("Conception hexagonale");
        assertThat(agentResponse.preferredModel()).isEqualTo("claude-sonnet-4-6");
    }

    @Test
    void toResponse_returnsEmptyLists_whenLotsAndAgentsEmpty() {
        WorkbenchOverview overview;
        WorkbenchOverviewResponse response;

        overview = new WorkbenchOverview(
                "Produit",
                "Résumé",
                "Frontend",
                "Backend",
                List.of(),
                List.of());

        response = mapper.toResponse(overview);

        assertThat(response.lots()).isNotNull();
        assertThat(response.lots()).isEmpty();
        assertThat(response.agents()).isNotNull();
        assertThat(response.agents()).isEmpty();
    }
}
