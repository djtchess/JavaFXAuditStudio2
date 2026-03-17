package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generateur du Bridge transitoire.
 * Produit un artefact encapsulant les regles non classifiees (ExtractionCandidate.NONE).
 * Le Bridge est marque comme transitoire et doit etre supprime apres classification manuelle.
 */
public final class BridgeGenerator implements ArtifactGeneratorStrategy {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Bridge";
        List<BusinessRule> unknownRules = rules.stream()
                .filter(r -> r.extractionCandidate() == ExtractionCandidate.NONE)
                .toList();
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("/**\n");
        sb.append(" * BRIDGE TRANSITOIRE — encapsule les methodes dont la responsabilite\n");
        sb.append(" * reste indeterminee apres classification automatique.\n");
        sb.append(" * A supprimer apres classification manuelle et migration complete.\n");
        sb.append(" * ").append(unknownRules.size()).append(" regle(s) non classifiee(s).\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" {\n\n");
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : unknownRules) {
            String method = GeneratorUtils.methodNameFromRule(rule);
            if (seen.add(method)) {
                String params = GeneratorUtils.buildMethodSignature(rule);
                sb.append("    // TODO [").append(rule.ruleId()).append("] ")
                  .append(rule.description()).append("\n");
                sb.append("    public void ").append(method).append("(").append(params).append(") {\n");
                sb.append("        // Responsabilite a determiner et migrer\n");
                sb.append("        throw new UnsupportedOperationException(\"Bridge non migre : ")
                  .append(method).append("\");\n");
                sb.append("    }\n\n");
            }
        }
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 1, ArtifactType.BRIDGE, className, sb.toString(), true);
    }
}
