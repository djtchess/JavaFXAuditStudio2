package ff.ss.javaFxAuditStudio.configuration;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Configuration explicite de Flyway pour garantir l'execution des migrations
 * avant la validation du schema Hibernate (ddl-auto=validate).
 *
 * <p>Spring Boot 4.x n'etablit pas automatiquement la dependance
 * entityManagerFactory -> flyway sans l'autoconfiguration Flyway complete.
 * Le BeanFactoryPostProcessor ci-dessous declare cette dependance explicitement,
 * ce qui force Spring a creer et migrer via Flyway avant d'initialiser JPA.
 *
 * <p>Desactive via spring.flyway.enabled=false (profil test : H2 in-memory).
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayMigrationConfiguration {

    /**
     * Force entityManagerFactory a dependre du bean "flyway".
     * Doit etre static pour etre traite dans la phase BeanFactoryPostProcessor
     * avant l'instanciation des autres beans de cette classe @Configuration.
     */
    @Bean
    public static BeanFactoryPostProcessor flywayDependencyEnforcer() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }
            BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] existing = emf.getDependsOn();
            String[] updated = existing == null
                    ? new String[]{"flyway"}
                    : Arrays.copyOf(existing, existing.length + 1);
            if (existing != null) {
                updated[existing.length] = "flyway";
            }
            emf.setDependsOn(updated);
        };
    }

    @Bean
    public Flyway flyway(final DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
