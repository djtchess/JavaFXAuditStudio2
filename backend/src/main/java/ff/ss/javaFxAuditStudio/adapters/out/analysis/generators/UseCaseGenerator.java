package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generateur de l'interface UseCase.
 * Produit l'interface portant les intentions utilisateur extraites des regles classifiees USE_CASE.
 * JAS-010 : implemente ArtifactGenerator (port applicatif) au lieu de ArtifactGeneratorStrategy.
 */
@Component
public final class UseCaseGenerator implements ArtifactGenerator {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "UseCase";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("/**\n");
        sb.append(" * Port d'entree — intentions utilisateur liees a ").append(baseName).append(".\n");
        sb.append(" */\n");
        sb.append("public interface ").append(className).append(" {\n\n");
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            String method = GeneratorUtils.cleanMethodName(GeneratorUtils.methodNameFromRule(rule));
            if (seen.add(method)) {
                sb.append("    /** ").append(rule.description()).append(" */\n");
                String returnType = GeneratorUtils.buildReturnType(rule);
                String params = GeneratorUtils.buildMethodSignature(rule);
                if (!rule.hasSignature()) {
                    sb.append("    ").append(returnType).append(" ").append(method)
                      .append("(").append(params).append("); // TODO: verifier signature\n\n");
                } else {
                    sb.append("    ").append(returnType).append(" ").append(method)
                      .append("(").append(params).append(");\n\n");
                }
            }
        }
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 2, ArtifactType.USE_CASE, className, sb.toString(), false);
    }
}
