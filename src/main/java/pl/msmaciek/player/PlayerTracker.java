package pl.msmaciek.player;

import pl.msmaciek.structs.Position;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks online players, their positions, IPs, and voice chat session assignments.
 */
public class PlayerTracker {
    private static final PlayerTracker INSTANCE = new PlayerTracker();

    // UUID -> Username mapping for online players
    private final Map<UUID, String> onlinePlayers = new ConcurrentHashMap<>();

    // IP -> Set of UUIDs (players connecting from that IP)
    private final Map<String, Set<UUID>> ipToPlayers = new ConcurrentHashMap<>();

    // UUID -> Current position
    private final Map<UUID, Position> playerPositions = new ConcurrentHashMap<>();

    // Username -> Session assignment (to prevent duplicate claims in NO_AUTH mode)
    private final Set<String> assignedUsernames = ConcurrentHashMap.newKeySet();

    // UUID -> IP address
    private final Map<UUID, String> playerIps = new ConcurrentHashMap<>();

    private PlayerTracker() {}

    public static PlayerTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a player joins the Hytale server.
     */
    public void playerJoined(UUID uuid, String username, String ipAddress) {
        onlinePlayers.put(uuid, username);
        playerIps.put(uuid, ipAddress);

        ipToPlayers.computeIfAbsent(ipAddress, k -> ConcurrentHashMap.newKeySet()).add(uuid);

        // Initialize position at origin
        playerPositions.put(uuid, new Position(0, 0, 0));
    }

    /**
     * Called when a player leaves the Hytale server.
     */
    public void playerLeft(UUID uuid) {
        String username = onlinePlayers.remove(uuid);
        String ip = playerIps.remove(uuid);
        playerPositions.remove(uuid);

        if (username != null) {
            assignedUsernames.remove(username.toLowerCase());
        }

        if (ip != null) {
            Set<UUID> playersAtIp = ipToPlayers.get(ip);
            if (playersAtIp != null) {
                playersAtIp.remove(uuid);
                if (playersAtIp.isEmpty()) {
                    ipToPlayers.remove(ip);
                }
            }
        }
    }

    /**
     * Update a player's position (called from movement ticking system).
     */
    public void updatePosition(UUID uuid, double x, double y, double z, float yaw) {
        updatePosition(uuid, x, y, z, yaw, null);
    }

    /**
     * Update a player's position with world UUID (called from movement ticking system).
     */
    public void updatePosition(UUID uuid, double x, double y, double z, float yaw, UUID worldUuid) {
        Position pos = playerPositions.get(uuid);
        if (pos != null) {
            pos.set(x, y, z, yaw, worldUuid);
        } else {
            playerPositions.put(uuid, new Position(x, y, z, yaw, worldUuid));
        }
    }

    /**
     * Get a player's current position.
     */
    public Position getPosition(UUID uuid) {
        return playerPositions.get(uuid);
    }

    /**
     * Get UUID by username (case-insensitive).
     */
    public UUID getUuidByUsername(String username) {
        for (Map.Entry<UUID, String> entry : onlinePlayers.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(username)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get username by UUID.
     */
    public String getUsername(UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    /**
     * Check if a username is online.
     */
    public boolean isOnline(String username) {
        return getUuidByUsername(username) != null;
    }

    /**
     * Get all online usernames that haven't been assigned to a voice session (for NO_AUTH mode).
     */
    public List<String> getUnassignedOnlineUsernames() {
        List<String> available = new ArrayList<>();

        for (String username : onlinePlayers.values()) {
            if (!assignedUsernames.contains(username.toLowerCase())) {
                available.add(username);
            }
        }

        return available;
    }

    /**
     * Try to claim a username for a voice chat session.
     * @return true if successfully claimed, false if already taken or invalid
     */
    public boolean tryClaimUsername(String username) {
        String lowerUsername = username.toLowerCase();

        // Check if username is online
        UUID uuid = getUuidByUsername(username);
        if (uuid == null) {
            return false; // Player not online
        }

        // Check if already assigned
        if (assignedUsernames.contains(lowerUsername)) {
            return false;
        }

        assignedUsernames.add(lowerUsername);
        return true;
    }

    /**
     * Release a username claim when session disconnects.
     */
    public void releaseUsername(String username) {
        if (username != null) {
            assignedUsernames.remove(username.toLowerCase());
        }
    }

    /**
     * Get all online player positions for broadcasting.
     */
    public Map<UUID, Position> getAllPositions() {
        return Collections.unmodifiableMap(playerPositions);
    }

    /**
     * Get all online players.
     */
    public Map<UUID, String> getOnlinePlayers() {
        return Collections.unmodifiableMap(onlinePlayers);
    }
}
