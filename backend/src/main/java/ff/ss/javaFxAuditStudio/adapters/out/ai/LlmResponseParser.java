package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parse la reponse textuelle d'un LLM pour en extraire les suggestions.
 *
 * <p>Le LLM est invite a repondre avec un JSON contenant une cle {@code suggestions}
 * au format objet. Le parser est volontairement fail-closed : toute reponse non conforme
 * retourne une map vide afin de laisser l'adaptateur degrader l'appel.
 */
final class LlmResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(LlmResponseParser.class);
    private static final String SUGGESTIONS_KEY = "suggestions";
    private static final String UNKNOWN_CONTROLLER_REF = "unknown";

    private final ObjectMapper objectMapper;

    LlmResponseParser(final ObjectMapper objectMapper) {
        this.objectMapper = configureObjectMapper(objectMapper);
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
            LOG.warn("LLM response vide [requestId={}, controllerRef={}]", requestId, effectiveControllerRef);
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(responseText.strip());
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
        return Map.of();
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

    private String effectiveControllerRef(final String controllerRef) {
        return (controllerRef == null || controllerRef.isBlank()) ? UNKNOWN_CONTROLLER_REF : controllerRef;
    }

    private ObjectMapper configureObjectMapper(final ObjectMapper sourceMapper) {
        ObjectMapper configuredMapper = Objects.requireNonNull(sourceMapper, "objectMapper must not be null").copy();
        configuredMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
        return configuredMapper;
    }
}
