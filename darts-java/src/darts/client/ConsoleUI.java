package darts.client;

import darts.common.Message;
import darts.common.Protocol;
import java.util.Scanner;

/**
 * Handles terminal rendering, user input parsing, and incoming message formatting for the client.
 */
public class ConsoleUI {
    private final ServerConnection connection;
    private String username;
    private String currentRoom = "general";
    private volatile boolean running = true;

    public ConsoleUI(ServerConnection connection) {
        this.connection = connection;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Starts the interactive command line input loop.
     */
    public void startInputLoop() {
        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("/")) {
                handleCommand(line);
            } else {
                // Default input: send room broadcast message
                if (username == null) {
                    System.out.println("[SYSTEM] Please set a username using '/login <username>' first.");
                    continue;
                }
                Message msg = new Message(Protocol.MSG_ROOM_MSG, username, null, currentRoom, line);
                connection.send(msg);
            }
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/login", "/name" -> {
                if (arg.isEmpty()) {
                    System.out.println("[SYSTEM] Usage: /login <username>");
                    return;
                }
                this.username = arg;
                Message loginMsg = new Message(Protocol.MSG_LOGIN, username, null, currentRoom, "");
                connection.send(loginMsg);
            }
            case "/all" -> {
                if (arg.isEmpty()) {
                    System.out.println("[SYSTEM] Usage: /all <message>");
                    return;
                }
                if (username == null) {
                    System.out.println("[SYSTEM] Please set a username using '/login <username>' first.");
                    return;
                }
                Message msg = new Message(Protocol.MSG_ROOM_MSG, username, null, currentRoom, arg);
                connection.send(msg);
            }
            case "/ping" -> {
                Message ping = new Message(Protocol.MSG_PING, username != null ? username : "guest", null, currentRoom, "");
                connection.send(ping);
            }
            case "/help" -> printHelp();
            case "/quit", "/exit" -> {
                running = false;
                if (username != null) {
                    connection.send(new Message(Protocol.MSG_QUIT, username, null, currentRoom, ""));
                }
                connection.disconnect();
                System.out.println("Goodbye!");
                System.exit(0);
            }
            default -> System.out.println("[SYSTEM] Unknown command: " + cmd + ". Type /help for available commands.");
        }
    }

    /**
     * Callback method for formatting and displaying incoming server messages.
     */
    public void onMessageReceived(Message msg) {
        switch (msg.getType()) {
            case Protocol.MSG_ROOM_MSG -> {
                System.out.println("[" + (msg.getRoom() != null ? msg.getRoom() : "general") + "] <" +
                        (msg.getFrom() != null ? msg.getFrom() : "unknown") + ">: " + msg.getBody());
            }
            case Protocol.MSG_PRESENCE -> {
                System.out.println("*** [PRESENCE] " + msg.getBody() + " ***");
            }
            case Protocol.MSG_LOGIN_OK -> {
                System.out.println("[SERVER] Login OK: " + msg.getBody());
            }
            case Protocol.MSG_LOGIN_FAIL -> {
                System.out.println("[SERVER ERROR] Login Failed: " + msg.getBody());
            }
            case Protocol.MSG_PONG -> {
                System.out.println("[SERVER] Pong!");
            }
            case Protocol.MSG_ERROR -> {
                System.out.println("[SERVER ERROR]: " + msg.getBody());
            }
            default -> {
                System.out.println("[" + msg.getType() + "] " +
                        (msg.getFrom() != null ? "<" + msg.getFrom() + "> " : "") + msg.getBody());
            }
        }
    }

    public void onError(String errorMessage) {
        System.out.println("[NETWORK ERROR] " + errorMessage);
    }

    private void printHelp() {
        System.out.println("==================================================");
        System.out.println("            DARTS-Java Terminal Client            ");
        System.out.println("==================================================");
        System.out.println(" Commands:");
        System.out.println("   /login <username>   - Set your username and join");
        System.out.println("   /all <message>      - Broadcast message to current room");
        System.out.println("   /ping               - Send heartbeat ping");
        System.out.println("   /help               - Show this help menu");
        System.out.println("   /quit               - Exit application");
        System.out.println("==================================================");
    }
}
