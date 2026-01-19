package pl.msmaciek.structs;

import lombok.Getter;

/**
 * Position data class with change detection.
 * Represents a 3D position with yaw rotation.
 */
@Getter
public class Position {
    private double x, y, z;
    private float yaw; // rotation in degrees (0 = +Z, 90 = -X, 180 = -Z, 270 = +X)
    private boolean changed = false;

    public Position(double x, double y, double z) {
        this(x, y, z, 0f);
    }

    public Position(double x, double y, double z, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    public void set(double x, double y, double z, float yaw) {
        if (this.x != x || this.y != y || this.z != z || this.yaw != yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.changed = true;
        }
    }

    public boolean hasChangedAndReset() {
        if (changed) {
            changed = false;
            return true;
        }
        return false;
    }

    public double distanceTo(Position other, boolean use2D) {
        double dx = this.x - other.x;
        double dy = use2D ? 0 : (this.y - other.y);
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
