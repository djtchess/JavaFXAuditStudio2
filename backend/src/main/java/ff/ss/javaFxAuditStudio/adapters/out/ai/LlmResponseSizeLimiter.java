package ff.ss.javaFxAuditStudio.adapters.out.ai;

/**
 * Utility that limits response text size using UTF-8 byte length.
 */
final class LlmResponseSizeLimiter {

    record LimitedResponse(String text, int originalSizeBytes, boolean truncated) {}

    private LlmResponseSizeLimiter() {
    }

    static LimitedResponse limit(final String responseText, final int maxBytes) {
        String safeText = responseText != null ? responseText : "";
        int safeMaxBytes = maxBytes > 0 ? maxBytes : 0;
        int originalSizeBytes = utf8Length(safeText);
        if (originalSizeBytes <= safeMaxBytes) {
            return new LimitedResponse(safeText, originalSizeBytes, false);
        }
        return new LimitedResponse(
                truncateToUtf8Bytes(safeText, safeMaxBytes),
                originalSizeBytes,
                true);
    }

    private static int utf8Length(final String text) {
        int byteCount = 0;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            byteCount += utf8BytesFor(codePoint);
            index += Character.charCount(codePoint);
        }
        return byteCount;
    }

    private static String truncateToUtf8Bytes(final String text, final int maxBytes) {
        if (maxBytes <= 0 || text.isEmpty()) {
            return "";
        }
        int byteCount = 0;
        int endIndex = 0;
        while (endIndex < text.length()) {
            int codePoint = text.codePointAt(endIndex);
            int charBytes = utf8BytesFor(codePoint);
            if (byteCount + charBytes > maxBytes) {
                break;
            }
            byteCount += charBytes;
            endIndex += Character.charCount(codePoint);
        }
        return text.substring(0, endIndex);
    }

    private static int utf8BytesFor(final int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        if (codePoint <= 0xFFFF) {
            return 3;
        }
        return 4;
    }
}
