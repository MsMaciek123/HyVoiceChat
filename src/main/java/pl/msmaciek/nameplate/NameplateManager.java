package pl.msmaciek.nameplate;

import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import pl.msmaciek.Main;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages nameplate text for voice chat status
 */
public class NameplateManager {
    private static final NameplateManager INSTANCE = new NameplateManager();

    private static final String TALKING_SUFFIX = " (Talking)";
    private static final String NOT_CONNECTED_SUFFIX = " (No voice chat)";
    private static final long TALKING_TIMEOUT_MS = 500; // How long to show suffix after last audio packet

    // Track when players last spoke
    private final Map<UUID, Long> lastTalkTime = new ConcurrentHashMap<>();

    // Track which players are connected to voice chat
    private final Map<UUID, Boolean> connectedPlayers = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    private NameplateManager() {}

    public static NameplateManager getInstance() {
        return INSTANCE;
    }

    public void removeOldNameplates(PlayerRef playerRef) {
        Nameplate nameplate = playerRef.getComponent(Nameplate.getComponentType());
        if (nameplate == null) return;

        String text = playerRef.getUsername();
        text = text.replace(TALKING_SUFFIX, "");
        text = text.replace(NOT_CONNECTED_SUFFIX, "");
        nameplate.setText(text);
    }

    /**
     * Start the nameplate update scheduler
     */
    public void start() {
        if (scheduler != null) return;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::updateAllNameplates, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the nameplate update scheduler
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        lastTalkTime.clear();
        connectedPlayers.clear();
    }

    /**
     * Mark a player as talking (called when audio data is received)
     */
    public void markTalking(UUID playerUuid) {
        lastTalkTime.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * Mark a player as connected to voice chat
     */
    public void markConnected(UUID playerUuid) {
        connectedPlayers.put(playerUuid, true);
    }

    /**
     * Mark a player as disconnected from voice chat
     */
    public void markDisconnected(UUID playerUuid) {
        connectedPlayers.remove(playerUuid);
        lastTalkTime.remove(playerUuid);
    }

    /**
     * Update all player nameplates based on their voice chat status
     */
    private void updateAllNameplates() {
        if (!Main.CONFIG.get().getGeneral().isOverrideNameplates()) return;

        Universe universe = Universe.get();
        if (universe == null) return;

        for (PlayerRef playerRef : universe.getPlayers()) {
            UUID playerUuid = playerRef.getUuid();
            UUID worldUuid = playerRef.getWorldUuid();

            if (worldUuid == null) continue;
            World world = universe.getWorld(worldUuid);
            if (world == null) continue;
            world.execute(() -> updateNameplate(playerRef, playerUuid));
        }
    }

    /**
     * Update a single player's nameplate
     */
    private void updateNameplate(PlayerRef playerRef, UUID playerUuid) {
        Nameplate nameplate = playerRef.getComponent(Nameplate.getComponentType());
        if (nameplate == null) return;

        boolean isTalking = isTalking(playerUuid);
        boolean isConnected = isConnected(playerUuid);

        String text = playerRef.getUsername();
        if (isTalking) {
            text += TALKING_SUFFIX;
        } else if (!isConnected) {
            text += NOT_CONNECTED_SUFFIX;
        }

        nameplate.setText(text);
    }

    public boolean isConnected(UUID playerUuid) {
        return connectedPlayers.getOrDefault(playerUuid, false);
    }

    public boolean isTalking(UUID playerUuid) {
        Long lastTalk = lastTalkTime.get(playerUuid);
        return lastTalk != null && (System.currentTimeMillis() - lastTalk) < TALKING_TIMEOUT_MS;
    }
}
