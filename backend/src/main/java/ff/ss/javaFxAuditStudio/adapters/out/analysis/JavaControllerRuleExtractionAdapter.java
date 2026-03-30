package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerDependency;
import ff.ss.javaFxAuditStudio.domain.analysis.DependencyKind;
import ff.ss.javaFxAuditStudio.domain.analysis.DetectionStatus;
import ff.ss.javaFxAuditStudio.domain.analysis.StateMachineInsight;
import ff.ss.javaFxAuditStudio.domain.analysis.StateTransition;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter d'extraction de regles de gestion par analyse textuelle (regex) du contenu Java source.
 *
 * <p>Limite documentee : {@code sourceLine} est toujours 0, l'analyse textuelle par regex
 * ne permet pas de retrouver les numeros de ligne de maniere fiable sans parser l'AST complet.
 *
 * <p>Pas d'annotation Spring : instanciation explicite via {@code ClassificationConfiguration}.
 */
public final class JavaControllerRuleExtractionAdapter implements RuleExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(JavaControllerRuleExtractionAdapter.class);

    private static final Pattern FXML_HANDLER_PATTERN =
            Pattern.compile("@FXML\\s+(?:public\\s+)?void\\s+(\\w+)\\s*\\(");

    private static final Pattern NAMED_HANDLER_PATTERN =
            Pattern.compile("(?:public|private|protected)\\s+void\\s+(on\\w+|handle\\w+)\\s*\\(");

    private static final Pattern FXML_FIELD_PATTERN =
            Pattern.compile("@FXML\\s+(?:private\\s+)?(\\w+)\\s+(\\w+)\\s*;");

    private static final Pattern INJECTED_SERVICE_PATTERN =
            Pattern.compile("@(?:Autowired|Inject)\\s+(?:private\\s+)?(\\w+)\\s+(\\w+)\\s*;");

    /** JAS-020 — Pattern de detection des methodes garde booléennes. */
    private static final Pattern BOOLEAN_GUARD_PATTERN =
            Pattern.compile(
                "(?:public|private|protected)\\s+(?:boolean|Boolean)\\s+" +
                "((?:is|can|has|should)[A-Z]\\w*)\\s*\\(");
    private static final Pattern STATE_FIELD_PATTERN =
            Pattern.compile("(?:private|protected|public)\\s+(?:boolean|Boolean)\\s+(is\\w+(?:Mode|State|Step|Stage))\\b");
    private static final Pattern METHOD_PATTERN =
            Pattern.compile("(?:public|private|protected)\\s+[\\w<>]+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
    private static final Pattern DIRECT_CONTROLLER_FIELD_PATTERN =
            Pattern.compile("(?:private|protected|public)\\s+(\\w+Controller)\\s+(\\w+)\\s*;");
    private static final Pattern DIRECT_CONTROLLER_CREATION_PATTERN =
            Pattern.compile("new\\s+(\\w+Controller)\\s*\\(");
    private static final List<String> SHARED_SERVICE_SUFFIXES = List.of(
            "Service", "Manager", "Handler", "Validator", "Facade", "Processor");

    private static final String UNKNOWN_REF = "unknown";

    private final Set<String> lifecycleExcluded;
    private final AnalysisProperties.ClassificationPatterns patterns;

    /**
     * Constructeur de compatibilite sans argument (utilise dans les tests unitaires).
     * Initialise avec un ensemble vide de methodes exclues et des patterns par defaut.
     */
    public JavaControllerRuleExtractionAdapter() {
        this(Set.of(), new AnalysisProperties.ClassificationPatterns(
                null, null, null, null, null, null, null, null));
    }

    /**
     * Constructeur de compatibilite avec exclusions lifecycle uniquement.
     * Utilise les patterns de classification par defaut.
     */
    public JavaControllerRuleExtractionAdapter(final Set<String> lifecycleExcluded) {
        this(lifecycleExcluded,
                new AnalysisProperties.ClassificationPatterns(
                        null, null, null, null, null, null, null, null));
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
        int[] excludedCount = {0};
        extractFxmlHandlers(ref, javaContent, rules, excludedCount);
        extractNamedHandlers(ref, javaContent, rules, excludedCount);
        extractFxmlFields(ref, javaContent, rules);
        extractBooleanGuards(ref, javaContent, rules, excludedCount);  // JAS-020
        extractInjectedServices(ref, javaContent, rules);
        StateMachineInsight stateMachine = detectStateMachine(javaContent);
        List<ControllerDependency> dependencies = detectDependencies(javaContent, ref);
        if (excludedCount[0] > 0) {
            log.info("{} methode(s) lifecycle exclue(s) - ref={}", excludedCount[0], ref);
        }
        return ExtractionResult.regexFallback(
                List.copyOf(rules),
                "Analyse textuelle regex",
                excludedCount[0],
                stateMachine,
                dependencies);
    }

    private void extractFxmlHandlers(
            final String ref, final String content, final List<BusinessRule> rules,
            final int[] excludedCount) {
        Matcher matcher = FXML_HANDLER_PATTERN.matcher(content);
        while (matcher.find()) {
            addHandlerRule(ref, content, matcher.end(), matcher.group(1), rules, excludedCount);
        }
    }

    private void extractNamedHandlers(
            final String ref, final String content, final List<BusinessRule> rules,
            final int[] excludedCount) {
        Matcher matcher = NAMED_HANDLER_PATTERN.matcher(content);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (!isAlreadyExtractedAsFxmlHandler(content, methodName)) {
                addHandlerRule(ref, content, matcher.end(), methodName, rules, excludedCount);
            }
        }
    }

    private void addHandlerRule(
            final String ref,
            final String content,
            final int bodyStart,
            final String methodName,
            final List<BusinessRule> rules,
            final int[] excludedCount) {
        if (lifecycleExcluded.contains(methodName)) {
            excludedCount[0]++;
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

    /**
     * JAS-020 — Extrait les methodes garde booléennes (isXxx, canXxx, hasXxx, shouldXxx)
     * et les classe BUSINESS / POLICY.
     */
    private void extractBooleanGuards(
            final String ref, final String content, final List<BusinessRule> rules,
            final int[] excludedCount) {
        Matcher matcher = BOOLEAN_GUARD_PATTERN.matcher(content);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (isUiGuardMethod(methodName)) {
                continue;
            }
            if (lifecycleExcluded.contains(methodName)) {
                excludedCount[0]++;
                continue;
            }
            // Eviter les doublons avec les handlers @FXML deja extraits (improbable pour boolean)
            boolean alreadyExtracted = rules.stream()
                    .anyMatch(r -> r.description().contains("handler " + methodName));
            if (alreadyExtracted) {
                continue;
            }
            String methodBody = extractApproximateBody(content, matcher.end());
            if (containsAnyKeyword(methodBody, patterns.effectiveUiKeywords())) {
                String ruleId = buildRuleId(ref, rules.size() + 1);
                String description = "Methode " + methodName + "() : garde UI detectee";
                rules.add(new BusinessRule(ruleId, description, ref, 0,
                        ResponsibilityClass.UI, ExtractionCandidate.VIEW_MODEL, false));
                continue;
            }
            String ruleId = buildRuleId(ref, rules.size() + 1);
            String description = "Methode garde " + methodName
                    + " : decision metier BUSINESS detectee";
            rules.add(new BusinessRule(ruleId, description, ref, 0,
                    ResponsibilityClass.BUSINESS, ExtractionCandidate.POLICY, false));
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

    private boolean isUiGuardMethod(final String methodName) {
        for (String uiGuardName : patterns.effectiveUiGuardMethodNames()) {
            if (uiGuardName.equals(methodName)) {
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

    private StateMachineInsight detectStateMachine(final String content) {
        LinkedHashSet<String> stateFields = new LinkedHashSet<>();
        Matcher matcher = STATE_FIELD_PATTERN.matcher(content);
        while (matcher.find()) {
            stateFields.add(matcher.group(1));
        }
        if (stateFields.size() < 2) {
            return StateMachineInsight.absent();
        }
        List<StateTransition> transitions = collectTransitions(content, List.copyOf(stateFields));
        double confidence = Math.min(1.0d, 0.20d + (stateFields.size() * 0.20d) + (transitions.size() * 0.15d));
        DetectionStatus status = confidence >= patterns.effectiveStateMachineConfidenceThreshold()
                ? DetectionStatus.CONFIRMED
                : DetectionStatus.POSSIBLE;
        return new StateMachineInsight(
                status,
                confidence,
                stateFields.stream().map(this::normalizeStateName).toList(),
                transitions);
    }

    private List<StateTransition> collectTransitions(final String content, final List<String> stateFields) {
        LinkedHashSet<StateTransition> transitions = new LinkedHashSet<>();
        Matcher matcher = METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            String body = extractApproximateBody(content, matcher.end() - 1);
            List<String> activated = new ArrayList<>();
            List<String> deactivated = new ArrayList<>();
            for (String stateField : stateFields) {
                if (body.contains(stateField + " = true")) {
                    activated.add(stateField);
                }
                if (body.contains(stateField + " = false")) {
                    deactivated.add(stateField);
                }
            }
            if (!activated.isEmpty()) {
                String fromState = deactivated.isEmpty()
                        ? "CURRENT"
                        : normalizeStateName(deactivated.get(0));
                for (String activatedState : activated) {
                    transitions.add(new StateTransition(
                            fromState,
                            normalizeStateName(activatedState),
                            methodName));
                }
            }
        }
        return List.copyOf(transitions);
    }

    private String normalizeStateName(final String fieldName) {
        String normalized = fieldName.startsWith("is") ? fieldName.substring(2) : fieldName;
        String[] suffixes = {"Mode", "State", "Step", "Stage"};
        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }
        return normalized;
    }

    private List<ControllerDependency> detectDependencies(final String content, final String ref) {
        LinkedHashSet<ControllerDependency> dependencies = new LinkedHashSet<>();
        String currentController = shortControllerRef(ref);
        Matcher fieldMatcher = DIRECT_CONTROLLER_FIELD_PATTERN.matcher(content);
        while (fieldMatcher.find()) {
            String typeName = fieldMatcher.group(1);
            String fieldName = fieldMatcher.group(2);
            if (!typeName.equals(currentController)) {
                dependencies.add(new ControllerDependency(
                        DependencyKind.DIRECT_CONTROLLER,
                        typeName,
                        "field:" + fieldName));
            }
        }
        Matcher creationMatcher = DIRECT_CONTROLLER_CREATION_PATTERN.matcher(content);
        while (creationMatcher.find()) {
            String typeName = creationMatcher.group(1);
            if (!typeName.equals(currentController)) {
                dependencies.add(new ControllerDependency(
                        DependencyKind.DIRECT_CONTROLLER,
                        typeName,
                        "new:" + typeName));
            }
        }
        Matcher injectedMatcher = INJECTED_SERVICE_PATTERN.matcher(content);
        while (injectedMatcher.find()) {
            String typeName = injectedMatcher.group(1);
            String fieldName = injectedMatcher.group(2);
            if (isSharedServiceType(typeName)) {
                dependencies.add(new ControllerDependency(
                        DependencyKind.SHARED_SERVICE,
                        typeName,
                        "field:" + fieldName));
            }
        }
        return List.copyOf(dependencies);
    }

    private boolean isSharedServiceType(final String typeName) {
        for (String suffix : SHARED_SERVICE_SUFFIXES) {
            if (typeName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
