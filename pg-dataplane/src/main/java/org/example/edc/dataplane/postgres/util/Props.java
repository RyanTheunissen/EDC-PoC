package org.example.edc.dataplane.postgres.util;


import java.util.Map;


public final class Props {
    private Props() {}


    @SuppressWarnings("unchecked")
    public static <T> T first(Map<String, Object> m, T defaultValue, String... keys) {
        for (var k : keys) {
            if (m.containsKey(k)) {
                try { return (T) m.get(k); } catch (ClassCastException ignored) { }
            }
        }
        return defaultValue;
    }
}