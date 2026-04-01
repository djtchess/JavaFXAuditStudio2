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
import ff.ss.javaFxAuditStudio.domain.rules.MethodParameter;
import ff.ss.javaFxAuditStudio.domain.rules.MethodSignature;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Problem;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Extraction de regles de gestion par analyse AST via JavaParser.
 * En cas d'echec du parsing AST, delegue au fallback regex.
 * Instancie par ClassificationConfiguration — pas d'annotation Spring.
 */
public final class JavaParserRuleExtractionAdapter implements RuleExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(JavaParserRuleExtractionAdapter.class);
    private static final String UNKNOWN_REF = "unknown";
    private static final List<String> STATE_FIELD_SUFFIXES = List.of("Mode", "State", "Step", "Stage");
    private static final List<String> SHARED_SERVICE_SUFFIXES = List.of(
            "Service", "Manager", "Handler", "Validator", "Facade", "Processor");

    private final RuleExtractionPort fallback;
    private final Set<String> lifecycleExcluded;
    private final AnalysisProperties.ClassificationPatterns patterns;

    public JavaParserRuleExtractionAdapter(final RuleExtractionPort fallback) {
        this(fallback, Set.of(),
                new AnalysisProperties.ClassificationPatterns(
                        null, null, null, null, null, null, null, null));
    }

    public JavaParserRuleExtractionAdapter(
            final RuleExtractionPort fallback,
            final Set<String> lifecycleExcluded) {
        this(fallback, lifecycleExcluded,
                new AnalysisProperties.ClassificationPatterns(
                        null, null, null, null, null, null, null, null));
    }

    public JavaParserRuleExtractionAdapter(
            final RuleExtractionPort fallback,
            final Set<String> lifecycleExcluded,
            final AnalysisProperties.ClassificationPatterns patterns) {
        this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
        this.lifecycleExcluded = Objects.requireNonNull(lifecycleExcluded, "lifecycleExcluded must not be null");
        this.patterns = Objects.requireNonNull(patterns, "patterns must not be null");
    }

    @Override
    public ExtractionResult extract(final String controllerRef, final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return ExtractionResult.ast(List.of());
        }
        String ref = (controllerRef == null) ? UNKNOWN_REF : controllerRef;
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaContent);
            int[] excludedCount = {0};
            List<BusinessRule> rules = extractFromAst(ref, cu, excludedCount);
            StateMachineInsight stateMachine = detectStateMachine(cu);
            List<ControllerDependency> dependencies = detectDependencies(cu, ref);
            return ExtractionResult.ast(rules, excludedCount[0], stateMachine, dependencies);
        } catch (Exception e) {
            String causeMessage = buildCauseMessage(ref, e);
            ExtractionResult fallbackResult = fallback.extract(controllerRef, javaContent);
            return ExtractionResult.regexFallback(
                    fallbackResult.rules(),
                    causeMessage,
                    fallbackResult.excludedLifecycleMethodsCount(),
                    fallbackResult.stateMachine(),
                    fallbackResult.dependencies());
        }
    }

    private String buildCauseMessage(final String ref, final Exception e) {
        if (e instanceof ParseProblemException ppe) {
            List<Problem> problems = ppe.getProblems();
            String firstMessage = problems.isEmpty() ? "(aucun detail)" : problems.get(0).getMessage();
            log.warn("JavaParser a echoue ({} probleme(s)) - ref={}, premier probleme: \"{}\" - delegation au fallback regex",
                    problems.size(), ref, firstMessage);
            for (Problem problem : problems) {
                log.debug("JavaParser probleme - ref={}, localisation={}, message={}",
                        ref,
                        problem.getLocation().map(Object::toString).orElse("inconnue"),
                        problem.getMessage());
            }
            return firstMessage;
        }
        log.warn("JavaParser a echoue - ref={}, cause={} - delegation au fallback regex",
                ref, e.getMessage());
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private List<BusinessRule> extractFromAst(
            final String ref, final CompilationUnit cu, final int[] excludedCount) {
        List<BusinessRule> rules = new ArrayList<>();
        List<String> injectedTypes = collectInjectedTypes(cu);
        addFxmlFieldRules(ref, cu, rules);
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
            if (lifecycleExcluded.contains(method.getNameAsString())) {
                log.debug("Methode lifecycle ignoree - ref={}, methode={}", ref, method.getNameAsString());
                excludedCount[0]++;
                continue;
            }
            BusinessRule rule = analyzeMethod(ref, method, injectedTypes, rules.size() + 1);
            rules.add(rule);
        }
        if (excludedCount[0] > 0) {
            log.info("{} methode(s) lifecycle exclue(s) de l'analyse - ref={}", excludedCount[0], ref);
        }
        return List.copyOf(rules);
    }

    /**
     * Collecte les champs @FXML (composants UI) et genere une regle VIEW_MODEL par champ.
     */
    private void addFxmlFieldRules(
            final String ref, final CompilationUnit cu, final List<BusinessRule> rules) {
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        for (FieldDeclaration field : fields) {
            if (!field.getAnnotationByName("FXML").isPresent()) continue;
            String typeName = field.getCommonType().asString();
            field.getVariables().forEach(v -> {
                String fieldName = v.getNameAsString();
                String ruleId = buildRuleId(ref, rules.size() + 1);
                String description = "Champ FXML " + typeName + " " + fieldName
                        + " : liaison UI directe detectee";
                rules.add(new BusinessRule(ruleId, description, ref, 0,
                        ResponsibilityClass.UI, ExtractionCandidate.VIEW_MODEL, false));
            });
        }
    }

    private List<String> collectInjectedTypes(final CompilationUnit cu) {
        List<String> types = new ArrayList<>();
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        for (FieldDeclaration field : fields) {
            boolean injected = field.getAnnotationByName("Inject").isPresent()
                    || field.getAnnotationByName("Autowired").isPresent();
            if (injected) {
                String typeName = field.getCommonType().asString();
                types.add(typeName);
            }
        }
        return types;
    }

    private BusinessRule analyzeMethod(
            final String ref,
            final MethodDeclaration method,
            final List<String> injectedTypes,
            final int index) {
        String methodName = method.getNameAsString();
        int sourceLine = method.getBegin().map(p -> p.line).orElse(0);
        int complexity = computeCyclomaticComplexity(method);
        boolean isFxml = method.getAnnotationByName("FXML").isPresent();
        String bodyText = method.getBody().map(Object::toString).orElse("");
        ResponsibilityClass rc;
        boolean policyGuard = isGuardMethod(method);
        boolean uiLikeGuard = policyGuard && containsUiKeywords(bodyText);
        if (policyGuard && !uiLikeGuard) {
            rc = ResponsibilityClass.BUSINESS;
        } else {
            rc = classifyMethod(isFxml, bodyText, injectedTypes);
        }
        ExtractionCandidate ec = candidateFor(rc);
        boolean uncertain = (rc == ResponsibilityClass.UNKNOWN);
        String description = buildDescription(methodName, method, isFxml, rc, complexity);
        String ruleId = buildRuleId(ref, index);
        MethodSignature signature = extractSignature(method);
        return new BusinessRule(ruleId, description, ref, sourceLine, rc, ec, uncertain, signature);
    }

    private MethodSignature extractSignature(final MethodDeclaration method) {
        String returnType = method.getTypeAsString();
        List<MethodParameter> params = method.getParameters().stream()
                .map(p -> MethodParameter.known(p.getTypeAsString(), p.getNameAsString()))
                .toList();
        return MethodSignature.of(returnType, params);
    }

    /**
     * Classifie une methode selon sa responsabilite.
     *
     * <p>Regle principale : un handler @FXML qui appelle un service injecte est APPLICATION
     * (point d'orchestration), pas UI. Seuls les handlers qui ne font que manipuler
     * l'etat visuel (setText, setVisible…) sont classes UI.
     */
    private ResponsibilityClass classifyMethod(
            final boolean isFxml,
            final String bodyText,
            final List<String> injectedTypes) {
        if (containsTechnicalKeywords(bodyText)) {
            return ResponsibilityClass.TECHNICAL;
        }
        if (containsBusinessLogic(bodyText)) {
            return ResponsibilityClass.BUSINESS;
        }
        if (containsServiceCalls(bodyText, injectedTypes)) {
            return ResponsibilityClass.APPLICATION;
        }
        if (containsCoordination(bodyText, injectedTypes)) {
            return ResponsibilityClass.APPLICATION;
        }
        if (containsUiKeywords(bodyText)) {
            return ResponsibilityClass.UI;
        }
        if (isFxml) {
            // Handler @FXML sans appel de service detecte → APPLICATION par defaut
            // (il orchestre une action utilisateur, meme si le corps n'est pas encore analysable)
            return ResponsibilityClass.APPLICATION;
        }
        return ResponsibilityClass.UNKNOWN;
    }

    /**
     * Detecte si le corps d'une methode appelle un des services injectes ou correspond
     * aux suffixes de service call configures (ex: calculateur., gestionnaire.).
     */
    private boolean containsServiceCalls(final String body, final List<String> injectedTypes) {
        String lower = body.toLowerCase();
        for (String type : injectedTypes) {
            // heuristique : champ = decapitalized type name ou contient le nom du type
            String fieldHint = Character.toLowerCase(type.charAt(0)) + type.substring(1);
            if (lower.contains(fieldHint.toLowerCase() + ".")) {
                return true;
            }
        }
        for (String suffix : patterns.effectiveServiceCallSuffixes()) {
            if (lower.contains(suffix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUiKeywords(final String body) {
        String lower = body.toLowerCase();
        for (String keyword : patterns.effectiveUiKeywords()) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTechnicalKeywords(final String body) {
        String lower = body.toLowerCase();
        for (String keyword : patterns.effectiveTechnicalKeywords()) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBusinessLogic(final String body) {
        String lower = body.toLowerCase();
        for (String keyword : patterns.effectiveBusinessKeywords()) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCoordination(final String body, final List<String> injectedTypes) {
        String lower = body.toLowerCase();
        for (String keyword : patterns.effectiveApplicationKeywords()) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        for (String injectedType : injectedTypes) {
            if (lower.contains(injectedType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * JAS-020 — Detecte si une methode est une methode garde : retour boolean ET nom
     * commencant par un prefixe de guard (is, can, has, should par defaut).
     * Ces methodes representent des decisions metier et vont vers la Policy.
     */
    private boolean isGuardMethod(final MethodDeclaration method) {
        String returnType = method.getTypeAsString();
        if (!"boolean".equals(returnType) && !"Boolean".equals(returnType)) {
            return false;
        }
        String name = method.getNameAsString();
        if (isUiGuardMethod(name)) {
            return false;
        }
        for (String prefix : patterns.effectivePolicyGuardPrefixes()) {
            if (name.length() > prefix.length()
                    && name.startsWith(prefix)
                    && Character.isUpperCase(name.charAt(prefix.length()))) {
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

    private int computeCyclomaticComplexity(final MethodDeclaration method) {
        int complexity = 1;
        complexity += method.findAll(IfStmt.class).size();
        complexity += method.findAll(ForStmt.class).size();
        complexity += method.findAll(ForEachStmt.class).size();
        complexity += method.findAll(WhileStmt.class).size();
        complexity += method.findAll(SwitchEntry.class).size();
        complexity += method.findAll(CatchClause.class).size();
        complexity += method.findAll(ConditionalExpr.class).size();
        complexity += countLogicalOperators(method);
        return complexity;
    }

    private int countLogicalOperators(final MethodDeclaration method) {
        List<BinaryExpr> binaryExprs = method.findAll(BinaryExpr.class);
        int count = 0;
        for (BinaryExpr expr : binaryExprs) {
            BinaryExpr.Operator op = expr.getOperator();
            if (op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR) {
                count++;
            }
        }
        return count;
    }

    private ExtractionCandidate candidateFor(final ResponsibilityClass rc) {
        return switch (rc) {
            case UI -> ExtractionCandidate.VIEW_MODEL;
            case PRESENTATION -> ExtractionCandidate.VIEW_MODEL;
            case BUSINESS -> ExtractionCandidate.POLICY;
            case APPLICATION -> ExtractionCandidate.USE_CASE;
            case TECHNICAL -> ExtractionCandidate.GATEWAY;
            default -> ExtractionCandidate.NONE;
        };
    }

    private String buildDescription(
            final String methodName,
            final MethodDeclaration method,
            final boolean isFxml,
            final ResponsibilityClass rc,
            final int complexity) {
        // JAS-020 : format special pour les gardes booléennes
        String returnTypeStr = method.getTypeAsString();
        if (rc == ResponsibilityClass.BUSINESS
                && ("boolean".equals(returnTypeStr) || "Boolean".equals(returnTypeStr))) {
            for (String prefix : patterns.effectivePolicyGuardPrefixes()) {
                if (methodName.length() > prefix.length()
                        && methodName.startsWith(prefix)
                        && Character.isUpperCase(methodName.charAt(prefix.length()))) {
                    return "Methode garde " + methodName + " : decision metier BUSINESS detectee [complexite=" + complexity + "]";
                }
            }
        }

        // Format "Methode handler X" pour les handlers @FXML — aligne sur le format
        // du fallback regex et les patterns de methodNameFromRule() du generateur.
        if (isFxml) {
            return "Methode handler " + methodName + " : responsabilite " + rc.name()
                    + " detectee [complexite=" + complexity + "]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Methode ").append(methodName).append("(");
        method.getParameters().forEach(p -> {
            if (sb.charAt(sb.length() - 1) != '(') {
                sb.append(", ");
            }
            sb.append(p.getTypeAsString());
        });
        sb.append(") [complexite=").append(complexity).append("]");
        return sb.toString();
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

    private StateMachineInsight detectStateMachine(final CompilationUnit cu) {
        List<String> stateFields = collectStateFields(cu);
        if (stateFields.size() < 2) {
            return StateMachineInsight.absent();
        }
        List<StateTransition> transitions = collectTransitions(cu, stateFields);
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

    private List<String> collectStateFields(final CompilationUnit cu) {
        LinkedHashSet<String> stateFields = new LinkedHashSet<>();
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        for (FieldDeclaration field : fields) {
            String typeName = field.getCommonType().asString();
            if (!"boolean".equals(typeName) && !"Boolean".equals(typeName)) {
                continue;
            }
            field.getVariables().forEach(variable -> {
                String fieldName = variable.getNameAsString();
                if (looksLikeStateField(fieldName)) {
                    stateFields.add(fieldName);
                }
            });
        }
        return List.copyOf(stateFields);
    }

    private boolean looksLikeStateField(final String fieldName) {
        if (!fieldName.startsWith("is") || fieldName.length() <= 2) {
            return false;
        }
        for (String suffix : STATE_FIELD_SUFFIXES) {
            if (fieldName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private List<StateTransition> collectTransitions(
            final CompilationUnit cu,
            final List<String> stateFields) {
        LinkedHashSet<StateTransition> transitions = new LinkedHashSet<>();
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
            List<String> activated = new ArrayList<>();
            List<String> deactivated = new ArrayList<>();
            List<AssignExpr> assignments = method.findAll(AssignExpr.class);
            for (AssignExpr assignment : assignments) {
                String target = assignmentTarget(assignment);
                if (!stateFields.contains(target) || !(assignment.getValue() instanceof BooleanLiteralExpr literal)) {
                    continue;
                }
                if (literal.getValue()) {
                    activated.add(target);
                } else {
                    deactivated.add(target);
                }
            }
            if (!activated.isEmpty()) {
                String fromState = deactivated.isEmpty()
                        ? "CURRENT"
                        : normalizeStateName(deactivated.get(0));
                String trigger = method.getNameAsString();
                for (String activatedState : activated) {
                    transitions.add(new StateTransition(
                            fromState,
                            normalizeStateName(activatedState),
                            trigger));
                }
            }
        }
        return List.copyOf(transitions);
    }

    private String assignmentTarget(final AssignExpr assignment) {
        if (assignment.getTarget() instanceof NameExpr nameExpr) {
            return nameExpr.getNameAsString();
        }
        if (assignment.getTarget() instanceof FieldAccessExpr fieldAccessExpr) {
            return fieldAccessExpr.getNameAsString();
        }
        return "";
    }

    private String normalizeStateName(final String fieldName) {
        String normalized = fieldName.substring(2);
        for (String suffix : STATE_FIELD_SUFFIXES) {
            if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }
        return normalized;
    }

    private List<ControllerDependency> detectDependencies(
            final CompilationUnit cu,
            final String controllerRef) {
        LinkedHashSet<ControllerDependency> dependencies = new LinkedHashSet<>();
        String currentController = shortControllerRef(controllerRef);
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(type ->
                addInheritanceDependency(type, currentController, dependencies));
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        for (FieldDeclaration field : fields) {
            String typeName = field.getCommonType().asString();
            field.getVariables().forEach(variable -> {
                if (typeName.endsWith("Controller") && !typeName.equals(currentController)) {
                    dependencies.add(new ControllerDependency(
                            DependencyKind.DIRECT_CONTROLLER,
                            typeName,
                            "field:" + variable.getNameAsString()));
                }
                if (isSharedServiceType(typeName)) {
                    dependencies.add(new ControllerDependency(
                            DependencyKind.SHARED_SERVICE,
                            typeName,
                            "field:" + variable.getNameAsString()));
                }
            });
        }
        List<ObjectCreationExpr> creations = cu.findAll(ObjectCreationExpr.class);
        for (ObjectCreationExpr creation : creations) {
            String typeName = creation.getType().getNameAsString();
            if (typeName.endsWith("Controller") && !typeName.equals(currentController)) {
                dependencies.add(new ControllerDependency(
                        DependencyKind.DIRECT_CONTROLLER,
                        typeName,
                        "new:" + typeName));
            }
        }
        addDynamicUiDependencies(cu, dependencies);
        return List.copyOf(dependencies);
    }

    private void addInheritanceDependency(
            final ClassOrInterfaceDeclaration type,
            final String currentController,
            final LinkedHashSet<ControllerDependency> dependencies) {
        type.getExtendedTypes().forEach(extendedType -> {
            String parentType = extendedType.getNameAsString();
            if (!parentType.equals(currentController) && !"Object".equals(parentType)) {
                dependencies.add(new ControllerDependency(
                        DependencyKind.INHERITANCE,
                        parentType,
                        "extends:" + parentType));
            }
        });
    }

    private void addDynamicUiDependencies(
            final CompilationUnit cu,
            final LinkedHashSet<ControllerDependency> dependencies) {
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            addDynamicUiDependency(call, dependencies);
        }
    }

    private void addDynamicUiDependency(
            final MethodCallExpr call,
            final LinkedHashSet<ControllerDependency> dependencies) {
        String methodName = call.getNameAsString();
        String scope = call.getScope().map(Object::toString).orElse("");
        String fullCall = call.toString();
        String target = normalizeDynamicTarget(scope);
        if (target.isBlank()) {
            return;
        }
        if (methodName.equals("bind") && fullCall.contains(".managedProperty().bind(")
                && fullCall.contains(".visibleProperty()")) {
            dependencies.add(new ControllerDependency(
                    DependencyKind.DYNAMIC_UI_VISIBILITY,
                    target,
                    "managed-visible-binding"));
        } else if (methodName.equals("bind")) {
            dependencies.add(new ControllerDependency(
                    DependencyKind.DYNAMIC_UI_BINDING,
                    target,
                    "bind"));
        } else if (methodName.equals("addListener")) {
            dependencies.add(new ControllerDependency(
                    DependencyKind.DYNAMIC_UI_LISTENER,
                    target,
                    "addListener"));
        } else if (methodName.startsWith("setOn")) {
            dependencies.add(new ControllerDependency(
                    DependencyKind.DYNAMIC_UI_EVENT_HANDLER,
                    target,
                    methodName));
        }
    }

    private String normalizeDynamicTarget(final String rawTarget) {
        String target = rawTarget == null ? "" : rawTarget.trim();
        if (target.endsWith(".managedProperty()")) {
            target = target.substring(0, target.length() - ".managedProperty()".length());
        }
        return target;
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
