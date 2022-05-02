package com.redhat.cloud.notifications.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.redhat.cloud.notifications.jackson.deserializer.LocalDateTimeDeserializer;
import com.redhat.cloud.notifications.jackson.serializer.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public class LocalDateTimeModule extends SimpleModule {

    public LocalDateTimeModule() {
        addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    }

}
