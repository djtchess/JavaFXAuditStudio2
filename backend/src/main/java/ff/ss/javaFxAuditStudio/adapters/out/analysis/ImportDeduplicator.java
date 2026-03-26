package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * JAS-009 — Post-traitement des imports dans les artefacts generes.
 *
 * <p>Deux operations en une passe sur le contenu source :
 * <ol>
 *   <li>Convertit les hints {@code // import X;} (generes par JAS-008) en vrais imports.</li>
 *   <li>Deduplique le bloc d'imports (ordre de premiere occurrence conserve).</li>
 * </ol>
 *
 * <p>Adapter technique pur : pas d'annotation Spring, instanciation directe.
 */
public final class ImportDeduplicator {

    /**
     * Retourne un nouvel artefact dont le contenu a les imports depollues.
     * Si aucun hint ni doublon, retourne l'artefact original sans modification.
     */
    public CodeArtifact dedup(final CodeArtifact artifact) {
        String cleaned = processContent(artifact.content());
        if (cleaned.equals(artifact.content())) {
            return artifact;
        }
        // Reconstruire l'artefact avec le nouveau contenu (meme statut — warnings recalcules apres)
        return new CodeArtifact(
                artifact.artifactId(),
                artifact.type(),
                artifact.lotNumber(),
                artifact.className(),
                cleaned,
                artifact.transitionalBridge());
    }

    /**
     * Traite le contenu Java source :
     * 1. Supprime les lignes de commentaire-bloc hint (// Imports suggeres...)
     * 2. Promeut les hints // import X; en vrais import X;
     * 3. Deduplique les import X; (ordre de premiere occurrence conserve via LinkedHashSet)
     *
     * <p>Visibilite package-private pour les tests unitaires.
     */
    String processContent(final String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String[] lines = content.split("\n", -1);
        // Collecter les imports reels (dedupliques via LinkedHashSet)
        LinkedHashSet<String> importLines = new LinkedHashSet<>();
        // Lignes hors imports (package, javadoc, declaration de classe, corps...)
        List<String> otherLines = new ArrayList<>();
        // Indique si on a rencontre au moins un import ou un hint
        boolean hasAnyImport = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Ignorer la ligne de commentaire bloc de hints generee par JAS-008
            if (trimmed.equals("// Imports suggeres (a ajuster selon le package reel) :")) {
                hasAnyImport = true;
                continue;
            }

            // Promouvoir les hints "// import X;" en vrais imports
            if (trimmed.startsWith("// import ") && trimmed.endsWith(";")) {
                // Extraire la partie apres "// " -> "import X;"
                String withoutComment = trimmed.substring(3); // supprime "// "
                // withoutComment est maintenant "import X;"
                importLines.add(withoutComment);
                hasAnyImport = true;
                continue;
            }

            // Deduplication des vrais imports existants
            if (trimmed.startsWith("import ") && trimmed.endsWith(";")) {
                importLines.add(trimmed);
                hasAnyImport = true;
                continue;
            }

            otherLines.add(line);
        }

        // Rien a faire si aucun import ni hint rencontre
        if (!hasAnyImport) {
            return content;
        }

        // Verifier s'il y avait des doublons ou des hints
        // Si importLines a le meme contenu que les imports originaux (pas de hint, pas de doublon),
        // on recalcule pour comparer
        long originalImportCount = java.util.Arrays.stream(lines)
                .filter(l -> l.trim().startsWith("import ") && l.trim().endsWith(";"))
                .count();
        long originalHintCount = java.util.Arrays.stream(lines)
                .filter(l -> l.trim().startsWith("// import ") && l.trim().endsWith(";"))
                .count();

        if (originalHintCount == 0 && importLines.size() == originalImportCount) {
            // Pas de hints et pas de doublons : retourner le contenu original
            return content;
        }

        // Reconstruction : extraire la ligne package (si presente) depuis otherLines
        String packageLine = null;
        List<String> nonPackageLines = new ArrayList<>();
        for (String line : otherLines) {
            String t = line.trim();
            if (packageLine == null && t.startsWith("package ") && t.endsWith(";")) {
                packageLine = line;
            } else {
                nonPackageLines.add(line);
            }
        }

        // Construire le resultat
        StringBuilder sb = new StringBuilder();

        // 1. Ligne package + ligne vide
        if (packageLine != null) {
            sb.append(packageLine).append("\n");
            sb.append("\n");
        }

        // 2. Bloc imports dedupliques
        if (!importLines.isEmpty()) {
            for (String imp : importLines) {
                sb.append(imp).append("\n");
            }
            sb.append("\n");
        }

        // 3. Le reste du contenu (supprimer les lignes vides initiales residuelles)
        int start = 0;
        while (start < nonPackageLines.size() && nonPackageLines.get(start).trim().isEmpty()) {
            start++;
        }
        for (int i = start; i < nonPackageLines.size(); i++) {
            sb.append(nonPackageLines.get(i)).append("\n");
        }

        // Supprimer le dernier \n superflu si le contenu original ne se terminait pas par \n
        String result = sb.toString();
        if (!content.endsWith("\n") && result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
