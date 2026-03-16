package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;

import java.util.ArrayList;
import java.util.List;

public final class RealCodeGenerationAdapter implements CodeGenerationPort {

    @Override
    public List<CodeArtifact> generate(final String controllerRef, final String javaContent) {
        String baseName = extractBaseName(controllerRef);
        List<CodeArtifact> artifacts = new ArrayList<>();

        artifacts.add(buildControllerSlim(baseName, 1));
        artifacts.add(buildViewModel(baseName, 2));
        artifacts.add(buildUseCase(baseName, 2));
        artifacts.add(buildPolicy(baseName, 3));
        artifacts.add(buildGateway(baseName, 4));
        artifacts.add(buildAssembler(baseName, 5));

        return List.copyOf(artifacts);
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

    private CodeArtifact buildControllerSlim(final String baseName, final int lotNumber) {
        String className = baseName + "SlimController";
        String content = "// BRIDGE TRANSITOIRE - à supprimer après migration complète\n"
                + "public class " + className + " {\n"
                + "    // TODO : implémenter - déléguer au ViewModel\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, lotNumber, ArtifactType.CONTROLLER_SLIM),
                ArtifactType.CONTROLLER_SLIM,
                lotNumber,
                className,
                content,
                true
        );
    }

    private CodeArtifact buildViewModel(final String baseName, final int lotNumber) {
        String className = baseName + "ViewModel";
        String content = "public class " + className + " {\n"
                + "    // TODO : implémenter - état de présentation\n"
                + "    private final javafx.beans.property.StringProperty status"
                + " = new javafx.beans.property.SimpleStringProperty();\n"
                + "    public javafx.beans.property.StringProperty statusProperty() { return status; }\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, lotNumber, ArtifactType.VIEW_MODEL),
                ArtifactType.VIEW_MODEL,
                lotNumber,
                className,
                content,
                false
        );
    }

    private CodeArtifact buildUseCase(final String baseName, final int lotNumber) {
        String className = baseName + "UseCase";
        String content = "public interface " + className + " {\n"
                + "    // TODO : implémenter - intention utilisateur\n"
                + "    void execute(String input);\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, lotNumber, ArtifactType.USE_CASE),
                ArtifactType.USE_CASE,
                lotNumber,
                className,
                content,
                false
        );
    }

    private CodeArtifact buildPolicy(final String baseName, final int lotNumber) {
        String className = baseName + "Policy";
        String content = "public class " + className + " {\n"
                + "    // TODO : implémenter - décision métier stable\n"
                + "    public boolean decide(Object context) { return false; }\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, lotNumber, ArtifactType.POLICY),
                ArtifactType.POLICY,
                lotNumber,
                className,
                content,
                false
        );
    }

    private CodeArtifact buildGateway(final String baseName, final int lotNumber) {
        String className = baseName + "Gateway";
        String content = "public interface " + className + " {\n"
                + "    // TODO : implémenter - accès externe (REST, fichier, matériel)\n"
                + "    Object call(Object request);\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, lotNumber, ArtifactType.GATEWAY),
                ArtifactType.GATEWAY,
                lotNumber,
                className,
                content,
                false
        );
    }

    private CodeArtifact buildAssembler(final String baseName, final int lotNumber) {
        String className = baseName + "Assembler";
        String content = "public class " + className + " {\n"
                + "    // TODO : implémenter - mapping UI -> Domain\n"
                + "    public Object assemble(Object formData) { return null; }\n"
                + "}";
        return new CodeArtifact(
                artifactId(baseName, lotNumber, ArtifactType.ASSEMBLER),
                ArtifactType.ASSEMBLER,
                lotNumber,
                className,
                content,
                false
        );
    }
}
