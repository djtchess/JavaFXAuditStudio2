package ff.ss.javaFxAuditStudio.adapters.out.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calcule un hash SHA-256 hex du contenu sanitise pour l'audit (JAS-029).
 * Ne jamais loguer le contenu hashe.
 */
public class PayloadHasher {

    private static final String SHA_256 = "SHA-256";

    /**
     * Calcule le hash SHA-256 hex du contenu sanitise.
     *
     * @param sanitizedContent contenu a hasher (null ou vide traites comme chaine vide)
     * @return hash SHA-256 en hexadecimal sur 64 caracteres
     */
    public String hash(final String sanitizedContent) {
        String content = (sanitizedContent != null) ? sanitizedContent : "";
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String toHex(final byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
