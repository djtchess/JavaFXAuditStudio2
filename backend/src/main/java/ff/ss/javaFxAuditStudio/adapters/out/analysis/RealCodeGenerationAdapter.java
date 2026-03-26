package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.AssemblerGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.BridgeGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.GatewayGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.GeneratorUtils;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.PolicyGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.SlimControllerGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.StrategyArtifactGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.TestSkeletonGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.UseCaseGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.ViewModelGenerator;
import ff.ss.javaFxAuditStudio.application.generation.ArtifactGenerator;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrateur de generation d'artefacts.
 * Deleguea chaque generateur specialise selon le type d'ExtractionCandidate.
 * Integre la validation structurelle (JAS-009) apres chaque generation.
 * JAS-010 : bean Spring detectable par component-scan ; les generateurs sont injectes
 * via List{@literal <ArtifactGenerator>}. Le constructeur sans argument reste disponible
 * pour les tests unitaires hors contexte Spring.
 */
@Component
public final class RealCodeGenerationAdapter implements CodeGenerationPort {

    private final SlimControllerGenerator slimControllerGenerator;
    private final ViewModelGenerator viewModelGenerator;
    private final UseCaseGenerator useCaseGenerator;
    private final PolicyGenerator policyGenerator;
    private final GatewayGenerator gatewayGenerator;
    private final AssemblerGenerator assemblerGenerator;
    private final StrategyArtifactGenerator strategyArtifactGenerator;
    private final BridgeGenerator bridgeGenerator;
    private final TestSkeletonGenerator testSkeletonGenerator;
    private final ArtifactCompilabilityValidator compilabilityValidator;
    private final ImportDeduplicator importDeduplicator;

    /**
     * Constructeur de compatibilite pour les tests unitaires hors contexte Spring.
     * Instancie directement les generateurs sans injection.
     */
    public RealCodeGenerationAdapter() {
        this.slimControllerGenerator = new SlimControllerGenerator();
        this.viewModelGenerator = new ViewModelGenerator();
        this.useCaseGenerator = new UseCaseGenerator();
        this.policyGenerator = new PolicyGenerator();
        this.gatewayGenerator = new GatewayGenerator();
        this.assemblerGenerator = new AssemblerGenerator();
        this.strategyArtifactGenerator = new StrategyArtifactGenerator();
        this.bridgeGenerator = new BridgeGenerator();
        this.testSkeletonGenerator = new TestSkeletonGenerator();
        this.compilabilityValidator = new ArtifactCompilabilityValidator();
        this.importDeduplicator = new ImportDeduplicator();
    }

    /**
     * Constructeur Spring : les generateurs sont injectes depuis le contexte.
     * Extrait chaque generateur specialise de la liste par type de classe.
     * JAS-010 : remplace l'instanciation directe par injection via List{@literal <ArtifactGenerator>}.
     */
    @Autowired
    public RealCodeGenerationAdapter(final List<ArtifactGenerator> generators) {
        this.slimControllerGenerator = extractGenerator(generators, SlimControllerGenerator.class);
        this.viewModelGenerator = extractGenerator(generators, ViewModelGenerator.class);
        this.useCaseGenerator = extractGenerator(generators, UseCaseGenerator.class);
        this.policyGenerator = extractGenerator(generators, PolicyGenerator.class);
        this.gatewayGenerator = extractGenerator(generators, GatewayGenerator.class);
        this.assemblerGenerator = extractGenerator(generators, AssemblerGenerator.class);
        this.strategyArtifactGenerator = extractGenerator(generators, StrategyArtifactGenerator.class);
        this.bridgeGenerator = extractGenerator(generators, BridgeGenerator.class);
        this.testSkeletonGenerator = extractGenerator(generators, TestSkeletonGenerator.class);
        this.compilabilityValidator = new ArtifactCompilabilityValidator();
        this.importDeduplicator = new ImportDeduplicator();
    }

    private static <T extends ArtifactGenerator> T extractGenerator(
            final List<ArtifactGenerator> generators, final Class<T> type) {
        return generators.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ArtifactGenerator manquant : " + type.getSimpleName()));
    }

    @Override
    public List<CodeArtifact> generate(
            final String controllerRef,
            final String javaContent,
            final List<BusinessRule> classifiedRules) {
        String baseName = GeneratorUtils.extractBaseName(controllerRef);
        String pkg = GeneratorUtils.extractPackage(javaContent);
        List<CodeArtifact> artifacts = new ArrayList<>();

        Map<ExtractionCandidate, List<BusinessRule>> grouped = groupByCandidate(classifiedRules);

        List<BusinessRule> useCaseRules  = grouped.getOrDefault(ExtractionCandidate.USE_CASE, List.of());
        List<BusinessRule> viewModelRules = grouped.getOrDefault(ExtractionCandidate.VIEW_MODEL, List.of());
        List<BusinessRule> policyRules   = grouped.getOrDefault(ExtractionCandidate.POLICY, List.of());
        List<BusinessRule> gatewayRules  = grouped.getOrDefault(ExtractionCandidate.GATEWAY, List.of());
        List<BusinessRule> strategyRules = grouped.getOrDefault(ExtractionCandidate.STRATEGY, List.of());

        addValidated(artifacts, slimControllerGenerator.generate(baseName, pkg, useCaseRules));
        addValidated(artifacts, viewModelGenerator.generate(baseName, pkg, viewModelRules));
        addUseCaseIfPresent(artifacts, baseName, pkg, useCaseRules);
        addPolicyIfPresent(artifacts, baseName, pkg, policyRules);
        addGatewayIfPresent(artifacts, baseName, pkg, gatewayRules);
        addAssemblerIfPresent(artifacts, baseName, pkg, grouped);
        addStrategyIfPresent(artifacts, baseName, pkg, strategyRules);
        addBridgeIfNeeded(artifacts, baseName, pkg, classifiedRules);
        addTestSkeletonIfPresent(artifacts, baseName, pkg, classifiedRules);

        return List.copyOf(artifacts);
    }

    private Map<ExtractionCandidate, List<BusinessRule>> groupByCandidate(
            final List<BusinessRule> classifiedRules) {
        return classifiedRules.stream()
                .filter(r -> r.extractionCandidate() != ExtractionCandidate.NONE)
                .collect(Collectors.groupingBy(BusinessRule::extractionCandidate));
    }

    private void addValidated(final List<CodeArtifact> artifacts, final CodeArtifact artifact) {
        // JAS-009 : deduplication et promotion des hints avant validation
        CodeArtifact deduped = importDeduplicator.dedup(artifact);
        artifacts.add(compilabilityValidator.validate(deduped));
    }

    private void addUseCaseIfPresent(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final List<BusinessRule> rules) {
        if (!rules.isEmpty()) {
            addValidated(artifacts, useCaseGenerator.generate(baseName, pkg, rules));
        }
    }

    private void addPolicyIfPresent(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final List<BusinessRule> rules) {
        if (!rules.isEmpty()) {
            addValidated(artifacts, policyGenerator.generate(baseName, pkg, rules));
        }
    }

    private void addGatewayIfPresent(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final List<BusinessRule> rules) {
        if (!rules.isEmpty()) {
            addValidated(artifacts, gatewayGenerator.generate(baseName, pkg, rules));
        }
    }

    private void addAssemblerIfPresent(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final Map<ExtractionCandidate, List<BusinessRule>> grouped) {
        if (grouped.containsKey(ExtractionCandidate.ASSEMBLER)) {
            addValidated(artifacts, assemblerGenerator.generate(baseName, pkg));
        }
    }

    private void addStrategyIfPresent(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final List<BusinessRule> rules) {
        if (!rules.isEmpty()) {
            addValidated(artifacts, strategyArtifactGenerator.generate(baseName, pkg, rules));
        }
    }

    private void addBridgeIfNeeded(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final List<BusinessRule> classifiedRules) {
        List<BusinessRule> unknownRules = classifiedRules.stream()
                .filter(r -> r.extractionCandidate() == ExtractionCandidate.NONE)
                .toList();
        if (!unknownRules.isEmpty()) {
            addValidated(artifacts, bridgeGenerator.generate(baseName, pkg, classifiedRules));
        }
    }

    private void addTestSkeletonIfPresent(
            final List<CodeArtifact> artifacts,
            final String baseName,
            final String pkg,
            final List<BusinessRule> classifiedRules) {
        boolean hasTestableRules = classifiedRules.stream().anyMatch(r ->
                r.extractionCandidate() == ExtractionCandidate.USE_CASE
                || r.extractionCandidate() == ExtractionCandidate.POLICY);
        if (hasTestableRules) {
            addValidated(artifacts, testSkeletonGenerator.generate(baseName, pkg, classifiedRules));
        }
    }
}
