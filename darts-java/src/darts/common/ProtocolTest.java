package darts.common;

import java.nio.ByteBuffer;

/**
 * Lightweight test suite for Message JSON serialization and Protocol length-prefix framing.
 */
public class ProtocolTest {
    public static void main(String[] args) {
        System.out.println("Running Protocol & Message unit tests...");

        testMessageJsonRoundtrip();
        testMessageWithNulls();
        testSpecialCharacters();
        testProtocolFraming();
        testMessageValidation();

        System.out.println("[SUCCESS] All Protocol & Message tests passed!");
    }

    private static void testMessageJsonRoundtrip() {
        Message original = new Message(Protocol.MSG_ROOM_MSG, "alice", null, "general", "Hello world!", 1737590400000L);
        String json = original.toJson();
        Message parsed = Message.fromJson(json);

        assertEq("type", original.getType(), parsed.getType());
        assertEq("from", original.getFrom(), parsed.getFrom());
        assertEq("to", original.getTo(), parsed.getTo());
        assertEq("room", original.getRoom(), parsed.getRoom());
        assertEq("body", original.getBody(), parsed.getBody());
        if (original.getTimestamp() != parsed.getTimestamp()) {
            throw new RuntimeException("Timestamp mismatch: " + original.getTimestamp() + " vs " + parsed.getTimestamp());
        }
    }

    private static void testMessageWithNulls() {
        Message msg = new Message(Protocol.MSG_LOGIN, null, null, null, "pass123");
        String json = msg.toJson();
        Message parsed = Message.fromJson(json);
        assertEq("type", Protocol.MSG_LOGIN, parsed.getType());
        if (parsed.getFrom() != null) throw new RuntimeException("Expected null 'from'");
        if (parsed.getTo() != null) throw new RuntimeException("Expected null 'to'");
        if (parsed.getRoom() != null) throw new RuntimeException("Expected null 'room'");
        assertEq("body", "pass123", parsed.getBody());
    }

    private static void testSpecialCharacters() {
        String complexBody = "Line 1\nLine 2 with \"quotes\" and \\slash and \t tab.";
        Message msg = new Message(Protocol.MSG_ROOM_MSG, "bob", "alice", "dev", complexBody);
        String json = msg.toJson();
        Message parsed = Message.fromJson(json);
        assertEq("body", complexBody, parsed.getBody());
    }

    private static void testProtocolFraming() {
        Message msg = new Message(Protocol.MSG_PING, "client1", null, null, "");
        byte[] frame = Protocol.encodeFrame(msg);
        ByteBuffer buf = ByteBuffer.wrap(frame);
        int len = buf.getInt();
        byte[] payload = new byte[len];
        buf.get(payload);
        String json = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        Message decoded = Message.fromJson(json);
        assertEq("type", Protocol.MSG_PING, decoded.getType());
    }

    private static void testMessageValidation() {
        Message valid = new Message(Protocol.MSG_ROOM_MSG, "alice", null, "general", "hi");
        if (Protocol.validateMessage(valid) != null) {
            throw new RuntimeException("Valid message reported error");
        }

        String longUsername = "a".repeat(35);
        Message invalidUser = new Message(Protocol.MSG_ROOM_MSG, longUsername, null, "general", "hi");
        if (Protocol.validateMessage(invalidUser) == null) {
            throw new RuntimeException("Expected validation error for username > 32 chars");
        }
    }

    private static void assertEq(String field, String expected, String actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new RuntimeException("Field mismatch for '" + field + "': expected [" + expected + "], got [" + actual + "]");
    }
}
