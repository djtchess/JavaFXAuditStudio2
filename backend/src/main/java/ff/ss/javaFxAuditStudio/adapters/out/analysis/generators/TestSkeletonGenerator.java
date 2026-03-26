package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * JAS-027 — Generateur de squelettes JUnit 5 pour les artefacts generes.
 *
 * <p>Produit une classe de test pour les regles USE_CASE, POLICY et GATEWAY
 * en utilisant les vrais types de parametres extraits par l'AST (JAS-006),
 * filtres des types JavaFX UI (JAS-008).
 */
@Component
public final class TestSkeletonGenerator implements ArtifactGenerator {

    @Override
    public CodeArtifact generate(
            final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Test";
        String useCaseClass = baseName + "UseCase";
        String gatewayClass = baseName + "Gateway";

        var sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);

        // Imports fixes
        sb.append("import org.junit.jupiter.api.BeforeEach;\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.extension.ExtendWith;\n");
        sb.append("import org.mockito.Mock;\n");
        sb.append("import org.mockito.junit.jupiter.MockitoExtension;\n");
        sb.append("import static org.assertj.core.api.Assertions.assertThat;\n");
        sb.append("import static org.mockito.Mockito.verify;\n\n");

        // Hints d'imports pour les types metier
        List<String> typeHints = GeneratorUtils.collectTypeHints(rules);
        if (!typeHints.isEmpty()) {
            sb.append("// Imports a ajuster selon le package reel :\n");
            for (String type : typeHints) {
                sb.append("// import ").append(type).append(";\n");
            }
            sb.append("\n");
        }

        sb.append("@ExtendWith(MockitoExtension.class)\n");
        sb.append("class ").append(className).append(" {\n\n");

        // @Mock pour Gateway si des regles GATEWAY existent
        boolean hasGateway = rules.stream()
                .anyMatch(r -> r.extractionCandidate() == ExtractionCandidate.GATEWAY);
        if (hasGateway) {
            sb.append("    @Mock\n");
            sb.append("    private ").append(gatewayClass).append(" gateway;\n\n");
        }

        sb.append("    private ").append(useCaseClass).append(" useCase;\n\n");
        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        sb.append("        // TODO: instancier useCase\n");
        sb.append("    }\n\n");

        // Methodes de test pour USE_CASE et POLICY
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            if (rule.extractionCandidate() != ExtractionCandidate.USE_CASE
                    && rule.extractionCandidate() != ExtractionCandidate.POLICY) {
                continue;
            }
            String method = GeneratorUtils.cleanMethodName(GeneratorUtils.methodNameFromRule(rule));
            if (!seen.add(method)) {
                continue;
            }
            appendTestMethod(sb, method, rule, useCaseClass);
        }

        sb.append("}\n");
        return GeneratorUtils.artifact(
                baseName, 2, ArtifactType.TEST_SKELETON, className, sb.toString(), false);
    }

    private void appendTestMethod(
            final StringBuilder sb,
            final String method,
            final BusinessRule rule,
            final String useCaseClass) {
        sb.append("    /** ").append(rule.description()).append(" */\n");
        sb.append("    @Test\n");

        String returnType = GeneratorUtils.buildReturnType(rule);
        boolean isBoolean = "boolean".equals(returnType) || "Boolean".equals(returnType);
        boolean isVoid = "void".equals(returnType);

        // Nom du test : method_shouldXxx
        String testName = isBoolean
                ? method + "_shouldReturnExpected"
                : method + "_shouldExecuteSuccessfully";
        sb.append("    void ").append(testName).append("() {\n");

        // given
        sb.append("        // given\n");
        List<MethodParameter> params = rule.hasSignature()
                ? JavaFxUiTypeFilter.filterForDomain(rule.signature()).parameters()
                : List.of();
        for (MethodParameter param : params) {
            sb.append("        final ").append(param.type()).append(" ")
              .append(param.name()).append(" = null; // TODO: instancier ")
              .append(param.type()).append("\n");
        }
        if (params.isEmpty()) {
            sb.append("        // (aucun parametre)\n");
        }

        // when
        sb.append("        // when\n");
        String argList = params.stream()
                .map(MethodParameter::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        if (isVoid) {
            sb.append("        useCase.").append(method).append("(").append(argList).append(");\n");
        } else {
            sb.append("        ").append(returnType).append(" result = useCase.")
              .append(method).append("(").append(argList).append(");\n");
        }

        // then
        sb.append("        // then\n");
        if (isVoid) {
            sb.append("        // TODO: verify(gateway).xxx(...)\n");
        } else if (isBoolean) {
            sb.append("        assertThat(result).isFalse(); // TODO: cas metier a verifier\n");
        } else {
            sb.append("        assertThat(result).isNotNull(); // TODO: assertions metier\n");
        }

        sb.append("    }\n\n");
    }
}
