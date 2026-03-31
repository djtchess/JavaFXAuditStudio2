package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ff.ss.javaFxAuditStudio.domain.ai.TaskType;

/**
 * Contrat commun de sortie structuree pour les fournisseurs LLM.
 */
final class StructuredOutputContract {

    private static final String BASE_SYSTEM_PROMPT =
            "Tu es un expert en migration JavaFX vers Spring Boot / architecture hexagonale.";
    private static final String STRICT_JSON_GUIDANCE =
            "Reponds uniquement avec un objet JSON valide, sans markdown, sans bloc de code, "
                    + "sans commentaire et sans texte autour.";
    private static final String SUGGESTIONS_SCHEMA_DESCRIPTION =
            "Le schema attendu est {\"suggestions\": {<cle>: <chaine>}}.";
    private static final String UNKNOWN_VALUE = "unknown";

    private StructuredOutputContract() {}

    static String strictSystemPrompt(final String controllerRef, final TaskType taskType) {
        String effectiveControllerRef = sanitize(controllerRef);
        String effectiveTaskType = (taskType != null) ? taskType.name() : UNKNOWN_VALUE;
        return BASE_SYSTEM_PROMPT
                + " Controleur de reference : " + effectiveControllerRef
                + ". Tache : " + effectiveTaskType
                + ". " + STRICT_JSON_GUIDANCE
                + " " + SUGGESTIONS_SCHEMA_DESCRIPTION;
    }

    static OpenAiHttpDtos.ResponseFormat openAiResponseFormat() {
        Map<String, Object> suggestionsSchema = new LinkedHashMap<>();
        suggestionsSchema.put("type", "object");
        suggestionsSchema.put("description", "Dictionnaire des suggestions retournees par le modele");
        suggestionsSchema.put("additionalProperties", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("suggestions", suggestionsSchema));
        schema.put("required", List.of("suggestions"));
        schema.put("additionalProperties", false);

        return new OpenAiHttpDtos.ResponseFormat(
                "json_schema",
                new OpenAiHttpDtos.JsonSchema("ai_enrichment_response", schema, true));
    }

    private static String sanitize(final String value) {
        return (value != null && !value.isBlank()) ? value.trim() : UNKNOWN_VALUE;
    }
}
