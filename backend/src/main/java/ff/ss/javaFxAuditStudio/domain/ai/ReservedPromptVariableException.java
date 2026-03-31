package ff.ss.javaFxAuditStudio.domain.ai;

/**
 * Levee quand une cle d'extraContext entre en collision avec une variable reservee du template (AI-6).
 */
public class ReservedPromptVariableException extends IllegalArgumentException {

    public ReservedPromptVariableException(final String key, final String requestId) {
        super("Reserved prompt variable collision: key='" + key
                + "' is reserved and must not appear in extraContext [requestId=" + requestId + "]");
    }
}
