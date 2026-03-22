package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generateur de la classe Assembler.
 * Produit la classe responsable du mapping entre les champs UI et les objets domaine.
 * L'Assembler n'a pas de regles associees — le parametre rules est ignore.
 * JAS-010 : implemente ArtifactGenerator (port applicatif) au lieu de ArtifactGeneratorStrategy.
 */
@Component
public final class AssemblerGenerator implements ArtifactGenerator {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Assembler";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("import org.springframework.stereotype.Component;\n\n");
        sb.append("/**\n");
        sb.append(" * Assembleur — mapping entre la couche UI et le domaine pour ").append(baseName).append(".\n");
        sb.append(" */\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    public Object assemble(final Object formData) {\n");
        sb.append("        // TODO : mapping UI -> Domain\n");
        sb.append("        throw new UnsupportedOperationException(\"assemble non implemente\");\n");
        sb.append("    }\n");
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 5, ArtifactType.ASSEMBLER, className, sb.toString(), false);
    }
}
