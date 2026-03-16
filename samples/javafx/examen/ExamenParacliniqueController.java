package ui.view.production.examen;

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

import .business.AffectationMaterielService;
import .business.ConstatPublieService;
import .business.ConsultantService;
import .business.DonneesEntretienMedicalConclusionService;
import .business.EditiqueStorageService;
import .business.ExamenParacliniqueRealiseService;
import .business.FapDataRdvService;
import .business.LanceurApplicationService;
import .business.SuiviOrientationService;
import .business.commons.exception.FileNotFoundInDirectoryException;
import .business.commons.exception.MaterielConnectedAquisitionFileNotFoundException;
import .business.commons.exception.MaterielConnectedAquisitionResultsEmptyException;
import .business.commons.validation.AbstractValidationRule;
import .business.commons.validation.ErrorLevel;
import .business.commons.validation.ValidationError;
import .business.commons.validation.ValidationRule;
import .business.commons.validation.ValidationRuleResult;
import .business.moteurregle.MoteurRegleContext;
import .business.moteurregle.MoteurRegleResult;
import .editique.commons.exception.Hl7MessageReaderException;
import .model.AffectationMateriel;
import .model.Constat;
import .model.ConstatMaterielRealise;
import .model.ConstatPublie;
import .model.ConstatRealise;
import .model.Consultant;
import .model.Dialog3BoutonsParam;
import .model.DocumentPieceJointe;
import .model.DonneesRealisees;
import .model.DossierPrestation;
import .model.EnumStyleClassBouton;
import .model.ExamenParacliniqueMaterielUtilise;
import .model.ExamenParacliniqueRealise;
import .model.FapDataRdv;
import .model.PieceJointeExamen;
import .model.PieceJointeExamenParaclinique;
import .model.Reference;
import .model.Document;
import .model.Salle;
import .model.Utilisateur;
import .model.builder.ExamenParacliniqueMaterielUtiliseBuilder;
import .model.commons.PrintableObject;
import .model.commons.exception.ModelTechnicalException;
import .model.context.ClientContextManager;
import .model.converter.Sage2DocumentToDocumentExamen;
import .model.criterias.ExamenParacliniqueRealiseCriterias;
import .model.criterias.RechercheAffectationMaterielCriterias;
import .model.ecg.TransmissionEcgSecondeLectureQuery;
import .model.enumeration.EnumAction;
import .model.enumeration.EnumConstatCodeLoinc;
import .model.enumeration.EnumEcran;
import .model.enumeration.EnumErreurLancementExamen;
import .model.enumeration.EnumFonction;
import .model.enumeration.EnumFormatDocument;
import .model.enumeration.EnumInteraction;
import .model.enumeration.EnumListeFormatConstat;
import .model.enumeration.EnumOuiNonNspEnCours;
import .model.enumeration.EnumPrintObject;
import .model.enumeration.EnumStatutDossierPrestation;
import .model.enumeration.EnumStatutInteraction;
import .model.enumeration.EnumTypeDocument;
import .model.enumeration.EnumTypeElementConstat;
import .model.enumeration.EnumTypeRessource;
import .model.enumeration.Habilitation;
import .model.examen.FichePatientCriteria;
import .model.factory.ConstatMaterielRealiseWithConstatLibelleVoFactory;
import .model.factory.ConstatRealiseFactory;
import .model.factory.ConstatRealiseWithOrigineInformationsVoFactory;
import .model.factory.DocumentFactory;
import .model.factory.UtilisateurVoFactory;
import .model.helper.ConstatRealiseHelper;
import .model.prestation.EnumTypeInteraction;
import .model.prestation.Interaction;
import .model.prestation.InteractionRealisee;
import .model.prestation.configurationbloc.ConfigurationBloc;
import .model.prestation.configurationbloc.ConstatParacliniqueConfiguration;
import .model.prestation.configurationbloc.EnumBlocEcranDynamiqueAutres;
import .model.prestation.configurationbloc.EnumBlocEcranDynamiqueGrilleDynamique;
import .model.prestation.configurationbloc.GrilleDynamique;
import .model.prestation.configurationbloc.TemplateConfiguration;
import .model.prestation.elementgrilledynamique.ElementConstat;
import .model.prestation.elementgrilledynamique.ElementGrilleDynamique;
import .model.prestation.elementgrilledynamique.EnumECGIOP;
import .model.prestation.elementgrilledynamique.EnumSpiroIOP;
import .model.prestation.elementgrilledynamique.LibelleRegleAffichage;
import .model.production.conclusion.DonneesEntretienMedicalConclusion;
import .model.vo.ConfigurationModeleEcranVo;
import .model.vo.ConstatMaterielRealiseWithConstatLibelleCourtVo;
import .model.vo.ConstatRealiseWithOrigineInformationsVo;
import .model.vo.ExamenParacliniqueRealiseVo;
import .model.vo.ExamenParacliniqueVo;
import .model.vo.TraceVo;
import .model.vo.UtilisateurVo;
import .rest.communication.formdata.ResourceRequest;
import ui.commons.configuration.UiPropertiesConfig;
import ui.commons.javafx.controller.ControllerMode;
import ui.commons.javafx.controller.formfield.FormField;
import ui.commons.javafx.spinner.Spinner;
import ui.dialog.DialogConfirmationController;
import ui.view.interfacegenerale.contentarea.EnumContentArea;
import ui.view.production.ecrandynamique.ElementConstatComposantProperties;
import ui.view.production.ecrandynamique.ElementGrilleDynamiqueComposantProperties;
import ui.view.production.ecrandynamique.ElementRealiseFactory;
import ui.view.production.ecrandynamique.ElementSaisieGrilleDynamiqueComposantProperties;
import ui.view.production.ecrandynamique.GrilleDynamiqueUi;
import ui.view.production.ecrandynamique.ImplementationComposantDynamique;
import ui.view.production.examen.base.AbstractExamenController;
import ui.view.production.examen.common.GrilleDynamiqueUtils;
import ui.view.production.examen.components.ExamensRealisesGridPane;
import ui.view.production.examen.components.ExamensRealisesTableView;
import ui.view.production.model.ResultatConstat;
import fr.cnamts.cpam.bordeaux.utils.DocumentUtils;
import fr.cnamts.cpam.bordeaux.utils.FileUtils;
import fr.cnamts.cpam.bordeaux.utils.JsonUtils;

@Component
@Scope("prototype")
public class ExamenParacliniqueController extends AbstractExamenController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExamenParacliniqueController.class);

    public static final String FORM_URL = "view/production/examen/ExamenParaclinique.fxml";
    public static final String MENU_NAME = "Paraclinique";
    public static final EnumContentArea CONTENT_AREA = EnumContentArea.MAIN_CONTENT_SMALL;
    private static final LinkedList<String> LIST_CSS_FILES = new LinkedList<>(Arrays.asList(
            "production/examen/examen-paraclinique.css", "production/ecrandynamique/ecran-dynamique.css",
            "production/ecrandynamique/tableau-questions.css"));

    static final String DELETE_EXAMEN_DIALOGBOX_TITLE = "Suppression Examen Paraclinique";
    static final String DELETE_EXAMEN_DIALOGBOX_MESSAGE = "Confirmez-vous la demande de suppression de l'examen sélectionné ?";
    static final String DIALOGBOX_TITRE_MODALE_IMPRIMER_SIMPLE = "Impression du document";
    static final String DIALOGBOX_TITRE_MODALE_IMPRIMER_AVEC_MAJ_STATUT_INTERACTION = "Options de finalisation du dossier";
    static final String DIALOGBOX_MESSAGE_ENREGISTRER_DOCUMENT = "Souhaitez-vous enregistrer ce document dans le dossier du consultant ?";
    static final String DIALOGBOX_MESSAGE_GENERATION_OU_AJOUT_PIECE_DOSSIER = "Pour clôturer le dossier, choisissez la génération d'un document ou l'ajout d'une pièce jointe.";
    static final String DIALOGBOX_TEXTE_BOUTON1_OUVRIR_SEULEMENT = "Ouvrir seulement";
    static final String DIALOGBOX_TEXTE_BOUTON2_GENERER_ET_CLOTURER = "Générer et clôturer";
    static final String DIALOGBOX_TEXTE_BOUTON2_ENREGISTRER_DANS_DOSSIER = "Enregistrer dans le dossier";
    static final String DIALOGBOX_TEXTE_BOUTON3_AJOUTER_PJ_ET_CLOTURER = "Ajouter une PJ et clôturer";

    static final String OUTPUT_MESSAGE_TITLE = "Abandon d'Opération";
    static final String OUTPUT_MESSAGE = "Des modifications ont été mises en place. Celles-ci n'ont pas été enregistrées.\n Etes-vous sûr de vouloir quitter l'opération actuelle ?";
    static final String EXAMEN_PARACLINIQUE_REALISE_KEY = "examen_paraclinique_realise";
    static final String LIBELLE_ANNULER_DEFAUT = "Annuler";
    private static final String LIBELLE_ANNULER_EXAMEN = "Annuler examen";
    static final String ERROR_DOSSIER_CONTENEUR_RESULTATS_MSG = "Le dossier spécifié lors de l'affectation du matériel comme conteneur des résultats n'a pas été trouvé.";
    private static final String ERROR_CONTENEUR_RESULTATS_MSG = "Aucun fichier présent au sein du dossier conteneur des résultats dont le chemin a été spécifié\nlors de l'affectation du matériel.";
    static final String ERROR_CONTENEUR_RESULTATS_LBL = "Conteneur résultat(s)";
    static final String ERROR_DOSSIER_FICHIER_PATIENT_MSG = "Le dossier spécifié lors de l'affectation du matériel comme dépôt du Fichier Patient n'a pas été trouvé.";
    private static final String ERROR_FICHIER_PATIENT_MSG = "Aucun fichier n'est présent au sein du dossier spécifié lors de l'affectation du matériel\ncomme dépôt du Fichier Patient.";
    static final String ERROR_FICHIER_PATIENT_LBL = "Fichier patient";
    static final String ERROR_LIGNE_COMMANDE_MSG = "Erreur au niveau du chemin de lancement du logiciel indiqué au sein de la ligne de commande\nentrée lors de l'affectation du matériel.";
    private static final String ERROR_RECUPERATION_PJ_MSG = "Erreur au niveau du la récupération des pièces jointes.";
    static final String ERROR_LIGNE_COMMANDE_LBL = "Ligne de commande";
    private static final String ERROR_PIECES_JOINTES_LBL = "Pièces jointes";
    private static final String ERROR_PLUSIEURS_TENT_LBL = "Interaction - Plusieurs tentatives";
    private static final String ERROR_PLUSIEURS_TENT_MSG = "L'interaction n'autorise qu'une seule tentative (\"Plusieurs tentatives\" à non)";
    private static final String ERROR_AUCUN_CONSTAT = "Aucun constat n'a été mis en place au sein de la grille dynamique";
    private static final String ERROR_AUCUN_CONSTAT_LBL = "INTERACTION - CONSTATS";
    private static final String ERROR_SUPPRESSION_EXAMEN_LBL = "Suppression examen";
    private static final String ERROR_SUPPRESSION_EXAMENT_MSG = "L'interaction n'autorise pas la suppression d'examens (\"Supprimable\" à non)";
    private static final String ERROR_INTERPRETATION_MSG = "Vous n'avez entré aucune donnée au sein de l'interprétation.";
    private static final String ERROR_INTERPRETATION_LBL = "Interprétation";

    private static final String CAS_INTERPRETE_EXAMEN = "Interprété par ";
    private static final String CAS_NON_INTERPRETE_EXAMEN = "Non interprété";

    static final String SAISIE_CONSTAT_REQUIRED = "Saisie constat obligatoire.";
    private static final String ACQUISITION_EXAMEN = "Acquisition examen";
    private static final String LANCEMENT_EXAMEN = "Lancement examen";
    private static final String NOT_ALL_RESULT_FILES = "Le compte rendu de l'examen est disponible mais il peut manquer des fichiers de résultats.\nVous pouvez le cas échéant renouveler votre examen.";
    private static final String LOGICIEL_CIBLE_NON_INSTALLER_MAL_CONFIGURER = "Cet examen ne peut pas être exécuté car le logiciel cible n'est pas installé ou incorrectement configuré";
    static final String SUPPRIMER_PJ_CSS = "button-supprimer-pj";

    private static final Predicate<AffectationMateriel> PREDICAT_MATERIEL_UTILISABLE = AffectationMateriel::isUtilisable;
    private static final String PREPARATION_FICHIERS = "PREPARATION DES FICHIERS";
    private static final String CSS_CLASS_SPINNER_TEXT = "spinner-text_visible";
    private static final String CONSTAT_MATERIEL_SUFFIXE = " (Donnée d'appareil)";

    private static final String NOM_FICHIER_IMPRESSION = "impression_%s.pdf";

    static final String CONSOLE_DOMAINE_PJ = "Pièce-jointe";
    static final String CONSOLE_MESSAGE_PJ_PAYLOADMAXREACHED = "Ajout de fichier impossible. La taille du fichier ne doit pas excéder %s";
    public static final String CONSTAT = "constat_";
    public static final String AJOUT_D_UNE_PJ = "Ajout d'une PJ";
    public static final String MESSAGE_AJOUT_PJ = "Attention: une transmission en seconde lecture est en cours pour cette examen. Si vous ajoutez une pièce jointe, cette transmission ne pourra plus être réceptionnée et l’examen passera au statut 'à interpréter'";

    static final String LECTURE_EXAMEN = "Saisir interprétation";
    private static final String CAS_LU_EXAMEN = "Interprété par ";
    private static final String CAS_NON_LU_EXAMEN = "Non interprété";

    private static final String LIBELLE_COURT_INTERACTION_URINES = "Urines";
    private static final String CHAMP_DATE_RECUEIL_URINES = "Date de recueil des urines";
    private static final String CHAMP_DATE_RECUEIL_URINES_AFTER_CURRENT_DATE_ERROR_MSG = "la date de recueil est postérieure à la date du jour";
    private static final String CHAMP_DATE_RECUEIL_URINE_LIBELLE_COURT = "DateReUrine2";
    private static final String CHAMP_HEURE_RECUEIL_URINES = "Heure de recueil des urines";
    private static final String CHAMP_HEURE_RECUEIL_URINES_AFTER_ANALYSE_HOUR_ERROR_MSG = "L’heure de recueil est postérieure à l’heure d’analyse";
    private static final String CHAMP_HEURE_RECUEIL_URINES_LIBELLE_COURT = "HeRecUrine2";
    private static final String CHAMP_HEURE_ANALYSE_URINES_LIBELLE_COURT = "HeAnaUrines";

    // FIXME ModeleMateriel -> type pour affectation de matériel
    private static final String SPIROMETRE = "Spiromètre";
    private static final String ECG = "Electrocardiogramme";
    private static final String AUDIOMETRE = "Audiomètre";

    private static final Integer BOUTON_WIDTH_750 = 750;

    @Autowired
    private ExamenParacliniqueRealiseService examenParacliniqueRealiseService;
    @Autowired
    private ConsultantService consultantService;
    @Autowired
    private AffectationMaterielService affectationMaterielService;
    @Autowired
    private LanceurApplicationService lanceurApplicationService;
    @Autowired
    private EditiqueStorageService editiqueStorageService;
    @Autowired
    private ConstatPublieService constatPublieService;
    @Autowired
    private SuiviOrientationService suiviOrientationService;
    @Autowired
    ImplementationComposantDynamique implementationComposantDynamique;

    @Autowired
    private ConstatRealiseFactory constatRealiseFactory;
    @Autowired
    private ElementRealiseFactory elementRealiseFactory;
    @Autowired
    private ConstatMaterielRealiseWithConstatLibelleVoFactory constatMaterielRealiseFac;
    @Autowired
    private Sage2DocumentToDocumentExamen converterSage2DocToDocExamen;
    @Autowired
    private FapDataRdvService fapDataRdvService;
    @Autowired
    private DonneesEntretienMedicalConclusionService donneesEntretienMedicalConclusionService;

    private FapDataRdv fapDataRdv;

    @Autowired
    private UiPropertiesConfig uiPropertiesConfig;
    @Autowired
    private FileUtils fileUtils;

    @FXML
    ExamensRealisesTableView epcListeExamensRealisesTableView;
    @FXML
    TableColumn<ExamenParacliniqueRealise, String> epcExamenCol;
    @FXML
    Label lblNomSalle;

    @FXML
    Button epcAnnulerExamenBtn;
    @FXML
    Button epcSupprimerExamenBtn;
    @FXML
    Button epcAjoutManuelPJBtn;
    @FXML
    Button epcModifierExamenBtn;
    @FXML
    Button epcEnregistrerExamenBtn;
    @FXML
    Button epcNouvelExamenBtn;
    @FXML
    Button epcInterpreterBtn;
    @FXML
    Button epcAcquisitionResultatsBtn;
    @FXML
    Button epcLancerExamenBtn;
    @FXML
    Button epcImprimerExamenBtn;

    @FXML
    HBox piecesJointesContainerHBox;
    @FXML
    HBox piecesJointesHb;
    @FXML
    ExamensRealisesGridPane examenGridPane;
    @FXML
    GridPane interpretationGridPane;

    @FXML
    GridPane lecturePrerequiseGridPane;
    @FXML
    Label lecturePrerequiseLbl;
    @FXML
    CheckBox lecturePrerequiseChcbx;

    @FXML
    VBox mainContentVBox;

    private Salle salle;
    private Utilisateur connectedUser;
    private UtilisateurVo connectedUserVo;
    private List<ExamenParacliniqueRealise> examensParacliniqueAlreadyExisting;
    private List<ElementConstat> lsElementsConstat = new ArrayList<>();
    private final List<LibelleRegleAffichage> lsLibelleRegleAffichage = new ArrayList<>();
    private List<ElementConstat> lsElementsConstatMateriel = new ArrayList<>();

    private List<ElementConstat> lsElemCstInterpOuLectureMateriel = new ArrayList<>();

    private ControllerMode interpretationControllerMode;
    private List<AffectationMateriel> lsAllAffectationsMateriel = new ArrayList<>();
    private List<AffectationMateriel> lsAffectationMaterielUtilisableToExamen = new ArrayList<>();
    private String idConsultantStr = null;
    AffectationMateriel affectationMaterielConnecte;
    boolean isLectureMode = false;
    private ExamenParacliniqueRealise editedExam;

    boolean isSoftwareLaunchOperation = false;
    SimpleBooleanProperty isConnectedExamen = new SimpleBooleanProperty(false);
    private boolean areEqualGeneric = true;

    private final Map<Long, ElementGrilleDynamiqueComposantProperties> reglesAffichageElementComposantPropertiesMap = new HashMap<>();

    private Spinner mainSpinner = null;
    TemplateConfiguration templateConfiguration;
    private Document DocumentImpressionMisas;
    private boolean isPjOpPostRefresh = false;
    SimpleBooleanProperty isRunningListChangeAffMatProperty = new SimpleBooleanProperty(false);
    private List<String> lsTypeMaterielBoundToInteraction;
    private ChangeListener<Boolean> onListChangeAffMatCurrentListener;

    // Configuration constats d'interpretation ou constats de lecture
    private String interpreteOrLu = CAS_INTERPRETE_EXAMEN;
    private String nonInterpreteOrNonLu = CAS_NON_INTERPRETE_EXAMEN;
    private EnumStatutInteraction statutInteractionAInterpreterOrLectureARealiser = EnumStatutInteraction.A_INTERPRETER;

    @Override
    protected void _initForm(final LinkedList<String> listCssFiles) {
        super._initForm(LIST_CSS_FILES);
        activateCharacterSizeChangeOperations(mainContentVBox);
        setMode(ControllerMode.CONSULTATION);
        initConnectedUserVo();
        initListeners();
        initTableView();
        initLecturePrerequiseBlocInterpretationOuLectureContent();
    }

    private void initConnectedUserVo() {
        connectedUser = sessionContext.getConnectedUser();
        connectedUserVo = UtilisateurVoFactory.buildUtilisateurVo(connectedUser);
    }

    private void initListeners() {
        piecesJointesContainerHBox.managedProperty().bind(piecesJointesContainerHBox.visibleProperty());
        lecturePrerequiseChcbx.selectedProperty().addListener(initLecturePrerequiseChcbxListener());
    }

    private ChangeListener<Boolean> initLecturePrerequiseChcbxListener() {
        return (observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                interpreteOrLu = CAS_INTERPRETE_EXAMEN;
                nonInterpreteOrNonLu = CAS_NON_INTERPRETE_EXAMEN;
                statutInteractionAInterpreterOrLectureARealiser = EnumStatutInteraction.A_INTERPRETER;
            } else {
                interpreteOrLu = CAS_LU_EXAMEN;
                nonInterpreteOrNonLu = CAS_NON_LU_EXAMEN;
                statutInteractionAInterpreterOrLectureARealiser = EnumStatutInteraction.LECTURE_A_REALISER;

            }
        };
    }

    private void initTableView() {
        epcExamenCol.prefWidthProperty().bind(epcListeExamensRealisesTableView.widthProperty());
        epcExamenCol.setCellValueFactory(cellValue -> new SimpleStringProperty(examenParacliniqueRealiseCellValueConcatenatation(cellValue.getValue())));
        epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty()
                .addListener(initSelectedRowFromTableViewChangeListener());
        selectedExamenParaclinique.bind(epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty());
    }

    private String examenParacliniqueRealiseCellValueConcatenatation(ExamenParacliniqueRealise examenParacliniqueRealise) {
        StringBuilder examenParacliniqueStrBuilder = new StringBuilder();
        examenParacliniqueStrBuilder.append(getDisplayedInteractionRealiseeInteraction().getLibelleLong().toUpperCase());
        examenParacliniqueService.createRealisateurCellValuePart(examenParacliniqueStrBuilder, examenParacliniqueRealise);
        examenParacliniqueService.createLieuDateCellValuePart(examenParacliniqueStrBuilder, examenParacliniqueRealise);
        examenParacliniqueService.createMaterielCellValuePart(examenParacliniqueStrBuilder, examenParacliniqueRealise);
        examenParacliniqueService.createInterpretationCellValuePart(examenParacliniqueStrBuilder, examenParacliniqueRealise,
                interpretationGridPane.isVisible(), configurationModeleEcran, isLectureMode, interpreteOrLu, nonInterpreteOrNonLu);
        return examenParacliniqueStrBuilder.toString();
    }

    protected ChangeListener<ExamenParacliniqueRealise> initSelectedRowFromTableViewChangeListener() {
        return (obs, oldSelection, newSelection) -> {
            initPieceJointeHBoxContentFromSpecificExamenParaclinique(selectedExamenParaclinique.get());
            rowSelectionOperationControllerModeCases(oldSelection, newSelection);
            if (selectedExamenParaclinique.get() != null) {
                updateButtonsAccordingToLastExamen(
                        selectedExamenParaclinique.get().equals(epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(null)));
            } else {
                updateButtonsAccordingToLastExamen(false);
            }
        };
    }

    private void initPieceJointeHBoxContentFromSpecificExamenParaclinique(ExamenParacliniqueRealise examenParacliniqueRealise) {
        piecesJointesHb.getChildren().clear();
        if (examenParacliniqueRealise != null && CollectionUtils.isNotEmpty(examenParacliniqueRealise.getListePiecesJointes())) {
            examenParacliniqueRealise.getListePiecesJointes().stream().filter(Doc -> Doc != null && Doc.getNom() != null)
                    .forEach(this::putDocIntoPJHBox);
        }
    }

    private void putDocIntoPJHBox(Document Doc) {
        if (Doc.getNom() != null && Doc.getNom().toLowerCase().endsWith(DocumentUtils.PDF_EXTENSION)) {
            Hyperlink hl = new Hyperlink(Doc.getNom());
            hl.setOnAction(pdfPJSelectionActionEvent(Doc));
            hl.getStyleClass().add("hyperlink-accueil");

            piecesJointesHb.getChildren().add(hl);
        } else {
            Label lab = new Label();
            lab.setText(Doc.getNom());
            piecesJointesHb.getChildren().add(lab);
        }

        if (!ClientContextManager._SYSTEM_USER_ID.equals(Doc.getUserCreationId())) {
            piecesJointesHb.getChildren().add(initSupprimerPieceJointeManuelle(Doc));
        }

    }

    private void tracerExamenParaclinique(EnumAction action, String idRessource) {
        sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                action,
                EnumTypeRessource.FAP,
                idRessource);
    }

    public void supprimerPieceJointe(HBox pjData, Document pieceJointe) {
        tracerExamenParaclinique(EnumAction.SUPPRIMER_PJ, idDossierPrestation.toString());

        examenParacliniqueService.supprimerPieceJointe(pieceJointe, selectedExamenParaclinique.get(), affectationMaterielConnecte, displayedInteractionRealisee,
                templateConfiguration, statutInteractionAInterpreterOrLectureARealiser, getInterpretationSaisieUtilisateurStream().findAny().isPresent());

        piecesJointesHb.getChildren().remove(pjData);
        refreshFormData();
    }

    private EventHandler<ActionEvent> pdfPJSelectionActionEvent(Document pdfDoc) {
        Document archivedPdf = editiqueStorageService.getDocumentById(pdfDoc.getId());
        if (EnumInteraction.ECG.getLibelleCourt().equals(pdfDoc.getSource())) {
            TransmissionEcgSecondeLectureQuery transEcgSndLectureQuery = new TransmissionEcgSecondeLectureQuery();
            transEcgSndLectureQuery.setStatutInteraction(displayedInteractionRealisee.getStatut());
            transEcgSndLectureQuery.setInteractionRealiseeId(displayedInteractionRealisee.getId());
            transEcgSndLectureQuery.setDossierPrestationId(displayedDossierPrestationProperty.getValue().getId());
            return actionEvent -> {
                pdfManager.showDocumentECG(archivedPdf, Habilitation.GERER_EXAMEN_CONSULTER_EXAMEN,
                        transEcgSndLectureQuery,
                        () -> {
                            showModaleTransmissionEcgSecondLecture(transEcgSndLectureQuery, this::refreshFromDataTransmissionEcg);
                        });
            };
        } else {
            return actionEvent -> pdfManager.showDocument(archivedPdf, Habilitation.GERER_EXAMEN_CONSULTER_EXAMEN);
        }
    }

    private void refreshFromDataTransmissionEcg() {
        displayedInteractionRealisee = interactionRealiseeService.getInteractionRealiseeById(displayedInteractionRealisee.getId());
        setMode(ControllerMode.CONSULTATION);
        refreshFormData();
    }

    private void rowSelectionOperationControllerModeCases(ExamenParacliniqueRealise oldSel, ExamenParacliniqueRealise newSel) {
        if (ControllerMode.CONSULTATION == getMode()) {
            rowSelectionOperationsInConsultationCase(oldSel, newSel);
        } else {
            rowSelectionOperationsInEditionCase(oldSel, newSel);
        }
    }

    @Override
    protected void anotherRowSelectedInEditionMode(ExamenParacliniqueRealise oldSelection, ExamenParacliniqueRealise newSelection) {
        if (isModifIntoGrilleDynamiqueData() && !isPjOpPostRefresh) {
            dialogService.openDialogConfirmation(getPrimaryStage(), OUTPUT_MESSAGE_TITLE, OUTPUT_MESSAGE,
                    onSelectionItemTableView(newSelection), onCancelDialogConfirmationBoxOperation(oldSelection));
        } else {
            rowSelectionOperations(newSelection);
            setMode(ControllerMode.CONSULTATION);
            isPjOpPostRefresh = false;
        }
    }

    @Override
    protected void postVerificationOperationsAfterRowSelectionInInterpretationModeCase(ExamenParacliniqueRealise oldSelection,
            ExamenParacliniqueRealise newSelection) {
        if (isModifIntoInterpretationDynamiquePartData()) {
            dialogService.openDialogConfirmation(getPrimaryStage(), OUTPUT_MESSAGE_TITLE, OUTPUT_MESSAGE,
                    onSelectionItemTableViewInterpretation(newSelection),
                    onCancelDialogConfirmationBoxOperation(oldSelection));
        } else {
            selectionItemTableViewInterpretationCommonOperations(newSelection);
        }
    }

    private Consumer<DialogConfirmationController> onSelectionItemTableViewInterpretation(final ExamenParacliniqueRealise examenRealise) {
        return t -> selectionItemTableViewInterpretationCommonOperations(examenRealise);
    }

    private void selectionItemTableViewInterpretationCommonOperations(final ExamenParacliniqueRealise examenRealise) {
        if (composantInterpretationOuLecturePropertiesByElementId != null && !composantInterpretationOuLecturePropertiesByElementId.isEmpty()) {
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        }
        isExamenMode = true;
        rowSelectionOperations(examenRealise);
        setMode(ControllerMode.CONSULTATION);
    }

    @Override
    protected void updateInteractionRealiseeForNewMainExamenParaclinique(ExamenParacliniqueRealise mainExamenRealise) {
        final EnumStatutInteraction oldStatut = displayedInteractionRealisee.getStatut();
        EnumStatutInteraction newStatut = EnumStatutInteraction.AFAIRE;
        if (mainExamenRealise != null) {
            if (allConstatObligatoireRenseigne(mainExamenRealise)) {
                if (examenParacliniqueService.isInteractionExamenConnecte(affectationMaterielConnecte)) {
                    if (Arrays.asList(EnumStatutInteraction.A_INTERPRETER, EnumStatutInteraction.LECTURE_A_REALISER, EnumStatutInteraction.FAIT)
                            .contains(oldStatut)) {
                        if (!existAnyInterpretationSaisieUtilisateurRenseigne(mainExamenRealise) && isAcquisitionDejaFaite()) {
                            newStatut = statutInteractionAInterpreterOrLectureARealiser;
                        } else {
                            newStatut = EnumStatutInteraction.FAIT;
                        }
                    }
                } else if (!existAnyInterpretationSaisieUtilisateurRenseigne(mainExamenRealise) && (!isLectureMode || lecturePrerequiseChcbx.isSelected())) {
                    newStatut = statutInteractionAInterpreterOrLectureARealiser;
                } else if (EnumStatutInteraction.FAIT != oldStatut && examenParacliniqueService.isChangementStatutOnlyAfterGeneratePdf(templateConfiguration)) {
                    newStatut = oldStatut;
                } else {
                    newStatut = EnumStatutInteraction.FAIT;
                }
            }
        }
        displayedInteractionRealisee.setStatut(
                (mainExamenRealise != null && EnumStatutInteraction.AFAIRE == newStatut
                        && isInteractionRealiseAInterpreterOrTransmis2ndLectureOrEnAttenteLectureState(displayedInteractionRealisee))
                                ? oldStatut
                                : newStatut);
        boolean isRealisateurInteractionRealiseeUpddated = majRealisateurSiStatutAutorise(connectedUserVoSupplier(),
                EnumStatutInteraction.STATUTS_NECESSITANT_REALISATEUR, displayedInteractionRealisee);
        if (!oldStatut.equals(newStatut) || isRealisateurInteractionRealiseeUpddated) {
            displayedInteractionRealisee = updateInteractionRealisee(displayedInteractionRealisee);
        }

    }

    private boolean isInteractionRealiseAInterpreterOrTransmis2ndLectureOrEnAttenteLectureState(InteractionRealisee interactionRealisee) {
        return EnumStatutInteraction.getInteractionStatutTransmis2ndLectureEnAttenteLectureState().contains(interactionRealisee.getStatut());
    }

    private boolean isInteractionRealiseTransmis2ndLectureOrEnAttenteLectureState(InteractionRealisee interactionRealisee) {
        return EnumStatutInteraction.getInteractionStatutTransmis2ndLectureEnAttenteLectureState().contains(interactionRealisee.getStatut());
    }

    private boolean isAcquisitionDejaFaite() {
        return selectedExamenParaclinique != null && selectedExamenParaclinique.get() != null
                && CollectionUtils.isNotEmpty(selectedExamenParaclinique.get().getListePiecesJointes());
    }

    private boolean allConstatObligatoireRenseigne(ExamenParacliniqueRealise examenRealise) {
        List<ElementConstat> lsElemsConstatAvailable = getAvailableElementConstatListFromFullList(lsElementsConstat);
        return lsElemsConstatAvailable.stream().filter(ElementConstat::getIsObligatoire)
                .allMatch(elementConstat -> examenParacliniqueService.existsValueForElementConstatInExamenParacliniqueRealise(examenRealise, elementConstat));
    }

    protected boolean existsValueForElementConstatInterpretationInExamenParacliniqueRealise(ExamenParacliniqueRealise examenRealise,
            ElementConstat elementConstat) {
        return examenParacliniqueService.existsValueForElementConstatInterpretationInExamenParacliniqueRealise(examenRealise, elementConstat);
    }

    private void initSalleAndAffectationMaterielChangeListener(Long idDP, long idCons, InteractionRealisee interRea) {
        isRunningListChangeAffMatProperty.bind(sessionContext.isRunningListChangeAffectationMaterielProperty());
        onListChangeAffMatCurrentListener = generateOnListChangeAffMatCurrentListener();
        isRunningListChangeAffMatProperty.addListener(onListChangeAffMatCurrentListener);
    }

    private ChangeListener<Boolean> generateOnListChangeAffMatCurrentListener() {
        return (obs, oldv, newv) -> {
            if (Boolean.FALSE.equals(newv)) {
                Platform.runLater(() -> launchVerificationsAndActionAfterListAffectationMaterielChanged());
            }
        };
    }

    protected void launchVerificationsAndActionAfterListAffectationMaterielChanged() {
        List<AffectationMateriel> lsMaterielFromSessionContext = sessionContext.getListeAffectationsMateriel().stream()
                .filter(am -> lsTypeMaterielBoundToInteraction.contains(am.getModele().getType()))
                .filter(PREDICAT_MATERIEL_UTILISABLE)
                .collect(Collectors.toList());
        Salle salleFromSessionContext = sessionContext.getSalle();
        if (!salleFromSessionContext.getNom().equals(salle.getNom())
                || (lsMaterielFromSessionContext.size() != lsAffectationMaterielUtilisableToExamen.size()
                        || !lsMaterielFromSessionContext.containsAll(lsAffectationMaterielUtilisableToExamen))) {
            operateMainDataReloadWhenSalleOrListMaterielWithModifications();
        }
    }

    private void operateMainDataReloadWhenSalleOrListMaterielWithModifications() {
        if ((ControllerMode.EDITION == getMode() || ControllerMode.EDITION == interpretationControllerMode) && existsModificationsInForm()) {
            dialogService.openDialogConfirmationAbandonModification(primaryStage,
                    dialog -> onAcceptAbandonModifications(),
                    dialog -> onCancelAbandonModifications(),
                    OUTPUT_MESSAGE);
        } else {
            if (ControllerMode.EDITION == getMode()) {
                setMode(ControllerMode.CONSULTATION);
            } else if (ControllerMode.EDITION == interpretationControllerMode) {
                saisirInterpretationMode(ControllerMode.CONSULTATION);
            }
            operateSalleAndMaterielAndFormDataReload();
        }
    }

    private void onAcceptAbandonModifications() {
        if (ControllerMode.EDITION == getMode()) {
            GrilleDynamiqueUtils.resetAllValues(composantExamenPropertiesByElementId);
            setMode(ControllerMode.CONSULTATION);
        } else if (ControllerMode.EDITION == interpretationControllerMode) {
            GrilleDynamiqueUtils.resetAllValues(composantInterpretationOuLecturePropertiesByElementId);
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        }
        operateSalleAndMaterielAndFormDataReload();
    }

    private void onCancelAbandonModifications() {
        isRunningListChangeAffMatProperty.unbind();
        isRunningListChangeAffMatProperty.removeListener(onListChangeAffMatCurrentListener);
        sessionContext.setListeAffectationsMateriel(lsAllAffectationsMateriel);
        sessionContext.setMaterielVerifie(Boolean.TRUE);
        sessionContext.setSalle(salle);
        isRunningListChangeAffMatProperty.bind(sessionContext.isRunningListChangeAffectationMaterielProperty());
        isRunningListChangeAffMatProperty.addListener(onListChangeAffMatCurrentListener);
    }

    private void operateSalleAndMaterielAndFormDataReload() {
        salle = sessionContext.getSalle();
        lblNomSalle.setText(salle.getNom());
        lsAllAffectationsMateriel.clear();
        lsAllAffectationsMateriel.addAll(sessionContext.getListeAffectationsMateriel());
        lsAffectationMaterielUtilisableToExamen = lsAllAffectationsMateriel.stream()
                .filter(am -> lsTypeMaterielBoundToInteraction.contains(am.getModele().getType()))
                .filter(PREDICAT_MATERIEL_UTILISABLE)
                .collect(Collectors.toList());
        refreshFormData();
    }

    public void consultExamenParaclinique(Long idDossierPrestation, Long idConsultant,
            InteractionRealisee interactionRealisee, List<AffectationMateriel> lsAffectationMateriel, List<String> lsTypeMaterielExpected) {
        salle = sessionContext.getSalle();
        lblNomSalle.setText(salle.getNom());
        lsTypeMaterielBoundToInteraction = lsTypeMaterielExpected;
        lsAllAffectationsMateriel.addAll(sessionContext.getListeAffectationsMateriel());
        lsAffectationMaterielUtilisableToExamen = lsAffectationMateriel.stream().filter(PREDICAT_MATERIEL_UTILISABLE).collect(Collectors.toList());
        initSalleAndAffectationMaterielChangeListener(idDossierPrestation, idConsultant, interactionRealisee);
        setDatas(idDossierPrestation, idConsultant, interactionRealisee);
        displayedInteractionRealisee = interactionRealisee;
        isExamenMode = true;
        interpretationControllerMode = ControllerMode.CONSULTATION;
        if (!interactionRealisee.getInteraction().getConfigurationEcrans().isEmpty()) {
            retrieveConfigurationModeleEcran();
        }
        templateConfiguration = getTemplateConfiguration();
        if (templateConfiguration != null) {
            epcImprimerExamenBtn.setVisible(templateConfiguration.getModeleDocumentId() != null);
            if (templateConfiguration.getLibelleBouton() != null && StringUtils.isNotBlank(templateConfiguration.getLibelleBouton().getIntitule())) {
                epcImprimerExamenBtn.setText(templateConfiguration.getLibelleBouton().getIntitule());
            }
        }

        if (isLectureMode) {
            lecturePrerequiseGridPane.setVisible(true);
            epcInterpreterBtn.setText(LECTURE_EXAMEN);
        }

        setMode(ControllerMode.CONSULTATION);
        refreshFormData();

        fapDataRdv = fapDataRdvService.getDataRdvByIdFap(idDossierPrestation);

        // Tracabilité
        Interaction displayedInteractionRealiseeInteraction = getInteractionFromPrestationConfigurationById(
                displayedInteractionRealisee.getInteraction().getId());
        EnumEcran enumEcran = EnumEcran.UNKNOW;
        String complement = StringUtils.EMPTY;
        if (displayedInteractionRealiseeInteraction != null && EnumTypeInteraction.EXAMEN == displayedInteractionRealiseeInteraction.getTypeInteraction()) {
            enumEcran = EnumEcran.EXAMEN_PARACLINIQUE;
            if (displayedInteractionRealiseeInteraction.getLibelleCourt() != null) {
                complement = fr.cnamts.cpam.bordeaux.utils.StringUtils.stripAccents(displayedInteractionRealiseeInteraction.getLibelleCourt().toUpperCase());
            }
        }
        TraceVo traceVo = new TraceVo();
        traceVo.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                enumEcran, complement,
                EnumAction.CONSULTER,
                EnumTypeRessource.FAP,
                idDossierPrestation.toString());
        traceService.create(traceVo);
        // *********

    }

    private void initLecturePrerequiseBlocInterpretationOuLectureContent() {
        lecturePrerequiseGridPane.managedProperty().bind(lecturePrerequiseGridPane.visibleProperty());
        interpretationGridPane.managedProperty().bind(interpretationGridPane.visibleProperty());
    }

    private void setLecturePrerequiseChildrenDisableValue() {
        if (isExamenMode) {
            lecturePrerequiseGridPane.setDisable(getMode().equals(ControllerMode.CONSULTATION));
        } else {
            lecturePrerequiseGridPane.setDisable(true);
        }
    }

    @Override
    protected Map<String, ResourceRequest> getFormResourceRequests() {
        Map<String, ResourceRequest> resources = super.getFormResourceRequests();
        if (displayedInteractionRealisee != null) {
            ExamenParacliniqueRealiseCriterias examenParacliniqueRealiseCriterias = new ExamenParacliniqueRealiseCriterias();
            examenParacliniqueRealiseCriterias.setIdInteractionRealise(displayedInteractionRealisee.getId());
            examenParacliniqueRealiseCriterias.setLastVersionExamen(false);
            resources.put(EXAMEN_PARACLINIQUE_REALISE_KEY, new ResourceRequest(ExamenParacliniqueRealise[].class, examenParacliniqueRealiseCriterias));
        }
        return resources;
    }

    @Override
    protected void setFormData(Map<String, Object> resources) {
        super.setFormData(resources);
        piecesJointesContainerHBox.setVisible(true);
        if (resources.containsKey(EXAMEN_PARACLINIQUE_REALISE_KEY)) {
            examensParacliniqueAlreadyExisting = Arrays.asList((ExamenParacliniqueRealise[]) resources.get(EXAMEN_PARACLINIQUE_REALISE_KEY));
        }
        if (!epcListeExamensRealisesTableView.getItems().isEmpty()) {
            epcListeExamensRealisesTableView.getItems().clear();
        }
        if (displayedInteractionRealisee != null) {
            retrieveAndVerifyIfAffectationMaterielIsConnected();
        }
        if (!examensParacliniqueAlreadyExisting.isEmpty()) {
            epcListeExamensRealisesTableView.getItems().addAll(FXCollections.observableArrayList(examensParacliniqueAlreadyExisting));
            selectRowAfterTableViewRefreshUsingFormData();
        } else {
            Stream.of(composantExamenPropertiesByElementId, composantInterpretationOuLecturePropertiesByElementId)
                    .forEach(GrilleDynamiqueUtils::resetAllValues);
        }

        if (isLectureMode) {
            interpretationGridPane.setVisible(lecturePrerequiseChcbx.isSelected());
        } else {
            interpretationGridPane.setVisible(true);
        }
    }

    private void selectRowAfterTableViewRefreshUsingFormData() {
        if (idExamenToSelect == null) {
            selectRowCorrespondingToLastExamenParacliniqueRealised();
        } else {
            epcListeExamensRealisesTableView.selectExamenByIdInTableView(idExamenToSelect);
        }
    }

    private void selectRowCorrespondingToLastExamenParacliniqueRealised() {
        ExamenParacliniqueRealise examenToSelect = examensParacliniqueAlreadyExisting.stream()
                .max(Comparator.comparing(ExamenParacliniqueRealise::getDateCreation)).orElse(null);
        Objects.requireNonNull(examenToSelect);
        epcListeExamensRealisesTableView.getSelectionModel().clearSelection();
        epcListeExamensRealisesTableView.getSelectionModel().select(examenToSelect);
        epcListeExamensRealisesTableView.scrollTo(examenToSelect);
    }

    private void retrieveAndVerifyIfAffectationMaterielIsConnected() {
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        if (interaction.getReferencesTypeMateriel() != null && !interaction.getReferencesTypeMateriel().isEmpty()) {
            RechercheAffectationMaterielCriterias criteria = new RechercheAffectationMaterielCriterias();
            criteria.setSalle(salle.getNom());
            List<Reference> lsTypeMateriel = interaction.getReferencesTypeMateriel();
            List<String> lsLibelleTypeMateriel = lsTypeMateriel.stream().map(Reference::getIntitule)
                    .collect(Collectors.toList());
            List<AffectationMateriel> affectationMaterielFromSalle = affectationMaterielService.getAffectationsMaterielByCriterias(criteria);
            affectationMaterielConnecte = affectationMaterielFromSalle.stream()
                    .filter(AffectationMateriel::isConnected)
                    .filter(afm -> lsLibelleTypeMateriel.contains(afm.getModele().getType())).findFirst().orElse(null);
            isConnectedExamen.set(affectationMaterielConnecte != null);
        }
    }

    @Override
    protected Map<Region, FormField> getFormFields() {
        final Map<Region, FormField> fields = new HashMap<>();
        fields.put(epcListeExamensRealisesTableView, new FormField(new ArrayList<>(), epcListeExamensRealisesTableView));
        return fields;
    }

    @Override
    protected void _setMode(ControllerMode mode) {
        if (ControllerMode.CONSULTATION == mode) {
            setExamenInConsultationMode();
        } else {
            setExamenInEditionMode();
        }
        setLecturePrerequiseChildrenDisableValue();
    }

    private void updateButtonsAccordingToLastExamen(Boolean isLastExamen) {
        if (Boolean.TRUE.equals(isLastExamen)) {
            epcInterpreterBtn.disableProperty().bind(interpreterBtnBinding().not());
            boolean isModifiyingAllowed = interactionHabilitationHandler
                    .checkHabilitationModifierExamen(getDisplayedInteractionRealiseeInteraction());

            if (displayedDossierPrestationProperty.getValue() != null && displayedDossierPrestationProperty.getValue().isUnmodifiable()) {
                isModifiyingAllowed = isModifiyingAllowed
                        && actionManager.hasHabilitation(Habilitation.GERER_PRESTATION_REALISEE_MODIFIER_DOSSIER_CLOTURE);
            }
            setDisable(epcModifierExamenBtn, !isModifiyingAllowed);
        } else {
            Stream.of(epcInterpreterBtn, epcModifierExamenBtn).forEach(btn -> setDisable(btn, true));
        }
    }

    private void setExamenInConsultationMode() {
        BooleanBinding isModifyingAllowedBinding = Bindings.createBooleanBinding(isModifiyingAllowedBinding(), displayedDossierPrestationProperty);
        Stream.of(epcAnnulerExamenBtn, epcEnregistrerExamenBtn, epcAcquisitionResultatsBtn, epcAjoutManuelPJBtn).forEach(btn -> setDisable(btn, true));
        Stream.of(epcModifierExamenBtn, epcSupprimerExamenBtn)
                .forEach(btn -> btn.disableProperty().bind(epcListeExamensRealisesTableView.getSelectionModel()
                        .selectedItemProperty().isNull().or(isModifyingAllowedBinding.not())));
        epcInterpreterBtn.disableProperty().bind(interpreterBtnBinding().not().or(isModifyingAllowedBinding.not()));

        habilitationEpcSupprimerExamenBtn();
        habilitationEpcNouvelExamenBtn();

        BooleanBinding canLancerExamenBeLaunched = Bindings.createBooleanBinding(canLancerExamenOperationBeLaunched(),
                epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty());
        epcLancerExamenBtn.disableProperty().bind(canLancerExamenBeLaunched.not().or(isModifyingAllowedBinding.not()));

        implementationComposantDynamique.switchModeTo(ControllerMode.CONSULTATION, composantExamenPropertiesByElementId);

        BooleanBinding isImprimerAllowedBinding = Bindings.createBooleanBinding(isImprimerAllowed(), displayedDossierPrestationProperty);
        epcImprimerExamenBtn.disableProperty().bind(epcListeExamensRealisesTableView.getSelectionModel()
                .selectedItemProperty().isNull().or(isImprimerAllowedBinding.not()));
    }

    private void habilitationEpcSupprimerExamenBtn() {
        if (displayedInteractionRealisee != null && isSupprimerExamenNotAllowed(getInteractionFromPrestationConfigurationById(
                displayedInteractionRealisee.getInteraction().getId()))) {
            setDisable(epcSupprimerExamenBtn, true);
        }
    }

    private void habilitationEpcNouvelExamenBtn() {
        setDisable(epcNouvelExamenBtn, !isNouvelExamenBtnAllowed());
    }

    private void setDisable(Button button, boolean disable) {
        button.disableProperty().unbind();
        button.setDisable(disable);
    }

    private Callable<Boolean> isImprimerAllowed() {
        return () -> (displayedDossierPrestationProperty.getValue() != null
                && actionManager.hasHabilitation(Habilitation.GERER_EXAMEN_REALISER_EXAMEN_IMPRIMER));
    }

    private Callable<Boolean> isModifiyingAllowedBinding() {
        return this::isModifiyingAllowed;
    }

    private boolean isModifiyingAllowed() {
        return displayedDossierPrestationProperty.getValue() == null
                || !displayedDossierPrestationProperty.getValue().isUnmodifiable()
                || actionManager.hasHabilitation(Habilitation.GERER_PRESTATION_REALISEE_MODIFIER_DOSSIER_CLOTURE);
    }

    private Callable<Boolean> canLancerExamenOperationBeLaunched() {
        return () -> {
            boolean isConnected = isConnectedExamen.get();
            ReadOnlyObjectProperty<ExamenParacliniqueRealise> examen = epcListeExamensRealisesTableView
                    .getSelectionModel().selectedItemProperty();
            boolean isValueSelected = examen.isNotNull().get();
            boolean hasNotAlreadyBeenLaunched = true;
            boolean isLastOneCreated = true;
            if (isValueSelected && isConnected) {
                List<ConstatMaterielRealise> lsCMR = epcListeExamensRealisesTableView.getSelectionModel().getSelectedItem().getListeConstatMaterielRealise();
                hasNotAlreadyBeenLaunched = !(lsCMR != null && !lsCMR.isEmpty());
                ExamenParacliniqueRealise lastCreatedExamen = epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(null);
                isLastOneCreated = lastCreatedExamen.equals(examen.getValue());
            }
            return isConnected && isValueSelected && hasNotAlreadyBeenLaunched && isLastOneCreated && interactionHabilitationHandler
                    .checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction());
        };
    }

    private void setExamenInEditionMode() {
        epcEnregistrerExamenBtn.setDisable(!interactionHabilitationHandler.checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction()));

        Stream.of(epcAnnulerExamenBtn, epcAjoutManuelPJBtn).forEach(btn -> setDisable(btn, false));
        Stream.of(epcModifierExamenBtn, epcSupprimerExamenBtn, epcImprimerExamenBtn, epcInterpreterBtn, epcAcquisitionResultatsBtn,
                epcLancerExamenBtn).forEach(this::editionButtonBindedOperations);
        setDisable(epcNouvelExamenBtn, true);
        if (selectedExamenParaclinique.get() == null || !actionManager.hasHabilitation(Habilitation.GERER_PJ_EXAMEN_PARACLINIQUE_AJOUTER)) {
            setDisable(epcAjoutManuelPJBtn, true);
        }
        implementationComposantDynamique.switchModeTo(ControllerMode.EDITION, composantExamenPropertiesByElementId);

        lecturePrerequiseChcbx.setDisable(displayedInteractionRealisee.getStatut() == EnumStatutInteraction.FAIT && lecturePrerequiseChcbx.isSelected());

    }

    private void editionButtonBindedOperations(Button btn) {
        btn.disableProperty().unbind();
        btn.setDisable(true);
    }

    @Override
    public boolean existsModificationsInForm() {
        if ((isExamenMode && getMode().equals(ControllerMode.EDITION)) || isSoftwareLaunchOperation) {
            return (isModifIntoGrilleDynamiqueData() || isNotDisabledAcquisitionButton());
        } else if (interpretationControllerMode.equals(ControllerMode.EDITION)) {
            return isModifIntoInterpretationDynamiquePartData();
        } else {
            return false;
        }
    }

    public ValidationRule createElementConstatDateRecueilUrineValidationRule(List<ElementConstat> lsElementsConstat) {
        return new AbstractValidationRule(CHAMP_DATE_RECUEIL_URINES) {
            @Override
            public ValidationRuleResult isValid() {
                final ValidationRuleResult vrr = new ValidationRuleResult();
                boolean hasSucceed = true;

                ElementConstat elemCstDateRecueilUrine = lsElementsConstat.stream()
                        .filter(elem -> null != elem.getConstat()
                                && CHAMP_DATE_RECUEIL_URINE_LIBELLE_COURT.equalsIgnoreCase(elem.getConstat().getLibelleCourt()))
                        .findFirst().orElse(null);
                if (elemCstDateRecueilUrine != null) {
                    ConstatRealise cstReaDateRecueilUrine = retrieveConstatRealiseFromElementConstat(elemCstDateRecueilUrine, null,
                            composantExamenPropertiesByElementId);
                    if (cstReaDateRecueilUrine.getLocalDateValue() != null) {
                        hasSucceed = !cstReaDateRecueilUrine.getLocalDateValue().isAfter(LocalDate.now());
                    }
                }
                vrr.setHasSucceed(hasSucceed);

                final ValidationError ve = new ValidationError(ErrorLevel.ERROR, CHAMP_DATE_RECUEIL_URINES,
                        CHAMP_DATE_RECUEIL_URINES_AFTER_CURRENT_DATE_ERROR_MSG);
                vrr.setValidationError(ve);
                return vrr;
            }
        };
    }

    public ValidationRule createElementConstatHeureRecueilUrineValidationRule(List<ElementConstat> lsElementsConstat) {
        return new AbstractValidationRule(CHAMP_HEURE_RECUEIL_URINES) {
            @Override
            public ValidationRuleResult isValid() {
                final ValidationRuleResult vrr = new ValidationRuleResult();
                boolean hasSucceed = true;

                ElementConstat elemCstHeureRecueilUrine = lsElementsConstat.stream()
                        .filter(elem -> null != elem.getConstat()
                                && CHAMP_HEURE_RECUEIL_URINES_LIBELLE_COURT.equalsIgnoreCase(elem.getConstat().getLibelleCourt()))
                        .findFirst().orElse(null);
                ElementConstat elemCstHeureAnalyseUrine = lsElementsConstat.stream()
                        .filter(elem -> null != elem.getConstat()
                                && CHAMP_HEURE_ANALYSE_URINES_LIBELLE_COURT.equalsIgnoreCase(elem.getConstat().getLibelleCourt()))
                        .findFirst().orElse(null);
                if (elemCstHeureRecueilUrine != null && elemCstHeureAnalyseUrine != null) {
                    ConstatRealise cstReaHeureRecueilUrine = retrieveConstatRealiseFromElementConstat(elemCstHeureRecueilUrine, null,
                            composantExamenPropertiesByElementId);
                    ConstatRealise cstReaHeureAnalyseUrine = retrieveConstatRealiseFromElementConstat(elemCstHeureAnalyseUrine, null,
                            composantExamenPropertiesByElementId);
                    if (cstReaHeureRecueilUrine != null && cstReaHeureRecueilUrine.getLocalTimeValue() != null
                            && cstReaHeureAnalyseUrine != null && cstReaHeureAnalyseUrine.getLocalTimeValue() != null) {
                        hasSucceed = !cstReaHeureRecueilUrine.getLocalTimeValue().isAfter(cstReaHeureAnalyseUrine.getLocalTimeValue());
                    }
                }
                vrr.setHasSucceed(hasSucceed);

                final ValidationError ve = new ValidationError(ErrorLevel.ERROR, CHAMP_HEURE_RECUEIL_URINES,
                        CHAMP_HEURE_RECUEIL_URINES_AFTER_ANALYSE_HOUR_ERROR_MSG);
                vrr.setValidationError(ve);
                return vrr;
            }
        };
    }

    @Override
    protected ValidationRule buildRequiredRule(ElementConstat elemConstat) {
        return createConstatRealiseValidationRule(elemConstat, false);
    }

    @Override
    protected boolean isElementConstatQuantitatif(ElementConstat elemConstat) {
        return elemConstat.isElementConstatQuantitatif();
    }

    @Override
    protected boolean isElementConstatWithVraisemblance(ElementConstat elemConstat) {
        return elemConstat.isElementConstatWithVraisemblance();
    }

    @Override
    protected Object getListeExamensRealisesFormFieldKey() {
        return epcListeExamensRealisesTableView;
    }

    @Override
    protected void updateValidationRulesIfInteractionUrines(List<ElementConstat> lsElemConstat, List<ValidationRule> validationRules) {
        Interaction interaction = getInteraction();
        if (interaction != null && interaction.getLibelleCourt() != null && LIBELLE_COURT_INTERACTION_URINES.equals(interaction.getLibelleCourt())) {
            validationRules.add(createElementConstatDateRecueilUrineValidationRule(lsElemConstat));
            validationRules.add(createElementConstatHeureRecueilUrineValidationRule(lsElemConstat));
        }
    }

    public List<ValidationRule> constructValidationRules() {
        final List<ElementConstat> lsElemConstat = isExamenMode ? lsElementsConstatExamen : lsElementsConstatInterpretation;
        return constructValidationRulesFromElementConstatList(lsElemConstat);
    }

    private boolean isControleOkElementConstatRequiredAndNotAnswered(ElementConstat elemConstat, boolean isAvantAcquisition) {
        ConstatRealise constatRealise = retrieveConstatRealiseFromElementConstat(elemConstat, null,
                isExamenMode ? composantExamenPropertiesByElementId : composantInterpretationOuLecturePropertiesByElementId);
        return !elemConstat.getIsObligatoire() || (Boolean.TRUE.equals(elemConstat.getIsObligatoireAvantAcquisition()) && !isAvantAcquisition)
                || !constatRealise.isEmptyConstatRealiseValue();
    }

    private AbstractValidationRule createConstatRealiseValidationRule(final ElementConstat elemConstat, final boolean isAvantAcquisition) {
        return new AbstractValidationRule(elemConstat.getConstat().getLibelle(),
                vr -> getFormFields().get(epcListeExamensRealisesTableView).setInError(true)) {
            @Override
            public ValidationRuleResult isValid() {
                final ValidationRuleResult vrr = new ValidationRuleResult();
                vrr.setHasSucceed(isControleOkElementConstatRequiredAndNotAnswered(elemConstat, isAvantAcquisition));
                final ValidationError ve = new ValidationError(ErrorLevel.ERROR, widgetLabel, SAISIE_CONSTAT_REQUIRED);
                vrr.setValidationError(ve);
                return vrr;
            }
        };
    }

    @FXML
    public void ajoutManuelPj() {
        if (displayedInteractionRealisee.getStatut() == EnumStatutInteraction.EN_ATTENTE_LECTURE
                || displayedInteractionRealisee.getStatut() == EnumStatutInteraction.TRANSMIS_2ND_LECTURE) {
            dialogService.openDialogConfirmation(getPrimaryStage(), AJOUT_D_UNE_PJ, MESSAGE_AJOUT_PJ,
                    dialog -> callAjoutManuelPj(), null);
        } else {
            callAjoutManuelPj();
        }
    }

    private void callAjoutManuelPj() {
        File file = launchFileChooserOperations();
        if (file != null && selectedExamenParaclinique.get() != null) {
            if (file.length() > uiPropertiesConfig.getHttpPayloadMax()) {
                consoleController.clearConsoleAndAddMessage(
                        CONSOLE_DOMAINE_PJ,
                        String.format(CONSOLE_MESSAGE_PJ_PAYLOADMAXREACHED, fileUtils.byteCountToDisplaySize(uiPropertiesConfig.getHttpPayloadMax())));
            } else {
                sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                        getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                        EnumAction.AJOUTER_PJ,
                        EnumTypeRessource.FAP,
                        idDossierPrestation.toString());

                Long idExamenParaclinique = selectedExamenParaclinique.get().getId();
                Document Document = DocumentFactory.buildDocument(file, EnumTypeDocument.PARACLINIQUE, "_" + idDossierPrestation);
                ajouterAttributsDocumentForDP(Document, getDisplayedDossierPrestationPropertyValue());
                PieceJointeExamenParaclinique pieceJointeExamenParaclinique = new PieceJointeExamenParaclinique(Document, idExamenParaclinique);
                examenParacliniqueRealiseService.ajouterPieceJointeExamenManuellement(pieceJointeExamenParaclinique);
                if (examenParacliniqueService.isInteractionExamenConnecte(affectationMaterielConnecte)) {
                    displayedInteractionRealisee.setStatut(statutInteractionAInterpreterOrLectureARealiser);
                    if (displayedInteractionRealisee.getRealisateur() == null && selectedExamenParaclinique.get().getRealisateur() != null) {
                        displayedInteractionRealisee.setRealisateur(selectedExamenParaclinique.get().getRealisateur());
                    }
                    interactionRealiseeService.updateInteractionRealisee(displayedInteractionRealisee);
                }
                operateRefreshOnAllDataFromSelectedExamOrOnPJItemsAccordingToExistModifInForm();
            }
        }
    }

    private void operateRefreshOnAllDataFromSelectedExamOrOnPJItemsAccordingToExistModifInForm() {
        if (ControllerMode.EDITION == getMode() && existsModificationsInForm()) {
            dialogService.openDialogConfirmation(getPrimaryStage(), OUTPUT_MESSAGE_TITLE, OUTPUT_MESSAGE, onConfirmAfterPJActionWithExistingModifsInForm(),
                    onCancelActionWithExistingModifsInForm());
        } else {
            refreshFormData();
        }
    }

    private Consumer<DialogConfirmationController> onConfirmAfterPJActionWithExistingModifsInForm() {
        return t -> {
            isPjOpPostRefresh = true;
            refreshFormData();
        };
    }

    private Consumer<DialogConfirmationController> onCancelActionWithExistingModifsInForm() {
        return t -> {
            final Long idSelectedExamen = selectedExamenParaclinique.get().getId();
            ExamenParacliniqueRealise examenPara = examenParacliniqueRealiseService.getExamenParacliniqueParacliniqueRealiseById(idSelectedExamen);
            initPieceJointeHBoxContentFromSpecificExamenParaclinique(examenPara);
        };
    }

    @FXML
    public void epcAnnulerExamen() {
        if (isExamenMode) {
            annulerExamenMode();
        } else {
            annulerInterpretationMode();
        }
    }

    private void annulerExamenMode() {
        if (isSoftwareLaunchOperation) {
            annulerLancementExamen();
        } else {
            annulerExamenBasicOperation();
        }
    }

    private void doAnnulerExamenPersisted() {
        GrilleDynamiqueUi composantsNonLiesAuLogiciel = retrieveNotBindedToSoftwareComponents();
        selectedExamenParaclinique.getValue().getListeConstatRealiseExamen().stream()
                .filter(cstRea -> composantsNonLiesAuLogiciel.containsKey(cstRea.getElementConstat().getId()))
                .forEach(cstRea -> composantsNonLiesAuLogiciel.putConstatRealiseValueInGrilleDynamique(cstRea));
        lecturePrerequiseChcbx.setSelected(Boolean.TRUE.equals(selectedExamenParaclinique.getValue().getIsLecturePrerequise()));
    }

    private void annulerExamenBasicOperation() {
        if (selectedExamenParaclinique.isNotNull().get()) {
            doAnnulerExamenPersisted();
        } else {
            GrilleDynamiqueUtils.resetAllValues(composantExamenPropertiesByElementId);
            lecturePrerequiseChcbx.setSelected(false);
        }
        setMode(ControllerMode.CONSULTATION);
    }

    private GrilleDynamiqueUi retrieveNotBindedToSoftwareComponents() {
        if (lsElementsConstatMateriel != null && !lsElementsConstatMateriel.isEmpty()) {
            GrilleDynamiqueUi composantsNonLiesAuLogiciel = new GrilleDynamiqueUi();
            List<Long> lsIdElementConstatMateriel = lsElementsConstatMateriel.stream().map(ElementGrilleDynamique::getId)
                    .collect(Collectors.toList());
            List<Long> lsNotElementConstatMateriel = composantExamenPropertiesByElementId.keySet().stream()
                    .filter(key -> !lsIdElementConstatMateriel.contains(key)).collect(Collectors.toList());
            lsNotElementConstatMateriel
                    .forEach(id -> composantsNonLiesAuLogiciel.put(id, composantExamenPropertiesByElementId.get(id)));
            return composantsNonLiesAuLogiciel;
        } else {
            return composantExamenPropertiesByElementId;
        }
    }

    private void annulerInterpretationMode() {
        List<ConstatRealise> lsConstatRealiseInterpretation = selectedExamenParaclinique.getValue().getListeConstatRealiseInterpretation();
        if (CollectionUtils.isNotEmpty(lsConstatRealiseInterpretation)) {
            lsConstatRealiseInterpretation
                    .forEach(cstRea -> composantInterpretationOuLecturePropertiesByElementId.putConstatRealiseValueInGrilleDynamique(cstRea));
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        } else {
            GrilleDynamiqueUtils.resetAllValues(composantInterpretationOuLecturePropertiesByElementId);
            saisirInterpretationMode(ControllerMode.CONSULTATION);
        }
        showOrHideConstatMaterielValue();
    }

    private void showOrHideConstatMaterielValue() {
        if (selectedExamenParaclinique.getValue().getAnalyste() == null) {
            List<ConstatMaterielRealise> lsConstatMaterielRealiseExamen = selectedExamenParaclinique.getValue()
                    .getListeConstatMaterielRealise();
            if (lsConstatMaterielRealiseExamen != null && !lsConstatMaterielRealiseExamen.isEmpty()
                    && !lsElemCstInterpOuLectureMateriel.isEmpty()) {
                putConstatMaterielValuesIntoInterpretationGrille(selectedExamenParaclinique.getValue());
            }
        }
    }

    void annulerLancementExamen() {
        doAnnulerExamenPersisted();
        epcAnnulerExamenBtn.setText(LIBELLE_ANNULER_DEFAUT);
        isSoftwareLaunchOperation = false;
        implementationComposantDynamique.changeAsterixToShowValue(false);

        setMode(ControllerMode.CONSULTATION);
    }

    private void reinitialiserLancementExamenMainOperation() throws MaterielConnectedAquisitionFileNotFoundException {
        String resultFilesFolderPath = affectationMaterielConnecte.getResultFilesFolderPath();
        String typeMateriel = affectationMaterielConnecte.getModele().getType();
        examenParacliniqueService.removeFilesFromPath(resultFilesFolderPath, typeMateriel);

    }

    @FXML
    public void epcImprimerExamen() {
        boolean isChangementStatutOnlyAfterGeneratePdf = !examenParacliniqueService.isInteractionExamenConnecte(affectationMaterielConnecte)
                && examenParacliniqueService.isChangementStatutOnlyAfterGeneratePdf(templateConfiguration);

        if (isChangementStatutOnlyAfterGeneratePdf) {
            Dialog3BoutonsParam dialog3BoutonsParam = new Dialog3BoutonsParam();
            dialog3BoutonsParam.setTitre(DIALOGBOX_TITRE_MODALE_IMPRIMER_AVEC_MAJ_STATUT_INTERACTION);
            dialog3BoutonsParam.setMessage(DIALOGBOX_MESSAGE_GENERATION_OU_AJOUT_PIECE_DOSSIER);
            dialog3BoutonsParam.setLibelleBoutons1(DIALOGBOX_TEXTE_BOUTON1_OUVRIR_SEULEMENT);
            dialog3BoutonsParam.setLibelleBoutons2(DIALOGBOX_TEXTE_BOUTON2_GENERER_ET_CLOTURER);
            dialog3BoutonsParam.setLibelleBoutons3(DIALOGBOX_TEXTE_BOUTON3_AJOUTER_PJ_ET_CLOTURER);
            dialog3BoutonsParam.setEnumStyleClassBouton1(EnumStyleClassBouton.LONG);
            dialog3BoutonsParam.setEnumStyleClassBouton2(EnumStyleClassBouton.LONG);
            dialog3BoutonsParam.setEnumStyleClassBouton3(EnumStyleClassBouton.LONGER);
            dialog3BoutonsParam.setMinWidthDialog(BOUTON_WIDTH_750);
            dialog3BoutonsParam.setPrefWidthDialog(BOUTON_WIDTH_750);

            dialogService.openDialog3BoutonsController(primaryStage, dialog3BoutonsParam,
                    ouvrir -> ouvrirImpressionSeulement(), generer -> ouvrirEtAjouterImpressionInListePJ(), ajouterPJ -> ajouterPJEtCloturer());
        } else {
            dialogService.openDialog2BoutonsController(primaryStage, DIALOGBOX_TITRE_MODALE_IMPRIMER_SIMPLE, DIALOGBOX_MESSAGE_ENREGISTRER_DOCUMENT,
                    DIALOGBOX_TEXTE_BOUTON1_OUVRIR_SEULEMENT, DIALOGBOX_TEXTE_BOUTON2_ENREGISTRER_DANS_DOSSIER,
                    ouvrir -> ouvrirImpressionSeulement(), ajouter -> ouvrirEtAjouterImpressionInListePJ());
        }
    }

    private void ouvrirImpressionSeulement() {
        PrintableObject<ExamenParacliniqueVo> examenParacliniqueVo = getExamenParacliniqueVoAsPrintableObject(selectedExamenParaclinique.get());
        DocumentImpressionMisas = editiqueTemplatingService.generateDocumentFromTemplateHtml(examenParacliniqueVo);

        if (DocumentImpressionMisas != null) {
            TraceVo traceVo = new TraceVo();
            final EnumEcran ecran = getEnumEcranOfExamenParaclinique();
            traceVo.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    ecran,
                    getComplementEnumEcranExamenParaclinique(ecran),
                    EnumAction.IMPRIMER,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());
            traceService.create(traceVo);

            pdfManager.showDocument(DocumentImpressionMisas, Habilitation.GERER_EXAMEN_CONSULTER_EXAMEN);
        }
    }

    private void ouvrirEtAjouterImpressionInListePJ() {
        ouvrirImpressionSeulement();
        if (selectedExamenParaclinique.get() != null && DocumentImpressionMisas != null) {

            sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                    EnumAction.AJOUTER_PJ,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());

            DocumentImpressionMisas
                    .setNom(FileUtils.getFilenameWithSuffix(
                            String.format(NOM_FICHIER_IMPRESSION, getInteraction() == null ? StringUtils.EMPTY : getInteraction().getLibelleCourt()),
                            "_" + idDossierPrestation));
            ajouterAttributsDocumentForDP(DocumentImpressionMisas, getDisplayedDossierPrestationPropertyValue());

            PieceJointeExamenParaclinique pieceJointeExamenParaclinique = new PieceJointeExamenParaclinique(DocumentImpressionMisas,
                    selectedExamenParaclinique.get().getId());

            examenParacliniqueRealiseService.ajouterPieceJointeExamenManuellement(pieceJointeExamenParaclinique);

            if (!examenParacliniqueService.isInteractionExamenConnecte(affectationMaterielConnecte)
                    && examenParacliniqueService.isChangementStatutOnlyAfterGeneratePdf(templateConfiguration)) {
                if (null != displayedInteractionRealisee.getStatut() && displayedInteractionRealisee.getStatut().isAInterpreterOrLectureARealiser()) {
                    if (existAnyInterpretationSaisieUtilisateurRenseigne(epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(null))) {
                        displayedInteractionRealisee.setStatut(EnumStatutInteraction.FAIT);
                        interactionRealiseeService.updateInteractionRealisee(displayedInteractionRealisee);
                    }
                } else if (EnumStatutInteraction.AFAIRE == displayedInteractionRealisee.getStatut()) {
                    displayedInteractionRealisee.setStatut(EnumStatutInteraction.FAIT);
                    displayedInteractionRealisee.setRealisateur(connectedUserVo);
                    suiviOrientationService.updateStatutSuivisOnSynthValidation(displayedDossierPrestationProperty.getValue().getFap().getId());
                    interactionRealiseeService.updateInteractionRealisee(displayedInteractionRealisee);
                }
            }

            refreshFormData();
        }
    }

    private void ajouterPJEtCloturer() {
        File file = launchFileChooserOperations();
        if (file != null) {
            if (file.length() > uiPropertiesConfig.getHttpPayloadMax()) {
                consoleController.clearConsoleAndAddMessage(
                        CONSOLE_DOMAINE_PJ,
                        String.format(CONSOLE_MESSAGE_PJ_PAYLOADMAXREACHED, fileUtils.byteCountToDisplaySize(uiPropertiesConfig.getHttpPayloadMax())));
            } else {
                Long idExamenParaclinique = selectedExamenParaclinique.get().getId();
                Document document = DocumentFactory.buildDocument(file, EnumTypeDocument.PARACLINIQUE, "_" + idDossierPrestation);

                ajouterAttributsDocumentForDP(document, getDisplayedDossierPrestationPropertyValue());

                examenParacliniqueRealiseService.ajouterPieceJointeExamenManuellement(new PieceJointeExamenParaclinique(document, idExamenParaclinique));

                if (doitMettreAJourStatutInteraction()) {
                    EnumStatutInteraction statut = displayedInteractionRealisee.getStatut();

                    if (statut != null && statut.isAInterpreterOrLectureARealiser()
                            && existAnyInterpretationSaisieUtilisateurRenseigne(
                                    epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(null))) {

                        displayedInteractionRealisee.setStatut(EnumStatutInteraction.FAIT);
                        interactionRealiseeService.updateInteractionRealisee(displayedInteractionRealisee);

                    } else if (EnumStatutInteraction.AFAIRE == statut) {

                        displayedInteractionRealisee.setStatut(EnumStatutInteraction.FAIT);
                        displayedInteractionRealisee.setRealisateur(connectedUserVo);

                        suiviOrientationService.updateStatutSuivisOnSynthValidation(displayedDossierPrestationProperty.getValue().getFap().getId());

                        interactionRealiseeService.updateInteractionRealisee(displayedInteractionRealisee);
                    }
                }
                refreshFormData();
            }
        }
    }

    private boolean doitMettreAJourStatutInteraction() {
        return !examenParacliniqueService.isInteractionExamenConnecte(affectationMaterielConnecte)
                && examenParacliniqueService
                        .isChangementStatutOnlyAfterGeneratePdf(templateConfiguration);
    }

    @Override
    protected Long getDisplayedInteractionId() {
        return displayedInteractionRealisee != null
                && displayedInteractionRealisee.getInteraction() != null
                        ? displayedInteractionRealisee.getInteraction().getId()
                        : null;
    }

    private PrintableObject<ExamenParacliniqueVo> getExamenParacliniqueVoAsPrintableObject(final ExamenParacliniqueRealise examenParacliniqueRealise) {
        final ExamenParacliniqueVo examenParacliniqueVo = buildExamenParacliniqueVo(examenParacliniqueRealise, displayedConsultant,
                displayedDossierPrestationProperty.getValue());
        final PrintableObject<ExamenParacliniqueVo> examenVoPrintableObject = new PrintableObject<>(examenParacliniqueVo,
                EnumPrintObject.EXAMEN_IMPRESSION,
                EnumTypeDocument.PARACLINIQUE);
        examenVoPrintableObject.setModeleDocumentId(templateConfiguration.getModeleDocumentId());
        return examenVoPrintableObject;
    }

    private ExamenParacliniqueVo buildExamenParacliniqueVo(final ExamenParacliniqueRealise examenParacliniqueRealise, final Consultant consultant,
            final DossierPrestation dossierPrestation) {
        final ExamenParacliniqueVo examenParacliniqueVo = new ExamenParacliniqueVo();
        examenParacliniqueVo.setId(examenParacliniqueRealise.getId());

        examenParacliniqueVo.setSignataireSynthese(getSignataireSynthese());
        InteractionRealisee interactionRealisee = interactionRealiseeService.getInteractionRealiseeById(examenParacliniqueRealise.getIdInteractionRealisee());
        DonneesEntretienMedicalConclusion conclusion = donneesEntretienMedicalConclusionService
                .getDonneesEntretienMedicalConclusionById(interactionRealisee.getIdPrestationRealisee());
        examenParacliniqueVo.setSignataireEntretienMedical(getSignataireEntretienMedical(conclusion));

        if (consultant != null) {
            examenParacliniqueVo.setConsultantId(consultant.getId());
            examenParacliniqueVo.setEntiteRattachementId(consultant.getCesRattachement());
        }

        if (dossierPrestation != null) {
            examenParacliniqueVo.setDossierPrestationId(dossierPrestation.getId());
            examenParacliniqueVo.setDatePrestation(dossierPrestation.getDateDemarrage());
        }

        if (CollectionUtils.isNotEmpty(examenParacliniqueRealise.getListeConstatRealiseExamen())) {
            examenParacliniqueRealise.getListeConstatRealiseExamen()
                    .forEach(constat -> examenParacliniqueVo.getConstatRealiseExamenMap().putIfAbsent(CONSTAT +
                            constat.getElementConstat().getConstat().getLibelleCourt(),
                            ConstatRealiseHelper.getConstatRealiseResultatStringValue(constat)));
        }
        if (CollectionUtils.isNotEmpty(examenParacliniqueRealise.getListeConstatRealiseInterpretation())) {
            examenParacliniqueRealise.getListeConstatRealiseInterpretation()
                    .forEach(constat -> examenParacliniqueVo.getConstatRealiseInterpretationExamenMap().putIfAbsent(CONSTAT +
                            constat.getElementConstat().getConstat().getLibelleCourt(),
                            ConstatRealiseHelper.getConstatRealiseResultatStringValue(constat)));
        }
        if (CollectionUtils.isNotEmpty(examenParacliniqueRealise.getListeConstatMaterielRealise())) {
            examenParacliniqueRealise.getListeConstatMaterielRealise().forEach(constat -> putConstatMaterielRealiseInMap(examenParacliniqueVo, constat));
        }

        return examenParacliniqueVo;
    }

    private void putConstatMaterielRealiseInMap(ExamenParacliniqueVo examenParacliniqueVo, ConstatMaterielRealise constatMaterielRealise) {
        if (constatMaterielRealise != null && constatMaterielRealise.getIdConstatPublie() != null) {
            ConstatPublie constatPublie = constatPublieService.getConstatPublieById(constatMaterielRealise.getIdConstatPublie());
            if (constatPublie != null && StringUtils.isNotBlank(constatPublie.getConstatJsonValue())) {
                Constat constat = JsonUtils.convertJsonStringToObject(constatPublie.getConstatJsonValue(), Constat.class);
                constat.setLibelle(constat.getLibelle() + CONSTAT_MATERIEL_SUFFIXE);
                ElementConstat elemConstat = new ElementConstat();
                elemConstat.setConstat(constat);
                ConstatRealise constatRea = new ConstatRealise();
                constatRea.setElementConstat(elemConstat);
                constatRea.setIdExamenParacliniqueRealise(examenParacliniqueVo.getId());
                constatRea.setTextValue(constatMaterielRealise.getTextValue());
                constatRea.setNumericValue(constatMaterielRealise.getNumericValue());

                examenParacliniqueVo.getConstatMaterielRealiseMap().putIfAbsent(CONSTAT +
                        constatRea.getElementConstat().getConstat().getLibelleCourt(),
                        ConstatRealiseHelper.getConstatRealiseResultatStringValue(constatRea));
            }
        }
    }

    @FXML
    public void epcSupprimerExamen() {
        if (interactionHabilitationHandler
                .checkHabilitationSupprimerExamen(getDisplayedInteractionRealiseeInteraction())) {
            supprimerExamenTreatment();
        } else {
            afficherErreurNonHabilite(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_SUPPRESSION_EXAMEN.getLibelle());
        }
    }

    private void supprimerExamenTreatment() {
        if (checkSupprimerExamenConditions()) {
            dialogService.openDialogConfirmation(getPrimaryStage(), DELETE_EXAMEN_DIALOGBOX_TITLE,
                    DELETE_EXAMEN_DIALOGBOX_MESSAGE, getOnAcceptDeleteDialogBoxCallBack(), null);
        } else {
            showSupprimerExamenErrorMessage();
        }
    }

    private boolean checkSupprimerExamenConditions() {
        return getDisplayedInteractionRealiseeInteraction().getIsSupprimable();
    }

    private void showSupprimerExamenErrorMessage() {
        getErrorMessageHandler().clear();
        ValidationError error = new ValidationError(ERROR_SUPPRESSION_EXAMEN_LBL, ERROR_SUPPRESSION_EXAMENT_MSG);
        getErrorMessageHandler().addErreur(error);
        consoleController.refreshDataAndShow();
    }

    private Consumer<DialogConfirmationController> getOnAcceptDeleteDialogBoxCallBack() {
        return t -> {
            ExamenParacliniqueRealise examenParaclinique = selectedExamenParaclinique.get();
            spinnerService.executeTask(() -> {

                sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                        getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                        EnumAction.SUPPRIMER,
                        EnumTypeRessource.FAP,
                        idDossierPrestation.toString());

                examenParacliniqueRealiseService.deleteExamenParacliniqueById(examenParaclinique.getId());
                return StringUtils.EMPTY;
            }, empty -> deleteExamenPostOperations(examenParaclinique), primaryStage);
        };
    }

    private void deleteExamenPostOperations(ExamenParacliniqueRealise removedExamenParaclinique) {
        ExamenParacliniqueRealise examenRealisePrecedent = epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(removedExamenParaclinique);
        EnumStatutInteraction oldStatut = displayedInteractionRealisee.getStatut();
        EnumStatutInteraction newStatut = EnumStatutInteraction.AFAIRE;
        boolean isRealisateurInteractionRealiseeUpddated = true;
        if (examenRealisePrecedent != null) {
            idExamenToSelect = examenRealisePrecedent.getId();
            newStatut = calculStatutInteractionRealiseeOfExamenRealisePrecedent(examenRealisePrecedent);
            if (CollectionUtils.isNotEmpty(examenRealisePrecedent.getListePiecesJointes())) {
                examenParacliniqueRealiseService.updateInclusCrOfPiecesJointes(Collections.singletonList(idExamenToSelect), true);
            }
            isRealisateurInteractionRealiseeUpddated = majRealisateurSiStatutAutorise(connectedUserVoSupplier(),
                    EnumStatutInteraction.STATUTS_NECESSITANT_REALISATEUR, displayedInteractionRealisee);
        } else {
            idExamenToSelect = null;
            displayedInteractionRealisee.setRealisateur(null);
        }
        if (!oldStatut.equals(newStatut) || isRealisateurInteractionRealiseeUpddated) {
            displayedInteractionRealisee.setStatut(newStatut);
            displayedInteractionRealisee = updateInteractionRealisee(displayedInteractionRealisee);
        }
        refreshFormData();
        habilitationEpcNouvelExamenBtn();
    }

    private EnumStatutInteraction calculStatutInteractionRealiseeOfExamenRealisePrecedent(ExamenParacliniqueRealise examenRealisePrecedent) {
        EnumStatutInteraction enumStatutInteraction = EnumStatutInteraction.AFAIRE;
        if (Boolean.TRUE.equals(examenRealisePrecedent.isInterprete())
                || !EnumInteraction.isExamenConnecte(displayedInteractionRealiseeInteraction.getLibelleCourt())) {
            enumStatutInteraction = EnumStatutInteraction.FAIT;
        } else if (CollectionUtils.isNotEmpty(examenRealisePrecedent.getListePiecesJointes())) {
            enumStatutInteraction = EnumStatutInteraction.A_INTERPRETER;
        }
        return enumStatutInteraction;
    }

    @FXML
    public void epcModifierExamen() {
        if (interactionHabilitationHandler.checkHabilitationModifierExamen(getDisplayedInteractionRealiseeInteraction())) {
            launchModifyOperations();
        } else {
            afficherErreurNonHabilite(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_MODIFICATION_EXAMEN.getLibelle());
        }
    }

    public void launchModifyOperations() {
        clearAndShowSpecificWarningMessageForDPTraiteWithClotureAvecEmSyntheseParam(getDisplayedDossierPrestationPropertyValue());
        editedExam = epcListeExamensRealisesTableView.getSelectionModel().getSelectedItem();
        setMode(ControllerMode.EDITION);
    }

    @FXML
    public void epcEnregistrer() {
        if (validate(constructValidationRules())) {
            launchRegisteringOperations();
        }
    }

    private void launchRegisteringOperations() {
        DossierPrestation displayedDossierPrestation = getDisplayedDossierPrestationPropertyValue();
        if (null != displayedDossierPrestation && EnumStatutDossierPrestation.isStatutTraite(displayedDossierPrestation.getStatut())) {
            operateRegisterOrDeleteAfterConfirmation(dialog -> enregistrerTreatment(), displayedDossierPrestation);
        } else {
            enregistrerTreatment();
        }
    }

    @Override
    protected boolean isSuppressionPieceJointeAutorisee() {
        return examenParacliniqueService.isSuppressionPieceJointeAutorisee(getStatutDossierPrestationAccordingToDPProperty());
    }

    @Override
    protected String getSupprimerPjCss() {
        return SUPPRIMER_PJ_CSS;
    }

    private void enregistrerTreatment() {
        if (isExamenMode) {
            enregistrerExamen();
        } else {
            enregistrerInterpretation();
        }
    }

    @Override
    protected ExamenParacliniqueRealise registerExamenDataBaseOperation(boolean isNouveauExamenPara) {
        ExamenParacliniqueRealise examenParacliniqueRealise = null;
        if (isNouveauExamenPara) {
            sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                    EnumAction.NOUVEL_EXAMEN,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());
            ExamenParacliniqueRealise examenRealise = beforeInsertExamenParacliniqueOperations();
            examenParacliniqueRealise = examenParacliniqueRealiseService.saveExamenParacliniqueRealise(examenRealise);
            if (CollectionUtils.isEmpty(examenRealise.getListePiecesJointes())) {
                // reset des cases a cocher des PJ des exams réalisés, dans l'EM
                List<ExamenParacliniqueRealiseVo> listExamenParacliniqueRealise = examenParacliniqueRealiseService
                        .getListeExamParacliniqueRealiseByIdInteractReal(examenRealise.getIdInteractionRealisee());
                if (CollectionUtils.isNotEmpty(listExamenParacliniqueRealise)) {
                    List<Long> listIdExamParacliniqueRealise = listExamenParacliniqueRealise.stream().map(ExamenParacliniqueRealiseVo::getId)
                            .collect(Collectors.toList());
                    examenParacliniqueRealiseService.updateInclusCrOfPiecesJointes(listIdExamParacliniqueRealise, false);
                }
            }
        } else {
            sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                    EnumAction.MODIFIER,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());
            ExamenParacliniqueRealise examenRealise = beforeUpdateExamenParacliniqueOperations();
            examenParacliniqueRealise = examenParacliniqueRealiseService.updateExamenParacliniqueRealise(examenRealise);
        }
        return examenParacliniqueRealise;
    }

    private void showEmptyInterpretationInsertError(String errorLibelle, String errorMessage) {
        getErrorMessageHandler().clear();
        ValidationError error = new ValidationError(errorLibelle, errorMessage);
        getErrorMessageHandler().addErreur(error);
        consoleController.refreshDataAndShow();
    }

    private void enregistrerInterpretation() {
        boolean isEquals = true;
        boolean isInsert = isInsertInterpretCase();
        List<ConstatRealise> lsConstatRea = isInsert ? beforeInsertInterpretationOperations() : beforeUpdateInterpretationOperations();
        boolean areEmptyConstatRea = ConstatRealiseHelper.areEmptyConstatReaValues(isEquals, lsConstatRea);
        if (isInsert) {
            addInterpretationToDatabaseCases(areEmptyConstatRea, lsConstatRea);
        } else {
            removeOrUpdateInterpretationToDatabase(lsConstatRea);
        }
    }

    private boolean isInsertInterpretCase() {
        boolean result = false;
        if (lsElemCstInterpOuLectureMateriel.isEmpty()) {
            result = !selectedExamenParaclinique.get().isInterprete();
        } else {
            result = isInsertWithCstInterpMaterielCase(selectedExamenParaclinique.get().getInterpreteur() != null);
        }
        return result;
    }

    private boolean isInsertWithCstInterpMaterielCase(boolean isAnalysed) {
        boolean result = !isAnalysed;
        if (isAnalysed && CollectionUtils.isEmpty(selectedExamenParaclinique.get().getListeConstatRealiseInterpretation())) {
            result = true;
        } else if (isAnalysed && areEmptyAllInterpretationFields(
                !selectedExamenParaclinique.get().getListeConstatRealiseInterpretation().isEmpty())) {
            result = !selectedExamenParaclinique.get().isInterprete();
        }
        return result;
    }

    private boolean areEmptyAllInterpretationFields(boolean isUpdate) {
        List<ConstatRealise> lsConstatRealises = getListInterpretationConstatRealise(
                selectedExamenParaclinique.get().getId(), isUpdate);
        boolean isEquals = true;
        return ConstatRealiseHelper.areEmptyConstatReaValues(isEquals, lsConstatRealises);
    }

    private void addInterpretationToDatabaseCases(boolean areAllConstatsRealisesAreEmpty, List<ConstatRealise> lsConstatRea) {
        boolean isNotAnalyse = selectedExamenParaclinique.get().getAnalyste() == null;
        boolean isModifIntoMaterielPart = !isModifIntoInterpretationDataInitAndMaterielCase(null, lsConstatRea) && isNotAnalyse;
        List<ConstatMaterielRealise> lsCMR = selectedExamenParaclinique.get().getListeConstatMaterielRealise();
        boolean wasExamenLaunched = lsCMR != null && !lsCMR.isEmpty();

        boolean isExamenMaterielDeleteCase = !lsElemCstInterpOuLectureMateriel.isEmpty() && areEmptyInterpretationData(lsConstatRea) && isNotAnalyse
                && wasExamenLaunched;
        if (!areAllConstatsRealisesAreEmpty || isModifIntoMaterielPart) {
            addInterpretationDatabase(lsConstatRea, areAllConstatsRealisesAreEmpty, isExamenMaterielDeleteCase);
        } else {
            showEmptyInterpretationInsertError(ERROR_INTERPRETATION_LBL, ERROR_INTERPRETATION_MSG);
        }
    }

    private void addInterpretationDatabase(List<ConstatRealise> lsConstatRea, boolean areEmptyConstatsRealises, boolean isMaterielDelCase) {
        spinnerService.executeTask(() -> {

            sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                    EnumAction.AJOUTER_INTERPRETATION,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());

            return examenParacliniqueRealiseService.saveExamenParacliniqueRealiseInterpretation(lsConstatRea, connectedUser, areEmptyConstatsRealises);
        }, examenRealise -> addInterpretationToDatabasePostOperations(examenRealise, isMaterielDelCase), primaryStage);
    }

    private void addInterpretationToDatabasePostOperations(ExamenParacliniqueRealise examenRealise, boolean isMaterielDelCase) {
        if (selectedExamenParaclinique.get().equals(epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(null)) && !isMaterielDelCase) {
            updateInteractionRealiseeForNewMainExamenParaclinique(examenRealise);
        }
        if (examenRealise != null) {
            idExamenToSelect = examenRealise.getId();
        }
        saisirInterpretationMode(ControllerMode.CONSULTATION);
        refreshFormData();
    }

    private void removeOrUpdateInterpretationToDatabase(List<ConstatRealise> lsConstatRea) {
        boolean isEmptyLsConstatReaInterpOuLecture = ConstatRealiseHelper.isListConstatRealiseEmpty(lsConstatRea);
        spinnerService.executeTask(() -> removeOrUpdateInterpretationDatabaseCases(isEmptyLsConstatReaInterpOuLecture, lsConstatRea),
                examenRealise -> removeOrUpdateInterpretationPostOperations(isEmptyLsConstatReaInterpOuLecture, examenRealise), primaryStage);
    }

    private ExamenParacliniqueRealise removeOrUpdateInterpretationDatabaseCases(boolean isEmptyLsConstat, List<ConstatRealise> lsConstatRea) {
        if (isEmptyLsConstat) {
            sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                    EnumAction.SUPPRIMER_INTERPRETATION,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());

            return examenParacliniqueRealiseService.deleteExamenParacliniqueRealiseInterpretation(lsConstatRea);
        } else {
            sessionContext.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE,
                    getEnumEcranOfExamenParaclinique(), getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                    EnumAction.MODIFIER_INTERPRETATION,
                    EnumTypeRessource.FAP,
                    idDossierPrestation.toString());
            Utilisateur existingInterp = selectedExamenParaclinique.get().getInterpreteur();
            return examenParacliniqueRealiseService.updateExamenParacliniqueRealiseInterpretation(lsConstatRea, existingInterp);
        }
    }

    private void removeOrUpdateInterpretationPostOperations(boolean isEmptyLsConstatReaInterpretation, ExamenParacliniqueRealise examenRealise) {
        boolean isLastExamen = selectedExamenParaclinique.get().equals(epcListeExamensRealisesTableView.getLastCreatedExamenParacliniqueRealise(null));
        boolean isExamenRealiseNotNull = examenRealise != null;
        updateButtonsAccordingToLastExamen(isLastExamen);
        if (isLastExamen) {
            updateInteractionRealiseeForNewMainExamenParaclinique(examenRealise);
        }
        if (isExamenRealiseNotNull) {
            idExamenToSelect = examenRealise.getId();
        }
        saisirInterpretationMode(ControllerMode.CONSULTATION);
        if (!isEmptyLsConstatReaInterpretation && isExamenRealiseNotNull && examenRealise.equals(selectedExamenParaclinique.get())) {
            epcListeExamensRealisesTableView.scrollTableViewToSelectedExamenParacliniqueRow(idExamenToSelect);
        } else {
            refreshFormData();
        }
    }

    private List<ConstatRealise> beforeInsertInterpretationOperations() {
        Long idExamenParacliniqueRealise = selectedExamenParaclinique.getValue().getId();
        return getListInterpretationConstatRealise(idExamenParacliniqueRealise, false);
    }

    @Override
    protected List<ConstatRealise> getInterpretationInsertCaseList() {
        return getListConstatsRealisesInterpretationOuLectureInsertCase();
    }

    private ExamenParacliniqueRealise beforeInsertExamenParacliniqueOperations() {
        ExamenParacliniqueRealise examenParaRealise = new ExamenParacliniqueRealise();
        examenParaRealise.setRealisateur(connectedUserVo);
        examenParaRealise.setDateCreation(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        examenParaRealise.setIdInteractionRealisee(displayedInteractionRealisee.getId());
        examenParaRealise.setInterprete(false);
        String infoMaterielUtiliseJsonString = JsonUtils
                .convertObjectToJsonString(getInfoMaterielUtiliseFromLsAffectationMaterielNew(lsAffectationMaterielUtilisableToExamen));
        examenParaRealise.setInfoMaterielUtilise(infoMaterielUtiliseJsonString);
        examenParaRealise.setListeConstatRealiseExamen(getListExamenConstatRealise());
        examenParaRealise.setIsLecturePrerequise(lecturePrerequiseChcbx.isSelected());
        return examenParaRealise;
    }

    private ExamenParacliniqueRealise beforeUpdateExamenParacliniqueOperations() {
        ExamenParacliniqueRealise examenParaRealise = selectedExamenParaclinique.getValue();
        LocalDateTime actualDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        if (null == examenParaRealise.getDatePremiereModification()) {
            examenParaRealise.setDatePremiereModification(actualDateTime);
        }
        examenParaRealise.setDateModification(actualDateTime);
        List<ConstatRealise> lsConstatsRealises = examenParaRealise.getListeConstatRealiseExamen();
        List<ConstatRealise> lsConstatsRealisesMaj = lsConstatsRealises.stream()
                .map(ctr -> retrieveConstatRealiseFromElementConstat(ctr.getElementConstat(), ctr.getId(), composantExamenPropertiesByElementId))
                .collect(Collectors.toList());
        examenParaRealise.setListeConstatRealiseExamen(lsConstatsRealisesMaj);
        examenParaRealise.setIsLecturePrerequise(lecturePrerequiseChcbx.isSelected());
        return examenParaRealise;
    }

    private List<ConstatRealise> getListExamenConstatRealise() {
        List<ConstatRealise> constatRealises = new ArrayList<>();
        if (!lsElementsConstatExamen.isEmpty()) {
            constatRealises = lsElementsConstatExamen.stream()
                    .map(cs -> retrieveConstatRealiseFromElementConstat(cs, null, composantExamenPropertiesByElementId))
                    .collect(Collectors.toList());
        }
        return constatRealises;
    }

    List<ConstatRealise> getListInterpretationConstatRealise(Long idExamenParacliniqueRealise, boolean isUpdate) {
        lsConstatRealiseInterpretation = new ArrayList<>();
        if (!isUpdate) {
            lsConstatRealiseInterpretation = getListConstatsRealisesInterpretationOuLectureInsertCase();
        } else {
            getListConstatsRealisesUpdateCase();
        }
        lsConstatRealiseInterpretation.forEach(constatReal -> constatReal.setIdExamenParacliniqueRealise(idExamenParacliniqueRealise));
        return lsConstatRealiseInterpretation;
    }

    private List<ConstatRealise> getListConstatsRealisesInterpretationOuLectureInsertCase() {
        return lsElementsConstatInterpretation.stream().map(
                cs -> retrieveConstatRealiseFromElementConstat(cs, null, composantInterpretationOuLecturePropertiesByElementId))
                .collect(Collectors.toList());
    }

    @Override
    protected List<ElementConstat> getInterpretationElements() {
        return lsElementsConstatInterpretation;
    }

    @Override
    protected Map<Long, ElementGrilleDynamiqueComposantProperties> getInterpretationPropertiesByElementId() {
        return composantInterpretationOuLecturePropertiesByElementId;
    }

    @FXML
    public void epcNouvelExamen() {
        final EnumErreurLancementExamen retourFaisabiliteExamen = verifierFaisabiliteExamen();
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        if (sessionContext.getListeAffectationsMateriel() != null
                && interaction.getReferencesTypeMateriel() != null
                && !interaction.getReferencesTypeMateriel().isEmpty()
                && !EnumErreurLancementExamen.AUCUNE_ERREUR.equals(retourFaisabiliteExamen)) {
            dialogService.openDialogAlerte(null, "Erreur utilisation matériel",
                    String.format(retourFaisabiliteExamen.getLibelleErreur(), sessionContext.getSalle().getNom()));
        } else {
            if (interactionHabilitationHandler.checkHabilitationRealiserExamen(interaction)) {
                constatVerificationBeforeOperation();
            } else {
                afficherErreurNonHabilite(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_REALISATION_EXAMEN.getLibelle());
            }
        }
    }

    private EnumErreurLancementExamen verifierFaisabiliteExamen() {
        EnumErreurLancementExamen faisabilite = EnumErreurLancementExamen.AUCUNE_ERREUR;
        final List<Reference> listeTypesMaterielInteraction = getDisplayedInteractionRealiseeInteraction().getReferencesTypeMateriel();

        final Set<String> listeNomsMaterielValideSession = listeAffectationsMaterielToSetOfStrings(sessionContext.getListeAffectationsMateriel());
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
        final Predicate<AffectationMateriel> justeLeMaterielNecessairePourInteraction = am -> listeNomsMaterielNecessairesInteraction
                .contains(am.getModele().getType());
        return sessionContext.getListeAffectationsMateriel().stream().filter(justeLeMaterielNecessairePourInteraction)
                .anyMatch(PREDICAT_MATERIEL_UTILISABLE.negate());
    }

    private boolean isContientMaterielUtilisable(final Set<String> listeNomsMaterielNecessairesInteraction) {
        final Predicate<AffectationMateriel> justeLeMaterielNecessairePourInteraction = am -> listeNomsMaterielNecessairesInteraction
                .contains(am.getModele().getType());
        final List<AffectationMateriel> contientMaterielInutilisable = sessionContext.getListeAffectationsMateriel().stream()
                .filter(justeLeMaterielNecessairePourInteraction).filter(AffectationMateriel::isUtilisable).collect(Collectors.toList());
        Set<String> typeMaterielUtilisableNecessaire = new HashSet<>();
        for (AffectationMateriel affectationMateriel : contientMaterielInutilisable) {
            typeMaterielUtilisableNecessaire.add(affectationMateriel.getModele().getType());
        }
        return listeNomsMaterielNecessairesInteraction.equals(typeMaterielUtilisableNecessaire);
    }

    private Set<String> listeTypeMaterielsToSetOfStrings(final List<Reference> listeTypesMateriel) {
        return listeTypesMateriel.stream().map(Reference::getIntitule).collect(Collectors.toSet());
    }

    private Set<String> listeAffectationsMaterielToSetOfStrings(final List<AffectationMateriel> listeAffectationsMateriel) {
        return listeAffectationsMateriel.stream().map(item -> item.getModele().getType()).collect(Collectors.toSet());
    }

    private void constatVerificationBeforeOperation() {
        if (isNouvelExamenOperationVerif()) {
            initNouvelExamenOperation();
        } else {
            clearConsoleAndAddMessage(ERROR_AUCUN_CONSTAT_LBL, ERROR_AUCUN_CONSTAT);
        }
    }

    private boolean isNouvelExamenOperationVerif() {
        return !lsElementsConstatExamen.isEmpty() || !lsElementsConstatMateriel.isEmpty();
    }

    private void initNouvelExamenOperation() {
        if (checkNouvelExamenConditions()) {
            launchNewExamenOperationWithWarningIfStatutDPTraite();
        } else {
            clearConsoleAndAddMessage(ERROR_PLUSIEURS_TENT_LBL, ERROR_PLUSIEURS_TENT_MSG);
        }
    }

    public void launchNewExamenOperationWithWarningIfStatutDPTraite() {
        clearAndShowSpecificWarningMessageForDPTraiteWithClotureAvecEmSyntheseParam(getDisplayedDossierPrestationPropertyValue());
        epcListeExamensRealisesTableView.getSelectionModel().clearSelection();
        editedExam = null;
        displayedInteractionRealisee.setStatut(EnumStatutInteraction.AFAIRE);
        if (examensParacliniqueAlreadyExisting.isEmpty()) {
            if (displayedPrestationRealiseeDonneesRealisees != null) {
                displayedPrestationRealiseeDonneesRealisees.getConstatRealises().stream().map(ResultatConstat::new)
                        .forEach(this::applyResultatConstatIntoGrilles);
            }

            MoteurRegleResult reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getMoteurRegleContextParaclinique());
            composantExamenPropertiesByElementId.refreshWithContext(getConsultantEntite(),
                    displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
            composantInterpretationOuLecturePropertiesByElementId.refreshWithContext(getConsultantEntite(),
                    displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
            refreshReglesAffichageEtConstats(reglesResult);
        } else {
            rowSelectionOperations(null);
        }
        implementationComposantDynamique.changeAsterixToShowValue(false);
        setMode(ControllerMode.EDITION);
    }

    private DonneesRealisees getDonneesRealiseesInteraction() {
        DonneesRealisees donneesRealiseesInteraction = new DonneesRealisees();
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        List<ConstatRealiseWithOrigineInformationsVo> constatRealiseWithOrigineInfos = getListConstatRealiseExamen()
                .stream()
                .map(constatRealise -> ConstatRealiseWithOrigineInformationsVoFactory
                        .addOrigineInformationsToConstatRealise(constatRealise,
                                interaction.getLibelleCourt(),
                                UtilisateurVoFactory.buildUtilisateurVo(sessionContext.getConnectedUser())))
                .collect(Collectors.toList());
        donneesRealiseesInteraction.setConstatRealises(constatRealiseWithOrigineInfos);

        List<ConstatRealiseWithOrigineInformationsVo> constatRealiseInterpretationInsertWithOrigineInfos = getListConstatsRealisesInterpretationOuLectureInsertCase()
                .stream()
                .map(constatRealise -> ConstatRealiseWithOrigineInformationsVoFactory
                        .addOrigineInformationsToConstatRealise(constatRealise,
                                interaction.getLibelleCourt(),
                                UtilisateurVoFactory.buildUtilisateurVo(sessionContext.getConnectedUser())))
                .collect(Collectors.toList());

        donneesRealiseesInteraction.getConstatRealises().addAll(constatRealiseInterpretationInsertWithOrigineInfos);

        if (displayedPrestationRealiseeDonneesRealisees != null) {
            List<ConstatRealiseWithOrigineInformationsVo> constatRealiseNotAlreadyPresent = displayedPrestationRealiseeDonneesRealisees
                    .getConstatRealises().stream()
                    .filter(constatRealise -> donneesRealiseesInteraction.getConstatRealises().stream()
                            .noneMatch(constatRealiseInteraction -> constatRealiseInteraction.getElementConstat()
                                    .getConstat().equals(constatRealise.getElementConstat().getConstat())))
                    .collect(Collectors.toList());
            donneesRealiseesInteraction.getConstatRealises().addAll(constatRealiseNotAlreadyPresent);

            donneesRealiseesInteraction.getConstatMaterielRealises()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getConstatMaterielRealises());
            donneesRealiseesInteraction.getAnteceFamiliaux()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getAnteceFamiliaux());
            donneesRealiseesInteraction.getAntecePersoMedicaux()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getAntecePersoMedicaux());
            donneesRealiseesInteraction.getAntecePersoOperes()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getAntecePersoOperes());
            donneesRealiseesInteraction.getMaladiesConnues()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getMaladiesConnues());
            donneesRealiseesInteraction.getQuestionReponseQuestionRealisees()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getQuestionReponseQuestionRealisees());
            donneesRealiseesInteraction.getSignesFonctionnels()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getSignesFonctionnels());
            donneesRealiseesInteraction.getSignesPhysiques()
                    .addAll(displayedPrestationRealiseeDonneesRealisees.getSignesPhysiques());
        }
        return donneesRealiseesInteraction;
    }

    private void applyResultatConstatIntoGrilles(ResultatConstat constatResult) {
        if (composantExamenPropertiesByElementId != null) {
            composantExamenPropertiesByElementId.applyResultatConstatIntoGrille(constatResult);
        }
        if (composantInterpretationOuLecturePropertiesByElementId != null) {
            composantInterpretationOuLecturePropertiesByElementId.applyResultatConstatIntoGrille(constatResult);
        }
    }

    private boolean checkNouvelExamenConditions() {
        Interaction interaction = getDisplayedInteractionRealiseeInteraction();
        return interaction.getIsPlusieursTentative() || (!interaction.getIsPlusieursTentative() && epcListeExamensRealisesTableView.getItems().isEmpty());
    }

    @FXML
    public void epcSaisirInterpretation() {
        if (interactionHabilitationHandler.checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction())) {
            launchAddInterpretationOperationsWithWarningIfStatutDPTraite();
        } else {
            afficherErreurNonHabilite(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_SAISIE_INTERPRETATION.getLibelle());
        }
    }

    public void launchAddInterpretationOperationsWithWarningIfStatutDPTraite() {
        clearAndShowSpecificWarningMessageForDPTraiteWithClotureAvecEmSyntheseParam(getDisplayedDossierPrestationPropertyValue());
        editedExam = epcListeExamensRealisesTableView.getSelectionModel().getSelectedItem();
        saisirInterpretationMode(ControllerMode.EDITION);
    }

    private void saisirInterpretationMode(ControllerMode mode) {
        if (mode.equals(ControllerMode.EDITION)) {
            saisirInterpretationEditionModeCaseOperations();
        } else {
            isExamenMode = true;
            saisirInterpretationConsultationModeCaseOperations();
        }
    }

    private void saisirInterpretationEditionModeCaseOperations() {
        interpretationControllerMode = ControllerMode.EDITION;
        isExamenMode = false;
        setLecturePrerequiseChildrenDisableValue();
        Stream.of(epcModifierExamenBtn, epcSupprimerExamenBtn, epcImprimerExamenBtn, epcInterpreterBtn, epcLancerExamenBtn)
                .forEach(btn -> btn.disableProperty().unbind());
        Stream.of(epcSupprimerExamenBtn, epcModifierExamenBtn, epcImprimerExamenBtn, epcNouvelExamenBtn, epcInterpreterBtn,
                epcAcquisitionResultatsBtn, epcLancerExamenBtn).forEach(btn -> setDisable(btn, true));
        Stream.of(epcAnnulerExamenBtn, epcAjoutManuelPJBtn)
                .forEach(btn -> setDisable(btn, false));
        epcEnregistrerExamenBtn.setDisable(!interactionHabilitationHandler.checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction()));
        implementationComposantDynamique.switchModeTo(ControllerMode.EDITION, composantInterpretationOuLecturePropertiesByElementId);
    }

    private void saisirInterpretationConsultationModeCaseOperations() {
        interpretationControllerMode = ControllerMode.CONSULTATION;

        setDisable(epcSupprimerExamenBtn, isSupprimerExamenNotAllowed(getDisplayedInteractionRealiseeInteraction()));
        setDisable(epcNouvelExamenBtn, !isNouvelExamenBtnAllowed());
        if (isLectureMode) {
            setDisable(epcInterpreterBtn, !actionManager.hasHabilitation(Habilitation.GERER_EXAMEN_LECTURE_EXAMEN) || !lecturePrerequiseChcbx.isSelected());
        } else {
            setDisable(epcInterpreterBtn, !interactionHabilitationHandler.checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction()));
        }
        setDisable(epcModifierExamenBtn, !interactionHabilitationHandler.checkHabilitationModifierExamen(getDisplayedInteractionRealiseeInteraction()));
        setDisable(epcImprimerExamenBtn, !interactionHabilitationHandler.checkHabilitationImprimerExamen(getDisplayedInteractionRealiseeInteraction()));

        epcSupprimerExamenBtn.disableProperty().bind(supprimerBtnBinding());

        Stream.of(epcModifierExamenBtn, epcImprimerExamenBtn).forEach(btn -> btn.disableProperty()
                .bind(epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty().isNull()));
        epcInterpreterBtn.disableProperty().bind(interpreterBtnBinding().not());
        Stream.of(epcEnregistrerExamenBtn, epcAnnulerExamenBtn, epcAcquisitionResultatsBtn, epcAjoutManuelPJBtn)
                .forEach(btn -> setDisable(btn, true));
        BooleanBinding canLancerExamenBeLaunched = Bindings.createBooleanBinding(canLancerExamenOperationBeLaunched(),
                epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty());
        epcLancerExamenBtn.disableProperty().bind(canLancerExamenBeLaunched.not());
        implementationComposantDynamique.switchModeTo(ControllerMode.CONSULTATION, composantInterpretationOuLecturePropertiesByElementId);
    }

    private BooleanBinding interpreterBtnBinding() {
        return Bindings.createBooleanBinding(() -> {
            boolean isModifiyingAllowed = isModifiyingAllowed();
            boolean isSelectedItem = epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty().isNotNull().get();
            boolean isInterpretationOuLectureItem = lsElementsConstatInterpretation.emptyProperty().not().get();
            boolean isHabilitationInterpreterOuLecture = !isLectureMode ? interactionHabilitationHandler
                    .checkHabilitationSaisirInterpretation(getDisplayedInteractionRealiseeInteraction())
                    : (actionManager.hasHabilitation(Habilitation.GERER_EXAMEN_LECTURE_EXAMEN) && lecturePrerequiseChcbx.isSelected());
            return isModifiyingAllowed && isSelectedItem && isInterpretationOuLectureItem && isHabilitationInterpreterOuLecture;
        }, epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty(), lsElementsConstatInterpretation.emptyProperty());
    }

    private BooleanBinding supprimerBtnBinding() {
        return Bindings.createBooleanBinding(() -> {
            boolean isNoSelectedItem = epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty().isNull().get();
            boolean isStateNotAllowed = isInteractionRealiseTransmis2ndLectureOrEnAttenteLectureState(displayedInteractionRealisee);
            return isNoSelectedItem || isStateNotAllowed;
        }, epcListeExamensRealisesTableView.getSelectionModel().selectedItemProperty());
    }

    private boolean isNouvelExamenBtnAllowed() {
        boolean isModifiyingAllowed = isModifiyingAllowed();
        boolean isHabilitationRealised = interactionHabilitationHandler
                .checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction());
        boolean isStateAllowed = displayedInteractionRealisee == null
                || !isInteractionRealiseTransmis2ndLectureOrEnAttenteLectureState(displayedInteractionRealisee);
        return isModifiyingAllowed && isHabilitationRealised && isStateAllowed;
    }

    private boolean isSupprimerExamenNotAllowed(Interaction interaction) {
        return !interactionHabilitationHandler.checkHabilitationSupprimerExamen(interaction)
                || isInteractionRealiseTransmis2ndLectureOrEnAttenteLectureState(displayedInteractionRealisee);
    }

    private Spinner getSpinner() {
        Spinner spinner;
        Pane mainPane = (Pane) primaryStage.getScene().getRoot();
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

    private void operateAcquisitionResultatTracing() {
        TraceVo traceVo = new TraceVo();
        traceVo.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE, EnumEcran.EXAMEN_PARACLINIQUE,
                getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                EnumAction.ACQUISITION_RESULTAT,
                EnumTypeRessource.FAP,
                idDossierPrestation.toString());
        traceService.create(traceVo);
    }

    @FXML
    public void epcAcquisitionResultats() {
        operateAcquisitionResultatTracing();

        if (validate(constructValidationRulesAvantAcquisition())) {
            // DANS CE CAS SEULEMENT, AJOUT DE MODIFICATIONS SUR LE CSS PROPRE AU SPINNER
            if (null != getPrimaryStage() && null != getPrimaryStage().getScene()) {
                mainSpinner = getSpinner();
                if (null != mainSpinner && !mainSpinner.getStyleClass().contains(CSS_CLASS_SPINNER_TEXT)) {
                    mainSpinner.getStyleClass().add(CSS_CLASS_SPINNER_TEXT);
                }
            }

            if (isFormatEchangeHL7()) {
                spinnerService.executeTaskWithText(this::acquerirResultatsHl7MaterielConnecte,
                        this::executePostAcquisitionResultatsHL7OnSpecificCase, PREPARATION_FICHIERS, primaryStage);
            } else {
                spinnerService.executeTaskWithText(this::aquisitionProgrammeEtudes,
                        this::executePostAcquisitionResultatsOnSpecificCase, PREPARATION_FICHIERS, primaryStage);
            }
        }
    }

    private boolean acquerirResultatsHl7MaterielConnecte() {
        boolean isOk = false;
        try {
            Map<String, List<ElementConstat>> mapElementConstat = getElementsConstatMapper();
            mapElementConstat.put(EnumTypeElementConstat.ELEMENT_CONSTAT_EXAMEN_OBLIGATOIRE_AVANT_ACQUISITION.getCode(),
                    getListElementConstatsObligatoiresAvantAcquisition());

            ExamenParacliniqueRealise examenParacliniqueRealiseToUpdate = beforeUpdateExamenParacliniqueOperations();
            InteractionRealisee interactionRealiseeToUpdate = postAcquisitionGetInteractionRealiseeToUpdate();

            examenParacliniqueRealiseService.acquerirResultatsHl7MaterielConnecte(affectationMaterielConnecte,
                    examenParacliniqueRealiseToUpdate,
                    interactionRealiseeToUpdate,
                    mapElementConstat,
                    idConsultant,
                    idDossierPrestation);
            isOk = true;
        } catch (Hl7MessageReaderException e) {
            LOGGER.warn(e.getMessage(), e);
            List<ValidationError> errors = e.getErreurs().stream().map(error -> new ValidationError(ACQUISITION_EXAMEN, error))
                    .collect(Collectors.toList());
            clearAddListOfValidationErrorAndShowConsole(errors);
        } catch (MaterielConnectedAquisitionResultsEmptyException e) {
            LOGGER.warn(e.getMessage(), e);
            clearAddValidationErrorAndShowConsole(new ValidationError(ErrorLevel.WARNING, ACQUISITION_EXAMEN, NOT_ALL_RESULT_FILES));
        } catch (MaterielConnectedAquisitionFileNotFoundException | FileNotFoundInDirectoryException e) {
            LOGGER.warn(e.getMessage(), e);
            clearConsoleAndAddMessage(ACQUISITION_EXAMEN, e.getMessage());
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            clearConsoleAndAddMessage(ACQUISITION_EXAMEN, "Erreur : " + e.getMessage());
        }
        return isOk;
    }

    private boolean isFormatEchangeHL7() {
        return affectationMaterielConnecte.getModele().getFormatEchange().isHl7();
    }

    private List<ValidationRule> constructValidationRulesAvantAcquisition() {
        List<ValidationRule> listValidationRules = new ArrayList<>();
        List<ElementConstat> listElementConstatsObligatoiresAvantAcquisition = getListElementConstatsObligatoiresAvantAcquisition();
        if (CollectionUtils.isNotEmpty(listElementConstatsObligatoiresAvantAcquisition)) {
            listElementConstatsObligatoiresAvantAcquisition
                    .forEach(elementConstat -> createConstatRealiseAvantAcquisitionValidationRule(listValidationRules, elementConstat));
        }
        return listValidationRules;
    }

    private void createConstatRealiseAvantAcquisitionValidationRule(List<ValidationRule> listValidationRules, ElementConstat elemConstat) {
        listValidationRules.add(createConstatRealiseValidationRule(elemConstat, true));
    }

    // Que la valeur retournée par la première opération soit true ou false, la méthode executeTaskWithText lance
    // la seconde opération. De ce fait, mise en place d'une vérification avant le lancement de la seconde opération
    private void executePostAcquisitionResultatsOnSpecificCase(Boolean isOk) {
        if (Boolean.TRUE.equals(isOk)) {
            epcPostAcquisitionResultatsOperations();
        }
        supprimerTextSpinner();
    }

    private void executePostAcquisitionResultatsHL7OnSpecificCase(Boolean isOk) {
        if (Boolean.TRUE.equals(isOk)) {
            refreshDonneesAfterAcquisitionData();
        }
        supprimerTextSpinner();
    }

    private void supprimerTextSpinner() {
        if (null != getPrimaryStage() && null != mainSpinner) {
            mainSpinner.getStyleClass().remove(CSS_CLASS_SPINNER_TEXT);
        }
    }

    private void executeSleepingOnThread() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private Boolean aquisitionProgrammeEtudes() {
        if (null != getPrimaryStage() && null != mainSpinner) {
            executeSleepingOnThread();
        }

        String programmeEtudeId = fapDataRdv != null ? fapDataRdv.getIdProgrammeEtude() : null;

        if (programmeEtudeId == null) {
            programmeEtudeId = idConsultantStr;
        }

        return executeAcquisitionAndSaveOperationWithData(programmeEtudeId);
    }

    private Map<String, List<ElementConstat>> getElementsConstatMapper() {
        Map<String, List<ElementConstat>> elementsConstatMapper = new HashMap<>();
        elementsConstatMapper.put("E", lsElementsConstatMateriel);
        elementsConstatMapper.put("I", lsElemCstInterpOuLectureMateriel);
        return elementsConstatMapper;
    }

    public void removeBindingWithSessionContextProperties() {
        isRunningListChangeAffMatProperty.unbind();
    }

    private Boolean executeAcquisitionAndSaveOperationWithData(String programmeEtudeId) {
        try {
            boolean isFullOperationRealised = examenParacliniqueRealiseService.retrieveAndSaveResultsFromFile(affectationMaterielConnecte,
                    selectedExamenParaclinique.get(), programmeEtudeId, getElementsConstatMapper());
            if (!isFullOperationRealised) {
                clearAddValidationErrorAndShowConsole(new ValidationError(ErrorLevel.WARNING, ACQUISITION_EXAMEN, NOT_ALL_RESULT_FILES));
            }
            return Boolean.TRUE;

        } catch (MaterielConnectedAquisitionFileNotFoundException e) {
            LOGGER.warn(e.getMessage(), e);
            clearConsoleAndAddMessage(ACQUISITION_EXAMEN, e.getMessage());
            return Boolean.FALSE;
        }
    }

    protected void epcPostAcquisitionResultatsOperations() {
        selectedExamenParaclinique.get().setListePiecesJointes(new ArrayList<>());
        if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
            try {
                List<Document> listeDocuments = new ArrayList<>();
                switch (affectationMaterielConnecte.getModele().getType()) {
                    case AUDIOMETRE:
                        creationEtRecupPJAudiometrie(listeDocuments);
                        break;
                    case ECG:
                        creationEtRecupPjECG(listeDocuments);
                        break;
                    case SPIROMETRE:
                        examenParacliniqueService.creationEtRecupPJSpirometrie(listeDocuments, affectationMaterielConnecte, idDossierPrestation,
                                this::ajouterAttributsDocument);
                        break;
                    default:
                        LOGGER.warn("Type de matériel inconnu : {}", affectationMaterielConnecte.getModele().getType());
                        break;
                }
                updateStatutAInterpreterEtRealisateurInteractionRealisee(listeDocuments);
                listeDocuments.forEach(Doc -> storePJ(converterSage2DocToDocExamen.convert(Doc)));

                if (isConstatsObligatoiresAvantAcquisitionPresent()) {
                    registerExamenDataBaseOperation(false);
                }
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
                clearConsoleAndAddMessage(ERROR_PIECES_JOINTES_LBL, ERROR_RECUPERATION_PJ_MSG);
            }
        }

        refreshDonneesAfterAcquisitionData();
    }

    private void refreshDonneesAfterAcquisitionData() {
        Long idExamenToSelect = selectedExamenParaclinique.get().getId();
        epcAnnulerExamenBtn.setText(LIBELLE_ANNULER_DEFAUT);
        isSoftwareLaunchOperation = false;
        setMode(ControllerMode.CONSULTATION);
        refreshFormData();
        MoteurRegleResult reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getMoteurRegleContextParaclinique());
        composantExamenPropertiesByElementId.refreshWithContext(getConsultantEntite(),
                displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
        composantInterpretationOuLecturePropertiesByElementId.refreshWithContext(getConsultantEntite(),
                displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
        if (idExamenToSelect != null) {
            notEmptyRowSelectionInterpAndMatOperations(selectedExamenParaclinique.get());
        }
        refreshReglesAffichageEtConstats(reglesResult);
        supprimerTextSpinner();
    }

    private List<ElementConstat> getListElementConstatsObligatoiresAvantAcquisition() {
        return getAvailableElementConstatListFromFullList(lsElementsConstatExamen).stream()
                .filter(eltConstat -> Boolean.TRUE.equals(eltConstat.getIsObligatoireAvantAcquisition()))
                .collect(Collectors.toList());
    }

    private boolean isConstatsObligatoiresAvantAcquisitionPresent() {
        List<ElementConstat> lsElementsConstatAvailable = getListElementConstatsObligatoiresAvantAcquisition();
        return CollectionUtils.isNotEmpty(lsElementsConstatAvailable);
    }

    private InteractionRealisee postAcquisitionGetInteractionRealiseeToUpdate() {
        final InteractionRealisee clonedInteractionRealisee = (InteractionRealisee) displayedInteractionRealisee.clone();
        clonedInteractionRealisee.setStatut(statutInteractionAInterpreterOrLectureARealiser);
        majRealisateurSiStatutAutorise(connectedUserVoSupplier(), EnumStatutInteraction.STATUTS_NECESSITANT_REALISATEUR, clonedInteractionRealisee);
        if (UtilisateurVoFactory.getUtilisateurVoSystem().equals(clonedInteractionRealisee.getInitiateur())) {
            clonedInteractionRealisee.setInitiateur(null);
        }
        return clonedInteractionRealisee;
    }

    private void updateStatutAInterpreterEtRealisateurInteractionRealisee(List<Document> listeDocuments) {
        if (CollectionUtils.isNotEmpty(listeDocuments)) {
            displayedInteractionRealisee.setStatut(statutInteractionAInterpreterOrLectureARealiser);
            majRealisateurSiStatutAutorise(connectedUserVoSupplier(), EnumStatutInteraction.STATUTS_NECESSITANT_REALISATEUR, displayedInteractionRealisee);
            displayedInteractionRealisee = updateInteractionRealisee(displayedInteractionRealisee);
        }
    }

    private Document creationDocumentPdfEcg(File file, String fileNameSuffix) {
        if (file != null && EnumTypeDocument.PARACLINIQUE != null && fileNameSuffix != null) {
            Document documentToUpload = new Document();
            try {
                byte[] pdfEcgAvecInsertionRectangeBlanc = fileUtils.cacherPartieDocPdf(Files.readAllBytes(file.toPath()));
                documentToUpload.setContent(pdfEcgAvecInsertionRectangeBlanc);
            } catch (IOException e) {
                throw new ModelTechnicalException("Une erreur est survenue lors de la lecture du fichier", e);
            }
            documentToUpload.setType(EnumTypeDocument.PARACLINIQUE);
            documentToUpload.setNom(FileUtils.getFilenameWithSuffix(file.getName(), fileNameSuffix));
            return documentToUpload;
        } else {
            return null;
        }
    }

    private void creationEtRecupPjECG(List<Document> listeDocuments) {
        File resultFileRootFolder = new File(affectationMaterielConnecte.getResultFilesFolderPath());
        List<Path> pieceJointes = new ArrayList<>();

        // traitement specifique aux pdf ECG
        // ***************************
        List<Path> listePathsFichiersPdfEcg = fileUtils.getMatchingFilesFromFileNameFilterInDirectory(resultFileRootFolder,
                (dir, fileName) -> EnumFormatDocument.PDF.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName)));

        listePathsFichiersPdfEcg.forEach(file -> {
            Document Doc = creationDocumentPdfEcg(file.toFile(), "_" + idDossierPrestation);
            if (Doc != null) {
                Doc.setUserCreationId(ClientContextManager._SYSTEM_USER_ID);
                ajouterAttributsDocument(Doc);
                listeDocuments.add(editiqueStorageService.createDocument(Doc));
            }
        });

        File resultFileFolder = new File(resultFileRootFolder.toPath().resolve("origin").toString());
        pieceJointes.addAll(fileUtils.getMatchingFilesFromFileNameFilterInDirectory(resultFileFolder,
                (dir, fileName) -> EnumFormatDocument.XML.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName))));
        File anonymizedResultFileFolder = new File(resultFileRootFolder.toPath().resolve("anonymized").toString());
        List<Path> listPath = fileUtils.getMatchingFilesFromFileNameFilterInDirectory(anonymizedResultFileFolder,
                (dir, fileName) -> EnumFormatDocument.XML.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName)));
        listPath.forEach(file -> changeFileNameAnonymized(anonymizedResultFileFolder, file));
        pieceJointes.addAll(fileUtils.getMatchingFilesFromFileNameFilterInDirectory(anonymizedResultFileFolder,
                (dir, fileName) -> EnumFormatDocument.XML.getType().equalsIgnoreCase(FilenameUtils.getExtension(fileName))));
        pieceJointes.forEach(file -> {
            Document Doc = DocumentFactory.buildDocument(file.toFile(), EnumTypeDocument.PARACLINIQUE, "_" + idDossierPrestation);
            Doc.setUserCreationId(ClientContextManager._SYSTEM_USER_ID);
            ajouterAttributsDocument(Doc);
            listeDocuments.add(editiqueStorageService.createDocument(Doc));
        });
    }

    private void changeFileNameAnonymized(File anonymizedResultFileFolder, Path file) {
        try {
            Files.move(file, anonymizedResultFileFolder.toPath().resolve("anonyme_" + file.getFileName()));
        } catch (IOException e) {
            LOGGER.warn(ERROR_RECUPERATION_PJ_MSG, e);
            clearConsoleAndAddMessage(ERROR_PIECES_JOINTES_LBL, ERROR_RECUPERATION_PJ_MSG);
        }
    }

    private void creationEtRecupPJAudiometrie(List<Document> listeDocuments) throws IOException {
        try (Stream<Path> pathStream = Files.walk(Paths.get(affectationMaterielConnecte.getResultFilesFolderPath()), 1)) {
            List<Path> listeFichiers = pathStream.filter(path -> !Files.isDirectory(path)).collect(Collectors.toList());
            listeFichiers
                    .forEach(fichier -> {
                        Document Doc = DocumentFactory.buildDocument(fichier.toFile(), EnumTypeDocument.PARACLINIQUE,
                                "_" + idDossierPrestation);
                        Doc.setUserCreationId(ClientContextManager._SYSTEM_USER_ID);
                        ajouterAttributsDocument(Doc);
                        listeDocuments.add(editiqueStorageService.createDocument(Doc));
                    });
        }
    }

    private void storePJ(DocumentPieceJointe document) {
        PieceJointeExamen pj = new PieceJointeExamen(document.getId(), selectedExamenParaclinique.get().getId());
        examenParacliniqueRealiseService.createPieceJointeExamen(pj);
        selectedExamenParaclinique.get().getListePiecesJointes().add(document);
    }

    private void operateLaunchExamenTracing() {
        TraceVo traceVo = new TraceVo();
        traceVo.setTrace(EnumFonction.REALISER_EXAMEN_PARACLINIQUE, EnumEcran.EXAMEN_PARACLINIQUE,
                getComplementEnumEcranExamenParaclinique(getEnumEcranOfExamenParaclinique()),
                EnumAction.LANCER_EXAMEN,
                EnumTypeRessource.FAP,
                idDossierPrestation.toString());
        traceService.create(traceVo);
    }

    @FXML
    public void epcLancerExamen() throws InterruptedException {
        operateLaunchExamenTracing();
        if (interactionHabilitationHandler.checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction())) {
            lancerExamenWithMaterielDataVerification();
        } else {
            afficherErreurNonHabilite(EnumExamenManqueHabilitation.ABSENCE_DE_DROIT_DE_REALISATION_EXAMEN.getLibelle());
        }
    }

    private void lancerExamenWithMaterielDataVerification() throws InterruptedException {
        List<ValidationError> errors = new ArrayList<>();
        if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
            errors = examenParacliniqueService
                    .verifyDataInAffectMatConnecteForCommandLineAndPathsOfPatientFileAndResultFileFolders(affectationMaterielConnecte);
        }
        if (errors.isEmpty()) {
            lancerExamenMainOperation();
        } else {
            clearAddListOfValidationErrorAndShowConsole(errors);
        }
    }

    private void lancerExamenMainOperation() throws InterruptedException {
        try {
            if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
                reinitialiserLancementExamenMainOperation();
            }
            genererFichePatient();
        } catch (MaterielConnectedAquisitionFileNotFoundException e) {
            LOGGER.warn(LOGICIEL_CIBLE_NON_INSTALLER_MAL_CONFIGURER, e);
            clearConsoleAndAddMessage(LANCEMENT_EXAMEN, LOGICIEL_CIBLE_NON_INSTALLER_MAL_CONFIGURER);
        }
        if (isConnectedExamen.get() && affectationMaterielConnecte.getCommandLine() != null) {
            launchExamNormalOrTestCase();
        }
    }

    private void genererFichePatient() {
        FichePatientCriteria fichePatientCriteria = genererFichePatientCriteria();
        examenParacliniqueRealiseService.genererFichePatient(fichePatientCriteria);
    }

    private void launchExamNormalOrTestCase() throws InterruptedException {
        if (getPrimaryStage() != null && getPrimaryStage().getScene() != null) {
            launchExamNormalCase();
        } else {
            setUiModeRecupAndSoftwareLaunchOk();
        }
    }

    private void launchExamNormalCase() throws InterruptedException {
        File dossierFichierPatient = getDossierFichierPatient(affectationMaterielConnecte.getPatientFileFolderPath());
        if (null != dossierFichierPatient.list() && dossierFichierPatient.list().length > 0) {
            try {
                lancerApplication();
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
                clearConsoleAndAddMessage(ERROR_LIGNE_COMMANDE_LBL, ERROR_LIGNE_COMMANDE_MSG);
            }
        } else {
            clearConsoleAndAddMessage(ERROR_FICHIER_PATIENT_LBL, ERROR_FICHIER_PATIENT_MSG);
        }
    }

    private void afficherErreurNonHabilite(final String message) {
        clearConsoleAndAddMessage("Habilitation", message);
    }

    private void lancerApplication() throws InterruptedException, IOException {
        lanceurApplicationService.launchApplication(affectationMaterielConnecte.getCommandLine(), EnumFormatDocument.EXE.getExtension());
        String resultFilesFolderPath = affectationMaterielConnecte.getResultFilesFolderPath();
        File dossierResultats = new File(resultFilesFolderPath);
        if (dossierResultats.list().length > 0) {
            setUiModeRecupAndSoftwareLaunchOk();
            editedExam = epcListeExamensRealisesTableView.getSelectionModel().getSelectedItem();
        } else {
            clearConsoleAndAddMessage(ERROR_CONTENEUR_RESULTATS_LBL, ERROR_CONTENEUR_RESULTATS_MSG);
        }
    }

    private FichePatientCriteria genererFichePatientCriteria() {
        final FichePatientCriteria fichePatientCriteria = new FichePatientCriteria();
        final Consultant consultant = consultantService.getConsultantById(super.idConsultant);
        idConsultantStr = consultant != null ? consultant.getId().toString() : null;
        fichePatientCriteria.setConsultant(consultant);
        fichePatientCriteria.setUtilisateur(sessionContext.getConnectedUser());
        fichePatientCriteria.setFapDataRdv(fapDataRdv);
        fichePatientCriteria.setMateriel(affectationMaterielConnecte);
        fichePatientCriteria.setCesExecution(displayedDossierPrestationProperty.getValue().getEntite());
        renseignerFichePatientByFormatEchangeMateriel(fichePatientCriteria);
        return fichePatientCriteria;
    }

    private void renseignerFichePatientByFormatEchangeMateriel(final FichePatientCriteria fichePatientCriteria) {
        if (fichePatientCriteria.isFormatEchangeMaterielHl7()) {
            renseignerByConstatCodeLoinc(fichePatientCriteria);
        } else {
            renseignerPoidsEtTaille(fichePatientCriteria);
        }
    }

    private void renseignerPoidsEtTaille(final FichePatientCriteria fichePatientCriteria) {
        if (configurationModeleEcran != null) {
            Optional<ConstatParacliniqueConfiguration> optionalPoidsTailleConfiguration = configurationModeleEcran
                    .getConfigurationBlocs().stream()
                    .filter(configBloc -> EnumBlocEcranDynamiqueAutres.PARACLINIQUE_CONSTATS
                            .equals(configBloc.getBlocEcranDynamique()))
                    .map(ConstatParacliniqueConfiguration.class::cast).findFirst();
            if (optionalPoidsTailleConfiguration.isPresent()) {
                MoteurRegleContext contextInteraction = getMoteurRegleContextParaclinique();
                if (optionalPoidsTailleConfiguration.get().getPoids() != null && contextInteraction.getConstatContext()
                        .containsKey(optionalPoidsTailleConfiguration.get().getPoids().getLibelleCourt())) {
                    String poidsValue = contextInteraction.getConstatContext()
                            .get(optionalPoidsTailleConfiguration.get().getPoids().getLibelleCourt());
                    if (poidsValue != null) {
                        fichePatientCriteria.setPoids(Double.parseDouble(poidsValue));
                    }
                }
                if (optionalPoidsTailleConfiguration.get().getTaille() != null && contextInteraction.getConstatContext()
                        .containsKey(optionalPoidsTailleConfiguration.get().getTaille().getLibelleCourt())) {
                    String tailleValue = contextInteraction.getConstatContext()
                            .get(optionalPoidsTailleConfiguration.get().getTaille().getLibelleCourt());
                    if (tailleValue != null) {
                        fichePatientCriteria.setTaille(Integer.parseInt(tailleValue));
                    }
                }
            }
        }
    }

    private void renseignerByConstatCodeLoinc(final FichePatientCriteria fichePatientCriteria) {
        MoteurRegleContext contextInteraction = getMoteurRegleContextParaclinique();
        String poidsValue = searchConstatRealiseValueByCodeLoinc(contextInteraction, EnumConstatCodeLoinc.CONSTAT_POIDS_CODE_LOINC);
        if (poidsValue != null) {
            fichePatientCriteria.setPoids(Double.parseDouble(poidsValue));
        }
        String tailleValue = searchConstatRealiseValueByCodeLoinc(contextInteraction, EnumConstatCodeLoinc.CONSTAT_TAILLE_CODE_LOINC);
        if (tailleValue != null) {
            fichePatientCriteria.setTaille(Integer.parseInt(tailleValue));
        }
        // La valeur du constat est oui/non ou vide
        String fumeur = searchConstatRealiseValueByCodeLoinc(contextInteraction, EnumConstatCodeLoinc.CONSTAT_FUMER_CODE_LOINC);
        fichePatientCriteria.setFumeur(EnumOuiNonNspEnCours.toBoolean(fumeur));
        // La valeur du constat est oui/non ou vide
        String ancienFumeur = searchConstatRealiseValueByCodeLoinc(contextInteraction, EnumConstatCodeLoinc.CONSTAT_ANCIEN_FUMEUR_CODE_LOINC);
        fichePatientCriteria.setAncienFumeur(EnumOuiNonNspEnCours.toBoolean(ancienFumeur));
    }

    private String searchConstatRealiseValueByCodeLoinc(final MoteurRegleContext contextInteraction, final EnumConstatCodeLoinc loinc) {
        return Optional.ofNullable(displayedPrestationRealiseeDonneesRealisees)
                .map(DonneesRealisees::getConstatRealises)
                .stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(ConstatRealiseWithOrigineInformationsVo::getElementConstat)
                .filter(Objects::nonNull)
                .map(ElementConstat::getConstat)
                .filter(Objects::nonNull)
                .filter(constat -> loinc != null
                        && loinc.getCodeLoinc().equals(constat.getCodeLOINC())
                        && contextInteraction.getConstatContext().containsKey(constat.getLibelleCourt()))
                .map(constat -> contextInteraction.getConstatContext().get(constat.getLibelleCourt()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private File getDossierFichierPatient(String folderPath) {
        return new File(folderPath);
    }

    private void setUiModeRecupAndSoftwareLaunchOk() {
        epcAnnulerExamenBtn.setWrapText(true);
        epcAnnulerExamenBtn.setText(LIBELLE_ANNULER_EXAMEN);
        setDisable(epcAnnulerExamenBtn, false);
        setDisable(epcAcquisitionResultatsBtn,
                !interactionHabilitationHandler.checkHabilitationRealiserExamen(getDisplayedInteractionRealiseeInteraction()));

        Stream.of(epcModifierExamenBtn, epcSupprimerExamenBtn, epcImprimerExamenBtn, epcInterpreterBtn, epcLancerExamenBtn)
                .forEach(btn -> btn.disableProperty().unbind());
        Stream.of(epcNouvelExamenBtn, epcEnregistrerExamenBtn, epcSupprimerExamenBtn, epcImprimerExamenBtn, epcModifierExamenBtn,
                epcInterpreterBtn, epcLancerExamenBtn, epcAjoutManuelPJBtn).forEach(btn -> setDisable(btn, true));

        setModeOfConstatsObligatoiresAvantAcquisition(ControllerMode.EDITION);

        isSoftwareLaunchOperation = true;
        implementationComposantDynamique.changeAsterixToShowValue(true);
    }

    private void setModeOfConstatsObligatoiresAvantAcquisition(ControllerMode controllerMode) {
        if (composantExamenPropertiesByElementId != null) {
            List<ElementConstat> lsElementsConstatAvailable = getAvailableElementConstatListFromFullList(lsElementsConstat);
            lsElementsConstatAvailable.stream()
                    .filter(eltConstat -> Boolean.TRUE.equals(eltConstat.getIsObligatoireAvantAcquisition()))
                    .forEach(eltConstat -> composantExamenPropertiesByElementId.get(eltConstat.getId()).setMode(controllerMode));
        }
    }

    private ExamenParacliniqueMaterielUtilise getInfoMaterielUtiliseFromLsAffectationMaterielNew(List<AffectationMateriel> lsAffectation) {
        return new ExamenParacliniqueMaterielUtiliseBuilder()
                .withIdSalle(salle.getId())
                .withNomSale(salle.getNom())
                .withListAffectationMateriel(lsAffectation).build();
    }

    private void retrieveConfigurationModeleEcran() {
        ConfigurationModeleEcranVo configEcranVo = displayedInteractionRealisee.getInteraction().getConfigurationEcrans().get(0);
        configurationModeleEcran = prestationService.getConfigurationModeleEcranById(configEcranVo.getId());
        createGrilleDynamiqueFromConfigurationModeleEcran(EnumBlocEcranDynamiqueGrilleDynamique.CONSTATS_MESURES);

        isLectureMode = isBlocEcranDynamiqueGrilleDynamiqueHasElements(EnumBlocEcranDynamiqueGrilleDynamique.CONSTATS_LECTURE);
        if (isLectureMode) {
            createGrilleDynamiqueFromConfigurationModeleEcran(EnumBlocEcranDynamiqueGrilleDynamique.CONSTATS_LECTURE);
        } else {
            createGrilleDynamiqueFromConfigurationModeleEcran(EnumBlocEcranDynamiqueGrilleDynamique.CONSTATS_INTERPRETATION);
        }
        loadRules();
    }

    private boolean isBlocEcranDynamiqueGrilleDynamiqueHasElements(EnumBlocEcranDynamiqueGrilleDynamique blocEcranDyn) {
        List<ConfigurationBloc> grilles = configurationModeleEcran.getListOfConfigurationBlocFromBlocEcranDynamiqueGrilleDynamique(blocEcranDyn);
        return grilles.stream().anyMatch(grille -> CollectionUtils.isNotEmpty(((GrilleDynamique) grille).getElements()));
    }

    private void createGrilleDynamiqueFromConfigurationModeleEcran(EnumBlocEcranDynamiqueGrilleDynamique blocEcranDyn) {
        List<ConfigurationBloc> grilles = configurationModeleEcran.getListOfConfigurationBlocFromBlocEcranDynamiqueGrilleDynamique(blocEcranDyn);
        grilles.forEach(grille -> createGrilleDynamiqueFromConfigurationBlocList(blocEcranDyn, grille));
    }

    protected void generateInterpretationGrilleDynamique() {
        retrieveElementsConstatFromInterpretationGrilleDynamique();
        composantInterpretationOuLecturePropertiesByElementId = implementationComposantDynamique
                .generateGrilleDynamiqueIntoGridPane(interpretationGrilleDynamique, interpretationGridPane, getPrimaryStage().getScene());
        operateColSpanOnAllInterpretationGridPaneCellsWhichContainConstatsAvailableToMaximumSizing();
        implementationComposantDynamique.switchModeTo(ControllerMode.CONSULTATION, composantInterpretationOuLecturePropertiesByElementId);
        retrieveLibelleAndRegleAffichage(interpretationGrilleDynamique, composantInterpretationOuLecturePropertiesByElementId);
        isListenerEnableProperty.set(true);
        initElemCstPropValChangeListener(lsElementsConstatInterpretation, composantInterpretationOuLecturePropertiesByElementId);
    }

    private void retrieveElementsConstatFromInterpretationGrilleDynamique() {
        lsElementsConstatInterpretation.setValue(interpretationGrilleDynamique.getElements().stream()
                .filter(ElementConstat.class::isInstance).map(ElementConstat.class::cast)
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
        lsElemCstInterpOuLectureMateriel = lsElementsConstatInterpretation.stream()
                .filter(ecm -> ecm.getConstat().getAppareilConnecte() != null)
                .collect(Collectors.toList());
    }

    private boolean verifyAvailabiltyForMaximumSizingOfConstat(List<ElementConstat> lsElementsConstat, ElementConstat elementConstat) {
        int col = elementConstat.getNumeroColonne();
        int row = elementConstat.getNumeroLigne();
        Constat constat = elementConstat.getConstat();

        boolean isMaxSizeExpected = EnumListeFormatConstat.QUALITATIF_TEXTE_LIBRE == constat.getFormResult()
                && constat.getMotifNonRealise() == null && col < 3;
        if (isMaxSizeExpected) {
            Optional<ElementConstat> nextConstat = lsElementsConstat.stream()
                    .filter(elemConstat -> col < elemConstat.getNumeroColonne() && row == elemConstat.getNumeroLigne()).findFirst();
            isMaxSizeExpected = !nextConstat.isPresent();
        }
        return isMaxSizeExpected;
    }

    private void operateColSpanOnAllExamGridPaneCellsWhichContainConstatsAvailableToMaximumSizing() {
        lsElementsConstat.forEach(elemCst -> examenGridPane.operateColumnSpanOnExamGridPaneCellsWhichContainsOneSpecificConstat(lsElementsConstat, elemCst));
        lsElementsConstat.stream()
                .filter(elemCst -> verifyAvailabiltyForMaximumSizingOfConstat(lsElementsConstat, elemCst))
                .forEach(elemCst -> operateColumnSpanOnGridPaneCellsWhichContainsOneSpecificConstat(elemCst, examenGridPane));
    }

    private void operateColSpanOnAllInterpretationGridPaneCellsWhichContainConstatsAvailableToMaximumSizing() {
        lsElementsConstatInterpretation.stream()
                .filter(elemCst -> verifyAvailabiltyForMaximumSizingOfConstat(lsElementsConstatInterpretation, elemCst))
                .forEach(elemCst -> operateColumnSpanOnGridPaneCellsWhichContainsOneSpecificConstat(elemCst, interpretationGridPane));
    }

    private void operateColumnSpanOnGridPaneCellsWhichContainsOneSpecificConstat(ElementConstat elemConstat, GridPane gridpane) {
        Integer numColCstExpected = elemConstat.getNumeroColonne() + 1;
        Integer numRowCstExpected = elemConstat.getNumeroLigne();
        gridpane.getChildren().stream().filter(node -> GridPane.getColumnIndex(node) != null && GridPane.getRowIndex(node) != null)
                .filter(node -> numColCstExpected.equals(GridPane.getColumnIndex(node)) && numRowCstExpected.equals(GridPane.getRowIndex(node)))
                .forEach(node -> GridPane.setColumnSpan(node, (numColCstExpected == 1 ? 3 : 2)));
    }

    protected void generateExamenGrilleDynamique() {
        composantExamenPropertiesByElementId = implementationComposantDynamique.generateGrilleDynamiqueIntoGridPane(
                examenGrilleDynamique, examenGridPane, getPrimaryStage().getScene());
        retrieveElementsConstatFromExamenGrilleDynamique();
        operateColSpanOnAllExamGridPaneCellsWhichContainConstatsAvailableToMaximumSizing();
        retrieveLibelleAndRegleAffichage(examenGrilleDynamique, composantExamenPropertiesByElementId);
        lsElementsConstatMateriel.forEach(element -> composantExamenPropertiesByElementId.applyModePropertyChangeListenerOnGridPaneElement(element));
        isListenerEnableProperty.set(true);
        initElemCstPropValChangeListener(lsElementsConstat, composantExamenPropertiesByElementId);
    }

    private void retrieveElementsConstatFromExamenGrilleDynamique() {
        lsElementsConstat = examenGrilleDynamique.getElements().stream().filter(ElementConstat.class::isInstance)
                .map(ElementConstat.class::cast).collect(Collectors.toList());
        lsElementsConstatExamen = lsElementsConstat.stream()
                .filter(ecm -> ecm.getConstat().getAppareilConnecte() == null).collect(Collectors.toList());
        lsElementsConstatMateriel = lsElementsConstat.stream()
                .filter(ecm -> ecm.getConstat().getAppareilConnecte() != null).collect(Collectors.toList());
    }

    private void retrieveLibelleAndRegleAffichage(GrilleDynamique grille, GrilleDynamiqueUi grilleUi) {
        List<LibelleRegleAffichage> lsLibRegleAff = grille.getElements().stream()
                .filter(LibelleRegleAffichage.class::isInstance).map(LibelleRegleAffichage.class::cast)
                .collect(Collectors.toList());
        reglesAffichageElementComposantPropertiesMap
                .putAll(lsLibRegleAff.stream().collect(Collectors.toMap(LibelleRegleAffichage::getId,
                        libelleRegleAffichage -> grilleUi.get(libelleRegleAffichage.getId()))));
        lsLibelleRegleAffichage.addAll(lsLibRegleAff);
    }

    private void checkUsableBeforeExecuteReglesAffichageEtCalcul(final ElementSaisieGrilleDynamiqueComposantProperties composantProperties, Object oldValue,
            boolean reexecuteRules) {
        if (Boolean.TRUE.equals(composantProperties.isUsable())) {
            // Execution du moteur de regle et refresh du formulaire de l'ecran
            MoteurRegleResult reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getMoteurRegleContextParaclinique());
            if (isControleValiditeResultatsRegleDeCalculOk(reglesResult)) {
                composantExamenPropertiesByElementId.refreshWithContext(getConsultantEntite(),
                        displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
                composantInterpretationOuLecturePropertiesByElementId.refreshWithContext(getConsultantEntite(),
                        displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);

                // Si au moins un element ayant une valeur saisie a ete demodule, alors supprimer cette precedente
                // valeur
                // Si une regle de calcul existe que sa valeur est null ou 0 alors supprimer la precedente valeur
                resetElementsGrilleDynamiUDemodulesValuesInEditionMode(composantExamenPropertiesByElementId, lsElementsConstat, reglesResult);
                // Rafraichissement des donnees de l'ecran calculees par le moteur de regles
                refreshReglesAffichageEtConstats(reglesResult);
            } else {
                composantProperties.setValue(oldValue);
            }
        } else if (!listenedUsableProperties.contains(composantProperties.usableProperty())) {
            composantProperties.usableProperty().addListener(getComposantPropertiesUusablePropertyListener(composantProperties, oldValue));
            listenedUsableProperties.add(composantProperties.usableProperty());
        }
    }

    private ChangeListener<Boolean> getComposantPropertiesUusablePropertyListener(ElementSaisieGrilleDynamiqueComposantProperties composantProperties,
            Object oldValue) {
        return new ChangeListener<Boolean>() {
            @Override
            public void changed(final ObservableValue<? extends Boolean> observableUsableProperty, final Boolean oldValueUsableProperty,
                    final Boolean newValueUsableProperty) {
                if (Boolean.TRUE.equals(newValueUsableProperty)) {
                    observableUsableProperty.removeListener(this);
                    listenedUsableProperties.remove(observableUsableProperty);
                    MoteurRegleResult reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getMoteurRegleContextParaclinique());
                    if (isControleValiditeResultatsRegleDeCalculOk(reglesResult)) {
                        composantExamenPropertiesByElementId.refreshWithContext(getConsultantEntite(),
                                displayedDossierPrestationProperty.getValue(),
                                getDonneesRealiseesInteraction(), reglesResult);
                        composantInterpretationOuLecturePropertiesByElementId.refreshWithContext(getConsultantEntite(),
                                displayedDossierPrestationProperty.getValue(),
                                getDonneesRealiseesInteraction(), reglesResult);
                        refreshReglesAffichageEtConstats(reglesResult);
                    } else {
                        composantProperties.setValue(oldValue);
                    }
                }
            }
        };
    }

    private Consumer<DialogConfirmationController> onSelectionItemTableView(
            final ExamenParacliniqueRealise itemSelectionne) {
        return t -> {
            rowSelectionOperations(itemSelectionne);
            setMode(ControllerMode.CONSULTATION);
        };
    }

    @Override
    protected void rowSelectionOperations(ExamenParacliniqueRealise examen) {
        isListenerEnableProperty.set(false);
        Stream.of(composantExamenPropertiesByElementId, composantInterpretationOuLecturePropertiesByElementId)
                .forEach(GrilleDynamiqueUtils::resetAllValues);
        lecturePrerequiseChcbx.setSelected(false);
        isListenerEnableProperty.set(true);
        reglesAffichageElementComposantPropertiesMap.values().forEach(egd -> egd.setVisible(Boolean.FALSE));
        if (examen != null) {
            lecturePrerequiseChcbx.setSelected(Boolean.TRUE.equals(examen.getIsLecturePrerequise()));

            examen.getListeConstatRealiseExamen().forEach(cstRea -> composantExamenPropertiesByElementId.putConstatRealiseValueInGrilleDynamique(cstRea));
            if (examen.isInterprete() != null && Boolean.TRUE.equals(examen.isInterprete())) {
                examen.getListeConstatRealiseInterpretation()
                        .forEach(cstRea -> composantInterpretationOuLecturePropertiesByElementId.putConstatRealiseValueInGrilleDynamique(cstRea));
            }

            MoteurRegleResult reglesResult = moteurRegleService.executeRulesWithDefaultRegistry(getMoteurRegleContextParaclinique());
            composantExamenPropertiesByElementId.refreshWithContext(getConsultantEntite(),
                    displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
            composantInterpretationOuLecturePropertiesByElementId.refreshWithContext(getConsultantEntite(),
                    displayedDossierPrestationProperty.getValue(), getDonneesRealiseesInteraction(), reglesResult);
            notEmptyRowSelectionInterpAndMatOperations(examen);
            refreshReglesAffichageEtConstats(reglesResult);
        }
    }

    private void notEmptyRowSelectionInterpAndMatOperations(ExamenParacliniqueRealise examen) {
        if (Boolean.TRUE.equals(examen.isInterprete())) {
            examen.getListeConstatRealiseInterpretation()
                    .forEach(cstRea -> composantInterpretationOuLecturePropertiesByElementId.putConstatRealiseValueInGrilleDynamique(cstRea));
        }
        if (!lsElementsConstatMateriel.isEmpty() && examen.getListeConstatMaterielRealise() != null
                && !examen.getListeConstatMaterielRealise().isEmpty()) {
            putConstatMaterielValuesIntoGrillesDynamiques(lsElementsConstatMateriel, examen,
                    composantExamenPropertiesByElementId);
            putConstatMaterielValuesIntoInterpretationGrille(examen);
        }

        List<ElementConstat> listElementConstatsObligatoiresAvantAcquisition = getListElementConstatsObligatoiresAvantAcquisition();
        if (CollectionUtils.isNotEmpty(listElementConstatsObligatoiresAvantAcquisition)) {
            boolean isAuMoins1ConstatObligPostAcquisitionIsSetted = listElementConstatsObligatoiresAvantAcquisition.stream()
                    .anyMatch(elementConstat -> isAuMoins1ConstatObligPostAcquisitionIsSetted(elementConstat));
            implementationComposantDynamique.changeAsterixToShowValue(Boolean.TRUE.equals(isAuMoins1ConstatObligPostAcquisitionIsSetted));
        }
    }

    private boolean isAuMoins1ConstatObligPostAcquisitionIsSetted(ElementConstat elemConstat) {
        ConstatRealise constatRealise = retrieveConstatRealiseFromElementConstat(elemConstat, null, composantExamenPropertiesByElementId);
        return !constatRealise.isEmptyConstatRealiseValue();
    }

    private void putConstatMaterielValuesIntoInterpretationGrille(ExamenParacliniqueRealise examen) {
        if (!lsElemCstInterpOuLectureMateriel.isEmpty() && examen.getAnalyste() == null) {
            Map<String, ConstatMaterielRealise> cstMaterielReaByIdIOPGeneriqueMap = new HashMap<>();
            lsElemCstInterpOuLectureMateriel.forEach(ecm -> generateCstMaterielReaByIdIOPOrCodeLoincMap(ecm, examen, cstMaterielReaByIdIOPGeneriqueMap));
            if (!cstMaterielReaByIdIOPGeneriqueMap.isEmpty()) {
                lsElemCstInterpOuLectureMateriel
                        .forEach(elemCst -> putConstatMaterielValueFromElemCstIntoInterpretationGrille(elemCst, cstMaterielReaByIdIOPGeneriqueMap));
            }
        }
    }

    private void putConstatMaterielValueFromElemCstIntoInterpretationGrille(ElementConstat elemCst,
            Map<String, ConstatMaterielRealise> cstMaterielReaByIdIOPGeneriqueMap) {
        String idIOPGenerique = isFormatEchangeHL7() ? elemCst.getConstat().getCodeLOINC() : elemCst.getConstat().getIdInteroperabilite();
        if (cstMaterielReaByIdIOPGeneriqueMap.containsKey(idIOPGenerique)) {
            ConstatMaterielRealise cstMatRea = cstMaterielReaByIdIOPGeneriqueMap.get(idIOPGenerique);
            Map<Long, ElementSaisieGrilleDynamiqueComposantProperties> composantPropByElemId = new HashMap<>();
            generateComponentsByElemIdMap(composantPropByElemId, composantInterpretationOuLecturePropertiesByElementId);
            setConstatMaterielValueByType(elemCst, cstMatRea, composantPropByElemId);
        }
    }

    private void generateCstMaterielReaByIdIOPOrCodeLoincMap(ElementConstat ecm, ExamenParacliniqueRealise examen,
            Map<String, ConstatMaterielRealise> cstMaterielReaByIdIOPOrCodeLoincMap) {
        if (isFormatEchangeHL7()) {
            String codeLoinc = ecm.getConstat().getCodeLOINC();
            examen.getListeConstatMaterielRealise().stream()
                    .filter(c -> null != c.getCodeLoinc() && c.getCodeLoinc().equals(codeLoinc)).findFirst()
                    .ifPresent(constatMat -> cstMaterielReaByIdIOPOrCodeLoincMap.put(codeLoinc, constatMat));
        } else {
            String idIOP = ecm.getConstat().getIdInteroperabilite();
            examen.getListeConstatMaterielRealise().stream()
                    .filter(c -> null != c.getIdIOP() && c.getIdIOP().equals(idIOP)).findFirst()
                    .ifPresent(constatMat -> cstMaterielReaByIdIOPOrCodeLoincMap.put(idIOP, constatMat));
        }
    }

    private void putConstatMaterielValuesIntoGrillesDynamiques(List<ElementConstat> lsElementConstat,
            ExamenParacliniqueRealise examen, Map<Long, ElementGrilleDynamiqueComposantProperties> composants) {
        lsElementConstat.stream()
                .filter(ecm -> examen.getListeConstatMaterielRealise().stream().map(ConstatMaterielRealise::getIdConstatPublie)
                        .collect(Collectors.toList()).contains(ecm.getConstat().getId()))
                .forEach(ecm -> setConstatMaterielValue(examen, ecm, composants));
    }

    private void setConstatMaterielValue(ExamenParacliniqueRealise examen, ElementConstat ecm,
            Map<Long, ElementGrilleDynamiqueComposantProperties> composants) {
        Long idCst = ecm.getConstat().getId();
        ConstatMaterielRealise constatMaterielResult = examen.getListeConstatMaterielRealise().stream()
                .filter(cmr -> cmr.getIdConstatPublie().equals(idCst)).findFirst().orElse(null);
        if (constatMaterielResult != null) {
            Map<Long, ElementSaisieGrilleDynamiqueComposantProperties> composantPropByElemId = new HashMap<>();
            generateComponentsByElemIdMap(composantPropByElemId, composants);
            setConstatMaterielValueByType(ecm, constatMaterielResult, composantPropByElemId);
        }
    }

    private void generateComponentsByElemIdMap(
            Map<Long, ElementSaisieGrilleDynamiqueComposantProperties> composantPropByElemId,
            Map<Long, ElementGrilleDynamiqueComposantProperties> composants) {
        composants.keySet().stream()
                .filter(key -> composants.get(key) instanceof ElementSaisieGrilleDynamiqueComposantProperties)
                .forEach(key -> composantPropByElemId.put(key, retrieveComponentByKey(key, composants)));
    }

    private void setConstatMaterielValueByType(ElementConstat ecm, ConstatMaterielRealise constatMaterielResult,
            Map<Long, ElementSaisieGrilleDynamiqueComposantProperties> composantPropByElemId) {
        ConstatMaterielRealiseWithConstatLibelleCourtVo constatMatRea = constatMaterielRealiseFac
                .buildConstatMaterielRealiseWithConstatLibelleCourtVo(constatMaterielResult);
        constatMatRea.setConstatFormatAndConstatNombreDecimalWithConstatData(ecm.getConstat());
        composantPropByElemId.get(ecm.getId()).setValue(ConstatRealiseHelper.getConstatMaterielRealiseResultatStringValue(constatMatRea));
    }

    private ElementSaisieGrilleDynamiqueComposantProperties retrieveComponentByKey(long key, Map<Long, ElementGrilleDynamiqueComposantProperties> composants) {
        return (ElementSaisieGrilleDynamiqueComposantProperties) composants.get(key);
    }

    private Consumer<DialogConfirmationController> onCancelDialogConfirmationBoxOperation(final ExamenParacliniqueRealise oldExamenParacliniqueRealise) {
        return t -> Platform.runLater(() -> {
            epcListeExamensRealisesTableView.resetSelectionModelValue(oldExamenParacliniqueRealise);
        });
    }

    private boolean isModifIntoGrilleDynamiqueData() {
        List<ConstatRealise> lsModifiedConstatRea = getListExamenConstatRealise();
        boolean isEquals = true;
        if (editedExam != null) {
            isEquals = ConstatRealiseHelper.isModifIntoConstatExamenDataCase(isEquals, editedExam, lsModifiedConstatRea);
        } else {
            isEquals = ConstatRealiseHelper.areEmptyConstatReaValues(isEquals, lsModifiedConstatRea);
        }
        return !isEquals;
    }

    private boolean isModifIntoInterpretationDynamiquePartData() {
        List<ConstatRealise> lsModifiedConstatRea = getListInterpretationConstatRealise(editedExam.getId(), !isInsertInterpretCase());
        boolean isEqual = true;
        if (editedExam != null && editedExam.isInterprete()) {
            isEqual = ConstatRealiseHelper.isModifIntoConstatInterpretationDataCase(isEqual, editedExam, lsModifiedConstatRea);
        } else if (editedExam != null && !lsElemCstInterpOuLectureMateriel.isEmpty() && editedExam.getAnalyste() == null) {
            isEqual = isModifIntoInterpretationDataInitAndMaterielCase(isEqual, lsModifiedConstatRea);
        } else if (editedExam != null && !lsElemCstInterpOuLectureMateriel.isEmpty()
                && editedExam.getListeConstatRealiseInterpretation() != null) {
            isEqual = ConstatRealiseHelper.isModifIntoConstatInterpretationDataCase(isEqual, editedExam, lsModifiedConstatRea);
        } else {
            isEqual = ConstatRealiseHelper.areEmptyConstatReaValues(isEqual, lsModifiedConstatRea);
        }
        return !isEqual;
    }

    private boolean isModifIntoInterpretationDataInitAndMaterielCase(Boolean isEqual,
            List<ConstatRealise> lsModifiedConstatRea) {
        List<ConstatMaterielRealise> lsConstatMateriel = editedExam.getListeConstatMaterielRealise();
        List<ConstatRealise> lsModifiedConstatReaBindedToMateriel = lsModifiedConstatRea.stream()
                .filter(cst -> lsElemCstInterpOuLectureMateriel.contains(cst.getElementConstat())).collect(Collectors.toList());
        areEqualGeneric = true;
        lsModifiedConstatReaBindedToMateriel
                .forEach(cstRea -> areEqualConstatMaterielAndConstatReaValues(cstRea, lsConstatMateriel));
        List<ConstatRealise> lsModifiedNotBindedToMat = lsModifiedConstatRea.stream()
                .filter(cst -> !lsElemCstInterpOuLectureMateriel.contains(cst.getElementConstat())).collect(Collectors.toList());
        if (isEqual != null) {
            return areEqualGeneric && ConstatRealiseHelper.areEmptyConstatReaValues(isEqual, lsModifiedNotBindedToMat);
        } else {
            return areEqualGeneric;
        }
    }

    private boolean areEmptyInterpretationData(List<ConstatRealise> lsModifiedConstatRea) {
        List<ConstatRealise> lsModifiedConstatInterp = lsModifiedConstatRea.stream()
                .filter(cst -> lsElementsConstatInterpretation.contains(cst.getElementConstat()))
                .collect(Collectors.toList());
        boolean isEquals = true;
        return ConstatRealiseHelper.areEmptyConstatReaValues(isEquals, lsModifiedConstatInterp);
    }

    private void areEqualConstatMaterielAndConstatReaValues(ConstatRealise constatRealise, List<ConstatMaterielRealise> lsConstatMaterielRealise) {
        ElementConstat elementConstat = constatRealise.getElementConstat();
        lsConstatMaterielRealise.stream()
                .filter(cm -> cm.getIdConstatPublie().equals(elementConstat.getConstat().getId())).findFirst()
                .ifPresent(beforeModifVal -> areEqualConstatMaterielAndConstatReaValueByType(beforeModifVal, constatRealise));
    }

    private void areEqualConstatMaterielAndConstatReaValueByType(ConstatMaterielRealise beforeModifVal, ConstatRealise constatRealise) {
        ElementConstat elementConstat = constatRealise.getElementConstat();
        if (isFormatEchangeHL7()) {
            if (EnumListeFormatConstat.getEnumListeFormatConstatNumeric().contains(elementConstat.getConstat().getFormResult())) {
                areEqualGeneric = areEqualGeneric
                        && ((beforeModifVal.getNumericValue() == null && constatRealise.getNumericValue() == null)
                                || (beforeModifVal.getNumericValue().equals(constatRealise.getNumericValue())));
            } else {
                areEqualGeneric = areEqualGeneric && Strings.nullToEmpty(beforeModifVal.getTextValue())
                        .equals(Strings.nullToEmpty(constatRealise.getTextValue()));
            }
        } else {
            String idInteroperabilite = elementConstat.getConstat().getIdInteroperabilite();
            if (idInteroperabilite != null && EnumSpiroIOP.containsCode(idInteroperabilite)) {
                areEqualGeneric = ConstatRealiseHelper.areEqualConstatMaterielSpiroCase(areEqualGeneric, beforeModifVal, constatRealise, elementConstat);
            } else if (idInteroperabilite != null && EnumECGIOP.containsCode(idInteroperabilite)) {
                areEqualGeneric = ConstatRealiseHelper.areEqualConstatMaterielECGCase(areEqualGeneric, beforeModifVal, constatRealise, elementConstat);
            } else {
                areEqualGeneric = areEqualGeneric
                        && ((beforeModifVal.getNumericValue() == null && constatRealise.getNumericValue() == null)
                                || (beforeModifVal.getNumericValue().equals(constatRealise.getNumericValue())));
            }
        }
    }

    @Override
    protected MoteurRegleContext getMoteurRegleContextParaclinique() {
        final MoteurRegleContext moteurRegleContext = getMoteurRegleContext();
        initializeMoteurDeRegleContextFromLsElementsConstat(moteurRegleContext);
        return moteurRegleContext;
    }

    private void initializeMoteurDeRegleContextFromLsElementsConstat(MoteurRegleContext moteurRegleContext) {
        List<ElementConstat> lsElementsConstatMatInterp = new ArrayList<>();
        List<ElementConstat> lsElementsConstatBasiqInterp = new ArrayList<>();
        if (!lsElementsConstatInterpretation.isEmpty()) {
            lsElementsConstatMatInterp = lsElementsConstatInterpretation.stream()
                    .filter(ec -> ec.getConstat().getAppareilConnecte() != null).collect(Collectors.toList());
            lsElementsConstatBasiqInterp = lsElementsConstatInterpretation.stream()
                    .filter(ec -> ec.getConstat().getAppareilConnecte() == null).collect(Collectors.toList());
        }

        initConstatRealiseIntoMoteurRegleContext(moteurRegleContext, lsElementsConstatBasiqInterp);

        initConstatMaterielRealiseIntoMoteurRegleContext(moteurRegleContext, lsElementsConstatMatInterp);
    }

    private void initConstatMaterielRealiseIntoMoteurRegleContext(MoteurRegleContext moteurRegleContext,
            List<ElementConstat> lsElementsConstatMatInterp) {
        List<ConstatMaterielRealise> lsConstatMaterielRealises = new ArrayList<>();
        List<ElementConstat> lsElemConstatMateriel = new ArrayList<>();
        if (!lsElementsConstatMateriel.isEmpty()) {
            lsConstatMaterielRealises.addAll(getListeConstatMaterielRealiseExamen());
            lsElemConstatMateriel.addAll(lsElementsConstatMateriel);
        }

        if (!lsElementsConstatMatInterp.isEmpty()) {
            lsConstatMaterielRealises.addAll(getListeConstatMaterielRealiseInterpretation(lsElementsConstatMatInterp));
            lsElemConstatMateriel.addAll(lsElementsConstatMatInterp);
        }
        if (!lsElemConstatMateriel.isEmpty()) {
            List<ConstatMaterielRealiseWithConstatLibelleCourtVo> lsConstatMaterielRealisesVO = lsConstatMaterielRealises
                    .stream().map(cmr -> constatMaterielRealiseFac.buildConstatMaterielRealiseVo(cmr, lsElemConstatMateriel))
                    .collect(Collectors.toList());
            moteurRegleContext.putConstatMaterielRealises(lsConstatMaterielRealisesVO);
        }
    }

    private void initConstatRealiseIntoMoteurRegleContext(MoteurRegleContext moteurRegleContext,
            List<ElementConstat> lsElementsConstatBasiqInterpretationOuLecture) {
        List<ConstatRealise> lsConstatRealises = new ArrayList<>();

        if (!lsElementsConstatExamen.isEmpty()) {
            lsConstatRealises.addAll(getListConstatRealiseExamen());
        }

        if (!lsElementsConstatBasiqInterpretationOuLecture.isEmpty()) {
            lsConstatRealises.addAll(getListConstatRealiseInterpretationOuLecture(lsElementsConstatBasiqInterpretationOuLecture));
        }

        if (!lsConstatRealises.isEmpty()) {
            moteurRegleContext.putConstatRealises(lsConstatRealises);
        }
    }

    private List<ConstatMaterielRealise> getListeConstatMaterielRealiseInterpretation(
            List<ElementConstat> lsElemConstatMatInterp) {
        final List<ConstatMaterielRealise> listeConstatMaterielRealise = new ArrayList<>();

        if (selectedExamenParaclinique.isNotNull().get()) {
            List<ConstatMaterielRealise> lsCstMatRea = selectedExamenParaclinique.get().getListeConstatMaterielRealise();

            if (CollectionUtils.isEmpty(lsCstMatRea)) {
                listeConstatMaterielRealise.addAll(generateConstatMaterielInterpRealise(lsElemConstatMatInterp));
            } else {
                if (isFormatEchangeHL7()) {
                    List<String> lsCodeLoinc = lsElemConstatMatInterp.stream()
                            .map(ecm -> ecm.getConstat().getCodeLOINC())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    listeConstatMaterielRealise
                            .addAll(lsCstMatRea.stream().filter(cstMat -> lsCodeLoinc.contains(cstMat.getCodeLoinc())).collect(Collectors.toList()));
                } else {

                    List<String> lsIdIOP = lsElemConstatMatInterp.stream()
                            .map(ecm -> ecm.getConstat().getIdInteroperabilite())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    listeConstatMaterielRealise.addAll(lsCstMatRea.stream().filter(cstMat -> lsIdIOP.contains(cstMat.getIdIOP())).collect(Collectors.toList()));
                }
            }
        } else {
            listeConstatMaterielRealise.addAll(generateConstatMaterielInterpRealise(lsElemConstatMatInterp));
        }

        return listeConstatMaterielRealise;
    }

    private List<ConstatMaterielRealise> getListeConstatMaterielRealiseExamen() {
        final List<ConstatMaterielRealise> listeConstatMaterielRealise = new ArrayList<>();
        if (selectedExamenParaclinique.isNotNull().get() && selectedExamenParaclinique.get().getListeConstatMaterielRealise() != null) {
            List<Long> lsIdElementsConstatMat = lsElementsConstatMateriel.stream().map(ec -> ec.getConstat().getId())
                    .collect(Collectors.toList());
            List<ConstatMaterielRealise> lsCstMatRea = selectedExamenParaclinique.get().getListeConstatMaterielRealise()
                    .stream().filter(cmr -> lsIdElementsConstatMat.contains(cmr.getIdConstatPublie()))
                    .collect(Collectors.toList());
            listeConstatMaterielRealise.addAll(!lsCstMatRea.isEmpty() ? lsCstMatRea : generateConstatMaterielRealise());
        } else {
            listeConstatMaterielRealise.addAll(generateConstatMaterielRealise());
        }
        return listeConstatMaterielRealise;
    }

    private List<ConstatMaterielRealise> generateConstatMaterielRealise() {
        return lsElementsConstatMateriel.stream()
                .map(elementConstat -> composantExamenPropertiesByElementId.generateConstatMaterielRealiseFromElementConstat(elementConstat))
                .collect(Collectors.toList());
    }

    private List<ConstatMaterielRealise> generateConstatMaterielInterpRealise(
            List<ElementConstat> lsElemConstatMatInterp) {
        return lsElemConstatMatInterp.stream()
                .map(elementConstat -> composantInterpretationOuLecturePropertiesByElementId.generateConstatMaterielRealiseFromElementConstat(elementConstat))
                .collect(Collectors.toList());
    }

    private List<ConstatRealise> getListConstatRealiseInterpretationOuLecture(List<ElementConstat> lsElementConstatInterpretation) {
        return lsElementConstatInterpretation.stream().map(elementConstat -> retrieveConstatRealise(elementConstat, true)).collect(Collectors.toList());
    }

    protected ConstatRealise retrieveConstatRealise(final ElementConstat elementConstat, boolean isInterpretationOuLecture) {
        final ConstatRealise constatRealise;
        final ElementConstatComposantProperties composantProperties;
        constatRealise = generateCstReaFromGrilleType(elementConstat, isInterpretationOuLecture);
        composantProperties = isInterpretationOuLecture
                ? (ElementConstatComposantProperties) composantInterpretationOuLecturePropertiesByElementId
                        .get(elementConstat.getId())
                : (ElementConstatComposantProperties) composantExamenPropertiesByElementId.get(elementConstat.getId());
        final ConstatRealise constatRealiseInGrid = elementRealiseFactory
                .buildConstatRealiseFromConstatElementConstatComposantProperties(constatRealise.getId(), elementConstat, composantProperties);
        constatRealiseInGrid.setIdPrestationRealisee(constatRealise.getIdPrestationRealisee());
        constatRealiseInGrid.setElementConstat(constatRealise.getElementConstat());
        return constatRealiseInGrid;
    }

    @Override
    protected GrilleDynamiqueUi getComposantExamenUi() {
        return composantExamenPropertiesByElementId;
    }

    @Override
    protected GrilleDynamiqueUi getComposantInterpretationOrLectureUi() {
        return composantInterpretationOuLecturePropertiesByElementId;
    }

    @Override
    protected boolean isEditedExam(ExamenParacliniqueRealise examen) {
        return examen != null && examen.equals(editedExam);
    }

    @Override
    protected Long getDisplayedPrestationRealiseeId() {
        return displayedPrestationRealisee.getId();
    }

    @Override
    protected ConstatRealise buildConstatRealise(Long prestationId, ElementConstat elementConstat) {
        return constatRealiseFactory.buildConstatRealise(prestationId, elementConstat);
    }

    @Override
    protected ConstatRealise buildConstatRealise(ConstatRealise constatRealise) {
        return constatRealiseFactory.buildConstatRealise(constatRealise);
    }

    @Override
    protected boolean isSoftwareLaunchOperation() {
        return isSoftwareLaunchOperation;
    }

    public void refreshConstatRealise(final ConstatRealise constatRealise) {
        ConstatRealiseWithOrigineInformationsVo constatRealiseWithOrigineInfos = ConstatRealiseWithOrigineInformationsVoFactory
                .addOrigineInformationsToConstatRealise(constatRealise, getDisplayedInteractionRealiseeInteraction().getLibelleCourt(),
                        UtilisateurVoFactory.buildUtilisateurVo(sessionContext.getConnectedUser()));
        final List<ConstatRealiseWithOrigineInformationsVo> constatRealises = displayedPrestationRealiseeDonneesRealisees.getConstatRealises();
        final Predicate<ConstatRealise> predicate = constatRea -> constatRea.getElementConstat().getConstat().getId()
                .equals(constatRealise.getElementConstat().getConstat().getId());
        final Optional<ConstatRealiseWithOrigineInformationsVo> optionalConstatRealise = constatRealises.stream().filter(predicate).findFirst();
        if (optionalConstatRealise.isPresent()) {
            final ConstatRealise constatRealise2 = optionalConstatRealise.get();
            constatRealise.setId(constatRealise2.getId());
            constatRealises.set(constatRealises.indexOf(constatRealise2), constatRealiseWithOrigineInfos);
        } else {
            constatRealises.add(constatRealiseWithOrigineInfos);
        }
    }

    @Override
    protected void refreshConstatsWithMoteurRegleResult(final MoteurRegleResult moteurRegleResult) {
        if (!lsElementsConstat.isEmpty()) {
            lsElementsConstat.forEach(elementConstat -> composantExamenPropertiesByElementId.setConstatScore(elementConstat, moteurRegleResult));
        }
        if (!lsElementsConstatInterpretation.isEmpty()) {
            lsElementsConstatInterpretation
                    .forEach(elementConstat -> composantInterpretationOuLecturePropertiesByElementId.setConstatScore(elementConstat, moteurRegleResult));
        }
    }

    @Override
    protected void refreshLibellesReglesAffichage(final MoteurRegleResult moteurRegleResult) {
        reglesAffichageElementComposantPropertiesMap.entrySet()
                .forEach(nodeRegleAffichage -> setReglesAffichagesVisibilites(moteurRegleResult, nodeRegleAffichage));
    }

    @Override
    protected void onRowSelectionPreRefreshHook(ExamenParacliniqueRealise examen) {
        lecturePrerequiseChcbx.setSelected(Boolean.TRUE.equals(examen.getIsLecturePrerequise()));
    }

    private void setReglesAffichagesVisibilites(final MoteurRegleResult moteurRegleResult,
            final Entry<Long, ElementGrilleDynamiqueComposantProperties> nodeRegleAffichage) {
        final Predicate<? super LibelleRegleAffichage> predicateLibelleRegleAffichage = libelleRegleAffichage -> libelleRegleAffichage
                .getId().equals(nodeRegleAffichage.getKey());
        final Optional<LibelleRegleAffichage> optionalLibelleRegleAffichage = lsLibelleRegleAffichage.stream()
                .filter(predicateLibelleRegleAffichage).findFirst();
        optionalLibelleRegleAffichage.ifPresent(libelleRegleAffichage -> setLibelleRegleAffichageVisibility(moteurRegleResult, nodeRegleAffichage,
                libelleRegleAffichage));
    }

    private void setLibelleRegleAffichageVisibility(final MoteurRegleResult moteurRegleResult,
            final Entry<Long, ElementGrilleDynamiqueComposantProperties> nodeRegleAffichage,
            final LibelleRegleAffichage libelleRegleAffichage) {
        final String libelleCourt = libelleRegleAffichage.getRegleAffichage().getLibelleCourt();
        final Boolean booleanRuleResult = moteurRegleResult.getBooleanRuleResult(libelleCourt);
        nodeRegleAffichage.getValue().setVisible(booleanRuleResult != null && booleanRuleResult);
    }

    public boolean isNotDisabledAcquisitionButton() {
        return !epcAcquisitionResultatsBtn.isDisable();
    }

    @Override
    protected void onValueChanged(ElementSaisieGrilleDynamiqueComposantProperties composantProperties, Object oldValue) {
        checkUsableBeforeExecuteReglesAffichageEtCalcul(composantProperties, oldValue, true);
    }
}
