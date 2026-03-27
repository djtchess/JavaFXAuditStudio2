package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import ff.ss.javaFxAuditStudio.domain.generation.CodeArtifact;
import ff.ss.javaFxAuditStudio.domain.rules.BusinessRule;
import ff.ss.javaFxAuditStudio.domain.rules.ExtractionCandidate;
import ff.ss.javaFxAuditStudio.domain.rules.ResponsibilityClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAS-007 — Tests du generateur de ViewModel pour les composants ObservableList
 * (TableView, ListView, TreeView) et non-regression sur les types standards.
 */
class ViewModelGeneratorTest {

    private final ViewModelGenerator generator = new ViewModelGenerator();

    // --- Helpers ---

    private BusinessRule champFxml(final String type, final String fieldName) {
        return new BusinessRule(
                "RG-001",
                "Champ FXML " + type + " " + fieldName + " : liaison UI directe detectee",
                "PatientController.java",
                0,
                ResponsibilityClass.UI,
                ExtractionCandidate.VIEW_MODEL,
                false);
    }

    // --- JAS-007 : TableView ---

    @Test
    void tableView_genereObservableListField() {
        BusinessRule rule = champFxml("TableView", "patientsTable");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("ObservableList<Object> patientsItems");
    }

    @Test
    void tableView_utiliseFXCollectionsObservableArrayList() {
        BusinessRule rule = champFxml("TableView", "patientsTable");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("FXCollections.observableArrayList()");
    }

    // --- JAS-007 : ListView ---

    @Test
    void listView_genereObservableListField() {
        BusinessRule rule = champFxml("ListView", "resultList");
        CodeArtifact artifact = generator.generate("Result", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("ObservableList<Object> resultItems");
        assertThat(artifact.content()).contains("FXCollections.observableArrayList()");
    }

    // --- JAS-007 : TreeView ---

    @Test
    void treeView_genereObservableListField() {
        BusinessRule rule = champFxml("TreeView", "fileTree");
        CodeArtifact artifact = generator.generate("File", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("ObservableList<Object> fileItems");
        assertThat(artifact.content()).contains("FXCollections.observableArrayList()");
    }

    // --- JAS-007 : imports conditionnels ---

    @Test
    void observableListPresente_importsCollectionsAjoutes() {
        BusinessRule rule = champFxml("TableView", "patientsTable");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("import javafx.collections.FXCollections;");
        assertThat(artifact.content()).contains("import javafx.collections.ObservableList;");
    }

    @Test
    void sansObservableList_importsCollectionsAbsents() {
        BusinessRule rule = champFxml("Label", "statusLabel");
        CodeArtifact artifact = generator.generate("Status", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("import javafx.collections.FXCollections;");
        assertThat(artifact.content()).doesNotContain("import javafx.collections.ObservableList;");
    }

    // --- Non-regression : TextField continue de generer StringProperty ---

    @Test
    void textField_genereSimpleStringProperty() {
        BusinessRule rule = champFxml("TextField", "nameField");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).contains("SimpleStringProperty");
        assertThat(artifact.content()).doesNotContain("ObservableList");
    }

    // --- JAS-007 : getter sans setter pour ObservableList ---

    @Test
    void observableList_getterPresentSansSetter() {
        BusinessRule rule = champFxml("TableView", "patientsTable");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        // Le getter doit etre present
        assertThat(artifact.content()).contains("getPatientsItems()");
        // Pas de setter : la liste est modifiee en place
        assertThat(artifact.content()).doesNotContain("setPatientsItems(");
    }

    // --- Non-regression : pas de commentaire TODO pour TableView/ListView/TreeView ---

    @Test
    void tableView_pasDeCommentaireTodo() {
        BusinessRule rule = champFxml("TableView", "patientsTable");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(rule));

        assertThat(artifact.content()).doesNotContain("// TODO [patientsTable]");
        assertThat(artifact.content()).doesNotContain("ObservableList ou propriete specifique");
    }

    // --- Non-regression : regles de type "Methode handler" ignorees ---

    @Test
    void methodeHandler_ignoree() {
        BusinessRule handler = new BusinessRule(
                "RG-002",
                "Methode handler onSave : responsabilite APPLICATION detectee",
                "PatientController.java",
                0,
                ResponsibilityClass.APPLICATION,
                ExtractionCandidate.USE_CASE,
                false);
        BusinessRule champ = champFxml("Label", "statusLabel");
        CodeArtifact artifact = generator.generate("Patient", "com.example", List.of(handler, champ));

        assertThat(artifact.content()).doesNotContain("onSave");
        assertThat(artifact.content()).contains("statusText");
    }
}
