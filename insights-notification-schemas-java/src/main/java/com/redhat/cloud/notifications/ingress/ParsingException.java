package com.redhat.cloud.notifications.ingress;

import com.networknt.schema.Error;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ParsingException extends RuntimeException {

    private Set<ErrorWrapper> validationMessages;

    public ParsingException(List<Error> validationMessages) {
        super("Validation failed: " + formatErrors(validationMessages));
        this.validationMessages = validationMessages.stream()
            .map(ErrorWrapper::new)
            .collect(Collectors.toSet());
    }

    private static String formatErrors(List<Error> errors) {
        return errors.stream()
            .map(error -> {
                String location = error.getInstanceLocation().toString();
                String message = error.getMessage();
                if (location != null && !location.isEmpty() && !location.equals("$")) {
                    return location + ": " + message;
                }
                return message;
            })
            .collect(Collectors.joining(", "));
    }

    public Set<ErrorWrapper> getValidationMessages() {
        return validationMessages;
    }

    /**
     * Wrapper class to maintain backward compatibility with the old ValidationMessage API
     * while using the new Error class from json-schema-validator 2.0.0
     */
    public static class ErrorWrapper {
        private final Error error;

        public ErrorWrapper(Error error) {
            this.error = error;
        }

        public String getMessage() {
            String location = error.getInstanceLocation().toString();
            String message = error.getMessage();
            if (location != null && !location.isEmpty() && !location.equals("$")) {
                return location + ": " + message;
            }
            return message;
        }

        public Error getError() {
            return error;
        }

        @Override
        public String toString() {
            return getMessage();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ErrorWrapper that = (ErrorWrapper) o;
            return error.equals(that.error);
        }

        @Override
        public int hashCode() {
            return error.hashCode();
        }
    }
}
