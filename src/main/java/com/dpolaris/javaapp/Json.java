package com.dpolaris.javaapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {
    }

    static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected trailing JSON content at index " + parser.index);
        }
        return value;
    }

    static String pretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writePretty(value, sb, 0);
        return sb.toString();
    }

    static String prettyFromString(String jsonText) {
        try {
            return pretty(parse(jsonText));
        } catch (RuntimeException ex) {
            return jsonText;
        }
    }

    static String compact(Object value) {
        StringBuilder sb = new StringBuilder();
        writeCompact(value, sb);
        return sb.toString();
    }

    static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected object but found: " + value);
    }

    @SuppressWarnings("unchecked")
    static List<Object> asArray(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new IllegalArgumentException("Expected array but found: " + value);
    }

    static String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static void writePretty(Object value, StringBuilder sb, int depth) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String text) {
            sb.append('"').append(escape(text)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append("{");
            if (!map.isEmpty()) {
                sb.append("\n");
                int idx = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    indent(sb, depth + 1);
                    sb.append('"').append(escape(String.valueOf(entry.getKey()))).append("\": ");
                    writePretty(entry.getValue(), sb, depth + 1);
                    if (idx < map.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                    idx++;
                }
                indent(sb, depth);
            }
            sb.append("}");
            return;
        }
        if (value instanceof List<?> list) {
            sb.append("[");
            if (!list.isEmpty()) {
                sb.append("\n");
                for (int i = 0; i < list.size(); i++) {
                    indent(sb, depth + 1);
                    writePretty(list.get(i), sb, depth + 1);
                    if (i < list.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                indent(sb, depth);
            }
            sb.append("]");
            return;
        }
        sb.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static void writeCompact(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String text) {
            sb.append('"').append(escape(text)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append("{");
            int idx = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (idx++ > 0) {
                    sb.append(",");
                }
                sb.append('"').append(escape(String.valueOf(entry.getKey()))).append("\":");
                writeCompact(entry.getValue(), sb);
            }
            sb.append("}");
            return;
        }
        if (value instanceof List<?> list) {
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                writeCompact(list.get(i), sb);
            }
            sb.append("]");
            return;
        }
        sb.append('"').append(escape(String.valueOf(value))).append('"');
    }

    private static void indent(StringBuilder sb, int depth) {
        sb.append("  ".repeat(Math.max(0, depth)));
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
            this.index = 0;
        }

        private boolean isAtEnd() {
            return index >= text.length();
        }

        private void skipWhitespace() {
            while (!isAtEnd()) {
                char c = text.charAt(index);
                if (!Character.isWhitespace(c)) {
                    break;
                }
                index++;
            }
        }

        private char peek() {
            if (isAtEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON input");
            }
            return text.charAt(index);
        }

        private char next() {
            char c = peek();
            index++;
            return c;
        }

        private Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseTrue();
                case 'f' -> parseFalse();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            next(); // {
            skipWhitespace();
            if (peek() == '}') {
                next();
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (next() != ':') {
                    throw new IllegalArgumentException("Expected ':' after object key at index " + index);
                }
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                char separator = next();
                if (separator == '}') {
                    break;
                }
                if (separator != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at index " + index);
                }
            }
            return object;
        }

        private List<Object> parseArray() {
            ArrayList<Object> array = new ArrayList<>();
            next(); // [
            skipWhitespace();
            if (peek() == ']') {
                next();
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();
                char separator = next();
                if (separator == ']') {
                    break;
                }
                if (separator != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at index " + index);
                }
            }
            return array;
        }

        private String parseString() {
            if (next() != '"') {
                throw new IllegalArgumentException("Expected string at index " + index);
            }
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (isAtEnd()) {
                    throw new IllegalArgumentException("Unterminated string literal");
                }
                char c = next();
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    if (isAtEnd()) {
                        throw new IllegalArgumentException("Unterminated escape sequence");
                    }
                    char esc = next();
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > text.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape sequence");
                            }
                            String hex = text.substring(index, index + 4);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException ex) {
                                throw new IllegalArgumentException("Invalid unicode escape sequence: " + hex);
                            }
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid escape sequence: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseTrue() {
            expectLiteral("true");
            return Boolean.TRUE;
        }

        private Boolean parseFalse() {
            expectLiteral("false");
            return Boolean.FALSE;
        }

        private Object parseNull() {
            expectLiteral("null");
            return null;
        }

        private Number parseNumber() {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            while (!isAtEnd() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (!isAtEnd() && text.charAt(index) == '.') {
                index++;
                while (!isAtEnd() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (!isAtEnd() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                index++;
                if (!isAtEnd() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                    index++;
                }
                while (!isAtEnd() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            String raw = text.substring(start, index);
            try {
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                    return Double.parseDouble(raw);
                }
                return Long.parseLong(raw);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + raw);
            }
        }

        private void expectLiteral(String expected) {
            if (index + expected.length() > text.length()) {
                throw new IllegalArgumentException("Expected literal " + expected + " at index " + index);
            }
            String actual = text.substring(index, index + expected.length());
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException("Expected literal " + expected + " at index " + index);
            }
            index += expected.length();
        }
    }
}
