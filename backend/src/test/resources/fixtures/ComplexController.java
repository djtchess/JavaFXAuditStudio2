// Fixture de test — pas un fichier compilé.
// Simule un controller JavaFX complexe pour les tests de parsing regex.
// Utilisé par FxmlCartographyAnalysisAdapterTest pour valider extractHandlers sur un cas riche.

package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;

public class ComplexController {

    // Champs FXML — composants liés au ComplexView.fxml

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button clearSearchButton;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TableView<Object> resultTable;

    @FXML
    private TableColumn<Object, String> nameColumn;

    @FXML
    private TableColumn<Object, String> typeColumn;

    @FXML
    private TableColumn<Object, String> statusColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Label infoLabel;

    // Services Spring injectés
    @Autowired
    private SearchService searchService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuditService auditService;

    // Méthodes d'initialisation

    @FXML
    public void initialize() {
        categoryService.loadAll().forEach(c -> categoryCombo.getItems().add(c));
        infoLabel.setText("Prêt.");
    }

    // Handlers d'événements

    @FXML
    public void handleSearch() {
        String term = searchField.getText();
        resultTable.getItems().setAll(searchService.search(term));
        infoLabel.setText("Résultats : " + resultTable.getItems().size());
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        resultTable.getItems().clear();
        infoLabel.setText("Effacé.");
    }

    @FXML
    public void handleCategoryChange() {
        String cat = categoryCombo.getValue();
        resultTable.getItems().setAll(searchService.searchByCategory(cat));
    }

    @FXML
    public void handleAdd() {
        itemService.openAddDialog();
        auditService.log("ADD");
    }

    @FXML
    public void handleEdit() {
        Object selected = resultTable.getSelectionModel().getSelectedItem();
        itemService.openEditDialog(selected);
        auditService.log("EDIT");
    }

    @FXML
    public void handleDelete() {
        Object selected = resultTable.getSelectionModel().getSelectedItem();
        itemService.delete(selected);
        auditService.log("DELETE");
        infoLabel.setText("Supprimé.");
    }

    @FXML
    void handleExport() {
        itemService.exportAll();
        infoLabel.setText("Export lancé.");
    }

    @FXML
    void handleRefresh() {
        handleClearSearch();
        infoLabel.setText("Actualisé.");
    }
}
