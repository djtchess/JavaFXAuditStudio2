package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.AssemblerGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.BridgeGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.GatewayGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.GeneratorUtils;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.PolicyGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.SlimControllerGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.StrategyArtifactGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.UseCaseGenerator;
import ff.ss.javaFxAuditStudio.adapters.out.analysis.generators.ViewModelGenerator;
import ff.ss.javaFxAuditStudio.application.ports.out.CodeGenerationPort;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrateur de generation d'artefacts.
 * Deleguea chaque generateur specialise selon le type d'ExtractionCandidate.
 * Integre la validation structurelle (JAS-009) apres chaque generation.
 * Instancie par GenerationConfiguration — pas d'annotation Spring.
 */
public final class RealCodeGenerationAdapter implements CodeGenerationPort {

    private final SlimControllerGenerator slimControllerGenerator;
    private final ViewModelGenerator viewModelGenerator;
    private final UseCaseGenerator useCaseGenerator;
    private final PolicyGenerator policyGenerator;
    private final GatewayGenerator gatewayGenerator;
    private final AssemblerGenerator assemblerGenerator;
    private final StrategyArtifactGenerator strategyArtifactGenerator;
    private final BridgeGenerator bridgeGenerator;
    private final ArtifactCompilabilityValidator compilabilityValidator;

    public RealCodeGenerationAdapter() {
        this.slimControllerGenerator = new SlimControllerGenerator();
        this.viewModelGenerator = new ViewModelGenerator();
        this.useCaseGenerator = new UseCaseGenerator();
        this.policyGenerator = new PolicyGenerator();
        this.gatewayGenerator = new GatewayGenerator();
        this.assemblerGenerator = new AssemblerGenerator();
        this.strategyArtifactGenerator = new StrategyArtifactGenerator();
        this.bridgeGenerator = new BridgeGenerator();
        this.compilabilityValidator = new ArtifactCompilabilityValidator();
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

        return List.copyOf(artifacts);
    }

    private Map<ExtractionCandidate, List<BusinessRule>> groupByCandidate(
            final List<BusinessRule> classifiedRules) {
        return classifiedRules.stream()
                .filter(r -> r.extractionCandidate() != ExtractionCandidate.NONE)
                .collect(Collectors.groupingBy(BusinessRule::extractionCandidate));
    }

    private void addValidated(final List<CodeArtifact> artifacts, final CodeArtifact artifact) {
        artifacts.add(compilabilityValidator.validate(artifact));
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
}
