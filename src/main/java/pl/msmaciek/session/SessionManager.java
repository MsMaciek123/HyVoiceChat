package pl.msmaciek.session;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import pl.msmaciek.Main;
import pl.msmaciek.config.VoiceChatConfig;
import pl.msmaciek.player.PlayerTracker;
import pl.msmaciek.structs.Position;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private static final Gson gson = new Gson();

    private final Map<Integer, UserSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, UserSession> uuidToSession = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private ScheduledExecutorService scheduler;
    private volatile boolean closed = false;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void startScheduler(long updateIntervalMs) {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (!closed) {
                updateAllPlayerPositions();
                broadcastPlayerSnapshot();
            }
        }, 0L, updateIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void stopScheduler() {
        closed = true;
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private void updateAllPlayerPositions() {
        Set<UUID> foundPlayers = new HashSet<>();

        try {
            Universe universe = Universe.get();
            if (universe == null) return;

            List<PlayerRef> players = universe.getPlayers();
            for (PlayerRef playerRef : players) {
                var transform = playerRef.getTransform();
                var position = transform.getPosition();

                UUID playerUuid = playerRef.getUuid();
                UUID worldUuid = playerRef.getWorldUuid();
                foundPlayers.add(playerUuid);

                double x = position.getX();
                double y = position.getY();
                double z = position.getZ();

                var rotation = transform.getRotation();
                float yawRadians = rotation.getY();
                float yaw = (float) Math.toDegrees(yawRadians);
                yaw = yaw % 360;
                if (yaw < 0) yaw += 360;

                PlayerTracker.getInstance().updatePosition(playerUuid, x, y, z, yaw, worldUuid);
            }
        } catch (Exception e) {
            System.err.println("Error updating player positions: " + e.getMessage());
        }

        for (UUID trackedUuid : PlayerTracker.getInstance().getOnlinePlayers().keySet()) {
            if (!foundPlayers.contains(trackedUuid)) {
                disconnectByUuid(trackedUuid);
                PlayerTracker.getInstance().playerLeft(trackedUuid);
            }
        }
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

    /**
     * Find and disconnect any existing session with the given username.
     * Used when a new session joins with the same name to kick the older one.
     * @param username The username to search for
     * @return The disconnected session, or null if none found
     */
    public UserSession disconnectByUsername(String username) {
        if (username == null) return null;

        for (UserSession session : sessions.values()) {
            if (username.equalsIgnoreCase(session.getName()) && session.getSession().isOpen()) {
                try {
                    // Send disconnect message before closing
                    JsonObject msg = new JsonObject();
                    msg.addProperty("type", "kicked");
                    msg.addProperty("reason", "Another session connected with this account.");
                    session.getSession().getRemote().sendString(gson.toJson(msg));
                    session.getSession().close();
                } catch (Exception e) {
                    System.err.println("Error disconnecting old session: " + e.getMessage());
                }
                return session;
            }
        }
        return null;
    }

    public Collection<UserSession> getAll() {
        return sessions.values();
    }

    /**
     * Disconnect a session by player UUID.
     * Used when a player leaves the game server to also disconnect their voice chat.
     * @param playerUuid The UUID of the player who left
     */
    public void disconnectByUuid(UUID playerUuid) {
        UserSession session = uuidToSession.get(playerUuid);
        if (session != null && session.getSession().isOpen()) {
            try {
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "kicked");
                msg.addProperty("reason", "Player left the game server.");
                session.getSession().getRemote().sendString(gson.toJson(msg));
                session.getSession().close();
            } catch (Exception e) {
                System.err.println("Error disconnecting session for leaving player: " + e.getMessage());
            }
        }
    }

    /**
     * Send a complete snapshot of all nearby players to each connected client.
     * This is stateless - clients replace their entire user list with this data.
     * Each client receives: their own position + list of nearby players in same world.
     */
    public void broadcastPlayerSnapshot() {
        VoiceChatConfig config = Main.CONFIG.get();
        double maxDistance = config.getMaxDistance() * config.getServerCutoffMultiplier();
        boolean use2D = config.getVoiceDimension() == VoiceChatConfig.VoiceDimension.TWO_D;

        for (UserSession targetSession : sessions.values()) {
            if (!targetSession.getSession().isOpen()) continue;
            if (targetSession.getPlayerUuid() == null) continue;

            Position targetPos = targetSession.getPosition();
            if (targetPos == null) continue;

            JsonObject msg = new JsonObject();
            msg.addProperty("type", "players_snapshot");

            // Add self info
            msg.add("self", packPositionPacket(targetSession, targetPos));

            // Add nearby players
            JsonArray nearbyPlayers = new JsonArray();
            for (UserSession otherSession : sessions.values()) {
                if (otherSession.getOdapId() == targetSession.getOdapId()) continue;
                if (!otherSession.getSession().isOpen()) continue;
                if (otherSession.getPlayerUuid() == null) continue;

                Position otherPos = otherSession.getPosition();
                if (otherPos == null) continue;

                // Check same world
                if (!targetPos.isSameWorld(otherPos)) continue;

                // Check distance
                double distance = targetPos.distanceTo(otherPos);
                if (distance > maxDistance) continue;

                // Add to nearby list
                nearbyPlayers.add(packPositionPacket(otherSession, otherPos));
            }
            msg.add("players", nearbyPlayers);

            try {
                targetSession.getSession().getRemote().sendString(gson.toJson(msg));
            } catch (IOException e) {
                System.err.println("Snapshot broadcast error: " + e.getMessage());
            }
        }
    }

    private JsonObject packPositionPacket(UserSession targetSession, Position targetPos) {
        JsonObject self = new JsonObject();
        self.addProperty("id", targetSession.getOdapId());
        self.addProperty("name", targetSession.getName());
        self.addProperty("x", targetPos.getX());
        self.addProperty("y", targetPos.getY());
        self.addProperty("z", targetPos.getZ());
        self.addProperty("yaw", targetPos.getYaw());
        return self;
    }
}
