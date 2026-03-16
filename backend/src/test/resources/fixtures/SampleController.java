// Fixture de test — pas un fichier compilé.
// Simule un controller JavaFX typique pour les tests de parsing regex.
// Utilisé par FxmlCartographyAnalysisAdapterTest pour valider extractHandlers.

package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleController {

    // Champs FXML injectés par le FXMLLoader
    @FXML
    private Button submitButton;

    @FXML
    private Button resetButton;

    @FXML
    private TextField nameField;

    @FXML
    private Label statusLabel;

    // Services Spring injectés
    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    // Handlers d'événements déclarés dans le FXML

    @FXML
    public void handleSubmit() {
        String name = nameField.getText();
        userService.save(name);
        statusLabel.setText("Enregistré : " + name);
    }

    @FXML
    public void handleReset() {
        nameField.clear();
        statusLabel.setText("En attente...");
    }

    @FXML
    public void handleClose() {
        notificationService.notify("Fermeture demandée");
    }
}
