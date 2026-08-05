package darts.server;

import darts.common.Message;
import darts.common.Protocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Manages per-connected-client state, wire protocol framing, non-blocking socket I/O,
 * and outgoing write queue for a single channel in the Selector event loop.
 */
public class ClientSession {
    private final SocketChannel channel;
    private final SelectionKey key;
    private final Server server;

    private String username;
    private String currentRoom;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
    private ByteBuffer payloadBuffer = null;
    private boolean readingHeader = true;
    private int payloadLength = 0;

    private final Queue<ByteBuffer> writeQueue = new ArrayDeque<>();
    private boolean closed = false;

    public ClientSession(SocketChannel channel, SelectionKey key, Server server) {
        this.channel = channel;
        this.key = key;
        this.server = server;
        this.currentRoom = "general";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    /**
     * Reads pending bytes from the socket channel and processes accumulated length-prefixed frames.
     */
    public void handleRead() {
        if (closed) return;
        try {
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) {
                close();
                return;
            }
            readBuffer.flip();
            while (readBuffer.hasRemaining()) {
                if (readingHeader) {
                    while (readBuffer.hasRemaining() && lengthBuffer.hasRemaining()) {
                        lengthBuffer.put(readBuffer.get());
                    }
                    if (!lengthBuffer.hasRemaining()) {
                        lengthBuffer.flip();
                        payloadLength = lengthBuffer.getInt();
                        lengthBuffer.clear();

                        if (payloadLength <= 0 || payloadLength > Protocol.MAX_PAYLOAD_SIZE) {
                            sendError("Frame length " + payloadLength + " exceeds maximum bound of " + Protocol.MAX_PAYLOAD_SIZE);
                            close();
                            return;
                        }
                        payloadBuffer = ByteBuffer.allocate(payloadLength);
                        readingHeader = false;
                    }
                }
                if (!readingHeader) {
                    while (readBuffer.hasRemaining() && payloadBuffer.hasRemaining()) {
                        payloadBuffer.put(readBuffer.get());
                    }
                    if (!payloadBuffer.hasRemaining()) {
                        payloadBuffer.flip();
                        byte[] payloadBytes = new byte[payloadLength];
                        payloadBuffer.get(payloadBytes);
                        String json = new String(payloadBytes, StandardCharsets.UTF_8);

                        readingHeader = true;
                        payloadBuffer = null;

                        processFrame(json);
                    }
                }
            }
            readBuffer.clear();
        } catch (IOException e) {
            close();
        } catch (Exception e) {
            System.err.println("Error processing client input for " + getClientAddress() + ": " + e.getMessage());
            sendError("Internal error processing frame: " + e.getMessage());
        }
    }

    /**
     * Non-blocking write handler that drains the outgoing buffer queue to the socket.
     */
    public void handleWrite() {
        if (closed) return;
        try {
            while (!writeQueue.isEmpty()) {
                ByteBuffer buf = writeQueue.peek();
                channel.write(buf);
                if (buf.hasRemaining()) {
                    // Socket send buffer full; remain registered for OP_WRITE
                    return;
                }
                writeQueue.poll();
            }
            // All pending writes completed; unregister OP_WRITE interest
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        } catch (IOException e) {
            close();
        }
    }

    /**
     * Enqueues a Message to be sent to this client in non-blocking mode.
     */
    public void send(Message message) {
        if (closed) return;
        try {
            ByteBuffer buf = Protocol.encodeFrameBuffer(message);
            writeQueue.add(buf);
            if (key.isValid()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                key.selector().wakeup();
            }
        } catch (Exception e) {
            System.err.println("Error encoding message for client: " + e.getMessage());
        }
    }

    /**
     * Helper to send an error message to the client.
     */
    public void sendError(String errorMsg) {
        send(new Message(Protocol.MSG_ERROR, "server", username, currentRoom, errorMsg));
    }

    /**
     * Processes a fully-framed JSON message payload from the client.
     */
    private void processFrame(String json) {
        Message msg;
        try {
            msg = Message.fromJson(json);
        } catch (Exception e) {
            sendError("Malformed JSON message: " + e.getMessage());
            return;
        }

        String validationError = Protocol.validateMessage(msg);
        if (validationError != null) {
            sendError("Validation error: " + validationError);
            return;
        }

        String type = msg.getType();

        if (Protocol.MSG_LOGIN.equals(type)) {
            // For Phase 1, unauthenticated broadcast chat uses the 'from' field or body as username
            String chosenName = (msg.getFrom() != null && !msg.getFrom().isBlank()) ? msg.getFrom() : "User_" + channel.socket().getPort();
            this.username = chosenName;
            send(new Message(Protocol.MSG_LOGIN_OK, "server", username, currentRoom, "Login successful"));
            server.getRoom(currentRoom).join(this);

            // Announce presence
            server.getRoom(currentRoom).broadcast(
                new Message(Protocol.MSG_PRESENCE, "server", null, currentRoom, username + " joined the room"), this
            );
            return;
        }

        if (username == null) {
            // Default guest username if message sent before MSG_LOGIN
            this.username = (msg.getFrom() != null && !msg.getFrom().isBlank()) ? msg.getFrom() : "User_" + channel.socket().getPort();
            server.getRoom(currentRoom).join(this);
        }

        switch (type) {
            case Protocol.MSG_ROOM_MSG -> {
                String targetRoom = (msg.getRoom() != null && !msg.getRoom().isBlank()) ? msg.getRoom() : currentRoom;
                Room room = server.getRoom(targetRoom);
                room.join(this);
                Message broadcastMsg = new Message(Protocol.MSG_ROOM_MSG, username, null, targetRoom, msg.getBody(), System.currentTimeMillis());
                room.broadcast(broadcastMsg);
            }
            case Protocol.MSG_PING -> {
                send(new Message(Protocol.MSG_PONG, "server", username, currentRoom, "pong"));
            }
            case Protocol.MSG_QUIT -> {
                close();
            }
            default -> {
                // Fallback broadcast or handle unhandled message type
                Room room = server.getRoom(currentRoom);
                Message broadcastMsg = new Message(type, username, msg.getTo(), currentRoom, msg.getBody(), System.currentTimeMillis());
                room.broadcast(broadcastMsg);
            }
        }
    }

    /**
     * Closes the client session and removes it from server room membership.
     */
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (currentRoom != null && server != null) {
                Room room = server.getRoom(currentRoom);
                room.leave(this);
                if (username != null) {
                    room.broadcast(new Message(Protocol.MSG_PRESENCE, "server", null, currentRoom, username + " left the room"));
                }
            }
            key.cancel();
            channel.close();
            System.out.println("Closed connection for " + (username != null ? username : getClientAddress()));
        } catch (IOException ignored) {
            // Ignored on socket close
        }
    }

    private String getClientAddress() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
