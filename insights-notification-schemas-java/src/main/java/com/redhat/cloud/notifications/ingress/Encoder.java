package com.redhat.cloud.notifications.ingress;

import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class Encoder {

    public String encode(Action action) {
        return _encode(action);
    }

    public String encode(GenericRecord action) {
        return _encode(action);
    }

    private <T extends GenericContainer> String _encode(T action) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(action.getSchema(), baos);

            DatumWriter<T> writer;
            if (action instanceof Action) {
                writer = new SpecificDatumWriter<T>((Class<T>) action.getClass());
            } else {
                writer = new GenericDatumWriter<>(action.getSchema());
            }

            writer.write(action, jsonEncoder);
            jsonEncoder.flush();

            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Unable to encode action", ioException);
        }
    }
}
