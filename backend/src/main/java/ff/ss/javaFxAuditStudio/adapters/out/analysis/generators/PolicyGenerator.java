package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generateur de la classe Policy.
 * Produit la classe de decision metier extraite des regles classifiees POLICY.
 * JAS-010 : implemente ArtifactGenerator (port applicatif) au lieu de ArtifactGeneratorStrategy.
 */
@Component
public final class PolicyGenerator implements ArtifactGenerator {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Policy";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("import org.springframework.stereotype.Component;\n\n");
        sb.append("/**\n");
        sb.append(" * Regles de gestion et decisions metier pour ").append(baseName).append(".\n");
        sb.append(" */\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append(" {\n\n");
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            String method = GeneratorUtils.methodNameFromRule(rule);
            if (seen.add(method)) {
                sb.append("    /** ").append(rule.description()).append(" */\n");

                // JAS-020 : utiliser la vraie signature si disponible
                String returnType = rule.hasSignature()
                        ? GeneratorUtils.buildReturnType(rule)
                        : "boolean";
                String params = rule.hasSignature()
                        ? GeneratorUtils.buildUseCaseParams(rule)
                        : "final Object context";
                // Fallback : si pas de signature ET params vides, mettre Object context
                if (params.isEmpty() && !rule.hasSignature()) {
                    params = "final Object context";
                }

                sb.append("    public ").append(returnType).append(" ").append(method)
                  .append("(").append(params).append(") {\n");
                sb.append("        // TODO : implementer la regle metier\n");
                if ("void".equals(returnType)) {
                    sb.append("        return;\n");
                } else {
                    String defaultValue = GeneratorUtils.defaultValueExpression(returnType);
                    sb.append("        return ").append(defaultValue).append(";\n");
                }
                sb.append("    }\n\n");
            }
        }
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 3, ArtifactType.POLICY, className, sb.toString(), false);
    }
}
