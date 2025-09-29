package com.ejada.testsupport.extensions;

import java.util.HashMap;
import java.util.Map;

final class SystemPropertyExtensionSupport {

    private SystemPropertyExtensionSupport() {
    }

    static Map<String, String> apply(Map<String, String> newValues) {
        Map<String, String> previousValues = new HashMap<>();
        newValues.forEach((key, value) -> {
            previousValues.put(key, System.getProperty(key));
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
        return previousValues;
    }

    static void restore(Map<String, String> previousValues) {
        previousValues.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }
}
