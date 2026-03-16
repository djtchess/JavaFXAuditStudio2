package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.List;

public interface CodeGenerationPort {

    /**
     * Genere les artefacts a partir des regles classifiees.
     *
     * @param controllerRef   reference du controller
     * @param javaContent     contenu source Java
     * @param classifiedRules regles classifiees issues de l'extraction
     * @return liste d'artefacts generes, jamais null
     */
    List<CodeArtifact> generate(String controllerRef, String javaContent, List<BusinessRule> classifiedRules);

    /**
     * Compatibilite arriere : deleguee a la surcharge avec regles vides.
     */
    default List<CodeArtifact> generate(String controllerRef, String javaContent) {
        return generate(controllerRef, javaContent, List.of());
    }
}
