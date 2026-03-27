package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parse la reponse textuelle d'un LLM pour en extraire les suggestions.
 *
 * <p>Le LLM est invite a repondre avec un JSON contenant une cle {@code suggestions}
 * au format objet. Le parser accepte le texte autour du JSON, mais il rejette les
 * structures invalides ou tronquees de facon explicite avant de retomber sur le texte brut.
 */
final class LlmResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(LlmResponseParser.class);
    private static final String SUGGESTIONS_KEY = "suggestions";
    private static final String UNKNOWN_CONTROLLER_REF = "unknown";

    private final ObjectMapper objectMapper;

    LlmResponseParser(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Extrait la map suggestions depuis le texte LLM.
     *
     * @param responseText  texte brut retourne par le LLM
     * @param controllerRef reference du controleur (cle de fallback)
     * @param requestId     identifiant de la requete, utilise dans les logs WARN
     * @return map suggestions : cle = identifiant, valeur = suggestion
     */
    Map<String, String> parse(
            final String responseText,
            final String controllerRef,
            final String requestId) {
        String effectiveControllerRef = effectiveControllerRef(controllerRef);
        if (responseText == null || responseText.isBlank()) {
            return Map.of(effectiveControllerRef, "");
        }
        JsonBlockExtraction extraction = extractJsonBlock(responseText);
        if (extraction.candidate() == null) {
            logMissingJson(requestId, extraction.truncated());
            return fallback(effectiveControllerRef, responseText);
        }
        try {
            JsonNode root = objectMapper.readTree(extraction.candidate());
            Map<String, String> suggestions = extractSuggestions(root, requestId);
            if (!suggestions.isEmpty()) {
                return Map.copyOf(suggestions);
            }
            LOG.warn("LLM response sans suggestion exploitable [requestId={}, controllerRef={}]",
                    requestId, effectiveControllerRef);
        } catch (Exception e) {
            LOG.warn("LLM response JSON invalide [requestId={}, controllerRef={}]: {}",
                    requestId, effectiveControllerRef, e.getMessage());
        }
        return fallback(effectiveControllerRef, responseText);
    }

    private Map<String, String> extractSuggestions(final JsonNode root, final String requestId) {
        if (root == null || !root.isObject()) {
            LOG.warn("LLM response JSON racine invalide [requestId={}]", requestId);
            return Map.of();
        }
        JsonNode suggestionsNode = root.get(SUGGESTIONS_KEY);
        if (suggestionsNode == null || suggestionsNode.isNull()) {
            LOG.warn("LLM response sans cle '{}' [requestId={}]", SUGGESTIONS_KEY, requestId);
            return Map.of();
        }
        if (!suggestionsNode.isObject()) {
            LOG.warn("LLM response '{}' non objet [requestId={}]", SUGGESTIONS_KEY, requestId);
            return Map.of();
        }
        Map<String, String> suggestions = new LinkedHashMap<>();
        int invalidCount = 0;
        var fields = suggestionsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();
            if (key == null || key.isBlank() || valueNode == null || !valueNode.isTextual()) {
                invalidCount++;
                continue;
            }
            String value = valueNode.asText().strip();
            if (value.isBlank()) {
                invalidCount++;
                continue;
            }
            suggestions.put(key, value);
        }
        if (invalidCount > 0) {
            LOG.warn("LLM response suggestions partiellement invalides [requestId={}, invalidCount={}]",
                    requestId, invalidCount);
        }
        return suggestions;
    }

    private Map<String, String> fallback(final String controllerRef, final String responseText) {
        return Map.of(controllerRef, responseText.strip());
    }

    private void logMissingJson(final String requestId, final boolean truncated) {
        if (truncated) {
            LOG.warn("LLM response JSON tronque [requestId={}]", requestId);
        } else {
            LOG.warn("LLM response sans JSON exploitable [requestId={}]", requestId);
        }
    }

    private String effectiveControllerRef(final String controllerRef) {
        return (controllerRef == null || controllerRef.isBlank()) ? UNKNOWN_CONTROLLER_REF : controllerRef;
    }

    private JsonBlockExtraction extractJsonBlock(final String text) {
        int start = text.indexOf('{');
        if (start < 0) {
            return new JsonBlockExtraction(null, false);
        }
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        char quote = '\0';
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                } else if (current == quote) {
                    inString = false;
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                inString = true;
                quote = current;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return new JsonBlockExtraction(text.substring(start, index + 1), false);
                }
                if (depth < 0) {
                    break;
                }
            }
        }
        return new JsonBlockExtraction(null, true);
    }

    private record JsonBlockExtraction(String candidate, boolean truncated) {
    }
}
