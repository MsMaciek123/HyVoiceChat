package pl.msmaciek.session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private static final Gson gson = new Gson();

    private final Map<Integer, UserSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, UserSession> uuidToSession = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public int nextId() {
        return idCounter.incrementAndGet();
    }

    public void add(UserSession session) {
        sessions.put(session.getOdapId(), session);
    }

    public void linkToPlayer(UserSession session, UUID playerUuid) {
        session.setPlayerUuid(playerUuid);
        uuidToSession.put(playerUuid, session);
    }

    public void remove(int odapId) {
        UserSession session = sessions.remove(odapId);
        if (session != null && session.getPlayerUuid() != null) {
            uuidToSession.remove(session.getPlayerUuid());
        }
    }

    public UserSession get(int odapId) {
        return sessions.get(odapId);
    }

    public UserSession getByUuid(UUID playerUuid) {
        return uuidToSession.get(playerUuid);
    }

    public Collection<UserSession> getAll() {
        return sessions.values();
    }

    /**
     * Get the username associated with an IP address (for IP-based auth).
     * Returns null if no session from this IP has claimed a username.
     */
    public String getUsernameForIp(String ipAddress) {
        for (UserSession session : sessions.values()) {
            if (session.getName() != null && ipAddress.equals(session.getClientIp())) {
                return session.getName();
            }
        }
        return null;
    }

    /**
     * Broadcast a position update to all connected voice clients.
     */
    public void broadcastPositionUpdate(UUID playerUuid, double x, double y, double z, float yaw) {
        UserSession session = uuidToSession.get(playerUuid);
        if (session == null) return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "position_update");
        msg.addProperty("id", session.getOdapId());
        msg.addProperty("x", x);
        msg.addProperty("y", y);
        msg.addProperty("z", z);
        msg.addProperty("yaw", yaw);

        String message = gson.toJson(msg);

        for (UserSession s : sessions.values()) {
            if (s.getSession().isOpen()) {
                try {
                    s.getSession().getRemote().sendString(message);
                } catch (IOException e) {
                    System.err.println("Position broadcast error: " + e.getMessage());
                }
            }
        }
    }
}
