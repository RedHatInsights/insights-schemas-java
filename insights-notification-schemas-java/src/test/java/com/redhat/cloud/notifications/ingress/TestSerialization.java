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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("2.0.0", deserializedAction.getVersion());
    }

    @Test
    void shouldFailWithoutARequiredField() throws JsonProcessingException {
        String template = "{\"recipients\":[], \"bundle\":\"a-bundle\", \"application\":\"Policies\",\"event_type\":\"Any\",\"timestamp\":\"2021-08-24T16:36:31.806149\",\"account_id\":\"testTenant\",\"org_id\":\"testTenant\",\"context\":\"{\\\"user_id\\\":\\\"123456-7890\\\",\\\"user_name\\\":\\\"foobar\\\"}\",\"events\":[{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"v2\\\",\\\"k3\\\":\\\"v\\\",\\\"k\\\":\\\"v\\\"}\"},{\"metadata\":{},\"payload\":\"{\\\"k2\\\":\\\"b2\\\",\\\"k3\\\":\\\"b\\\",\\\"k\\\":\\\"b\\\"}\"}]}\n";

        // required
        testRequiredField("bundle", true, template);
        testRequiredField("application", true, template);
        testRequiredField("event_type", true, template);
        testRequiredField("timestamp", true, template);
        testRequiredField("account_id", true, template);
        testRequiredField("events", true, template);
        testRequiredField("events.0.payload", true, template);

        // optional
        testRequiredField("org_id", false, template);
        testRequiredField("context", false, template);
        testRequiredField("events.0.metadata", false, template);
        testRequiredField("recipients", false, template);
    }

     @Test
     void shouldHaveDefaultValuesWhenNotSet() {
         Action action = getValidAction();
         action.setContext(null);
         action.setRecipients(null);
         action.setVersion(null);

         Action otherAction = Parser.decode(Parser.encode(action));

         assertNotNull(otherAction.getContext());
         assertNotNull(otherAction.getRecipients());
         assertEquals("2.0.0", otherAction.getVersion());

         action = getValidAction();
         action.getRecipients().get(0).setIgnoreUserPreferences(null);
         action.getRecipients().get(0).setOnlyAdmins(null);
         action.getRecipients().get(0).setGroups(null);
         action.getRecipients().get(0).setUsers(null);

         otherAction = Parser.decode(Parser.encode(action));

         assertEquals(Boolean.FALSE, otherAction.getRecipients().get(0).getIgnoreUserPreferences());
         assertEquals(Boolean.FALSE, otherAction.getRecipients().get(0).getOnlyAdmins());
         assertEquals(List.of(), otherAction.getRecipients().get(0).getGroups());
         assertEquals(List.of(), otherAction.getRecipients().get(0).getUsers());
     }

     @Test
     void shouldAcceptExtraPropertiesAtEventLevelWhenDeserializingAndFailIfSerializingOrValidating() {
         String jsonAction = "{\"bundle\": \"rhel\", \"application\": \"advisor\", \"event_type\": \"new-recommendation\", \"timestamp\": \"2022-05-02T19:47:15.626507+00:00\", \"account_id\": \"6089719\", \"context\": \"{\\\"inventory_id\\\": \\\"4c548b3c-b816-4d69-97cf-d8046fc10139\\\", \\\"hostname\\\": \\\"test_device_api_cvkg\\\", \\\"display_name\\\": \\\"test_device_api_cvkg\\\", \\\"rhel_version\\\": \\\"8.4\\\", \\\"host_url\\\": \\\"https://console.redhat.com/insights/inventory/4c548b3c-b816-4d69-97cf-d8046fc10139\\\", \\\"tags\\\": [{\\\"key\\\": \\\"jctxJ\\\", \\\"value\\\": \\\"dJjCYQP\\\", \\\"namespace\\\": \\\"NtLDHI\\\"}, {\\\"key\\\": \\\"sxrhV\\\", \\\"value\\\": \\\"kZjZrMHs\\\", \\\"namespace\\\": \\\"ruRyTu\\\"}, {\\\"key\\\": \\\"czrNqdS\\\", \\\"value\\\": \\\"MQkQcqgn\\\", \\\"namespace\\\": \\\"kLGZLDf\\\"}, {\\\"key\\\": \\\"PtWmHtuj\\\", \\\"value\\\": \\\"SahNXHzIPG\\\", \\\"namespace\\\": \\\"xeMNCpmnI\\\"}, {\\\"key\\\": \\\"OFSMsL\\\", \\\"value\\\": \\\"nWckNHkYVf\\\", \\\"namespace\\\": \\\"LzBnzpDZZA\\\"}]}\", \"events\": [{\"metadata\": {}, \"payload\": \"{\\\"rule_id\\\": \\\"hardening_grub|GRUB_HARDENING_3\\\", \\\"rule_description\\\": \\\"Decreased security: GRUB configuration permissions\\\", \\\"total_risk\\\": \\\"2\\\", \\\"publish_date\\\": \\\"2016-10-31T04:08:31+00:00\\\", \\\"rule_url\\\": \\\"https://console.redhat.com/insights/advisor/recommendations/hardening_grub|GRUB_HARDENING_3/\\\", \\\"reboot_required\\\": false, \\\"has_incident\\\": false}\", \"report_url\": \"https://console.redhat.com/insights/advisor/recommendations/hardening_grub|GRUB_HARDENING_3/4c548b3c-b816-4d69-97cf-d8046fc10139\"}, {\"metadata\": {}, \"payload\": \"{\\\"rule_id\\\": \\\"insights_core_egg_not_up2date|INSIGHTS_CORE_EGG_NOT_UP2DATE\\\", \\\"rule_description\\\": \\\"System is not able to get the latest recommendations and may miss bug fixes when the Insights Client Core egg file is outdated\\\", \\\"total_risk\\\": \\\"2\\\", \\\"publish_date\\\": \\\"2021-03-13T18:44:00+00:00\\\", \\\"rule_url\\\": \\\"https://console.redhat.com/insights/advisor/recommendations/insights_core_egg_not_up2date|INSIGHTS_CORE_EGG_NOT_UP2DATE/\\\", \\\"reboot_required\\\": false, \\\"has_incident\\\": false}\", \"report_url\": \"https://console.redhat.com/insights/advisor/recommendations/insights_core_egg_not_up2date|INSIGHTS_CORE_EGG_NOT_UP2DATE/4c548b3c-b816-4d69-97cf-d8046fc10139\"}, {\"metadata\": {}, \"payload\": \"{\\\"rule_id\\\": \\\"wrong_tuned_profile|WRONG_TUNED_PROFILE_USED_FOR_VIRTUAL_GUEST\\\", \\\"rule_description\\\": \\\"Decreased performance occurs when KVM and VMWare guests are not running with recommended tuned service configuration\\\", \\\"total_risk\\\": \\\"2\\\", \\\"publish_date\\\": \\\"2020-10-10T07:27:00+00:00\\\", \\\"rule_url\\\": \\\"https://console.redhat.com/insights/advisor/recommendations/wrong_tuned_profile|WRONG_TUNED_PROFILE_USED_FOR_VIRTUAL_GUEST/\\\", \\\"reboot_required\\\": false, \\\"has_incident\\\": false}\", \"report_url\": \"https://console.redhat.com/insights/advisor/recommendations/wrong_tuned_profile|WRONG_TUNED_PROFILE_USED_FOR_VIRTUAL_GUEST/4c548b3c-b816-4d69-97cf-d8046fc10139\"}]}";
         Action action = Parser.decode(jsonAction);
         assertTrue(action.getEvents().get(0).getAdditionalProperties().containsKey("report_url"));

         assertThrows(ParsingException.class, () -> Parser.encode(action));
         assertThrows(ParsingException.class, () -> Parser.validate(action));

         // Should throw ParsingException but does throw ClassCastException
         // fixed by: https://github.com/networknt/json-schema-validator/pull/561
         assertThrows(Exception.class, () -> Parser.validate(jsonAction));
     }

    @Test
    void shouldAcceptTimestampWith00OffsetWhenDeserializingAndFailIfValidating() {
        String jsonAction = "{\"version\":\"v1.1.0\",\"bundle\":\"rhel\",\"application\":\"compliance\",\"event_type\":\"compliance-below-threshold\",\"timestamp\":\"2022-05-02T17:30:54+00:00\",\"account_id\":\"6089719\",\"events\":[{\"metadata\":{},\"payload\":\"{\\\"host_id\\\":\\\"597f855d-2abc-412c-aabc-8213e0f52655\\\",\\\"host_name\\\":\\\"rhiqe.g1myw1.test\\\",\\\"policy_id\\\":\\\"7c247099-4e1b-42e6-adaa-022e026bd6c3\\\",\\\"policy_name\\\":\\\"Red Hat Corporate Profile for Certified Cloud Providers (RH CCP)\\\",\\\"policy_threshold\\\":100.0,\\\"compliance_score\\\":50.5}\"}],\"context\":\"{\\\"display_name\\\":\\\"rhiqe.g1myw1.test\\\",\\\"host_url\\\":\\\"https://console.redhat.com/insights/inventory/597f855d-2abc-412c-aabc-8213e0f52655\\\",\\\"inventory_id\\\":\\\"597f855d-2abc-412c-aabc-8213e0f52655\\\",\\\"rhel_version\\\":\\\"7.8\\\",\\\"tags\\\":[{\\\"key\\\":\\\"pnzXd\\\",\\\"value\\\":\\\"fjMNEWZq\\\",\\\"namespace\\\":\\\"foRxKl\\\"},{\\\"key\\\":\\\"QTCudz\\\",\\\"value\\\":\\\"PmUkZUgAW\\\",\\\"namespace\\\":\\\"wQAFzbA\\\"},{\\\"key\\\":\\\"KivqqjEz\\\",\\\"value\\\":\\\"sRLKnWkj\\\",\\\"namespace\\\":\\\"AxWVzGyCK\\\"},{\\\"key\\\":\\\"pnYHATPvHo\\\",\\\"value\\\":\\\"nRBuWbU\\\",\\\"namespace\\\":\\\"XnvpVIWHZ\\\"},{\\\"key\\\":\\\"tQfAQQ\\\",\\\"value\\\":\\\"agDLlxVKl\\\",\\\"namespace\\\":\\\"KvHmTtqgzh\\\"}]}\",\"recipients\":[]}";
        Action action = Parser.decode(jsonAction);
        assertEquals(LocalDateTime.of(2022, 5, 2, 17, 30 ,54), action.getTimestamp());

        // Should throw ParsingException but does throw ClassCastException
        // fixed by: https://github.com/networknt/json-schema-validator/pull/561
        assertThrows(Exception.class, () -> Parser.validate(jsonAction));
    }

    @Test
    void shouldFailWithoutRequiredFieldsWhenSerializing() throws JsonProcessingException {
        Action action = getValidAction();
        testParserEncode(action, false);

        // required
        action = getValidAction();
        action.setBundle(null);
        testParserEncode(action, true);

        action = getValidAction();
        action.setApplication(null);
        testParserEncode(action, true);

        action = getValidAction();
        action.setEventType(null);
        testParserEncode(action, true);

        action = getValidAction();
        action.setTimestamp(null);
        testParserEncode(action, true);

        action = getValidAction();
        action.setAccountId(null);
        testParserEncode(action, true);

        action = getValidAction();
        action.setEvents(null);
        testParserEncode(action, true);

        action = getValidAction();
        action.getEvents().get(0).setPayload(null);
        testParserEncode(action, true);

        // optional
        action = getValidAction();
        action.setOrgId(null);
        testParserEncode(action, false);

        action = getValidAction();
        action.setContext(null);
        testParserEncode(action, false);

        action = getValidAction();
        action.getEvents().get(0).setMetadata(null);
        testParserEncode(action, false);

        action = getValidAction();
        action.setRecipients(null);
        testParserEncode(action, false);
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

        testDate(
                "2011-12-03T10:15:30+00:00",
                LocalDateTime.of(
                        2011,
                        12,
                        3,
                        10,
                        15,
                        30
                )
        );
    }

    @Test
    void shouldHaveAccountIdAndOrgIdIsOptional() {
        Action action = getValidAction();

        action.setAccountId(null);
        action.setOrgId(null);
        assertThrows(ParsingException.class, () -> Parser.validate(action));

        action.setAccountId("foo");
        action.setOrgId(null);
        assertDoesNotThrow(() -> Parser.validate(action));

        action.setAccountId(null);
        action.setOrgId("foo");
        // Once we support org_id, this wont fail anymore.
        assertThrows(ParsingException.class, () -> Parser.validate(action));

        action.setAccountId("foo");
        action.setOrgId("foo");
        assertDoesNotThrow(() -> Parser.validate(action));
    }

    private Action getValidAction() {
        return new Action.ActionBuilder()
                .withAccountId("account-id")
                .withOrgId("my-org-id")
                .withBundle("my-bundle")
                .withApplication("my-app")
                .withEventType("my-event-type")
                .withTimestamp(LocalDateTime.now())
                .withContext(
                        new Context.ContextBuilder().withAdditionalProperty("foo", "bar").build()
                )
                .withRecipients(
                        List.of(
                                new Recipient.RecipientBuilder().build()
                        )
                )
                .withEvents(
                        List.of(
                                new Event.EventBuilder()
                                        .withMetadata(new Metadata.MetadataBuilder().build())
                                        .withPayload(new Payload.PayloadBuilder()
                                                .withAdditionalProperty("foo", "bar")
                                                .build())
                                        .build()
                        )
                )
                .build();
    }

    private void testParserEncode(Action action, boolean fails) {
        Executable encoding = () -> Parser.encode(action);
        if (fails) {
            assertThrows(ParsingException.class, encoding);
        } else {
            assertDoesNotThrow(encoding);
        }
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

    /**
     * This method removes a field from a valid template and checks one of these two cases:
     - if {@code isRequired} is true, the field removal should trigger an exception throw when the template is decoded
     - otherwise, the field removal should not cause any exception during the decoding as the field is expected to be optional
     */
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
