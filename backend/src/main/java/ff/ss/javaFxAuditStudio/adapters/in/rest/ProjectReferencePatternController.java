package ff.ss.javaFxAuditStudio.adapters.in.rest;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectReferencePatternCollectionResponse;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectReferencePatternRequest;
import ff.ss.javaFxAuditStudio.adapters.in.rest.dto.ProjectReferencePatternResponse;
import ff.ss.javaFxAuditStudio.application.ports.in.ListProjectReferencePatternsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RegisterProjectReferencePatternUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

@RestController
@RequestMapping("/api/v1/ai/reference-patterns")
public class ProjectReferencePatternController {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectReferencePatternController.class);

    private final RegisterProjectReferencePatternUseCase registerProjectReferencePatternUseCase;
    private final ListProjectReferencePatternsUseCase listProjectReferencePatternsUseCase;

    public ProjectReferencePatternController(
            final RegisterProjectReferencePatternUseCase registerProjectReferencePatternUseCase,
            final ListProjectReferencePatternsUseCase listProjectReferencePatternsUseCase) {
        this.registerProjectReferencePatternUseCase = Objects.requireNonNull(
                registerProjectReferencePatternUseCase,
                "registerProjectReferencePatternUseCase must not be null");
        this.listProjectReferencePatternsUseCase = Objects.requireNonNull(
                listProjectReferencePatternsUseCase,
                "listProjectReferencePatternsUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<ProjectReferencePatternResponse> register(@RequestBody final ProjectReferencePatternRequest request) {
        try {
            ProjectReferencePattern pattern = registerProjectReferencePatternUseCase.register(
                    request.artifactType(),
                    request.referenceName(),
                    request.content());
            LOG.debug("Pattern projet enregistre pour {}", pattern.artifactType());
            return ResponseEntity.ok(toResponse(pattern));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<ProjectReferencePatternCollectionResponse> list(
            @RequestParam(name = "artifactType", required = false) final String artifactType) {
        try {
            List<ProjectReferencePatternResponse> patterns = listProjectReferencePatternsUseCase.list(artifactType)
                    .stream()
                    .map(this::toResponse)
                    .toList();
            return ResponseEntity.ok(new ProjectReferencePatternCollectionResponse(patterns));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    private ProjectReferencePatternResponse toResponse(final ProjectReferencePattern pattern) {
        return new ProjectReferencePatternResponse(
                pattern.patternId(),
                pattern.artifactType(),
                pattern.referenceName(),
                pattern.content(),
                pattern.createdAt());
    }
}
