package com.redhat.cloud.notifications.jackson.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        if (jsonParser.hasTokenId(JsonTokenId.ID_STRING)) {
            // One of the tenants is sending the datetime with a 0-offset - ISO_LOCAL_DATE_TIME fails to parse it.
            TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_DATE_TIME.parse(jsonParser.getText());
            return LocalDateTime.from(temporalAccessor);
        }

        return (LocalDateTime) deserializationContext.handleUnexpectedToken(LocalDateTime.class, jsonParser);
    }
}
