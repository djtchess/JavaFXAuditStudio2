package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ff.ss.javaFxAuditStudio.application.ports.in.ListProjectReferencePatternsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RegisterProjectReferencePatternUseCase;
import ff.ss.javaFxAuditStudio.domain.ai.ProjectReferencePattern;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ProjectReferencePatternControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private RegisterProjectReferencePatternUseCase registerProjectReferencePatternUseCase;

    @MockitoBean
    private ListProjectReferencePatternsUseCase listProjectReferencePatternsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void should_register_reference_pattern() throws Exception {
        when(registerProjectReferencePatternUseCase.register(anyString(), anyString(), anyString()))
                .thenReturn(pattern("USE_CASE", "ReferenceUseCase"));

        mockMvc.perform(post("/api/v1/ai/reference-patterns")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"USE_CASE","referenceName":"ReferenceUseCase","content":"code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactType").value("USE_CASE"))
                .andExpect(jsonPath("$.referenceName").value("ReferenceUseCase"));
    }

    @Test
    void should_list_reference_patterns_for_artifact_type() throws Exception {
        when(listProjectReferencePatternsUseCase.list("USE_CASE"))
                .thenReturn(List.of(pattern("USE_CASE", "ReferenceUseCase")));

        mockMvc.perform(get("/api/v1/ai/reference-patterns").param("artifactType", "USE_CASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patterns[0].artifactType").value("USE_CASE"))
                .andExpect(jsonPath("$.patterns[0].referenceName").value("ReferenceUseCase"));
    }

    @Test
    void should_return_400_when_reference_pattern_request_is_invalid() throws Exception {
        when(registerProjectReferencePatternUseCase.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("invalid"));

        mockMvc.perform(post("/api/v1/ai/reference-patterns")
                        .contentType("application/json")
                        .content("""
                                {"artifactType":"bad","referenceName":"Ref","content":"code"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private static ProjectReferencePattern pattern(final String artifactType, final String referenceName) {
        return new ProjectReferencePattern(
                "pattern-1",
                artifactType,
                referenceName,
                "package ff.example;\npublic interface " + referenceName + " {}",
                Instant.parse("2026-03-27T07:00:00Z"));
    }
}
