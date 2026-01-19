package pl.msmaciek.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import pl.msmaciek.player.PlayerTracker;
import pl.msmaciek.session.SessionManager;
import pl.msmaciek.structs.Position;

/**
 * Ticking system that monitors player movement and broadcasts position updates.
 * Also tracks player presence to detect when players leave.
 */
public class MovementTickingSystem extends EntityTickingSystem<EntityStore> {

    private int tickCounter = 0;

    @Override
    public void tick(float deltaTime, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) return;

        // Mark player as present for leave detection
        PlayerPresenceTracker.getInstance().markPresent(playerRef.getUuid());

        // Get position from PlayerRef's transform
        var transform = playerRef.getTransform();
        var position = transform.getPosition();
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();

        // Get rotation - Y component is yaw (horizontal rotation)
        var rotation = transform.getRotation();
        // Hytale returns rotation in radians, convert to degrees for client
        float yawRadians = rotation.getY();
        float yaw = (float) Math.toDegrees(yawRadians);

        // Normalize yaw to 0-360 range
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;

        // Update tracker with current position and rotation
        PlayerTracker.getInstance().updatePosition(playerRef.getUuid(), x, y, z, yaw);

        // Check if position changed and broadcast to voice clients
        Position trackedPos = PlayerTracker.getInstance().getPosition(playerRef.getUuid());
        if (trackedPos != null && trackedPos.hasChangedAndReset()) {
            SessionManager.getInstance().broadcastPositionUpdate(
                    playerRef.getUuid(),
                    trackedPos.getX(),
                    trackedPos.getY(),
                    trackedPos.getZ(),
                    trackedPos.getYaw()
            );
        }

        tickCounter++;
        if (tickCounter >= 30) {
            tickCounter = 0;
            PlayerPresenceTracker.getInstance().endTick();
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
