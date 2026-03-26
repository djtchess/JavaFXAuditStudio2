package ff.ss.javaFxAuditStudio.domain.sanitization;

/**
 * Exception levee lorsque la sanitisation detecte un marqueur sensible residuel (JAS-018).
 *
 * <p>Classe de domaine pur — aucune dependance Spring.
 * Levee par {@code SanitizationPipelineAdapter} quand :
 * <ul>
 *   <li>un marqueur sensible subsiste apres toutes les transformations ; ou</li>
 *   <li>le nombre de tokens estime depasse le plafond configure.</li>
 * </ul>
 */
public class SanitizationRefusedException extends RuntimeException {

    public SanitizationRefusedException(final String message) {
        super(message);
    }

    public SanitizationRefusedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
