package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Parse la reponse textuelle d'un LLM pour en extraire les suggestions.
 *
 * <p>Le LLM est invite a repondre avec le JSON :
 * {@code {"suggestions": {"controllerRef": "valeur"}}}
 *
 * <p>Si le JSON est invalide ou absent, le texte brut est retourne
 * comme suggestion sous la cle {@code controllerRef}.
 */
final class LlmResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(LlmResponseParser.class);
    private static final TypeReference<Map<String, Map<String, String>>> SUGGESTIONS_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    LlmResponseParser(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        if (responseText == null || responseText.isBlank()) {
            return Map.of(controllerRef, "");
        }
        // Tenter d'extraire le premier bloc JSON du texte (le LLM peut ajouter du texte autour)
        String jsonCandidate = extractJsonBlock(responseText);
        if (jsonCandidate != null) {
            try {
                Map<String, Map<String, String>> parsed =
                        objectMapper.readValue(jsonCandidate, SUGGESTIONS_TYPE);
                Map<String, String> suggestions = parsed.get("suggestions");
                if (suggestions != null && !suggestions.isEmpty()) {
                    return Map.copyOf(suggestions);
                }
                // JSON parse avec succes mais cle "suggestions" absente
                LOG.warn("LLM response sans cle 'suggestions', fallback texte brut [requestId={}]",
                        requestId);
            } catch (Exception e) {
                // JSON detecte mais parse echoue : reponse tronquee
                LOG.warn("LLM response JSON tronque, fallback texte brut [requestId={}] : {}",
                        requestId, e.getMessage());
            }
        } else {
            // Aucun delimiteur JSON trouve : texte brut pur
            LOG.warn("LLM response texte brut sans JSON, fallback [requestId={}]", requestId);
        }
        // Fallback : texte brut entier comme suggestion
        return Map.of(controllerRef, responseText.strip());
    }

    private String extractJsonBlock(final String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }
}
