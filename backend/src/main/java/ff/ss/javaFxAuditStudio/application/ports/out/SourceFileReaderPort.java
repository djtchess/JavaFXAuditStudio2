package ff.ss.javaFxAuditStudio.application.ports.out;

import java.util.Optional;

/**
 * Port hexagonal de lecture de fichier source (IAP-5).
 *
 * <p>Isole le I/O filesystem de la couche application. Les adapters implementant
 * ce port gerent les erreurs d'acces et retournent {@code Optional.empty()} en
 * cas d'echec. Les services font {@code port.read(path).orElse(controllerRef)}.
 */
public interface SourceFileReaderPort {

    /**
     * Lit le contenu d'un fichier source.
     *
     * @param filePath chemin absolu vers le fichier
     * @return contenu du fichier, ou {@code Optional.empty()} si illisible
     */
    Optional<String> read(String filePath);
}
