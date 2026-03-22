package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.List;

/**
 * Strategy de generation d'artefact.
 * Chaque implementation produit un artefact d'un type donne a partir du baseName,
 * du package et de la liste de regles classifiees.
 *
 * @deprecated depuis JAS-010 — utiliser {@link ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator}
 *     a la place. Cette interface reste presente pour eviter les regressions le temps de la migration.
 *     Elle sera supprimee dans un lot ulterieur.
 */
@Deprecated(forRemoval = true)
public interface ArtifactGeneratorStrategy {

    @Deprecated(forRemoval = true)
    CodeArtifact generate(String baseName, String pkg, List<BusinessRule> rules);

    /**
     * Surcharge sans regles : deleguee a la methode principale avec une liste vide.
     * Utilisee notamment par AssemblerGenerator qui n'a pas de regles associees.
     */
    @Deprecated(forRemoval = true)
    default CodeArtifact generate(String baseName, String pkg) {
        return generate(baseName, pkg, List.of());
    }
}
