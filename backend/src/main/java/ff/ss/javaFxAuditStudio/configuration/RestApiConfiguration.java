package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration REST et CORS de l'application.
 *
 * <p>Regles de securite appliquees (JAS-03) :
 * <ul>
 *   <li>Origine CORS strictement controllee via propriete externalisee {@code app.frontend.origin}.
 *       La valeur wildcard {@code *} est interdite en profil prod.</li>
 *   <li>Methodes HTTP autorisees couvrant les besoins actuels et futurs des flux d'ingestion.</li>
 *   <li>En-tetes entrants limites aux en-tetes applicatifs necessaires.</li>
 *   <li>En-tete {@code X-Correlation-Id} expose au client pour la tracabilite.</li>
 *   <li>Credentials non autorises par defaut (aucune session HTTP prevue a ce stade).</li>
 *   <li>Preflight mis en cache {@code app.cors.max-age} secondes pour reduire les requetes OPTIONS.</li>
 * </ul>
 */
@Configuration
public class RestApiConfiguration {

    @Bean
    public WebMvcConfigurer webMvcConfigurer(
            @Value("${app.frontend.origin}") final String frontendOrigin,
            @Value("${app.cors.max-age:3600}") final long corsMaxAge) {

        WebMvcConfigurer configurer;

        configurer = new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(final CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(frontendOrigin)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Accept", "X-Correlation-Id", "Authorization")
                        .exposedHeaders("X-Correlation-Id")
                        .allowCredentials(false)
                        .maxAge(corsMaxAge);
            }
        };
        return configurer;
    }
}
