package com.redhat.cloud.notifications.ingress;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestSerialization {

    private Decoder decoder = new Decoder();
    private Encoder encoder = new Encoder();

    @Test
    void testActionSerialization() throws Exception {
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
    void deserializeWithV1_0_0() throws Exception {
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
}
