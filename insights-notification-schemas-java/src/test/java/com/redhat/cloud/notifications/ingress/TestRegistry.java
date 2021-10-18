package com.redhat.cloud.notifications.ingress;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRegistry {

    @Test
    public void loadsVersions() {
        Registry registry = new Registry();
        Assertions.assertNotNull(registry.getSchema("v1.0.0"));
        Assertions.assertNotNull(registry.getSchema("v1.1.0"));
    }

    @Test
    public void throwsOnUnknownVersion() {
        Registry registry = new Registry();
        Assertions.assertThrows(RuntimeException.class, () -> registry.getSchema("foobar"));
    }

}
