package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Deprecated(forRemoval = true) // Suppression prevue JAS-301
@Profile("stub")
@Component
public class StubCodeGenerationAdapter implements CodeGenerationPort {

    @Override
    public List<CodeArtifact> generate(
            final String controllerRef,
            final String javaContent,
            final List<BusinessRule> classifiedRules) {
        return List.of();
    }
}
