package pl.msmaciek;

import com.hypixel.hytale.server.core.Message;
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
import pl.msmaciek.session.SessionManager;

import java.awt.*;
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

        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerJoin);

        this.getCommandRegistry().registerCommand(new VoiceChatReloadCommand());
        this.getCommandRegistry().registerCommand(new VoiceChatVerifyCommand());

        webServer = new WebServer(this.getLogger(), CONFIG.get());
        webServer.startAsync();

        SessionManager.getInstance().startScheduler(CONFIG.get().getUpdateIntervalMs());

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

        String joinMessage = CONFIG.get().getJoinMessage();
        if (joinMessage != null && !joinMessage.isEmpty()) {
            String formattedMessage = joinMessage.replace("{port}", String.valueOf(CONFIG.get().getWebSocketPort()));
            player.sendMessage(Message.raw("[VoiceChat] " + formattedMessage).color(Color.CYAN));
        }

        this.getLogger().at(Level.INFO).log("Player joined: " + player.getDisplayName());
    }


    @Override
    protected void shutdown() {
        super.shutdown();
        SessionManager.getInstance().stopScheduler();
        if (webServer != null) {
            webServer.stop();
        }
    }
}