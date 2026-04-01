package ff.ss.javaFxAuditStudio.application.service;

import ff.ss.javaFxAuditStudio.configuration.AnalysisProperties;
import ff.ss.javaFxAuditStudio.domain.analysis.ControllerFlowAnalysis;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDependencyGraph;
import ff.ss.javaFxAuditStudio.domain.analysis.ProjectDeltaAnalysis;
import ff.ss.javaFxAuditStudio.domain.ingestion.SourceInput;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ControllerAnalysisSupport {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?m)^\\s*(?:private|protected|public)\\s+(?:static\\s+)?([\\w<>.?]+)\\s+(\\w+)\\s*(?:=|;)");
    private static final Pattern INJECTED_FIELD_PATTERN = Pattern.compile(
            "(?m)(?:@Autowired|@Inject)\\s*(?:\\R\\s*)*(?:private|protected|public)\\s+"
                    + "(?:static\\s+)?([\\w<>.?]+)\\s+(\\w+)\\s*(?:=|;)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)^(\\s*(?:@FXML\\s+)?)\\s*(?:public|private|protected)\\s+"
                    + "(?:static\\s+)?([\\w<>.?\\[\\]]+)\\s+(\\w+)\\s*\\([^\\)]*\\)\\s*\\{");
    private static final Pattern BOOLEAN_METHOD_PATTERN = Pattern.compile(
            "(?m)^\\s*(?:@FXML\\s+)?(?:public|private|protected)\\s+(?:static\\s+)?(?:boolean|Boolean)\\s+(\\w+)\\s*\\(");
    private static final Pattern CLASS_SIGNATURE_PATTERN = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?(?:abstract\\s+)?class\\s+\\w+\\s*"
                    + "(?:extends\\s+([\\w.]+))?\\s*(?:implements\\s+([^\\{]+))?\\s*\\{");
    private static final Pattern OVERRIDE_METHOD_PATTERN = Pattern.compile(
            "(?m)@Override\\s*(?:\\R\\s*)*(?:public|private|protected)\\s+"
                    + "(?:static\\s+)?(?:[\\w<>.?\\[\\]]+)\\s+(\\w+)\\s*\\(");
    private static final Pattern SUPER_CALL_PATTERN = Pattern.compile("\\bsuper\\.(\\w+)\\s*\\(");
    private static final Pattern PROPERTY_BINDING_PATTERN = Pattern.compile(
            "(?m)^.*(?:\\b\\w+Property\\(\\)\\.(?:bind|bindBidirectional)|\\b\\w+\\.(?:bind|bindBidirectional))\\([^;]+;\\s*$");
    private static final Pattern LISTENER_PATTERN = Pattern.compile(
            "(?m)^.*\\.addListener\\([^;]+;\\s*$");
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "(?m)^.*\\.setOn[A-Z]\\w*\\([^;]+;\\s*$");
    private static final Pattern DYNAMIC_NODE_MUTATION_PATTERN = Pattern.compile(
            "(?m)^.*(?:getChildren\\(\\)\\.(?:add|addAll)|setItems\\(|setCellFactory\\(|setRowFactory\\(|new\\s+[A-Z][A-Za-z0-9_]*\\s*\\().*;\\s*$");
    private static final Pattern CASE_PATTERN = Pattern.compile("\\bcase\\s+([A-Z0-9_]+)\\b");
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
            "\\b(?:mode|state|phase|step|screen|view|status)\\s*=\\s*([A-Z0-9_]+)\\b");
    private static final Pattern CONTROLLER_CALL_PATTERN = Pattern.compile("\\bnew\\s+([A-Z][A-Za-z0-9_]*Controller)\\s*\\(");

    private ControllerAnalysisSupport() {
    }

    static String controllerName(final String ref) {
        if (ref == null || ref.isBlank()) {
            return "unknown";
        }
        int lastSlash = Math.max(ref.lastIndexOf('/'), ref.lastIndexOf('\\'));
        String name = lastSlash >= 0 ? ref.substring(lastSlash + 1) : ref;
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }

    static String readSource(
            final String ref,
            final ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort sourceReaderPort) {
        if (ref == null || ref.isBlank() || sourceReaderPort == null) {
            return "";
        }
        return sourceReaderPort.read(ref).map(SourceInput::content).orElse("");
    }

    static Set<String> injectedServices(final String source) {
        Set<String> services = new LinkedHashSet<>();
        if (source == null || source.isBlank()) {
            return services;
        }
        Matcher matcher = INJECTED_FIELD_PATTERN.matcher(source);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            String fieldName = matcher.group(2);
            services.add(typeName + ":" + fieldName);
        }
        return services;
    }

    static ControllerFlowAnalysis analyzeFlow(
            final String controllerRef,
            final String source,
            final AnalysisProperties.ClassificationPatterns patterns) {
        String controllerName = controllerName(controllerRef);
        List<StateCandidate> states = collectStates(source);
        List<String> policyGuards = collectPolicyGuards(source, patterns);
        List<String> uiGuards = collectUiGuards(source, patterns);
        List<ControllerFlowAnalysis.StateTransition> transitions = collectTransitions(source, states);
        ControllerFlowAnalysis.ConditionalAnalysis inheritanceAnalysis = analyzeInheritance(source);
        ControllerFlowAnalysis.ConditionalAnalysis dynamicUiAnalysis = analyzeDynamicUi(source);
        List<String> evidence = new ArrayList<>();
        states.forEach(state -> evidence.add(state.label()));
        evidence.addAll(policyGuards);
        evidence.addAll(uiGuards);
        evidence.addAll(inheritanceAnalysis.evidence());
        evidence.addAll(dynamicUiAnalysis.evidence());
        double confidence = confidence(states.size(), transitions.size(), policyGuards.size());
        double threshold = patterns.effectiveStateMachineConfidenceThreshold();
        boolean detected = confidence >= threshold && !states.isEmpty();
        String detectionLevel = detected ? "CONFIRMED" : (confidence > 0.0d ? "POSSIBLE" : "NONE");
        List<String> warnings = new ArrayList<>();
        if (states.isEmpty()) {
            warnings.add("Aucun signal de state machine detecte");
        }
        List<String> consolidatedInsights = buildConsolidatedInsights(
                detected,
                states,
                transitions,
                policyGuards,
                uiGuards,
                inheritanceAnalysis,
                dynamicUiAnalysis);
        return new ControllerFlowAnalysis(
                controllerRef,
                controllerName,
                detected,
                confidence,
                detectionLevel,
                states.stream().map(StateCandidate::label).toList(),
                transitions,
                policyGuards,
                uiGuards,
                inheritanceAnalysis,
                dynamicUiAnalysis,
                deduplicate(evidence),
                consolidatedInsights,
                warnings);
    }

    static ControllerFlowAnalysis.ConditionalAnalysis analyzeInheritance(final String source) {
        List<String> findings = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        String currentSource = source == null ? "" : source;
        Matcher classSignatureMatcher = CLASS_SIGNATURE_PATTERN.matcher(currentSource);
        if (classSignatureMatcher.find()) {
            collectInheritanceClause(classSignatureMatcher.group(1), "extends ", findings, evidence);
            collectInheritanceInterfaces(classSignatureMatcher.group(2), findings, evidence);
        }
        Matcher overrideMatcher = OVERRIDE_METHOD_PATTERN.matcher(currentSource);
        while (overrideMatcher.find()) {
            String overrideMethod = "override " + overrideMatcher.group(1);
            findings.add(overrideMethod);
            evidence.add(overrideMethod);
        }
        Matcher superCallMatcher = SUPER_CALL_PATTERN.matcher(currentSource);
        while (superCallMatcher.find()) {
            String superCall = "super." + superCallMatcher.group(1) + "()";
            findings.add(superCall);
            evidence.add(superCall);
        }
        return buildConditionalAnalysis(findings, evidence);
    }

    static ControllerFlowAnalysis.ConditionalAnalysis analyzeDynamicUi(final String source) {
        List<String> bindings = collectLineMatches(source, PROPERTY_BINDING_PATTERN);
        List<String> listeners = collectLineMatches(source, LISTENER_PATTERN);
        List<String> eventHandlers = collectLineMatches(source, EVENT_HANDLER_PATTERN);
        List<String> mutations = collectLineMatches(source, DYNAMIC_NODE_MUTATION_PATTERN);
        List<String> findings = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        addDynamicFinding(findings, evidence, "bindings detectes", bindings);
        addDynamicFinding(findings, evidence, "listeners detectes", listeners);
        addDynamicFinding(findings, evidence, "event handlers detectes", eventHandlers);
        addDynamicFinding(findings, evidence, "mutations UI detectees", mutations);
        return buildConditionalAnalysis(findings, evidence);
    }

    static List<ControllerFlowAnalysis.StateTransition> collectTransitions(
            final String source,
            final List<StateCandidate> states) {
        List<ControllerFlowAnalysis.StateTransition> transitions = new ArrayList<>();
        List<MethodBlock> methods = methodBlocks(source);
        for (MethodBlock method : methods) {
            List<StateCandidate> referenced = referencedStates(method.body(), states);
            if (referenced.size() >= 2) {
                transitions.add(new ControllerFlowAnalysis.StateTransition(
                        referenced.get(0).label(),
                        referenced.get(1).label(),
                        method.name(),
                        firstCondition(method.body()),
                        method.line()));
            } else if (referenced.size() == 1) {
                String target = inferTargetState(method.body(), states)
                        .orElse(referenced.get(0).label());
                transitions.add(new ControllerFlowAnalysis.StateTransition(
                        referenced.get(0).label(),
                        target,
                        method.name(),
                        firstCondition(method.body()),
                        method.line()));
            }
        }
        return List.copyOf(transitions);
    }

    private static List<String> buildConsolidatedInsights(
            final boolean stateMachineDetected,
            final List<StateCandidate> states,
            final List<ControllerFlowAnalysis.StateTransition> transitions,
            final List<String> policyGuards,
            final List<String> uiGuards,
            final ControllerFlowAnalysis.ConditionalAnalysis inheritanceAnalysis,
            final ControllerFlowAnalysis.ConditionalAnalysis dynamicUiAnalysis) {
        List<String> insights = new ArrayList<>();
        insights.add("consolidation:mandatory-core");
        insights.add(inheritanceAnalysis.activated()
                ? "inheritance-analysis:active-conditional-path"
                : "inheritance-analysis:inactive");
        insights.add(dynamicUiAnalysis.activated()
                ? "dynamic-ui-analysis:active-priority-path"
                : "dynamic-ui-analysis:inactive");
        insights.add("pdf-analysis:out-of-mvp-by-default");
        if (stateMachineDetected) {
            insights.add("state-machine confirmee : " + states.size() + " etats, "
                    + transitions.size() + " transitions");
        }
        if (!policyGuards.isEmpty()) {
            insights.add("policy guards detectes : " + policyGuards.size());
        }
        if (!uiGuards.isEmpty()) {
            insights.add("ui guards detectes : " + uiGuards.size());
        }
        if (inheritanceAnalysis.activated()) {
            insights.add("inheritance-analysis actif : " + String.join("; ", inheritanceAnalysis.findings()));
        }
        if (dynamicUiAnalysis.activated()) {
            insights.add("dynamic-ui-analysis actif : " + String.join("; ", dynamicUiAnalysis.findings()));
        }
        return deduplicate(insights);
    }

    private static void collectInheritanceClause(
            final String rawType,
            final String prefix,
            final List<String> findings,
            final List<String> evidence) {
        String typeName = normalizeTypeName(rawType);
        if (!typeName.isBlank() && !"Object".equals(typeName)) {
            String finding = prefix + typeName;
            findings.add(finding);
            evidence.add(finding);
        }
    }

    private static void collectInheritanceInterfaces(
            final String rawInterfaces,
            final List<String> findings,
            final List<String> evidence) {
        if (rawInterfaces != null && !rawInterfaces.isBlank()) {
            String[] segments = rawInterfaces.split(",");
            for (String segment : segments) {
                collectInheritanceClause(segment, "implements ", findings, evidence);
            }
        }
    }

    private static String normalizeTypeName(final String rawType) {
        String normalized = rawType == null ? "" : rawType.trim();
        int genericIndex = normalized.indexOf('<');
        if (genericIndex >= 0) {
            normalized = normalized.substring(0, genericIndex);
        }
        int packageSeparator = normalized.lastIndexOf('.');
        if (packageSeparator >= 0 && packageSeparator < normalized.length() - 1) {
            normalized = normalized.substring(packageSeparator + 1);
        }
        return normalized.trim();
    }

    private static List<String> collectLineMatches(final String source, final Pattern pattern) {
        List<String> matches = new ArrayList<>();
        String currentSource = source == null ? "" : source;
        Matcher matcher = pattern.matcher(currentSource);
        while (matcher.find()) {
            String line = matcher.group().trim();
            if (!line.isBlank()) {
                matches.add(line);
            }
        }
        return deduplicate(matches);
    }

    private static void addDynamicFinding(
            final List<String> findings,
            final List<String> evidence,
            final String label,
            final List<String> matches) {
        if (!matches.isEmpty()) {
            findings.add(label + " : " + matches.size());
            evidence.addAll(matches);
        }
    }

    private static ControllerFlowAnalysis.ConditionalAnalysis buildConditionalAnalysis(
            final List<String> findings,
            final List<String> evidence) {
        List<String> distinctFindings = deduplicate(findings);
        List<String> distinctEvidence = deduplicate(evidence);
        return distinctFindings.isEmpty() && distinctEvidence.isEmpty()
                ? ControllerFlowAnalysis.ConditionalAnalysis.inactive()
                : new ControllerFlowAnalysis.ConditionalAnalysis(true, distinctFindings, distinctEvidence);
    }

    static List<String> collectPolicyGuards(
            final String source,
            final AnalysisProperties.ClassificationPatterns patterns) {
        List<String> result = new ArrayList<>();
        Matcher matcher = BOOLEAN_METHOD_PATTERN.matcher(source == null ? "" : source);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (!isUiGuard(methodName, patterns) && isPolicyGuard(methodName, patterns)) {
                result.add(methodName);
            }
        }
        return List.copyOf(result);
    }

    static List<String> collectUiGuards(
            final String source,
            final AnalysisProperties.ClassificationPatterns patterns) {
        List<String> result = new ArrayList<>();
        Matcher matcher = BOOLEAN_METHOD_PATTERN.matcher(source == null ? "" : source);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (isUiGuard(methodName, patterns)) {
                result.add(methodName);
            }
        }
        return List.copyOf(result);
    }

    static List<ProjectDependencyGraph.DependencyEdge> buildEdges(
            final Map<String, ControllerSnapshot> snapshots) {
        List<ProjectDependencyGraph.DependencyEdge> edges = new ArrayList<>();
        for (ControllerSnapshot source : snapshots.values()) {
            for (ControllerSnapshot target : snapshots.values()) {
                if (source != target) {
                    java.util.Optional<String> shared = sharedServiceEvidence(source, target);
                    if (shared.isPresent()) {
                        edges.add(new ProjectDependencyGraph.DependencyEdge(
                                source.controllerRef(),
                                target.controllerRef(),
                                ProjectDependencyGraph.DependencyType.SHARED_SERVICE,
                                shared.orElseThrow()));
                    }
                    java.util.Optional<String> direct = directCallEvidence(source, target);
                    if (direct.isPresent()) {
                        edges.add(new ProjectDependencyGraph.DependencyEdge(
                                source.controllerRef(),
                                target.controllerRef(),
                                ProjectDependencyGraph.DependencyType.DIRECT_CALL,
                                direct.orElseThrow()));
                    }
                }
            }
        }
        return List.copyOf(edges);
    }

    static List<String> topologicalOrder(
            final Map<String, ControllerSnapshot> snapshots,
            final List<ProjectDependencyGraph.DependencyEdge> edges) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        snapshots.values().forEach(snapshot -> indegree.put(snapshot.controllerRef(), 0));
        for (ProjectDependencyGraph.DependencyEdge edge : edges) {
            if (edge.type() == ProjectDependencyGraph.DependencyType.DIRECT_CALL) {
                indegree.computeIfPresent(edge.toController(), (k, value) -> value + 1);
            }
        }
        List<String> order = new ArrayList<>();
        List<String> queue = indegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        List<String> mutableQueue = new ArrayList<>(queue);
        while (!mutableQueue.isEmpty()) {
            String current = mutableQueue.remove(0);
            order.add(current);
            for (ProjectDependencyGraph.DependencyEdge edge : edges) {
                if (edge.type() == ProjectDependencyGraph.DependencyType.DIRECT_CALL
                        && edge.fromController().equals(current)) {
                    int next = indegree.getOrDefault(edge.toController(), 0) - 1;
                    indegree.put(edge.toController(), next);
                    if (next == 0 && !order.contains(edge.toController())
                            && !mutableQueue.contains(edge.toController())) {
                        mutableQueue.add(edge.toController());
                        mutableQueue.sort(Comparator.naturalOrder());
                    }
                }
            }
        }
        snapshots.values().stream()
                .map(ControllerSnapshot::controllerRef)
                .filter(ref -> !order.contains(ref))
                .sorted()
                .forEach(order::add);
        return List.copyOf(order);
    }

    static List<ProjectDeltaAnalysis.ControllerDelta> diffSnapshots(
            final Map<String, ControllerSnapshot> baseline,
            final Map<String, ControllerSnapshot> current) {
        List<ProjectDeltaAnalysis.ControllerDelta> deltas = new ArrayList<>();
        Set<String> refs = new LinkedHashSet<>();
        refs.addAll(baseline.keySet());
        refs.addAll(current.keySet());
        for (String ref : refs) {
            ControllerSnapshot before = baseline.get(ref);
            ControllerSnapshot after = current.get(ref);
            if (before == null) {
                deltas.add(new ProjectDeltaAnalysis.ControllerDelta(
                        ref,
                        ProjectDeltaAnalysis.DeltaStatus.NEW,
                        after.ruleFingerprints(),
                        List.of(),
                        after.transitionFingerprints(),
                        List.of(),
                        List.of("Nouveau controller dans le lot courant")));
            } else if (after == null) {
                deltas.add(new ProjectDeltaAnalysis.ControllerDelta(
                        ref,
                        ProjectDeltaAnalysis.DeltaStatus.REMOVED,
                        List.of(),
                        before.ruleFingerprints(),
                        List.of(),
                        before.transitionFingerprints(),
                        List.of("Controller absent du lot courant")));
            } else {
                List<String> addedRules = difference(after.ruleFingerprints(), before.ruleFingerprints());
                List<String> removedRules = difference(before.ruleFingerprints(), after.ruleFingerprints());
                List<String> addedTransitions = difference(after.transitionFingerprints(), before.transitionFingerprints());
                List<String> removedTransitions = difference(before.transitionFingerprints(), after.transitionFingerprints());
                boolean modified = !addedRules.isEmpty() || !removedRules.isEmpty()
                        || !addedTransitions.isEmpty() || !removedTransitions.isEmpty();
                deltas.add(new ProjectDeltaAnalysis.ControllerDelta(
                        ref,
                        modified ? ProjectDeltaAnalysis.DeltaStatus.MODIFIED : ProjectDeltaAnalysis.DeltaStatus.UNCHANGED,
                        addedRules,
                        removedRules,
                        addedTransitions,
                        removedTransitions,
                        List.of()));
            }
        }
        return List.copyOf(deltas);
    }

    static Map<String, ControllerSnapshot> buildSnapshots(
            final List<String> controllerRefs,
            final ff.ss.javaFxAuditStudio.application.ports.out.SourceReaderPort sourceReaderPort,
            final ff.ss.javaFxAuditStudio.application.ports.out.RuleExtractionPort ruleExtractionPort,
            final AnalysisProperties.ClassificationPatterns patterns) {
        Map<String, ControllerSnapshot> snapshots = new LinkedHashMap<>();
        for (String ref : controllerRefs) {
            String source = readSource(ref, sourceReaderPort);
            Set<String> injected = injectedServices(source);
            ExtractionResult extraction = ruleExtractionPort.extract(ref, source);
            ControllerFlowAnalysis flow = analyzeFlow(ref, source, patterns);
            snapshots.put(ref, new ControllerSnapshot(
                    ref,
                    controllerName(ref),
                    source,
                    injected,
                    ruleFingerprints(extraction),
                    transitionFingerprints(flow),
                    flow));
        }
        return snapshots;
    }

    static List<String> ruleFingerprints(final ExtractionResult extraction) {
        List<String> fingerprints = new ArrayList<>();
        extraction.rules().forEach(rule -> fingerprints.add(fingerprint(rule)));
        return List.copyOf(fingerprints);
    }

    static List<String> transitionFingerprints(final ControllerFlowAnalysis flow) {
        List<String> fingerprints = new ArrayList<>();
        flow.transitions().forEach(transition -> fingerprints.add(
                transition.sourceState() + "->" + transition.targetState() + ":" + transition.triggerMethod()));
        return List.copyOf(fingerprints);
    }

    private static boolean isPolicyGuard(
            final String methodName,
            final AnalysisProperties.ClassificationPatterns patterns) {
        for (String prefix : patterns.effectivePolicyGuardPrefixes()) {
            if (methodName.startsWith(prefix) && methodName.length() > prefix.length()) {
                char next = methodName.charAt(prefix.length());
                if (Character.isUpperCase(next)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isUiGuard(
            final String methodName,
            final AnalysisProperties.ClassificationPatterns patterns) {
        return patterns.effectiveUiGuardMethodNames().contains(methodName);
    }

    private static List<MethodBlock> methodBlocks(final String source) {
        List<MethodBlock> methods = new ArrayList<>();
        if (source == null || source.isBlank()) {
            return methods;
        }
        Matcher matcher = METHOD_PATTERN.matcher(source);
        while (matcher.find()) {
            String name = matcher.group(3);
            int openBrace = source.indexOf('{', matcher.end() - 1);
            if (openBrace >= 0) {
                int closeBrace = matchingBrace(source, openBrace);
                if (closeBrace > openBrace) {
                    String body = source.substring(openBrace + 1, closeBrace);
                    methods.add(new MethodBlock(name, body, lineNumber(source, matcher.start())));
                }
            }
        }
        return methods;
    }

    private static List<StateCandidate> collectStates(final String source) {
        List<StateCandidate> states = new ArrayList<>();
        Matcher matcher = FIELD_PATTERN.matcher(source == null ? "" : source);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            if (looksLikeState(type, name)) {
                states.add(new StateCandidate(name, normalizeStateLabel(name)));
            }
        }
        return List.copyOf(states);
    }

    private static List<StateCandidate> referencedStates(
            final String body,
            final List<StateCandidate> states) {
        List<StateCandidate> referenced = new ArrayList<>();
        String lower = body.toLowerCase(Locale.ROOT);
        for (StateCandidate state : states) {
            if (lower.contains(state.fieldName().toLowerCase(Locale.ROOT))) {
                referenced.add(state);
            }
        }
        Matcher matcher = CASE_PATTERN.matcher(body);
        while (matcher.find()) {
            referenced.add(new StateCandidate(matcher.group(1), normalizeStateLabel(matcher.group(1))));
        }
        return referenced;
    }

    private static java.util.Optional<String> inferTargetState(
            final String body,
            final List<StateCandidate> states) {
        Matcher assignment = ASSIGNMENT_PATTERN.matcher(body);
        if (assignment.find()) {
            return java.util.Optional.of(normalizeStateLabel(assignment.group(1)));
        }
        for (StateCandidate state : states) {
            if (body.contains(state.fieldName() + " =")) {
                return java.util.Optional.of(state.label());
            }
        }
        return java.util.Optional.empty();
    }

    private static String firstCondition(final String body) {
        int ifIndex = body.indexOf("if (");
        if (ifIndex >= 0) {
            int end = body.indexOf(')', ifIndex);
            if (end > ifIndex) {
                return body.substring(ifIndex, end + 1).trim();
            }
        }
        int switchIndex = body.indexOf("switch (");
        if (switchIndex >= 0) {
            int end = body.indexOf(')', switchIndex);
            if (end > switchIndex) {
                return body.substring(switchIndex, end + 1).trim();
            }
        }
        return body.lines().findFirst().map(String::trim).orElse("");
    }

    private static int matchingBrace(final String source, final int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int lineNumber(final String source, final int index) {
        int line = 1;
        for (int i = 0; i < index && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static boolean looksLikeState(final String type, final String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        String typeLower = type.toLowerCase(Locale.ROOT);
        return lower.startsWith("is") || lower.startsWith("has") || lower.startsWith("can")
                || lower.contains("mode") || lower.contains("state") || lower.contains("step")
                || lower.contains("phase") || lower.contains("status") || lower.contains("selection")
                || typeLower.endsWith("mode") || typeLower.endsWith("state") || typeLower.equals("boolean");
    }

    private static String normalizeStateLabel(final String raw) {
        String cleaned = raw.replaceFirst("^(is|has|can|should)", "");
        cleaned = cleaned.replaceAll("(Mode|State|Step|Phase|Status|Selection|Screen|View)$", "");
        cleaned = cleaned.replaceAll("([a-z])([A-Z])", "$1_$2");
        cleaned = cleaned.replaceAll("_+", "_");
        cleaned = cleaned.replaceAll("^_+|_+$", "");
        if (cleaned.isBlank()) {
            cleaned = raw;
        }
        return cleaned.toUpperCase(Locale.ROOT);
    }

    private static double confidence(final int stateCount, final int transitionCount, final int policyGuardCount) {
        double raw = stateCount * 0.20d + transitionCount * 0.25d + policyGuardCount * 0.15d;
        return Math.min(1.0d, raw);
    }

    private static List<String> deduplicate(final List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private static List<String> difference(final List<String> left, final List<String> right) {
        List<String> result = new ArrayList<>(left);
        result.removeAll(right);
        return List.copyOf(result);
    }

    private static java.util.Optional<String> sharedServiceEvidence(
            final ControllerSnapshot source,
            final ControllerSnapshot target) {
        for (String service : source.injectedServices()) {
            if (target.injectedServices().contains(service)) {
                return java.util.Optional.of("shared-service:" + service);
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<String> directCallEvidence(
            final ControllerSnapshot source,
            final ControllerSnapshot target) {
        String simpleName = target.controllerName();
        String sourceText = source.source().toLowerCase(Locale.ROOT);
        if (sourceText.contains("new " + simpleName.toLowerCase(Locale.ROOT) + "(")
                || sourceText.contains(simpleName.toLowerCase(Locale.ROOT) + ".")
                || sourceText.contains(simpleName.toLowerCase(Locale.ROOT) + "controller")) {
            return java.util.Optional.of("direct-call:" + simpleName);
        }
        Matcher matcher = CONTROLLER_CALL_PATTERN.matcher(source.source());
        while (matcher.find()) {
            if (matcher.group(1).equals(simpleName)) {
                return java.util.Optional.of("direct-call:" + simpleName);
            }
        }
        return java.util.Optional.empty();
    }

    private static String fingerprint(final ff.ss.javaFxAuditStudio.domain.rules.BusinessRule rule) {
        return rule.extractionCandidate().name()
                + "|" + rule.responsibilityClass().name()
                + "|" + rule.description();
    }

    record ControllerSnapshot(
            String controllerRef,
            String controllerName,
            String source,
            Set<String> injectedServices,
            List<String> ruleFingerprints,
            List<String> transitionFingerprints,
            ControllerFlowAnalysis flow) {
    }

    private record MethodBlock(String name, String body, int line) {
    }

    private record StateCandidate(String fieldName, String label) {
    }
}
