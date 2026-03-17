package ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleController {

    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;
    @FXML private TextField nameField;

    @Autowired private PatientService patientService;
    @Autowired private AuditService auditService;

    @FXML
    public void initialize() {
        statusLabel.setText("Prêt");
    }

    @FXML
    public void onSaveButtonClicked() {
        patientService.save(nameField.getText());
        statusLabel.setText("Sauvegardé");
    }

    @FXML
    public void onCancelButtonClicked() {
        statusLabel.setText("Annulé");
        nameField.clear();
    }

    @FXML
    public void onDeletePatient() {
        patientService.delete();
        auditService.log("delete");
    }

    public void onValueChanged() {
        statusLabel.setText(nameField.getText());
    }
}
