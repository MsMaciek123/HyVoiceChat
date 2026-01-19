package pl.msmaciek.systems;

import pl.msmaciek.player.PlayerTracker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players were seen in the current tick cycle.
 * Used to detect when players leave the server.
 */
public class PlayerPresenceTracker {
    private static final PlayerPresenceTracker INSTANCE = new PlayerPresenceTracker();

    private final Set<UUID> currentTickPlayers = new HashSet<>();
    private final Set<UUID> previousTickPlayers = new HashSet<>();

    private PlayerPresenceTracker() {}

    public static PlayerPresenceTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Mark a player as present in the current tick.
     */
    public void markPresent(UUID uuid) {
        currentTickPlayers.add(uuid);
    }

    /**
     * Called at the end of each tick cycle to detect players who left.
     */
    public void endTick() {
        // Find players who were in previous tick but not in current
        for (UUID uuid : previousTickPlayers) {
            if (!currentTickPlayers.contains(uuid)) {
                // Player left
                PlayerTracker.getInstance().playerLeft(uuid);
            }
        }

        // Swap sets for next tick
        previousTickPlayers.clear();
        previousTickPlayers.addAll(currentTickPlayers);
        currentTickPlayers.clear();
    }
}
