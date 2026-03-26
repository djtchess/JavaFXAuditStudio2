package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Source unique de verite pour les suffixes metier utilises lors de la sanitisation
 * des identifiants Java (JAS-018, QW-2).
 *
 * <p>Les deux sanitizers AST et regex ({@link IdentifierSanitizer} et
 * {@link OpenRewriteIdentifierSanitizer}) s'appuient exclusivement sur cette classe
 * pour garantir une couverture identique quel que soit le mode de traitement.
 *
 * <p>Tous les membres sont {@code final} et immutables. Aucune dependance Spring, JPA
 * ni autre framework technique : cette classe reste un detail d'implementation de
 * l'adapter de sanitisation.
 */
public final class BusinessTermDictionary {

    /**
     * Liste ordonnee des suffixes metier reconnus.
     * Toute modification ici se repercute automatiquement dans les deux sanitizers.
     */
    static final List<String> BUSINESS_SUFFIXES = List.of(
            "Service",
            "Manager",
            "Controller",
            "Repository",
            "Gateway",
            "Handler",
            "Processor",
            "Calculator",
            "Engine"
    );

    /**
     * Alternance regex construite a partir de {@link #BUSINESS_SUFFIXES}, prete a
     * etre injectee dans un {@link Pattern}.
     * Exemple : {@code "Service|Manager|Controller|..."}.
     */
    static final String BUSINESS_SUFFIXES_ALTERNATION = String.join("|", BUSINESS_SUFFIXES);

    /**
     * Pattern complet ciblant les identifiants metier du style {@code OrderService},
     * {@code InvoiceManager} ou {@code CustomerController}.
     *
     * <p>Cible : un ou plusieurs mots CamelCase suivis d'un suffixe metier reconnu,
     * dans n'importe quel contexte (declaration, reference, cast, new-expression…).
     */
    static final Pattern BUSINESS_IDENTIFIER_PATTERN = Pattern.compile(
            "\\b([A-Z][a-z]+(?:[A-Z][a-z]+)*)(?:" + BUSINESS_SUFFIXES_ALTERNATION + ")\\b");

    /**
     * Pattern ciblant uniquement les <em>declarations</em> de classe dont le nom
     * contient un suffixe metier : {@code class CustomerService}, {@code class InvoiceManager}…
     *
     * <p>Utilise par le mode fallback regex de {@link OpenRewriteIdentifierSanitizer}.
     */
    static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
            "\\bclass\\s+([A-Z][A-Za-z0-9_]*(?:" + BUSINESS_SUFFIXES_ALTERNATION + "))\\b");

    /**
     * Pattern ciblant le nom de classe complet incluant le suffixe, pour les verificatons
     * AST ({@link OpenRewriteIdentifierSanitizer}).
     *
     * <p>Exemple : {@code "^[A-Z][A-Za-z0-9_]*(?:Service|Manager|...)$"}.
     */
    static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "^[A-Z][A-Za-z0-9_]*(?:" + BUSINESS_SUFFIXES_ALTERNATION + ")$");

    private BusinessTermDictionary() {
        // utilitaire — pas d'instanciation
    }
}
