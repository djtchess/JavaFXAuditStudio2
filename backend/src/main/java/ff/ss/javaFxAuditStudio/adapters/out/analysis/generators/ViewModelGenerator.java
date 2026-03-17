package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Generateur du ViewModel.
 * Produit la classe portant l'etat de presentation extrait des regles classifiees VIEW_MODEL.
 */
public final class ViewModelGenerator implements ArtifactGeneratorStrategy {

    @Override
    public CodeArtifact generate(final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "ViewModel";
        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("import javafx.beans.property.SimpleBooleanProperty;\n");
        sb.append("import javafx.beans.property.SimpleIntegerProperty;\n");
        sb.append("import javafx.beans.property.SimpleStringProperty;\n");
        sb.append("import javafx.beans.property.BooleanProperty;\n");
        sb.append("import javafx.beans.property.IntegerProperty;\n");
        sb.append("import javafx.beans.property.StringProperty;\n");
        sb.append("import org.springframework.stereotype.Component;\n\n");
        sb.append("/**\n");
        sb.append(" * ViewModel de presentation pour ").append(baseName).append(".\n");
        sb.append(" * Expose les proprietes JavaFX observables liees aux composants FXML.\n");
        sb.append(" * Les composants conteneurs (VBox, HBox, GridPane) exposent une propriete visible.\n");
        sb.append(" * Les boutons exposent une propriete enabled. Les labels exposent leur texte.\n");
        sb.append(" */\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append(" {\n\n");
        if (rules.isEmpty()) {
            sb.append("    // TODO : declarer les proprietes JavaFX (StringProperty, BooleanProperty...)\n");
        } else {
            var seenFields = new LinkedHashSet<String>();
            for (BusinessRule rule : rules) {
                if (rule.description().startsWith("Methode handler")) {
                    continue;
                }
                String rawField = GeneratorUtils.fieldNameFromRule(rule);
                String fxmlType = GeneratorUtils.extractFxmlType(rule.description());
                ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty(rawField, fxmlType);
                if (vmp == null) {
                    if (seenFields.add(rawField)) {
                        sb.append("    // TODO [").append(rawField).append("]")
                          .append(" : ObservableList ou propriete specifique a definir")
                          .append(" (").append(fxmlType).append(")\n\n");
                    }
                    continue;
                }
                if (!seenFields.add(vmp.fieldName())) {
                    continue;
                }
                PropertyType pt = vmp.type();
                sb.append("    /** ").append(rule.description()).append(" */\n");
                sb.append("    private final ").append(pt.simpleClass()).append(" ")
                  .append(vmp.fieldName())
                  .append(" = new ").append(pt.simpleClass()).append("();\n\n");
                sb.append("    public ").append(pt.propertyClass()).append(" ")
                  .append(vmp.fieldName()).append("Property() {\n");
                sb.append("        return ").append(vmp.fieldName()).append(";\n    }\n\n");
                sb.append("    public ").append(pt.javaType()).append(" get")
                  .append(GeneratorUtils.capitalize(vmp.fieldName())).append("() {\n");
                sb.append("        return ").append(vmp.fieldName()).append(".get();\n    }\n\n");
                sb.append("    public void set").append(GeneratorUtils.capitalize(vmp.fieldName()))
                  .append("(final ").append(pt.javaType()).append(" value) {\n");
                sb.append("        ").append(vmp.fieldName()).append(".set(value);\n    }\n\n");
            }
        }
        sb.append("}\n");
        return GeneratorUtils.artifact(baseName, 2, ArtifactType.VIEW_MODEL, className, sb.toString(), false);
    }
}
