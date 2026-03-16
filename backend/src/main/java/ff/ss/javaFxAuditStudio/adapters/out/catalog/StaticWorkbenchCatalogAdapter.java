package ff.ss.javaFxAuditStudio.adapters.out.catalog;

import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.application.ports.out.WorkbenchCatalogPort;
import ff.ss.javaFxAuditStudio.domain.workbench.AgentOverview;
import ff.ss.javaFxAuditStudio.domain.workbench.RefactoringLot;
import ff.ss.javaFxAuditStudio.domain.workbench.WorkbenchOverview;

@Component
public class StaticWorkbenchCatalogAdapter implements WorkbenchCatalogPort {

    @Override
    public WorkbenchOverview load() {
        WorkbenchOverview workbenchOverview;

        workbenchOverview = new WorkbenchOverview(
                "JavaFX Audit Studio",
                "Cockpit de refactoring progressif pour controllers JavaFX + Spring.",
                "Angular 21.2.x",
                "JDK 21 / Spring Boot 4.0.3",
                buildLots(),
                buildAgents());
        return workbenchOverview;
    }

    private List<RefactoringLot> buildLots() {
        List<RefactoringLot> lots;

        lots = List.of(
                new RefactoringLot(1, "Diagnostic", "Qualifier la dette et la cible.", "Cartographie et responsabilites"),
                new RefactoringLot(2, "Socle applicatif", "Introduire ViewModel, UseCases et policy.", "Premiers points d'appui"),
                new RefactoringLot(3, "Flux majeurs", "Migrer les flux les plus charges.", "Controller aminci"),
                new RefactoringLot(4, "Adaptateurs Spring", "Reconnecter l'existant via ports et adapters.", "Wiring backend"),
                new RefactoringLot(5, "Assemblers et strategies", "Finaliser les variantes et la fabrication de donnees.",
                        "Extraction structurelle complete"));
        return lots;
    }

    private List<AgentOverview> buildAgents() {
        List<AgentOverview> agents;

        agents = List.of(
                new AgentOverview("architecture-applicative", "Architecture applicative",
                        "Cadre produit et arbitrages", "claude-opus-4-6"),
                new AgentOverview("backend-hexagonal", "Backend hexagonal",
                        "Use cases, ports et adapters Spring Boot", "claude-sonnet-4-6"),
                new AgentOverview("frontend-angular", "Frontend Angular",
                        "Cockpit conversationnel et restitution", "claude-sonnet-4-6"),
                new AgentOverview("implementation-moteur-analyse", "Implementation moteur",
                        "Analyse, classification et generation", "claude-sonnet-4-6"),
                new AgentOverview("gouvernance", "Gouvernance",
                        "Coherence globale et validation transverse", "claude-opus-4-6"));
        return agents;
    }
}
