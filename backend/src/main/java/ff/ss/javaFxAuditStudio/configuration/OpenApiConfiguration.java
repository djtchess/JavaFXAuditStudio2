package ff.ss.javaFxAuditStudio.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI 3.1 pour JavaFX Audit Studio (JAS-028).
 *
 * <p>Expose la documentation API sur /api-docs (JSON) et /swagger-ui/index.html (UI).
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI javaFxAuditStudioOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JavaFX Audit Studio API")
                        .description("""
                                API REST de JavaFX Audit Studio — outil d'analyse et de migration de controllers JavaFX.

                                ## Fonctionnalites principales
                                - **Analyse** : soumission de fichiers source JavaFX, extraction de regles metier
                                - **Cartographie** : inventaire des composants FXML et handlers
                                - **Classification** : categorisation des regles (USE_CASE, GATEWAY, POLICY, VIEW_MODEL, LIFECYCLE)
                                - **Generation** : production d'artefacts de migration (UseCase, Gateway, ViewModel, Policy, tests)
                                - **Reclassification** : correction manuelle des categories avec audit trail
                                - **Enrichissement IA** : suggestions de nommage via Claude ou OpenAI
                                - **Tableau de bord** : metriques de progression par projet

                                ## Flux principal
                                1. `POST /api/v1/analysis/sessions` — creer une session d'analyse
                                2. `POST /api/v1/analysis/sessions/{sessionId}/run` — executer le pipeline complet
                                3. `GET /api/v1/analysis/sessions/{sessionId}/classification` — consulter les regles
                                4. `GET /api/v1/analysis/sessions/{sessionId}/artifacts` — recuperer les artefacts generes
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("JavaFX Audit Studio")
                                .email("support@jas.internal"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://jas.internal/license")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Developpement local"),
                        new Server().url("https://jas.internal").description("Production")))
                .tags(List.of(
                        new Tag().name("Analyse").description("Sessions d'analyse JavaFX et orchestration"),
                        new Tag().name("Cartographie").description("Inventaire FXML et bindings handlers"),
                        new Tag().name("Classification").description("Regles metier et categories"),
                        new Tag().name("Artefacts").description("Generation et export de code migre"),
                        new Tag().name("Reclassification").description("Correction manuelle et audit trail"),
                        new Tag().name("Enrichissement IA").description("Suggestions de nommage par LLM"),
                        new Tag().name("Tableau de bord").description("Metriques de progression par projet"),
                        new Tag().name("Workbench").description("Vue globale du workbench"),
                        new Tag().name("Audit LLM").description("Journal des appels LLM")
                ));
    }
}
