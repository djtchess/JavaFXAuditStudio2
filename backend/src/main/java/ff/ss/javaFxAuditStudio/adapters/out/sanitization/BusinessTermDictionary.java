package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.List;
import java.util.Set;
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
     *
     * <p>Couvre :
     * <ul>
     *   <li>Couche applicative courante : Service, Manager, Controller, Repository, etc.</li>
     *   <li>Patterns DDD : Entity, ValueObject, Aggregate, DomainService, DomainEvent</li>
     *   <li>Architecture hexagonale : Port, UseCase, Interactor, Command, Query, Event</li>
     *   <li>GoF : Facade, Adapter, Delegate, Strategy, Observer, Decorator</li>
     *   <li>Creation : Factory, Builder</li>
     *   <li>Validation : Validator, Checker, Guard</li>
     *   <li>Mapping : Mapper, Converter, Transformer, Assembler</li>
     *   <li>JavaFX / MVP : Presenter, ViewModel, Model</li>
     *   <li>Spring/Jakarta : Component, Bean, Configuration, Provider</li>
     * </ul>
     */
    static final List<String> BUSINESS_SUFFIXES = List.of(
            // Couche applicative courante
            "Service",
            "Manager",
            "Controller",
            "Repository",
            "Gateway",
            "Handler",
            "Processor",
            "Calculator",
            "Engine",
            // Patterns DDD
            "Entity",
            "ValueObject",
            "Aggregate",
            "DomainService",
            "DomainEvent",
            // Architecture hexagonale
            "Port",
            "UseCase",
            "Interactor",
            "Command",
            "Query",
            "Event",
            // GoF
            "Facade",
            "Delegate",
            "Strategy",
            "Observer",
            "Decorator",
            // Creation
            "Factory",
            "Builder",
            // Validation
            "Validator",
            "Checker",
            "Guard",
            // Mapping
            "Mapper",
            "Converter",
            "Transformer",
            "Assembler",
            // JavaFX / MVP
            "Presenter",
            "ViewModel",
            // Spring/Jakarta
            "Component",
            "Provider"
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

    /**
     * Pattern ciblant les noms de methodes metier publiques ou protected.
     *
     * <p>Une methode est consideree metier si son nom :
     * <ul>
     *   <li>se termine par un suffixe metier reconnu (ex : {@code processOrder}), OU</li>
     *   <li>contient un radical metier connu (defini dans {@link #BUSINESS_METHOD_TERMS}).</li>
     * </ul>
     *
     * <p>Ce pattern n'est utilise que dans le fallback regex.
     */
    static final Pattern METHOD_DECLARATION_PATTERN = Pattern.compile(
            "(?m)^\\s*(?:public|protected)\\s+(?:[\\w<>\\[\\]]+\\s+)+"
            + "([a-z][A-Za-z0-9_]*)\\s*\\(");

    /**
     * Termes qui, presents dans un nom de methode, la qualifient de metier.
     * Insensibles a la casse lors de la comparaison (toLowerCase).
     */
    static final Set<String> BUSINESS_METHOD_TERMS = Set.of(
            "calculer", "compute", "calculate",
            "traiter", "process", "handle",
            "valider", "validate", "check",
            "enregistrer", "save", "persist",
            "charger", "load", "fetch",
            "generer", "generate", "build",
            "convertir", "convert", "transform",
            "exporter", "export",
            "importer", "import",
            "initialiser", "init", "setup",
            "creer", "create",
            "supprimer", "delete", "remove",
            "modifier", "update", "edit",
            "rechercher", "search", "find",
            "afficher", "display", "show",
            "notifier", "notify", "alert",
            "planifier", "schedule",
            "executer", "execute", "run",
            "envoyer", "send", "publish",
            "recevoir", "receive", "consume"
    );

    /**
     * Methodes du cycle de vie Java/JavaFX/Spring qui ne doivent JAMAIS etre renommees.
     */
    static final Set<String> LIFECYCLE_METHODS = Set.of(
            "main",
            "toString",
            "equals",
            "hashCode",
            "compareTo",
            "clone",
            "finalize",
            "initialize",
            "dispose",
            "start",
            "stop",
            "init",
            "destroy",
            "shutdown",
            "close",
            "open",
            "run",
            "call",
            "get",
            "set",
            "is"
    );

    /**
     * Segments de packages organisationnels a anonymiser.
     * Remplace les segments non-standards (noms d'organisations, projets, etc.)
     * par {@code com.neutralized}.
     */
    static final Set<String> SENSITIVE_PACKAGE_SEGMENTS = Set.of(
            "cnamts",
            "ameli",
            "cpam",
            "assurance",
            "sante",
            "mutuelle",
            "prevoyance",
            "retraite",
            "chomage",
            "rsa",
            "caf",
            "secu",
            "securite_sociale"
    );

    /**
     * Packages techniques "safe" qui ne doivent jamais etre anonymises.
     */
    static final Set<String> SAFE_PACKAGE_PREFIXES = Set.of(
            "java.",
            "javax.",
            "jakarta.",
            "org.springframework.",
            "com.fasterxml.",
            "org.slf4j.",
            "org.junit.",
            "org.assertj.",
            "org.mockito.",
            "org.openrewrite.",
            "com.github.",
            "com.h2database.",
            "org.postgresql.",
            "org.flywaydb."
    );

    private BusinessTermDictionary() {
        // utilitaire — pas d'instanciation
    }
}
