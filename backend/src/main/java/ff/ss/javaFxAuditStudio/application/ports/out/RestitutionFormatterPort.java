package ff.ss.javaFxAuditStudio.application.ports.out;

import ff.ss.javaFxAuditStudio.domain.restitution.RestitutionReport;

/**
 * Port sortant pour la mise en forme d'un rapport de restitution.
 * Les adapters techniques (Markdown, HTML, PDF, etc.) implementent ce port
 * sans introduire de logique metier.
 */
public interface RestitutionFormatterPort {

    /**
     * Formate le rapport en Markdown lisible par developpeurs et Product Owner.
     *
     * @param report rapport complet, non null
     * @return contenu Markdown, jamais null ni vide
     */
    String formatAsMarkdown(RestitutionReport report);
}
