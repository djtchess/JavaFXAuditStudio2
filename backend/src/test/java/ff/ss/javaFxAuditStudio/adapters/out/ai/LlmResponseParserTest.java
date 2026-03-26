package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de {@link LlmResponseParser}.
 *
 * <p>Verifie l'extraction des suggestions depuis les reponses textuelles LLM.
 */
class LlmResponseParserTest {

    private LlmResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new LlmResponseParser(new ObjectMapper());
    }

    @Test
    void should_parse_valid_json_suggestions() {
        // JSON valide avec cle suggestions
        String json = "{\"suggestions\": {\"ctrl\": \"SavePatientUseCase\"}}";

        Map<String, String> result = parser.parse(json, "ctrl", "req-test");

        assertThat(result).containsEntry("ctrl", "SavePatientUseCase");
    }

    @Test
    void should_extract_json_block_surrounded_by_text() {
        // JSON valide encadre de texte brut (comportement LLM reel)
        String response = "Voici ma suggestion :\n"
                + "{\"suggestions\": {\"ctrl\": \"LoadReportUseCase\"}}\n"
                + "En esperant que cela vous convienne.";

        Map<String, String> result = parser.parse(response, "ctrl", "req-test");

        assertThat(result).containsEntry("ctrl", "LoadReportUseCase");
    }

    @Test
    void should_fallback_to_raw_text_when_no_json() {
        // Texte sans JSON — doit retourner le texte brut sous la cle controllerRef
        String rawText = "SavePatientUseCase";

        Map<String, String> result = parser.parse(rawText, "myController", "req-test");

        assertThat(result).containsEntry("myController", "SavePatientUseCase");
        assertThat(result).hasSize(1);
    }

    @Test
    void should_return_empty_string_for_blank_response() {
        // Texte vide — doit retourner map avec valeur vide sous controllerRef
        Map<String, String> result = parser.parse("", "myController", "req-test");

        assertThat(result).containsEntry("myController", "");
    }

    @Test
    void should_fallback_to_raw_text_when_json_has_no_suggestions_key() {
        // JSON valide mais sans cle "suggestions"
        String json = "{\"result\": {\"ctrl\": \"SomeUseCase\"}}";

        Map<String, String> result = parser.parse(json, "ctrl", "req-test");

        // Fallback : le JSON brut devient la valeur sous controllerRef
        assertThat(result).containsEntry("ctrl", json.strip());
    }

    @Test
    void should_return_empty_string_for_null_response() {
        // null doit etre traite comme vide
        Map<String, String> result = parser.parse(null, "myController", "req-test");

        assertThat(result).containsEntry("myController", "");
    }

    @Test
    void should_warn_and_fallback_on_truncated_json() {
        // JSON coupe au milieu — parse doit echouer et fallback sur le texte brut
        String truncated = "{\"suggestions\": {\"ctrl\": \"SaveP"; // JSON tronque

        Map<String, String> result = parser.parse(truncated, "ctrl", "req-123");

        // Le fallback retourne le texte brut sous controllerRef
        assertThat(result).containsEntry("ctrl", truncated.strip());
    }
}
