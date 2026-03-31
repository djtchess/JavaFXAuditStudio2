package ff.ss.javaFxAuditStudio.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("ci-postgres")
@EnabledIfEnvironmentVariable(named = "CI_POSTGRES_ENABLED", matches = "true")
class PostgresServiceContainerIT {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    PostgresServiceContainerIT(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void bootsAgainstPostgresAndAppliesMigrations() {
        Integer ping = jdbcTemplate.queryForObject("select 1", Integer.class);
        String databaseName = jdbcTemplate.queryForObject("select current_database()", String.class);
        Integer analysisSessionTables = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.tables
                        where table_schema = current_schema()
                          and table_name = 'analysis_session'
                        """,
                Integer.class);
        Integer migrationRows = jdbcTemplate.queryForObject("select count(*) from flyway_schema_history", Integer.class);

        assertThat(ping).isEqualTo(1);
        assertThat(databaseName).isEqualTo("javafx_audit");
        assertThat(analysisSessionTables).isEqualTo(1);
        assertThat(migrationRows).isGreaterThan(0);
    }
}
