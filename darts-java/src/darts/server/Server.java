package darts.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main server entry point implementing a single-threaded non-blocking Selector event loop.
 * Listens for incoming client connections, routes I/O events, and manages room registries.
 */
public class Server {
    public static final int DEFAULT_PORT = 8888;

    private final int port;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = false;

    public Server(int port) {
        this.port = port;
        rooms.put("general", new Room("general"));
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number specified, defaulting to " + DEFAULT_PORT);
            }
        }

        Server server = new Server(port);
        server.start();
    }

    /**
     * Initializes the ServerSocketChannel, binds to the specified port, and enters the Selector event loop.
     */
    public void start() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            running = true;
            System.out.println("DARTS Server started on port " + port + ". Event loop running...");

            while (running) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        } else if (key.isReadable()) {
                            ClientSession session = (ClientSession) key.attachment();
                            if (session != null) {
                                session.handleRead();
                            }
                        } else if (key.isWritable()) {
                            ClientSession session = (ClientSession) key.attachment();
                            if (session != null) {
                                session.handleWrite();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Exception handling selection key: " + e.getMessage());
                        ClientSession session = (ClientSession) key.attachment();
                        if (session != null) {
                            session.close();
                        } else {
                            key.cancel();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverSocketChannel.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            ClientSession session = new ClientSession(clientChannel, clientKey, this);
            clientKey.attach(session);
            System.out.println("Accepted new connection from " + clientChannel.getRemoteAddress());
        }
    }

    public Room getRoom(String name) {
        return rooms.computeIfAbsent(name, Room::new);
    }

    public void stop() {
        running = false;
        if (selector != null && selector.isOpen()) {
            try {
                for (SelectionKey key : selector.keys()) {
                    if (key.attachment() instanceof ClientSession session) {
                        session.close();
                    }
                }
                selector.close();
                if (serverChannel != null && serverChannel.isOpen()) {
                    serverChannel.close();
                }
                System.out.println("DARTS Server stopped successfully.");
            } catch (IOException e) {
                System.err.println("Error closing server: " + e.getMessage());
            }
        }
    }
}
