package com.aegis.showcase.util;

import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Json {
    private Json() {}

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    public static String pretty(Object value) {
        String compact = write(value);
        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (quoted) {
                out.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
                continue;
            }
            if (c == '"') {
                quoted = true;
                out.append(c);
            } else if (c == '{' || c == '[') {
                out.append(c).append('\n');
                indent++;
                indent(out, indent);
            } else if (c == '}' || c == ']') {
                out.append('\n');
                indent--;
                indent(out, indent);
                out.append(c);
            } else if (c == ',') {
                out.append(c).append('\n');
                indent(out, indent);
            } else if (c == ':') {
                out.append(": ");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String || value instanceof Character || value instanceof Enum<?> || value instanceof TemporalAccessor) {
            quote(out, value.toString());
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Number number) {
            if (number instanceof Double d && !Double.isFinite(d)) throw new IllegalArgumentException("Non-finite JSON number.");
            if (number instanceof Float f && !Float.isFinite(f)) throw new IllegalArgumentException("Non-finite JSON number.");
            out.append(number);
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) sorted.put(String.valueOf(entry.getKey()), entry.getValue());
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) out.append(',');
                first = false;
                quote(out, entry.getKey());
                out.append(':');
                append(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Collection<?> collection) {
            out.append('[');
            boolean first = true;
            for (Object item : collection) {
                if (!first) out.append(',');
                first = false;
                append(out, item);
            }
            out.append(']');
        } else if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            out.append('[');
            for (int i = 0; i < length; i++) {
                if (i > 0) out.append(',');
                append(out, java.lang.reflect.Array.get(value, i));
            }
            out.append(']');
        } else if (value.getClass().isRecord()) {
            Map<String, Object> record = new TreeMap<>();
            for (RecordComponent component : value.getClass().getRecordComponents()) {
                try {
                    record.put(component.getName(), component.getAccessor().invoke(value));
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Unable to serialize record component " + component.getName(), ex);
                }
            }
            append(out, record);
        } else {
            throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass().getName());
        }
    }

    private static void quote(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private static void indent(StringBuilder out, int count) {
        out.append("  ".repeat(Math.max(0, count)));
    }

    public static Map<String, Object> map(Object... keyValues) {
        if (keyValues.length % 2 != 0) throw new IllegalArgumentException("Key/value pairs required.");
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        return map;
    }
}
