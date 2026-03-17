package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub remplace par l'implementation reelle de parsing Java/FXML.
 * Retourne une liste vide en attendant le parsing effectif prevu dans les stories suivantes de JAS-40.
 * Actif uniquement avec le profil Spring "stub".
 */
@Deprecated(forRemoval = true) // Suppression prevue JAS-301
@Profile("stub")
@Component
public class StubRuleExtractionAdapter implements RuleExtractionPort {

    @Override
    public ExtractionResult extract(final String controllerRef, final String javaContent) {
        // Parsing Java/FXML reel prevu en JAS-40 impl
        return ExtractionResult.ast(List.of());
    }
}
