package ff.ss.javaFxAuditStudio.domain.ai;

/**
 * Estimateur partage de tokens pour le sous-systeme IA.
 *
 * <p>Estimation locale et deterministe basee sur la structure lexicale du texte.
 * L'objectif est de rester offline tout en se rapprochant davantage du volume de
 * tokens qu'avec une simple heuristique de longueur.
 */
public final class TokenEstimator {

    private static final char UNDERSCORE = '_';
    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';

    private TokenEstimator() {
        // Utility class.
    }

    /**
     * Estime un volume de tokens a partir des blocs lexicaux du texte.
     */
    public static int estimate(final String source) {
        if (source == null || source.isBlank()) {
            return 0;
        }
        String normalized = source.replace("\r\n", "\n");
        int index = 0;
        int tokens = 0;
        while (index < normalized.length()) {
            char current = normalized.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (current == DOUBLE_QUOTE || current == SINGLE_QUOTE) {
                int end = consumeQuotedLiteral(normalized, index);
                tokens += estimateQuotedLiteral(normalized, index, end);
                index = end;
            } else if (isWordStart(current)) {
                int end = consumeWord(normalized, index);
                tokens += estimateWord(normalized, index, end);
                index = end;
            } else if (isOperatorPair(normalized, index)) {
                tokens++;
                index += 2;
            } else {
                tokens++;
                index++;
            }
        }
        return tokens;
    }

    private static int consumeQuotedLiteral(final String text, final int start) {
        char quote = text.charAt(start);
        int index = start + 1;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '\\' && index + 1 < text.length()) {
                index += 2;
                continue;
            }
            if (current == quote) {
                return index + 1;
            }
            index++;
        }
        return text.length();
    }

    private static int estimateQuotedLiteral(
            final String text,
            final int startInclusive,
            final int endExclusive) {
        if (endExclusive <= startInclusive + 1) {
            return 1;
        }
        int innerEnd = endExclusive == text.length() ? endExclusive : endExclusive - 1;
        int internalTokens = estimateInnerTokens(text.substring(startInclusive + 1, innerEnd));
        return Math.max(1, internalTokens);
    }

    private static int estimateInnerTokens(final String text) {
        int tokens = 0;
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (isWordStart(current)) {
                int end = consumeWord(text, index);
                tokens += estimateWord(text, index, end);
                index = end;
            } else if (isOperatorPair(text, index)) {
                tokens++;
                index += 2;
            } else {
                tokens++;
                index++;
            }
        }
        return tokens;
    }

    private static boolean isWordStart(final char current) {
        return Character.isLetterOrDigit(current) || current == UNDERSCORE;
    }

    private static int consumeWord(final String text, final int start) {
        int index = start + 1;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (!Character.isLetterOrDigit(current) && current != UNDERSCORE) {
                break;
            }
            index++;
        }
        return index;
    }

    private static int estimateWord(final String text, final int start, final int endExclusive) {
        String word = text.substring(start, endExclusive);
        if (word.indexOf(UNDERSCORE) >= 0) {
            return countUnderscoreSeparatedParts(word);
        }
        return estimateWordTokens(word);
    }

    private static int countUnderscoreSeparatedParts(final String word) {
        int tokens = 0;
        int segmentStart = 0;
        for (int index = 0; index < word.length(); index++) {
            if (word.charAt(index) == UNDERSCORE) {
                if (index > segmentStart) {
                    tokens += estimateWordTokens(word.substring(segmentStart, index));
                }
                segmentStart = index + 1;
            }
        }
        if (segmentStart < word.length()) {
            tokens += estimateWordTokens(word.substring(segmentStart));
        }
        return Math.max(1, tokens);
    }

    private static int estimateWordTokens(final String word) {
        if (word.isBlank()) {
            return 1;
        }
        int tokens = 1;
        for (int index = 1; index < word.length(); index++) {
            char previous = word.charAt(index - 1);
            char current = word.charAt(index);
            char next = index + 1 < word.length() ? word.charAt(index + 1) : '\0';
            if (shouldSplit(previous, current, next)) {
                tokens++;
            }
        }
        return tokens;
    }

    private static boolean shouldSplit(
            final char previous,
            final char current,
            final char next) {
        if (Character.isDigit(previous) != Character.isDigit(current)) {
            return true;
        }
        if (Character.isLowerCase(previous) && Character.isUpperCase(current)) {
            return true;
        }
        return Character.isUpperCase(previous)
                && Character.isUpperCase(current)
                && Character.isLowerCase(next);
    }

    private static boolean isOperatorPair(final String text, final int index) {
        if (index + 1 >= text.length()) {
            return false;
        }
        char current = text.charAt(index);
        char next = text.charAt(index + 1);
        return switch (current) {
            case '=', '!', '<', '>', '&', '|', '+', '-', '*', '/', '%', ':'
                -> next == current || next == '=' || (current == '-' && next == '>')
                        || (current == ':' && next == ':');
            default -> false;
        };
    }
}
