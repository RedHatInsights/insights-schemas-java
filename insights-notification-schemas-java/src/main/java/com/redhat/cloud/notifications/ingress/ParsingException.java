package com.redhat.cloud.notifications.ingress;

import com.networknt.schema.ValidationMessage;

import java.util.Set;

public class ParsingException extends RuntimeException {

    private Set<ValidationMessage> validationMessages;

    public ParsingException(Set<ValidationMessage> validationMessages) {
        super("Validation failed: " + validationMessages.toString());
        this.validationMessages = validationMessages;
    }

    public Set<ValidationMessage> getValidationMessages() {
        return validationMessages;
    }
}
