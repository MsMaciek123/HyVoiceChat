package pl.msmaciek.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import pl.msmaciek.auth.VerificationManager;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command to verify voice chat with a code.
 * Usage: /voicechat <code>
 */
public class VoiceChatVerifyCommand extends AbstractPlayerCommand {

    private static final Message INVALID_CODE = Message.raw("[VoiceChat] Invalid or expired code.").color(Color.RED);
    private static final Message ALREADY_VERIFIED = Message.raw("[VoiceChat] This code has already been verified.").color(Color.YELLOW);
    private static final Message SUCCESS = Message.raw("[VoiceChat] Verified! You can now join the voice chat.").color(Color.GREEN);

    private final RequiredArg<String> codeArg;

    public VoiceChatVerifyCommand() {
        super("voicechat", "Verify your voice chat connection with a code");
        this.addAliases("vc", "voice");
        this.codeArg = this.withRequiredArg("code", "The verification code", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        CommandSender sender = commandContext.sender();

        if (!(sender instanceof Player player)) return;

        String code = commandContext.get(this.codeArg);
        if (code == null || code.isEmpty())  return;

        code = code.toUpperCase();

        // Check if code exists
        VerificationManager vm = VerificationManager.getInstance();

        if (vm.isVerified(code)) {
            player.sendMessage(ALREADY_VERIFIED);
            return;
        }

        // Try to verify
        boolean success = vm.verify(code, playerRef.getUuid(), player.getDisplayName());

        if (success) {
            player.sendMessage(SUCCESS);
        } else {
            player.sendMessage(INVALID_CODE);
        }
    }
}
