package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Convertisseur JPA pour stocker une liste de chaines en colonne TEXT[] PostgreSQL.
 */
@Converter
public class StringListTextArrayConverter implements AttributeConverter<List<String>, String[]> {

    @Override
    public String[] convertToDatabaseColumn(final List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return new String[0];
        }
        return attribute.toArray(new String[0]);
    }

    @Override
    public List<String> convertToEntityAttribute(final String[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(dbData);
    }
}
