package com.apex.payroll.util;

import com.apex.payroll.model.masterdata.MdFkAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MdFkActionConverter implements AttributeConverter<MdFkAction, String> {

    @Override
    public String convertToDatabaseColumn(MdFkAction attribute) {
        if (attribute == null) {
            return null;
        }
        // Persist using the SQL keyword representation (e.g. "SET NULL")
        return attribute.getSqlKeyword();
    }

    @Override
    public MdFkAction convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        String normalized = dbData.trim().toUpperCase().replace(' ', '_');

        for (MdFkAction action : MdFkAction.values()) {
            String nameNorm = action.name().toUpperCase();
            String sqlNorm = action.getSqlKeyword().trim().toUpperCase().replace(' ', '_');

            if (normalized.equals(nameNorm) || normalized.equals(sqlNorm)) {
                return action;
            }
        }

        throw new IllegalArgumentException("Unknown MdFkAction value: " + dbData);
    }
}

