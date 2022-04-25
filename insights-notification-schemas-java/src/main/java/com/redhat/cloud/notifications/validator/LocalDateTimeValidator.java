package com.redhat.cloud.notifications.validator;

import com.networknt.schema.Format;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class LocalDateTimeValidator implements Format {

    private String message;

    @Override
    public String getName() {
        return "date-time";
    }

    @Override
    public boolean matches(String text) {
        try {
            TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_DATE_TIME.parse(text);
            if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
                // Dates and times have to be expressed in UTC. Values with an offset are considered invalid.
                return temporalAccessor.get(ChronoField.OFFSET_SECONDS) == 0;
            }
            return true;
        } catch (DateTimeParseException exception) {
            message = exception.getMessage();
            return false;
        }
    }

    @Override
    public String getErrorMessageDescription() {
        return message;
    }
}
