package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generation contextuelle d'artefacts a partir des regles classifiees.
 * Regroupe les regles par ExtractionCandidate et genere un artefact par groupe.
 * Instancie par GenerationConfiguration — pas d'annotation Spring.
 */
public final class RealCodeGenerationAdapter implements CodeGenerationPort {

    @Override
    public List<CodeArtifact> generate(
            final String controllerRef,
            final String javaContent,
            final List<BusinessRule> classifiedRules) {
        String baseName = extractBaseName(controllerRef);
        List<CodeArtifact> artifacts = new ArrayList<>();
        artifacts.add(buildControllerSlim(baseName));
        if (classifiedRules.isEmpty()) {
            addGenericArtifacts(baseName, artifacts);
            return List.copyOf(artifacts);
        }
        Map<ExtractionCandidate, List<BusinessRule>> grouped = groupByCandidate(classifiedRules);
        addContextualArtifacts(baseName, grouped, artifacts);
        addBridgeIfNeeded(baseName, classifiedRules, artifacts);
        return List.copyOf(artifacts);
    }

    private Map<ExtractionCandidate, List<BusinessRule>> groupByCandidate(
            final List<BusinessRule> rules) {
        return rules.stream()
                .filter(r -> r.extractionCandidate() != ExtractionCandidate.NONE)
                .collect(Collectors.groupingBy(BusinessRule::extractionCandidate));
    }

    private void addContextualArtifacts(
            final String baseName,
            final Map<ExtractionCandidate, List<BusinessRule>> grouped,
            final List<CodeArtifact> artifacts) {
        if (grouped.containsKey(ExtractionCandidate.VIEW_MODEL)) {
            artifacts.add(buildViewModel(baseName, grouped.get(ExtractionCandidate.VIEW_MODEL)));
        }
        if (grouped.containsKey(ExtractionCandidate.USE_CASE)) {
            artifacts.add(buildUseCase(baseName, grouped.get(ExtractionCandidate.USE_CASE)));
        }
        if (grouped.containsKey(ExtractionCandidate.POLICY)) {
            artifacts.add(buildPolicy(baseName, grouped.get(ExtractionCandidate.POLICY)));
        }
        if (grouped.containsKey(ExtractionCandidate.GATEWAY)) {
            artifacts.add(buildGateway(baseName, grouped.get(ExtractionCandidate.GATEWAY)));
        }
        if (grouped.containsKey(ExtractionCandidate.ASSEMBLER)) {
            artifacts.add(buildAssembler(baseName));
        }
        if (grouped.containsKey(ExtractionCandidate.STRATEGY)) {
            artifacts.add(buildStrategy(baseName, grouped.get(ExtractionCandidate.STRATEGY)));
        }
    }

    private void addGenericArtifacts(final String baseName, final List<CodeArtifact> artifacts) {
        artifacts.add(buildViewModel(baseName, List.of()));
        artifacts.add(buildUseCase(baseName, List.of()));
        artifacts.add(buildPolicy(baseName, List.of()));
        artifacts.add(buildGateway(baseName, List.of()));
        artifacts.add(buildAssembler(baseName));
    }

    private void addBridgeIfNeeded(
            final String baseName,
            final List<BusinessRule> rules,
            final List<CodeArtifact> artifacts) {
        boolean hasUnknown = rules.stream()
                .anyMatch(r -> r.extractionCandidate() == ExtractionCandidate.NONE);
        if (hasUnknown) {
            artifacts.add(buildBridge(baseName, rules));
        }
    }

    private CodeArtifact buildControllerSlim(final String baseName) {
        String className = baseName + "SlimController";
        String content = "public class " + className + " {\n"
                + "    // Delegation au ViewModel et aux UseCases\n"
                + "    // Les handlers FXML appellent le ViewModel pour l'etat UI\n"
                + "    // et les UseCases pour les intentions utilisateur.\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, 1, ArtifactType.CONTROLLER_SLIM),
                ArtifactType.CONTROLLER_SLIM, 1, className, content, true);
    }

    private CodeArtifact buildViewModel(final String baseName, final List<BusinessRule> rules) {
        String className = baseName + "ViewModel";
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(className).append(" {\n");
        if (rules.isEmpty()) {
            sb.append("    // Etat de presentation a definir\n");
        } else {
            for (BusinessRule rule : rules) {
                sb.append("    // ").append(rule.description()).append("\n");
            }
        }
        sb.append("}");
        return new CodeArtifact(
                artifactId(baseName, 2, ArtifactType.VIEW_MODEL),
                ArtifactType.VIEW_MODEL, 2, className, sb.toString(), false);
    }

    private CodeArtifact buildUseCase(final String baseName, final List<BusinessRule> rules) {
        String className = baseName + "UseCase";
        StringBuilder sb = new StringBuilder();
        sb.append("public interface ").append(className).append(" {\n");
        if (rules.isEmpty()) {
            sb.append("    void execute(String input);\n");
        } else {
            for (BusinessRule rule : rules) {
                String method = methodNameFromRule(rule);
                sb.append("    // ").append(rule.description()).append("\n");
                sb.append("    void ").append(method).append("();\n");
            }
        }
        sb.append("}");
        return new CodeArtifact(
                artifactId(baseName, 2, ArtifactType.USE_CASE),
                ArtifactType.USE_CASE, 2, className, sb.toString(), false);
    }

    private CodeArtifact buildPolicy(final String baseName, final List<BusinessRule> rules) {
        String className = baseName + "Policy";
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(className).append(" {\n");
        if (rules.isEmpty()) {
            sb.append("    public boolean decide(Object context) { return false; }\n");
        } else {
            for (BusinessRule rule : rules) {
                String method = methodNameFromRule(rule);
                sb.append("    // ").append(rule.description()).append("\n");
                sb.append("    public boolean ").append(method).append("(Object context) {\n");
                sb.append("        return false; // A implementer\n");
                sb.append("    }\n");
            }
        }
        sb.append("}");
        return new CodeArtifact(
                artifactId(baseName, 3, ArtifactType.POLICY),
                ArtifactType.POLICY, 3, className, sb.toString(), false);
    }

    private CodeArtifact buildGateway(final String baseName, final List<BusinessRule> rules) {
        String className = baseName + "Gateway";
        StringBuilder sb = new StringBuilder();
        sb.append("public interface ").append(className).append(" {\n");
        if (rules.isEmpty()) {
            sb.append("    Object call(Object request);\n");
        } else {
            for (BusinessRule rule : rules) {
                String method = methodNameFromRule(rule);
                sb.append("    // ").append(rule.description()).append("\n");
                sb.append("    Object ").append(method).append("(Object request);\n");
            }
        }
        sb.append("}");
        return new CodeArtifact(
                artifactId(baseName, 4, ArtifactType.GATEWAY),
                ArtifactType.GATEWAY, 4, className, sb.toString(), false);
    }

    private CodeArtifact buildAssembler(final String baseName) {
        String className = baseName + "Assembler";
        String content = "public class " + className + " {\n"
                + "    public Object assemble(Object formData) {\n"
                + "        return null; // Mapping UI vers Domain a implementer\n"
                + "    }\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, 5, ArtifactType.ASSEMBLER),
                ArtifactType.ASSEMBLER, 5, className, content, false);
    }

    private CodeArtifact buildStrategy(final String baseName, final List<BusinessRule> rules) {
        String className = baseName + "Strategy";
        StringBuilder sb = new StringBuilder();
        sb.append("public interface ").append(className).append(" {\n");
        for (BusinessRule rule : rules) {
            sb.append("    // ").append(rule.description()).append("\n");
        }
        sb.append("    Object apply(Object context);\n");
        sb.append("}");
        return new CodeArtifact(
                artifactId(baseName, 5, ArtifactType.STRATEGY),
                ArtifactType.STRATEGY, 5, className, sb.toString(), false);
    }

    private CodeArtifact buildBridge(final String baseName, final List<BusinessRule> rules) {
        String className = baseName + "Bridge";
        long unknownCount = rules.stream()
                .filter(r -> r.extractionCandidate() == ExtractionCandidate.NONE).count();
        String content = "// BRIDGE TRANSITOIRE - " + unknownCount + " regles non classifiees\n"
                + "public class " + className + " {\n"
                + "    // Encapsule les methodes dont la responsabilite reste indeterminee.\n"
                + "    // A supprimer apres classification manuelle.\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, 1, ArtifactType.BRIDGE),
                ArtifactType.BRIDGE, 1, className, content, true);
    }

    private String extractBaseName(final String controllerRef) {
        if (controllerRef == null || controllerRef.isBlank()) {
            return "Default";
        }
        String fileName = controllerRef;
        int lastSlash = Math.max(controllerRef.lastIndexOf('/'), controllerRef.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            fileName = controllerRef.substring(lastSlash + 1);
        }
        if (fileName.endsWith(".java")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        if (fileName.endsWith("Controller")) {
            fileName = fileName.substring(0, fileName.length() - "Controller".length());
        }
        return fileName.isBlank() ? "Default" : fileName;
    }

    private String artifactId(final String baseName, final int lotNumber, final ArtifactType type) {
        return baseName + "-lot" + lotNumber + "-" + type.name().toLowerCase();
    }

    private String methodNameFromRule(final BusinessRule rule) {
        String desc = rule.description();
        int parenIndex = desc.indexOf('(');
        if (parenIndex < 0) {
            return "handle";
        }
        int spaceIndex = desc.lastIndexOf(' ', parenIndex);
        if (spaceIndex < 0) {
            return "handle";
        }
        String name = desc.substring(spaceIndex + 1, parenIndex);
        return name.isBlank() ? "handle" : name;
    }
}
