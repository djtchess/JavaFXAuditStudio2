package ff.ss.javaFxAuditStudio.application.generation;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.List;

/**
 * Port applicatif de generation d'un type d'artefact de refactoring.
 * Chaque implementation produit un artefact structurel distinct
 * (UseCase, Gateway, ViewModel, Policy, Bridge, Assembler, Strategy).
 * JAS-010 : remplace ArtifactGeneratorStrategy — interface dans le package application, non adapters.
 */
public interface ArtifactGenerator extends ArtifactResult {

    CodeArtifact generate(String baseName, String pkg, List<BusinessRule> rules);

    /**
     * Surcharge sans regles : deleguee a la methode principale avec une liste vide.
     * Utilisee notamment par AssemblerGenerator qui n'a pas de regles associees.
     */
    default CodeArtifact generate(String baseName, String pkg) {
        return generate(baseName, pkg, List.of());
    }
}
