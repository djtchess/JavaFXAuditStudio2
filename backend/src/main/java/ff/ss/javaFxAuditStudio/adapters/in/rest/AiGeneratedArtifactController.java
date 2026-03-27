package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiArtifactCoherenceResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiArtifactRefineRequest;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiCodeGenerationResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiGeneratedArtifactCollectionResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.AiGeneratedArtifactResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.ExportAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ListAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RefineAiArtifactUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.VerifyAiArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactCoherenceResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactRefinementCommand;
import ff.ss.javaFxAuditStudio.domain.ai.AiArtifactZipExport;
import ff.ss.javaFxAuditStudio.domain.ai.AiCodeGenerationResult;
import ff.ss.javaFxAuditStudio.domain.ai.AiGeneratedArtifact;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;

@RestController
@RequestMapping("/api/v1/analyses")
public class AiGeneratedArtifactController {

    private final ListAiGeneratedArtifactsUseCase listAiGeneratedArtifactsUseCase;
    private final RefineAiArtifactUseCase refineAiArtifactUseCase;
    private final VerifyAiArtifactCoherenceUseCase verifyAiArtifactCoherenceUseCase;
    private final ExportAiGeneratedArtifactsUseCase exportAiGeneratedArtifactsUseCase;

    public AiGeneratedArtifactController(
            final ListAiGeneratedArtifactsUseCase listAiGeneratedArtifactsUseCase,
            final RefineAiArtifactUseCase refineAiArtifactUseCase,
            final VerifyAiArtifactCoherenceUseCase verifyAiArtifactCoherenceUseCase,
            final ExportAiGeneratedArtifactsUseCase exportAiGeneratedArtifactsUseCase) {
        this.listAiGeneratedArtifactsUseCase = Objects.requireNonNull(
                listAiGeneratedArtifactsUseCase,
                "listAiGeneratedArtifactsUseCase must not be null");
        this.refineAiArtifactUseCase = Objects.requireNonNull(
                refineAiArtifactUseCase,
                "refineAiArtifactUseCase must not be null");
        this.verifyAiArtifactCoherenceUseCase = Objects.requireNonNull(
                verifyAiArtifactCoherenceUseCase,
                "verifyAiArtifactCoherenceUseCase must not be null");
        this.exportAiGeneratedArtifactsUseCase = Objects.requireNonNull(
                exportAiGeneratedArtifactsUseCase,
                "exportAiGeneratedArtifactsUseCase must not be null");
    }

    @GetMapping("/{sessionId}/artifacts/ai")
    public ResponseEntity<AiGeneratedArtifactCollectionResponse> listLatest(
            @PathVariable final String sessionId) {
        try {
            List<AiGeneratedArtifact> artifacts = listAiGeneratedArtifactsUseCase.listLatest(sessionId);
            return ResponseEntity.ok(toCollectionResponse(sessionId, artifacts));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{sessionId}/artifacts/ai/{artifactType}/versions")
    public ResponseEntity<AiGeneratedArtifactCollectionResponse> listVersions(
            @PathVariable final String sessionId,
            @PathVariable final String artifactType) {
        try {
            String normalizedArtifactType = normalizeArtifactType(artifactType);
            List<AiGeneratedArtifact> artifacts = listAiGeneratedArtifactsUseCase.listVersions(sessionId, normalizedArtifactType);
            return ResponseEntity.ok(toCollectionResponse(sessionId, artifacts));
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionId}/artifacts/ai/refine")
    public ResponseEntity<AiCodeGenerationResponse> refine(
            @PathVariable final String sessionId,
            @RequestBody final AiArtifactRefineRequest request) {
        return refineInternal(sessionId, request);
    }

    @PostMapping("/{sessionId}/generate/ai/refine")
    public ResponseEntity<AiCodeGenerationResponse> refineAlias(
            @PathVariable final String sessionId,
            @RequestBody final AiArtifactRefineRequest request) {
        return refineInternal(sessionId, request);
    }

    private ResponseEntity<AiCodeGenerationResponse> refineInternal(
            final String sessionId,
            final AiArtifactRefineRequest request) {
        try {
            AiArtifactRefinementCommand command = toCommand(request);
            AiCodeGenerationResult result = refineAiArtifactUseCase.refine(
                    sessionId,
                    command);
            return ResponseEntity.ok(toGenerationResponse(result));
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionId}/artifacts/ai/coherence")
    public ResponseEntity<AiArtifactCoherenceResponse> verifyCoherence(
            @PathVariable final String sessionId) {
        try {
            AiArtifactCoherenceResult result = verifyAiArtifactCoherenceUseCase.verify(sessionId);
            return ResponseEntity.ok(toCoherenceResponse(result));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{sessionId}/artifacts/ai/export")
    public ResponseEntity<byte[]> exportZip(@PathVariable final String sessionId) {
        return exportZipInternal(sessionId);
    }

    @GetMapping("/{sessionId}/generate/ai/export/zip")
    public ResponseEntity<byte[]> exportZipAlias(@PathVariable final String sessionId) {
        return exportZipInternal(sessionId);
    }

    @PostMapping("/{sessionId}/generate/ai/export/zip")
    public ResponseEntity<byte[]> exportZipPostAlias(@PathVariable final String sessionId) {
        return exportZipInternal(sessionId);
    }

    private ResponseEntity<byte[]> exportZipInternal(final String sessionId) {
        try {
            AiArtifactZipExport export = exportAiGeneratedArtifactsUseCase.export(sessionId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDisposition(ContentDisposition.attachment().filename(export.fileName()).build());
            headers.setContentLength(export.content().length);
            headers.set("X-AI-Artifact-Count", Integer.toString(export.artifactCount()));
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(export.content());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.noContent().build();
        }
    }

    private AiGeneratedArtifactCollectionResponse toCollectionResponse(
            final String sessionId,
            final List<AiGeneratedArtifact> artifacts) {
        return new AiGeneratedArtifactCollectionResponse(
                sessionId,
                artifacts.stream().map(this::toResponse).toList());
    }

    private AiGeneratedArtifactResponse toResponse(final AiGeneratedArtifact artifact) {
        return new AiGeneratedArtifactResponse(
                artifact.artifactType(),
                artifact.className(),
                artifact.content(),
                artifact.versionNumber(),
                artifact.parentVersionId(),
                artifact.requestId(),
                artifact.provider().value(),
                artifact.originTask().name(),
                artifact.createdAt());
    }

    private AiCodeGenerationResponse toGenerationResponse(final AiCodeGenerationResult result) {
        return new AiCodeGenerationResponse(
                result.requestId(),
                result.degraded(),
                result.degradationReason(),
                result.generatedClasses(),
                result.tokensUsed(),
                result.provider().value());
    }

    private AiArtifactCoherenceResponse toCoherenceResponse(final AiArtifactCoherenceResult result) {
        return new AiArtifactCoherenceResponse(
                result.requestId(),
                result.degraded(),
                result.degradationReason(),
                result.summary(),
                Map.copyOf(result.artifactFindings()),
                result.globalFindings(),
                result.tokensUsed(),
                result.provider().value());
    }

    private AiArtifactRefinementCommand toCommand(final AiArtifactRefineRequest request) {
        if (request == null) {
            throw new IllegalStateException("request must not be null");
        }
        try {
            return new AiArtifactRefinementCommand(
                    normalizeArtifactType(request.artifactType()),
                    request.instruction(),
                    request.previousCode());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("invalid refine request", exception);
        }
    }

    private String normalizeArtifactType(final String artifactType) {
        try {
            return ArtifactType.valueOf(artifactType.trim().toUpperCase()).name();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("invalid artifact type", exception);
        }
    }
}
