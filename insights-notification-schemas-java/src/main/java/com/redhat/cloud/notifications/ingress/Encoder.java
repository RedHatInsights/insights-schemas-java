package com.redhat.cloud.notifications.ingress;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Encoder {

    public String encode(Action action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(Action.getClassSchema(), baos);
        DatumWriter<Action> writer = new SpecificDatumWriter<>(Action.class);
        writer.write(action, jsonEncoder);
        jsonEncoder.flush();

        return baos.toString(StandardCharsets.UTF_8);
    }
}
