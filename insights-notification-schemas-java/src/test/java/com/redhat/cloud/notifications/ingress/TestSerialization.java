package com.redhat.cloud.notifications.ingress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSerialization {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testActionSerialization() {
        Action targetAction = new Action.ActionBuilder()
                .withVersion("v1.1.0")
                .withBundle("my-bundle")
                .withApplication("Policies")
                .withTimestamp(LocalDateTime.now())
                .withEventType("Any")
                .withAccountId("testTenant")
                .withContext(
                        new Context.ContextBuilder()
                                .withAdditionalProperty("user_id", "123456-7890")
                                .withAdditionalProperty("user_name", "foobar")
                                .build()
                )
                .withEvents(List.of(
                        new Event.EventBuilder()
                                .withMetadata(new Metadata())
                                .withPayload(
                                        new Payload.PayloadBuilder()
                                                .withAdditionalProperty("k", "v")
                                                .withAdditionalProperty("k2", "v2")
                                                .withAdditionalProperty("k3", "v")
                                                .build()
                                ).build(),
                        new Event.EventBuilder()
                                .withMetadata(new Metadata())
                                .withPayload(
                                        new Payload.PayloadBuilder()
                                                .withAdditionalProperty("k", "b")
                                                .withAdditionalProperty("k2", "b2")
                                                .withAdditionalProperty("k3", "b")
                                                .build()
                                ).build()
                ))
                .build();

        String serializedAction = Parser.encode(targetAction);
        System.out.println(serializedAction);
        Action deserializedAction = Parser.decode(serializedAction);

        assertNotNull(deserializedAction);
        assertEquals(targetAction.getAccountId(), deserializedAction.getAccountId());
        assertEquals("123456-7890", deserializedAction.getContext().getAdditionalProperties().get("user_id"));
        assertEquals("foobar", deserializedAction.getContext().getAdditionalProperties().get("user_name"));

        assertEquals(2, deserializedAction.getEvents().size());
        assertEquals("v2", deserializedAction.getEvents().get(0).getPayload().getAdditionalProperties().get("k2"));
        assertEquals("b2", deserializedAction.getEvents().get(1).getPayload().getAdditionalProperties().get("k2"));
    }

    @Test
    void deserializeWithStringContextAndPayload() {
        String serializedWithoutRecipients = "{\"bundle\":\"my-bundle\",\"application\":\"Policies\",\"event_type\":\"Any\",\"timestamp\":\"2021-08-24T16:36:31.806149\",\"account_id\":\"testTenant\",\"context\":\"{\\\"user_id\\\":\\\"123456-7890\\\",\\\"user_name\\\":\\\"foobar\\\"}\",\"events\":[{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"v2\\\",\\\"k3\\\":\\\"v\\\",\\\"k\\\":\\\"v\\\"}\"},{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"b2\\\",\\\"k3\\\":\\\"b\\\",\\\"k\\\":\\\"b\\\"}\"}]}\n";
        Action deserializedAction = Parser.decode(serializedWithoutRecipients);
        assertNotNull(deserializedAction);
        assertEquals("123456-7890", deserializedAction.getContext().getAdditionalProperties().get("user_id"));
        assertEquals("foobar", deserializedAction.getContext().getAdditionalProperties().get("user_name"));

        assertEquals(2, deserializedAction.getEvents().size());
        assertEquals("v2", deserializedAction.getEvents().get(0).getPayload().getAdditionalProperties().get("k2"));
        assertEquals("b2", deserializedAction.getEvents().get(1).getPayload().getAdditionalProperties().get("k2"));
        assertEquals("v1.0.0", deserializedAction.getVersion());
    }

    @Test
    void shouldFailWithoutARequiredField() throws JsonProcessingException {
        String template = "{\"recipients\":[], \"bundle\":\"a-bundle\", \"application\":\"Policies\",\"event_type\":\"Any\",\"timestamp\":\"2021-08-24T16:36:31.806149\",\"account_id\":\"testTenant\",\"org_id\":\"testTenant\",\"context\":\"{\\\"user_id\\\":\\\"123456-7890\\\",\\\"user_name\\\":\\\"foobar\\\"}\",\"events\":[{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"v2\\\",\\\"k3\\\":\\\"v\\\",\\\"k\\\":\\\"v\\\"}\"},{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"b2\\\",\\\"k3\\\":\\\"b\\\",\\\"k\\\":\\\"b\\\"}\"}]}\n";

        testRequiredField("bundle", true, template);
        testRequiredField("application", true, template);
        testRequiredField("event_type", true, template);
        testRequiredField("timestamp", true, template);
        testRequiredField("account_id", true, template);
        // optional for now
        testRequiredField("org_id", false, template);
        testRequiredField("context", false, template);
        testRequiredField("events", true, template);
        testRequiredField("events.0.metadata", false, template);
        testRequiredField("events.0.payload", true, template);
        testRequiredField("recipients", false, template);

    }

    @Test
    void shouldDeserializeISO8601Dates() {
        testDate(
                "2020-07-14T13:22:10Z",
                LocalDateTime.of(
                        2020,
                        7,
                        14,
                        13,
                        22,
                        10
                )
        );

        testDate(
                "2020-07-14T13:22:10.133",
                LocalDateTime.of(
                        2020,
                        7,
                        14,
                        13,
                        22,
                        10,
                        133000000
                )
        );
    }

    private void testDate(String stringAsDate, LocalDateTime dateTime) {
        String serialized = String.format(
                "{\"bundle\":\"my-bundle\",\"application\":\"Policies\",\"event_type\":\"Any\",\"timestamp\":\"%s\",\"account_id\":\"testTenant\",\"context\":{},\"events\":[]}\n",
                stringAsDate
        );
        assertEquals(
                dateTime,
                Parser.decode(serialized).getTimestamp()
        );
    }

    private Optional<Integer> getInteger(String string) {
        try {
            return Optional.of(Integer.parseInt(string));
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    private boolean hasField(JsonNode node, String field) {
        Optional<Integer> numericField = getInteger(field);
        if (numericField.isPresent()) {
            return node.has(numericField.get());
        }

        return node.has(field);
    }

    private JsonNode getField(JsonNode node, String field) {
        Optional<Integer> numericField = getInteger(field);
        if (numericField.isPresent()) {
            return node.get(numericField.get());
        }

        return node.get(field);
    }

    private void testRequiredField(String field, boolean isRequired, String template) throws JsonProcessingException {
        JsonNode base = objectMapper.readTree(template);
        String[] steps = field.split("\\.");

        JsonNode node = base;
        for (int i = 0; i < steps.length; i++) {
            String step = steps[i];

            if (!hasField(node, step)) {
                throw new RuntimeException("Field " + field + " does not exist in the template - unable to remove it. Current step " + step);
            }

            if (i == steps.length - 1) {
                ((ObjectNode)node).remove(step);
            } else {
                node = getField(node, step);
            }
        }

        String serialized = objectMapper.writeValueAsString(base);

        Executable parsing = () -> Parser.decode(serialized);

        if (isRequired) {
            assertThrows(ParsingException.class, parsing);
        } else {
            assertDoesNotThrow(parsing);
        }

    }

}
