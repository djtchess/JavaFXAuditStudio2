package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generateur de l'interface Gateway.
 * Produit l'interface encapsulant les appels techniques externes identifies par les regles GATEWAY.
 */
public final class GatewayGenerator implements ArtifactGeneratorStrategy {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Gateway";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("/**\n");
        sb.append(" * Port sortant — appels techniques et IO pour ").append(baseName).append(".\n");
        sb.append(" */\n");
        sb.append("public interface ").append(className).append(" {\n\n");
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            String method = GeneratorUtils.cleanMethodName(GeneratorUtils.methodNameFromRule(rule));
            if (seen.add(method)) {
                sb.append("    /** ").append(rule.description()).append(" */\n");
                String returnType = GeneratorUtils.buildReturnType(rule);
                String params = GeneratorUtils.buildMethodSignature(rule);
                String effectiveParams = params.isEmpty() ? "Object request" : params;
                sb.append("    ").append(returnType).append(" ").append(method)
                  .append("(").append(effectiveParams).append(");\n\n");
            }
        }
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 4, ArtifactType.GATEWAY, className, sb.toString(), false);
    }
}
