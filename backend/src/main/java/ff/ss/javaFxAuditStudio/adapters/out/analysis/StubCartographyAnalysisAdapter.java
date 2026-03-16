package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptateur stub de cartographie.
 * Retourne des listes vides en attendant l'implémentation réelle du parsing FXML et Java.
 * Cet adapter sera remplacé par l'implémentation réelle en JAS-40,
 * qui connectera le parsing FXML (extractComponents) et le parsing Java (extractHandlers)
 * aux bibliothèques d'analyse appropriées.
 * Actif uniquement sous le profil Spring "stub".
 */
@Deprecated(forRemoval = true) // Suppression prevue JAS-301
@Profile("stub")
@Component
public class StubCartographyAnalysisAdapter implements CartographyAnalysisPort {

    @Override
    public List<FxmlComponent> extractComponents(final String fxmlContent) {
        // Parsing FXML réel prévu en JAS-40
        return List.of();
    }

    @Override
    public List<HandlerBinding> extractHandlers(final String javaContent) {
        // Parsing Java réel prévu en JAS-40
        return List.of();
    }
}
