package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter d'extraction de règles de gestion par analyse textuelle (regex) du contenu Java source.
 *
 * <p>Limite documentée : {@code sourceLine} est toujours 0, l'analyse textuelle par regex
 * ne permet pas de retrouver les numéros de ligne de manière fiable sans parser l'AST complet.
 *
 * <p>Pas d'annotation Spring : instanciation explicite via {@code ClassificationConfiguration}.
 */
public final class JavaControllerRuleExtractionAdapter implements RuleExtractionPort {

    private static final Pattern FXML_HANDLER_PATTERN =
            Pattern.compile("@FXML\\s+(?:public\\s+)?void\\s+(\\w+)\\s*\\(");

    private static final Pattern NAMED_HANDLER_PATTERN =
            Pattern.compile("(?:public|private|protected)\\s+void\\s+(on\\w+|handle\\w+)\\s*\\(");

    private static final Pattern FXML_FIELD_PATTERN =
            Pattern.compile("@FXML\\s+(?:private\\s+)?(\\w+)\\s+(\\w+)\\s*;");

    private static final Pattern INJECTED_SERVICE_PATTERN =
            Pattern.compile("@(?:Autowired|Inject)\\s+(?:private\\s+)?(\\w+)\\s+(\\w+)\\s*;");

    private static final String UNKNOWN_REF = "unknown";

    @Override
    public List<BusinessRule> extract(final String controllerRef, final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return List.of();
        }
        String ref = (controllerRef == null) ? UNKNOWN_REF : controllerRef;
        List<BusinessRule> rules = new ArrayList<>();
        extractFxmlHandlers(ref, javaContent, rules);
        extractNamedHandlers(ref, javaContent, rules);
        extractFxmlFields(ref, javaContent, rules);
        extractInjectedServices(ref, javaContent, rules);
        return List.copyOf(rules);
    }

    private void extractFxmlHandlers(
            final String ref, final String content, final List<BusinessRule> rules) {
        Matcher matcher = FXML_HANDLER_PATTERN.matcher(content);
        while (matcher.find()) {
            addHandlerRule(ref, content, matcher.end(), matcher.group(1), rules);
        }
    }

    private void extractNamedHandlers(
            final String ref, final String content, final List<BusinessRule> rules) {
        Matcher matcher = NAMED_HANDLER_PATTERN.matcher(content);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (!isAlreadyExtractedAsFxmlHandler(content, methodName)) {
                addHandlerRule(ref, content, matcher.end(), methodName, rules);
            }
        }
    }

    private void addHandlerRule(
            final String ref,
            final String content,
            final int bodyStart,
            final String methodName,
            final List<BusinessRule> rules) {
        String methodBody = extractApproximateBody(content, bodyStart);
        ResponsibilityClass rc = classifyByKeywords(methodBody);
        ExtractionCandidate ec = candidateFor(rc);
        boolean uncertain = rc == ResponsibilityClass.UNKNOWN;
        String ruleId = buildRuleId(ref, rules.size() + 1);
        String description = buildHandlerDescription(methodName, rc);
        rules.add(new BusinessRule(ruleId, description, ref, 0, rc, ec, uncertain));
    }

    private void extractFxmlFields(
            final String ref, final String content, final List<BusinessRule> rules) {
        Matcher matcher = FXML_FIELD_PATTERN.matcher(content);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            String fieldName = matcher.group(2);
            String ruleId = buildRuleId(ref, rules.size() + 1);
            String description =
                    "Champ FXML " + typeName + " " + fieldName + " : liaison UI directe detectee";
            rules.add(new BusinessRule(
                    ruleId, description, ref, 0,
                    ResponsibilityClass.UI, ExtractionCandidate.VIEW_MODEL, false));
        }
    }

    private void extractInjectedServices(
            final String ref, final String content, final List<BusinessRule> rules) {
        Matcher matcher = INJECTED_SERVICE_PATTERN.matcher(content);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            String fieldName = matcher.group(2);
            boolean isAppLayer = isApplicationLayerService(typeName);
            ResponsibilityClass rc =
                    isAppLayer ? ResponsibilityClass.APPLICATION : ResponsibilityClass.TECHNICAL;
            ExtractionCandidate ec =
                    isAppLayer ? ExtractionCandidate.USE_CASE : ExtractionCandidate.GATEWAY;
            String ruleId = buildRuleId(ref, rules.size() + 1);
            String description =
                    "Service injecte " + typeName + " " + fieldName
                    + " : responsabilite " + rc.name() + " detectee";
            rules.add(new BusinessRule(ruleId, description, ref, 0, rc, ec, false));
        }
    }

    private boolean isAlreadyExtractedAsFxmlHandler(final String content, final String methodName) {
        String fxmlBeforePattern = "@FXML[\\s\\S]{0,60}void\\s+" + Pattern.quote(methodName) + "\\s*\\(";
        return Pattern.compile(fxmlBeforePattern).matcher(content).find();
    }

    private String extractApproximateBody(final String content, final int startIndex) {
        int openBrace = content.indexOf('{', startIndex);
        if (openBrace < 0) {
            return "";
        }
        int depth = 1;
        int pos = openBrace + 1;
        while (pos < content.length() && depth > 0) {
            char c = content.charAt(pos);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            pos++;
        }
        return content.substring(openBrace, pos);
    }

    private ResponsibilityClass classifyByKeywords(final String body) {
        if (containsAny(body,
                "setText", "setVisible", "getChildren", "setStyle", "setDisable", "getScene")) {
            return ResponsibilityClass.UI;
        }
        if (containsAny(body,
                "service.save", "repository", "persist", "entityManager", "flush", "commit")) {
            return ResponsibilityClass.BUSINESS;
        }
        if (containsAny(body,
                "execute", "invoke", "useCase", "command", "submit", "dispatch")) {
            return ResponsibilityClass.APPLICATION;
        }
        if (containsAny(body,
                "restTemplate", "webClient", "http", "ftp", "socket", "printJob", "file.write")) {
            return ResponsibilityClass.TECHNICAL;
        }
        return ResponsibilityClass.UNKNOWN;
    }

    private ExtractionCandidate candidateFor(final ResponsibilityClass rc) {
        return switch (rc) {
            case UI -> ExtractionCandidate.VIEW_MODEL;
            case BUSINESS -> ExtractionCandidate.POLICY;
            case APPLICATION -> ExtractionCandidate.USE_CASE;
            case TECHNICAL -> ExtractionCandidate.GATEWAY;
            default -> ExtractionCandidate.NONE;
        };
    }

    private boolean isApplicationLayerService(final String typeName) {
        return typeName.contains("Service") || typeName.contains("UseCase");
    }

    private boolean containsAny(final String text, final String... keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String buildRuleId(final String ref, final int index) {
        String shortRef = shortControllerRef(ref);
        return String.format("RG-%s-%03d", shortRef, index);
    }

    private String shortControllerRef(final String ref) {
        int lastSlash = Math.max(ref.lastIndexOf('/'), ref.lastIndexOf('\\'));
        String name = (lastSlash >= 0) ? ref.substring(lastSlash + 1) : ref;
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }

    private String buildHandlerDescription(final String methodName, final ResponsibilityClass rc) {
        return "Methode handler " + methodName + " : responsabilite " + rc.name() + " detectee";
    }
}
