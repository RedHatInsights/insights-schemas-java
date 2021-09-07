package com.redhat.cloud.notifications.ingress;

import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Registry {

    private final Map<String, Schema> avroSchemas = new HashMap<>();

    public Schema getSchema(String version) {

        if (!avroSchemas.containsKey(version)) {
            InputStream avroInputStream = getClass().getResourceAsStream(String.format(
                    "/avro/action-%s.avsc", version
            ));

            if (avroInputStream == null) {
                throw new RuntimeException(String.format("Version %s not found", version));
            }

            try {
                avroSchemas.put(
                        version,
                        new Schema.Parser().parse(avroInputStream)
                );
            } catch (IOException ioException) {
                throw new RuntimeException(String.format("Unable to load version %s", version), ioException);
            }
        }

        return avroSchemas.get(version);
    }

}
