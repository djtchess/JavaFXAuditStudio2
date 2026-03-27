package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * JAS-027 - Generateur de squelettes JUnit 5 pour les artefacts generes.
 *
 * <p>Produit une classe de test plus exploitable:
 * <ul>
 *   <li>un double de use case compileable sans Mockito, via une implementation anonyme;</li>
 *   <li>une instance concrete de policy quand des regles POLICY existent;</li>
 *   <li>des valeurs d'entree plus riches pour les types simples connus;</li>
 *   <li>des assertions plus expressives que de simples stubs.</li>
 * </ul>
 *
 * <p>Les types JavaFX UI continuent d'etre filtres hors du domaine via
 * {@link JavaFxUiTypeFilter}.
 */
@Component
public final class TestSkeletonGenerator implements ArtifactGenerator {

    @Override
    public CodeArtifact generate(
            final String baseName, final String pkg, final List<BusinessRule> rules) {
        String className = baseName + "Test";
        String useCaseClass = baseName + "UseCase";
        String policyClass = baseName + "Policy";
        boolean hasUseCase = hasCandidate(rules, ExtractionCandidate.USE_CASE);
        boolean hasPolicy = hasCandidate(rules, ExtractionCandidate.POLICY);

        StringBuilder sb = new StringBuilder();
        GeneratorUtils.addPackage(sb, pkg);
        sb.append("import org.junit.jupiter.api.BeforeEach;\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import static org.assertj.core.api.Assertions.assertThat;\n");
        sb.append("import static org.assertj.core.api.Assertions.assertThatCode;\n\n");

        List<String> typeHints = GeneratorUtils.collectTypeHints(rules);
        if (!typeHints.isEmpty()) {
            sb.append("// Imports a ajuster selon le package reel :\n");
            for (String type : typeHints) {
                sb.append("// import ").append(type).append(";\n");
            }
            sb.append("\n");
        }

        sb.append("class ").append(className).append(" {\n\n");
        if (hasUseCase) {
            sb.append("    private ").append(useCaseClass).append(" useCase;\n\n");
        }
        if (hasPolicy) {
            sb.append("    private ").append(policyClass).append(" policy;\n\n");
        }

        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        if (hasUseCase) {
            appendUseCaseDouble(sb, rules, useCaseClass);
        }
        if (hasPolicy) {
            sb.append("        policy = new ").append(policyClass).append("();\n");
        }
        if (!hasUseCase && !hasPolicy) {
            sb.append("        // TODO: initialiser les doubles necessaires\n");
        }
        sb.append("    }\n\n");

        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            if (rule.extractionCandidate() == ExtractionCandidate.USE_CASE
                    || rule.extractionCandidate() == ExtractionCandidate.POLICY) {
                String method = GeneratorUtils.cleanMethodName(GeneratorUtils.methodNameFromRule(rule));
                if (seen.add(method)) {
                    appendTestMethod(sb, rule, method);
                }
            }
        }

        sb.append("}\n");
        return GeneratorUtils.artifact(
                baseName, 2, ArtifactType.TEST_SKELETON, className, sb.toString(), false);
    }

    private void appendUseCaseDouble(
            final StringBuilder sb,
            final List<BusinessRule> rules,
            final String useCaseClass) {
        sb.append("        useCase = new ").append(useCaseClass).append("() {\n");
        var seen = new LinkedHashSet<String>();
        for (BusinessRule rule : rules) {
            if (rule.extractionCandidate() == ExtractionCandidate.USE_CASE) {
                String method = GeneratorUtils.cleanMethodName(GeneratorUtils.methodNameFromRule(rule));
                if (seen.add(method)) {
                    appendUseCaseMethod(sb, rule, method);
                }
            }
        }
        sb.append("        };\n");
    }

    private void appendUseCaseMethod(
            final StringBuilder sb,
            final BusinessRule rule,
            final String method) {
        String returnType = GeneratorUtils.buildReturnType(rule);
        String params = buildDomainParameterList(rule);
        sb.append("            @Override\n");
        sb.append("            public ").append(returnType).append(" ").append(method)
                .append("(").append(params).append(") {\n");
        if ("void".equals(returnType)) {
            sb.append("            }\n");
        } else {
            sb.append("                return ").append(GeneratorUtils.defaultValueExpression(returnType))
                    .append(";\n");
            sb.append("            }\n");
        }
    }

    private void appendTestMethod(
            final StringBuilder sb,
            final BusinessRule rule,
            final String method) {
        String target = rule.extractionCandidate() == ExtractionCandidate.POLICY ? "policy" : "useCase";
        String returnType = effectiveReturnType(rule);
        List<MethodParameter> params = effectiveParameters(rule);
        String call = target + "." + method + "(" + buildArgumentList(params) + ")";

        sb.append("    /** ").append(rule.description()).append(" */\n");
        sb.append("    @Test\n");
        sb.append("    void ").append(method).append("_shouldBehaveAsExpected() {\n");
        appendParameterDeclarations(sb, params);
        if ("void".equals(returnType)) {
            sb.append("        assertThatCode(() -> ").append(call)
                    .append(").doesNotThrowAnyException();\n");
        } else if ("boolean".equals(returnType) || "Boolean".equals(returnType)) {
            sb.append("        boolean result = ").append(call).append(";\n");
            sb.append("        assertThat(result).isFalse();\n");
        } else {
            sb.append("        ").append(returnType).append(" result = ").append(call).append(";\n");
            appendResultAssertion(sb, returnType);
        }
        sb.append("    }\n\n");
    }

    private void appendParameterDeclarations(final StringBuilder sb, final List<MethodParameter> params) {
        if (params.isEmpty()) {
            sb.append("        // (aucun parametre)\n");
        } else {
            for (MethodParameter param : params) {
                String defaultValue = GeneratorUtils.defaultValueExpression(param.type());
                if ("null".equals(defaultValue)) {
                    sb.append("        final ").append(param.type()).append(" ").append(param.name())
                            .append(" = null; // TODO: fournir une valeur metier\n");
                } else {
                    sb.append("        final ").append(param.type()).append(" ").append(param.name())
                            .append(" = ").append(defaultValue).append(";\n");
                }
            }
        }
    }

    private List<MethodParameter> effectiveParameters(final BusinessRule rule) {
        if (!rule.hasSignature()) {
            if (rule.extractionCandidate() == ExtractionCandidate.POLICY) {
                return List.of(MethodParameter.known("Object", "context"));
            }
            return List.of();
        }
        MethodSignature filtered = JavaFxUiTypeFilter.filterForDomain(rule.signature());
        return filtered.parameters();
    }

    private String effectiveReturnType(final BusinessRule rule) {
        if (rule.extractionCandidate() == ExtractionCandidate.POLICY && !rule.hasSignature()) {
            return "boolean";
        }
        return GeneratorUtils.buildReturnType(rule);
    }

    private String buildDomainParameterList(final BusinessRule rule) {
        List<MethodParameter> params = effectiveParameters(rule);
        return buildParameterList(params);
    }

    private String buildArgumentList(final List<MethodParameter> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder arguments = new StringBuilder();
        for (MethodParameter param : params) {
            if (!arguments.isEmpty()) {
                arguments.append(", ");
            }
            arguments.append(param.name());
        }
        return arguments.toString();
    }

    private String buildParameterList(final List<MethodParameter> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder signature = new StringBuilder();
        for (MethodParameter param : params) {
            if (!signature.isEmpty()) {
                signature.append(", ");
            }
            signature.append("final ").append(param.type()).append(" ").append(param.name());
        }
        return signature.toString();
    }

    private void appendResultAssertion(final StringBuilder sb, final String returnType) {
        String simpleType = simpleTypeName(returnType);
        switch (simpleType) {
            case "String" -> sb.append("        assertThat(result).isEqualTo(\"sample\");\n");
            case "List", "Set", "Map", "Optional" -> sb.append("        assertThat(result).isEmpty();\n");
            case "int", "Integer" -> sb.append("        assertThat(result).isZero();\n");
            case "long", "Long" -> sb.append("        assertThat(result).isZero();\n");
            case "double", "Double" -> sb.append("        assertThat(result).isZero();\n");
            case "float", "Float" -> sb.append("        assertThat(result).isZero();\n");
            default -> sb.append("        assertThat(result).isNotNull();\n");
        }
    }

    private String simpleTypeName(final String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        String cleaned = type.trim();
        int genericStart = cleaned.indexOf('<');
        if (genericStart >= 0) {
            cleaned = cleaned.substring(0, genericStart);
        }
        int dot = cleaned.lastIndexOf('.');
        if (dot >= 0) {
            cleaned = cleaned.substring(dot + 1);
        }
        return cleaned;
    }

    private boolean hasCandidate(
            final List<BusinessRule> rules, final ExtractionCandidate candidate) {
        for (BusinessRule rule : rules) {
            if (rule.extractionCandidate() == candidate) {
                return true;
            }
        }
        return false;
    }
}
