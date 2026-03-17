package ff.ss.javaFxAuditStudio.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * Proprietes de configuration du pipeline d'analyse.
 *
 * <p>Active via {@link AnalysisConfiguration} avec {@code @EnableConfigurationProperties}.
 * Chargee depuis le prefixe {@code analysis} dans {@code application.properties}.
 */
@ConfigurationProperties(prefix = "analysis")
public record AnalysisProperties(
        LifecycleMethods lifecycleMethods,
        ClassificationPatterns classificationPatterns) {

    public record LifecycleMethods(List<String> excluded) {
        public Set<String> asSet() {
            return excluded == null ? Set.of() : Set.copyOf(excluded);
        }
    }

    /**
     * Familles de mots-cles pour la classification de responsabilites des handlers JavaFX.
     *
     * <p>Chaque famille dispose d'une methode {@code effective*()} qui retourne la liste
     * configuree ou, si null, une liste de valeurs par defaut couvrant les cas courants.
     * Cela permet la surcharge via {@code application.properties} sans obliger a tout redefinir.
     */
    public record ClassificationPatterns(
            List<String> uiKeywords,
            List<String> applicationKeywords,
            List<String> businessKeywords,
            List<String> technicalKeywords,
            List<String> serviceCallSuffixes) {

        /** Retourne les mots-cles UI ou une liste par defaut si null. */
        public List<String> effectiveUiKeywords() {
            return uiKeywords != null ? uiKeywords : List.of(
                "setText", "setVisible", "getChildren", "setStyle", "setDisable", "getScene",
                "setGraphic", "setManaged", "pseudoClassStateChanged", "setItems"
            );
        }

        public List<String> effectiveApplicationKeywords() {
            return applicationKeywords != null ? applicationKeywords : List.of(
                "execute", "invoke", "useCase", "command", "submit", "dispatch",
                "process", "handle", "perform", "run", "apply", "trigger"
            );
        }

        public List<String> effectiveBusinessKeywords() {
            return businessKeywords != null ? businessKeywords : List.of(
                "service.save", "repository", "persist", "entityManager", "flush", "commit",
                "valider", "calculer", "verifier", "appliquer", "enregistrer"
            );
        }

        public List<String> effectiveTechnicalKeywords() {
            return technicalKeywords != null ? technicalKeywords : List.of(
                "restTemplate", "webClient", "http", "ftp", "socket", "printJob", "file.write",
                "connection", "stream", "serialize", "deserialize"
            );
        }

        /**
         * Suffixes qui, combines a ".", indiquent un appel de service
         * (ex: Service., Manager., Handler.).
         */
        public List<String> effectiveServiceCallSuffixes() {
            return serviceCallSuffixes != null ? serviceCallSuffixes : List.of(
                "Service.", "service.", "Manager.", "manager.", "Handler.", "handler.",
                "Processor.", "processor.", "Executor.", "executor.", "Calculateur.", "calculateur.",
                "Gestionnaire.", "gestionnaire.", "Moteur.", "moteur."
            );
        }
    }
}
