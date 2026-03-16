package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;

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

/**
 * Extraction de regles de gestion par analyse AST via JavaParser.
 * En cas d'echec du parsing AST, delegue au fallback regex.
 * Instancie par ClassificationConfiguration — pas d'annotation Spring.
 */
public final class JavaParserRuleExtractionAdapter implements RuleExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(JavaParserRuleExtractionAdapter.class);
    private static final String UNKNOWN_REF = "unknown";

    private final RuleExtractionPort fallback;

    public JavaParserRuleExtractionAdapter(final RuleExtractionPort fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
    }

    @Override
    public List<BusinessRule> extract(final String controllerRef, final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return List.of();
        }
        String ref = (controllerRef == null) ? UNKNOWN_REF : controllerRef;
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaContent);
            return extractFromAst(ref, cu);
        } catch (Exception e) {
            log.warn("JavaParser a echoue, delegation au fallback regex - ref={}", ref, e);
            return fallback.extract(controllerRef, javaContent);
        }
    }

    private List<BusinessRule> extractFromAst(final String ref, final CompilationUnit cu) {
        List<BusinessRule> rules = new ArrayList<>();
        List<String> injectedTypes = collectInjectedTypes(cu);
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
            BusinessRule rule = analyzeMethod(ref, method, injectedTypes, rules.size() + 1);
            rules.add(rule);
        }
        return List.copyOf(rules);
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
        String description = buildDescription(methodName, method, complexity);
        String ruleId = buildRuleId(ref, index);
        return new BusinessRule(ruleId, description, ref, sourceLine, rc, ec, uncertain);
    }

    private ResponsibilityClass classifyMethod(
            final boolean isFxml,
            final String bodyText,
            final List<String> injectedTypes) {
        if (isFxml && containsUiKeywords(bodyText)) {
            return ResponsibilityClass.UI;
        }
        if (containsTechnicalKeywords(bodyText)) {
            return ResponsibilityClass.TECHNICAL;
        }
        if (containsBusinessLogic(bodyText)) {
            return ResponsibilityClass.BUSINESS;
        }
        if (containsCoordination(bodyText, injectedTypes)) {
            return ResponsibilityClass.APPLICATION;
        }
        if (isFxml) {
            return ResponsibilityClass.UI;
        }
        return ResponsibilityClass.UNKNOWN;
    }

    private boolean containsUiKeywords(final String body) {
        String lower = body.toLowerCase();
        return lower.contains("settext") || lower.contains("setvisible")
                || lower.contains("getchildren") || lower.contains("setstyle")
                || lower.contains("setdisable") || lower.contains("getscene");
    }

    private boolean containsTechnicalKeywords(final String body) {
        String lower = body.toLowerCase();
        return lower.contains("resttemplate") || lower.contains("webclient")
                || lower.contains("httpurl") || lower.contains("socket")
                || lower.contains("file.write") || lower.contains("printjob");
    }

    private boolean containsBusinessLogic(final String body) {
        String lower = body.toLowerCase();
        return lower.contains("service.save") || lower.contains("repository")
                || lower.contains("persist") || lower.contains("entitymanager")
                || lower.contains("flush") || lower.contains("commit");
    }

    private boolean containsCoordination(final String body, final List<String> injectedTypes) {
        String lower = body.toLowerCase();
        boolean hasCoordWords = lower.contains("execute") || lower.contains("invoke")
                || lower.contains("usecase") || lower.contains("command")
                || lower.contains("dispatch");
        if (hasCoordWords) {
            return true;
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
            final int complexity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Methode ").append(methodName).append("(");
        method.getParameters().forEach(p -> {
            if (sb.charAt(sb.length() - 1) != '(') {
                sb.append(", ");
            }
            sb.append(p.getTypeAsString());
        });
        sb.append(")");
        method.getAnnotations().forEach(a -> sb.append(" @").append(a.getNameAsString()));
        sb.append(" [complexite=").append(complexity).append("]");
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
