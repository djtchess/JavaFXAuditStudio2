package ff.ss.javaFxAuditStudio.domain.ai;

import java.util.regex.Pattern;

/**
 * Heuristiques minimales pour signaler un artefact IA manifestement incomplet.
 */
public final class AiArtifactImplementationInspector {

    private static final Pattern TODO_MARKER = Pattern.compile(
            "(?im)^\\s*(?://|/\\*+|\\*)\\s*TODO\\b");
    private static final Pattern NOT_IMPLEMENTED_MARKER = Pattern.compile(
            "(?i)\\b(?:UnsupportedOperationException|NotImplementedException)\\b");

    private AiArtifactImplementationInspector() {
    }

    public static AiArtifactImplementationStatus resolveStatus(final String content) {
        AiArtifactImplementationStatus status = AiArtifactImplementationStatus.READY;
        if (isIncomplete(content)) {
            status = AiArtifactImplementationStatus.INCOMPLETE;
        }
        return status;
    }

    public static boolean isIncomplete(final String content) {
        String normalizedContent = content != null ? content : "";
        boolean incomplete = normalizedContent.isBlank();
        if (!incomplete) {
            incomplete = TODO_MARKER.matcher(normalizedContent).find();
        }
        if (!incomplete) {
            incomplete = NOT_IMPLEMENTED_MARKER.matcher(normalizedContent).find();
        }
        return incomplete;
    }

    public static String resolveWarning(final String content) {
        String warning = null;
        if (isIncomplete(content)) {
            warning = "Artefact IA incomplet detecte : placeholder d'implementation residuel.";
        }
        return warning;
    }
}
