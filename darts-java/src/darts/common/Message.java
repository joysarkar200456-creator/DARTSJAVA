package darts.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable message representation according to the wire protocol envelope specification.
 * Contains type, sender, recipient, room, body content, and timestamp.
 */
public final class Message {
    private final String type;
    private final String from;
    private final String to;
    private final String room;
    private final String body;
    private final long timestamp;

    public Message(String type, String from, String to, String room, String body, long timestamp) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.from = from;
        this.to = to;
        this.room = room;
        this.body = body != null ? body : "";
        this.timestamp = timestamp;
    }

    public Message(String type, String from, String to, String room, String body) {
        this(type, from, to, room, body, System.currentTimeMillis());
    }

    public String getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getRoom() {
        return room;
    }

    public String getBody() {
        return body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Serializes this Message into a UTF-8 JSON envelope string.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":").append(escapeJsonString(type)).append(",");
        sb.append("\"from\":").append(escapeJsonString(from)).append(",");
        sb.append("\"to\":").append(escapeJsonString(to)).append(",");
        sb.append("\"room\":").append(escapeJsonString(room)).append(",");
        sb.append("\"body\":").append(escapeJsonString(body)).append(",");
        sb.append("\"timestamp\":").append(timestamp);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parses a UTF-8 JSON string envelope into a Message instance.
     *
     * @param json the raw JSON envelope string
     * @return parsed Message instance
     * @throws IllegalArgumentException if JSON is malformed or missing type
     */
    public static Message fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be empty");
        }
        Map<String, String> fields = parseJsonFields(json.trim());
        String type = fields.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Missing required 'type' field in JSON envelope");
        }
        String from = fields.get("from");
        String to = fields.get("to");
        String room = fields.get("room");
        String body = fields.get("body");
        String tsStr = fields.get("timestamp");
        long timestamp = System.currentTimeMillis();
        if (tsStr != null) {
            try {
                timestamp = Long.parseLong(tsStr);
            } catch (NumberFormatException ignored) {
                // Fallback to system timestamp if unparseable
            }
        }
        return new Message(type, from, to, room, body, timestamp);
    }

    private static String escapeJsonString(String str) {
        if (str == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
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
        sb.append("\"");
        return sb.toString();
    }

    private static Map<String, String> parseJsonFields(String json) {
        Map<String, String> map = new HashMap<>();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON envelope boundary");
        }
        String content = json.substring(1, json.length() - 1).trim();
        int i = 0;
        int len = content.length();

        while (i < len) {
            while (i < len && (Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ',')) {
                i++;
            }
            if (i >= len) {
                break;
            }
            if (content.charAt(i) != '"') {
                i++;
                continue;
            }
            int keyStart = i + 1;
            int keyEnd = findStringEnd(content, keyStart);
            if (keyEnd == -1) {
                break;
            }
            String key = unescapeJsonString(content.substring(keyStart, keyEnd));
            i = keyEnd + 1;

            while (i < len && content.charAt(i) != ':') {
                i++;
            }
            if (i >= len) {
                break;
            }
            i++;

            while (i < len && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i >= len) {
                break;
            }

            if (content.charAt(i) == '"') {
                int valStart = i + 1;
                int valEnd = findStringEnd(content, valStart);
                if (valEnd == -1) {
                    break;
                }
                String val = unescapeJsonString(content.substring(valStart, valEnd));
                map.put(key, val);
                i = valEnd + 1;
            } else if (content.startsWith("null", i)) {
                map.put(key, null);
                i += 4;
            } else {
                int valStart = i;
                while (i < len && content.charAt(i) != ',' && content.charAt(i) != '}') {
                    i++;
                }
                String val = content.substring(valStart, i).trim();
                map.put(key, val);
            }
        }
        return map;
    }

    private static int findStringEnd(String s, int start) {
        boolean escaped = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescapeJsonString(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = str.length();
        while (i < len) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    case '/' -> { sb.append('/'); i += 2; }
                    case 'b' -> { sb.append('\b'); i += 2; }
                    case 'f' -> { sb.append('\f'); i += 2; }
                    case 'n' -> { sb.append('\n'); i += 2; }
                    case 'r' -> { sb.append('\r'); i += 2; }
                    case 't' -> { sb.append('\t'); i += 2; }
                    case 'u' -> {
                        if (i + 5 < len) {
                            String hex = str.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 6;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                                i++;
                            }
                        } else {
                            sb.append(c);
                            i++;
                        }
                    }
                    default -> {
                        sb.append(next);
                        i += 2;
                    }
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return timestamp == message.timestamp &&
                Objects.equals(type, message.type) &&
                Objects.equals(from, message.from) &&
                Objects.equals(to, message.to) &&
                Objects.equals(room, message.room) &&
                Objects.equals(body, message.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, from, to, room, body, timestamp);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", room='" + room + '\'' +
                ", body='" + body + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
