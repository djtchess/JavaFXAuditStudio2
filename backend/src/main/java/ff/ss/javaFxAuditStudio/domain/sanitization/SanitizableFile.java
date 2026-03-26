package ff.ss.javaFxAuditStudio.domain.sanitization;

import java.util.Objects;

/**
 * Fichier candidate a la sanitisation avant envoi au LLM (QW-5).
 *
 * <p>Represente un fichier source identifie par son nom, son contenu brut
 * et son type (extension sans le point). Supporte les types Java et non-Java
 * (yaml, properties, sql, xml).
 *
 * @param fileName  Nom du fichier (non null, non blank)
 * @param content   Contenu brut a sanitiser (non null)
 * @param fileType  Extension sans le point, ex. "java", "yaml", "properties", "sql" (non null, non blank)
 */
public record SanitizableFile(String fileName, String content, String fileType) {

    public SanitizableFile {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(fileType, "fileType must not be null");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (fileType.isBlank()) {
            throw new IllegalArgumentException("fileType must not be blank");
        }
    }

    /**
     * Retourne vrai si le fichier est un fichier Java.
     *
     * @return vrai si {@code fileType} est "java" (insensible a la casse)
     */
    public boolean isJava() {
        return "java".equalsIgnoreCase(fileType);
    }
}
