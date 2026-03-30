package ff.ss.javaFxAuditStudio.adapters.out.sanitization;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationRuleType;
import ff.ss.javaFxAuditStudio.domain.sanitization.SanitizationTransformation;

/**
 * Audit pré-sanitisation du pipeline (Action 6 / JAS-018).
 *
 * <p>Ce sanitizer est positionne en tete du pipeline, avant toute transformation.
 * Il n'applique <strong>aucune modification</strong> au source Java : il retourne le source inchange.
 *
 * <p>Son seul role est de comptabiliser les elements sensibles detectes dans le source brut
 * afin d'etablir une base de reference pour le rapport final de sanitisation.
 * Les categories analysees sont :
 * <ol>
 *   <li>Noms de classes avec suffixes metier (Service, Manager, Controller, etc.)</li>
 *   <li>Secrets potentiels : annotations {@code @Value("${...}")}, champs password/secret/token</li>
 *   <li>URLs hardcodees (http/https)</li>
 *   <li>Adresses email</li>
 *   <li>Commentaires (une ligne et multi-lignes)</li>
 *   <li>Annotations JPA : {@code @Column}, {@code @Id}, {@code @ManyToOne}, {@code @Entity}</li>
 * </ol>
 *
 * <p>Thread-safety : le compteur est reinitialise localement a chaque appel de {@link #apply(String)}.
 * La valeur est conservee via un {@link AtomicInteger} pour garantir une visibilite correcte
 * dans les environnements concurrents.
 *
 * <p>Assemble via {@code @Bean} dans {@code AiEnrichmentOrchestraConfiguration}.
 */
public class PreSanitizationAuditSanitizer implements Sanitizer {

    /** Classes metier reconnaissables par leur suffixe (source : BusinessTermDictionary). */
    private static final Pattern BUSINESS_CLASS_PATTERN =
            BusinessTermDictionary.BUSINESS_IDENTIFIER_PATTERN;

    /** Annotations Spring @Value("${...}") et champs nommes password/secret/token/credentials. */
    private static final Pattern SECRET_FIELD_PATTERN = Pattern.compile(
            "@Value\\(\"\\$\\{[^}]+\\}\"\\)"
            + "|(?i)\\b(?:password|secret|token|credentials|apiKey|api_key)\\s*[=:]");

    /** URLs hardcodees http ou https. */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"']+");

    /** Adresses email standard. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    /** Commentaires Java : //... ou /* ... *​/ */
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "//[^\n]*|/\\*[\\s\\S]*?\\*/");

    /** Annotations JPA courantes (@Column, @Id, @ManyToOne, @OneToMany, @Entity, @Table). */
    private static final Pattern JPA_ANNOTATION_PATTERN = Pattern.compile(
            "@(?:Column|Id|ManyToOne|OneToMany|ManyToMany|OneToOne|Entity|Table|JoinColumn)\\b");

    private final AtomicInteger detectedCount = new AtomicInteger(0);

    @Override
    public String apply(final String source) {
        detectedCount.set(countSensitivePatterns(source));
        return source;
    }

    @Override
    public SanitizationTransformation report() {
        return new SanitizationTransformation(
                SanitizationRuleType.PRE_SANITIZATION_AUDIT,
                detectedCount.get(),
                "Audit pre-sanitisation : " + detectedCount.get()
                        + " element(s) sensible(s) detecte(s) avant transformation");
    }

    @Override
    public SanitizationRuleType ruleType() {
        return SanitizationRuleType.PRE_SANITIZATION_AUDIT;
    }

    private int countSensitivePatterns(final String source) {
        int count = 0;
        count += countMatches(source, BUSINESS_CLASS_PATTERN);
        count += countMatches(source, SECRET_FIELD_PATTERN);
        count += countMatches(source, URL_PATTERN);
        count += countMatches(source, EMAIL_PATTERN);
        count += countMatches(source, COMMENT_PATTERN);
        count += countMatches(source, JPA_ANNOTATION_PATTERN);
        return count;
    }

    private int countMatches(final String source, final Pattern pattern) {
        int count = 0;
        java.util.regex.Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
