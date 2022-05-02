package com.redhat.cloud.notifications.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.redhat.cloud.notifications.jackson.deserializer.LocalDateTimeDeserializer;
import com.redhat.cloud.notifications.jackson.serializer.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public class LocalDateTimeModule extends SimpleModule {

    public LocalDateTimeModule(boolean relaxed) {
        addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(relaxed));
        addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    }

}
