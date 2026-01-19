package pl.msmaciek.structs;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Position data class.
 * Represents a 3D position with yaw rotation and world UUID.
 */
@Getter
public class Position {
    private double x, y, z;
    private float yaw; // rotation in degrees (0 = +Z, 90 = -X, 180 = -Z, 270 = +X)
    private UUID worldUuid;

    public Position(double x, double y, double z) {
        this(x, y, z, 0f, null);
    }

    public Position(double x, double y, double z, float yaw) {
        this(x, y, z, yaw, null);
    }

    public Position(double x, double y, double z, float yaw, UUID worldUuid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.worldUuid = worldUuid;
    }

    public void set(double x, double y, double z, float yaw) {
        set(x, y, z, yaw, this.worldUuid);
    }

    public void set(double x, double y, double z, float yaw, UUID worldUuid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.worldUuid = worldUuid;
    }

    /**
     * Check if this position is in the same world as another position.
     */
    public boolean isSameWorld(Position other) {
        if (other == null) return false;
        return Objects.equals(this.worldUuid, other.worldUuid);
    }


    /**
     * Calculate distance to another position.
     * Note: use2D parameter is kept for API compatibility but distance always uses all 3 axes.
     * 2D voice refers to audio panning (mono/stereo), not distance calculation.
     */
    public double distanceTo(Position other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
