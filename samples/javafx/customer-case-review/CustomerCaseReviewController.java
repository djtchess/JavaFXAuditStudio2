package app.ui.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomerCaseReviewController extends AbstractCaseWorkflowController implements Initializable {

    private static final BigDecimal EXPRESS_MAX_AMOUNT = new BigDecimal("5000");
    private static final BigDecimal PREMIUM_MIN_AMOUNT = new BigDecimal("10000");
    private static final BigDecimal MANUAL_REVIEW_THRESHOLD = new BigDecimal("25000");

    @Autowired
    private CustomerCaseService customerCaseService;

    @Autowired
    private EligibilityService eligibilityService;

    @Autowired
    private DocumentExportService documentExportService;

    @Autowired
    private NavigationService navigationService;

    @FXML
    private VBox premiumSection;

    @FXML
    private VBox manualReviewSection;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label globalMessageLabel;

    @FXML
    private Label documentStatusLabel;

    @FXML
    private TextField caseNumberField;

    @FXML
    private TextField customerCodeField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> caseTypeComboBox;

    @FXML
    private ChoiceBox<String> customerSegmentChoiceBox;

    @FXML
    private ComboBox<String> caseStatusComboBox;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private TextField requestedAmountField;

    @FXML
    private TextField remainingBudgetField;

    @FXML
    private TextField premiumApprovalCodeField;

    @FXML
    private TextArea analystCommentArea;

    @FXML
    private TextArea manualReviewReasonArea;

    @FXML
    private TextArea riskExplanationArea;

    @FXML
    private CheckBox includeArchivedCheckBox;

    @FXML
    private CheckBox premiumCustomerCheckBox;

    @FXML
    private CheckBox kycCompletedCheckBox;

    @FXML
    private CheckBox documentsCompleteCheckBox;

    @FXML
    private CheckBox fraudAlertCheckBox;

    @FXML
    private CheckBox complianceBlockCheckBox;

    @FXML
    private RadioButton standardModeRadio;

    @FXML
    private RadioButton expressModeRadio;

    @FXML
    private RadioButton manualReviewRadio;

    @FXML
    private Button validateCaseButton;

    @FXML
    private Button generateSummaryPdfButton;

    @FXML
    private Button generateDecisionPdfButton;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab riskTab;

    @FXML
    private Tab documentsTab;

    @FXML
    private TableView<CaseEventRow> eventHistoryTable;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        super.initializeWorkflow();
        caseTypeComboBox.getItems().setAll("STANDARD", "PREMIUM", "SENSITIVE");
        customerSegmentChoiceBox.getItems().setAll("RETAIL", "SME", "CORPORATE");
        caseStatusComboBox.getItems().setAll("DRAFT", "IN_REVIEW", "VALIDATED", "REJECTED");
        priorityComboBox.getItems().setAll("LOW", "NORMAL", "HIGH", "CRITICAL");
        caseStatusComboBox.setValue("DRAFT");
        priorityComboBox.setValue("NORMAL");
        remainingBudgetField.setText("0.00");
        globalMessageLabel.setText("Initialisation terminee. Recherche en attente.");
        loadHistory();
        updateDynamicSections();
    }

    @FXML
    public void onSearch() {
        super.validateContext();
        loadingIndicator.setVisible(true);

        if (!validateSearchCriteria()) {
            loadingIndicator.setVisible(false);
            return;
        }

        customerCaseService.searchCases(
                caseNumberField.getText(),
                customerCodeField.getText(),
                startDatePicker.getValue(),
                endDatePicker.getValue(),
                includeArchivedCheckBox.isSelected()
        );

        globalMessageLabel.setText("Recherche executee avec les filtres fournis.");
        loadingIndicator.setVisible(false);
    }

    @FXML
    public void onReset() {
        caseNumberField.clear();
        customerCodeField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        caseTypeComboBox.setValue(null);
        customerSegmentChoiceBox.setValue(null);
        requestedAmountField.clear();
        analystCommentArea.clear();
        manualReviewReasonArea.clear();
        riskExplanationArea.clear();
        includeArchivedCheckBox.setSelected(false);
        premiumCustomerCheckBox.setSelected(false);
        kycCompletedCheckBox.setSelected(false);
        documentsCompleteCheckBox.setSelected(false);
        fraudAlertCheckBox.setSelected(false);
        complianceBlockCheckBox.setSelected(false);
        standardModeRadio.setSelected(true);
        caseStatusComboBox.setValue("DRAFT");
        priorityComboBox.setValue("NORMAL");
        remainingBudgetField.setText("0.00");
        documentStatusLabel.setText("Aucun document genere");
        globalMessageLabel.setText("Formulaire reinitialise.");
        updateDynamicSections();
    }

    @FXML
    public void onSearchCriteriaChanged() {
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                globalMessageLabel.setText("RG-01: la date de fin doit etre posterieure ou egale a la date de debut.");
                searchButtonShouldStayDisabled(true);
            } else {
                searchButtonShouldStayDisabled(false);
            }
        }
    }

    @FXML
    public void onCaseTypeChanged() {
        updateDynamicSections();
        loadPremiumEligibility();
    }

    @FXML
    public void onStatusChanged() {
        updateValidationAvailability();
    }

    @FXML
    public void onPriorityChanged() {
        BigDecimal amount = parseAmount();
        if ("CRITICAL".equals(priorityComboBox.getValue()) && amount.compareTo(PREMIUM_MIN_AMOUNT) < 0) {
            globalMessageLabel.setText("RG-02: une priorite CRITICAL exige un montant minimum de 10000.");
        }
        updateDynamicSections();
    }

    @FXML
    public void onAmountChanged() {
        BigDecimal remainingBudget = computeRemainingBudget();
        remainingBudgetField.setText(remainingBudget.toPlainString());
        updateDynamicSections();
    }

    @FXML
    public void onRiskFlagsChanged() {
        boolean hasBlockingRisk = fraudAlertCheckBox.isSelected() || complianceBlockCheckBox.isSelected();
        riskTab.setDisable(false);
        documentsTab.setDisable(hasBlockingRisk);
        validateCaseButton.setDisable(hasBlockingRisk);

        if (hasBlockingRisk) {
            globalMessageLabel.setText("RG-03: un dossier bloque par fraude ou conformite ne peut pas etre valide.");
        }
    }

    @FXML
    public void onLoadPremiumEligibility() {
        loadPremiumEligibility();
    }

    @FXML
    public void onSaveDraft() {
        if (caseStatusComboBox.getValue() == null) {
            globalMessageLabel.setText("Impossible d enregistrer: statut absent.");
            return;
        }

        customerCaseService.saveDraft(
                caseNumberField.getText(),
                analystCommentArea.getText(),
                caseStatusComboBox.getValue(),
                priorityComboBox.getValue()
        );
        globalMessageLabel.setText("Brouillon enregistre.");
    }

    @FXML
    public void onValidateCase() {
        if (!validateCaseBusinessRules()) {
            return;
        }

        customerCaseService.validateCase(caseNumberField.getText(), buildValidationPayload());
        globalMessageLabel.setText("Dossier valide.");
        caseStatusComboBox.setValue("VALIDATED");
        updateValidationAvailability();
    }

    @FXML
    public void onRejectCase() {
        if (analystCommentArea.getText().isBlank()) {
            globalMessageLabel.setText("RG-04: un rejet doit etre motive dans le commentaire analyste.");
            return;
        }

        customerCaseService.rejectCase(caseNumberField.getText(), analystCommentArea.getText());
        caseStatusComboBox.setValue("REJECTED");
        globalMessageLabel.setText("Dossier rejete.");
    }

    @FXML
    public void onGenerateSummaryPdf() {
        if (!canGeneratePdf()) {
            return;
        }

        documentExportService.generatePdf(caseNumberField.getText(), "SUMMARY");
        documentStatusLabel.setText("PDF de synthese genere.");
    }

    @FXML
    public void onGenerateDecisionPdf() {
        if (!canGeneratePdf()) {
            return;
        }
        if (!"VALIDATED".equals(caseStatusComboBox.getValue())) {
            globalMessageLabel.setText("RG-05: le PDF de decision est reserve aux dossiers valides.");
            return;
        }

        documentExportService.generatePdf(caseNumberField.getText(), "DECISION");
        documentStatusLabel.setText("PDF de decision genere.");
    }

    @FXML
    public void onOpenDocumentHistory() {
        navigationService.openModal("DOCUMENT_HISTORY", caseNumberField.getText());
    }

    @FXML
    public void onOpenCustomerDialog() {
        navigationService.openModal("CUSTOMER_PROFILE", customerCodeField.getText());
    }

    @FXML
    public void onCreateFollowUp() {
        if ("REJECTED".equals(caseStatusComboBox.getValue())) {
            navigationService.navigateTo("follow-up/rejected-case");
        } else {
            navigationService.navigateTo("follow-up/standard-case");
        }
    }

    private boolean validateSearchCriteria() {
        boolean valid = true;
        if (caseNumberField.getText().isBlank() && customerCodeField.getText().isBlank()) {
            globalMessageLabel.setText("RG-06: renseigner au minimum un numero de dossier ou un code client.");
            valid = false;
        }
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                globalMessageLabel.setText("RG-01: la date de fin doit etre posterieure ou egale a la date de debut.");
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateCaseBusinessRules() {
        CustomerCaseValidator.ValidationResult validationResult = new CustomerCaseValidator().validate(buildCustomerCaseForm());
        if (!validationResult.valid()) {
            globalMessageLabel.setText(validationResult.message());
        }
        return validationResult.valid();
    }

    private CustomerCaseValidator.CustomerCaseForm buildCustomerCaseForm() {
        return new CustomerCaseValidator.CustomerCaseForm(
                caseTypeComboBox.getValue(),
                parseAmount(),
                premiumCustomerCheckBox.isSelected(),
                kycCompletedCheckBox.isSelected(),
                documentsCompleteCheckBox.isSelected(),
                fraudAlertCheckBox.isSelected(),
                complianceBlockCheckBox.isSelected(),
                expressModeRadio.isSelected(),
                manualReviewRadio.isSelected(),
                manualReviewReasonArea.getText()
        );
    }

    private void loadPremiumEligibility() {
        if (!"PREMIUM".equals(caseTypeComboBox.getValue())) {
            premiumSection.setVisible(false);
            premiumSection.setManaged(false);
            return;
        }

        boolean eligible = eligibilityService.isPremiumEligible(customerCodeField.getText(), parseAmount());
        premiumSection.setVisible(true);
        premiumSection.setManaged(true);
        premiumCustomerCheckBox.setSelected(eligible);

        if (!eligible) {
            globalMessageLabel.setText("RG-11: le dossier premium est refuse si le client n est pas eligible.");
        }
    }

    private void updateDynamicSections() {
        BigDecimal amount = parseAmount();
        boolean premiumCase = "PREMIUM".equals(caseTypeComboBox.getValue());
        boolean manualReviewRequired = manualReviewRadio.isSelected()
                || amount.compareTo(MANUAL_REVIEW_THRESHOLD) > 0
                || "CRITICAL".equals(priorityComboBox.getValue());

        premiumSection.setVisible(premiumCase);
        premiumSection.setManaged(premiumCase);
        manualReviewSection.setVisible(manualReviewRequired);
        manualReviewSection.setManaged(manualReviewRequired);

        if (manualReviewRequired) {
            riskTab.setDisable(false);
            mainTabPane.getSelectionModel().select(riskTab);
        }
    }

    private void updateValidationAvailability() {
        boolean readOnlyStatus = "VALIDATED".equals(caseStatusComboBox.getValue())
                || "REJECTED".equals(caseStatusComboBox.getValue());
        validateCaseButton.setDisable(readOnlyStatus);
        generateDecisionPdfButton.setDisable(!"VALIDATED".equals(caseStatusComboBox.getValue()));
    }

    private BigDecimal computeRemainingBudget() {
        BigDecimal requestedAmount = parseAmount();
        BigDecimal portfolioBudget = customerCaseService.loadAvailableBudget(customerCodeField.getText());
        BigDecimal remainingBudget = portfolioBudget.subtract(requestedAmount);

        if (remainingBudget.signum() < 0) {
            globalMessageLabel.setText("RG-12: le montant demande ne doit pas depasser le budget disponible.");
        }
        return remainingBudget;
    }

    private boolean canGeneratePdf() {
        if (caseNumberField.getText().isBlank()) {
            globalMessageLabel.setText("RG-13: impossible de generer un PDF sans numero de dossier.");
            return false;
        }
        if (!documentsCompleteCheckBox.isSelected()) {
            globalMessageLabel.setText("RG-14: les documents doivent etre complets avant tout export PDF.");
            return false;
        }
        return true;
    }

    private ValidationPayload buildValidationPayload() {
        return new ValidationPayload(
                caseNumberField.getText(),
                customerCodeField.getText(),
                caseStatusComboBox.getValue(),
                priorityComboBox.getValue(),
                parseAmount(),
                analystCommentArea.getText(),
                riskExplanationArea.getText()
        );
    }

    private BigDecimal parseAmount() {
        String rawValue = requestedAmountField.getText();
        BigDecimal amount = BigDecimal.ZERO;
        if (rawValue != null && !rawValue.isBlank()) {
            amount = new BigDecimal(rawValue.trim());
        }
        return amount;
    }

    private void loadHistory() {
        List<CaseEventRow> rows = customerCaseService.loadHistory(caseNumberField.getText());
        eventHistoryTable.getItems().setAll(rows);
    }

    private void searchButtonShouldStayDisabled(boolean disabled) {
        generateSummaryPdfButton.setDisable(disabled);
    }

    public record ValidationPayload(
            String caseNumber,
            String customerCode,
            String status,
            String priority,
            BigDecimal requestedAmount,
            String analystComment,
            String riskExplanation
    ) {
    }

    public record CaseEventRow(
            LocalDate eventDate,
            String eventType,
            String actor,
            String detail
    ) {
    }
}
