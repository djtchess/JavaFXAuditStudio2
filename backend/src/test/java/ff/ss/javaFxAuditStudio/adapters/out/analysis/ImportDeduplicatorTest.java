package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.domain.generation.ArtifactType;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour ImportDeduplicator (JAS-009).
 */
class ImportDeduplicatorTest {

    private ImportDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        deduplicator = new ImportDeduplicator();
    }

    // -------------------------------------------------------------------------
    // Tests de processContent
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnContentUnchangedWhenNoImports() {
        // Cas 1 : contenu sans imports -> retourne identique
        String content = "public interface FooUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        assertThat(result).isEqualTo(content);
    }

    @Test
    void shouldDeduplicateDoubleImport() {
        // Cas 2 : deux fois le meme import -> un seul dans le resultat
        String content = "package com.example;\n\n"
                + "import java.util.List;\n"
                + "import java.util.List;\n\n"
                + "public interface FooUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        long count = countOccurrences(result, "import java.util.List;");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldPromoteHintImportToRealImport() {
        // Cas 3 : "// import Patient;" -> "import Patient;" dans le bloc imports
        String content = "package com.example;\n\n"
                + "// Imports suggeres (a ajuster selon le package reel) :\n"
                + "// import Patient;\n\n"
                + "public interface PatientUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        assertThat(result).contains("import Patient;");
        assertThat(result).doesNotContain("// import Patient;");
    }

    @Test
    void shouldDeduplicateWhenHintMatchesExistingImport() {
        // Cas 4 : "import Patient;" + "// import Patient;" -> un seul "import Patient;"
        String content = "package com.example;\n\n"
                + "import Patient;\n"
                + "// Imports suggeres (a ajuster selon le package reel) :\n"
                + "// import Patient;\n\n"
                + "public interface PatientUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        long count = countOccurrences(result, "import Patient;");
        assertThat(count).isEqualTo(1);
        assertThat(result).doesNotContain("// import Patient;");
    }

    @Test
    void shouldRemoveHintBlockCommentLine() {
        // Cas 5 : la ligne "// Imports suggeres (a ajuster selon le package reel) :" est absente du resultat
        String content = "package com.example;\n\n"
                + "// Imports suggeres (a ajuster selon le package reel) :\n"
                + "// import Patient;\n\n"
                + "public interface PatientUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        assertThat(result).doesNotContain("// Imports suggeres");
    }

    @Test
    void shouldPreserveOrderOfFirstOccurrence() {
        // Cas 6 : "import java.util.List;" puis "import Patient;" -> meme ordre dans le resultat
        String content = "package com.example;\n\n"
                + "import java.util.List;\n"
                + "import Patient;\n\n"
                + "public interface PatientUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        int posListImport = result.indexOf("import java.util.List;");
        int posPatientImport = result.indexOf("import Patient;");
        assertThat(posListImport).isLessThan(posPatientImport);
    }

    @Test
    void shouldReturnEmptyContentUnchanged() {
        // Cas 7 : contenu vide -> retourne tel quel
        assertThat(deduplicator.processContent("")).isEqualTo("");
        assertThat(deduplicator.processContent(null)).isNull();
        assertThat(deduplicator.processContent("   ")).isEqualTo("   ");
    }

    @Test
    void shouldPreservePackageDeclarationBeforeImports() {
        // Verifie que la ligne package reste avant les imports
        String content = "package com.example.migration;\n\n"
                + "// Imports suggeres (a ajuster selon le package reel) :\n"
                + "// import ExamenType;\n\n"
                + "public interface ExamenUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        int posPackage = result.indexOf("package com.example.migration;");
        int posImport = result.indexOf("import ExamenType;");
        assertThat(posPackage).isGreaterThanOrEqualTo(0);
        assertThat(posImport).isGreaterThanOrEqualTo(0);
        assertThat(posPackage).isLessThan(posImport);
    }

    @Test
    void shouldHandleMultipleHintTypes() {
        // Plusieurs hints differents sont tous promus en vrais imports
        String content = "package com.example;\n\n"
                + "// Imports suggeres (a ajuster selon le package reel) :\n"
                + "// import Patient;\n"
                + "// import ExamenType;\n\n"
                + "public interface DiagnosticUseCase {\n    void save();\n}\n";

        String result = deduplicator.processContent(content);

        assertThat(result).contains("import Patient;");
        assertThat(result).contains("import ExamenType;");
        assertThat(result).doesNotContain("// import Patient;");
        assertThat(result).doesNotContain("// import ExamenType;");
    }

    // -------------------------------------------------------------------------
    // Tests d'integration sur dedup(CodeArtifact)
    // -------------------------------------------------------------------------

    @Test
    void shouldTransformArtifactWithHintImport() {
        // Cas 8 : artefact avec hint "// import Patient;" -> contenu transforme avec import reel
        String content = "package com.example;\n\n"
                + "// Imports suggeres (a ajuster selon le package reel) :\n"
                + "// import Patient;\n\n"
                + "public interface PatientUseCase {\n    void save();\n}\n";
        CodeArtifact artifact = buildArtifact(content);

        CodeArtifact result = deduplicator.dedup(artifact);

        assertThat(result.content()).contains("import Patient;");
        assertThat(result.content()).doesNotContain("// import Patient;");
        // Les autres champs sont conserves
        assertThat(result.artifactId()).isEqualTo(artifact.artifactId());
        assertThat(result.type()).isEqualTo(artifact.type());
        assertThat(result.lotNumber()).isEqualTo(artifact.lotNumber());
        assertThat(result.className()).isEqualTo(artifact.className());
        assertThat(result.transitionalBridge()).isEqualTo(artifact.transitionalBridge());
    }

    @Test
    void shouldReturnOriginalInstanceWhenNoHintNorDuplicate() {
        // Cas 9 : artefact sans hints ni doublons -> retourne l'instance originale (assertSame)
        String content = "public interface FooUseCase {\n    void save();\n}\n";
        CodeArtifact artifact = buildArtifact(content);

        CodeArtifact result = deduplicator.dedup(artifact);

        assertThat(result).isSameAs(artifact);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CodeArtifact buildArtifact(final String content) {
        return new CodeArtifact(
                "test-lot2-use_case",
                ArtifactType.USE_CASE,
                2,
                "PatientUseCase",
                content,
                false);
    }

    private long countOccurrences(final String text, final String substring) {
        long count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) >= 0) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
