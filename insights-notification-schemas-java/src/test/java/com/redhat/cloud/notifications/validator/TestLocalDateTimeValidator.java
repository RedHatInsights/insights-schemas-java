package com.redhat.cloud.notifications.validator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLocalDateTimeValidator {

    @Test
    void shouldMatchIsoDates() {
        LocalDateTimeValidator validator = new LocalDateTimeValidator();

        assertTrue(validator.matches("2020-07-14T13:22:10.133"));
        assertTrue(validator.matches("2020-07-14T13:22:10"));

        // Offsets
        assertTrue(validator.matches("2011-12-03T10:15:30+01:00[Europe/Paris]"));
        assertFalse(validator.matches("2011-12-03T10:15:30[Europe/Paris]"));
        assertTrue(validator.matches("2011-12-03T10:15:30+01:00"));

        // Week format
        assertFalse(validator.matches("2007-W44-6T16:18:05Z"));

        // Text
        assertFalse(validator.matches("Tomorrow"));
        assertFalse(validator.matches("As soon as possible!!"));

        // Only date or time
        assertFalse(validator.matches("2020-07-14"));
        assertFalse(validator.matches("22:10:10"));

        assertTrue(validator.matches("2020-07-14T13:22:10Z"));
        assertTrue(validator.matches("2011-12-03T10:15:30+00:00"));
    }

    @Test
    void shouldExposeMessageKeyOnlyAfterFailedMatch() {
        LocalDateTimeValidator validator = new LocalDateTimeValidator();

        assertFalse(validator.matches("Tomorrow"));
        assertTrue(validator.getMessageKey().contains("Tomorrow"));

        // getMessageKey() clears the captured message after reading it
        assertNull(validator.getMessageKey());

        assertTrue(validator.matches("2020-07-14T13:22:10Z"));
        assertNull(validator.getMessageKey());
    }

    @Test
    void shouldNotLeakMessagesAcrossThreads() throws InterruptedException {
        LocalDateTimeValidator validator = new LocalDateTimeValidator();
        int threadCount = 20;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<String> failures = new CopyOnWriteArrayList<>();

        List<Thread> threads = IntStream.range(0, threadCount)
            .mapToObj(i -> new Thread(() -> {
                String input = "not-a-date-" + i;
                try {
                    ready.countDown();
                    start.await();

                    boolean matched = validator.matches(input);
                    String messageKey = validator.getMessageKey();

                    if (matched || messageKey == null || !messageKey.contains(input)) {
                        failures.add("Thread " + i + " observed unexpected messageKey: " + messageKey);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }))
            .collect(Collectors.toList());

        threads.forEach(Thread::start);
        ready.await();
        start.countDown();
        done.await();

        assertEquals(List.of(), failures);
    }

}
