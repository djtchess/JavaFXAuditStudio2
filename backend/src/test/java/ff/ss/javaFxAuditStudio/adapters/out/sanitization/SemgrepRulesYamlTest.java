package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de chargement et de validite du fichier semgrep/sanitization-rules.yaml (AI-3).
 *
 * <p>Verifie que le fichier YAML des regles statiques est present sur le classpath
 * de test, qu'il est parsable et qu'il contient les 3 regles attendues avec les
 * champs obligatoires renseignes.
 */
class SemgrepRulesYamlTest {

    private static final String RULES_CLASSPATH = "/semgrep/sanitization-rules.yaml";

    private static final Set<String> EXPECTED_RULE_IDS = Set.of(
            "hardcoded-secret",
            "internal-ip-url",
            "hardcoded-url");

    @Test
    void should_find_rules_yaml_on_classpath() {
        try (InputStream stream = getClass().getResourceAsStream(RULES_CLASSPATH)) {
            assertThat(stream)
                    .as("Le fichier %s doit etre present sur le classpath de test", RULES_CLASSPATH)
                    .isNotNull();
        } catch (Exception e) {
            assertThat(e).as("Aucune exception lors de la lecture classpath").isNull();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_parse_valid_yaml_with_required_fields() {
        Map<String, Object> root = loadYaml();

        assertThat(root).containsKey("rules");
        List<Map<String, Object>> rules = (List<Map<String, Object>>) root.get("rules");
        assertThat(rules).isNotNull().isNotEmpty();

        for (Map<String, Object> rule : rules) {
            assertThat(rule.get("id"))
                    .as("Chaque regle doit avoir un id non null")
                    .isNotNull();
            assertThat(rule.get("severity"))
                    .as("Chaque regle doit avoir une severity non null")
                    .isNotNull();
            assertThat(rule.get("message"))
                    .as("Chaque regle doit avoir un message non null")
                    .isNotNull();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_contain_three_expected_rules() {
        Map<String, Object> root = loadYaml();
        List<Map<String, Object>> rules = (List<Map<String, Object>>) root.get("rules");

        assertThat(rules).hasSize(3);

        Set<String> actualIds = Set.of(
                (String) rules.get(0).get("id"),
                (String) rules.get(1).get("id"),
                (String) rules.get(2).get("id"));

        assertThat(actualIds).containsExactlyInAnyOrderElementsOf(EXPECTED_RULE_IDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_have_correct_severities() {
        Map<String, Object> root = loadYaml();
        List<Map<String, Object>> rules = (List<Map<String, Object>>) root.get("rules");

        for (Map<String, Object> rule : rules) {
            String id = (String) rule.get("id");
            String severity = (String) rule.get("severity");
            if ("hardcoded-secret".equals(id)) {
                assertThat(severity).isEqualTo("ERROR");
            } else {
                assertThat(severity).isEqualTo("WARNING");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml() {
        try (InputStream stream = getClass().getResourceAsStream(RULES_CLASSPATH)) {
            assertThat(stream).isNotNull();
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Yaml yaml = new Yaml();
            return yaml.load(content);
        } catch (Exception e) {
            throw new AssertionError("Impossible de charger le YAML : " + e.getMessage(), e);
        }
    }
}
