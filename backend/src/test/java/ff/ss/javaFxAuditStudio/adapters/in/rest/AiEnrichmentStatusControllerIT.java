package ff.ss.javaFxAuditStudio.adapters.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc pour AiEnrichmentStatusController (JAS-022).
 * Utilise @SpringBootTest(MOCK) + profil "test" (H2, Flyway desactive).
 *
 * <p>Le cas "credential absent" est isole dans une classe imbriquee car
 * {@code @TestPropertySource} est une annotation de niveau classe uniquement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AiEnrichmentStatusControllerIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void should_return_status_disabled_by_default() throws Exception {
        // Le profil test herite de application.properties qui a ai.enrichment.enabled=false
        mockMvc.perform(get("/api/v1/ai-enrichment/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void should_return_status_with_provider_name() throws Exception {
        // Le profil test herite de application.properties qui a ai.enrichment.provider=claude-code
        mockMvc.perform(get("/api/v1/ai-enrichment/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("claude-code"));
    }

    @Test
    void should_not_expose_api_key_value() throws Exception {
        // La reponse ne doit JAMAIS contenir de champ "apiKey" ou "claudeApiKey"
        mockMvc.perform(get("/api/v1/ai-enrichment/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.claudeApiKey").doesNotExist())
                .andExpect(jsonPath("$.openaiApiKey").doesNotExist())
                .andExpect(jsonPath("$.credentialPresent").exists());
    }

    /**
     * Cas isole : cles vides explicitement forcees via @TestPropertySource.
     * Necessite une classe imbriquee car @TestPropertySource n'est applicable qu'au niveau classe.
     */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "ai.enrichment.claude-code.api-key=",
            "ai.enrichment.openai.api-key="
    })
    class WhenApiKeysAreBlank {

        @Autowired
        private WebApplicationContext wac;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        }

        @Test
        void should_return_credential_absent_when_key_blank() throws Exception {
            // Cles vides => credentialPresent doit etre false
            mockMvc.perform(get("/api/v1/ai-enrichment/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credentialPresent").value(false));
        }
    }
}
