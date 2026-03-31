package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTOs HTTP pour l'API OpenAI Chat Completions.
 * Package-private — usage interne uniquement.
 */
final class OpenAiHttpDtos {

    private OpenAiHttpDtos() {}

    record ChatRequest(
            String model,
            List<Message> messages,
            @JsonProperty("max_tokens") int maxTokens,
            @JsonProperty("temperature") double temperature,
            @JsonProperty("response_format") ResponseFormat responseFormat) {}

    record Message(String role, String content) {}

    record ResponseFormat(
            String type,
            @JsonProperty("json_schema") JsonSchema jsonSchema) {}

    record JsonSchema(
            String name,
            Map<String, Object> schema,
            boolean strict) {}

    record ChatResponse(
            List<Choice> choices,
            Usage usage) {}

    record Choice(Message message) {}

    record Usage(@JsonProperty("total_tokens") int totalTokens) {}
}
