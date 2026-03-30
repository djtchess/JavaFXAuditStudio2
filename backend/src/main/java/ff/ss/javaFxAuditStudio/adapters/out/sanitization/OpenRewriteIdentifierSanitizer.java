package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Sanitizer AST-based utilisant OpenRewrite pour neutraliser les identifiants metier
 * dans du code Java avant envoi au LLM (JAS-018).
 *
 * <p>Couvre :
 * <ol>
 *   <li><strong>Classes</strong> : renomme les declarations de classe dont le nom contient
 *       un suffixe metier reconnu en {@code Neutralized_N}.</li>
 *   <li><strong>Methodes metier</strong> (5a) : renomme les methodes publiques/protected dont
 *       le nom contient un terme metier en {@code processOperation_N}.
 *       Les methodes du cycle de vie ({@code initialize}, {@code start}, {@code stop}, etc.)
 *       sont preservees.</li>
 *   <li><strong>Champs d'entites</strong> (5b) : renomme les champs annotes {@code @Column},
 *       {@code @Id}, {@code @ManyToOne}, {@code @OneToMany}, {@code @ManyToMany},
 *       {@code @OneToOne} dont le nom contient un terme metier en {@code field_N}.</li>
 *   <li><strong>Packages organisationnels</strong> (5c) : remplace dans les declarations
 *       {@code package} et {@code import} les segments d'organisation sensibles
 *       (cnamts, ameli, cpam, etc.) par {@code com.neutralized}.</li>
 * </ol>
 *
 * <p>Fonctionnement best-effort :
 * <ol>
 *   <li>Tentative de parsing OpenRewrite du source Java passe en String.</li>
 *   <li>Si le parsing reussit, une {@link Recipe} inline visite l'AST.</li>
 *   <li>Si OpenRewrite ne peut pas parser (classpath incomplet, syntaxe partielle, etc.),
 *       bascule silencieusement sur des regexes appliquees au texte brut.</li>
 * </ol>
 *
 * <p>Ne jamais logger le contenu du source (securite).
 * Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class OpenRewriteIdentifierSanitizer implements Sanitizer {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRewriteIdentifierSanitizer.class);

    /** Pattern pour les declarations de classe avec suffixe metier (fallback). */
    private static final Pattern CLASS_DECL_PATTERN =
            BusinessTermDictionary.CLASS_DECLARATION_PATTERN;

    /** Pattern pour les declarations de methode publique/protected (fallback 5a). */
    private static final Pattern METHOD_DECL_PATTERN =
            BusinessTermDictionary.METHOD_DECLARATION_PATTERN;

    /** Annotations JPA/Jakarta signalant un champ d'entite sensible (5b). */
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "@Column", "@Id", "@ManyToOne", "@OneToMany", "@ManyToMany", "@OneToOne",
            "@JoinColumn", "@EmbeddedId", "@Embedded"
    );

    /** Pattern pour les annotations JPA sur une ligne (fallback 5b). */
    private static final Pattern JPA_ANNOTATION_PATTERN = Pattern.compile(
            "@(?:Column|Id|ManyToOne|OneToMany|ManyToMany|OneToOne|JoinColumn|EmbeddedId|Embedded)"
            + "(?:\\([^)]*\\))?");

    /** Pattern pour une declaration de champ apres une annotation JPA (fallback 5b). */
    private static final Pattern FIELD_AFTER_ANNOTATION_PATTERN = Pattern.compile(
            "(?m)^(\\s*(?:private|protected|public)?\\s+[\\w<>\\[\\]]+\\s+)([a-z][A-Za-z0-9_]*)\\s*;");

    /** Pattern pour les declarations de package (5c). */
    private static final Pattern PACKAGE_DECL_PATTERN = Pattern.compile(
            "^(\\s*package\\s+)([\\w.]+)(\\s*;)", Pattern.MULTILINE);

    /** Pattern pour les declarations d'import (5c). */
    private static final Pattern IMPORT_DECL_PATTERN = Pattern.compile(
            "^(\\s*import\\s+(?:static\\s+)?)([\\w.]+\\*?)(\\s*;)", Pattern.MULTILINE);

    private int occurrenceCount;

    @Override
    public String apply(final String source) {
        occurrenceCount = 0;
        if (source == null || source.isBlank()) {
            return source;
        }
        try {
            return applyOpenRewrite(source);
        } catch (Exception e) {
            LOG.warn("OpenRewriteIdentifierSanitizer : echec parsing AST, bascule sur regex ({})",
                    e.getMessage());
            return applyRegexFallback(source);
        }
    }

    // -------------------------------------------------------------------------
    // Mode AST OpenRewrite
    // -------------------------------------------------------------------------

    private String applyOpenRewrite(final String source) {
        ExecutionContext ctx = new InMemoryExecutionContext(
                t -> LOG.debug("OpenRewrite context warning: {}", t.getMessage()));

        JavaParser parser = JavaParser.fromJavaVersion().build();
        List<SourceFile> parsed = parser.parse(source).toList();

        if (parsed.isEmpty()) {
            LOG.debug("OpenRewriteIdentifierSanitizer : parsing vide, bascule sur regex");
            return applyRegexFallback(source);
        }

        AtomicInteger classCounter = new AtomicInteger(1);
        AtomicInteger methodCounter = new AtomicInteger(1);
        AtomicInteger fieldCounter = new AtomicInteger(1);
        Map<String, String> classRenameMap = new HashMap<>();
        Map<String, String> methodRenameMap = new HashMap<>();
        Map<String, String> fieldRenameMap = new HashMap<>();

        collectBusinessNames(parsed, classRenameMap, methodRenameMap, fieldRenameMap,
                classCounter, methodCounter, fieldCounter, ctx);

        int totalMappings = classRenameMap.size() + methodRenameMap.size() + fieldRenameMap.size();
        if (totalMappings == 0) {
            occurrenceCount = 0;
            // Applique tout de meme la sanitisation de packages (5c) qui ne passe pas par AST
            return sanitizePackages(source);
        }

        NeutralizeBusinessNamesRecipe recipe = new NeutralizeBusinessNamesRecipe(
                classRenameMap, methodRenameMap, fieldRenameMap);
        List<SourceFile> result = recipe.run(new InMemoryLargeSourceSet(parsed), ctx)
                .getChangeset().getAllResults()
                .stream()
                .map(r -> r.getAfter())
                .filter(Objects::nonNull)
                .toList();

        occurrenceCount = totalMappings;

        String transformed = result.isEmpty() ? source : result.get(0).printAll();
        return sanitizePackages(transformed);
    }

    /**
     * Collecte les noms metier dans les ClassDeclarations, MethodDeclarations et
     * VariableDeclarations, sans modifier l'AST.
     */
    private void collectBusinessNames(
            final List<SourceFile> sources,
            final Map<String, String> classRenameMap,
            final Map<String, String> methodRenameMap,
            final Map<String, String> fieldRenameMap,
            final AtomicInteger classCounter,
            final AtomicInteger methodCounter,
            final AtomicInteger fieldCounter,
            final ExecutionContext ctx) {

        Pattern classSuffixPattern = BusinessTermDictionary.CLASS_NAME_PATTERN;
        Set<String> methodTerms = BusinessTermDictionary.BUSINESS_METHOD_TERMS;
        Set<String> lifecycleMethods = BusinessTermDictionary.LIFECYCLE_METHODS;
        List<String> suffixes = BusinessTermDictionary.BUSINESS_SUFFIXES;

        for (SourceFile sf : sources) {
            if (!(sf instanceof J.CompilationUnit cu)) {
                continue;
            }
            new JavaIsoVisitor<ExecutionContext>() {

                @Override
                public J.ClassDeclaration visitClassDeclaration(
                        final J.ClassDeclaration classDecl,
                        final ExecutionContext execCtx) {
                    String name = classDecl.getSimpleName();
                    if (classSuffixPattern.matcher(name).matches()) {
                        classRenameMap.computeIfAbsent(
                                name, k -> "Neutralized_" + classCounter.getAndIncrement());
                    }
                    return super.visitClassDeclaration(classDecl, execCtx);
                }

                @Override
                public J.MethodDeclaration visitMethodDeclaration(
                        final J.MethodDeclaration method,
                        final ExecutionContext execCtx) {
                    String name = method.getSimpleName();
                    if (isBusinessMethodName(name, methodTerms, suffixes, lifecycleMethods)) {
                        methodRenameMap.computeIfAbsent(
                                name, k -> "processOperation_" + methodCounter.getAndIncrement());
                    }
                    return super.visitMethodDeclaration(method, execCtx);
                }

                @Override
                public J.VariableDeclarations visitVariableDeclarations(
                        final J.VariableDeclarations varDecl,
                        final ExecutionContext execCtx) {
                    if (hasEntityAnnotation(varDecl)) {
                        for (J.VariableDeclarations.NamedVariable namedVar : varDecl.getVariables()) {
                            String fieldName = namedVar.getSimpleName();
                            if (containsBusinessTerm(fieldName, methodTerms, suffixes)) {
                                fieldRenameMap.computeIfAbsent(
                                        fieldName,
                                        k -> "field_" + fieldCounter.getAndIncrement());
                            }
                        }
                    }
                    return super.visitVariableDeclarations(varDecl, execCtx);
                }

            }.visit(cu, ctx);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers semantiques
    // -------------------------------------------------------------------------

    /**
     * Determine si un nom de methode est metier.
     *
     * <p>Une methode est metier si :
     * <ul>
     *   <li>son nom en minuscules contient un terme metier connu, OU</li>
     *   <li>son nom se termine par un suffixe metier reconnu.</li>
     * </ul>
     * Les methodes du cycle de vie sont exclues.
     */
    private boolean isBusinessMethodName(
            final String name,
            final Set<String> terms,
            final List<String> suffixes,
            final Set<String> lifecycle) {
        if (lifecycle.contains(name)) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (lower.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifie si au moins une annotation JPA/Jakarta qualifiant une entite est presente
     * sur la declaration de variable.
     */
    private boolean hasEntityAnnotation(final J.VariableDeclarations varDecl) {
        return varDecl.getLeadingAnnotations().stream()
                .map(a -> "@" + a.getSimpleName())
                .anyMatch(ENTITY_ANNOTATIONS::contains);
    }

    /**
     * Verifie si un nom de champ contient un terme metier ou un suffixe metier.
     */
    private boolean containsBusinessTerm(
            final String name,
            final Set<String> terms,
            final List<String> suffixes) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (lower.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String suffix : suffixes) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Sanitisation de packages (5c) — appliquee apres AST ou en fallback
    // -------------------------------------------------------------------------

    /**
     * Remplace les segments de package organisationnels sensibles dans les declarations
     * {@code package} et {@code import}.
     *
     * <p>Ne touche pas aux packages "safe" (java.*, javax.*, jakarta.*, org.springframework.*, etc.).
     */
    private String sanitizePackages(final String source) {
        Set<String> sensitiveSegments = BusinessTermDictionary.SENSITIVE_PACKAGE_SEGMENTS;
        Set<String> safePrefixes = BusinessTermDictionary.SAFE_PACKAGE_PREFIXES;

        String result = replaceInPackageDeclarations(source, sensitiveSegments, safePrefixes);
        return replaceInImportDeclarations(result, sensitiveSegments, safePrefixes);
    }

    private String replaceInPackageDeclarations(
            final String source,
            final Set<String> sensitiveSegments,
            final Set<String> safePrefixes) {
        Matcher m = PACKAGE_DECL_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String packageName = m.group(2);
            if (isSafePackage(packageName, safePrefixes)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            } else {
                String neutralized = neutralizePackageName(packageName, sensitiveSegments);
                m.appendReplacement(sb,
                        Matcher.quoteReplacement(m.group(1) + neutralized + m.group(3)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replaceInImportDeclarations(
            final String source,
            final Set<String> sensitiveSegments,
            final Set<String> safePrefixes) {
        Matcher m = IMPORT_DECL_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String importedName = m.group(2);
            if (isSafePackage(importedName, safePrefixes)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            } else {
                String neutralized = neutralizePackageName(importedName, sensitiveSegments);
                m.appendReplacement(sb,
                        Matcher.quoteReplacement(m.group(1) + neutralized + m.group(3)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean isSafePackage(final String packageName, final Set<String> safePrefixes) {
        for (String safe : safePrefixes) {
            if (packageName.startsWith(safe) || packageName.equals(safe.stripTrailing().replace(".", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remplace les segments sensibles d'un nom de package par {@code com.neutralized}.
     *
     * <p>Si le package contient un segment sensible, tout le prefixe jusqu'a ce segment
     * est remplace par {@code com.neutralized}. Les segments restants (sous-packages
     * fonctionnels) sont conserves.
     */
    private String neutralizePackageName(
            final String packageName,
            final Set<String> sensitiveSegments) {
        String[] parts = packageName.split("\\.");
        StringBuilder sb = new StringBuilder();
        boolean neutralized = false;
        for (int i = 0; i < parts.length; i++) {
            if (!neutralized && sensitiveSegments.contains(parts[i].toLowerCase(Locale.ROOT))) {
                sb.setLength(0);
                sb.append("com.neutralized");
                neutralized = true;
            } else if (neutralized) {
                sb.append(".").append(parts[i]);
            } else {
                if (i > 0) {
                    sb.append(".");
                }
                sb.append(parts[i]);
            }
        }
        return neutralized ? sb.toString() : packageName;
    }

    // -------------------------------------------------------------------------
    // Mode fallback regex
    // -------------------------------------------------------------------------

    /**
     * Fallback regex pour les cas ou OpenRewrite ne peut pas parser le source.
     *
     * <p>Couvre :
     * <ul>
     *   <li>Declarations de classe metier (renommage en Neutralized_N).</li>
     *   <li>Declarations de methode metier publique/protected (renommage en processOperation_N).</li>
     *   <li>Packages organisationnels (neutralisation en com.neutralized).</li>
     * </ul>
     */
    private String applyRegexFallback(final String source) {
        Map<String, String> classRenameMap = new HashMap<>();
        Map<String, String> methodRenameMap = new HashMap<>();
        AtomicInteger classCounter = new AtomicInteger(1);
        AtomicInteger methodCounter = new AtomicInteger(1);

        collectClassNamesFallback(source, classRenameMap, classCounter);
        collectMethodNamesFallback(source, methodRenameMap, methodCounter);

        String result = applyRenameFallback(source, classRenameMap, methodRenameMap);
        result = sanitizePackages(result);

        occurrenceCount = classRenameMap.size() + methodRenameMap.size();
        return result;
    }

    private void collectClassNamesFallback(
            final String source,
            final Map<String, String> classRenameMap,
            final AtomicInteger counter) {
        Matcher m = CLASS_DECL_PATTERN.matcher(source);
        while (m.find()) {
            String name = m.group(1);
            classRenameMap.computeIfAbsent(name, k -> "Neutralized_" + counter.getAndIncrement());
        }
    }

    private void collectMethodNamesFallback(
            final String source,
            final Map<String, String> methodRenameMap,
            final AtomicInteger counter) {
        Set<String> terms = BusinessTermDictionary.BUSINESS_METHOD_TERMS;
        List<String> suffixes = BusinessTermDictionary.BUSINESS_SUFFIXES;
        Set<String> lifecycle = BusinessTermDictionary.LIFECYCLE_METHODS;

        Matcher m = METHOD_DECL_PATTERN.matcher(source);
        while (m.find()) {
            String name = m.group(1);
            if (isBusinessMethodName(name, terms, suffixes, lifecycle)) {
                methodRenameMap.computeIfAbsent(
                        name, k -> "processOperation_" + counter.getAndIncrement());
            }
        }
    }

    private String applyRenameFallback(
            final String source,
            final Map<String, String> classRenameMap,
            final Map<String, String> methodRenameMap) {
        String result = source;
        for (Map.Entry<String, String> entry : classRenameMap.entrySet()) {
            result = replaceWordBoundary(result, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : methodRenameMap.entrySet()) {
            result = replaceWordBoundary(result, entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String replaceWordBoundary(
            final String source,
            final String original,
            final String replacement) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(original) + "\\b");
        Matcher m = p.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Rapport
    // -------------------------------------------------------------------------

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.OPENREWRITE_REMEDIATION,
                occurrenceCount,
                "OpenRewrite : " + occurrenceCount + " identifiant(s) neutralise(s)");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.OPENREWRITE_REMEDIATION;
    }

    // -------------------------------------------------------------------------
    // Recipe inline pour le renommage AST
    // -------------------------------------------------------------------------

    /**
     * Recipe OpenRewrite composite qui renomme les classes, methodes et champs metier.
     *
     * <p>Visite les {@link J.Identifier} en les remplacant par leur equivalent neutre
     * si present dans l'une des maps. La granularite se fait au niveau de l'identifiant
     * simple pour couvrir declarations ET references.
     */
    private static final class NeutralizeBusinessNamesRecipe extends Recipe {

        private final Map<String, String> classRenameMap;
        private final Map<String, String> methodRenameMap;
        private final Map<String, String> fieldRenameMap;

        NeutralizeBusinessNamesRecipe(
                final Map<String, String> classRenameMap,
                final Map<String, String> methodRenameMap,
                final Map<String, String> fieldRenameMap) {
            this.classRenameMap = Map.copyOf(classRenameMap);
            this.methodRenameMap = Map.copyOf(methodRenameMap);
            this.fieldRenameMap = Map.copyOf(fieldRenameMap);
        }

        @Override
        public String getDisplayName() {
            return "Neutralize business identifiers (classes, methods, fields)";
        }

        @Override
        public String getDescription() {
            return "Renames class declarations, business method names and entity fields "
                    + "whose names contain business suffixes or terms to generic neutral names.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {

                @Override
                public J.MethodDeclaration visitMethodDeclaration(
                        final J.MethodDeclaration method,
                        final ExecutionContext ctx) {
                    J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                    String neutralName = methodRenameMap.get(m.getSimpleName());
                    if (neutralName != null) {
                        m = m.withName(m.getName().withSimpleName(neutralName));
                    }
                    return m;
                }

                @Override
                public J.VariableDeclarations visitVariableDeclarations(
                        final J.VariableDeclarations varDecl,
                        final ExecutionContext ctx) {
                    J.VariableDeclarations vd = super.visitVariableDeclarations(varDecl, ctx);
                    boolean hasEntityAnnotation = vd.getLeadingAnnotations().stream()
                            .map(a -> "@" + a.getSimpleName())
                            .anyMatch(name -> Set.of(
                                    "@Column", "@Id", "@ManyToOne", "@OneToMany",
                                    "@ManyToMany", "@OneToOne", "@JoinColumn",
                                    "@EmbeddedId", "@Embedded").contains(name));
                    if (!hasEntityAnnotation) {
                        return vd;
                    }
                    List<J.VariableDeclarations.NamedVariable> renamed = vd.getVariables().stream()
                            .map(namedVar -> {
                                String neutral = fieldRenameMap.get(namedVar.getSimpleName());
                                if (neutral == null) {
                                    return namedVar;
                                }
                                J.Identifier newIdent = namedVar.getName()
                                        .withSimpleName(neutral);
                                return namedVar.withName(newIdent);
                            })
                            .toList();
                    return vd.withVariables(renamed);
                }

                @Override
                public J.Identifier visitIdentifier(
                        final J.Identifier ident,
                        final ExecutionContext ctx) {
                    J.Identifier visited = super.visitIdentifier(ident, ctx);
                    String name = visited.getSimpleName();
                    String neutralClass = classRenameMap.get(name);
                    if (neutralClass != null) {
                        return visited.withSimpleName(neutralClass);
                    }
                    return visited;
                }
            };
        }
    }
}
