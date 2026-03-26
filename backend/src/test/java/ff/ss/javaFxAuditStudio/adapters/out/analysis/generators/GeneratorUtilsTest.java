package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAS-007 — Tests de la methode fxmlTypeToProperty pour la resolution
 * des types TableView/ListView/TreeView vers OBSERVABLE_LIST, et non-regression
 * sur les types standards.
 */
class GeneratorUtilsTest {

    // --- JAS-007 : correspondance exacte TableView / ListView / TreeView ---

    @Test
    void fxmlTypeToProperty_tableView_retourneObservableList() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("table", "TableView");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.OBSERVABLE_LIST);
        assertThat(vmp.fieldName()).isEqualTo("tableItems");
    }

    @Test
    void fxmlTypeToProperty_listView_retourneObservableList() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("myList", "ListView");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.OBSERVABLE_LIST);
        assertThat(vmp.fieldName()).isEqualTo("myListItems");
    }

    @Test
    void fxmlTypeToProperty_treeView_retourneObservableList() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("myTree", "TreeView");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.OBSERVABLE_LIST);
        assertThat(vmp.fieldName()).isEqualTo("myTreeItems");
    }

    // --- JAS-007 : heuristique sur types custom heritant de vues liste ---

    @Test
    void fxmlTypeToProperty_typeCustomTableView_retourneObservableList() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("patients", "PatientTableView");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.OBSERVABLE_LIST);
        assertThat(vmp.fieldName()).isEqualTo("patientsItems");
    }

    @Test
    void fxmlTypeToProperty_typeCustomListView_retourneObservableList() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("results", "CustomListView");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.OBSERVABLE_LIST);
        assertThat(vmp.fieldName()).isEqualTo("resultsItems");
    }

    @Test
    void fxmlTypeToProperty_typeCustomTreeView_retourneObservableList() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("files", "FileTreeView");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.OBSERVABLE_LIST);
        assertThat(vmp.fieldName()).isEqualTo("filesItems");
    }

    // --- Non-regression : Label -> STRING ---

    @Test
    void fxmlTypeToProperty_label_retourneString() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("patientLabel", "Label");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.STRING);
        assertThat(vmp.fieldName()).isEqualTo("patientLabelText");
    }

    // --- Non-regression : Button -> BOOLEAN ---

    @Test
    void fxmlTypeToProperty_button_retourneBoolean() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("saveBtn", "Button");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.BOOLEAN);
        assertThat(vmp.fieldName()).isEqualTo("saveBtnEnabled");
    }

    // --- Non-regression : TextField -> STRING ---

    @Test
    void fxmlTypeToProperty_textField_retourneString() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("nameField", "TextField");

        assertThat(vmp).isNotNull();
        assertThat(vmp.type()).isEqualTo(PropertyType.STRING);
        assertThat(vmp.fieldName()).isEqualTo("nameFieldText");
    }

    // --- Non-regression : nom de champ suffixe en "Items" ---

    @Test
    void fxmlTypeToProperty_tableView_nomChampSuffixeItems() {
        ViewModelProperty vmp = GeneratorUtils.fxmlTypeToProperty("patientsTable", "TableView");

        assertThat(vmp.fieldName()).isEqualTo("patientsTableItems");
    }
}
