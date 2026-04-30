package com.javaproject.splitewise.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class NumericStringAttributeConverter implements AttributeConverter<String, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(attribute.trim());
    }

    @Override
    public String convertToEntityAttribute(BigDecimal dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData.stripTrailingZeros().toPlainString();
    }
}
