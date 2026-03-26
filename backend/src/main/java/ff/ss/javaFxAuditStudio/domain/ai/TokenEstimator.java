package ff.ss.javaFxAuditStudio.domain.ai;

/**
 * Estimateur partage de tokens pour le sous-systeme IA.
 *
 * <p>Cette heuristique sera remplacee par un estimateur calibre dans un lot dedie.
 */
public final class TokenEstimator {

    private static final int TOKEN_ESTIMATION_DIVISOR = 4;

    private TokenEstimator() {
        // Utility class.
    }

    /**
     * Estime un volume de tokens a partir de la longueur du texte.
     */
    public static int estimate(final String source) {
        int estimatedTokens = 0;
        if (source != null && !source.isBlank()) {
            estimatedTokens = source.length() / TOKEN_ESTIMATION_DIVISOR;
        }
        return estimatedTokens;
    }
}
