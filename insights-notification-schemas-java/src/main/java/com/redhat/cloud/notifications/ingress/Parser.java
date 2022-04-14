package com.redhat.cloud.notifications.ingress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.ApplyDefaultsStrategy;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidatorTypeCode;
import com.networknt.schema.ValidationResult;
import com.redhat.cloud.notifications.validator.LocalDateTimeValidator;

import java.io.UncheckedIOException;

public class Parser {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static JsonSchema jsonSchema;

    private final static String CONTEXT_FIELD = "context";
    private final static String EVENTS_FIELD = "events";
    private final static String PAYLOAD_FIELD = "payload";

    static {
        jsonSchema = getJsonSchema();

        // Provides LocalDateTime support
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Validates and decodes the json string to an Action
     * - Default values are set for supported values (see schema)
     * @param actionJson json-serialized Action
     * @return Action valid Action
     */
    public static Action decode(String actionJson) {
        try {
            JsonNode action = objectMapper.readTree(actionJson);
            updateContextAndPayload(action);
            validate(action);

            return objectMapper.treeToValue(action, Action.class);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("Unable to decode action", exception);
        }
    }

    /**
     * Validates and encodes an Action to a json-string
     * - Default values are set for supported values (see schema)
     * @param action Action to be encoded
     * @return String json-serialized action.
     */
    public static String encode(Action action) {
        try {
            JsonNode asNode = objectMapper.valueToTree(action);
            validate(asNode);

            return objectMapper.writeValueAsString(asNode);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("Unable to encode action", exception);
        }
    }

    /**
     * Validates action and ensures all the values conform to the schema.
     * @param action to be validated
     */
    public static void validate(Action action) {
        validate(objectMapper.valueToTree(action));
    }

    private static void validate(JsonNode action) {
        ValidationResult result = jsonSchema.walk(action, true);

        if (result.getValidationMessages().size() > 0) {
            throw new ParsingException(result.getValidationMessages());
        }
    }

    // context and events[].payload could be strings, change these values to jsons objects.
    private static void updateContextAndPayload(JsonNode action) throws JsonProcessingException {
        parseFieldIfNeeded(action, CONTEXT_FIELD);

        if (action.has(EVENTS_FIELD)) {
            JsonNode events = action.get(EVENTS_FIELD);
            if (events.getNodeType() == JsonNodeType.ARRAY) {
                for (int i = 0; i < events.size(); i++) {
                    parseFieldIfNeeded(events.get(i), PAYLOAD_FIELD);
                }
            }
        }
    }

    private static void parseFieldIfNeeded(JsonNode container, String field) throws JsonProcessingException {
        if (container.has(field)) {
            JsonNode target = container.get(field);
            if (target.getNodeType() == JsonNodeType.STRING) {
                ((ObjectNode)container).replace(field, objectMapper.readTree(target.asText()));
            }
        }
    }

    private static JsonSchema getJsonSchema() {
        SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
        schemaValidatorsConfig.setApplyDefaultsStrategy(new ApplyDefaultsStrategy(
                true,
                true,
                true
        ));

        return jsonSchemaFactory().getSchema(
                Parser.class.getResourceAsStream("/schemas/Action.json"),
                schemaValidatorsConfig
        );
    }

    private static JsonSchemaFactory jsonSchemaFactory() {
        String ID = "$id";

        JsonMetaSchema overrideDateTimeValidator = new JsonMetaSchema.Builder(JsonMetaSchema.getV7().getUri())
                .idKeyword(ID)
                .addKeywords(ValidatorTypeCode.getNonFormatKeywords(SpecVersion.VersionFlag.V7))
                .addFormats(JsonMetaSchema.COMMON_BUILTIN_FORMATS)
                .addFormat(new LocalDateTimeValidator())
                .build();

        return new JsonSchemaFactory.Builder().defaultMetaSchemaURI(overrideDateTimeValidator.getUri())
                .addMetaSchema(overrideDateTimeValidator)
                .build();

    }
}
