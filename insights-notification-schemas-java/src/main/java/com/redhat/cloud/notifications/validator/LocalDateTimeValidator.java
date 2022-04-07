package com.redhat.cloud.notifications.validator;

import com.networknt.schema.Format;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeValidator implements Format {

    private String message;

    @Override
    public String getName() {
        return "date-time";
    }

    @Override
    public boolean matches(String s) {
        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(s);
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
