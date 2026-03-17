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

    public RealCodeGenerationAdapter() {
        this.slimControllerGenerator = new SlimControllerGenerator();
        this.viewModelGenerator = new ViewModelGenerator();
        this.useCaseGenerator = new UseCaseGenerator();
        this.policyGenerator = new PolicyGenerator();
        this.gatewayGenerator = new GatewayGenerator();
        this.assemblerGenerator = new AssemblerGenerator();
        this.strategyArtifactGenerator = new StrategyArtifactGenerator();
        this.bridgeGenerator = new BridgeGenerator();
    }

    @Override
    public List<CodeArtifact> generate(
            final String controllerRef,
            final String javaContent,
            final List<BusinessRule> classifiedRules) {
        String baseName = GeneratorUtils.extractBaseName(controllerRef);
        String pkg = GeneratorUtils.extractPackage(javaContent);
        List<CodeArtifact> artifacts = new ArrayList<>();

        Map<ExtractionCandidate, List<BusinessRule>> grouped = classifiedRules.stream()
                .filter(r -> r.extractionCandidate() != ExtractionCandidate.NONE)
                .collect(Collectors.groupingBy(BusinessRule::extractionCandidate));

        List<BusinessRule> useCaseRules  = grouped.getOrDefault(ExtractionCandidate.USE_CASE, List.of());
        List<BusinessRule> viewModelRules = grouped.getOrDefault(ExtractionCandidate.VIEW_MODEL, List.of());
        List<BusinessRule> policyRules   = grouped.getOrDefault(ExtractionCandidate.POLICY, List.of());
        List<BusinessRule> gatewayRules  = grouped.getOrDefault(ExtractionCandidate.GATEWAY, List.of());
        List<BusinessRule> strategyRules = grouped.getOrDefault(ExtractionCandidate.STRATEGY, List.of());

        // Lot 1 — Diagnostic
        artifacts.add(slimControllerGenerator.generate(baseName, pkg, useCaseRules));

        // Lot 2 — ViewModel + UseCase
        artifacts.add(viewModelGenerator.generate(baseName, pkg, viewModelRules));
        if (!useCaseRules.isEmpty()) {
            artifacts.add(useCaseGenerator.generate(baseName, pkg, useCaseRules));
        }

        // Lot 3 — Policies
        if (!policyRules.isEmpty()) {
            artifacts.add(policyGenerator.generate(baseName, pkg, policyRules));
        }

        // Lot 4 — Gateways
        if (!gatewayRules.isEmpty()) {
            artifacts.add(gatewayGenerator.generate(baseName, pkg, gatewayRules));
        }

        // Lot 5 — Assembler + Strategy
        if (grouped.containsKey(ExtractionCandidate.ASSEMBLER)) {
            artifacts.add(assemblerGenerator.generate(baseName, pkg));
        }
        if (!strategyRules.isEmpty()) {
            artifacts.add(strategyArtifactGenerator.generate(baseName, pkg, strategyRules));
        }

        // Bridge si regles non classifiees
        List<BusinessRule> unknownRules = classifiedRules.stream()
                .filter(r -> r.extractionCandidate() == ExtractionCandidate.NONE)
                .toList();
        if (!unknownRules.isEmpty()) {
            artifacts.add(bridgeGenerator.generate(baseName, pkg, classifiedRules));
        }

        return List.copyOf(artifacts);
    }
}
