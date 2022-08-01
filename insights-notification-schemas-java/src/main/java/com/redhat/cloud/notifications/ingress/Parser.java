package com.redhat.cloud.notifications.ingress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ApplyDefaultsStrategy;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidatorTypeCode;
import com.networknt.schema.ValidationResult;
import com.redhat.cloud.notifications.jackson.LocalDateTimeModule;
import com.redhat.cloud.notifications.validator.LocalDateTimeValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

public class Parser {

    final static ObjectMapper objectMapper = new ObjectMapper();
    private final static JsonSchema jsonSchema;

    private final static String CONTEXT_FIELD = "context";
    private final static String EVENTS_FIELD = "events";
    private final static String PAYLOAD_FIELD = "payload";

    static {
        jsonSchema = getJsonSchema();
        objectMapper.registerModule(new LocalDateTimeModule());
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
            updateContextAndPayload(action, objectMapper);
            validate(action, jsonSchema);

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
            validate(asNode, jsonSchema);

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
        validate(objectMapper.valueToTree(action), jsonSchema);
    }

    /**
     * Validates action and ensures all the values conform to the schema.
     * @param actionJson string of json encoded action to be validated
     *                   Note that this validate does not perform the string-to-json of
     *                   the context and events[*].payload fields and are reported as
     *                   errors.
     */
    public static void validate(String actionJson) {
        try {
            JsonNode action = objectMapper.readTree(actionJson);
            validate(action, jsonSchema);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("Unable to decode action", exception);
        }
    }

    /**
     * Validates action and ensures all the values conform to the schema.
     * @param action JsonNode to be validated
     *                   Note that this validate does not perform the string-to-json of
     *                   the context and events[*].payload fields and are reported as
     *                   errors.
     */
    public static void validate(JsonNode action, JsonSchema jsonSchema) {
        ValidationResult result = jsonSchema.walk(action, true);

        if (result.getValidationMessages().size() > 0) {
            throw new ParsingException(result.getValidationMessages());
        }
    }

    // context and events[].payload could be strings, change these values to jsons objects.
    private static void updateContextAndPayload(JsonNode action, ObjectMapper objectMapper) throws JsonProcessingException {
        parseFieldIfNeeded(action, CONTEXT_FIELD, objectMapper);

        if (action.has(EVENTS_FIELD)) {
            JsonNode events = action.get(EVENTS_FIELD);
            if (events.getNodeType() == JsonNodeType.ARRAY) {
                for (int i = 0; i < events.size(); i++) {
                    parseFieldIfNeeded(events.get(i), PAYLOAD_FIELD, objectMapper);
                }
            }
        }
    }

    private static void parseFieldIfNeeded(JsonNode container, String field, ObjectMapper objectMapper) throws JsonProcessingException {
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

        try (InputStream jsonSchemaStream = Parser.class.getResourceAsStream("/schemas/Action.json")) {
            JsonNode schema = objectMapper.readTree(jsonSchemaStream);

            return jsonSchemaFactory().getSchema(
                    schema,
                    schemaValidatorsConfig
            );
        } catch (IOException ioe) {
            throw new JsonSchemaException(ioe);
        }
    }

    private static JsonSchemaFactory jsonSchemaFactory() {
        String ID = "$id";

        JsonMetaSchema overrideDateTimeValidator = new JsonMetaSchema.Builder(JsonMetaSchema.getV7().getUri())
                .idKeyword(ID)
                .addKeywords(ValidatorTypeCode.getNonFormatKeywords(SpecVersion.VersionFlag.V7))
                .addKeywords(List.of(
                        new NonValidationKeyword("title"),
                        new NonValidationKeyword("$comment"),
                        new NonValidationKeyword("description"),
                        new NonValidationKeyword("default")
                ))
                .addFormats(JsonMetaSchema.COMMON_BUILTIN_FORMATS)
                .addFormat(new LocalDateTimeValidator())
                .build();

        return new JsonSchemaFactory.Builder().defaultMetaSchemaURI(overrideDateTimeValidator.getUri())
                .addMetaSchema(overrideDateTimeValidator)
                .build();

    }
}
