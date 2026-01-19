package pl.msmaciek.session;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.eclipse.jetty.websocket.api.Session;
import pl.msmaciek.config.VoiceChatConfig;
import pl.msmaciek.player.PlayerTracker;
import pl.msmaciek.structs.Position;

import java.io.IOException;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserSession {
    private final int odapId;
    private final Session session;
    private final String clientIp;

    @Setter private String name;
    @Setter private UUID playerUuid;
    @Setter private boolean speaking;

    /**
     * Get live position from PlayerTracker (server-authoritative).
     */
    public Position getPosition() {
        if (playerUuid == null) return null;
        return PlayerTracker.getInstance().getPosition(playerUuid);
    }

    /**
     * Get X coordinate from server position.
     */
    public double getX() {
        Position pos = getPosition();
        return pos != null ? pos.getX() : 0;
    }

    /**
     * Get Y coordinate from server position.
     */
    public double getY() {
        Position pos = getPosition();
        return pos != null ? pos.getY() : 0;
    }

    /**
     * Get Z coordinate from server position.
     */
    public double getZ() {
        Position pos = getPosition();
        return pos != null ? pos.getZ() : 0;
    }

    /**
     * Get yaw rotation from server position (degrees).
     */
    public float getYaw() {
        Position pos = getPosition();
        return pos != null ? pos.getYaw() : 0;
    }

    /**
     * Calculate distance to another session.
     * @param other The other session
     * @param use2D If true, ignores Y coordinate (for 2D voice)
     * @return Distance in blocks
     */
    public double distanceTo(UserSession other, boolean use2D) {
        Position myPos = getPosition();
        Position otherPos = other.getPosition();

        if (myPos == null || otherPos == null) {
            return Double.MAX_VALUE; // Cannot calculate, assume far away
        }

        return myPos.distanceTo(otherPos, use2D);
    }

    /**
     * Send server configuration to this session's client.
     * @param config The VoiceChatConfig to send
     */
    public void sendConfig(VoiceChatConfig config) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "config");
        msg.addProperty("maxDistance", config.getMaxDistance());
        msg.addProperty("distanceFormula", config.getDistanceFormula().name());
        msg.addProperty("voiceDimension", config.getVoiceDimension().toString());
        msg.addProperty("rolloffFactor", config.getRolloffFactor());
        msg.addProperty("refDistance", config.getRefDistance());
        msg.addProperty("blend2dDistance", config.getBlend2dDistance());
        msg.addProperty("full3dDistance", config.getFull3dDistance());

        try {
            session.getRemote().sendString(msg.toString());
        } catch (IOException e) {
            System.err.println("Failed to send config to session " + odapId + ": " + e.getMessage());
        }
    }
}
