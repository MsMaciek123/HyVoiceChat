package pl.msmaciek.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import pl.msmaciek.Main;
import pl.msmaciek.session.SessionManager;
import pl.msmaciek.session.UserSession;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * Console-only command to reload the VoiceChat configuration.
 */
public class VoiceChatReloadCommand extends AbstractAsyncCommand {

    private static final Message RELOAD_SUCCESS = Message.raw("[VoiceChat] Configuration reloaded successfully!").color(Color.GREEN);
    private static final Message RELOAD_ERROR = Message.raw("[VoiceChat] Failed to reload configuration!").color(Color.RED);
    private static final Message CONSOLE_ONLY = Message.raw("[VoiceChat] This command can only be used from the console.").color(Color.RED);

    public VoiceChatReloadCommand() {
        super("voicechat-reload", "Reloads the VoiceChat configuration (console only)");
        this.addAliases("vcreload", "vc-reload");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();

        // Only allow console to use this command
        if (sender instanceof Player player) {
            player.sendMessage(CONSOLE_ONLY);
            return CompletableFuture.completedFuture(null);
        }

        Main.CONFIG.load();
        System.out.println("[VoiceChat] Configuration reloaded successfully!");

        int count = 0;
        for (UserSession session : SessionManager.getInstance().getAll()) {
            if (session.getSession().isOpen()) {
                session.sendConfig(Main.CONFIG.get());
                count++;
            }
        }
        System.out.println("[VoiceChat] Updated config sent to " + count + " connected user(s)");

        return CompletableFuture.completedFuture(null);
    }
}