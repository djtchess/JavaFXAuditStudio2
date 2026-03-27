package ff.ss.javaFxAuditStudio.adapters.out.observability;

import java.util.Objects;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import ff.ss.javaFxAuditStudio.configuration.AiEnrichmentProperties;

/**
 * Health indicator qui expose l'etat de configuration de l'enrichissement IA.
 */
public final class LlmEnrichmentHealthIndicator implements HealthIndicator {

    private final AiEnrichmentProperties properties;

    public LlmEnrichmentHealthIndicator(final AiEnrichmentProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public Health health() {
        boolean enabled = properties.enabled();
        boolean supportedProvider = properties.isSupportedProvider();
        boolean credentialRequired = properties.isCredentialRequired();
        boolean credentialPresent = !credentialRequired || hasCredential();
        Health.Builder builder;

        if (!enabled) {
            builder = Health.up().withDetail("mode", "disabled");
        } else if (!supportedProvider) {
            builder = Health.down().withDetail("mode", "unsupported-provider");
        } else if (!credentialPresent) {
            builder = Health.down().withDetail("mode", "missing-credential");
        } else {
            builder = Health.up().withDetail("mode", "ready");
        }

        return builder
                .withDetail("enabled", enabled)
                .withDetail("provider", properties.provider())
                .withDetail("credentialRequired", credentialRequired)
                .withDetail("credentialPresent", credentialPresent)
                .withDetail("auditEnabled", properties.auditEnabled())
                .withDetail("timeoutMs", properties.effectiveTimeoutMs())
                .build();
    }

    private boolean hasCredential() {
        return properties.activeApiKey() != null && !properties.activeApiKey().isBlank();
    }
}
