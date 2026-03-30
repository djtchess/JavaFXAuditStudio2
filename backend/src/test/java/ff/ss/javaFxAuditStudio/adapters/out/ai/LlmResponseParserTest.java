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
        String json = "{\"suggestions\": {\"ctrl\": \"SavePatientUseCase\"}}";

        Map<String, String> result = parser.parse(json, "ctrl", "req-test");

        assertThat(result).containsEntry("ctrl", "SavePatientUseCase");
    }

    @Test
    void should_extract_json_block_surrounded_by_text() {
        String response = "Voici ma suggestion :\n"
                + "{\"suggestions\": {\"ctrl\": \"LoadReportUseCase\"}}\n"
                + "En esperant que cela vous convienne.";

        Map<String, String> result = parser.parse(response, "ctrl", "req-test");

        assertThat(result).containsEntry("ctrl", "LoadReportUseCase");
    }

    @Test
    void should_parse_json_with_comments_from_llm_output() {
        String response = """
                {
                  // commentaire du modele
                  "suggestions": {
                    "USE_CASE": "package ff.example.usecase;\\npublic interface PatientUseCase {}"
                  }
                }
                """;

        Map<String, String> result = parser.parse(response, "ctrl", "req-test");

        assertThat(result).containsEntry(
                "USE_CASE",
                "package ff.example.usecase;\npublic interface PatientUseCase {}");
    }

    @Test
    void should_fallback_to_raw_text_when_no_json() {
        String rawText = "SavePatientUseCase";

        Map<String, String> result = parser.parse(rawText, "myController", "req-test");

        assertThat(result).containsEntry("myController", "SavePatientUseCase");
        assertThat(result).hasSize(1);
    }

    @Test
    void should_return_empty_string_for_blank_response() {
        Map<String, String> result = parser.parse("", "myController", "req-test");

        assertThat(result).containsEntry("myController", "");
    }

    @Test
    void should_fallback_to_raw_text_when_json_has_no_suggestions_key() {
        String json = "{\"result\": {\"ctrl\": \"SomeUseCase\"}}";

        Map<String, String> result = parser.parse(json, "ctrl", "req-test");

        assertThat(result).containsEntry("ctrl", json.strip());
    }

    @Test
    void should_return_empty_string_for_null_response() {
        Map<String, String> result = parser.parse(null, "myController", "req-test");

        assertThat(result).containsEntry("myController", "");
    }

    @Test
    void should_warn_and_fallback_on_truncated_json() {
        String truncated = "{\"suggestions\": {\"ctrl\": \"SaveP";

        Map<String, String> result = parser.parse(truncated, "ctrl", "req-123");

        assertThat(result).containsEntry("ctrl", truncated.strip());
    }

    @Test
    void should_fallback_to_raw_text_when_suggestion_values_are_not_textual() {
        String json = "{\"suggestions\": {\"ctrl\": 123}}";

        Map<String, String> result = parser.parse(json, "ctrl", "req-test");

        assertThat(result).containsEntry("ctrl", json.strip());
    }

    @Test
    void should_use_unknown_key_when_controller_ref_is_blank() {
        String rawText = "SavePatientUseCase";

        Map<String, String> result = parser.parse(rawText, "   ", "req-test");

        assertThat(result).containsEntry("unknown", "SavePatientUseCase");
    }
}
