package ff.ss.javaFxAuditStudio.adapters.out.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTOs HTTP pour l'API Anthropic Claude Messages.
 * Package-private — usage interne uniquement.
 */
final class ClaudeHttpDtos {

    private ClaudeHttpDtos() {}

    record MessagesRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<Message> messages,
            @JsonProperty("temperature") double temperature) {}

    record Message(String role, String content) {}

    record MessagesResponse(
            List<ContentBlock> content,
            Usage usage) {}

    record ContentBlock(String type, String text) {}

    record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens) {}
}
