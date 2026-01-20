package pl.msmaciek.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import pl.msmaciek.Main;
import pl.msmaciek.auth.VerificationManager;
import pl.msmaciek.config.VoiceChatConfig;
import pl.msmaciek.nameplate.NameplateManager;
import pl.msmaciek.player.PlayerTracker;
import pl.msmaciek.session.SessionManager;
import pl.msmaciek.session.UserSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class VoiceChatEndpoint extends WebSocketAdapter {
    private static final Gson gson = new Gson();
    private static final SessionManager sessions = SessionManager.getInstance();

    private int odapId;
    private UserSession userSession;
    private String clientIp;
    private String verificationCode;

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);

        // Extract client IP address
        clientIp = extractClientIp(session);

        odapId = sessions.nextId();
        userSession = new UserSession(odapId, session, clientIp);
        sessions.add(userSession);

        // Send session ID and config
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "id");
        msg.addProperty("id", odapId);
        send(session, msg);

        // Send server config for client-side audio processing
        userSession.sendConfig(Main.CONFIG.get());

        // Generate or get verification code for this IP
        verificationCode = VerificationManager.getInstance().getOrCreateCode(clientIp);
        sendVerificationCode(session);

        System.out.println("WebSocket connected: " + odapId + " from IP: " + clientIp + ", code: " + verificationCode);
    }

    private String extractClientIp(Session session) {
        try {
            InetSocketAddress remoteAddr = (InetSocketAddress) session.getRemoteAddress();
            if (remoteAddr != null) {
                return remoteAddr.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            System.err.println("Could not extract client IP: " + e.getMessage());
        }
        return "unknown";
    }

    private void sendVerificationCode(Session session) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "verification_code");
        msg.addProperty("code", verificationCode);
        msg.addProperty("command", "/voicechat " + verificationCode);
        send(session, msg);
    }

    private void sendVerificationStatus(Session session) {
        VerificationManager vm = VerificationManager.getInstance();
        boolean verified = vm.isVerified(verificationCode);
        String username = verified ? vm.getVerifiedUsername(verificationCode) : null;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "verification_status");
        msg.addProperty("verified", verified);
        if (username != null) {
            msg.addProperty("username", username);
        }
        send(session, msg);
    }

    @Override
    public void onWebSocketText(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                case "check_verification" -> sendVerificationStatus(getSession());
                case "join" -> handleJoin();
                case "ping" -> handlePing(json);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (userSession == null || userSession.getName() == null || userSession.getPlayerUuid() == null) return;

        NameplateManager.getInstance().markTalking(userSession.getPlayerUuid());

        VoiceChatConfig config = Main.CONFIG.get();
        boolean use2D = config.getVoiceDimension() == VoiceChatConfig.VoiceDimension.TWO_D;
        // Server cutoff is maxDistance * multiplier to allow client-side attenuation to work
        double serverCutoff = config.getMaxDistance() * config.getServerCutoffMultiplier();

        ByteBuffer buffer = ByteBuffer.allocate(4 + len);
        buffer.putInt(odapId);
        buffer.put(payload, offset, len);
        buffer.flip();

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        for (UserSession s : sessions.getAll()) {
            if (s.getOdapId() != odapId && s.getSession().isOpen() && s.getName() != null && s.getPlayerUuid() != null) {
                // Calculate distance and skip if beyond server cutoff
                double distance = userSession.distanceTo(s);
                if (distance > serverCutoff) {
                    continue; // Don't send audio to players too far away
                }

                try {
                    ByteBuffer sendBuffer = ByteBuffer.wrap(data);
                    s.getSession().getRemote().sendBytes(sendBuffer);
                } catch (IOException e) {
                    System.err.println("Audio send error: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);

        // Mark player as disconnected from voice chat
        if (userSession != null && userSession.getPlayerUuid() != null) {
            NameplateManager.getInstance().markDisconnected(userSession.getPlayerUuid());
        }

        // Release the username claim
        if (userSession != null && userSession.getName() != null) {
            PlayerTracker.getInstance().releaseUsername(userSession.getName());
        }

        // Invalidate verification code if not consumed
        if (verificationCode != null && userSession.getName() == null) {
            VerificationManager.getInstance().invalidateForIp(clientIp);
        }

        sessions.remove(odapId);

        SessionManager.getInstance().broadcastPlayerSnapshot();

        System.out.println("WebSocket disconnected: " + odapId);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        System.err.println("WebSocket error: " + cause.getMessage());
    }

    private void handleJoin() {
        VerificationManager vm = VerificationManager.getInstance();

        // Check if verified
        if (!vm.isVerified(verificationCode)) {
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("type", "join_error");
            errorMsg.addProperty("error", "Please verify first by typing the command in game chat.");
            send(getSession(), errorMsg);
            return;
        }

        // Get verified player info
        UUID playerUuid = vm.getVerifiedPlayer(verificationCode);
        String playerName = vm.getVerifiedUsername(verificationCode);

        if (playerUuid == null || playerName == null) {
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("type", "join_error");
            errorMsg.addProperty("error", "Verification expired. Please refresh and try again.");
            send(getSession(), errorMsg);
            return;
        }

        // Check if player is still online
        if (!PlayerTracker.getInstance().isOnline(playerName)) {
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("type", "join_error");
            errorMsg.addProperty("error", "Player is no longer online.");
            send(getSession(), errorMsg);
            vm.consumeCode(verificationCode);
            return;
        }

        // Disconnect any existing session with this username (kick the older one)
        UserSession existingSession = sessions.disconnectByUsername(playerName);
        if (existingSession != null) {
            System.out.println("Disconnected existing session for: " + playerName);
            // Release the username claim from the old session
            PlayerTracker.getInstance().releaseUsername(playerName);
        }

        // Try to claim the username
        if (!PlayerTracker.getInstance().tryClaimUsername(playerName)) {
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("type", "join_error");
            errorMsg.addProperty("error", "This player is already in voice chat.");
            send(getSession(), errorMsg);
            vm.consumeCode(verificationCode);
            return;
        }

        // Success - consume the code
        vm.consumeCode(verificationCode);

        userSession.setName(playerName);
        sessions.linkToPlayer(userSession, playerUuid);

        // Mark player as connected to voice chat
        NameplateManager.getInstance().markConnected(playerUuid);

        // Send join success - snapshots will handle player list
        JsonObject joinMsg = new JsonObject();
        joinMsg.addProperty("type", "join_success");
        joinMsg.addProperty("id", odapId);
        joinMsg.addProperty("name", playerName);
        send(getSession(), joinMsg);

        System.out.println("User joined voice chat: " + playerName + " (ID: " + odapId + ")");
    }


    private void handlePing(JsonObject json) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "pong");
        msg.addProperty("timestamp", json.get("timestamp").getAsLong());
        send(getSession(), msg);
    }

    private void send(Session session, JsonObject json) {
        try {
            session.getRemote().sendString(gson.toJson(json));
        } catch (IOException e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }
}
