package pl.msmaciek.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class NearbyPlayersUI extends CustomUIHud {
    public NearbyPlayersUI(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    public static void apply(Player player, PlayerRef playerRef) {
        player.getHudManager().setCustomHud(playerRef, new NearbyPlayersUI(playerRef));
    }

    public void updateNearbyPlayers(ArrayList<String> playerNames) {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();

        StringBuilder playersText = new StringBuilder();
        if(!playerNames.isEmpty()) {
            playersText.append("Speaking nearby (").append(playerNames.size()).append(" players):\n");
        }

        for (String playerName : playerNames) {
            playersText.append(playerName).append("\n");
        }

        uiCommandBuilder.set("#Players.TextSpans", Message.raw(
            playersText.toString()
        ));
        update(false, uiCommandBuilder);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hud/NearbyPlayers.ui");
    }
}
