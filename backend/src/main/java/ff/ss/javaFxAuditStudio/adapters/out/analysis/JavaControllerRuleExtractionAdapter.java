package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter d'extraction de regles de gestion par analyse textuelle (regex) du contenu Java source.
 *
 * <p>Limite documentee : {@code sourceLine} est toujours 0, l'analyse textuelle par regex
 * ne permet pas de retrouver les numeros de ligne de maniere fiable sans parser l'AST complet.
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

    private final Set<String> lifecycleExcluded;
    private final AnalysisProperties.ClassificationPatterns patterns;

    /**
     * Constructeur de compatibilite sans argument (utilise dans les tests unitaires).
     * Initialise avec un ensemble vide de methodes exclues et des patterns par defaut.
     */
    public JavaControllerRuleExtractionAdapter() {
        this(Set.of(), new AnalysisProperties.ClassificationPatterns(null, null, null, null, null));
    }

    /**
     * Constructeur de compatibilite avec exclusions lifecycle uniquement.
     * Utilise les patterns de classification par defaut.
     */
    public JavaControllerRuleExtractionAdapter(final Set<String> lifecycleExcluded) {
        this(lifecycleExcluded,
                new AnalysisProperties.ClassificationPatterns(null, null, null, null, null));
    }

    /**
     * Constructeur principal : injection explicite des exclusions lifecycle et des patterns
     * de classification depuis {@code ClassificationConfiguration}.
     */
    public JavaControllerRuleExtractionAdapter(
            final Set<String> lifecycleExcluded,
            final AnalysisProperties.ClassificationPatterns patterns) {
        this.lifecycleExcluded = Objects.requireNonNull(lifecycleExcluded, "lifecycleExcluded must not be null");
        this.patterns = Objects.requireNonNull(patterns, "patterns must not be null");
    }

    @Override
    public ExtractionResult extract(final String controllerRef, final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return ExtractionResult.regexFallback(List.of(), "Analyse textuelle regex");
        }
        String ref = (controllerRef == null) ? UNKNOWN_REF : controllerRef;
        List<BusinessRule> rules = new ArrayList<>();
        extractFxmlHandlers(ref, javaContent, rules);
        extractNamedHandlers(ref, javaContent, rules);
        extractFxmlFields(ref, javaContent, rules);
        extractInjectedServices(ref, javaContent, rules);
        return ExtractionResult.regexFallback(List.copyOf(rules), "Analyse textuelle regex");
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
        if (lifecycleExcluded.contains(methodName)) {
            return;
        }
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
            // Les services injectes dans un controller JavaFX sont des ports sortants
            // (GATEWAY) du point de vue de l'architecture hexagonale.
            // Les intentions utilisateur proviennent des handlers @FXML, pas des services.
            ExtractionCandidate ec = isInfrastructureType(typeName)
                    ? ExtractionCandidate.GATEWAY
                    : ExtractionCandidate.GATEWAY; // toujours GATEWAY — le UseCase agrege
            String ruleId = buildRuleId(ref, rules.size() + 1);
            String description =
                    "Service injecte " + typeName + " " + fieldName
                    + " : responsabilite TECHNICAL detectee";
            rules.add(new BusinessRule(ruleId, description, ref, 0,
                    ResponsibilityClass.TECHNICAL, ec, false));
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

    /**
     * Classifie le corps d'un handler selon les familles de mots-cles configurables.
     *
     * <p>Ordre de priorite : UI > BUSINESS > APPLICATION (mots-cles) > APPLICATION (service call)
     * > TECHNICAL > UNKNOWN.
     */
    private ResponsibilityClass classifyByKeywords(final String body) {
        if (containsAnyKeyword(body, patterns.effectiveUiKeywords())) {
            return ResponsibilityClass.UI;
        }
        if (containsAnyKeyword(body, patterns.effectiveBusinessKeywords())) {
            return ResponsibilityClass.BUSINESS;
        }
        if (containsAnyKeyword(body, patterns.effectiveApplicationKeywords())) {
            return ResponsibilityClass.APPLICATION;
        }
        if (containsServiceCall(body)) {
            return ResponsibilityClass.APPLICATION;
        }
        if (containsAnyKeyword(body, patterns.effectiveTechnicalKeywords())) {
            return ResponsibilityClass.TECHNICAL;
        }
        return ResponsibilityClass.UNKNOWN;
    }

    /**
     * Detecte un appel de service dans le corps de la methode en cherchant les suffixes
     * configures (ex: {@code calculateur.}, {@code gestionnaire.}, {@code service.}).
     */
    private boolean containsServiceCall(final String body) {
        String lower = body.toLowerCase();
        for (String suffix : patterns.effectiveServiceCallSuffixes()) {
            if (lower.contains(suffix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyKeyword(final String text, final List<String> keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
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

    private boolean isInfrastructureType(final String typeName) {
        return typeName.contains("Factory") || typeName.contains("Utils")
                || typeName.contains("Config") || typeName.contains("Converter")
                || typeName.contains("Adapter") || typeName.contains("Helper");
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
