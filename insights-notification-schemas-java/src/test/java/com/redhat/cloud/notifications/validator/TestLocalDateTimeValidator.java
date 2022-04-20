package com.redhat.cloud.notifications.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLocalDateTimeValidator {

    @Test
    void shouldParseIsoDates() {
        LocalDateTimeValidator validator = new LocalDateTimeValidator();

        assertTrue(validator.matches("2020-07-14T13:22:10.133"));
        assertTrue(validator.matches("2020-07-14T13:22:10"));
        assertTrue(validator.matches("2020-07-14T13:22:10Z"));

        // Week format
        assertFalse(validator.matches("2007-W44-6T16:18:05Z"));

        // Text
        assertFalse(validator.matches("Tomorrow"));
        assertFalse(validator.matches("As soon as possible!!"));

        // Only date or time
        assertFalse(validator.matches("2020-07-14"));
        assertFalse(validator.matches("22:10:10"));
    }

}
