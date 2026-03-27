package ff.ss.javaFxAuditStudio.configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import ff.ss.javaFxAuditStudio.adapters.out.ai.AiCircuitBreaker;
import ff.ss.javaFxAuditStudio.adapters.out.ai.ClaudeCodeAiEnrichmentAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.ai.ClaudeCodeCliAiEnrichmentAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.ai.OpenAiGpt54AiEnrichmentAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.ai.PayloadHasher;
import ff.ss.javaFxAuditStudio.adapters.out.ai.PromptTemplateLoader;
import ff.ss.javaFxAuditStudio.adapters.out.ai.RoutingAiEnrichmentAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.filesystem.FilesystemSourceFileReaderAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.CommentSanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.DataSubstitutionSanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.IdentifierSanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.MultiFileSanitizationAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.OpenRewriteIdentifierSanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.SanitizationPipelineAdapter;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.Sanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.SecretSanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.SemgrepScanSanitizer;
import ff.ss.javaFxAuditStudio.adapters.out.sanitization.SensitiveMarkerDetector;
import ff.ss.javaFxAuditStudio.application.ports.in.EnrichAnalysisUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ExportAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.GenerateSpringBootClassesUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ListAiGeneratedArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ListProjectReferencePatternsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.PreviewSanitizedSourceUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RefineAiArtifactUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.ReviewArtifactsUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RefineArtifactUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.RegisterProjectReferencePatternUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.VerifyAiArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.application.ports.in.VerifyArtifactCoherenceUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.AiArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.AiEnrichmentPort;
import ff.ss.javaFxAuditStudio.application.ports.out.AnalysisSessionPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ArtifactPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.ClassificationPersistencePort;
import ff.ss.javaFxAuditStudio.application.ports.out.LlmAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.MultiFileSanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ProjectReferencePatternPort;
import ff.ss.javaFxAuditStudio.application.ports.out.ReclassificationAuditPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SanitizationPort;
import ff.ss.javaFxAuditStudio.application.ports.out.SourceFileReaderPort;
import ff.ss.javaFxAuditStudio.application.ports.out.WorkflowObservabilityPort;
import ff.ss.javaFxAuditStudio.application.service.AiArtifactCatalogService;
import ff.ss.javaFxAuditStudio.application.service.AiSpringBootGenerationService;
import ff.ss.javaFxAuditStudio.application.service.EnrichAnalysisService;
import ff.ss.javaFxAuditStudio.application.service.PreviewSanitizedSourceService;
import ff.ss.javaFxAuditStudio.application.service.ProjectReferencePatternCatalogService;
import ff.ss.javaFxAuditStudio.application.service.RefineAiArtifactService;
import ff.ss.javaFxAuditStudio.application.service.RefineArtifactService;
import ff.ss.javaFxAuditStudio.application.service.ReviewArtifactsService;
import ff.ss.javaFxAuditStudio.application.service.VerifyAiArtifactCoherenceService;
import ff.ss.javaFxAuditStudio.application.service.VerifyArtifactCoherenceService;

/**
 * Assemblage hexagonal du sous-systeme d'enrichissement IA (JAS-017 / JAS-018).
 *
 * <p>Seule classe autorisee a instancier les classes du sous-systeme AI et sanitisation.
 * Aucun {@code @Service} / {@code @Component} sur les services ni les adaptateurs.
 */
@Configuration
@EnableConfigurationProperties({SanitizationProperties.class, AiHttpProxyProperties.class, SemgrepScanProperties.class})
public class AiEnrichmentOrchestraConfiguration {

    private static final int CIRCUIT_SLIDING_WINDOW = 5;
    private static final int CIRCUIT_FAILURE_THRESHOLD = 50;
    private static final Duration CIRCUIT_WAIT_OPEN = Duration.ofSeconds(30);

    // --- Beans IA existants ---

    @Bean
    public AiCircuitBreaker aiCircuitBreaker() {
        return new AiCircuitBreaker(
                CIRCUIT_SLIDING_WINDOW,
                CIRCUIT_FAILURE_THRESHOLD,
                CIRCUIT_WAIT_OPEN);
    }

    @Bean
    public PromptTemplateLoader promptTemplateLoader() {
        return new PromptTemplateLoader();
    }

    @Bean
    public ObjectMapper aiObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestClient aiRestClient(
            final AiEnrichmentProperties properties,
            final AiHttpProxyProperties proxyProperties) {
        long timeoutMs = properties.effectiveTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeoutMs);
        factory.setReadTimeout((int) timeoutMs);
        if (proxyProperties.isConfigured()) {
            factory.setProxy(new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(proxyProperties.host(), proxyProperties.port())));
        }
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    public ClaudeCodeAiEnrichmentAdapter claudeCodeAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final PromptTemplateLoader promptTemplateLoader,
            final RestClient aiRestClient,
            final ObjectMapper aiObjectMapper) {
        return new ClaudeCodeAiEnrichmentAdapter(properties, promptTemplateLoader, aiRestClient, aiObjectMapper);
    }

    @Bean
    public OpenAiGpt54AiEnrichmentAdapter openAiGpt54AiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final PromptTemplateLoader promptTemplateLoader,
            final RestClient aiRestClient,
            final ObjectMapper aiObjectMapper) {
        return new OpenAiGpt54AiEnrichmentAdapter(properties, promptTemplateLoader, aiRestClient, aiObjectMapper);
    }

    @Bean
    public ClaudeCodeCliAiEnrichmentAdapter claudeCodeCliAiEnrichmentAdapter(
            final AiEnrichmentProperties properties,
            final PromptTemplateLoader promptTemplateLoader,
            final ObjectMapper aiObjectMapper) {
        return new ClaudeCodeCliAiEnrichmentAdapter(
                properties, promptTemplateLoader, aiObjectMapper, properties.effectiveCliCommand());
    }

    @Bean
    public AiEnrichmentPort aiEnrichmentPort(
            final AiEnrichmentProperties properties,
            final ClaudeCodeAiEnrichmentAdapter claudeAdapter,
            final OpenAiGpt54AiEnrichmentAdapter openAiAdapter,
            final ClaudeCodeCliAiEnrichmentAdapter cliAdapter,
            final AiCircuitBreaker circuitBreaker) {
        return new RoutingAiEnrichmentAdapter(
                properties, claudeAdapter, openAiAdapter, cliAdapter, circuitBreaker);
    }

    // --- Beans sanitisation (JAS-018) ---

    @Bean
    public OpenRewriteIdentifierSanitizer openRewriteIdentifierSanitizer() {
        return new OpenRewriteIdentifierSanitizer();
    }

    @Bean
    public IdentifierSanitizer identifierSanitizer() {
        return new IdentifierSanitizer();
    }

    @Bean
    public SecretSanitizer secretSanitizer() {
        return new SecretSanitizer();
    }

    @Bean
    public CommentSanitizer commentSanitizer() {
        return new CommentSanitizer();
    }

    @Bean
    public DataSubstitutionSanitizer dataSubstitutionSanitizer() {
        return new DataSubstitutionSanitizer();
    }

    @Bean
    public SemgrepScanSanitizer semgrepScanSanitizer(
            final SemgrepScanProperties semgrepScanProperties,
            final ObjectMapper aiObjectMapper) {
        return new SemgrepScanSanitizer(semgrepScanProperties, aiObjectMapper);
    }

    @Bean
    public SensitiveMarkerDetector sensitiveMarkerDetector() {
        return new SensitiveMarkerDetector();
    }

    @Bean
    public SanitizationPort sanitizationPort(
            final OpenRewriteIdentifierSanitizer openRewriteIdentifierSanitizer,
            final IdentifierSanitizer identifierSanitizer,
            final SecretSanitizer secretSanitizer,
            final CommentSanitizer commentSanitizer,
            final DataSubstitutionSanitizer dataSubstitutionSanitizer,
            final SemgrepScanSanitizer semgrepScanSanitizer,
            final SensitiveMarkerDetector sensitiveMarkerDetector,
            final SanitizationProperties sanitizationProperties) {
        // Ordre du pipeline : AST-rewrite (OpenRewrite) → regex-cleanup → secrets
        // → commentaires → donnees → scan Semgrep post-sanitisation
        List<Sanitizer> pipeline = List.of(
                openRewriteIdentifierSanitizer,
                identifierSanitizer,
                secretSanitizer,
                commentSanitizer,
                dataSubstitutionSanitizer,
                semgrepScanSanitizer);
        return new SanitizationPipelineAdapter(pipeline, sensitiveMarkerDetector, sanitizationProperties);
    }

    @Bean
    public MultiFileSanitizationPort multiFileSanitizationPort(
            final SanitizationPort sanitizationPort,
            final SecretSanitizer secretSanitizer,
            final SemgrepScanSanitizer semgrepScanSanitizer) {
        return new MultiFileSanitizationAdapter(sanitizationPort, secretSanitizer, semgrepScanSanitizer);
    }

    // --- Adapter lecture source fichier (IAP-5) ---

    @Bean
    public SourceFileReaderPort sourceFileReaderPort() {
        return new FilesystemSourceFileReaderAdapter();
    }

    // --- Assemblage du use case ---

    @Bean
    public EnrichAnalysisUseCase enrichAnalysisUseCase(
            final AnalysisSessionPort sessionPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final LlmAuditPort llmAuditPort,
            final PayloadHasher payloadHasher,
            final LlmAuditProperties llmAuditProperties,
            final SourceFileReaderPort sourceFileReaderPort,
            final WorkflowObservabilityPort observabilityPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new EnrichAnalysisService(
                sessionPort,
                aiEnrichmentPort,
                effectivePort,
                llmAuditPort,
                payloadHasher,
                llmAuditProperties,
                sourceFileReaderPort,
                observabilityPort);
    }

    @Bean
    public ReviewArtifactsUseCase reviewArtifactsUseCase(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new ReviewArtifactsService(
                sessionPort, classificationPort, cartographyPort, reclassificationAuditPort,
                aiEnrichmentPort, effectivePort, sourceFileReaderPort);
    }

    @Bean
    public GenerateSpringBootClassesUseCase generateSpringBootClassesUseCase(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new AiSpringBootGenerationService(
                sessionPort, classificationPort, cartographyPort, reclassificationAuditPort,
                aiArtifactPersistencePort, projectReferencePatternPort, aiEnrichmentPort, effectivePort, sourceFileReaderPort);
    }

    @Bean
    public RefineArtifactUseCase refineArtifactUseCase(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new RefineArtifactService(
                sessionPort, classificationPort, cartographyPort, reclassificationAuditPort,
                aiEnrichmentPort, effectivePort, sourceFileReaderPort);
    }

    @Bean
    public RefineAiArtifactUseCase refineAiArtifactUseCase(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new RefineAiArtifactService(
                sessionPort,
                classificationPort,
                cartographyPort,
                reclassificationAuditPort,
                aiArtifactPersistencePort,
                projectReferencePatternPort,
                aiEnrichmentPort,
                effectivePort,
                sourceFileReaderPort);
    }

    @Bean
    public VerifyArtifactCoherenceUseCase verifyArtifactCoherenceUseCase(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ArtifactPersistencePort artifactPersistencePort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new VerifyArtifactCoherenceService(
                sessionPort, classificationPort, cartographyPort, artifactPersistencePort,
                reclassificationAuditPort, aiEnrichmentPort, effectivePort, sourceFileReaderPort);
    }

    @Bean
    public VerifyAiArtifactCoherenceUseCase verifyAiArtifactCoherenceUseCase(
            final AnalysisSessionPort sessionPort,
            final ClassificationPersistencePort classificationPort,
            final CartographyPersistencePort cartographyPort,
            final ReclassificationAuditPort reclassificationAuditPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort,
            final ProjectReferencePatternPort projectReferencePatternPort,
            final AiEnrichmentPort aiEnrichmentPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new VerifyAiArtifactCoherenceService(
                sessionPort,
                classificationPort,
                cartographyPort,
                reclassificationAuditPort,
                aiArtifactPersistencePort,
                projectReferencePatternPort,
                aiEnrichmentPort,
                effectivePort,
                sourceFileReaderPort);
    }

    @Bean
    public PreviewSanitizedSourceUseCase previewSanitizedSourceUseCase(
            final AnalysisSessionPort sessionPort,
            final SanitizationPort sanitizationPort,
            final SanitizationProperties sanitizationProperties,
            final SourceFileReaderPort sourceFileReaderPort) {
        SanitizationPort effectivePort = sanitizationProperties.enabled() ? sanitizationPort : null;
        return new PreviewSanitizedSourceService(sessionPort, effectivePort, sourceFileReaderPort);
    }

    @Bean
    public ListAiGeneratedArtifactsUseCase listAiGeneratedArtifactsUseCase(
            final AnalysisSessionPort sessionPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort) {
        return new AiArtifactCatalogService(sessionPort, aiArtifactPersistencePort);
    }

    @Bean
    public ExportAiGeneratedArtifactsUseCase exportAiGeneratedArtifactsUseCase(
            final AnalysisSessionPort sessionPort,
            final AiArtifactPersistencePort aiArtifactPersistencePort) {
        return new AiArtifactCatalogService(sessionPort, aiArtifactPersistencePort);
    }

    @Bean
    public RegisterProjectReferencePatternUseCase registerProjectReferencePatternUseCase(
            final ProjectReferencePatternPort projectReferencePatternPort) {
        return new ProjectReferencePatternCatalogService(projectReferencePatternPort);
    }

    @Bean
    public ListProjectReferencePatternsUseCase listProjectReferencePatternsUseCase(
            final ProjectReferencePatternPort projectReferencePatternPort) {
        return new ProjectReferencePatternCatalogService(projectReferencePatternPort);
    }
}
