package darts.server;

import darts.common.Message;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages room membership and non-blocking message broadcasting for connected clients.
 */
public class Room {
    private final String name;
    private final Set<ClientSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Adds a client session to this room.
     */
    public void join(ClientSession session) {
        sessions.add(session);
    }

    /**
     * Removes a client session from this room.
     */
    public void leave(ClientSession session) {
        sessions.remove(session);
    }

    /**
     * Broadcasts a message to all active sessions in this room.
     *
     * @param message the message to broadcast
     */
    public void broadcast(Message message) {
        for (ClientSession session : sessions) {
            session.send(message);
        }
    }

    /**
     * Broadcasts a message to all active sessions except the specified sender.
     *
     * @param message the message to broadcast
     * @param sender session to exclude from broadcast
     */
    public void broadcast(Message message, ClientSession sender) {
        for (ClientSession session : sessions) {
            if (session != sender) {
                session.send(message);
            }
        }
    }

    public Set<ClientSession> getSessions() {
        return Collections.unmodifiableSet(sessions);
    }
}
