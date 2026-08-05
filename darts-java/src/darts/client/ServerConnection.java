package darts.client;

import darts.common.Message;
import darts.common.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Handles underlying TCP socket connection, stream length-prefix framing,
 * and background network reading thread for the client.
 */
public class ServerConnection {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Consumer<Message> messageHandler;
    private Consumer<String> errorHandler;
    private volatile boolean connected = false;
    private Thread readerThread;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Establishes TCP connection to the DARTS server and starts background reader thread.
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        connected = true;

        readerThread = new Thread(this::readLoop, "DARTS-NetworkReader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Sends a length-prefixed Message to the server.
     */
    public synchronized void send(Message message) {
        if (!connected || socket == null || socket.isClosed()) {
            if (errorHandler != null) {
                errorHandler.accept("Cannot send message: Not connected to server.");
            }
            return;
        }
        try {
            byte[] frame = Protocol.encodeFrame(message);
            out.write(frame);
            out.flush();
        } catch (IOException e) {
            disconnect();
            if (errorHandler != null) {
                errorHandler.accept("Network write failed: " + e.getMessage());
            }
        }
    }

    private void readLoop() {
        while (connected && !Thread.currentThread().isInterrupted()) {
            try {
                int length = in.readInt();
                if (length <= 0 || length > Protocol.MAX_PAYLOAD_SIZE) {
                    if (errorHandler != null) {
                        errorHandler.accept("Received invalid frame length: " + length);
                    }
                    disconnect();
                    break;
                }
                byte[] payload = new byte[length];
                in.readFully(payload);
                String json = new String(payload, StandardCharsets.UTF_8);
                Message msg = Message.fromJson(json);

                if (messageHandler != null) {
                    messageHandler.accept(msg);
                }
            } catch (IOException e) {
                if (connected) {
                    disconnect();
                    if (errorHandler != null) {
                        errorHandler.accept("Disconnected from server.");
                    }
                }
                break;
            } catch (Exception e) {
                if (errorHandler != null) {
                    errorHandler.accept("Error parsing server frame: " + e.getMessage());
                }
            }
        }
    }

    public void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
