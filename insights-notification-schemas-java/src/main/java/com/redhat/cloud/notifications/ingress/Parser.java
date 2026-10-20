package com.redhat.cloud.notifications.ingress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.walk.ApplyDefaultsStrategy;
import com.networknt.schema.format.Formats;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.BasicDialectRegistry;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.keyword.AnnotationKeyword;
import com.networknt.schema.path.PathType;
import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.Result;
import com.redhat.cloud.notifications.jackson.LocalDateTimeModule;
import com.redhat.cloud.notifications.validator.LocalDateTimeValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

public class Parser {

    final static ObjectMapper objectMapper = new ObjectMapper();
    private final static Schema jsonSchema;

    private final static String ACTION_SCHEMA_PATH = "/schemas/Action.json";
    private final static String ACTION_OUT_SCHEMA_PATH = "/schemas/Action-out.json";

    private final static String CONTEXT_FIELD = "context";
    private final static String EVENTS_FIELD = "events";
    private final static String PAYLOAD_FIELD = "payload";

    static {
        jsonSchema = getJsonSchema(ACTION_SCHEMA_PATH);
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
    public static void validate(JsonNode action, Schema jsonSchema) {
        Result result = jsonSchema.walk(action, true, executionContext -> {
            executionContext.walkConfig(walkConfig -> {
                walkConfig.applyDefaultsStrategy(applyDefaults -> {
                    applyDefaults.applyArrayDefaults(true)
                        .applyPropertyDefaults(true)
                        .applyPropertyDefaultsIfNull(true);
                });
            });
        });

        if (!result.getErrors().isEmpty()) {
            throw new ParsingException(result.getErrors());
        }
    }

    /**
     * Validates an "Action out" and ensures that all the values conform to the schema.
     * @param actionOut ActionOut to be validated.
     */
    public static void validate(final ActionOut actionOut) {
        final Schema actionOutJsonSchema = getJsonSchema(ACTION_OUT_SCHEMA_PATH);

        validate(objectMapper.valueToTree(actionOut), actionOutJsonSchema);
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

    private static Schema getJsonSchema(final String schemaPath) {
        SchemaRegistryConfig schemaRegistryConfig = SchemaRegistryConfig.builder()
            .pathType(PathType.LEGACY)
            .errorMessageKeyword("message")
            .build();

        try (InputStream jsonSchemaStream = Parser.class.getResourceAsStream(schemaPath)) {
            JsonNode schema = objectMapper.readTree(jsonSchemaStream);

            SchemaRegistry schemaRegistry = jsonSchemaFactory(schemaRegistryConfig);
            return schemaRegistry.getSchema(com.networknt.schema.SchemaLocation.of(schemaPath), schema);
        } catch (IOException ioe) {
            throw new SchemaException(ioe);
        }
    }

    private static SchemaRegistry jsonSchemaFactory(SchemaRegistryConfig schemaRegistryConfig) {
        String ID = "$id";

        Dialect overrideDateTimeValidator = Dialect.builder(Dialects.getDraft7())
                .idKeyword(ID)
                .keywords(keywords -> {
                    keywords.put("title", new AnnotationKeyword("title"));
                    keywords.put("$comment", new AnnotationKeyword("$comment"));
                    keywords.put("description", new AnnotationKeyword("description"));
                    keywords.put("default", new AnnotationKeyword("default"));
                })
                .format(new LocalDateTimeValidator())
                .specificationVersion(SpecificationVersion.DRAFT_7)
                .build();

        BasicDialectRegistry dialectRegistry = new BasicDialectRegistry(List.of(overrideDateTimeValidator));

        return SchemaRegistry.builder()
                .defaultDialectId(overrideDateTimeValidator.getId())
                .dialectRegistry(dialectRegistry)
                .schemaRegistryConfig(schemaRegistryConfig)
                .build();
    }
}
