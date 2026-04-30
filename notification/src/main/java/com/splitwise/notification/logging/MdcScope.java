package com.splitwise.notification.logging;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

public final class MdcScope implements AutoCloseable {
    private final Map<String, String> previousValues = new HashMap<>();

    private MdcScope(Map<String, String> values) {
        values.forEach((key, value) -> {
            previousValues.put(key, MDC.get(key));
            if (value == null || value.isBlank()) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }

    public static MdcScope with(Map<String, String> values) {
        return new MdcScope(values);
    }

    @Override
    public void close() {
        previousValues.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }
}
