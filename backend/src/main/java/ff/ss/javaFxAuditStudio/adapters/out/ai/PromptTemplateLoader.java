package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Charge et rend les templates Mustache depuis le classpath (src/main/resources/prompts/).
 *
 * <p>Thread-safe : les templates sont compiles une seule fois et caches dans un
 * {@link ConcurrentHashMap}.
 * Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration} — pas de
 * {@code @Component}.
 */
public class PromptTemplateLoader {

    private static final String PROMPTS_PREFIX = "prompts/";
    private static final String TEMPLATE_SUFFIX = ".mustache";
    private static final String FALLBACK_TEMPLATE = "enrichment-default";

    private final ConcurrentHashMap<String, Mustache> templateCache = new ConcurrentHashMap<>();
    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    /**
     * Rend le template Mustache identifie par {@code templateName} avec le contexte fourni.
     *
     * <p>Si le template n'est pas trouve dans le classpath, le template de fallback
     * {@code enrichment-default} est utilise. Leve {@link IllegalStateException} si meme le
     * fallback est absent.
     *
     * @param templateName nom du template (sans extension ni prefixe)
     * @param context      variables a injecter dans le template
     * @return le prompt rendu sous forme de chaine
     */
    public String render(final String templateName, final Map<String, Object> context) {
        Mustache mustache = resolveTemplate(templateName);
        StringWriter writer = new StringWriter();
        mustache.execute(writer, context);
        return writer.toString();
    }

    private Mustache resolveTemplate(final String templateName) {
        if (classpathResourceExists(PROMPTS_PREFIX + templateName + TEMPLATE_SUFFIX)) {
            return templateCache.computeIfAbsent(templateName, this::compileTemplate);
        }
        return templateCache.computeIfAbsent(FALLBACK_TEMPLATE, this::compileFallbackTemplate);
    }

    private Mustache compileTemplate(final String templateName) {
        String resourcePath = PROMPTS_PREFIX + templateName + TEMPLATE_SUFFIX;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            return compileFallbackTemplate(FALLBACK_TEMPLATE);
        }
        return mustacheFactory.compile(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                templateName);
    }

    private Mustache compileFallbackTemplate(final String ignored) {
        String fallbackPath = PROMPTS_PREFIX + FALLBACK_TEMPLATE + TEMPLATE_SUFFIX;
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fallbackPath);
        if (stream == null) {
            throw new IllegalStateException(
                    "Template fallback introuvable dans le classpath : " + fallbackPath);
        }
        return mustacheFactory.compile(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                FALLBACK_TEMPLATE);
    }

    private boolean classpathResourceExists(final String resourcePath) {
        return getClass().getClassLoader().getResource(resourcePath) != null;
    }
}
