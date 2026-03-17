package ff.ss.javaFxAuditStudio.adapters.out.analysis.generators;

/**
 * Types de proprietes JavaFX supportes pour la generation du ViewModel.
 */
public enum PropertyType {
    STRING("SimpleStringProperty", "StringProperty", "String"),
    BOOLEAN("SimpleBooleanProperty", "BooleanProperty", "boolean"),
    INTEGER("SimpleIntegerProperty", "IntegerProperty", "int");

    private final String simpleClass;
    private final String propertyClass;
    private final String javaType;

    PropertyType(final String simpleClass, final String propertyClass, final String javaType) {
        this.simpleClass = simpleClass;
        this.propertyClass = propertyClass;
        this.javaType = javaType;
    }

    public String simpleClass() {
        return simpleClass;
    }

    public String propertyClass() {
        return propertyClass;
    }

    public String javaType() {
        return javaType;
    }
}
