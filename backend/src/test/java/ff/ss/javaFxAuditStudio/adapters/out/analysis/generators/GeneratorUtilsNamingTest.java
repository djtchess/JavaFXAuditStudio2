package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorUtilsNamingTest {

    @Test
    void methodNameFromRule_preservesExplicitHandlerName() {
        BusinessRule rule = rule("Methode handler onSave : responsabilite APPLICATION detectee");

        assertThat(GeneratorUtils.methodNameFromRule(rule)).isEqualTo("onSave");
    }

    @Test
    void cleanMethodName_removesTechnicalAffixesWhenRequested() {
        assertThat(GeneratorUtils.cleanMethodName("onSave")).isEqualTo("save");
        assertThat(GeneratorUtils.cleanMethodName("handleDeleteAction")).isEqualTo("delete");
    }

    private BusinessRule rule(final String description) {
        return new BusinessRule(
                "RG-001",
                description,
                "PatientController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
    }
}
