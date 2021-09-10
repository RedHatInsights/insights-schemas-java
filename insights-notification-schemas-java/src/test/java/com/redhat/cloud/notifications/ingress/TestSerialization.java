package com.redhat.cloud.notifications.ingress;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestSerialization {

    @Test
    void testActionSerialization() {

        Registry registry = new Registry();
        Decoder decoder = new Decoder(registry);
        Encoder encoder = new Encoder();

        Action targetAction = new Action();
        targetAction.setVersion("v1.1.0");
        targetAction.setBundle("my-bundle");
        targetAction.setApplication("Policies");
        targetAction.setTimestamp(LocalDateTime.now());
        targetAction.setEventType("Any");
        targetAction.setAccountId("testTenant");
        Map<String, String> context = new HashMap<>();
        context.put("user_id", "123456-7890");
        context.put("user_name", "foobar");

        Map<String, String> payload1 = new HashMap<>();
        payload1.put("k", "v");
        payload1.put("k2", "v2");
        payload1.put("k3", "v");

        Map<String, String> payload2 = new HashMap<>();
        payload2.put("k", "b");
        payload2.put("k2", "b2");
        payload2.put("k3", "b");

        ArrayList<Event> events = new ArrayList<>();
        Event event1 = Event.newBuilder()
                .setMetadata(Metadata.newBuilder().build())
                .setPayload(payload1)
                .build();
        Event event2 = Event.newBuilder()
                .setMetadata(Metadata.newBuilder().build())
                .setPayload(payload2)
                .build();

        events.add(event1);
        events.add(event2);

        targetAction.setEvents(events);
        targetAction.setContext(context);

        String serializedAction = encoder.encode(targetAction);

        Action deserializedAction = decoder.decode(serializedAction);
        assertNotNull(deserializedAction);
        assertEquals(targetAction.getAccountId(), deserializedAction.getAccountId());

        assertEquals("123456-7890", deserializedAction.getContext().get("user_id"));
        assertEquals("foobar", deserializedAction.getContext().get("user_name"));

        assertEquals(2, deserializedAction.getEvents().size());
        assertEquals("v2", deserializedAction.getEvents().get(0).getPayload().get("k2"));
        assertEquals("b2", deserializedAction.getEvents().get(1).getPayload().get("k2"));
    }

    @Test
    void deserializeWithV1_0_0() {
        Registry registry = new Registry();
        Decoder decoder = new Decoder(registry);
        String serializedWithoutRecipients = "{\"bundle\":\"my-bundle\",\"application\":\"Policies\",\"event_type\":\"Any\",\"timestamp\":\"2021-08-24T16:36:31.806149\",\"account_id\":\"testTenant\",\"context\":\"{\\\"user_id\\\":\\\"123456-7890\\\",\\\"user_name\\\":\\\"foobar\\\"}\",\"events\":[{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"v2\\\",\\\"k3\\\":\\\"v\\\",\\\"k\\\":\\\"v\\\"}\"},{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"b2\\\",\\\"k3\\\":\\\"b\\\",\\\"k\\\":\\\"b\\\"}\"}]}\n";
        Action deserializedAction = decoder.decode(serializedWithoutRecipients);
        assertNotNull(deserializedAction);
        assertEquals("123456-7890", deserializedAction.getContext().get("user_id"));
        assertEquals("foobar", deserializedAction.getContext().get("user_name"));

        assertEquals(2, deserializedAction.getEvents().size());
        assertEquals("v2", deserializedAction.getEvents().get(0).getPayload().get("k2"));
        assertEquals("b2", deserializedAction.getEvents().get(1).getPayload().get("k2"));
        assertEquals("v1.0.0", deserializedAction.getVersion());
    }

    @Test
    void encodeAndDecodeV1_0_0() {
        Registry registry = new Registry();
        Encoder encoder = new Encoder();
        Decoder decoder = new Decoder(registry);

        GenericData.Record action = new GenericData.Record(registry.getSchema("v1.0.0"));

        action.put("bundle", "my bundle");
        action.put("application", "policies");
        action.put("event_type", "sent-stuff");
        action.put("timestamp", "2021-08-24T16:36:31.806149");
        action.put("account_id", "123456");
        action.put("context", "{}");
        action.put("events", List.of());

        // This version does not have a "version" field
        // Assertions.assertThrows(AvroRuntimeException.class, () -> action.put("version", "v1.0.0"));

        String encoded = encoder.encode(action);

        System.out.println("encoded:" + encoded);

        GenericRecord decoded = decoder.decode(encoded, "v1.0.0");

        assertEquals("my bundle", decoded.get("bundle").toString());
        assertEquals("policies", decoded.get("application").toString());
        assertEquals("sent-stuff", decoded.get("event_type").toString());
        assertEquals("2021-08-24T16:36:31.806149", decoded.get("timestamp").toString());
        assertEquals("123456", decoded.get("account_id").toString());
        assertEquals("{}", decoded.get("context").toString());
        assertEquals(List.of(), decoded.get("events"));

        // Decoded does not have "version" field
        Assertions.assertThrows(AvroRuntimeException.class, () -> decoded.put("version", "v1.0.0"));

    }
}
