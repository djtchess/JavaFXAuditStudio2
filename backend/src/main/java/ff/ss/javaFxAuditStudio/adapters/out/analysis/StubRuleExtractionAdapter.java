package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub remplacé par l'implémentation réelle de parsing Java/FXML.
 * Retourne une liste vide en attendant le parsing effectif prévu dans les stories suivantes de JAS-40.
 * Actif uniquement avec le profil Spring "stub".
 */
@Profile("stub")
@Component
public class StubRuleExtractionAdapter implements RuleExtractionPort {

    @Override
    public List<BusinessRule> extract(final String controllerRef, final String javaContent) {
        // Parsing Java/FXML réel prévu en JAS-40 impl
        return List.of();
    }
}
