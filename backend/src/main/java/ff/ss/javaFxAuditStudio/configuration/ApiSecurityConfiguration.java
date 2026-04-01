package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.in.rest.ApiAuthenticationEntryPoint;
import ff.ss.javaFxAuditStudio.adapters.in.rest.ApiKeyAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Configuration Spring Security minimale du backend.
 *
 * <p>Le mode "enabled" reste opt-in pour eviter de casser le frontend tant que
 * le bearer token n'est pas propage par tous les clients browser.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class ApiSecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ApiSecurityConfiguration.class);

    private final ApiSecurityProperties apiSecurityProperties;

    public ApiSecurityConfiguration(final ApiSecurityProperties apiSecurityProperties) {
        this.apiSecurityProperties = apiSecurityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            final ApiAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        validateConfiguration();
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(configurer -> configurer.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(apiKeyAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    authorize.requestMatchers(ApiSecurityEndpointCatalog.PUBLIC_ENDPOINT_PATTERNS).permitAll();
                    if (apiSecurityProperties.apiKeyEnabled()) {
                        authorize.requestMatchers(ApiSecurityEndpointCatalog.PROTECTED_ENDPOINT_PATTERNS).authenticated();
                    } else {
                        authorize.requestMatchers(ApiSecurityEndpointCatalog.PROTECTED_ENDPOINT_PATTERNS).permitAll();
                    }
                    authorize.anyRequest().permitAll();
                });
        return http.build();
    }

    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        return new ApiKeyAuthenticationFilter(apiSecurityProperties);
    }

    @Bean
    public ApiAuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return new ApiAuthenticationEntryPoint();
    }

    private void validateConfiguration() {
        if (apiSecurityProperties.apiKeyEnabled() && !apiSecurityProperties.hasApiKeyConfigured()) {
            throw new IllegalStateException("app.security.api-key-enabled=true requiert APP_SECURITY_API_KEY");
        }
        if (!apiSecurityProperties.apiKeyEnabled()) {
            LOG.warn("Socle securite minimal en mode observe: endpoints sensibles non bloques tant que APP_SECURITY_API_KEY_ENABLED=false");
        }
    }
}
