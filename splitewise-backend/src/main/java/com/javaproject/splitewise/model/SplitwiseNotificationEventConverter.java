package com.javaproject.splitewise.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaproject.splitewise.messaging.SplitwiseNotificationEvent;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SplitwiseNotificationEventConverter implements AttributeConverter<SplitwiseNotificationEvent, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    public String convertToDatabaseColumn(SplitwiseNotificationEvent attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize splitwise notification event payload", ex);
        }
    }

    @Override
    public SplitwiseNotificationEvent convertToEntityAttribute(String dbData) {
        try {
            return OBJECT_MAPPER.readValue(dbData, SplitwiseNotificationEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to deserialize splitwise notification event payload", ex);
        }
    }
}
