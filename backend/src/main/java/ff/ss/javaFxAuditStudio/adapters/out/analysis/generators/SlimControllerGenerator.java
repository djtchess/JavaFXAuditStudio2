package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generateur du controller allege (Slim Controller).
 * Produit un squelette de controller qui deleguera au ViewModel et aux UseCases.
 */
public final class SlimControllerGenerator implements ArtifactGeneratorStrategy {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "SlimController";
        String vmClass = baseName + "ViewModel";
        String useCaseClass = baseName + "UseCase";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("import javafx.fxml.FXML;\n");
        sb.append("import org.springframework.stereotype.Component;\n\n");
        sb.append("/**\n");
        sb.append(" * Controller JavaFX allege — delegue l'etat UI au ViewModel\n");
        sb.append(" * et les intentions utilisateur aux UseCases.\n");
        sb.append(" */\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    private final ").append(vmClass).append(" viewModel;\n");
        if (!rules.isEmpty()) {
            sb.append("    private final ").append(useCaseClass).append(" useCase;\n");
        }
        sb.append("\n");
        sb.append("    public ").append(className).append("(\n");
        sb.append("            final ").append(vmClass).append(" viewModel");
        if (!rules.isEmpty()) {
            sb.append(",\n            final ").append(useCaseClass).append(" useCase");
        }
        sb.append(") {\n");
        sb.append("        this.viewModel = viewModel;\n");
        if (!rules.isEmpty()) {
            sb.append("        this.useCase = useCase;\n");
        }
        sb.append("    }\n\n");
        sb.append("    @FXML\n");
        sb.append("    public void initialize() {\n");
        sb.append("        // TODO : lier les composants FXML aux proprietes du ViewModel\n");
        sb.append("    }\n");
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            String handler = GeneratorUtils.cleanMethodName(GeneratorUtils.methodNameFromRule(rule));
            if (seen.add(handler)) {
                appendHandlerMethod(sb, handler, rule);
            }
        }
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 1, ArtifactType.CONTROLLER_SLIM, className, sb.toString(), true);
    }

    private void appendHandlerMethod(final StringBuilder sb, final String handler, final BusinessRule rule) {
        String params = GeneratorUtils.buildMethodSignature(rule);
        String args = GeneratorUtils.buildArgumentList(rule);
        sb.append("\n    @FXML\n");
        sb.append("    public void ").append(handler).append("(").append(params).append(") {\n");
        sb.append("        useCase.").append(handler).append("(").append(args).append(");\n");
        sb.append("    }\n");
    }
}
