package com.redhat.cloud.notifications.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser.hasTokenId(JsonTokenId.ID_STRING)) {
            TemporalAccessor temporalAccessor = dateTimeFormatter.parse(jsonParser.getText());
            return OffsetDateTime.from(temporalAccessor).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        }

        return (LocalDateTime) deserializationContext.handleUnexpectedToken(LocalDateTime.class, jsonParser);
    }
}
