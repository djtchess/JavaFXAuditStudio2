package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generateur de l'interface Strategy metier.
 * Produit l'interface modelisant les variantes de workflow conditionnelles (ExtractionCandidate.STRATEGY).
 * Le nom de la classe est suffixe "ArtifactGenerator" pour eviter la collision
 * avec le port applicatif ArtifactGenerator (JAS-010).
 * JAS-010 : implemente ArtifactGenerator (port applicatif) au lieu de ArtifactGeneratorStrategy.
 */
@Component
public final class StrategyArtifactGenerator implements ArtifactGenerator {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Strategy";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("/**\n");
        sb.append(" * Strategie de comportement variable pour ").append(baseName).append(".\n");
        sb.append(" */\n");
        sb.append("public interface ").append(className).append(" {\n\n");
        for (BusinessRule rule : rules) {
            sb.append("    /** ").append(rule.description()).append(" */\n");
        }
        sb.append("    Object apply(Object context);\n");
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 5, ArtifactType.STRATEGY, className, sb.toString(), false);
    }
}
