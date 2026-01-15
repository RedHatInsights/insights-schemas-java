package com.redhat.cloud.notifications.validator;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.format.Format;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeValidator implements Format {

    private String message;

    @Override
    public String getName() {
        return "date-time";
    }

    @Override
    public boolean matches(ExecutionContext executionContext, String text) {
        return matches(text);
    }

    public boolean matches(String text) {
        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(text);
            return true;
        } catch (DateTimeParseException exception) {
            message = exception.getMessage();
            return false;
        }
    }

    @Override
    public String getMessageKey() {
        return message;
    }
}
