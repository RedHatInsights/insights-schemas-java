package com.redhat.cloud.notifications.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public class Decoder {

    private static final String VERSION_PROPERTY = "version";
    private static final String DEFAULT_VERSION = "v1.0.0";

    private final Registry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Decoder(Registry registry) {
        this.registry = registry;
    }

    public Action decode(String actionJson) {
        return this.decode(actionJson, Optional.empty(), Action.class);
    }

    public GenericRecord decode(String actionJson, String readerVersion) {
        return this.decode(actionJson, Optional.of(readerVersion), GenericData.Record.class);
    }

    private <T extends GenericContainer> T decode(String actionJson, Optional<String> readerVersion, Class<T> returnClass) {
        if (!returnClass.isAssignableFrom(Action.class) && returnClass.isAssignableFrom(GenericRecord.class)) {
            throw new IllegalArgumentException("Invalid return class supplied: " + returnClass);
        }

        String actionVersion = getVersion(actionJson);
        try {
            Schema schemaWriter = registry.getSchema(actionVersion);
            Schema schemaReader = readerVersion.isPresent() ? registry.getSchema(readerVersion.get()) : Action.getClassSchema();
            JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schemaWriter, actionJson);

            DatumReader<T> reader;
            T action;
            if (returnClass.equals(Action.class)) {
                reader = new SpecificDatumReader<T>(schemaWriter, schemaReader, SpecificData.getForClass(returnClass));
                action = returnClass.getDeclaredConstructor().newInstance();
            } else {
                reader = new GenericDatumReader<>(schemaWriter, schemaReader);
                action = returnClass.getDeclaredConstructor(Schema.class).newInstance(schemaWriter);
            }

            reader.read(action, jsonDecoder);
            return action;
        } catch (IOException ioException) {
            throw new UncheckedIOException("Unable to decode action", ioException);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new RuntimeException("Error while trying to create action class", reflectiveOperationException);
        }
    }

    public String getVersion(String actionJson) {
        try {
            JsonNode versionProperty = objectMapper.readTree(actionJson).get(VERSION_PROPERTY);
            if (versionProperty == null) {
                return DEFAULT_VERSION;
            }

            return versionProperty.asText(DEFAULT_VERSION);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Error while reading version", ioException);
        }
    }
}
