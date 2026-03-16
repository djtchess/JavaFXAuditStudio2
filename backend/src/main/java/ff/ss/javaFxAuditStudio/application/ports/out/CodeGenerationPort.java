package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;

import java.util.List;

public interface CodeGenerationPort {

    List<CodeArtifact> generate(String controllerRef, String javaContent);
}
