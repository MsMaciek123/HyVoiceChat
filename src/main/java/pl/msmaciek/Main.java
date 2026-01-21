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
import pl.msmaciek.api.ServeoApi;
import pl.msmaciek.commands.VoiceChatReloadCommand;
import pl.msmaciek.commands.VoiceChatVerifyCommand;
import pl.msmaciek.config.VoiceChatConfig;
import pl.msmaciek.nameplate.NameplateManager;
import pl.msmaciek.player.PlayerTracker;
import pl.msmaciek.server.WebServer;
import pl.msmaciek.session.SessionManager;
import pl.msmaciek.ui.NearbyPlayersUI;

import java.awt.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JavaPlugin {
    public static Config<VoiceChatConfig> CONFIG;
    @Getter private static Main instance;
    private WebServer webServer;
    @Getter private static String tunnelUrl = null;

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s]+)");


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

        webServer = new WebServer(this.getLogger(), CONFIG.get(), CONFIG.get().getTunnel().isUseTunnel());
        webServer.startAsync();
        // Initialize tunnel if enabled
        if (CONFIG.get().getTunnel().isUseTunnel()) {
            this.getLogger().at(Level.INFO).log("Starting Serveo tunnel...");
            tunnelUrl = ServeoApi.open(CONFIG.get().getServer().getWebSocketPort());
            if (tunnelUrl != null) {
                this.getLogger().at(Level.INFO).log("Tunnel opened at: " + tunnelUrl);
            } else {
                this.getLogger().at(Level.SEVERE).log("Failed to create tunnel! Something went wrong during creation of tunnel.");
            }
        }

        SessionManager.getInstance().startScheduler(CONFIG.get().getGeneral().getUpdateIntervalMs());
        NameplateManager.getInstance().start();

        this.getLogger().at(Level.INFO).log("HyVoiceChat mod initialized!");
    }

    private void onPlayerJoin(AddPlayerToWorldEvent event) {
        var player = event.getHolder().getComponent(Player.getComponentType());
        var playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;

        PlayerTracker.getInstance().playerJoined(
            playerRef.getUuid(),
            player.getDisplayName()
        );

        String joinMessage = CONFIG.get().getMessages().getJoinMessage();
        if (joinMessage != null && !joinMessage.isEmpty()) {
            String formattedMessage = joinMessage.replace("{port}", String.valueOf(CONFIG.get().getServer().getWebSocketPort()));

            if (formattedMessage.contains("{tunnelurl}")) {
                if (tunnelUrl != null) {
                    formattedMessage = formattedMessage.replace("{tunnelurl}", tunnelUrl);
                } else {
                    player.sendMessage(Message.raw("[VoiceChat] Something went wrong during creation of tunnel.").color(Color.RED));
                    formattedMessage = formattedMessage.replace("{tunnelurl}", "[tunnel unavailable]");
                }
            }

            String url = null;

            Matcher matcher = URL_PATTERN.matcher(formattedMessage);
            if (matcher.find()) url = matcher.group(1);

            var message = Message.raw("[VoiceChat] " + formattedMessage).color(Color.CYAN);

            if (url != null)
                message = message.link(url);

            player.sendMessage(message);
        }

        if (CONFIG.get().getGeneral().isEnableUI())
            NearbyPlayersUI.apply(player, playerRef);

        NameplateManager.getInstance().removeOldNameplates(playerRef);
        this.getLogger().at(Level.INFO).log("Player joined: " + player.getDisplayName());
    }


    @Override
    protected void shutdown() {
        super.shutdown();
        SessionManager.getInstance().stopScheduler();
        NameplateManager.getInstance().stop();
        if (webServer != null) {
            webServer.stop();
        }
    }
}