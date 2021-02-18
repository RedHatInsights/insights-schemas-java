package com.redhat.cloud.notifications;

import com.redhat.cloud.notifications.ingress.Action;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestHelpers {

    public static String serializeAction(Action action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(Action.getClassSchema(), baos);
        DatumWriter<Action> writer = new SpecificDatumWriter<>(Action.class);
        writer.write(action, jsonEncoder);
        jsonEncoder.flush();

        return baos.toString(StandardCharsets.UTF_8);
    }

    public static Action deserializeAction(String actionString) throws IOException {
        Action action = new Action();
        JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(Action.getClassSchema(), actionString);
        DatumReader<Action> reader = new SpecificDatumReader<>(Action.class);
        reader.read(action, jsonDecoder);

        return action;
    }

}
