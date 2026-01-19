package pl.msmaciek;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import lombok.Getter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.msmaciek.commands.VoiceChatReloadCommand;
import pl.msmaciek.commands.VoiceChatVerifyCommand;
import pl.msmaciek.config.VoiceChatConfig;
import pl.msmaciek.player.PlayerTracker;
import pl.msmaciek.server.WebServer;
import pl.msmaciek.systems.MovementTickingSystem;

import java.util.logging.Level;

public class Main extends JavaPlugin {
    public static Config<VoiceChatConfig> CONFIG;
    @Getter private static Main instance;
    private WebServer webServer;

    public Main(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
        CONFIG = this.withConfig("HyVoiceChat", VoiceChatConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        CONFIG.save();

        // Register movement tracking system
        this.getEntityStoreRegistry().registerSystem(new MovementTickingSystem());

        // Register player join event
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerJoin);

        // Register console commands
        this.getCommandRegistry().registerCommand(new VoiceChatReloadCommand());
        this.getCommandRegistry().registerCommand(new VoiceChatVerifyCommand());

        // Start WebSocket server
        webServer = new WebServer(this.getLogger(), CONFIG.get());
        webServer.startAsync();

        this.getLogger().at(Level.INFO).log("HyVoiceChat mod initialized!");
        this.getLogger().at(Level.INFO).log("Voice chat available at http://localhost:" + CONFIG.get().getWebSocketPort());
    }

    private void onPlayerJoin(AddPlayerToWorldEvent event) {
        var player = event.getHolder().getComponent(Player.getComponentType());
        var playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;

        PlayerTracker.getInstance().playerJoined(
            playerRef.getUuid(),
            player.getDisplayName(),
            "unknown" // IP not available from Hytale API yet
        );

        this.getLogger().at(Level.INFO).log("Player joined: " + player.getDisplayName());
    }


    @Override
    protected void shutdown() {
        super.shutdown();
        if (webServer != null) {
            webServer.stop();
        }
    }
}