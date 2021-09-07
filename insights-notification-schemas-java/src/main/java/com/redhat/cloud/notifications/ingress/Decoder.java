package com.redhat.cloud.notifications.ingress;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;

import java.io.IOException;

public class Decoder {

    private static final String VERSION_PROPERTY = "version";
    private static final String DEFAULT_VERSION = "v1.0.0";

    private final Registry registry = new Registry();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Action decode(String actionJson) {
        String version = getVersion(actionJson);
        try {
            Action action = new Action();
            Schema schemaWriter = registry.getSchema(version);
            Schema schemaReader = Action.getClassSchema();
            JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schemaWriter, actionJson);
            DatumReader<Action> reader = new SpecificDatumReader<Action>(schemaWriter, schemaReader, SpecificData.getForClass(Action.class));
            reader.read(action, jsonDecoder);
            return action;
        } catch (IOException ioException) {
            throw new RuntimeException("Unable to decode action", ioException);
        }
    }

    public String getVersion(String actionJson) {
        try {
            return objectMapper.readTree(actionJson).get(VERSION_PROPERTY).asText(DEFAULT_VERSION);
        } catch (IOException ioException) {
            throw new RuntimeException("Error while reading version", ioException);
        } catch (NullPointerException npe) {
            // property does not exist
            return DEFAULT_VERSION;
        }
    }
}
