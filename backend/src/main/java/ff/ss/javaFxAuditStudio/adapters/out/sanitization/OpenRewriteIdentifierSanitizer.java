package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Sanitizer AST-based utilisant OpenRewrite pour neutraliser les declarations de classe
 * dont le nom contient un suffixe metier reconnaissable (JAS-018).
 *
 * <p>Fonctionnement :
 * <ol>
 *   <li>Tentative de parsing OpenRewrite du source Java passe en String.</li>
 *   <li>Si le parsing reussit, une {@link Recipe} inline visite chaque
 *       {@link J.ClassDeclaration} et renomme celles dont le nom correspond au pattern
 *       metier en {@code Neutralized_N}.</li>
 *   <li>Si OpenRewrite ne peut pas parser (classpath incomplet, syntaxe partielle, etc.),
 *       bascule silencieusement sur une regex appliquee aux declarations de classe
 *       ({@code class XxxService}, {@code class XxxManager}, …).</li>
 * </ol>
 *
 * <p><strong>Note sur le mode embedded</strong> : OpenRewrite necessite un classpath complet
 * pour resoudre les types. Pour du code isole (fichiers passes en String sans leurs
 * dependances), le parser signalera des avertissements de resolution de types et peut
 * retourner une liste vide. Dans ce cas le mode fallback regex prend le relais de facon
 * transparente, garantissant un comportement best-effort sans lever d'exception.
 *
 * <p>Ne jamais logger le contenu du source (securite).
 * Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class OpenRewriteIdentifierSanitizer implements Sanitizer {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRewriteIdentifierSanitizer.class);

    /**
     * Suffixes metier cibles — alignes sur ceux de {@code IdentifierSanitizer} pour
     * garantir une couverture coherente entre la passe AST et la passe regex.
     */
    private static final String BUSINESS_SUFFIXES =
            "Service|Manager|Controller|Repository|Gateway|Handler|Processor|Calculator|Engine";

    /**
     * Pattern pour les declarations de classe avec suffixe metier :
     * {@code class CustomerService}, {@code class InvoiceManager}, etc.
     * Capture le nom complet incluant le suffixe.
     */
    private static final Pattern CLASS_DECL_PATTERN = Pattern.compile(
            "\\bclass\\s+([A-Z][A-Za-z0-9_]*(?:" + BUSINESS_SUFFIXES + "))\\b");

    /**
     * Pattern equivalent au niveau de l'identifiant seul, pour les remplacements
     * en cascade apres la detection initiale (references au nom de classe dans le corps).
     */
    private static final Pattern BUSINESS_IDENTIFIER_PATTERN = Pattern.compile(
            "\\b([A-Z][a-z]+(?:[A-Z][a-z]+)*)(?:" + BUSINESS_SUFFIXES + ")\\b");

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

        AtomicInteger counter = new AtomicInteger(1);
        Map<String, String> renameMap = new HashMap<>();

        // Collecte les noms metier presents dans les ClassDeclarations
        collectBusinessClassNames(parsed, renameMap, counter, ctx);

        if (renameMap.isEmpty()) {
            // Aucun identifiant metier detecte par l'AST — rien a transformer
            occurrenceCount = 0;
            return source;
        }

        // Applique la recipe de renommage
        NeutralizeClassNamesRecipe recipe = new NeutralizeClassNamesRecipe(renameMap);
        List<SourceFile> result = recipe.run(new InMemoryLargeSourceSet(parsed), ctx)
                .getChangeset().getAllResults()
                .stream()
                .map(r -> r.getAfter())
                .filter(Objects::nonNull)
                .toList();

        occurrenceCount = renameMap.size();

        if (result.isEmpty()) {
            // La recipe n'a produit aucun changement (noms deja neutres ou non detectes)
            return source;
        }

        // Retourne le premier source transforme (on traite un fichier a la fois)
        return result.get(0).printAll();
    }

    /**
     * Collecte les noms de {@link J.ClassDeclaration} correspondant au pattern metier,
     * sans modifier l'AST, afin de construire la {@code renameMap}.
     */
    private void collectBusinessClassNames(
            final List<SourceFile> sources,
            final Map<String, String> renameMap,
            final AtomicInteger counter,
            final ExecutionContext ctx) {

        Pattern suffixPattern = Pattern.compile(
                "^[A-Z][A-Za-z0-9_]*(?:" + BUSINESS_SUFFIXES + ")$");

        for (SourceFile sf : sources) {
            if (sf instanceof J.CompilationUnit cu) {
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(
                            final J.ClassDeclaration classDecl,
                            final ExecutionContext execCtx) {
                        String name = classDecl.getSimpleName();
                        if (suffixPattern.matcher(name).matches()) {
                            renameMap.computeIfAbsent(
                                    name, k -> "Neutralized_" + counter.getAndIncrement());
                        }
                        return super.visitClassDeclaration(classDecl, execCtx);
                    }
                }.visit(cu, ctx);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Mode fallback regex (declarations de classe uniquement)
    // -------------------------------------------------------------------------

    /**
     * Fallback regex cible les declarations {@code class XxxSuffix} et les references
     * a ces noms dans le meme source, en preservant la coherence des remplacements.
     */
    private String applyRegexFallback(final String source) {
        // Phase 1 : identifier tous les noms de classes metier declares
        Map<String, String> renameMap = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(1);

        Matcher declMatcher = CLASS_DECL_PATTERN.matcher(source);
        while (declMatcher.find()) {
            String originalName = declMatcher.group(1);
            renameMap.computeIfAbsent(
                    originalName, k -> "Neutralized_" + counter.getAndIncrement());
        }

        if (renameMap.isEmpty()) {
            occurrenceCount = 0;
            return source;
        }

        // Phase 2 : remplacer toutes les occurrences de ces noms dans le source
        // (declarations ET references)
        String result = source;
        int totalReplacements = 0;
        for (Map.Entry<String, String> entry : renameMap.entrySet()) {
            Pattern namePattern = Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\b");
            Matcher m = namePattern.matcher(result);
            StringBuffer sb = new StringBuffer();
            int count = 0;
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(entry.getValue()));
                count++;
            }
            m.appendTail(sb);
            result = sb.toString();
            totalReplacements += count;
        }

        occurrenceCount = totalReplacements;
        return result;
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
     * Recipe OpenRewrite composite qui renomme les declarations de classe dont
     * le nom figure dans la {@code renameMap}.
     *
     * <p>Utilise un {@link JavaIsoVisitor} pour visiter les {@link J.ClassDeclaration}
     * et remplace le nom simple par la valeur neutre correspondante.
     */
    private static final class NeutralizeClassNamesRecipe extends Recipe {

        private final Map<String, String> renameMap;

        NeutralizeClassNamesRecipe(final Map<String, String> renameMap) {
            this.renameMap = Map.copyOf(renameMap);
        }

        @Override
        public String getDisplayName() {
            return "Neutralize business class names";
        }

        @Override
        public String getDescription() {
            return "Renames class declarations whose names contain a business suffix "
                    + "(Service, Manager, Controller, etc.) to generic neutral names.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {
                @Override
                public J.Identifier visitIdentifier(
                        final J.Identifier ident,
                        final ExecutionContext ctx) {
                    J.Identifier visited = super.visitIdentifier(ident, ctx);
                    String neutralName = renameMap.get(visited.getSimpleName());
                    // Renomme toutes les occurrences de l'identifiant metier :
                    // declaration de classe, references de type, new ClassName(), etc.
                    return neutralName != null ? visited.withSimpleName(neutralName) : visited;
                }
            };
        }
    }
}
