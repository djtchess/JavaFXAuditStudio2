package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
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
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    private final RuleExtractionPort fallback;
    private final Set<String> lifecycleExcluded;
    private final AnalysisProperties.ClassificationPatterns patterns;

    public JavaParserRuleExtractionAdapter(final RuleExtractionPort fallback) {
        this(fallback, Set.of(),
                new AnalysisProperties.ClassificationPatterns(null, null, null, null, null));
    }

    public JavaParserRuleExtractionAdapter(
            final RuleExtractionPort fallback,
            final Set<String> lifecycleExcluded) {
        this(fallback, lifecycleExcluded,
                new AnalysisProperties.ClassificationPatterns(null, null, null, null, null));
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
            List<BusinessRule> rules = extractFromAst(ref, cu);
            return ExtractionResult.ast(rules);
        } catch (Exception e) {
            String causeMessage = buildCauseMessage(ref, e);
            return ExtractionResult.regexFallback(
                    fallback.extract(controllerRef, javaContent).rules(),
                    causeMessage);
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

    private List<BusinessRule> extractFromAst(final String ref, final CompilationUnit cu) {
        List<BusinessRule> rules = new ArrayList<>();
        List<String> injectedTypes = collectInjectedTypes(cu);
        addFxmlFieldRules(ref, cu, rules);
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
            if (lifecycleExcluded.contains(method.getNameAsString())) {
                log.debug("Methode lifecycle ignoree - ref={}, methode={}", ref, method.getNameAsString());
                continue;
            }
            BusinessRule rule = analyzeMethod(ref, method, injectedTypes, rules.size() + 1);
            rules.add(rule);
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
        ResponsibilityClass rc = classifyMethod(isFxml, bodyText, injectedTypes);
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
        if (isFxml && containsUiKeywords(bodyText)) {
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
}
