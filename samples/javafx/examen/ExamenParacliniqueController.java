package terminal.operation.transit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import .business.AttributionEquipementService;
import .business.SignalDiffusionService;
import .business.PassagerService;
import .business.SyntheseTransitService;
import .business.ArchivageDocumentService;
import .business.OperationTransitFinaliseeService;
import .business.FichePassageService;
import .business.LanceurModuleService;
import .business.AcheminementService;
import .business.commons.exception.FileNotFoundInDirectoryException;
import .business.commons.exception.MaterielConnectedAquisitionFileNotFoundException;
import .business.commons.exception.MaterielConnectedAquisitionResultsEmptyException;
import .business.commons.validation.AbstractValidationRule;
import .business.commons.validation.ErrorLevel;
import .business.commons.validation.ValidationError;
import .business.commons.validation.ValidationRule;
import .business.commons.validation.ValidationRuleResult;
import .business.moteurregle.RegleContext;
import .business.moteurregle.RegleResultat;
import .editique.commons.exception.Hl7MessageReaderException;
import .model.AttributionEquipement;
import .model.SignalObserve;
import .model.SignalEquipementValide;
import .model.SignalDiffuse;
import .model.SignalObserveValide;
import .model.Consultant;
import .model.Dialog3BoutonsParam;
import .model.DocumentAnnexe;
import .model.DonneesFinalisees;
import .model.DossierTransit;
import .model.EnumStyleClassBouton;
import .model.OperationTransitEquipement;
import .model.OperationTransitFinalisee;
import .model.FapDataRdv;
import .model.AnnexeOperation;
import .model.AnnexeOperationTransit;
import .model.ReferenceTransit;
import .model.RapportTransit;
import .model.ZoneTransit;
import .model.Operateur;
import .model.builder.OperationTransitEquipementBuilder;
import .model.commons.PrintableObject;
import .model.commons.exception.ModelTechnicalException;
import .model.context.ClientContextManager;
import .model.converter.DocumentVersRapportTransitConverter;
import .model.criterias.OperationTransitFinaliseeCriteres;
import .model.criterias.RechercheAttributionEquipementCriteres;
import .model.ecg.TransmissionSignalSecondaireQuery;
import .model.enumeration.EnumActionTerminal;
import .model.enumeration.EnumCodeSignalStandard;
import .model.enumeration.EnumTerminal;
import .model.enumeration.EnumErreurLancementOperation;
import .model.enumeration.EnumRole;
import .model.enumeration.EnumFormatRapportTransit;
import .model.enumeration.EnumInteraction;
import .model.enumeration.EnumListeFormatSignal;
import .model.enumeration.EnumOuiNonIndetermineEnCours;
import .model.enumeration.EnumPrintObject;
import .model.enumeration.EnumStatutDossierTransit;
import .model.enumeration.EnumStatutPhase;
import .model.enumeration.EnumTypeRapport;
import .model.enumeration.EnumTypeElementSignal;
import .model.enumeration.EnumTypeRessource;
import .model.enumeration.Habilitation;
import .model.examen.FichePassageCritere;
import .model.factory.SignalEquipementValideFactory;
import .model.factory.SignalObserveValideFactory;
import .model.factory.SignalObserveValideVoFactory;
import .model.factory.RapportTransitFactory;
import .model.factory.OperateurVoFactory;
import .model.helper.SignalObserveValideHelper;
import .model.prestation.EnumTypePhase;
import .model.prestation.Interaction;
import .model.prestation.InteractionRealisee;
import .model.prestation.configurationbloc.BlocTerminal;
import .model.prestation.configurationbloc.SignalTransitConfiguration;
import .model.prestation.configurationbloc.EnumBlocTerminalDynamiqueAutres;
import .model.prestation.configurationbloc.EnumBlocTerminalGrilleDynamique;
import .model.prestation.configurationbloc.GrilleDynamiqueTerminal;
import .model.prestation.configurationbloc.ModeleTerminal;
import .model.prestation.elementgrilledynamique.ElementSignal;
import .model.prestation.elementgrilledynamique.ElementGrilleTerminal;
import .model.prestation.elementgrilledynamique.EnumSignalCardiaqueStandard;
import .model.prestation.elementgrilledynamique.EnumSignalRespiratoireStandard;
import .model.prestation.elementgrilledynamique.LibelleRegleTerminal;
import .model.production.conclusion.SyntheseTransitDonnees;
import .model.vo.ConfigurationTerminalVo;
import .model.vo.SignalEquipementValideVo;
import .model.vo.SignalObserveValideVo;
import .model.vo.OperationTransitFinaliseeVo;
import .model.vo.OperationTransitVo;
import .model.vo.TraceVo;
import .model.vo.OperateurVo;
import .rest.communication.formdata.ResourceRequest;
import ui.commons.configuration.TerminalConfig;
import ui.commons.javafx.controller.ControllerMode;
import ui.commons.javafx.controller.formfield.FormField;
import ui.commons.javafx.spinner.Spinner;
import ui.dialog.DialogConfirmationController;
import ui.view.interfacegenerale.contentarea.EnumContentArea;
import ui.view.production.ecrandynamique.ElementSignalPropriete;
import ui.view.production.ecrandynamique.ElementGrilleTerminalPropriete;
import ui.view.production.ecrandynamique.ElementSignalFactory;
import ui.view.production.ecrandynamique.ElementSaisieGrillePropriete;
import ui.view.production.ecrandynamique.GrilleTerminalUi;
import ui.view.production.ecrandynamique.ModuleDynamiqueImpl;
import terminal.operation.transit.base.AbstractExamenController;
import terminal.operation.transit.common.GrilleDynamiqueTerminalUtils;
import terminal.operation.transit.components.OperationsFinaliseesGridPane;
import terminal.operation.transit.components.OperationsFinaliseesTableView;
import ui.view.production.model.ResultatSignalObserve;
import fr.cnamts.cpam.bordeaux.utils.RapportTransitUtils;
import fr.cnamts.cpam.bordeaux.utils.FileUtils;
import fr.cnamts.cpam.bordeaux.utils.JsonUtils;

@Component
@Scope("prototype")
public class OperationTransitController extends AbstractExamenController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationTransitController.class);

    public static final String FORM_URL = "texte1";
    public static final String MENU_NAME = "texte2";
    public static final EnumContentArea CONTENT_AREA = EnumContentArea.texte3;
    private static final LinkedList<String> LIST_CSS_FILES = new LinkedList<>(Arrays.asList(
            "texte4", "texte5",
            "texte6"));

    static final String TITRE_BOITE_DIALOGUE_SUPPRESSION_OPERATION = "texte7";
    static final String MESSAGE_BOITE_DIALOGUE_SUPPRESSION_OPERATION = "texte8";
    static final String TITRE_MODALE_IMPRESSION_SIMPLE = "texte9";
    static final String TITRE_MODALE_IMPRESSION_AVEC_MAJ_STATUT_PHASE = "texte10";
    static final String MESSAGE_ENREGISTREMENT_DOCUMENT = "texte11";
    static final String MESSAGE_GENERATION_AJOUT_ANNEXE_DOSSIER = "texte12";
    static final String DIALOGBOX_TEXTE_BOUTON1_OUVRIR_SEULEMENT = "texte13";
    static final String DIALOGBOX_TEXTE_BOUTON2_GENERER_ET_CLOTURER = "texte14";
    static final String DIALOGBOX_TEXTE_BOUTON2_ENREGISTRER_DANS_DOSSIER = "texte15";
    static final String DIALOGBOX_TEXTE_BOUTON3_AJOUTER_PJ_ET_CLOTURER = "texte16";

    static final String OUTPUT_MESSAGE_TITLE = "texte17";
    static final String OUTPUT_MESSAGE = "Des modifications ont été mises en place. Celles-ci n'ont pas été enregistrées.\n Etes-vous sûr de vouloir quitter l'opération actuelle ?";
    static final String CLE_OPERATION_TRANSIT_FINALISEE = "texte19";
    static final String LIBELLE_ANNULER_PAR_DEFAUT = "texte20";
    private static final String LIBELLE_ANNULER_OPERATION = "texte21";
    static final String ERREUR_DOSSIER_CONTENEUR_RESULTATS_MSG = "texte22";
    private static final String ERREUR_CONTENEUR_RESULTATS_MSG = "Aucun fichier présent au sein du dossier conteneur des résultats dont le chemin a été spécifié\nlors de l'affectation du matériel.";
    static final String ERREUR_CONTENEUR_RESULTATS_LIBELLE = "texte24";
    static final String ERREUR_DOSSIER_FICHE_PASSAGE_MSG = "texte25";
    private static final String ERREUR_FICHE_PASSAGE_MSG = "Aucun fichier n'est présent au sein du dossier spécifié lors de l'affectation du matériel\ncomme dépôt du Fichier Patient.";
    static final String ERREUR_FICHE_PASSAGE_LIBELLE = "texte27";
    static final String ERREUR_LIGNE_COMMANDE_MSG = "Erreur au niveau du chemin de lancement du logiciel indiqué au sein de la ligne de commande\nentrée lors de l'affectation du matériel.";
    private static final String ERREUR_RECUPERATION_ANNEXE_MSG = "texte30";
    static final String ERROR_LIGNE_COMMANDE_LBL = "texte29";
    private static final String ERREUR_ANNEXES_LIBELLE = "texte31 jointes";
    private static final String ERREUR_TENTATIVES_MULTIPLES_LIBELLE = "Interaction - Plusieurs tentatives";
    private static final String ERREUR_TENTATIVES_MULTIPLES_MSG = "L'interaction n'autorise qu'une seule tentative (\"Plusieurs tentatives\" à non)";
    private static final String ERREUR_AUCUN_SIGNAL = "Aucun constat n'a été mis en place au sein de la grille dynamique";
    private static final String ERREUR_AUCUN_SIGNAL_LIBELLE = "INTERACTION - SIGNAL_OBSERVES";
    private static final String ERREUR_SUPPRESSION_OPERATION_LIBELLE = "Suppression examen";
    private static final String ERREUR_SUPPRESSION_OPERATION_MSG = "L'interaction n'autorise pas la suppression d'examens (\"Supprimable\" à non)";
    private static final String ERREUR_SYNTHESE_MSG = "Vous n'avez entré aucune donnée au sein de l'interprétation.";
    private static final String ERREUR_SYNTHESE_LIBELLE = "Interprétation";

    private static final String CAS_SYNTHESE_OPERATION = "Interprété par ";
    private static final String CAS_NON_SYNTHESE_OPERATION = "Non interprété";

    static final String SAISIE_SIGNAL_OBLIGATOIRE = "Saisie constat obligatoire.";
    private static final String RECUPERATION_OPERATION = "Acquisition examen";
    private static final String LANCER_OPERATION = "Lancement examen";
    private static final String FICHIERS_RESULTAT_INCOMPLETS = "Le compte rendu de l'examen est disponible mais il peut manquer des fichiers de résultats.\nVous pouvez le cas échéant renouveler votre examen.";
    private static final String MODULE_CIBLE_NON_INSTALLE_MAL_CONFIGURE = "Cet examen ne peut pas être exécuté car le logiciel cible n'est pas installé ou incorrectement configuré";
    static final String SUPPRIMER_ANNEXE_CSS = "button-supprimer-pj";

    private static final Predicate<AttributionEquipement> PREDICAT_MATERIEL_UTILISABLE = AttributionEquipement::isUtilisable;
    private static final String PREPARATION_FICHIERS = "PREPARATION DES FICHIERS";
    private static final String CLASSE_CSS_INDICATEUR_TEXTE = "spinner-text_visible";
    private static final String SUFFIXE_SIGNAL_EQUIPEMENT = " (Donnée d'appareil)";

    private static final String NOM_FICHIER_IMPRESSION = "impression_%s.pdf";

    static final String DOMAINE_CONSOLE_ANNEXE = "Pièce-jointe";
    static final String MESSAGE_CONSOLE_ANNEXE_TAILL_MAX = "Ajout de fichier impossible. La taille du fichier ne doit pas excéder %s";
    public static final String SIGNAL_OBSERVE = "constat_";
    public static final String AJOUT_ANNEXE = "Ajout d'une PJ";
    public static final String MESSAGE_AJOUT_ANNEXE = "Attention: une transmission en seconde lecture est en cours pour cette examen. Si vous ajoutez une pièce jointe, cette transmission ne pourra plus être réceptionnée et l’examen passera au statut 'à interpréter'";

    static final String LECTURE_OPERATION = "Saisir interprétation";
    private static final String CAS_OPERATION_LUE = "Interprété par ";
    private static final String CAS_OPERATION_NON_LUE = "Non interprété";

    private static final String LIBELLE_COURT_PHASE_SPECIFIQUE = "Urines";
    private static final String CHAMP_DATE_RECUEIL = "Date de recueil des urines";
    private static final String ERREUR_DATE_RECUEIL_APRES_DATE_COURANTE = "la date de recueil est postérieure à la date du jour";
    private static final String LIBELLE_COURT_DATE_RECUEIL = "DateReUrine2";
    private static final String CHAMP_HEURE_RECUEIL = "Heure de recueil des urines";
    private static final String ERREUR_HEURE_RECUEIL_APRES_HEURE_ANALYSE = "L’heure de recueil est postérieure à l’heure d’analyse";
    private static final String LIBELLE_COURT_HEURE_RECUEIL = "HeRecUrine2";
    private static final String LIBELLE_COURT_HEURE_ANALYSE = "HeAnaUrines";

    // FIXME ModeleMateriel -> type pour affectation de matériel
    private static final String EQUIPEMENT_MESURE_FLUX = "Spiromètre";
    private static final String EQUIPEMENT_SIGNAL_CARDIQUE = "Electrocardiogramme";
    private static final String EQUIPEMENT_MESURE_AUDIO = "Audiomètre";

    private static final Integer BOUTON_WIDTH_750 = 750;

    @Autowired
    private OperationTransitFinaliseeService operationTransitFinaliseeService;
    @Autowired
    private PassagerService passagerService;
    @Autowired
    private AttributionEquipementService attributionEquipementService;
    @Autowired
    private LanceurModuleService lanceurModuleService;
    @Autowired
    private ArchivageDocumentService archivageDocumentService;
    @Autowired
    private SignalDiffusionService signalDiffusionService;
    @Autowired
    private AcheminementService acheminementService;
    @Autowired
    ModuleDynamiqueImpl moduleDynamiqueImpl;

    @Autowired
    private SignalObserveValideFactory signalObserveValideFactory;
    @Autowired
    private ElementSignalFactory elementSignalFactory;
    @Autowired
    private SignalEquipementValideFactory signalEquipementValideFactory;
    @Autowired
    private DocumentVersRapportTransitConverter converterDocumentVersRapportTransit;
    @Autowired
    private FichePassageService fichePassageService;
    @Autowired
    private SyntheseTransitService syntheseTransitService;

    private FapDataRdv fichePassageData;

    @Autowired
    private TerminalConfig configurationTerminal;
    @Autowired
    private FileUtils fileUtils;

    @FXML
    OperationsFinaliseesTableView tableauOperationsFinalisees;
    @FXML
    TableColumn<OperationTransitFinalisee, String> colonneOperation;
    @FXML
    Label libelleZoneTransit;

    @FXML
    Button boutonAnnulerOperation;
    @FXML
    Button boutonSupprimerOperation;
    @FXML
    Button boutonAjouterAnnexeManuelle;
    @FXML
    Button boutonModifierOperation;
    @FXML
    Button boutonEnregistrerOperation;
    @FXML
    Button boutonNouvelleOperation;
    @FXML
    Button boutonSynthese;
    @FXML
    Button boutonRecupererResultats;
    @FXML
    Button boutonLancerOperation;
    @FXML
    Button boutonImprimerOperation;

    @FXML
    HBox conteneurAnnexes;
    @FXML
    HBox barreAnnexes;
    @FXML
    OperationsFinaliseesGridPane grilleOperation;
    @FXML
    GridPane grilleSynthese;

    @FXML
    GridPane grillePreRequis;
    @FXML
    Label libellePreRequis;
    @FXML
    CheckBox caseCocherPreRequis;

    @FXML
    VBox conteneurPrincipal;

    private ZoneTransit zoneTransit;
    private Operateur operateurConnecte;
    private OperateurVo operateurConnecteVo;
    private List<OperationTransitFinalisee> operationsTransitExistantes;
    private List<ElementSignal> elementsSignal = new ArrayList<>();
    private final List<LibelleRegleTerminal> libellesRegleAffichage = new ArrayList<>();
    private List<ElementSignal> elementsSignalEquipement = new ArrayList<>();

    private List<ElementSignal> elementsSignalSyntheseEquipement = new ArrayList<>();

    private ControllerMode modeControleurSynthese;
    private List<AttributionEquipement> attributionsEquipementTotales = new ArrayList<>();
    private List<AttributionEquipement> attributionsEquipementUtilisables = new ArrayList<>();
    private String idPassagerStr = null;
    AttributionEquipement attributionEquipementConnecte;
    boolean estModeLecture = false;
    private OperationTransitFinalisee operationEnEdition;

    boolean estLancementModule = false;
    SimpleBooleanProperty estOperationConnectee = new SimpleBooleanProperty(false);
    private boolean sontEgauxGenerique = true;

    private final Map<Long, ElementGrilleTerminalPropriete> reglesAffichageProprietesComposantMap = new HashMap<>();

    private Spinner mainSpinner = null;
    ModeleTerminal configurationModele;
    private RapportTransit documentImpression;
    private boolean estOperationAnnexePostRafraichissement = false;
    SimpleBooleanProperty estExecutionModificationAttribution = new SimpleBooleanProperty(false);
    private List<String> typesEquipementLiesPhase;
    private ChangeListener<Boolean> ecouteurModificationAttributionCourante;

    // Configuration constats d'interpretation ou constats de lecture
    private String synthetiseOuLu = CAS_SYNTHESE_OPERATION;
    private String nonSynthetiseOuNonLu = CAS_NON_SYNTHESE_OPERATION;
    private EnumStatutPhase statutPhaseASynthetiserOuALire = EnumStatutPhase.A_INTERPRETER;

    @Override
    protected void initTerminal(final LinkedList<String> fichiersStyle) {
        super.initTerminal(LIST_CSS_FILES);
        activateCharacterSizeChangeOperations(conteneurPrincipal);
        setMode(ControllerMode.CONSULTATION);
        initOperateurVo();
        initEcouteurs();
        initTableauOperations();
        initBlocPreRequisSynthese();
    }

    private void initOperateurVo() {
        operateurConnecte = contexteSession.getConnectedUser();
        operateurConnecteVo = OperateurVoFactory.buildOperateurVo(operateurConnecte);
    }

    private void initEcouteurs() {
        conteneurAnnexes.managedProperty().bind(conteneurAnnexes.visibleProperty());
        caseCocherPreRequis.selectedProperty().addListener(initLecturePrerequiseChcbxListener());
    }

    private ChangeListener<Boolean> initLecturePrerequiseChcbxListener() {
        return (observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                synthetiseOuLu = CAS_SYNTHESE_OPERATION;
                nonSynthetiseOuNonLu = CAS_NON_SYNTHESE_OPERATION;
                statutPhaseASynthetiserOuALire = EnumStatutPhase.A_INTERPRETER;
            } else {
                synthetiseOuLu = CAS_OPERATION_LUE;
                nonSynthetiseOuNonLu = CAS_OPERATION_NON_LUE;
                statutPhaseASynthetiserOuALire = EnumStatutPhase.LECTURE_A_REALISER;

            }
        };
    }

    private void initTableauOperations() {
        colonneOperation.prefWidthProperty().bind(tableauOperationsFinalisees.widthProperty());
        colonneOperation.setCellValueFactory(cellValue -> new SimpleStringProperty(operationFinaliseeValeurCellule(cellValue.getValue())));
        tableauOperationsFinalisees.getSelectionModel().selectedItemProperty()
                .addListener(initLigneSelectionneeEcouteur());
        operationTransitSelectionnee.bind(tableauOperationsFinalisees.getSelectionModel().selectedItemProperty());
    }

    private String operationFinaliseeValeurCellule(OperationTransitFinalisee examentexte2Realise) {
        StringBuilder operationBuilder = new StringBuilder();
        operationBuilder.append(getDisplayedInteractionRealiseeInteraction().getLibelleLong().toUpperCase());
        examentexte2Service.createRealisateurCellValuePart(operationBuilder, examentexte2Realise);
        examentexte2Service.createLieuDateCellValuePart(operationBuilder, examentexte2Realise);
        examentexte2Service.createMaterielCellValuePart(operationBuilder, examentexte2Realise);
        examentexte2Service.createInterpretationCellValuePart(operationBuilder, examentexte2Realise,
                grilleSynthese.isVisible(), configurationModeleEcran, estModeLecture, synthetiseOuLu, nonSynthetiseOuNonLu);
        return operationBuilder.toString();
    }

    protected ChangeListener<OperationTransitFinalisee> initLigneSelectionneeEcouteur() {
        return (obs, oldSelection, newSelection) -> {
            initContenuAnnexeOperation(operationTransitSelectionnee.get());
            gestionSelectionLigneModeOperation(oldSelection, newSelection);
            if (operationTransitSelectionnee.get() != null) {
                mettreAJourBoutonsDerniereOperation(
                        operationTransitSelectionnee.get().equals(tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(null)));
            } else {
                mettreAJourBoutonsDerniereOperation(false);
            }
        };
    }

    private void initContenuAnnexeOperation(OperationTransitFinalisee examentexte2Realise) {
        barreAnnexes.getChildren().clear();
        if (examentexte2Realise != null && CollectionUtils.isNotEmpty(examentexte2Realise.getListePiecesJointes())) {
            examentexte2Realise.getListePiecesJointes().stream().filter(Doc -> Doc != null && Doc.getNom() != null)
                    .forEach(this::ajouterDocumentAnnexe);
        }
    }

    private void ajouterDocumentAnnexe(RapportTransit Doc) {
        if (Doc.getNom() != null && Doc.getNom().toLowerCase().endsWith(RapportTransitUtils.PDF_EXTENSION)) {
            Hyperlink hl = new Hyperlink(Doc.getNom());
            hl.setOnAction(actionSelectionPdfAnnexe(Doc));
            hl.getStyleClass().add("hyperlink-accueil");

            barreAnnexes.getChildren().add(hl);
        } else {
            Label lab = new Label();
            lab.setText(Doc.getNom());
            barreAnnexes.getChildren().add(lab);
        }

        if (!ClientContextManager._SYSTEM_USER_ID.equals(Doc.getUserCreationId())) {
            barreAnnexes.getChildren().add(initSupprimerPieceJointeManuelle(Doc));
        }

    }

    private void tracerOperationTransit(EnumActionTerminal action, String idRessource) {
        contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                action,
                EnumTypeRessource.FAP,
                idRessource);
    }

    public void supprimerAnnexe(HBox pjData, RapportTransit pieceJointe) {
        tracerOperationTransit(EnumActionTerminal.SUPPRIMER_PJ, idDossierTransit.toString());

        examentexte2Service.supprimerAnnexe(pieceJointe, operationTransitSelectionnee.get(), attributionEquipementConnecte, phaseTransitAffichage,
                configurationModele, statutPhaseASynthetiserOuALire, getInterpretationSaisieOperateurStream().findAny().isPresent());

        barreAnnexes.getChildren().remove(pjData);
        refreshFormData();
    }

    private EventHandler<ActionEvent> actionSelectionPdfAnnexe(RapportTransit pdfDoc) {
        RapportTransit archivedPdf = archivageDocumentService.getRapportTransitById(pdfDoc.getId());
        if (EnumInteraction.EQUIPEMENT_SIGNAL_CARDIQUE.getLibelleCourt().equals(pdfDoc.getSource())) {
            TransmissionSignalSecondaireQuery transEcgSndLectureQuery = new TransmissionSignalSecondaireQuery();
            transEcgSndLectureQuery.setStatutInteraction(phaseTransitAffichage.getStatut());
            transEcgSndLectureQuery.setInteractionRealiseeId(phaseTransitAffichage.getId());
            transEcgSndLectureQuery.setDossierTransitId(dossierTransitAffichagePropriete.getValue().getId());
            return actionEvent -> {
                gestionnairePdf.showRapportTransitEQUIPEMENT_SIGNAL_CARDIQUE(archivedPdf, Habilitation.GERER_EXAMEN_CONSULTER_EXAMEN,
                        transEcgSndLectureQuery,
                        () -> {
                            showModaleTransmissionEcgSecondLecture(transEcgSndLectureQuery, this::rafraichirDepuisTransmissionSignal);
                        });
            };
        } else {
            return actionEvent -> gestionnairePdf.showRapportTransit(archivedPdf, Habilitation.GERER_EXAMEN_CONSULTER_EXAMEN);
        }
    }

    private void rafraichirDepuisTransmissionSignal() {
        phaseTransitAffichage = phaseTransitService.getInteractionRealiseeById(phaseTransitAffichage.getId());
        setMode(ControllerMode.CONSULTATION);
        refreshFormData();
    }

    private void gestionSelectionLigneModeOperation(OperationTransitFinalisee oldSel, OperationTransitFinalisee newSel) {
        if (ControllerMode.CONSULTATION == getMode()) {
            rowSelectionOperationsInConsultationCase(oldSel, newSel);
        } else {
            rowSelectionOperationsInEditionCase(oldSel, newSel);
        }
    }

    @Override
    protected void autreLigneSelectionneeModeEdition(OperationTransitFinalisee oldSelection, OperationTransitFinalisee newSelection) {
        if (modificationDansGrilleDynamique() && !estOperationAnnexePostRafraichissement) {
            serviceDialogue.openDialogConfirmation(getPrimaryStage(), OUTPUT_MESSAGE_TITLE, OUTPUT_MESSAGE,
                    surSelectionElementTableau(newSelection), surAnnulationBoiteConfirmation(oldSelection));
        } else {
            rowSelectionOperations(newSelection);
            setMode(ControllerMode.CONSULTATION);
            estOperationAnnexePostRafraichissement = false;
        }
    }

    @Override
    protected void verificationPostSelectionModeSynthese(OperationTransitFinalisee oldSelection,
            OperationTransitFinalisee newSelection) {
        if (modificationDansSyntheseDynamique()) {
            serviceDialogue.openDialogConfirmation(getPrimaryStage(), OUTPUT_MESSAGE_TITLE, OUTPUT_MESSAGE,
                    selectionElementTableauSynthese(newSelection),
                    surAnnulationBoiteConfirmation(oldSelection));
        } else {
            operationsSelectionElementSynthese(newSelection);
        }
    }

    private Consumer<DialogConfirmationController> selectionElementTableauSynthese(final OperationTransitFinalisee examenRealise) {
        return t -> operationsSelectionElementSynthese(examenRealise);
    }

    private void operationsSelectionElementSynthese(final OperationTransitFinalisee examenRealise) {
        if (composantSyntheseProprietesParIdElement != null && !composantSyntheseProprietesParIdElement.isEmpty()) {
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        }
        estModeOperation = true;
        rowSelectionOperations(examenRealise);
        setMode(ControllerMode.CONSULTATION);
    }

    @Override
    protected void mettreAJourPhasePourNouvelleOperation(OperationTransitFinalisee mainExamenRealise) {
        final EnumStatutPhase oldStatut = phaseTransitAffichage.getStatut();
        EnumStatutPhase newStatut = EnumStatutPhase.AFAIRE;
        if (mainExamenRealise != null) {
            if (signauxObligatoiresRenseignes(mainExamenRealise)) {
                if (examentexte2Service.isInteractionExamenConnecte(attributionEquipementConnecte)) {
                    if (Arrays.asList(EnumStatutPhase.A_INTERPRETER, EnumStatutPhase.LECTURE_A_REALISER, EnumStatutPhase.FAIT)
                            .contains(oldStatut)) {
                        if (!existAnyInterpretationSaisieOperateurRenseigne(mainExamenRealise) && estRecuperationEffectuee()) {
                            newStatut = statutPhaseASynthetiserOuALire;
                        } else {
                            newStatut = EnumStatutPhase.FAIT;
                        }
                    }
                } else if (!existAnyInterpretationSaisieOperateurRenseigne(mainExamenRealise) && (!estModeLecture || caseCocherPreRequis.isSelected())) {
                    newStatut = statutPhaseASynthetiserOuALire;
                } else if (EnumStatutPhase.FAIT != oldStatut && examentexte2Service.isChangementStatutOnlyAfterGeneratePdf(configurationModele)) {
                    newStatut = oldStatut;
                } else {
                    newStatut = EnumStatutPhase.FAIT;
                }
            }
        }
        phaseTransitAffichage.setStatut(
                (mainExamenRealise != null && EnumStatutPhase.AFAIRE == newStatut
                        && estPhaseEnAttenteSyntheseOuTransmission(phaseTransitAffichage))
                                ? oldStatut
                                : newStatut);
        boolean estOperateurPhaseMisAJour = majRealisateurSiStatutAutorise(operateurConnecteVoSupplier(),
                EnumStatutPhase.STATUTS_NECESSITANT_REALISATEUR, phaseTransitAffichage);
        if (!oldStatut.equals(newStatut) || estOperateurPhaseMisAJour) {
            phaseTransitAffichage = updateInteractionRealisee(phaseTransitAffichage);
        }

    }

    private boolean estPhaseEnAttenteSyntheseOuTransmission(InteractionRealisee phaseTransit) {
        return EnumStatutPhase.getInteractionStatutTransmis2ndLectureEnAttenteLectureState().contains(phaseTransit.getStatut());
    }

    private boolean estPhaseTransmiseOuEnAttente(InteractionRealisee phaseTransit) {
        return EnumStatutPhase.getInteractionStatutTransmis2ndLectureEnAttenteLectureState().contains(phaseTransit.getStatut());
    }

    private boolean estRecuperationEffectuee() {
        return operationTransitSelectionnee != null && operationTransitSelectionnee.get() != null
                && CollectionUtils.isNotEmpty(operationTransitSelectionnee.get().getListePiecesJointes());
    }

    private boolean signauxObligatoiresRenseignes(OperationTransitFinalisee examenRealise) {
        List<ElementSignal> elementsSignalDisponibles = getAvailableElementSignalListFromFullList(elementsSignal);
        return elementsSignalDisponibles.stream().filter(ElementSignal::getIsObligatoire)
                .allMatch(elementSignalObserve -> examentexte2Service.existsValueForElementSignalInOperationTransitFinalisee(examenRealise, elementSignalObserve));
    }

    protected boolean elementSignalSyntheseExiste(OperationTransitFinalisee examenRealise,
            ElementSignal elementSignalObserve) {
        return examentexte2Service.elementSignalSyntheseExiste(examenRealise, elementSignalObserve);
    }

    private void initZoneEtAttributionEquipementEcouteur(Long idDossierTransit, long idPassager, InteractionRealisee phaseRealisee) {
        estExecutionModificationAttribution.bind(contexteSession.isRunningListChangeAttributionEquipementProperty());
        ecouteurModificationAttributionCourante = genererEcouteurModificationAttribution();
        estExecutionModificationAttribution.addListener(ecouteurModificationAttributionCourante);
    }

    private ChangeListener<Boolean> genererEcouteurModificationAttribution() {
        return (obs, oldv, newv) -> {
            if (Boolean.FALSE.equals(newv)) {
                Platform.runLater(() -> lancerVerificationsApresModificationAttribution());
            }
        };
    }

    protected void lancerVerificationsApresModificationAttribution() {
        List<AttributionEquipement> equipementsDepuisContexte = contexteSession.getListeAffectationsMateriel().stream()
                .filter(am -> typesEquipementLiesPhase.contains(am.getModele().getType()))
                .filter(PREDICAT_MATERIEL_UTILISABLE)
                .collect(Collectors.toList());
        ZoneTransit zoneDepuisContexte = contexteSession.getZoneTransit();
        if (!zoneDepuisContexte.getNom().equals(zoneTransit.getNom())
                || (equipementsDepuisContexte.size() != attributionsEquipementUtilisables.size()
                        || !equipementsDepuisContexte.containsAll(attributionsEquipementUtilisables))) {
            rechargerDonneesApresModificationZoneOuEquipement();
        }
    }

    private void rechargerDonneesApresModificationZoneOuEquipement() {
        if ((ControllerMode.EDITION == getMode() || ControllerMode.EDITION == modeControleurSynthese) && modificationsExistent()) {
            serviceDialogue.openDialogConfirmationAbandonModification(scenePrincipale,
                    dialog -> accepterAbandonModifications(),
                    dialog -> annulerAbandonModifications(),
                    OUTPUT_MESSAGE);
        } else {
            if (ControllerMode.EDITION == getMode()) {
                setMode(ControllerMode.CONSULTATION);
            } else if (ControllerMode.EDITION == modeControleurSynthese) {
                saisirInterpretationMode(ControllerMode.CONSULTATION);
            }
            rechargerZoneEquipementEtDonnees();
        }
    }

    private void accepterAbandonModifications() {
        if (ControllerMode.EDITION == getMode()) {
            GrilleDynamiqueTerminalUtils.resetAllValues(composantOperationProprietesParIdElement);
            setMode(ControllerMode.CONSULTATION);
        } else if (ControllerMode.EDITION == modeControleurSynthese) {
            GrilleDynamiqueTerminalUtils.resetAllValues(composantSyntheseProprietesParIdElement);
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        }
        rechargerZoneEquipementEtDonnees();
    }

    private void annulerAbandonModifications() {
        estExecutionModificationAttribution.unbind();
        estExecutionModificationAttribution.removeListener(ecouteurModificationAttributionCourante);
        contexteSession.setListeAffectationsMateriel(attributionsEquipementTotales);
        contexteSession.setMaterielVerifie(Boolean.TRUE);
        contexteSession.setZoneTransit(zoneTransit);
        estExecutionModificationAttribution.bind(contexteSession.isRunningListChangeAttributionEquipementProperty());
        estExecutionModificationAttribution.addListener(ecouteurModificationAttributionCourante);
    }

    private void rechargerZoneEquipementEtDonnees() {
        zoneTransit = contexteSession.getZoneTransit();
        libelleZoneTransit.setText(zoneTransit.getNom());
        attributionsEquipementTotales.clear();
        attributionsEquipementTotales.addAll(contexteSession.getListeAffectationsMateriel());
        attributionsEquipementUtilisables = attributionsEquipementTotales.stream()
                .filter(am -> typesEquipementLiesPhase.contains(am.getModele().getType()))
                .filter(PREDICAT_MATERIEL_UTILISABLE)
                .collect(Collectors.toList());
        refreshFormData();
    }

    public void consulterOperationTransit(Long idDossierTransit, Long idPassager,
            InteractionRealisee phaseTransit, List<AttributionEquipement> attributionsEquipement, List<String> typesEquipementAttendus) {
        zoneTransit = contexteSession.getZoneTransit();
        libelleZoneTransit.setText(zoneTransit.getNom());
        typesEquipementLiesPhase = typesEquipementAttendus;
        attributionsEquipementTotales.addAll(contexteSession.getListeAffectationsMateriel());
        attributionsEquipementUtilisables = attributionsEquipement.stream().filter(PREDICAT_MATERIEL_UTILISABLE).collect(Collectors.toList());
        initZoneEtAttributionEquipementEcouteur(idDossierTransit, idPassager, phaseTransit);
        setDatas(idDossierTransit, idPassager, phaseTransit);
        phaseTransitAffichage = phaseTransit;
        estModeOperation = true;
        modeControleurSynthese = ControllerMode.CONSULTATION;
        if (!phaseTransit.getInteraction().getConfigurationEcrans().isEmpty()) {
            recupererConfigurationTerminal();
        }
        configurationModele = getModeleTerminal();
        if (configurationModele != null) {
            boutonImprimerOperation.setVisible(configurationModele.getModeleRapportTransitId() != null);
            if (configurationModele.getLibelleBouton() != null && StringUtils.isNotBlank(configurationModele.getLibelleBouton().getIntitule())) {
                boutonImprimerOperation.setText(configurationModele.getLibelleBouton().getIntitule());
            }
        }

        if (estModeLecture) {
            grillePreRequis.setVisible(true);
            boutonSynthese.setText(LECTURE_OPERATION);
        }

        setMode(ControllerMode.CONSULTATION);
        refreshFormData();

        fichePassageData = fichePassageService.getDataRdvByIdFap(idDossierTransit);

        // Tracabilité
        Interaction phaseTransitAffichageInteraction = getInteractionFromPrestationConfigurationById(
                phaseTransitAffichage.getInteraction().getId());
        EnumTerminal enumEcran = EnumTerminal.UNKNOW;
        String complement = StringUtils.EMPTY;
        if (phaseTransitAffichageInteraction != null && EnumTypePhase.EXAMEN == phaseTransitAffichageInteraction.getTypeInteraction()) {
            enumEcran = EnumTerminal.EXAMEN_PARACLINIQUE;
            if (phaseTransitAffichageInteraction.getLibelleCourt() != null) {
                complement = fr.cnamts.cpam.bordeaux.utils.StringUtils.stripAccents(phaseTransitAffichageInteraction.getLibelleCourt().toUpperCase());
            }
        }
        TraceVo traceVo = new TraceVo();
        traceVo.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                enumEcran, complement,
                EnumActionTerminal.CONSULTER,
                EnumTypeRessource.FAP,
                idDossierTransit.toString());
        serviceTrace.create(traceVo);
        // *********

    }

    private void initBlocPreRequisSynthese() {
        grillePreRequis.managedProperty().bind(grillePreRequis.visibleProperty());
        grilleSynthese.managedProperty().bind(grilleSynthese.visibleProperty());
    }

    private void definirEtatEnfantsPreRequis() {
        if (estModeOperation) {
            grillePreRequis.definirDesactive(getMode().equals(ControllerMode.CONSULTATION));
        } else {
            grillePreRequis.definirDesactive(true);
        }
    }

    @Override
    protected Map<String, ResourceRequest> obtenirRequetesRessourcesTerminal() {
        Map<String, ResourceRequest> resources = super.obtenirRequetesRessourcesTerminal();
        if (phaseTransitAffichage != null) {
            OperationTransitFinaliseeCriteres examentexte2RealiseCriterias = new OperationTransitFinaliseeCriteres();
            examentexte2RealiseCriterias.setIdInteractionRealise(phaseTransitAffichage.getId());
            examentexte2RealiseCriterias.setLastVersionExamen(false);
            resources.put(CLE_OPERATION_TRANSIT_FINALISEE, new ResourceRequest(OperationTransitFinalisee[].class, examentexte2RealiseCriterias));
        }
        return resources;
    }

    @Override
    protected void definirDonneesTerminal(Map<String, Object> resources) {
        super.definirDonneesTerminal(resources);
        conteneurAnnexes.setVisible(true);
        if (resources.containsKey(CLE_OPERATION_TRANSIT_FINALISEE)) {
            operationsTransitExistantes = Arrays.asList((OperationTransitFinalisee[]) resources.get(CLE_OPERATION_TRANSIT_FINALISEE));
        }
        if (!tableauOperationsFinalisees.getItems().isEmpty()) {
            tableauOperationsFinalisees.getItems().clear();
        }
        if (phaseTransitAffichage != null) {
            verifierAttributionEquipementConnecte();
        }
        if (!operationsTransitExistantes.isEmpty()) {
            tableauOperationsFinalisees.getItems().addAll(FXCollections.observableArrayList(operationsTransitExistantes));
            selectionnerLigneApresRafraichissement();
        } else {
            Stream.of(composantOperationProprietesParIdElement, composantSyntheseProprietesParIdElement)
                    .forEach(GrilleDynamiqueTerminalUtils::resetAllValues);
        }

        if (estModeLecture) {
            grilleSynthese.setVisible(caseCocherPreRequis.isSelected());
        } else {
            grilleSynthese.setVisible(true);
        }
    }

    private void selectionnerLigneApresRafraichissement() {
        if (idOperationASelectionner == null) {
            selectionnerLigneDerniereOperationFinalisee();
        } else {
            tableauOperationsFinalisees.selectExamenByIdInTableView(idOperationASelectionner);
        }
    }

    private void selectionnerLigneDerniereOperationFinalisee() {
        OperationTransitFinalisee examenToSelect = operationsTransitExistantes.stream()
                .max(Comparator.comparing(OperationTransitFinalisee::getDateCreation)).orElse(null);
        Objects.requireNonNull(examenToSelect);
        tableauOperationsFinalisees.getSelectionModel().clearSelection();
        tableauOperationsFinalisees.getSelectionModel().select(examenToSelect);
        tableauOperationsFinalisees.scrollTo(examenToSelect);
    }

    private void verifierAttributionEquipementConnecte() {
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        if (interaction.getReferenceTransitsTypeMateriel() != null && !interaction.getReferenceTransitsTypeMateriel().isEmpty()) {
            RechercheAttributionEquipementCriteres criteria = new RechercheAttributionEquipementCriteres();
            criteria.setZoneTransit(zoneTransit.getNom());
            List<ReferenceTransit> lsTypeMateriel = interaction.getReferenceTransitsTypeMateriel();
            List<String> libellesTypeEquipement = lsTypeMateriel.stream().map(ReferenceTransit::getIntitule)
                    .collect(Collectors.toList());
            List<AttributionEquipement> affectationMaterielFromZoneTransit = attributionEquipementService.getAffectationsMaterielByCriterias(criteria);
            attributionEquipementConnecte = affectationMaterielFromZoneTransit.stream()
                    .filter(AttributionEquipement::isConnected)
                    .filter(afm -> libellesTypeEquipement.contains(afm.getModele().getType())).findFirst().orElse(null);
            estOperationConnectee.set(attributionEquipementConnecte != null);
        }
    }

    @Override
    protected Map<Region, FormField> obtenirChampsTerminal() {
        final Map<Region, FormField> fields = new HashMap<>();
        fields.put(tableauOperationsFinalisees, new FormField(new ArrayList<>(), tableauOperationsFinalisees));
        return fields;
    }

    @Override
    protected void definirMode(ControllerMode mode) {
        if (ControllerMode.CONSULTATION == mode) {
            definirModeConsultationOperation();
        } else {
            definirModeEditionOperation();
        }
        definirEtatEnfantsPreRequis();
    }

    private void mettreAJourBoutonsDerniereOperation(Boolean isLastExamen) {
        if (Boolean.TRUE.equals(isLastExamen)) {
            boutonSynthese.disableProperty().bind(interpreterBtnBinding().not());
            boolean estModificationAutorisee = gestionnaireAutorisationPhase
                    .checkHabilitationModifierExamen(getDisplayedInteractionRealiseeInteraction());

            if (dossierTransitAffichagePropriete.getValue() != null && dossierTransitAffichagePropriete.getValue().isUnmodifiable()) {
                estModificationAutorisee = estModificationAutorisee
                        && gestionnaireAction.hasHabilitation(Habilitation.GERER_PRESTATION_REALISEE_MODIFIER_DOSSIER_CLOTURE);
            }
            definirDesactive(boutonModifierOperation, !estModificationAutorisee);
        } else {
            Stream.of(boutonSynthese, boutonModifierOperation).forEach(btn -> definirDesactive(btn, true));
        }
    }

    private void definirModeConsultationOperation() {
        BooleanBinding isModifyingAllowedBinding = Bindings.createBooleanBinding(estModificationAutoriseeBinding(), dossierTransitAffichagePropriete);
        Stream.of(boutonAnnulerOperation, boutonEnregistrerOperation, boutonRecupererResultats, boutonAjouterAnnexeManuelle).forEach(btn -> definirDesactive(btn, true));
        Stream.of(boutonModifierOperation, boutonSupprimerOperation)
                .forEach(btn -> btn.disableProperty().bind(tableauOperationsFinalisees.getSelectionModel()
                        .selectedItemProperty().isNull().or(isModifyingAllowedBinding.not())));
        boutonSynthese.disableProperty().bind(interpreterBtnBinding().not().or(isModifyingAllowedBinding.not()));

        habilitationEpcSupprimerExamenBtn();
        habilitationEpcNouvelExamenBtn();

        BooleanBinding canLancerExamenBeLaunched = Bindings.createBooleanBinding(peutLancerOperation(),
                tableauOperationsFinalisees.getSelectionModel().selectedItemProperty());
        boutonLancerOperation.disableProperty().bind(canLancerExamenBeLaunched.not().or(isModifyingAllowedBinding.not()));

        moduleDynamiqueImpl.switchModeTo(ControllerMode.CONSULTATION, composantOperationProprietesParIdElement);

        BooleanBinding isImprimerAllowedBinding = Bindings.createBooleanBinding(isImprimerAllowed(), dossierTransitAffichagePropriete);
        boutonImprimerOperation.disableProperty().bind(tableauOperationsFinalisees.getSelectionModel()
                .selectedItemProperty().isNull().or(isImprimerAllowedBinding.not()));
    }

    private void habilitationEpcSupprimerExamenBtn() {
        if (phaseTransitAffichage != null && isSupprimerExamenNotAllowed(getInteractionFromPrestationConfigurationById(
                phaseTransitAffichage.getInteraction().getId()))) {
            definirDesactive(boutonSupprimerOperation, true);
        }
    }

    private void habilitationEpcNouvelExamenBtn() {
        definirDesactive(boutonNouvelleOperation, !isNouvelExamenBtnAllowed());
    }

    private void definirDesactive(Button button, boolean disable) {
        button.disableProperty().unbind();
        button.definirDesactive(disable);
    }

    private Callable<Boolean> isImprimerAllowed() {
        return () -> (dossierTransitAffichagePropriete.getValue() != null
                && gestionnaireAction.hasHabilitation(Habilitation.GERER_EXAMEN_REALISER_EXAMEN_IMPRIMER));
    }

    private Callable<Boolean> estModificationAutoriseeBinding() {
        return this::estModificationAutorisee;
    }

    private boolean estModificationAutorisee() {
        return dossierTransitAffichagePropriete.getValue() == null
                || !dossierTransitAffichagePropriete.getValue().isUnmodifiable()
                || gestionnaireAction.hasHabilitation(Habilitation.GERER_PRESTATION_REALISEE_MODIFIER_DOSSIER_CLOTURE);
    }

    private Callable<Boolean> peutLancerOperation() {
        return () -> {
            boolean isConnected = estOperationConnectee.get();
            ReadOnlyObjectProperty<OperationTransitFinalisee> examen = tableauOperationsFinalisees
                    .getSelectionModel().selectedItemProperty();
            boolean isValueSelected = examen.isNotNull().get();
            boolean hasNotAlreadyBeenLaunched = true;
            boolean isLastOneCreated = true;
            if (isValueSelected && isConnected) {
                List<SignalEquipementValide> lsCMR = tableauOperationsFinalisees.getSelectionModel().getSelectedItem().getListeSignalEquipementValide();
                hasNotAlreadyBeenLaunched = !(lsCMR != null && !lsCMR.isEmpty());
                OperationTransitFinalisee lastCreatedExamen = tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(null);
                isLastOneCreated = lastCreatedExamen.equals(examen.getValue());
            }
            return isConnected && isValueSelected && hasNotAlreadyBeenLaunched && isLastOneCreated && gestionnaireAutorisationPhase
                    .checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction());
        };
    }

    private void definirModeEditionOperation() {
        boutonEnregistrerOperation.definirDesactive(!gestionnaireAutorisationPhase.checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction()));

        Stream.of(boutonAnnulerOperation, boutonAjouterAnnexeManuelle).forEach(btn -> definirDesactive(btn, false));
        Stream.of(boutonModifierOperation, boutonSupprimerOperation, boutonImprimerOperation, boutonSynthese, boutonRecupererResultats,
                boutonLancerOperation).forEach(this::operationsBoutonEdition);
        definirDesactive(boutonNouvelleOperation, true);
        if (operationTransitSelectionnee.get() == null || !gestionnaireAction.hasHabilitation(Habilitation.GERER_PJ_EXAMEN_PARACLINIQUE_AJOUTER)) {
            definirDesactive(boutonAjouterAnnexeManuelle, true);
        }
        moduleDynamiqueImpl.switchModeTo(ControllerMode.EDITION, composantOperationProprietesParIdElement);

        caseCocherPreRequis.definirDesactive(phaseTransitAffichage.getStatut() == EnumStatutPhase.FAIT && caseCocherPreRequis.isSelected());

    }

    private void operationsBoutonEdition(Button btn) {
        btn.disableProperty().unbind();
        btn.definirDesactive(true);
    }

    @Override
    public boolean modificationsExistent() {
        if ((estModeOperation && getMode().equals(ControllerMode.EDITION)) || estLancementModule) {
            return (modificationDansGrilleDynamique() || boutonRecuperationActive());
        } else if (modeControleurSynthese.equals(ControllerMode.EDITION)) {
            return modificationDansSyntheseDynamique();
        } else {
            return false;
        }
    }

    public ValidationRule creerRegleValidationDateRecueil(List<ElementSignal> elementsSignal) {
        return new AbstractValidationRule(CHAMP_DATE_RECUEIL) {
            @Override
            public ValidationRuleResult isValid() {
                final ValidationRuleResult vrr = new ValidationRuleResult();
                boolean hasSucceed = true;

                ElementSignal elemCstDateRecueilUrine = elementsSignal.stream()
                        .filter(elem -> null != elem.getSignalObserve()
                                && LIBELLE_COURT_DATE_RECUEIL.equalsIgnoreCase(elem.getSignalObserve().getLibelleCourt()))
                        .findFirst().orElse(null);
                if (elemCstDateRecueilUrine != null) {
                    SignalObserveValide cstReaDateRecueilUrine = retrieveSignalObserveValideFromElementSignal(elemCstDateRecueilUrine, null,
                            composantOperationProprietesParIdElement);
                    if (cstReaDateRecueilUrine.getLocalDateValue() != null) {
                        hasSucceed = !cstReaDateRecueilUrine.getLocalDateValue().isAfter(LocalDate.now());
                    }
                }
                vrr.setHasSucceed(hasSucceed);

                final ValidationError ve = new ValidationError(ErrorLevel.ERROR, CHAMP_DATE_RECUEIL,
                        ERREUR_DATE_RECUEIL_APRES_DATE_COURANTE);
                vrr.setValidationError(ve);
                return vrr;
            }
        };
    }

    public ValidationRule creerRegleValidationHeureRecueil(List<ElementSignal> elementsSignal) {
        return new AbstractValidationRule(CHAMP_HEURE_RECUEIL) {
            @Override
            public ValidationRuleResult isValid() {
                final ValidationRuleResult vrr = new ValidationRuleResult();
                boolean hasSucceed = true;

                ElementSignal elemCstHeureRecueilUrine = elementsSignal.stream()
                        .filter(elem -> null != elem.getSignalObserve()
                                && LIBELLE_COURT_HEURE_RECUEIL.equalsIgnoreCase(elem.getSignalObserve().getLibelleCourt()))
                        .findFirst().orElse(null);
                ElementSignal elemCstHeureAnalyseUrine = elementsSignal.stream()
                        .filter(elem -> null != elem.getSignalObserve()
                                && LIBELLE_COURT_HEURE_ANALYSE.equalsIgnoreCase(elem.getSignalObserve().getLibelleCourt()))
                        .findFirst().orElse(null);
                if (elemCstHeureRecueilUrine != null && elemCstHeureAnalyseUrine != null) {
                    SignalObserveValide cstReaHeureRecueilUrine = retrieveSignalObserveValideFromElementSignal(elemCstHeureRecueilUrine, null,
                            composantOperationProprietesParIdElement);
                    SignalObserveValide cstReaHeureAnalyseUrine = retrieveSignalObserveValideFromElementSignal(elemCstHeureAnalyseUrine, null,
                            composantOperationProprietesParIdElement);
                    if (cstReaHeureRecueilUrine != null && cstReaHeureRecueilUrine.getLocalTimeValue() != null
                            && cstReaHeureAnalyseUrine != null && cstReaHeureAnalyseUrine.getLocalTimeValue() != null) {
                        hasSucceed = !cstReaHeureRecueilUrine.getLocalTimeValue().isAfter(cstReaHeureAnalyseUrine.getLocalTimeValue());
                    }
                }
                vrr.setHasSucceed(hasSucceed);

                final ValidationError ve = new ValidationError(ErrorLevel.ERROR, CHAMP_HEURE_RECUEIL,
                        ERREUR_HEURE_RECUEIL_APRES_HEURE_ANALYSE);
                vrr.setValidationError(ve);
                return vrr;
            }
        };
    }

    @Override
    protected ValidationRule construireRegleObligatoire(ElementSignal elemSignalObserve) {
        return creerRegleValidationSignalObserve(elemSignalObserve, false);
    }

    @Override
    protected boolean estElementSignalQuantitatif(ElementSignal elemSignalObserve) {
        return elemSignalObserve.estElementSignalQuantitatif();
    }

    @Override
    protected boolean estElementSignalPlausible(ElementSignal elemSignalObserve) {
        return elemSignalObserve.estElementSignalPlausible();
    }

    @Override
    protected Object obtenirCleChampOperationsFinalisees() {
        return tableauOperationsFinalisees;
    }

    @Override
    protected void mettreAJourReglesValidationSiPhaseSpecifique(List<ElementSignal> lsElemSignalObserve, List<ValidationRule> validationRules) {
        Interaction interaction = getInteraction();
        if (interaction != null && interaction.getLibelleCourt() != null && LIBELLE_COURT_PHASE_SPECIFIQUE.equals(interaction.getLibelleCourt())) {
            validationRules.add(creerRegleValidationDateRecueil(lsElemSignalObserve));
            validationRules.add(creerRegleValidationHeureRecueil(lsElemSignalObserve));
        }
    }

    public List<ValidationRule> construireReglesValidation() {
        final List<ElementSignal> lsElemSignalObserve = estModeOperation ? elementsSignalExamen : elementsSignalInterpretation;
        return construireReglesValidationFromElementSignalList(lsElemSignalObserve);
    }

    private boolean estValidationOkElementObligatoireNonRenseigne(ElementSignal elemSignalObserve, boolean isAvantAcquisition) {
        SignalObserveValide constatRealise = retrieveSignalObserveValideFromElementSignal(elemSignalObserve, null,
                estModeOperation ? composantOperationProprietesParIdElement : composantSyntheseProprietesParIdElement);
        return !elemSignalObserve.getIsObligatoire() || (Boolean.TRUE.equals(elemSignalObserve.getIsObligatoireAvantAcquisition()) && !isAvantAcquisition)
                || !constatRealise.isEmptySignalObserveValideValue();
    }

    private AbstractValidationRule creerRegleValidationSignalObserve(final ElementSignal elemSignalObserve, final boolean isAvantAcquisition) {
        return new AbstractValidationRule(elemSignalObserve.getSignalObserve().getLibelle(),
                vr -> obtenirChampsTerminal().get(tableauOperationsFinalisees).setInError(true)) {
            @Override
            public ValidationRuleResult isValid() {
                final ValidationRuleResult vrr = new ValidationRuleResult();
                vrr.setHasSucceed(estValidationOkElementObligatoireNonRenseigne(elemSignalObserve, isAvantAcquisition));
                final ValidationError ve = new ValidationError(ErrorLevel.ERROR, widgetLabel, SAISIE_SIGNAL_OBLIGATOIRE);
                vrr.setValidationError(ve);
                return vrr;
            }
        };
    }

    @FXML
    public void ajouterAnnexeManuelle() {
        if (phaseTransitAffichage.getStatut() == EnumStatutPhase.EN_ATTENTE_LECTURE
                || phaseTransitAffichage.getStatut() == EnumStatutPhase.TRANSMIS_2ND_LECTURE) {
            serviceDialogue.openDialogConfirmation(getPrimaryStage(), AJOUT_ANNEXE, MESSAGE_AJOUT_ANNEXE,
                    dialog -> appelerAjoutAnnexeManuelle(), null);
        } else {
            appelerAjoutAnnexeManuelle();
        }
    }

    private void appelerAjoutAnnexeManuelle() {
        File file = launchFileChooserOperations();
        if (file != null && operationTransitSelectionnee.get() != null) {
            if (file.length() > configurationTerminal.getHttpPayloadMax()) {
                controleurConsole.clearConsoleAndAddMessage(
                        DOMAINE_CONSOLE_ANNEXE,
                        String.format(MESSAGE_CONSOLE_ANNEXE_TAILL_MAX, fileUtils.byteCountToDisplaySize(configurationTerminal.getHttpPayloadMax())));
            } else {
                contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                        getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                        EnumActionTerminal.AJOUTER_PJ,
                        EnumTypeRessource.FAP,
                        idDossierTransit.toString());

                Long idExamentexte2 = operationTransitSelectionnee.get().getId();
                RapportTransit RapportTransit = RapportTransitFactory.buildRapportTransit(file, EnumTypeRapport.PARACLINIQUE, "_" + idDossierTransit);
                ajouterAttributsRapportTransitForDP(RapportTransit, getDisplayedDossierTransitPropertyValue());
                AnnexeOperationTransit pieceJointeExamentexte2 = new AnnexeOperationTransit(RapportTransit, idExamentexte2);
                operationTransitFinaliseeService.ajouterAnnexeOperationManuellement(pieceJointeExamentexte2);
                if (examentexte2Service.isInteractionExamenConnecte(attributionEquipementConnecte)) {
                    phaseTransitAffichage.setStatut(statutPhaseASynthetiserOuALire);
                    if (phaseTransitAffichage.getRealisateur() == null && operationTransitSelectionnee.get().getRealisateur() != null) {
                        phaseTransitAffichage.setRealisateur(operationTransitSelectionnee.get().getRealisateur());
                    }
                    phaseTransitService.updateInteractionRealisee(phaseTransitAffichage);
                }
                operateRefreshOnAllDataFromSelectedExamOrOnPJItemsAccordingToExistModifInForm();
            }
        }
    }

    private void operateRefreshOnAllDataFromSelectedExamOrOnPJItemsAccordingToExistModifInForm() {
        if (ControllerMode.EDITION == getMode() && modificationsExistent()) {
            serviceDialogue.openDialogConfirmation(getPrimaryStage(), OUTPUT_MESSAGE_TITLE, OUTPUT_MESSAGE, onConfirmAfterPJActionWithExistingModifsInForm(),
                    onCancelActionWithExistingModifsInForm());
        } else {
            refreshFormData();
        }
    }

    private Consumer<DialogConfirmationController> onConfirmAfterPJActionWithExistingModifsInForm() {
        return t -> {
            estOperationAnnexePostRafraichissement = true;
            refreshFormData();
        };
    }

    private Consumer<DialogConfirmationController> onCancelActionWithExistingModifsInForm() {
        return t -> {
            final Long idSelectedExamen = operationTransitSelectionnee.get().getId();
            OperationTransitFinalisee examenPara = operationTransitFinaliseeService.getExamentexte2texte2RealiseById(idSelectedExamen);
            initContenuAnnexeOperation(examenPara);
        };
    }

    @FXML
    public void annulerOperationTransit() {
        if (estModeOperation) {
            annulerOperationMode();
        } else {
            annulerModeSynthese();
        }
    }

    private void annulerOperationMode() {
        if (estLancementModule) {
            annulerLancementOperation();
        } else {
            annulerOperationBase();
        }
    }

    private void annulerOperationPersistee() {
        GrilleTerminalUi composantsNonLiesAuLogiciel = retrieveNotBindedToSoftwareComponents();
        operationTransitSelectionnee.getValue().getListeSignalObserveValideExamen().stream()
                .filter(cstRea -> composantsNonLiesAuLogiciel.containsKey(cstRea.getElementSignal().getId()))
                .forEach(cstRea -> composantsNonLiesAuLogiciel.putSignalObserveValideValueInGrilleDynamiqueTerminal(cstRea));
        caseCocherPreRequis.setSelected(Boolean.TRUE.equals(operationTransitSelectionnee.getValue().getIsLecturePrerequise()));
    }

    private void annulerOperationBase() {
        if (operationTransitSelectionnee.isNotNull().get()) {
            annulerOperationPersistee();
        } else {
            GrilleDynamiqueTerminalUtils.resetAllValues(composantOperationProprietesParIdElement);
            caseCocherPreRequis.setSelected(false);
        }
        setMode(ControllerMode.CONSULTATION);
    }

    private GrilleTerminalUi retrieveNotBindedToSoftwareComponents() {
        if (elementsSignalEquipement != null && !elementsSignalEquipement.isEmpty()) {
            GrilleTerminalUi composantsNonLiesAuLogiciel = new GrilleTerminalUi();
            List<Long> idsElementSignalEquipement = elementsSignalEquipement.stream().map(ElementGrilleTerminal::getId)
                    .collect(Collectors.toList());
            List<Long> elementsNonSignalEquipement = composantOperationProprietesParIdElement.keySet().stream()
                    .filter(key -> !idsElementSignalEquipement.contains(key)).collect(Collectors.toList());
            elementsNonSignalEquipement
                    .forEach(id -> composantsNonLiesAuLogiciel.put(id, composantOperationProprietesParIdElement.get(id)));
            return composantsNonLiesAuLogiciel;
        } else {
            return composantOperationProprietesParIdElement;
        }
    }

    private void annulerModeSynthese() {
        List<SignalObserveValide> signauxObservesSynthese = operationTransitSelectionnee.getValue().getListeSignalObserveValideInterpretation();
        if (CollectionUtils.isNotEmpty(signauxObservesSynthese)) {
            signauxObservesSynthese
                    .forEach(cstRea -> composantSyntheseProprietesParIdElement.putSignalObserveValideValueInGrilleDynamiqueTerminal(cstRea));
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        } else {
            GrilleDynamiqueTerminalUtils.resetAllValues(composantSyntheseProprietesParIdElement);
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        }
        afficherMasquerValeurSignalEquipement();
    }

    private void afficherMasquerValeurSignalEquipement() {
        if (operationTransitSelectionnee.getValue().getAnalyste() == null) {
            List<SignalEquipementValide> lsSignalEquipementValideExamen = operationTransitSelectionnee.getValue()
                    .getListeSignalEquipementValide();
            if (lsSignalEquipementValideExamen != null && !lsSignalEquipementValideExamen.isEmpty()
                    && !elementsSignalSyntheseEquipement.isEmpty()) {
                placerValeursSignalEquipementDansGrilleSynthese(operationTransitSelectionnee.getValue());
            }
        }
    }

    void annulerLancementOperation() {
        annulerOperationPersistee();
        boutonAnnulerOperation.setText(LIBELLE_ANNULER_PAR_DEFAUT);
        estLancementModule = false;
        moduleDynamiqueImpl.changeAsterixToShowValue(false);

        setMode(ControllerMode.CONSULTATION);
    }

    private void reinitialiserLancementOperation() throws MaterielConnectedAquisitionFileNotFoundException {
        String resultFilesFolderPath = attributionEquipementConnecte.getResultFilesFolderPath();
        String typeMateriel = attributionEquipementConnecte.getModele().getType();
        examentexte2Service.removeFilesFromPath(resultFilesFolderPath, typeMateriel);

    }

    @FXML
    public void imprimerOperationTransit() {
        boolean isChangementStatutOnlyAfterGeneratePdf = !examentexte2Service.isInteractionExamenConnecte(attributionEquipementConnecte)
                && examentexte2Service.isChangementStatutOnlyAfterGeneratePdf(configurationModele);

        if (isChangementStatutOnlyAfterGeneratePdf) {
            Dialog3BoutonsParam dialog3BoutonsParam = new Dialog3BoutonsParam();
            dialog3BoutonsParam.setTitre(TITRE_MODALE_IMPRESSION_AVEC_MAJ_STATUT_PHASE);
            dialog3BoutonsParam.setMessage(MESSAGE_GENERATION_AJOUT_ANNEXE_DOSSIER);
            dialog3BoutonsParam.setLibelleBoutons1(DIALOGBOX_TEXTE_BOUTON1_OUVRIR_SEULEMENT);
            dialog3BoutonsParam.setLibelleBoutons2(DIALOGBOX_TEXTE_BOUTON2_GENERER_ET_CLOTURER);
            dialog3BoutonsParam.setLibelleBoutons3(DIALOGBOX_TEXTE_BOUTON3_AJOUTER_PJ_ET_CLOTURER);
            dialog3BoutonsParam.setEnumStyleClassBouton1(EnumStyleClassBouton.LONG);
            dialog3BoutonsParam.setEnumStyleClassBouton2(EnumStyleClassBouton.LONG);
            dialog3BoutonsParam.setEnumStyleClassBouton3(EnumStyleClassBouton.LONGER);
            dialog3BoutonsParam.setMinWidthDialog(BOUTON_WIDTH_750);
            dialog3BoutonsParam.setPrefWidthDialog(BOUTON_WIDTH_750);

            serviceDialogue.openDialog3BoutonsController(scenePrincipale, dialog3BoutonsParam,
                    ouvrir -> ouvrirImpression(), generer -> ouvrirEtAjouterImpressionAnnexe(), ajouterPJ -> ajouterAnnexeEtCloturer());
        } else {
            serviceDialogue.openDialog2BoutonsController(scenePrincipale, TITRE_MODALE_IMPRESSION_SIMPLE, MESSAGE_ENREGISTREMENT_DOCUMENT,
                    DIALOGBOX_TEXTE_BOUTON1_OUVRIR_SEULEMENT, DIALOGBOX_TEXTE_BOUTON2_ENREGISTRER_DANS_DOSSIER,
                    ouvrir -> ouvrirImpression(), ajouter -> ouvrirEtAjouterImpressionAnnexe());
        }
    }

    private void ouvrirImpression() {
        PrintableObject<OperationTransitVo> examentexte2Vo = obtenirOperationTransitVoImprimable(operationTransitSelectionnee.get());
        documentImpression = serviceModeleDocument.generateRapportTransitFromTemplateHtml(examentexte2Vo);

        if (documentImpression != null) {
            TraceVo traceVo = new TraceVo();
            final EnumTerminal ecran = getEnumTerminalOfExamentexte2();
            traceVo.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    ecran,
                    getComplementEnumTerminalExamentexte2(ecran),
                    EnumActionTerminal.IMPRIMER,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());
            serviceTrace.create(traceVo);

            gestionnairePdf.showRapportTransit(documentImpression, Habilitation.GERER_EXAMEN_CONSULTER_EXAMEN);
        }
    }

    private void ouvrirEtAjouterImpressionAnnexe() {
        ouvrirImpression();
        if (operationTransitSelectionnee.get() != null && documentImpression != null) {

            contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                    EnumActionTerminal.AJOUTER_PJ,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());

            documentImpression
                    .setNom(FileUtils.getFilenameWithSuffix(
                            String.format(NOM_FICHIER_IMPRESSION, getInteraction() == null ? StringUtils.EMPTY : getInteraction().getLibelleCourt()),
                            "_" + idDossierTransit));
            ajouterAttributsRapportTransitForDP(documentImpression, getDisplayedDossierTransitPropertyValue());

            AnnexeOperationTransit pieceJointeExamentexte2 = new AnnexeOperationTransit(documentImpression,
                    operationTransitSelectionnee.get().getId());

            operationTransitFinaliseeService.ajouterAnnexeOperationManuellement(pieceJointeExamentexte2);

            if (!examentexte2Service.isInteractionExamenConnecte(attributionEquipementConnecte)
                    && examentexte2Service.isChangementStatutOnlyAfterGeneratePdf(configurationModele)) {
                if (null != phaseTransitAffichage.getStatut() && phaseTransitAffichage.getStatut().isAInterpreterOrLectureARealiser()) {
                    if (existAnyInterpretationSaisieOperateurRenseigne(tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(null))) {
                        phaseTransitAffichage.setStatut(EnumStatutPhase.FAIT);
                        phaseTransitService.updateInteractionRealisee(phaseTransitAffichage);
                    }
                } else if (EnumStatutPhase.AFAIRE == phaseTransitAffichage.getStatut()) {
                    phaseTransitAffichage.setStatut(EnumStatutPhase.FAIT);
                    phaseTransitAffichage.setRealisateur(operateurConnecteVo);
                    acheminementService.updateStatutSuivisOnSynthValidation(dossierTransitAffichagePropriete.getValue().getFap().getId());
                    phaseTransitService.updateInteractionRealisee(phaseTransitAffichage);
                }
            }

            refreshFormData();
        }
    }

    private void ajouterAnnexeEtCloturer() {
        File file = launchFileChooserOperations();
        if (file != null) {
            if (file.length() > configurationTerminal.getHttpPayloadMax()) {
                controleurConsole.clearConsoleAndAddMessage(
                        DOMAINE_CONSOLE_ANNEXE,
                        String.format(MESSAGE_CONSOLE_ANNEXE_TAILL_MAX, fileUtils.byteCountToDisplaySize(configurationTerminal.getHttpPayloadMax())));
            } else {
                Long idExamentexte2 = operationTransitSelectionnee.get().getId();
                RapportTransit document = RapportTransitFactory.buildRapportTransit(file, EnumTypeRapport.PARACLINIQUE, "_" + idDossierTransit);

                ajouterAttributsRapportTransitForDP(document, getDisplayedDossierTransitPropertyValue());

                operationTransitFinaliseeService.ajouterAnnexeOperationManuellement(new AnnexeOperationTransit(document, idExamentexte2));

                if (doitMettreAJourStatutPhase()) {
                    EnumStatutPhase statut = phaseTransitAffichage.getStatut();

                    if (statut != null && statut.isAInterpreterOrLectureARealiser()
                            && existAnyInterpretationSaisieOperateurRenseigne(
                                    tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(null))) {

                        phaseTransitAffichage.setStatut(EnumStatutPhase.FAIT);
                        phaseTransitService.updateInteractionRealisee(phaseTransitAffichage);

                    } else if (EnumStatutPhase.AFAIRE == statut) {

                        phaseTransitAffichage.setStatut(EnumStatutPhase.FAIT);
                        phaseTransitAffichage.setRealisateur(operateurConnecteVo);

                        acheminementService.updateStatutSuivisOnSynthValidation(dossierTransitAffichagePropriete.getValue().getFap().getId());

                        phaseTransitService.updateInteractionRealisee(phaseTransitAffichage);
                    }
                }
                refreshFormData();
            }
        }
    }

    private boolean doitMettreAJourStatutPhase() {
        return !examentexte2Service.isInteractionExamenConnecte(attributionEquipementConnecte)
                && examentexte2Service
                        .isChangementStatutOnlyAfterGeneratePdf(configurationModele);
    }

    @Override
    protected Long obtenirIdPhaseAffichage() {
        return phaseTransitAffichage != null
                && phaseTransitAffichage.getInteraction() != null
                        ? phaseTransitAffichage.getInteraction().getId()
                        : null;
    }

    private PrintableObject<OperationTransitVo> obtenirOperationTransitVoImprimable(final OperationTransitFinalisee examentexte2Realise) {
        final OperationTransitVo examentexte2Vo = construireOperationTransitVo(examentexte2Realise, passagerAffichage,
                dossierTransitAffichagePropriete.getValue());
        final PrintableObject<OperationTransitVo> examenVoPrintableObject = new PrintableObject<>(examentexte2Vo,
                EnumPrintObject.EXAMEN_IMPRESSION,
                EnumTypeRapport.PARACLINIQUE);
        examenVoPrintableObject.setModeleRapportTransitId(configurationModele.getModeleRapportTransitId());
        return examenVoPrintableObject;
    }

    private OperationTransitVo construireOperationTransitVo(final OperationTransitFinalisee examentexte2Realise, final Consultant consultant,
            final DossierTransit dossierPrestation) {
        final OperationTransitVo examentexte2Vo = new OperationTransitVo();
        examentexte2Vo.setId(examentexte2Realise.getId());

        examentexte2Vo.setSignataireSynthese(getSignataireSynthese());
        InteractionRealisee phaseTransit = phaseTransitService.getInteractionRealiseeById(examentexte2Realise.getIdInteractionRealisee());
        SyntheseTransitDonnees conclusion = syntheseTransitService
                .getSyntheseTransitDonneesById(phaseTransit.getIdPrestationRealisee());
        examentexte2Vo.setSignataireEntretienMedical(getSignataireEntretienMedical(conclusion));

        if (consultant != null) {
            examentexte2Vo.setConsultantId(consultant.getId());
            examentexte2Vo.setEntiteRattachementId(consultant.getCesRattachement());
        }

        if (dossierPrestation != null) {
            examentexte2Vo.setDossierTransitId(dossierPrestation.getId());
            examentexte2Vo.setDatePrestation(dossierPrestation.getDateDemarrage());
        }

        if (CollectionUtils.isNotEmpty(examentexte2Realise.getListeSignalObserveValideExamen())) {
            examentexte2Realise.getListeSignalObserveValideExamen()
                    .forEach(constat -> examentexte2Vo.getSignalObserveValideExamenMap().putIfAbsent(SIGNAL_OBSERVE +
                            constat.getElementSignal().getSignalObserve().getLibelleCourt(),
                            SignalObserveValideHelper.getSignalObserveValideResultatStringValue(constat)));
        }
        if (CollectionUtils.isNotEmpty(examentexte2Realise.getListeSignalObserveValideInterpretation())) {
            examentexte2Realise.getListeSignalObserveValideInterpretation()
                    .forEach(constat -> examentexte2Vo.getSignalObserveValideInterpretationExamenMap().putIfAbsent(SIGNAL_OBSERVE +
                            constat.getElementSignal().getSignalObserve().getLibelleCourt(),
                            SignalObserveValideHelper.getSignalObserveValideResultatStringValue(constat)));
        }
        if (CollectionUtils.isNotEmpty(examentexte2Realise.getListeSignalEquipementValide())) {
            examentexte2Realise.getListeSignalEquipementValide().forEach(constat -> placerSignalEquipementDansMap(examentexte2Vo, constat));
        }

        return examentexte2Vo;
    }

    private void placerSignalEquipementDansMap(OperationTransitVo examentexte2Vo, SignalEquipementValide constatMaterielRealise) {
        if (constatMaterielRealise != null && constatMaterielRealise.getIdSignalDiffuse() != null) {
            SignalDiffuse constatPublie = signalDiffusionService.getSignalDiffuseById(constatMaterielRealise.getIdSignalDiffuse());
            if (constatPublie != null && StringUtils.isNotBlank(constatPublie.getSignalObserveJsonValue())) {
                SignalObserve constat = JsonUtils.convertJsonStringToObject(constatPublie.getSignalObserveJsonValue(), SignalObserve.class);
                constat.setLibelle(constat.getLibelle() + SUFFIXE_SIGNAL_EQUIPEMENT);
                ElementSignal elemSignalObserve = new ElementSignal();
                elemSignalObserve.setSignalObserve(constat);
                SignalObserveValide constatRea = new SignalObserveValide();
                constatRea.setElementSignal(elemSignalObserve);
                constatRea.setIdOperationTransitFinalisee(examentexte2Vo.getId());
                constatRea.setTextValue(constatMaterielRealise.getTextValue());
                constatRea.setNumericValue(constatMaterielRealise.getNumericValue());

                examentexte2Vo.getSignalEquipementValideMap().putIfAbsent(SIGNAL_OBSERVE +
                        constatRea.getElementSignal().getSignalObserve().getLibelleCourt(),
                        SignalObserveValideHelper.getSignalObserveValideResultatStringValue(constatRea));
            }
        }
    }

    @FXML
    public void supprimerOperationTransit() {
        if (gestionnaireAutorisationPhase
                .checkHabilitationSupprimerExamen(getDisplayedInteractionRealiseeInteraction())) {
            supprimerOperationTraitement();
        } else {
            afficherErreurNonAutorise(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_SUPPRESSION_EXAMEN.getLibelle());
        }
    }

    private void supprimerOperationTraitement() {
        if (verifierConditionsSuppressionOperation()) {
            serviceDialogue.openDialogConfirmation(getPrimaryStage(), TITRE_BOITE_DIALOGUE_SUPPRESSION_OPERATION,
                    MESSAGE_BOITE_DIALOGUE_SUPPRESSION_OPERATION, obtenirRappelBoiteDialogueSuppression(), null);
        } else {
            afficherMessageErreurSuppressionOperation();
        }
    }

    private boolean verifierConditionsSuppressionOperation() {
        return getDisplayedInteractionRealiseeInteraction().getIsSupprimable();
    }

    private void afficherMessageErreurSuppressionOperation() {
        getErrorMessageHandler().clear();
        ValidationError error = new ValidationError(ERREUR_SUPPRESSION_OPERATION_LIBELLE, ERREUR_SUPPRESSION_OPERATION_MSG);
        getErrorMessageHandler().addErreur(error);
        controleurConsole.refreshDataAndShow();
    }

    private Consumer<DialogConfirmationController> obtenirRappelBoiteDialogueSuppression() {
        return t -> {
            OperationTransitFinalisee examentexte2 = operationTransitSelectionnee.get();
            spinnerService.executeTask(() -> {

                contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                        getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                        EnumActionTerminal.SUPPRIMER,
                        EnumTypeRessource.FAP,
                        idDossierTransit.toString());

                operationTransitFinaliseeService.deleteExamentexte2ById(examentexte2.getId());
                return StringUtils.EMPTY;
            }, empty -> operationsPostSuppressionOperation(examentexte2), scenePrincipale);
        };
    }

    private void operationsPostSuppressionOperation(OperationTransitFinalisee removedExamentexte2) {
        OperationTransitFinalisee examenRealisePrecedent = tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(removedExamentexte2);
        EnumStatutPhase oldStatut = phaseTransitAffichage.getStatut();
        EnumStatutPhase newStatut = EnumStatutPhase.AFAIRE;
        boolean estOperateurPhaseMisAJour = true;
        if (examenRealisePrecedent != null) {
            idOperationASelectionner = examenRealisePrecedent.getId();
            newStatut = calculerStatutPhaseOperationPrecedente(examenRealisePrecedent);
            if (CollectionUtils.isNotEmpty(examenRealisePrecedent.getListePiecesJointes())) {
                operationTransitFinaliseeService.updateInclusCrOfPiecesJointes(Collections.singletonList(idOperationASelectionner), true);
            }
            estOperateurPhaseMisAJour = majRealisateurSiStatutAutorise(operateurConnecteVoSupplier(),
                    EnumStatutPhase.STATUTS_NECESSITANT_REALISATEUR, phaseTransitAffichage);
        } else {
            idOperationASelectionner = null;
            phaseTransitAffichage.setRealisateur(null);
        }
        if (!oldStatut.equals(newStatut) || estOperateurPhaseMisAJour) {
            phaseTransitAffichage.setStatut(newStatut);
            phaseTransitAffichage = updateInteractionRealisee(phaseTransitAffichage);
        }
        refreshFormData();
        habilitationEpcNouvelExamenBtn();
    }

    private EnumStatutPhase calculerStatutPhaseOperationPrecedente(OperationTransitFinalisee examenRealisePrecedent) {
        EnumStatutPhase enumStatutInteraction = EnumStatutPhase.AFAIRE;
        if (Boolean.TRUE.equals(examenRealisePrecedent.isInterprete())
                || !EnumInteraction.isExamenConnecte(phaseTransitAffichageInteraction.getLibelleCourt())) {
            enumStatutInteraction = EnumStatutPhase.FAIT;
        } else if (CollectionUtils.isNotEmpty(examenRealisePrecedent.getListePiecesJointes())) {
            enumStatutInteraction = EnumStatutPhase.A_INTERPRETER;
        }
        return enumStatutInteraction;
    }

    @FXML
    public void modifierOperationTransit() {
        if (gestionnaireAutorisationPhase.checkHabilitationModifierExamen(getDisplayedInteractionRealiseeInteraction())) {
            lancerModifications();
        } else {
            afficherErreurNonAutorise(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_MODIFICATION_EXAMEN.getLibelle());
        }
    }

    public void lancerModifications() {
        clearAndShowSpecificWarningMessageForDPTraiteWithClotureAvecEmSyntheseParam(getDisplayedDossierTransitPropertyValue());
        operationEnEdition = tableauOperationsFinalisees.getSelectionModel().getSelectedItem();
        setMode(ControllerMode.EDITION);
    }

    @FXML
    public void enregistrerOperation() {
        if (validate(construireReglesValidation())) {
            lancerEnregistrementOperations();
        }
    }

    private void lancerEnregistrementOperations() {
        DossierTransit displayedDossierTransit = getDisplayedDossierTransitPropertyValue();
        if (null != displayedDossierTransit && EnumStatutDossierTransit.isStatutTraite(displayedDossierTransit.getStatut())) {
            operateRegisterOrDeleteAfterConfirmation(dialog -> enregistrerTraitement(), displayedDossierTransit);
        } else {
            enregistrerTraitement();
        }
    }

    @Override
    protected boolean estSuppressionAnnexeAutorisee() {
        return examentexte2Service.estSuppressionAnnexeAutorisee(getStatutDossierTransitAccordingToDPProperty());
    }

    @Override
    protected String obtenirStyleSuppressionAnnexe() {
        return SUPPRIMER_ANNEXE_CSS;
    }

    private void enregistrerTraitement() {
        if (estModeOperation) {
            enregistrerOperation();
        } else {
            enregistrerSynthese();
        }
    }

    @Override
    protected OperationTransitFinalisee enregistrerOperationBaseDonnees(boolean isNouveauExamenPara) {
        OperationTransitFinalisee examentexte2Realise = null;
        if (isNouveauExamenPara) {
            contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                    EnumActionTerminal.NOUVEL_EXAMEN,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());
            OperationTransitFinalisee examenRealise = operationsPreInsertionOperation();
            examentexte2Realise = operationTransitFinaliseeService.saveOperationTransitFinalisee(examenRealise);
            if (CollectionUtils.isEmpty(examenRealise.getListePiecesJointes())) {
                // reset des cases a cocher des PJ des exams réalisés, dans l'EM
                List<OperationTransitFinaliseeVo> listOperationTransitFinalisee = operationTransitFinaliseeService
                        .getListeExamtexte2RealiseByIdInteractReal(examenRealise.getIdInteractionRealisee());
                if (CollectionUtils.isNotEmpty(listOperationTransitFinalisee)) {
                    List<Long> listIdExamtexte2Realise = listOperationTransitFinalisee.stream().map(OperationTransitFinaliseeVo::getId)
                            .collect(Collectors.toList());
                    operationTransitFinaliseeService.updateInclusCrOfPiecesJointes(listIdExamtexte2Realise, false);
                }
            }
        } else {
            contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                    EnumActionTerminal.MODIFIER,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());
            OperationTransitFinalisee examenRealise = operationsPreMiseAJourOperation();
            examentexte2Realise = operationTransitFinaliseeService.updateOperationTransitFinalisee(examenRealise);
        }
        return examentexte2Realise;
    }

    private void afficherErreurSyntheseVide(String errorLibelle, String errorMessage) {
        getErrorMessageHandler().clear();
        ValidationError error = new ValidationError(errorLibelle, errorMessage);
        getErrorMessageHandler().addErreur(error);
        controleurConsole.refreshDataAndShow();
    }

    private void enregistrerSynthese() {
        boolean isEquals = true;
        boolean isInsert = estInsertionSynthese();
        List<SignalObserveValide> lsSignalObserveRea = isInsert ? operationsPreInsertionSynthese() : beforeUpdateInterpretationOperations();
        boolean areEmptySignalObserveRea = SignalObserveValideHelper.areEmptySignalObserveReaValues(isEquals, lsSignalObserveRea);
        if (isInsert) {
            ajouterSyntheseBaseDonnees(areEmptySignalObserveRea, lsSignalObserveRea);
        } else {
            supprimerOuMettreAJourSynthese(lsSignalObserveRea);
        }
    }

    private boolean estInsertionSynthese() {
        boolean result = false;
        if (elementsSignalSyntheseEquipement.isEmpty()) {
            result = !operationTransitSelectionnee.get().isInterprete();
        } else {
            result = estInsertionAvecSignalEquipement(operationTransitSelectionnee.get().getInterpreteur() != null);
        }
        return result;
    }

    private boolean estInsertionAvecSignalEquipement(boolean isAnalysed) {
        boolean result = !isAnalysed;
        if (isAnalysed && CollectionUtils.isEmpty(operationTransitSelectionnee.get().getListeSignalObserveValideInterpretation())) {
            result = true;
        } else if (isAnalysed && syntheseChampsVides(
                !operationTransitSelectionnee.get().getListeSignalObserveValideInterpretation().isEmpty())) {
            result = !operationTransitSelectionnee.get().isInterprete();
        }
        return result;
    }

    private boolean syntheseChampsVides(boolean isUpdate) {
        List<SignalObserveValide> lsSignalObserveValides = obtenirListeSyntheseSignaux(
                operationTransitSelectionnee.get().getId(), isUpdate);
        boolean isEquals = true;
        return SignalObserveValideHelper.areEmptySignalObserveReaValues(isEquals, lsSignalObserveValides);
    }

    private void ajouterSyntheseBaseDonnees(boolean areAllSignalObservesRealisesAreEmpty, List<SignalObserveValide> lsSignalObserveRea) {
        boolean isNotAnalyse = operationTransitSelectionnee.get().getAnalyste() == null;
        boolean isModifIntoMaterielPart = !modificationDansSyntheseInitEtEquipement(null, lsSignalObserveRea) && isNotAnalyse;
        List<SignalEquipementValide> lsCMR = operationTransitSelectionnee.get().getListeSignalEquipementValide();
        boolean wasExamenLaunched = lsCMR != null && !lsCMR.isEmpty();

        boolean isExamenMaterielDeleteCase = !elementsSignalSyntheseEquipement.isEmpty() && syntheseDonneesVides(lsSignalObserveRea) && isNotAnalyse
                && wasExamenLaunched;
        if (!areAllSignalObservesRealisesAreEmpty || isModifIntoMaterielPart) {
            addInterpretationDatabase(lsSignalObserveRea, areAllSignalObservesRealisesAreEmpty, isExamenMaterielDeleteCase);
        } else {
            afficherErreurSyntheseVide(ERREUR_SYNTHESE_LIBELLE, ERREUR_SYNTHESE_MSG);
        }
    }

    private void addInterpretationDatabase(List<SignalObserveValide> lsSignalObserveRea, boolean areEmptySignalObservesRealises, boolean isMaterielDelCase) {
        spinnerService.executeTask(() -> {

            contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                    EnumActionTerminal.AJOUTER_INTERPRETATION,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());

            return operationTransitFinaliseeService.saveOperationTransitFinaliseeInterpretation(lsSignalObserveRea, operateurConnecte, areEmptySignalObservesRealises);
        }, examenRealise -> operationsPostAjoutSyntheseBase(examenRealise, isMaterielDelCase), scenePrincipale);
    }

    private void operationsPostAjoutSyntheseBase(OperationTransitFinalisee examenRealise, boolean isMaterielDelCase) {
        if (operationTransitSelectionnee.get().equals(tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(null)) && !isMaterielDelCase) {
            mettreAJourPhasePourNouvelleOperation(examenRealise);
        }
        if (examenRealise != null) {
            idOperationASelectionner = examenRealise.getId();
        }
        saisirInterpretationMode(ControllerMode.CONSULTATION);
        refreshFormData();
    }

    private void supprimerOuMettreAJourSynthese(List<SignalObserveValide> lsSignalObserveRea) {
        boolean isEmptyLsSignalObserveReaInterpOuLecture = SignalObserveValideHelper.isListSignalObserveValideEmpty(lsSignalObserveRea);
        spinnerService.executeTask(() -> casSuppressionOuMiseAJourSynthese(isEmptyLsSignalObserveReaInterpOuLecture, lsSignalObserveRea),
                examenRealise -> operationsPostSuppressionOuMiseAJourSynthese(isEmptyLsSignalObserveReaInterpOuLecture, examenRealise), scenePrincipale);
    }

    private OperationTransitFinalisee casSuppressionOuMiseAJourSynthese(boolean isEmptyLsSignalObserve, List<SignalObserveValide> lsSignalObserveRea) {
        if (isEmptyLsSignalObserve) {
            contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                    EnumActionTerminal.SUPPRIMER_INTERPRETATION,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());

            return operationTransitFinaliseeService.deleteOperationTransitFinaliseeInterpretation(lsSignalObserveRea);
        } else {
            contexteSession.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumTerminalOfExamentexte2(), getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                    EnumActionTerminal.MODIFIER_INTERPRETATION,
                    EnumTypeRessource.FAP,
                    idDossierTransit.toString());
            Operateur existingInterp = operationTransitSelectionnee.get().getInterpreteur();
            return operationTransitFinaliseeService.updateOperationTransitFinaliseeInterpretation(lsSignalObserveRea, existingInterp);
        }
    }

    private void operationsPostSuppressionOuMiseAJourSynthese(boolean isEmptyLsSignalObserveReaInterpretation, OperationTransitFinalisee examenRealise) {
        boolean isLastExamen = operationTransitSelectionnee.get().equals(tableauOperationsFinalisees.getLastCreatedOperationTransitFinalisee(null));
        boolean isExamenRealiseNotNull = examenRealise != null;
        mettreAJourBoutonsDerniereOperation(isLastExamen);
        if (isLastExamen) {
            mettreAJourPhasePourNouvelleOperation(examenRealise);
        }
        if (isExamenRealiseNotNull) {
            idOperationASelectionner = examenRealise.getId();
        }
        saisirInterpretationMode(ControllerMode.CONSULTATION);
        if (!isEmptyLsSignalObserveReaInterpretation && isExamenRealiseNotNull && examenRealise.equals(operationTransitSelectionnee.get())) {
            tableauOperationsFinalisees.scrollTableViewToSelectedExamentexte2Row(idOperationASelectionner);
        } else {
            refreshFormData();
        }
    }

    private List<SignalObserveValide> operationsPreInsertionSynthese() {
        Long idOperationTransitFinalisee = operationTransitSelectionnee.getValue().getId();
        return obtenirListeSyntheseSignaux(idOperationTransitFinalisee, false);
    }

    @Override
    protected List<SignalObserveValide> obtenirListeCasInsertionSynthese() {
        return obtenirListeSignauxSyntheseInsertion();
    }

    private OperationTransitFinalisee operationsPreInsertionOperation() {
        OperationTransitFinalisee examenParaRealise = new OperationTransitFinalisee();
        examenParaRealise.setRealisateur(operateurConnecteVo);
        examenParaRealise.setDateCreation(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        examenParaRealise.setIdInteractionRealisee(phaseTransitAffichage.getId());
        examenParaRealise.setInterprete(false);
        String infoMaterielUtiliseJsonString = JsonUtils
                .convertObjectToJsonString(obtenirInfoEquipementDepuisAttribution(attributionsEquipementUtilisables));
        examenParaRealise.setInfoMaterielUtilise(infoMaterielUtiliseJsonString);
        examenParaRealise.setListeSignalObserveValideExamen(obtenirListeOperationsSignaux());
        examenParaRealise.setIsLecturePrerequise(caseCocherPreRequis.isSelected());
        return examenParaRealise;
    }

    private OperationTransitFinalisee operationsPreMiseAJourOperation() {
        OperationTransitFinalisee examenParaRealise = operationTransitSelectionnee.getValue();
        LocalDateTime actualDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        if (null == examenParaRealise.getDatePremiereModification()) {
            examenParaRealise.setDatePremiereModification(actualDateTime);
        }
        examenParaRealise.setDateModification(actualDateTime);
        List<SignalObserveValide> lsSignalObservesRealises = examenParaRealise.getListeSignalObserveValideExamen();
        List<SignalObserveValide> lsSignalObservesRealisesMaj = lsSignalObservesRealises.stream()
                .map(ctr -> retrieveSignalObserveValideFromElementSignal(ctr.getElementSignal(), ctr.getId(), composantOperationProprietesParIdElement))
                .collect(Collectors.toList());
        examenParaRealise.setListeSignalObserveValideExamen(lsSignalObservesRealisesMaj);
        examenParaRealise.setIsLecturePrerequise(caseCocherPreRequis.isSelected());
        return examenParaRealise;
    }

    private List<SignalObserveValide> obtenirListeOperationsSignaux() {
        List<SignalObserveValide> constatRealises = new ArrayList<>();
        if (!elementsSignalExamen.isEmpty()) {
            constatRealises = elementsSignalExamen.stream()
                    .map(cs -> retrieveSignalObserveValideFromElementSignal(cs, null, composantOperationProprietesParIdElement))
                    .collect(Collectors.toList());
        }
        return constatRealises;
    }

    List<SignalObserveValide> obtenirListeSyntheseSignaux(Long idOperationTransitFinalisee, boolean isUpdate) {
        signauxObservesSynthese = new ArrayList<>();
        if (!isUpdate) {
            signauxObservesSynthese = obtenirListeSignauxSyntheseInsertion();
        } else {
            getListSignalObservesRealisesUpdateCase();
        }
        signauxObservesSynthese.forEach(constatReal -> constatReal.setIdOperationTransitFinalisee(idOperationTransitFinalisee));
        return signauxObservesSynthese;
    }

    private List<SignalObserveValide> obtenirListeSignauxSyntheseInsertion() {
        return elementsSignalInterpretation.stream().map(
                cs -> retrieveSignalObserveValideFromElementSignal(cs, null, composantSyntheseProprietesParIdElement))
                .collect(Collectors.toList());
    }

    @Override
    protected List<ElementSignal> getInterpretationElements() {
        return elementsSignalInterpretation;
    }

    @Override
    protected Map<Long, ElementGrilleTerminalPropriete> getInterpretationPropertiesByElementId() {
        return composantSyntheseProprietesParIdElement;
    }

    @FXML
    public void epcNouvelExamen() {
        final EnumErreurLancementOperation retourFaisabiliteExamen = verifierFaisabiliteExamen();
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        if (contexteSession.getListeAffectationsMateriel() != null
                && interaction.getReferenceTransitsTypeMateriel() != null
                && !interaction.getReferenceTransitsTypeMateriel().isEmpty()
                && !EnumErreurLancementOperation.AUCUNE_ERREUR.equals(retourFaisabiliteExamen)) {
            serviceDialogue.openDialogAlerte(null, "Erreur utilisation matériel",
                    String.format(retourFaisabiliteExamen.getLibelleErreur(), contexteSession.getZoneTransit().getNom()));
        } else {
            if (gestionnaireAutorisationPhase.checkHabilitationRealiserExamen(interaction)) {
                constatVerificationBeforeOperation();
            } else {
                afficherErreurNonAutorise(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_REALISATION_EXAMEN.getLibelle());
            }
        }
    }

    private EnumErreurLancementOperation verifierFaisabiliteExamen() {
        EnumErreurLancementOperation faisabilite = EnumErreurLancementOperation.AUCUNE_ERREUR;
        final List<ReferenceTransit> listeTypesMaterielInteraction = getDisplayedInteractionRealiseeInteraction().getReferenceTransitsTypeMateriel();

        final Set<String> listeNomsMaterielValideSession = listeAffectationsMaterielToSetOfStrings(contexteSession.getListeAffectationsMateriel());
        final Set<String> listeNomsMaterielNecessairesInteraction = listeTypesMaterielInteraction != null
                ? listeTypeMaterielsToSetOfStrings(listeTypesMaterielInteraction)
                : new HashSet<>();
        final boolean contientMaterielInutilisable = isContientMaterielInutilisable(listeNomsMaterielNecessairesInteraction);
        final boolean contientMaterielUtilisable = isContientMaterielUtilisable(listeNomsMaterielNecessairesInteraction);
        final boolean materielAbsent = !listeNomsMaterielValideSession.containsAll(listeNomsMaterielNecessairesInteraction);
        faisabilite = getTypeErreurByFaisabilite(faisabilite, contientMaterielInutilisable, contientMaterielUtilisable, materielAbsent);
        return faisabilite;
    }

    private boolean isContientMaterielInutilisable(final Set<String> listeNomsMaterielNecessairesInteraction) {
        final Predicate<AttributionEquipement> justeLeMaterielNecessairePourInteraction = am -> listeNomsMaterielNecessairesInteraction
                .contains(am.getModele().getType());
        return contexteSession.getListeAffectationsMateriel().stream().filter(justeLeMaterielNecessairePourInteraction)
                .anyMatch(PREDICAT_MATERIEL_UTILISABLE.negate());
    }

    private boolean isContientMaterielUtilisable(final Set<String> listeNomsMaterielNecessairesInteraction) {
        final Predicate<AttributionEquipement> justeLeMaterielNecessairePourInteraction = am -> listeNomsMaterielNecessairesInteraction
                .contains(am.getModele().getType());
        final List<AttributionEquipement> contientMaterielInutilisable = contexteSession.getListeAffectationsMateriel().stream()
                .filter(justeLeMaterielNecessairePourInteraction).filter(AttributionEquipement::isUtilisable).collect(Collectors.toList());
        Set<String> typeMaterielUtilisableNecessaire = new HashSet<>();
        for (AttributionEquipement affectationMateriel : contientMaterielInutilisable) {
            typeMaterielUtilisableNecessaire.add(affectationMateriel.getModele().getType());
        }
        return listeNomsMaterielNecessairesInteraction.equals(typeMaterielUtilisableNecessaire);
    }

    private Set<String> listeTypeMaterielsToSetOfStrings(final List<ReferenceTransit> listeTypesMateriel) {
        return listeTypesMateriel.stream().map(ReferenceTransit::getIntitule).collect(Collectors.toSet());
    }

    private Set<String> listeAffectationsMaterielToSetOfStrings(final List<AttributionEquipement> listeAffectationsMateriel) {
        return listeAffectationsMateriel.stream().map(item -> item.getModele().getType()).collect(Collectors.toSet());
    }

    private void constatVerificationBeforeOperation() {
        if (estNouvelleOperationValide()) {
            initialiserNouvelleOperation();
        } else {
            clearConsoleAndAddMessage(ERREUR_AUCUN_SIGNAL_LIBELLE, ERREUR_AUCUN_SIGNAL);
        }
    }

    private boolean estNouvelleOperationValide() {
        return !elementsSignalExamen.isEmpty() || !elementsSignalEquipement.isEmpty();
    }

    private void initialiserNouvelleOperation() {
        if (verifierConditionsNouvelleOperation()) {
            lancerNouvelleOperationAvecAvertissement();
        } else {
            clearConsoleAndAddMessage(ERREUR_TENTATIVES_MULTIPLES_LIBELLE, ERREUR_TENTATIVES_MULTIPLES_MSG);
        }
    }

    public void lancerNouvelleOperationAvecAvertissement() {
        clearAndShowSpecificWarningMessageForDPTraiteWithClotureAvecEmSyntheseParam(getDisplayedDossierTransitPropertyValue());
        tableauOperationsFinalisees.getSelectionModel().clearSelection();
        operationEnEdition = null;
        phaseTransitAffichage.setStatut(EnumStatutPhase.AFAIRE);
        if (operationsTransitExistantes.isEmpty()) {
            if (operationRealiseeDonneesFinalisees != null) {
                operationRealiseeDonneesFinalisees.getSignalObserveValides().stream().map(ResultatSignalObserve::new)
                        .forEach(this::applyResultatSignalObserveIntoGrilles);
            }

            RegleResultat reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getRegleContexttexte2());
            composantOperationProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                    dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
            composantSyntheseProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                    dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
            refreshReglesAffichageEtSignalObserves(reglesResult);
        } else {
            rowSelectionOperations(null);
        }
        moduleDynamiqueImpl.changeAsterixToShowValue(false);
        setMode(ControllerMode.EDITION);
    }

    private DonneesFinalisees getDonneesFinaliseesInteraction() {
        DonneesFinalisees donneesRealiseesInteraction = new DonneesFinalisees();
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        List<SignalObserveValideVo> constatRealiseWithOrigineInfos = getListSignalObserveValideExamen()
                .stream()
                .map(constatRealise -> SignalObserveValideVoFactory
                        .addOrigineInformationsToSignalObserveValide(constatRealise,
                                interaction.getLibelleCourt(),
                                OperateurVoFactory.buildOperateurVo(contexteSession.getConnectedUser())))
                .collect(Collectors.toList());
        donneesRealiseesInteraction.setSignalObserveValides(constatRealiseWithOrigineInfos);

        List<SignalObserveValideVo> constatRealiseInterpretationInsertWithOrigineInfos = obtenirListeSignauxSyntheseInsertion()
                .stream()
                .map(constatRealise -> SignalObserveValideVoFactory
                        .addOrigineInformationsToSignalObserveValide(constatRealise,
                                interaction.getLibelleCourt(),
                                OperateurVoFactory.buildOperateurVo(contexteSession.getConnectedUser())))
                .collect(Collectors.toList());

        donneesRealiseesInteraction.getSignalObserveValides().addAll(constatRealiseInterpretationInsertWithOrigineInfos);

        if (operationRealiseeDonneesFinalisees != null) {
            List<SignalObserveValideVo> constatRealiseNotAlreadyPresent = operationRealiseeDonneesFinalisees
                    .getSignalObserveValides().stream()
                    .filter(constatRealise -> donneesRealiseesInteraction.getSignalObserveValides().stream()
                            .noneMatch(constatRealiseInteraction -> constatRealiseInteraction.getElementSignal()
                                    .getSignalObserve().equals(constatRealise.getElementSignal().getSignalObserve())))
                    .collect(Collectors.toList());
            donneesRealiseesInteraction.getSignalObserveValides().addAll(constatRealiseNotAlreadyPresent);

            donneesRealiseesInteraction.getSignalEquipementValides()
                    .addAll(operationRealiseeDonneesFinalisees.getSignalEquipementValides());
            donneesRealiseesInteraction.getAnteceFamiliaux()
                    .addAll(operationRealiseeDonneesFinalisees.getAnteceFamiliaux());
            donneesRealiseesInteraction.getAntecePersoMedicaux()
                    .addAll(operationRealiseeDonneesFinalisees.getAntecePersoMedicaux());
            donneesRealiseesInteraction.getAntecePersoOperes()
                    .addAll(operationRealiseeDonneesFinalisees.getAntecePersoOperes());
            donneesRealiseesInteraction.getMaladiesConnues()
                    .addAll(operationRealiseeDonneesFinalisees.getMaladiesConnues());
            donneesRealiseesInteraction.getQuestionReponseQuestionRealisees()
                    .addAll(operationRealiseeDonneesFinalisees.getQuestionReponseQuestionRealisees());
            donneesRealiseesInteraction.getSignesFonctionnels()
                    .addAll(operationRealiseeDonneesFinalisees.getSignesFonctionnels());
            donneesRealiseesInteraction.getSignesPhysiques()
                    .addAll(operationRealiseeDonneesFinalisees.getSignesPhysiques());
        }
        return donneesRealiseesInteraction;
    }

    private void applyResultatSignalObserveIntoGrilles(ResultatSignalObserve constatResult) {
        if (composantOperationProprietesParIdElement != null) {
            composantOperationProprietesParIdElement.applyResultatSignalObserveIntoGrille(constatResult);
        }
        if (composantSyntheseProprietesParIdElement != null) {
            composantSyntheseProprietesParIdElement.applyResultatSignalObserveIntoGrille(constatResult);
        }
    }

    private boolean verifierConditionsNouvelleOperation() {
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        return interaction.getIsPlusieursTentative() || (!interaction.getIsPlusieursTentative() && tableauOperationsFinalisees.getItems().isEmpty());
    }

    @FXML
    public void epcSaisirInterpretation() {
        if (gestionnaireAutorisationPhase.checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction())) {
            launchAddInterpretationOperationsWithWarningIfStatutDPTraite();
        } else {
            afficherErreurNonAutorise(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_SAISIE_INTERPRETATION.getLibelle());
        }
    }

    public void launchAddInterpretationOperationsWithWarningIfStatutDPTraite() {
        clearAndShowSpecificWarningMessageForDPTraiteWithClotureAvecEmSyntheseParam(getDisplayedDossierTransitPropertyValue());
        operationEnEdition = tableauOperationsFinalisees.getSelectionModel().getSelectedItem();
        saisirInterpretationMode(ControllerMode.EDITION);
    }

    private void saisirInterpretationMode(ControllerMode mode) {
        if (mode.equals(ControllerMode.EDITION)) {
            saisirInterpretationEditionModeCaseOperations();
        } else {
            estModeOperation = true;
            saisirInterpretationConsultationModeCaseOperations();
        }
    }

    private void saisirInterpretationEditionModeCaseOperations() {
        modeControleurSynthese = ControllerMode.EDITION;
        estModeOperation = false;
        definirEtatEnfantsPreRequis();
        Stream.of(boutonModifierOperation, boutonSupprimerOperation, boutonImprimerOperation, boutonSynthese, boutonLancerOperation)
                .forEach(btn -> btn.disableProperty().unbind());
        Stream.of(boutonSupprimerOperation, boutonModifierOperation, boutonImprimerOperation, boutonNouvelleOperation, boutonSynthese,
                boutonRecupererResultats, boutonLancerOperation).forEach(btn -> definirDesactive(btn, true));
        Stream.of(boutonAnnulerOperation, boutonAjouterAnnexeManuelle)
                .forEach(btn -> definirDesactive(btn, false));
        boutonEnregistrerOperation.definirDesactive(!gestionnaireAutorisationPhase.checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction()));
        moduleDynamiqueImpl.switchModeTo(ControllerMode.EDITION, composantSyntheseProprietesParIdElement);
    }

    private void saisirInterpretationConsultationModeCaseOperations() {
        modeControleurSynthese = ControllerMode.CONSULTATION;

        definirDesactive(boutonSupprimerOperation, isSupprimerExamenNotAllowed(getDisplayedInteractionRealiseeInteraction()));
        definirDesactive(boutonNouvelleOperation, !isNouvelExamenBtnAllowed());
        if (estModeLecture) {
            definirDesactive(boutonSynthese, !gestionnaireAction.hasHabilitation(Habilitation.GERER_EXAMEN_LECTURE_OPERATION) || !caseCocherPreRequis.isSelected());
        } else {
            definirDesactive(boutonSynthese, !gestionnaireAutorisationPhase.checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction()));
        }
        definirDesactive(boutonModifierOperation, !gestionnaireAutorisationPhase.checkHabilitationModifierExamen(getDisplayedInteractionRealiseeInteraction()));
        definirDesactive(boutonImprimerOperation, !gestionnaireAutorisationPhase.checkHabilitationImprimerExamen(getDisplayedInteractionRealiseeInteraction()));

        boutonSupprimerOperation.disableProperty().bind(supprimerBtnBinding());

        Stream.of(boutonModifierOperation, boutonImprimerOperation).forEach(btn -> btn.disableProperty()
                .bind(tableauOperationsFinalisees.getSelectionModel().selectedItemProperty().isNull()));
        boutonSynthese.disableProperty().bind(interpreterBtnBinding().not());
        Stream.of(boutonEnregistrerOperation, boutonAnnulerOperation, boutonRecupererResultats, boutonAjouterAnnexeManuelle)
                .forEach(btn -> definirDesactive(btn, true));
        BooleanBinding canLancerExamenBeLaunched = Bindings.createBooleanBinding(peutLancerOperation(),
                tableauOperationsFinalisees.getSelectionModel().selectedItemProperty());
        boutonLancerOperation.disableProperty().bind(canLancerExamenBeLaunched.not());
        moduleDynamiqueImpl.switchModeTo(ControllerMode.CONSULTATION, composantSyntheseProprietesParIdElement);
    }

    private BooleanBinding interpreterBtnBinding() {
        return Bindings.createBooleanBinding(() -> {
            boolean estModificationAutorisee = estModificationAutorisee();
            boolean isSelectedItem = tableauOperationsFinalisees.getSelectionModel().selectedItemProperty().isNotNull().get();
            boolean isInterpretationOuLectureItem = elementsSignalInterpretation.emptyProperty().not().get();
            boolean isHabilitationInterpreterOuLecture = !estModeLecture ? gestionnaireAutorisationPhase
                    .checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction())
                    : (gestionnaireAction.hasHabilitation(Habilitation.GERER_EXAMEN_LECTURE_OPERATION) && caseCocherPreRequis.isSelected());
            return estModificationAutorisee && isSelectedItem && isInterpretationOuLectureItem && isHabilitationInterpreterOuLecture;
        }, tableauOperationsFinalisees.getSelectionModel().selectedItemProperty(), elementsSignalInterpretation.emptyProperty());
    }

    private BooleanBinding supprimerBtnBinding() {
        return Bindings.createBooleanBinding(() -> {
            boolean isNoSelectedItem = tableauOperationsFinalisees.getSelectionModel().selectedItemProperty().isNull().get();
            boolean isStateNotAllowed = estPhaseTransmiseOuEnAttente(phaseTransitAffichage);
            return isNoSelectedItem || isStateNotAllowed;
        }, tableauOperationsFinalisees.getSelectionModel().selectedItemProperty());
    }

    private boolean isNouvelExamenBtnAllowed() {
        boolean estModificationAutorisee = estModificationAutorisee();
        boolean isHabilitationRealised = gestionnaireAutorisationPhase
                .checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction());
        boolean isStateAllowed = phaseTransitAffichage == null
                || !estPhaseTransmiseOuEnAttente(phaseTransitAffichage);
        return estModificationAutorisee && isHabilitationRealised && isStateAllowed;
    }

    private boolean isSupprimerExamenNotAllowed(Interaction interaction) {
        return !gestionnaireAutorisationPhase.checkHabilitationSupprimerExamen(interaction)
                || estPhaseTransmiseOuEnAttente(phaseTransitAffichage);
    }

    private Spinner getSpinner() {
        Spinner spinner;
        Pane mainPane = (Pane) scenePrincipale.getScene().getRoot();
        if (null != mainPane) {
            if (!mainPane.getChildren().isEmpty()) {
                Node nodeSpinner = mainPane.getChildren().stream().filter(Spinner.class::isInstance).findFirst().orElse(null);
                spinner = (null != nodeSpinner ? (Spinner) nodeSpinner : null);
            } else {
                spinner = new Spinner();
                mainPane.getChildren().add(spinner);
            }
            return spinner;
        }
        return null;
    }

    private void tracerRecuperationResultats() {
        TraceVo traceVo = new TraceVo();
        traceVo.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE, EnumTerminal.EXAMEN_PARACLINIQUE,
                getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                EnumActionTerminal.ACQUISITION_RESULTAT,
                EnumTypeRessource.FAP,
                idDossierTransit.toString());
        serviceTrace.create(traceVo);
    }

    @FXML
    public void recupererResultatsOperation() {
        tracerRecuperationResultats();

        if (validate(construireReglesValidationAvantRecuperation())) {
            // DANS CE CAS SEULEMENT, AJOUT DE MODIFICATIONS SUR LE CSS PROPRE AU SPINNER
            if (null != getPrimaryStage() && null != getPrimaryStage().getScene()) {
                mainSpinner = getSpinner();
                if (null != mainSpinner && !mainSpinner.getStyleClass().contains(CLASSE_CSS_INDICATEUR_TEXTE)) {
                    mainSpinner.getStyleClass().add(CLASSE_CSS_INDICATEUR_TEXTE);
                }
            }

            if (estFormatEchangeStandard()) {
                spinnerService.executeTaskWithText(this::recupererResultatsEquipementConnecte,
                        this::executerPostRecuperationStandardCasSpecifique, PREPARATION_FICHIERS, scenePrincipale);
            } else {
                spinnerService.executeTaskWithText(this::recupererProgrammeAnalyse,
                        this::executerPostRecuperationCasSpecifique, PREPARATION_FICHIERS, scenePrincipale);
            }
        }
    }

    private boolean recupererResultatsEquipementConnecte() {
        boolean isOk = false;
        try {
            Map<String, List<ElementSignal>> mapElementSignal = obtenirMapperElementsSignal();
            mapElementSignal.put(EnumTypeElementSignal.ELEMENT_SIGNAL_OBSERVE_EXAMEN_OBLIGATOIRE_AVANT_ACQUISITION.getCode(),
                    obtenirListeElementsSignauxObligatoires());

            OperationTransitFinalisee examentexte2RealiseToUpdate = operationsPreMiseAJourOperation();
            InteractionRealisee phaseTransitToUpdate = obtenirPhaseAMettreAJourPostRecuperation();

            operationTransitFinaliseeService.recupererResultatsEquipementConnecte(attributionEquipementConnecte,
                    examentexte2RealiseToUpdate,
                    phaseTransitToUpdate,
                    mapElementSignal,
                    idPassager,
                    idDossierTransit);
            isOk = true;
        } catch (Hl7MessageReaderException e) {
            LOGGER.warn(e.getMessage(), e);
            List<ValidationError> errors = e.getErreurs().stream().map(error -> new ValidationError(RECUPERATION_OPERATION, error))
                    .collect(Collectors.toList());
            clearAddListOfValidationErrorAndShowConsole(errors);
        } catch (MaterielConnectedAquisitionResultsEmptyException e) {
            LOGGER.warn(e.getMessage(), e);
            clearAddValidationErrorAndShowConsole(new ValidationError(ErrorLevel.WARNING, RECUPERATION_OPERATION, FICHIERS_RESULTAT_INCOMPLETS));
        } catch (MaterielConnectedAquisitionFileNotFoundException | FileNotFoundInDirectoryException e) {
            LOGGER.warn(e.getMessage(), e);
            clearConsoleAndAddMessage(RECUPERATION_OPERATION, e.getMessage());
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            clearConsoleAndAddMessage(RECUPERATION_OPERATION, "Erreur : " + e.getMessage());
        }
        return isOk;
    }

    private boolean estFormatEchangeStandard() {
        return attributionEquipementConnecte.getModele().getFormatEchange().isHl7();
    }

    private List<ValidationRule> construireReglesValidationAvantRecuperation() {
        List<ValidationRule> listValidationRules = new ArrayList<>();
        List<ElementSignal> listElementSignalsObligatoiresAvantAcquisition = obtenirListeElementsSignauxObligatoires();
        if (CollectionUtils.isNotEmpty(listElementSignalsObligatoiresAvantAcquisition)) {
            listElementSignalsObligatoiresAvantAcquisition
                    .forEach(elementSignalObserve -> creerRegleValidationSignalAvantRecuperation(listValidationRules, elementSignalObserve));
        }
        return listValidationRules;
    }

    private void creerRegleValidationSignalAvantRecuperation(List<ValidationRule> listValidationRules, ElementSignal elemSignalObserve) {
        listValidationRules.add(creerRegleValidationSignalObserve(elemSignalObserve, true));
    }

    // Que la valeur retournée par la première opération soit true ou false, la méthode executeTaskWithText lance
    // la seconde opération. De ce fait, mise en place d'une vérification avant le lancement de la seconde opération
    private void executerPostRecuperationCasSpecifique(Boolean isOk) {
        if (Boolean.TRUE.equals(isOk)) {
            operationsPostRecuperationResultats();
        }
        supprimerIndicateurTexte();
    }

    private void executerPostRecuperationStandardCasSpecifique(Boolean isOk) {
        if (Boolean.TRUE.equals(isOk)) {
            refreshDonneesAfterAcquisitionData();
        }
        supprimerIndicateurTexte();
    }

    private void supprimerIndicateurTexte() {
        if (null != getPrimaryStage() && null != mainSpinner) {
            mainSpinner.getStyleClass().remove(CLASSE_CSS_INDICATEUR_TEXTE);
        }
    }

    private void executerPauseThread() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private Boolean recupererProgrammeAnalyse() {
        if (null != getPrimaryStage() && null != mainSpinner) {
            executerPauseThread();
        }

        String programmeEtudeId = fichePassageData != null ? fichePassageData.getIdProgrammeEtude() : null;

        if (programmeEtudeId == null) {
            programmeEtudeId = idPassagerStr;
        }

        return executerRecuperationEtEnregistrement(programmeEtudeId);
    }

    private Map<String, List<ElementSignal>> obtenirMapperElementsSignal() {
        Map<String, List<ElementSignal>> elementsSignalObserveMapper = new HashMap<>();
        elementsSignalObserveMapper.put("E", elementsSignalEquipement);
        elementsSignalObserveMapper.put("I", elementsSignalSyntheseEquipement);
        return elementsSignalObserveMapper;
    }

    public void supprimerLiaisonProprietesContexte() {
        estExecutionModificationAttribution.unbind();
    }

    private Boolean executerRecuperationEtEnregistrement(String programmeEtudeId) {
        try {
            boolean isFullOperationRealised = operationTransitFinaliseeService.retrieveAndSaveResultsFromFile(attributionEquipementConnecte,
                    operationTransitSelectionnee.get(), programmeEtudeId, obtenirMapperElementsSignal());
            if (!isFullOperationRealised) {
                clearAddValidationErrorAndShowConsole(new ValidationError(ErrorLevel.WARNING, RECUPERATION_OPERATION, FICHIERS_RESULTAT_INCOMPLETS));
            }
            return Boolean.TRUE;

        } catch (MaterielConnectedAquisitionFileNotFoundException e) {
            LOGGER.warn(e.getMessage(), e);
            clearConsoleAndAddMessage(RECUPERATION_OPERATION, e.getMessage());
            return Boolean.FALSE;
        }
    }

    protected void operationsPostRecuperationResultats() {
        operationTransitSelectionnee.get().setListePiecesJointes(new ArrayList<>());
        if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
            try {
                List<RapportTransit> listeRapportTransits = new ArrayList<>();
                switch (attributionEquipementConnecte.getModele().getType()) {
                    case EQUIPEMENT_MESURE_AUDIO:
                        creerEtRecupererAnnexeAudio(listeRapportTransits);
                        break;
                    case EQUIPEMENT_SIGNAL_CARDIQUE:
                        creerEtRecupererAnnexeSignal(listeRapportTransits);
                        break;
                    case EQUIPEMENT_MESURE_FLUX:
                        examentexte2Service.creationEtRecupPJSpirometrie(listeRapportTransits, attributionEquipementConnecte, idDossierTransit,
                                this::ajouterAttributsRapportTransit);
                        break;
                    default:
                        LOGGER.warn("Type de matériel inconnu : {}", attributionEquipementConnecte.getModele().getType());
                        break;
                }
                mettreAJourStatutPhaseEtOperateur(listeRapportTransits);
                listeRapportTransits.forEach(Doc -> stockerAnnexe(converterDocumentVersRapportTransit.convert(Doc)));

                if (signauxObligatoiresAvantRecuperationExistent()) {
                    enregistrerOperationBaseDonnees(false);
                }
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
                clearConsoleAndAddMessage(ERREUR_ANNEXES_LIBELLE, ERREUR_RECUPERATION_ANNEXE_MSG);
            }
        }

        refreshDonneesAfterAcquisitionData();
    }

    private void refreshDonneesAfterAcquisitionData() {
        Long idOperationASelectionner = operationTransitSelectionnee.get().getId();
        boutonAnnulerOperation.setText(LIBELLE_ANNULER_PAR_DEFAUT);
        estLancementModule = false;
        setMode(ControllerMode.CONSULTATION);
        refreshFormData();
        RegleResultat reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getRegleContexttexte2());
        composantOperationProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
        composantSyntheseProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
        if (idOperationASelectionner != null) {
            notEmptyRowSelectionInterpAndMatOperations(operationTransitSelectionnee.get());
        }
        refreshReglesAffichageEtSignalObserves(reglesResult);
        supprimerIndicateurTexte();
    }

    private List<ElementSignal> obtenirListeElementsSignauxObligatoires() {
        return getAvailableElementSignalListFromFullList(elementsSignalExamen).stream()
                .filter(eltSignalObserve -> Boolean.TRUE.equals(eltSignalObserve.getIsObligatoireAvantAcquisition()))
                .collect(Collectors.toList());
    }

    private boolean signauxObligatoiresAvantRecuperationExistent() {
        List<ElementSignal> elementsSignalAvailable = obtenirListeElementsSignauxObligatoires();
        return CollectionUtils.isNotEmpty(elementsSignalAvailable);
    }

    private InteractionRealisee obtenirPhaseAMettreAJourPostRecuperation() {
        final InteractionRealisee clonedInteractionRealisee = (InteractionRealisee) phaseTransitAffichage.clone();
        clonedInteractionRealisee.setStatut(statutPhaseASynthetiserOuALire);
        majRealisateurSiStatutAutorise(operateurConnecteVoSupplier(), EnumStatutPhase.STATUTS_NECESSITANT_REALISATEUR, clonedInteractionRealisee);
        if (OperateurVoFactory.getOperateurVoSystem().equals(clonedInteractionRealisee.getInitiateur())) {
            clonedInteractionRealisee.setInitiateur(null);
        }
        return clonedInteractionRealisee;
    }

    private void mettreAJourStatutPhaseEtOperateur(List<RapportTransit> listeRapportTransits) {
        if (CollectionUtils.isNotEmpty(listeRapportTransits)) {
            phaseTransitAffichage.setStatut(statutPhaseASynthetiserOuALire);
            majRealisateurSiStatutAutorise(operateurConnecteVoSupplier(), EnumStatutPhase.STATUTS_NECESSITANT_REALISATEUR, phaseTransitAffichage);
            phaseTransitAffichage = updateInteractionRealisee(phaseTransitAffichage);
        }
    }

    private RapportTransit creerDocumentPdfSignal(File file, String fileNameSuffix) {
        if (file != null && EnumTypeRapport.PARACLINIQUE != null && fileNameSuffix != null) {
            RapportTransit documentToUpload = new RapportTransit();
            try {
                byte[] pdfEcgAvecInsertionRectangeBlanc = fileUtils.cacherPartieDocPdf(Files.readAllBytes(file.toPath()));
                documentToUpload.setContent(pdfEcgAvecInsertionRectangeBlanc);
            } catch (IOException e) {
                throw new ModelTechnicalException("Une erreur est survenue lors de la lecture du fichier", e);
            }
            documentToUpload.setType(EnumTypeRapport.PARACLINIQUE);
            documentToUpload.setNom(FileUtils.getFilenameWithSuffix(file.getName(), fileNameSuffix));
            return documentToUpload;
        } else {
            return null;
        }
    }

    private void creerEtRecupererAnnexeSignal(List<RapportTransit> listeRapportTransits) {
        File resultFileRootFolder = new File(attributionEquipementConnecte.getResultFilesFolderPath());
        List<Path> pieceJointes = new ArrayList<>();

        // traitement specifique aux pdf EQUIPEMENT_SIGNAL_CARDIQUE
        // ***************************
        List<Path> listePathsFichiersPdfEcg = fileUtils.getMatchingFilesFromFileNameFilterInDirectory(resultFileRootFolder,
                (dir, fileName) -> EnumFormatRapportTransit.PDF.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName)));

        listePathsFichiersPdfEcg.forEach(file -> {
            RapportTransit Doc = creerDocumentPdfSignal(file.toFile(), "_" + idDossierTransit);
            if (Doc != null) {
                Doc.setUserCreationId(ClientContextManager._SYSTEM_USER_ID);
                ajouterAttributsRapportTransit(Doc);
                listeRapportTransits.add(archivageDocumentService.createRapportTransit(Doc));
            }
        });

        File resultFileFolder = new File(resultFileRootFolder.toPath().resolve("origin").toString());
        pieceJointes.addAll(fileUtils.getMatchingFilesFromFileNameFilterInDirectory(resultFileFolder,
                (dir, fileName) -> EnumFormatRapportTransit.XML.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName))));
        File anonymizedResultFileFolder = new File(resultFileRootFolder.toPath().resolve("anonymized").toString());
        List<Path> listPath = fileUtils.getMatchingFilesFromFileNameFilterInDirectory(anonymizedResultFileFolder,
                (dir, fileName) -> EnumFormatRapportTransit.XML.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName)));
        listPath.forEach(file -> changerNomFichierAnonyme(anonymizedResultFileFolder, file));
        pieceJointes.addAll(fileUtils.getMatchingFilesFromFileNameFilterInDirectory(anonymizedResultFileFolder,
                (dir, fileName) -> EnumFormatRapportTransit.XML.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName))));
        pieceJointes.forEach(file -> {
            RapportTransit Doc = RapportTransitFactory.buildRapportTransit(file.toFile(), EnumTypeRapport.PARACLINIQUE, "_" + idDossierTransit);
            Doc.setUserCreationId(ClientContextManager._SYSTEM_USER_ID);
            ajouterAttributsRapportTransit(Doc);
            listeRapportTransits.add(archivageDocumentService.createRapportTransit(Doc));
        });
    }

    private void changerNomFichierAnonyme(File anonymizedResultFileFolder, Path file) {
        try {
            Files.move(file, anonymizedResultFileFolder.toPath().resolve("anonyme_" + file.getFileName()));
        } catch (IOException e) {
            LOGGER.warn(ERREUR_RECUPERATION_ANNEXE_MSG, e);
            clearConsoleAndAddMessage(ERREUR_ANNEXES_LIBELLE, ERREUR_RECUPERATION_ANNEXE_MSG);
        }
    }

    private void creerEtRecupererAnnexeAudio(List<RapportTransit> listeRapportTransits) throws IOException {
        try (Stream<Path> pathStream = Files.walk(Paths.get(attributionEquipementConnecte.getResultFilesFolderPath()), 1)) {
            List<Path> listeFichiers = pathStream.filter(path -> !Files.isDirectory(path)).collect(Collectors.toList());
            listeFichiers
                    .forEach(fichier -> {
                        RapportTransit Doc = RapportTransitFactory.buildRapportTransit(fichier.toFile(), EnumTypeRapport.PARACLINIQUE,
                                "_" + idDossierTransit);
                        Doc.setUserCreationId(ClientContextManager._SYSTEM_USER_ID);
                        ajouterAttributsRapportTransit(Doc);
                        listeRapportTransits.add(archivageDocumentService.createRapportTransit(Doc));
                    });
        }
    }

    private void stockerAnnexe(DocumentAnnexe document) {
        AnnexeOperation pj = new AnnexeOperation(document.getId(), operationTransitSelectionnee.get().getId());
        operationTransitFinaliseeService.createAnnexeOperation(pj);
        operationTransitSelectionnee.get().getListePiecesJointes().add(document);
    }

    private void tracerLancementOperation() {
        TraceVo traceVo = new TraceVo();
        traceVo.setTrace(EnumRole.REALISER_EXAMEN_PARACLINIQUE, EnumTerminal.EXAMEN_PARACLINIQUE,
                getComplementEnumTerminalExamentexte2(getEnumTerminalOfExamentexte2()),
                EnumActionTerminal.LANCER_EXAMEN,
                EnumTypeRessource.FAP,
                idDossierTransit.toString());
        serviceTrace.create(traceVo);
    }

    @FXML
    public void lancerOperationTransit() throws InterruptedException {
        tracerLancementOperation();
        if (gestionnaireAutorisationPhase.checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction())) {
            lancerOperationAvecVerificationEquipement();
        } else {
            afficherErreurNonAutorise(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_REALISATION_EXAMEN.getLibelle());
        }
    }

    private void lancerOperationAvecVerificationEquipement() throws InterruptedException {
        List<ValidationError> errors = new ArrayList<>();
        if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
            errors = examentexte2Service
                    .verifyDataInAffectMatConnecteForCommandLineAndPathsOfPatientFileAndResultFileFolders(attributionEquipementConnecte);
        }
        if (errors.isEmpty()) {
            lancerOperationPrincipale();
        } else {
            clearAddListOfValidationErrorAndShowConsole(errors);
        }
    }

    private void lancerOperationPrincipale() throws InterruptedException {
        try {
            if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
                reinitialiserLancementOperation();
            }
            genererFichePassage();
        } catch (MaterielConnectedAquisitionFileNotFoundException e) {
            LOGGER.warn(MODULE_CIBLE_NON_INSTALLE_MAL_CONFIGURE, e);
            clearConsoleAndAddMessage(LANCER_OPERATION, MODULE_CIBLE_NON_INSTALLE_MAL_CONFIGURE);
        }
        if (estOperationConnectee.get() && attributionEquipementConnecte.getCommandLine() != null) {
            lancerOperationNormaleOuTest();
        }
    }

    private void genererFichePassage() {
        FichePassageCritere fichePatientCriteria = genererFichePassageCriteres();
        operationTransitFinaliseeService.genererFichePassage(fichePatientCriteria);
    }

    private void lancerOperationNormaleOuTest() throws InterruptedException {
        if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
            lancerOperationNormale();
        } else {
            definirModeTerminalRecuperationOk();
        }
    }

    private void lancerOperationNormale() throws InterruptedException {
        File dossierFichierPatient = obtenirDossierFichePassage(attributionEquipementConnecte.getPatientFileFolderPath());
        if (null != dossierFichierPatient.list() && dossierFichierPatient.list().length > 0) {
            try {
                lancerModule();
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
                clearConsoleAndAddMessage(ERROR_LIGNE_COMMANDE_LBL, ERREUR_LIGNE_COMMANDE_MSG);
            }
        } else {
            clearConsoleAndAddMessage(ERREUR_FICHE_PASSAGE_LIBELLE, ERREUR_FICHE_PASSAGE_MSG);
        }
    }

    private void afficherErreurNonAutorise(final String message) {
        clearConsoleAndAddMessage("Habilitation", message);
    }

    private void lancerModule() throws InterruptedException, IOException {
        lanceurModuleService.launchApplication(attributionEquipementConnecte.getCommandLine(), EnumFormatRapportTransit.EXE.getExtension());
        String resultFilesFolderPath = attributionEquipementConnecte.getResultFilesFolderPath();
        File dossierResultats = new File(resultFilesFolderPath);
        if (dossierResultats.list().length > 0) {
            definirModeTerminalRecuperationOk();
            operationEnEdition = tableauOperationsFinalisees.getSelectionModel().getSelectedItem();
        } else {
            clearConsoleAndAddMessage(ERREUR_CONTENEUR_RESULTATS_LIBELLE, ERREUR_CONTENEUR_RESULTATS_MSG);
        }
    }

    private FichePassageCritere genererFichePassageCriteres() {
        final FichePassageCritere fichePatientCriteria = new FichePassageCritere();
        final Consultant consultant = passagerService.getConsultantById(super.idPassager);
        idPassagerStr = consultant != null ? consultant.getId().toString() : null;
        fichePatientCriteria.setConsultant(consultant);
        fichePatientCriteria.setOperateur(contexteSession.getConnectedUser());
        fichePatientCriteria.setFapDataRdv(fichePassageData);
        fichePatientCriteria.setMateriel(attributionEquipementConnecte);
        fichePatientCriteria.setCesExecution(dossierTransitAffichagePropriete.getValue().getEntite());
        renseignerFichePassageParFormatStandard(fichePatientCriteria);
        return fichePatientCriteria;
    }

    private void renseignerFichePassageParFormatStandard(final FichePassageCritere fichePatientCriteria) {
        if (fichePatientCriteria.isFormatEchangeMaterielHl7()) {
            renseignerParCodeSignal(fichePatientCriteria);
        } else {
            renseignerDonneesPassage(fichePatientCriteria);
        }
    }

    private void renseignerDonneesPassage(final FichePassageCritere fichePatientCriteria) {
        if (configurationModeleEcran != null) {
            Optional<SignalTransitConfiguration> optionalPoidsTailleConfiguration = configurationModeleEcran
                    .getBlocTerminals().stream()
                    .filter(configBloc -> EnumBlocTerminalDynamiqueAutres.PARACLINIQUE_SIGNAL_OBSERVES
                            .equals(configBloc.getBlocEcranDynamique()))
                    .map(SignalTransitConfiguration.class::cast).findFirst();
            if (optionalPoidsTailleConfiguration.isPresent()) {
                RegleContext contextInteraction = getRegleContexttexte2();
                if (optionalPoidsTailleConfiguration.get().getPoids() != null && contextInteraction.getSignalObserveContext()
                        .containsKey(optionalPoidsTailleConfiguration.get().getPoids().getLibelleCourt())) {
                    String poidsValue = contextInteraction.getSignalObserveContext()
                            .get(optionalPoidsTailleConfiguration.get().getPoids().getLibelleCourt());
                    if (poidsValue != null) {
                        fichePatientCriteria.setPoids(Double.parseDouble(poidsValue));
                    }
                }
                if (optionalPoidsTailleConfiguration.get().getTaille() != null && contextInteraction.getSignalObserveContext()
                        .containsKey(optionalPoidsTailleConfiguration.get().getTaille().getLibelleCourt())) {
                    String tailleValue = contextInteraction.getSignalObserveContext()
                            .get(optionalPoidsTailleConfiguration.get().getTaille().getLibelleCourt());
                    if (tailleValue != null) {
                        fichePatientCriteria.setTaille(Integer.parseInt(tailleValue));
                    }
                }
            }
        }
    }

    private void renseignerParCodeSignal(final FichePassageCritere fichePatientCriteria) {
        RegleContext contextInteraction = getRegleContexttexte2();
        String poidsValue = rechercherValeurSignalParCode(contextInteraction, EnumCodeSignalStandard.SIGNAL_OBSERVE_POIDS_CODE_LOINC);
        if (poidsValue != null) {
            fichePatientCriteria.setPoids(Double.parseDouble(poidsValue));
        }
        String tailleValue = rechercherValeurSignalParCode(contextInteraction, EnumCodeSignalStandard.SIGNAL_OBSERVE_TAILLE_CODE_LOINC);
        if (tailleValue != null) {
            fichePatientCriteria.setTaille(Integer.parseInt(tailleValue));
        }
        // La valeur du constat est oui/non ou vide
        String fumeur = rechercherValeurSignalParCode(contextInteraction, EnumCodeSignalStandard.SIGNAL_OBSERVE_FUMER_CODE_LOINC);
        fichePatientCriteria.setFumeur(EnumOuiNonIndetermineEnCours.toBoolean(fumeur));
        // La valeur du constat est oui/non ou vide
        String ancienFumeur = rechercherValeurSignalParCode(contextInteraction, EnumCodeSignalStandard.SIGNAL_OBSERVE_ANCIEN_FUMEUR_CODE_LOINC);
        fichePatientCriteria.setAncienFumeur(EnumOuiNonIndetermineEnCours.toBoolean(ancienFumeur));
    }

    private String rechercherValeurSignalParCode(final RegleContext contextInteraction, final EnumCodeSignalStandard loinc) {
        return Optional.ofNullable(operationRealiseeDonneesFinalisees)
                .map(DonneesFinalisees::getSignalObserveValides)
                .stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(SignalObserveValideVo::getElementSignal)
                .filter(Objects::nonNull)
                .map(ElementSignal::getSignalObserve)
                .filter(Objects::nonNull)
                .filter(constat -> loinc != null
                        && loinc.getCodeLoinc().equals(constat.getCodeLOINC())
                        && contextInteraction.getSignalObserveContext().containsKey(constat.getLibelleCourt()))
                .map(constat -> contextInteraction.getSignalObserveContext().get(constat.getLibelleCourt()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private File obtenirDossierFichePassage(String folderPath) {
        return new File(folderPath);
    }

    private void definirModeTerminalRecuperationOk() {
        boutonAnnulerOperation.setWrapText(true);
        boutonAnnulerOperation.setText(LIBELLE_ANNULER_OPERATION);
        definirDesactive(boutonAnnulerOperation, false);
        definirDesactive(boutonRecupererResultats,
                !gestionnaireAutorisationPhase.checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction()));

        Stream.of(boutonModifierOperation, boutonSupprimerOperation, boutonImprimerOperation, boutonSynthese, boutonLancerOperation)
                .forEach(btn -> btn.disableProperty().unbind());
        Stream.of(boutonNouvelleOperation, boutonEnregistrerOperation, boutonSupprimerOperation, boutonImprimerOperation, boutonModifierOperation,
                boutonSynthese, boutonLancerOperation, boutonAjouterAnnexeManuelle).forEach(btn -> definirDesactive(btn, true));

        definirModeSignauxObligatoires(ControllerMode.EDITION);

        estLancementModule = true;
        moduleDynamiqueImpl.changeAsterixToShowValue(true);
    }

    private void definirModeSignauxObligatoires(ControllerMode controllerMode) {
        if (composantOperationProprietesParIdElement != null) {
            List<ElementSignal> elementsSignalAvailable = getAvailableElementSignalListFromFullList(elementsSignal);
            elementsSignalAvailable.stream()
                    .filter(eltSignalObserve -> Boolean.TRUE.equals(eltSignalObserve.getIsObligatoireAvantAcquisition()))
                    .forEach(eltSignalObserve -> composantOperationProprietesParIdElement.get(eltSignalObserve.getId()).setMode(controllerMode));
        }
    }

    private OperationTransitEquipement obtenirInfoEquipementDepuisAttribution(List<AttributionEquipement> lsAffectation) {
        return new OperationTransitEquipementBuilder()
                .withIdZoneTransit(zoneTransit.getId())
                .withNomSale(zoneTransit.getNom())
                .withListAttributionEquipement(lsAffectation).build();
    }

    private void recupererConfigurationTerminal() {
        ConfigurationTerminalVo configEcranVo = phaseTransitAffichage.getInteraction().getConfigurationEcrans().get(0);
        configurationModeleEcran = operationService.getConfigurationModeleEcranById(configEcranVo.getId());
        creerGrilleDynamiqueDepuisConfiguration(EnumBlocTerminalGrilleDynamique.SIGNAL_OBSERVES_MESURES);

        estModeLecture = blocTerminalDynamiqueContientElements(EnumBlocTerminalGrilleDynamique.SIGNAL_OBSERVES_LECTURE);
        if (estModeLecture) {
            creerGrilleDynamiqueDepuisConfiguration(EnumBlocTerminalGrilleDynamique.SIGNAL_OBSERVES_LECTURE);
        } else {
            creerGrilleDynamiqueDepuisConfiguration(EnumBlocTerminalGrilleDynamique.SIGNAL_OBSERVES_INTERPRETATION);
        }
        loadRules();
    }

    private boolean blocTerminalDynamiqueContientElements(EnumBlocTerminalGrilleDynamique blocEcranDyn) {
        List<BlocTerminal> grilles = configurationModeleEcran.getListOfBlocTerminalFromBlocEcranDynamiqueGrilleDynamiqueTerminal(blocEcranDyn);
        return grilles.stream().anyMatch(grille -> CollectionUtils.isNotEmpty(((GrilleDynamiqueTerminal) grille).getElements()));
    }

    private void creerGrilleDynamiqueDepuisConfiguration(EnumBlocTerminalGrilleDynamique blocEcranDyn) {
        List<BlocTerminal> grilles = configurationModeleEcran.getListOfBlocTerminalFromBlocEcranDynamiqueGrilleDynamiqueTerminal(blocEcranDyn);
        grilles.forEach(grille -> createGrilleDynamiqueTerminalFromBlocTerminalList(blocEcranDyn, grille));
    }

    protected void genererGrilleSyntheseDynamique() {
        obtenirElementsSignalDepuisGrilleSynthese();
        composantSyntheseProprietesParIdElement = moduleDynamiqueImpl
                .generateGrilleDynamiqueTerminalIntoGridPane(grilleSyntheseDynamique, grilleSynthese, getPrimaryStage().getScene());
        appliquerFusionColonnesGrilleSynthese();
        moduleDynamiqueImpl.switchModeTo(ControllerMode.CONSULTATION, composantSyntheseProprietesParIdElement);
        obtenirLibelleEtRegleAffichage(grilleSyntheseDynamique, composantSyntheseProprietesParIdElement);
        estEcouteurActivePropriete.set(true);
        initElemCstPropValChangeListener(elementsSignalInterpretation, composantSyntheseProprietesParIdElement);
    }

    private void obtenirElementsSignalDepuisGrilleSynthese() {
        elementsSignalInterpretation.setValue(grilleSyntheseDynamique.getElements().stream()
                .filter(ElementSignal.class::isInstance).map(ElementSignal.class::cast)
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
        elementsSignalSyntheseEquipement = elementsSignalInterpretation.stream()
                .filter(ecm -> ecm.getSignalObserve().getAppareilConnecte() != null)
                .collect(Collectors.toList());
    }

    private boolean verifierDisponibiliteTailleMaxSignal(List<ElementSignal> elementsSignal, ElementSignal elementSignalObserve) {
        int col = elementSignalObserve.getNumeroColonne();
        int row = elementSignalObserve.getNumeroLigne();
        SignalObserve constat = elementSignalObserve.getSignalObserve();

        boolean isMaxSizeExpected = EnumListeFormatSignal.QUALITATIF_TEXTE_LIBRE == constat.getFormResult()
                && constat.getMotifNonRealise() == null && col < 3;
        if (isMaxSizeExpected) {
            Optional<ElementSignal> nextSignalObserve = elementsSignal.stream()
                    .filter(elemSignalObserve -> col < elemSignalObserve.getNumeroColonne() && row == elemSignalObserve.getNumeroLigne()).findFirst();
            isMaxSizeExpected = !nextSignalObserve.isPresent();
        }
        return isMaxSizeExpected;
    }

    private void appliquerFusionColonnesGrilleOperations() {
        elementsSignal.forEach(elemCst -> grilleOperation.operateColumnSpanOnExamGridPaneCellsWhichContainsOneSpecificSignalObserve(elementsSignal, elemCst));
        elementsSignal.stream()
                .filter(elemCst -> verifierDisponibiliteTailleMaxSignal(elementsSignal, elemCst))
                .forEach(elemCst -> appliquerFusionColonnesGrilleSignalSpecifique(elemCst, grilleOperation));
    }

    private void appliquerFusionColonnesGrilleSynthese() {
        elementsSignalInterpretation.stream()
                .filter(elemCst -> verifierDisponibiliteTailleMaxSignal(elementsSignalInterpretation, elemCst))
                .forEach(elemCst -> appliquerFusionColonnesGrilleSignalSpecifique(elemCst, grilleSynthese));
    }

    private void appliquerFusionColonnesGrilleSignalSpecifique(ElementSignal elemSignalObserve, GridPane gridpane) {
        Integer numColCstExpected = elemSignalObserve.getNumeroColonne() + 1;
        Integer numRowCstExpected = elemSignalObserve.getNumeroLigne();
        gridpane.getChildren().stream().filter(node -> GridPane.getColumnIndex(node) != null && GridPane.getRowIndex(node) != null)
                .filter(node -> numColCstExpected.equals(GridPane.getColumnIndex(node)) && numRowCstExpected.equals(GridPane.getRowIndex(node)))
                .forEach(node -> GridPane.setColumnSpan(node, (numColCstExpected == 1 ? 3 : 2)));
    }

    protected void genererGrilleOperationDynamique() {
        composantOperationProprietesParIdElement = moduleDynamiqueImpl.generateGrilleDynamiqueTerminalIntoGridPane(
                grilleOperationDynamique, grilleOperation, getPrimaryStage().getScene());
        obtenirElementsSignalDepuisGrilleOperation();
        appliquerFusionColonnesGrilleOperations();
        obtenirLibelleEtRegleAffichage(grilleOperationDynamique, composantOperationProprietesParIdElement);
        elementsSignalEquipement.forEach(element -> composantOperationProprietesParIdElement.applyModePropertyChangeListenerOnGridPaneElement(element));
        estEcouteurActivePropriete.set(true);
        initElemCstPropValChangeListener(elementsSignal, composantOperationProprietesParIdElement);
    }

    private void obtenirElementsSignalDepuisGrilleOperation() {
        elementsSignal = grilleOperationDynamique.getElements().stream().filter(ElementSignal.class::isInstance)
                .map(ElementSignal.class::cast).collect(Collectors.toList());
        elementsSignalExamen = elementsSignal.stream()
                .filter(ecm -> ecm.getSignalObserve().getAppareilConnecte() == null).collect(Collectors.toList());
        elementsSignalEquipement = elementsSignal.stream()
                .filter(ecm -> ecm.getSignalObserve().getAppareilConnecte() != null).collect(Collectors.toList());
    }

    private void obtenirLibelleEtRegleAffichage(GrilleDynamiqueTerminal grille, GrilleTerminalUi grilleUi) {
        List<LibelleRegleTerminal> lsLibRegleAff = grille.getElements().stream()
                .filter(LibelleRegleTerminal.class::isInstance).map(LibelleRegleTerminal.class::cast)
                .collect(Collectors.toList());
        reglesAffichageProprietesComposantMap
                .putAll(lsLibRegleAff.stream().collect(Collectors.toMap(LibelleRegleTerminal::getId,
                        libelleRegleAffichage -> grilleUi.get(libelleRegleAffichage.getId()))));
        libellesRegleAffichage.addAll(lsLibRegleAff);
    }

    private void verifierUtilisableAvantExecutionRegles(final ElementSaisieGrillePropriete composantProperties, Object oldValue,
            boolean reexecuteRules) {
        if (Boolean.TRUE.equals(composantProperties.isUsable())) {
            // Execution du moteur de regle et refresh du formulaire de l'ecran
            RegleResultat reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getRegleContexttexte2());
            if (isControleValiditeResultatsRegleDeCalculOk(reglesResult)) {
                composantOperationProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                        dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
                composantSyntheseProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                        dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);

                // Si au moins un element ayant une valeur saisie a ete demodule, alors supprimer cette precedente
                // valeur
                // Si une regle de calcul existe que sa valeur est null ou 0 alors supprimer la precedente valeur
                resetElementsGrilleDynamiUDemodulesValuesInEditionMode(composantOperationProprietesParIdElement, elementsSignal, reglesResult);
                // Rafraichissement des donnees de l'ecran calculees par le moteur de regles
                refreshReglesAffichageEtSignalObserves(reglesResult);
            } else {
                composantProperties.setValue(oldValue);
            }
        } else if (!proprietesUtilisablesEcoutees.contains(composantProperties.usableProperty())) {
            composantProperties.usableProperty().addListener(obtenirProprieteEcouteurUtilisable(composantProperties, oldValue));
            proprietesUtilisablesEcoutees.add(composantProperties.usableProperty());
        }
    }

    private ChangeListener<Boolean> obtenirProprieteEcouteurUtilisable(ElementSaisieGrillePropriete composantProperties,
            Object oldValue) {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(final ObservableValue<? extends Boolean> observableUsableProperty, final Boolean oldValueUsableProperty,
                    final Boolean newValueUsableProperty) {
                if (Boolean.TRUE.equals(newValueUsableProperty)) {
                    observableUsableProperty.removeListener(this);
                    proprietesUtilisablesEcoutees.remove(observableUsableProperty);
                    RegleResultat reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getRegleContexttexte2());
                    if (isControleValiditeResultatsRegleDeCalculOk(reglesResult)) {
                        composantOperationProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                                dossierTransitAffichagePropriete.getValue(),
                                getDonneesFinaliseesInteraction(), reglesResult);
                        composantSyntheseProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                                dossierTransitAffichagePropriete.getValue(),
                                getDonneesFinaliseesInteraction(), reglesResult);
                        refreshReglesAffichageEtSignalObserves(reglesResult);
                    } else {
                        composantProperties.setValue(oldValue);
                    }
                }
            }
        };
    }

    private Consumer<DialogConfirmationController> surSelectionElementTableau(
            final OperationTransitFinalisee itemSelectionne) {
        return t -> {
            rowSelectionOperations(itemSelectionne);
            setMode(ControllerMode.CONSULTATION);
        };
    }

    @Override
    protected void rowSelectionOperations(OperationTransitFinalisee examen) {
        estEcouteurActivePropriete.set(false);
        Stream.of(composantOperationProprietesParIdElement, composantSyntheseProprietesParIdElement)
                .forEach(GrilleDynamiqueTerminalUtils::resetAllValues);
        caseCocherPreRequis.setSelected(false);
        estEcouteurActivePropriete.set(true);
        reglesAffichageProprietesComposantMap.values().forEach(egd -> egd.setVisible(Boolean.FALSE));
        if (examen != null) {
            caseCocherPreRequis.setSelected(Boolean.TRUE.equals(examen.getIsLecturePrerequise()));

            examen.getListeSignalObserveValideExamen().forEach(cstRea -> composantOperationProprietesParIdElement.putSignalObserveValideValueInGrilleDynamiqueTerminal(cstRea));
            if (examen.isInterprete() != null && Boolean.TRUE.equals(examen.isInterprete())) {
                examen.getListeSignalObserveValideInterpretation()
                        .forEach(cstRea -> composantSyntheseProprietesParIdElement.putSignalObserveValideValueInGrilleDynamiqueTerminal(cstRea));
            }

            RegleResultat reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getRegleContexttexte2());
            composantOperationProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                    dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
            composantSyntheseProprietesParIdElement.refreshWithContext(getConsultantEntite(),
                    dossierTransitAffichagePropriete.getValue(), getDonneesFinaliseesInteraction(), reglesResult);
            notEmptyRowSelectionInterpAndMatOperations(examen);
            refreshReglesAffichageEtSignalObserves(reglesResult);
        }
    }

    private void notEmptyRowSelectionInterpAndMatOperations(OperationTransitFinalisee examen) {
        if (Boolean.TRUE.equals(examen.isInterprete())) {
            examen.getListeSignalObserveValideInterpretation()
                    .forEach(cstRea -> composantSyntheseProprietesParIdElement.putSignalObserveValideValueInGrilleDynamiqueTerminal(cstRea));
        }
        if (!elementsSignalEquipement.isEmpty() && examen.getListeSignalEquipementValide() != null
                && !examen.getListeSignalEquipementValide().isEmpty()) {
            placerValeursSignalEquipementDansGrilles(elementsSignalEquipement, examen,
                    composantOperationProprietesParIdElement);
            placerValeursSignalEquipementDansGrilleSynthese(examen);
        }

        List<ElementSignal> listElementSignalsObligatoiresAvantAcquisition = obtenirListeElementsSignauxObligatoires();
        if (CollectionUtils.isNotEmpty(listElementSignalsObligatoiresAvantAcquisition)) {
            boolean auMoinsUnSignalObligatoirePostRecuperationRenseigne = listElementSignalsObligatoiresAvantAcquisition.stream()
                    .anyMatch(elementSignalObserve -> auMoinsUnSignalObligatoirePostRecuperationRenseigne(elementSignalObserve));
            moduleDynamiqueImpl.changeAsterixToShowValue(Boolean.TRUE.equals(auMoinsUnSignalObligatoirePostRecuperationRenseigne));
        }
    }

    private boolean auMoinsUnSignalObligatoirePostRecuperationRenseigne(ElementSignal elemSignalObserve) {
        SignalObserveValide constatRealise = retrieveSignalObserveValideFromElementSignal(elemSignalObserve, null, composantOperationProprietesParIdElement);
        return !constatRealise.isEmptySignalObserveValideValue();
    }

    private void placerValeursSignalEquipementDansGrilleSynthese(OperationTransitFinalisee examen) {
        if (!elementsSignalSyntheseEquipement.isEmpty() && examen.getAnalyste() == null) {
            Map<String, SignalEquipementValide> cstMaterielReaByIdIOPGeneriqueMap = new HashMap<>();
            elementsSignalSyntheseEquipement.forEach(ecm -> genererSignalEquipementParIdOuCode(ecm, examen, cstMaterielReaByIdIOPGeneriqueMap));
            if (!cstMaterielReaByIdIOPGeneriqueMap.isEmpty()) {
                elementsSignalSyntheseEquipement
                        .forEach(elemCst -> placerValeurSignalDepuisElementDansGrilleSynthese(elemCst, cstMaterielReaByIdIOPGeneriqueMap));
            }
        }
    }

    private void placerValeurSignalDepuisElementDansGrilleSynthese(ElementSignal elemCst,
            Map<String, SignalEquipementValide> cstMaterielReaByIdIOPGeneriqueMap) {
        String idIOPGenerique = estFormatEchangeStandard() ? elemCst.getSignalObserve().getCodeLOINC() : elemCst.getSignalObserve().getIdInteroperabilite();
        if (cstMaterielReaByIdIOPGeneriqueMap.containsKey(idIOPGenerique)) {
            SignalEquipementValide cstMatRea = cstMaterielReaByIdIOPGeneriqueMap.get(idIOPGenerique);
            Map<Long, ElementSaisieGrillePropriete> composantPropByElemId = new HashMap<>();
            genererComposantsParIdElement(composantPropByElemId, composantSyntheseProprietesParIdElement);
            definirValeurSignalEquipementParType(elemCst, cstMatRea, composantPropByElemId);
        }
    }

    private void genererSignalEquipementParIdOuCode(ElementSignal ecm, OperationTransitFinalisee examen,
            Map<String, SignalEquipementValide> cstMaterielReaByIdIOPOrCodeLoincMap) {
        if (estFormatEchangeStandard()) {
            String codeLoinc = ecm.getSignalObserve().getCodeLOINC();
            examen.getListeSignalEquipementValide().stream()
                    .filter(c -> null != c.getCodeLoinc() && c.getCodeLoinc().equals(codeLoinc)).findFirst()
                    .ifPresent(constatMat -> cstMaterielReaByIdIOPOrCodeLoincMap.put(codeLoinc, constatMat));
        } else {
            String idIOP = ecm.getSignalObserve().getIdInteroperabilite();
            examen.getListeSignalEquipementValide().stream()
                    .filter(c -> null != c.getIdIOP() && c.getIdIOP().equals(idIOP)).findFirst()
                    .ifPresent(constatMat -> cstMaterielReaByIdIOPOrCodeLoincMap.put(idIOP, constatMat));
        }
    }

    private void placerValeursSignalEquipementDansGrilles(List<ElementSignal> lsElementSignal,
            OperationTransitFinalisee examen, Map<Long, ElementGrilleTerminalPropriete> composants) {
        lsElementSignal.stream()
                .filter(ecm -> examen.getListeSignalEquipementValide().stream().map(SignalEquipementValide::getIdSignalDiffuse)
                        .collect(Collectors.toList()).contains(ecm.getSignalObserve().getId()))
                .forEach(ecm -> definirValeurSignalEquipement(examen, ecm, composants));
    }

    private void definirValeurSignalEquipement(OperationTransitFinalisee examen, ElementSignal ecm,
            Map<Long, ElementGrilleTerminalPropriete> composants) {
        Long idCst = ecm.getSignalObserve().getId();
        SignalEquipementValide constatMaterielResult = examen.getListeSignalEquipementValide().stream()
                .filter(cmr -> cmr.getIdSignalDiffuse().equals(idCst)).findFirst().orElse(null);
        if (constatMaterielResult != null) {
            Map<Long, ElementSaisieGrillePropriete> composantPropByElemId = new HashMap<>();
            genererComposantsParIdElement(composantPropByElemId, composants);
            definirValeurSignalEquipementParType(ecm, constatMaterielResult, composantPropByElemId);
        }
    }

    private void genererComposantsParIdElement(
            Map<Long, ElementSaisieGrillePropriete> composantPropByElemId,
            Map<Long, ElementGrilleTerminalPropriete> composants) {
        composants.keySet().stream()
                .filter(key -> composants.get(key) instanceof ElementSaisieGrillePropriete)
                .forEach(key -> composantPropByElemId.put(key, retrieveComponentByKey(key, composants)));
    }

    private void definirValeurSignalEquipementParType(ElementSignal ecm, SignalEquipementValide constatMaterielResult,
            Map<Long, ElementSaisieGrillePropriete> composantPropByElemId) {
        SignalEquipementValideVo constatMatRea = signalEquipementValideFactory
                .buildSignalEquipementValideVo(constatMaterielResult);
        constatMatRea.setSignalObserveFormatAndSignalObserveNombreDecimalWithSignalObserveData(ecm.getSignalObserve());
        composantPropByElemId.get(ecm.getId()).setValue(SignalObserveValideHelper.getSignalEquipementValideResultatStringValue(constatMatRea));
    }

    private ElementSaisieGrillePropriete retrieveComponentByKey(long key, Map<Long, ElementGrilleTerminalPropriete> composants) {
        return (ElementSaisieGrillePropriete) composants.get(key);
    }

    private Consumer<DialogConfirmationController> surAnnulationBoiteConfirmation(final OperationTransitFinalisee oldOperationTransitFinalisee) {
        return t -> Platform.runLater(() -> {
            tableauOperationsFinalisees.resetSelectionModelValue(oldOperationTransitFinalisee);
        });
    }

    private boolean modificationDansGrilleDynamique() {
        List<SignalObserveValide> lsModifiedSignalObserveRea = obtenirListeOperationsSignaux();
        boolean isEquals = true;
        if (operationEnEdition != null) {
            isEquals = SignalObserveValideHelper.isModifIntoSignalObserveExamenDataCase(isEquals, operationEnEdition, lsModifiedSignalObserveRea);
        } else {
            isEquals = SignalObserveValideHelper.areEmptySignalObserveReaValues(isEquals, lsModifiedSignalObserveRea);
        }
        return !isEquals;
    }

    private boolean modificationDansSyntheseDynamique() {
        List<SignalObserveValide> lsModifiedSignalObserveRea = obtenirListeSyntheseSignaux(operationEnEdition.getId(), !estInsertionSynthese());
        boolean isEqual = true;
        if (operationEnEdition != null && operationEnEdition.isInterprete()) {
            isEqual = SignalObserveValideHelper.isModifIntoSignalObserveInterpretationDataCase(isEqual, operationEnEdition, lsModifiedSignalObserveRea);
        } else if (operationEnEdition != null && !elementsSignalSyntheseEquipement.isEmpty() && operationEnEdition.getAnalyste() == null) {
            isEqual = modificationDansSyntheseInitEtEquipement(isEqual, lsModifiedSignalObserveRea);
        } else if (operationEnEdition != null && !elementsSignalSyntheseEquipement.isEmpty()
                && operationEnEdition.getListeSignalObserveValideInterpretation() != null) {
            isEqual = SignalObserveValideHelper.isModifIntoSignalObserveInterpretationDataCase(isEqual, operationEnEdition, lsModifiedSignalObserveRea);
        } else {
            isEqual = SignalObserveValideHelper.areEmptySignalObserveReaValues(isEqual, lsModifiedSignalObserveRea);
        }
        return !isEqual;
    }

    private boolean modificationDansSyntheseInitEtEquipement(Boolean isEqual,
            List<SignalObserveValide> lsModifiedSignalObserveRea) {
        List<SignalEquipementValide> lsSignalObserveMateriel = operationEnEdition.getListeSignalEquipementValide();
        List<SignalObserveValide> lsModifiedSignalObserveReaBindedToMateriel = lsModifiedSignalObserveRea.stream()
                .filter(cst -> elementsSignalSyntheseEquipement.contains(cst.getElementSignal())).collect(Collectors.toList());
        sontEgauxGenerique = true;
        lsModifiedSignalObserveReaBindedToMateriel
                .forEach(cstRea -> signauxEquipementEtValidesEgaux(cstRea, lsSignalObserveMateriel));
        List<SignalObserveValide> lsModifiedNotBindedToMat = lsModifiedSignalObserveRea.stream()
                .filter(cst -> !elementsSignalSyntheseEquipement.contains(cst.getElementSignal())).collect(Collectors.toList());
        if (isEqual != null) {
            return sontEgauxGenerique && SignalObserveValideHelper.areEmptySignalObserveReaValues(isEqual, lsModifiedNotBindedToMat);
        } else {
            return sontEgauxGenerique;
        }
    }

    private boolean syntheseDonneesVides(List<SignalObserveValide> lsModifiedSignalObserveRea) {
        List<SignalObserveValide> lsModifiedSignalObserveInterp = lsModifiedSignalObserveRea.stream()
                .filter(cst -> elementsSignalInterpretation.contains(cst.getElementSignal()))
                .collect(Collectors.toList());
        boolean isEquals = true;
        return SignalObserveValideHelper.areEmptySignalObserveReaValues(isEquals, lsModifiedSignalObserveInterp);
    }

    private void signauxEquipementEtValidesEgaux(SignalObserveValide constatRealise, List<SignalEquipementValide> lsSignalEquipementValide) {
        ElementSignal elementSignalObserve = constatRealise.getElementSignal();
        lsSignalEquipementValide.stream()
                .filter(cm -> cm.getIdSignalDiffuse().equals(elementSignalObserve.getSignalObserve().getId())).findFirst()
                .ifPresent(beforeModifVal -> signauxEquipementEtValidesEgauxParType(beforeModifVal, constatRealise));
    }

    private void signauxEquipementEtValidesEgauxParType(SignalEquipementValide beforeModifVal, SignalObserveValide constatRealise) {
        ElementSignal elementSignalObserve = constatRealise.getElementSignal();
        if (estFormatEchangeStandard()) {
            if (EnumListeFormatSignal.getEnumListeFormatSignalNumeric().contains(elementSignalObserve.getSignalObserve().getFormResult())) {
                sontEgauxGenerique = sontEgauxGenerique
                        && ((beforeModifVal.getNumericValue() == null && constatRealise.getNumericValue() == null)
                                || (beforeModifVal.getNumericValue().equals(constatRealise.getNumericValue())));
            } else {
                sontEgauxGenerique = sontEgauxGenerique && Strings.nullToEmpty(beforeModifVal.getTextValue())
                        .equals(Strings.nullToEmpty(constatRealise.getTextValue()));
            }
        } else {
            String idInteroperabilite = elementSignalObserve.getSignalObserve().getIdInteroperabilite();
            if (idInteroperabilite != null && EnumSignalRespiratoireStandard.containsCode(idInteroperabilite)) {
                sontEgauxGenerique = SignalObserveValideHelper.areEqualSignalObserveMaterielSpiroCase(sontEgauxGenerique, beforeModifVal, constatRealise, elementSignalObserve);
            } else if (idInteroperabilite != null && EnumSignalCardiaqueStandard.containsCode(idInteroperabilite)) {
                sontEgauxGenerique = SignalObserveValideHelper.areEqualSignalObserveMaterielEQUIPEMENT_SIGNAL_CARDIQUECase(sontEgauxGenerique, beforeModifVal, constatRealise, elementSignalObserve);
            } else {
                sontEgauxGenerique = sontEgauxGenerique
                        && ((beforeModifVal.getNumericValue() == null && constatRealise.getNumericValue() == null)
                                || (beforeModifVal.getNumericValue().equals(constatRealise.getNumericValue())));
            }
        }
    }

    @Override
    protected RegleContext getRegleContexttexte2() {
        final RegleContext moteurRegleContext = getRegleContext();
        initializeMoteurDeRegleContextFromLsElementsSignalObserve(moteurRegleContext);
        return moteurRegleContext;
    }

    private void initializeMoteurDeRegleContextFromLsElementsSignalObserve(RegleContext moteurRegleContext) {
        List<ElementSignal> elementsSignalMatInterp = new ArrayList<>();
        List<ElementSignal> elementsSignalBasiqInterp = new ArrayList<>();
        if (!elementsSignalInterpretation.isEmpty()) {
            elementsSignalMatInterp = elementsSignalInterpretation.stream()
                    .filter(ec -> ec.getSignalObserve().getAppareilConnecte() != null).collect(Collectors.toList());
            elementsSignalBasiqInterp = elementsSignalInterpretation.stream()
                    .filter(ec -> ec.getSignalObserve().getAppareilConnecte() == null).collect(Collectors.toList());
        }

        initSignalObserveValideIntoRegleContext(moteurRegleContext, elementsSignalBasiqInterp);

        initSignalEquipementValideIntoRegleContext(moteurRegleContext, elementsSignalMatInterp);
    }

    private void initSignalEquipementValideIntoRegleContext(RegleContext moteurRegleContext,
            List<ElementSignal> elementsSignalMatInterp) {
        List<SignalEquipementValide> lsSignalEquipementValides = new ArrayList<>();
        List<ElementSignal> lsElemSignalObserveMateriel = new ArrayList<>();
        if (!elementsSignalEquipement.isEmpty()) {
            lsSignalEquipementValides.addAll(getListeSignalEquipementValideExamen());
            lsElemSignalObserveMateriel.addAll(elementsSignalEquipement);
        }

        if (!elementsSignalMatInterp.isEmpty()) {
            lsSignalEquipementValides.addAll(getListeSignalEquipementValideInterpretation(elementsSignalMatInterp));
            lsElemSignalObserveMateriel.addAll(elementsSignalMatInterp);
        }
        if (!lsElemSignalObserveMateriel.isEmpty()) {
            List<SignalEquipementValideVo> lsSignalEquipementValidesVO = lsSignalEquipementValides
                    .stream().map(cmr -> signalEquipementValideFactory.buildSignalEquipementValideVo(cmr, lsElemSignalObserveMateriel))
                    .collect(Collectors.toList());
            moteurRegleContext.putSignalEquipementValides(lsSignalEquipementValidesVO);
        }
    }

    private void initSignalObserveValideIntoRegleContext(RegleContext moteurRegleContext,
            List<ElementSignal> elementsSignalBasiqInterpretationOuLecture) {
        List<SignalObserveValide> lsSignalObserveValides = new ArrayList<>();

        if (!elementsSignalExamen.isEmpty()) {
            lsSignalObserveValides.addAll(getListSignalObserveValideExamen());
        }

        if (!elementsSignalBasiqInterpretationOuLecture.isEmpty()) {
            lsSignalObserveValides.addAll(getListSignalObserveValideInterpretationOuLecture(elementsSignalBasiqInterpretationOuLecture));
        }

        if (!lsSignalObserveValides.isEmpty()) {
            moteurRegleContext.putSignalObserveValides(lsSignalObserveValides);
        }
    }

    private List<SignalEquipementValide> getListeSignalEquipementValideInterpretation(
            List<ElementSignal> lsElemSignalObserveMatInterp) {
        final List<SignalEquipementValide> listeSignalEquipementValide = new ArrayList<>();

        if (operationTransitSelectionnee.isNotNull().get()) {
            List<SignalEquipementValide> lsCstMatRea = operationTransitSelectionnee.get().getListeSignalEquipementValide();

            if (CollectionUtils.isEmpty(lsCstMatRea)) {
                listeSignalEquipementValide.addAll(generateSignalObserveMaterielInterpRealise(lsElemSignalObserveMatInterp));
            } else {
                if (estFormatEchangeStandard()) {
                    List<String> lsCodeLoinc = lsElemSignalObserveMatInterp.stream()
                            .map(ecm -> ecm.getSignalObserve().getCodeLOINC())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    listeSignalEquipementValide
                            .addAll(lsCstMatRea.stream().filter(cstMat -> lsCodeLoinc.contains(cstMat.getCodeLoinc())).collect(Collectors.toList()));
                } else {

                    List<String> lsIdIOP = lsElemSignalObserveMatInterp.stream()
                            .map(ecm -> ecm.getSignalObserve().getIdInteroperabilite())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    listeSignalEquipementValide.addAll(lsCstMatRea.stream().filter(cstMat -> lsIdIOP.contains(cstMat.getIdIOP())).collect(Collectors.toList()));
                }
            }
        } else {
            listeSignalEquipementValide.addAll(generateSignalObserveMaterielInterpRealise(lsElemSignalObserveMatInterp));
        }

        return listeSignalEquipementValide;
    }

    private List<SignalEquipementValide> getListeSignalEquipementValideExamen() {
        final List<SignalEquipementValide> listeSignalEquipementValide = new ArrayList<>();
        if (operationTransitSelectionnee.isNotNull().get() && operationTransitSelectionnee.get().getListeSignalEquipementValide() != null) {
            List<Long> lsIdElementsSignalObserveMat = elementsSignalEquipement.stream().map(ec -> ec.getSignalObserve().getId())
                    .collect(Collectors.toList());
            List<SignalEquipementValide> lsCstMatRea = operationTransitSelectionnee.get().getListeSignalEquipementValide()
                    .stream().filter(cmr -> lsIdElementsSignalObserveMat.contains(cmr.getIdSignalDiffuse()))
                    .collect(Collectors.toList());
            listeSignalEquipementValide.addAll(!lsCstMatRea.isEmpty() ? lsCstMatRea : generateSignalEquipementValide());
        } else {
            listeSignalEquipementValide.addAll(generateSignalEquipementValide());
        }
        return listeSignalEquipementValide;
    }

    private List<SignalEquipementValide> generateSignalEquipementValide() {
        return elementsSignalEquipement.stream()
                .map(elementSignalObserve -> composantOperationProprietesParIdElement.generateSignalEquipementValideFromElementSignal(elementSignalObserve))
                .collect(Collectors.toList());
    }

    private List<SignalEquipementValide> generateSignalObserveMaterielInterpRealise(
            List<ElementSignal> lsElemSignalObserveMatInterp) {
        return lsElemSignalObserveMatInterp.stream()
                .map(elementSignalObserve -> composantSyntheseProprietesParIdElement.generateSignalEquipementValideFromElementSignal(elementSignalObserve))
                .collect(Collectors.toList());
    }

    private List<SignalObserveValide> getListSignalObserveValideInterpretationOuLecture(List<ElementSignal> lsElementSignalInterpretation) {
        return lsElementSignalInterpretation.stream().map(elementSignalObserve -> retrieveSignalObserveValide(elementSignalObserve, true)).collect(Collectors.toList());
    }

    protected SignalObserveValide retrieveSignalObserveValide(final ElementSignal elementSignalObserve, boolean isInterpretationOuLecture) {
        final SignalObserveValide constatRealise;
        final ElementSignalPropriete composantProperties;
        constatRealise = generateCstReaFromGrilleType(elementSignalObserve, isInterpretationOuLecture);
        composantProperties = isInterpretationOuLecture
                ? (ElementSignalPropriete) composantSyntheseProprietesParIdElement
                        .get(elementSignalObserve.getId())
                : (ElementSignalPropriete) composantOperationProprietesParIdElement.get(elementSignalObserve.getId());
        final SignalObserveValide constatRealiseInGrid = elementSignalFactory
                .buildSignalObserveValideFromSignalObserveElementSignalPropriete(constatRealise.getId(), elementSignalObserve, composantProperties);
        constatRealiseInGrid.setIdPrestationRealisee(constatRealise.getIdPrestationRealisee());
        constatRealiseInGrid.setElementSignal(constatRealise.getElementSignal());
        return constatRealiseInGrid;
    }

    @Override
    protected GrilleTerminalUi getComposantExamenUi() {
        return composantOperationProprietesParIdElement;
    }

    @Override
    protected GrilleTerminalUi getComposantInterpretationOrLectureUi() {
        return composantSyntheseProprietesParIdElement;
    }

    @Override
    protected boolean isEditedExam(OperationTransitFinalisee examen) {
        return examen != null && examen.equals(operationEnEdition);
    }

    @Override
    protected Long getDisplayedPrestationRealiseeId() {
        return displayedPrestationRealisee.getId();
    }

    @Override
    protected SignalObserveValide buildSignalObserveValide(Long prestationId, ElementSignal elementSignalObserve) {
        return signalObserveValideFactory.buildSignalObserveValide(prestationId, elementSignalObserve);
    }

    @Override
    protected SignalObserveValide buildSignalObserveValide(SignalObserveValide constatRealise) {
        return signalObserveValideFactory.buildSignalObserveValide(constatRealise);
    }

    @Override
    protected boolean estLancementModule() {
        return estLancementModule;
    }

    public void refreshSignalObserveValide(final SignalObserveValide constatRealise) {
        SignalObserveValideVo constatRealiseWithOrigineInfos = SignalObserveValideVoFactory
                .addOrigineInformationsToSignalObserveValide(constatRealise, getDisplayedInteractionRealiseeInteraction().getLibelleCourt(),
                        OperateurVoFactory.buildOperateurVo(contexteSession.getConnectedUser()));
        final List<SignalObserveValideVo> constatRealises = operationRealiseeDonneesFinalisees.getSignalObserveValides();
        final Predicate<SignalObserveValide> predicate = constatRea -> constatRea.getElementSignal().getSignalObserve().getId()
                .equals(constatRealise.getElementSignal().getSignalObserve().getId());
        final Optional<SignalObserveValideVo> optionalSignalObserveValide = constatRealises.stream().filter(predicate).findFirst();
        if (optionalSignalObserveValide.isPresent()) {
            final SignalObserveValide constatRealise2 = optionalSignalObserveValide.get();
            constatRealise.setId(constatRealise2.getId());
            constatRealises.set(constatRealises.indexOf(constatRealise2), constatRealiseWithOrigineInfos);
        } else {
            constatRealises.add(constatRealiseWithOrigineInfos);
        }
    }

    @Override
    protected void refreshSignalObservesWithRegleResultat(final RegleResultat moteurRegleResult) {
        if (!elementsSignal.isEmpty()) {
            elementsSignal.forEach(elementSignalObserve -> composantOperationProprietesParIdElement.setSignalObserveScore(elementSignalObserve, moteurRegleResult));
        }
        if (!elementsSignalInterpretation.isEmpty()) {
            elementsSignalInterpretation
                    .forEach(elementSignalObserve -> composantSyntheseProprietesParIdElement.setSignalObserveScore(elementSignalObserve, moteurRegleResult));
        }
    }

    @Override
    protected void refreshLibellesReglesAffichage(final RegleResultat moteurRegleResult) {
        reglesAffichageProprietesComposantMap.entrySet()
                .forEach(nodeRegleAffichage -> definirReglesAffichageVisibilite(moteurRegleResult, nodeRegleAffichage));
    }

    @Override
    protected void surSelectionLignePreRafraichissement(OperationTransitFinalisee examen) {
        caseCocherPreRequis.setSelected(Boolean.TRUE.equals(examen.getIsLecturePrerequise()));
    }

    private void definirReglesAffichageVisibilite(final RegleResultat moteurRegleResult,
            final Entry<Long, ElementGrilleTerminalPropriete> nodeRegleAffichage) {
        final Predicate<? super LibelleRegleTerminal> predicateLibelleRegleTerminal = libelleRegleAffichage -> libelleRegleAffichage
                .getId().equals(nodeRegleAffichage.getKey());
        final Optional<LibelleRegleTerminal> optionalLibelleRegleTerminal = libellesRegleAffichage.stream()
                .filter(predicateLibelleRegleTerminal).findFirst();
        optionalLibelleRegleTerminal.ifPresent(libelleRegleAffichage -> definirVisibiliteLibelleRegleAffichage(moteurRegleResult, nodeRegleAffichage,
                libelleRegleAffichage));
    }

    private void definirVisibiliteLibelleRegleAffichage(final RegleResultat moteurRegleResult,
            final Entry<Long, ElementGrilleTerminalPropriete> nodeRegleAffichage,
            final LibelleRegleTerminal libelleRegleAffichage) {
        final String libelleCourt = libelleRegleAffichage.getRegleAffichage().getLibelleCourt();
        final Boolean booleanRuleResult = moteurRegleResult.getBooleanRuleResult(libelleCourt);
        nodeRegleAffichage.getValue().setVisible(booleanRuleResult != null && booleanRuleResult);
    }

    public boolean boutonRecuperationActive() {
        return !boutonRecupererResultats.isDisable();
    }

    @Override
    protected void surValeurChangee(ElementSaisieGrillePropriete composantProperties, Object oldValue) {
        verifierUtilisableAvantExecutionRegles(composantProperties, oldValue, true);
    }
}
