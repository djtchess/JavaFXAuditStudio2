package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ff.ss.javaFxAuditStudio.adapters.out.ai.PayloadHasher;
import ff.ss.javaFxAuditStudio.adapters.out.persistence.JpaLlmAuditAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.persistence.LlmAuditRepository;
import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;

/**
 * Configuration d'assemblage du sous-systeme d'audit LLM (JAS-029).
 *
 * <p>Seule classe autorisee a instancier les beans d'audit LLM.
 */
@Configuration
@EnableConfigurationProperties(LlmAuditProperties.class)
public class LlmAuditConfiguration {

    /**
     * Bean de calcul de hash SHA-256 pour les payloads sanitises.
     */
    @Bean
    public PayloadHasher payloadHasher() {
        return new PayloadHasher();
    }

    /**
     * Bean du port sortant d'audit LLM.
     *
     * @param repo repository Spring Data JPA
     * @return adaptateur implementant LlmAuditPort
     */
    @Bean
    public LlmAuditPort llmAuditPort(final LlmAuditRepository repo) {
        return new JpaLlmAuditAdapter(repo);
    }
}
