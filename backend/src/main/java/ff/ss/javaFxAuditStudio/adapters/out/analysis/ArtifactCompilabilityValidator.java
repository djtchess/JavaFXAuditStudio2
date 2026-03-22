package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import ff.ss.javaFxAuditStudio.domain.generation.ArtifactValidationWarning;
import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JAS-009 — Valide la compilabilite structurelle d'un artefact genere.
 *
 * <p>Utilise JavaParser pour detecter :
 * <ul>
 *   <li>les doublons de noms de methodes dans le meme type ;</li>
 *   <li>les imports manifestes manquants (types non qualifies sans import declare) ;</li>
 *   <li>les corps vides (aucune methode significative) ;</li>
 *   <li>les erreurs de syntaxe Java.</li>
 * </ul>
 *
 * <p>Cette classe est un adapter technique pur : aucune logique metier, pas d'annotation Spring,
 * pas de dependance vers le domaine autre que les types de sortie.
 */
public final class ArtifactCompilabilityValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactCompilabilityValidator.class);

    /**
     * Valide le contenu Java de l'artefact et retourne un artefact enrichi des avertissements.
     *
     * @param artifact artefact a valider
     * @return artefact enrichi avec la liste d'avertissements et le statut mis a jour
     */
    public CodeArtifact validate(final CodeArtifact artifact) {
        List<ArtifactValidationWarning> warnings = new ArrayList<>();
        CompilationUnit cu = tryParse(artifact, warnings);
        if (cu == null) {
            return rebuildWithWarnings(artifact, warnings);
        }
        checkDuplicateMethods(cu, warnings);
        checkMissingImports(cu, warnings);
        checkEmptyBody(cu, warnings);
        return rebuildWithWarnings(artifact, warnings);
    }

    private CompilationUnit tryParse(
            final CodeArtifact artifact,
            final List<ArtifactValidationWarning> warnings) {
        try {
            return StaticJavaParser.parse(artifact.content());
        } catch (ParseProblemException e) {
            LOG.debug("[JAS-009] Erreur de parsing pour '{}': {}", artifact.artifactId(), e.getMessage());
            warnings.add(ArtifactValidationWarning.PARSE_ERROR);
            return null;
        }
    }

    private void checkDuplicateMethods(
            final CompilationUnit cu,
            final List<ArtifactValidationWarning> warnings) {
        Set<String> seen = new HashSet<>();
        boolean hasDuplicate = cu.findAll(MethodDeclaration.class).stream()
                .map(MethodDeclaration::getNameAsString)
                .anyMatch(name -> !seen.add(name));
        if (hasDuplicate) {
            warnings.add(ArtifactValidationWarning.DUPLICATE_METHOD_NAME);
        }
    }

    private void checkMissingImports(
            final CompilationUnit cu,
            final List<ArtifactValidationWarning> warnings) {
        Set<String> imported = collectImportedSimpleNames(cu);
        boolean hasMissing = cu.findAll(com.github.javaparser.ast.type.ClassOrInterfaceType.class)
                .stream()
                .map(t -> t.getNameAsString())
                .filter(name -> !isPrimitive(name) && !isJavaLang(name))
                .anyMatch(name -> !imported.contains(name));
        if (hasMissing) {
            warnings.add(ArtifactValidationWarning.MISSING_IMPORT);
        }
    }

    private Set<String> collectImportedSimpleNames(final CompilationUnit cu) {
        Set<String> names = new HashSet<>();
        cu.getImports().forEach(imp -> {
            String importStr = imp.getNameAsString();
            int dot = importStr.lastIndexOf('.');
            if (dot >= 0) {
                names.add(importStr.substring(dot + 1));
            } else {
                names.add(importStr);
            }
        });
        return names;
    }

    private void checkEmptyBody(
            final CompilationUnit cu,
            final List<ArtifactValidationWarning> warnings) {
        boolean hasMethods = !cu.findAll(MethodDeclaration.class).isEmpty();
        if (!hasMethods) {
            warnings.add(ArtifactValidationWarning.EMPTY_BODY);
        }
    }

    private CodeArtifact rebuildWithWarnings(
            final CodeArtifact artifact,
            final List<ArtifactValidationWarning> warnings) {
        String status = warnings.isEmpty() ? "OK" : "WARNING";
        return new CodeArtifact(
                artifact.artifactId(),
                artifact.type(),
                artifact.lotNumber(),
                artifact.className(),
                artifact.content(),
                artifact.transitionalBridge(),
                warnings,
                status);
    }

    private boolean isPrimitive(final String name) {
        return switch (name) {
            case "void", "int", "long", "double", "float", "boolean", "byte", "short", "char" -> true;
            default -> false;
        };
    }

    private boolean isJavaLang(final String name) {
        return switch (name) {
            case "String", "Object", "Integer", "Long", "Double", "Float",
                    "Boolean", "Byte", "Short", "Character", "Number",
                    "Comparable", "Iterable", "Runnable", "Exception",
                    "RuntimeException", "Enum", "Record", "Class" -> true;
            default -> false;
        };
    }
}
