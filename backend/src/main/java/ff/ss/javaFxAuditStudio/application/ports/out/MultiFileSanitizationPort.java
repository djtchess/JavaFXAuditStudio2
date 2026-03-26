package ff.ss.javaFxAuditStudio.application.ports.out;

import java.util.List;

import ff.ss.javaFxAuditStudio.domain.ai.SanitizedBundle;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizableFile;

/**
 * Port sortant de sanitisation multi-fichiers (QW-5).
 *
 * <p>Complement additif de {@link SanitizationPort} : gere des lots de fichiers
 * de types heterogenes (Java, YAML, properties, SQL, XML).
 *
 * <p>Pour les fichiers Java, l'implementation deleguera a {@link SanitizationPort}.
 * Pour les fichiers non-Java, elle appliquera uniquement les sanitizers adaptes
 * (secrets et scan Semgrep avec extension correcte), sans traitement AST ni
 * remplacement d'identifiants Java.
 *
 * <p>Ce port ne remplace pas et ne modifie pas {@link SanitizationPort}.
 * Les consommateurs existants de {@link SanitizationPort} ne sont pas impactes.
 */
public interface MultiFileSanitizationPort {

    /**
     * Sanitise une liste de fichiers et retourne un bundle sanitise par fichier.
     *
     * <p>Le traitement est applique fichier par fichier. L'ordre de la liste
     * d'entree est conserve dans la liste de sortie. Si la liste d'entree est
     * vide, une liste vide est retournee.
     *
     * @param bundleId identifiant de session pour la tracabilite (non null, non blank)
     * @param files    liste des fichiers a sanitiser (non null, peut etre vide)
     * @return liste de bundles sanitises, un par fichier, jamais null
     */
    List<SanitizedBundle> sanitizeFiles(String bundleId, List<SanitizableFile> files);
}
