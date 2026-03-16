package ff.ss.javaFxAuditStudio.adapters.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactsResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ArtifactsResponse.CodeArtifactDto;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.GenerationResult;

@Component
public class ArtifactsResponseMapper {

    public ArtifactsResponse toResponse(final GenerationResult result) {
        return new ArtifactsResponse(
                result.controllerRef(),
                mapArtifacts(result.artifacts()),
                result.warnings());
    }

    private List<CodeArtifactDto> mapArtifacts(final List<CodeArtifact> artifacts) {
        return artifacts.stream()
                .map(a -> new CodeArtifactDto(
                        a.artifactId(),
                        a.type().name(),
                        a.lotNumber(),
                        a.className(),
                        a.transitionalBridge()))
                .toList();
    }
}
