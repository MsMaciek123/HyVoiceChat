package pl.msmaciek.auth;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationManager {
    private static final VerificationManager INSTANCE = new VerificationManager();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, String> sessionToCode = new ConcurrentHashMap<>();
    private final Map<String, String> codeToSession = new ConcurrentHashMap<>();
    private final Map<String, UUID> verifiedCodes = new ConcurrentHashMap<>();
    private final Map<String, String> verifiedUsernames = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    private VerificationManager() {}

    public static VerificationManager getInstance() {
        return INSTANCE;
    }

    public String getOrCreateCode(String sessionId) {
        synchronized (lock) {
            String existingCode = sessionToCode.get(sessionId);
            if (existingCode != null) {
                return existingCode;
            }

            String code = generateCode();
            while (codeToSession.containsKey(code)) {
                code = generateCode();
            }

            sessionToCode.put(sessionId, code);
            codeToSession.put(code, sessionId);
            return code;
        }
    }

    public boolean verify(String code, UUID playerUuid, String playerName) {
        synchronized (lock) {
            code = code.toUpperCase();

            String session = codeToSession.get(code);
            if (session == null) {
                return false;
            }

            verifiedCodes.put(code, playerUuid);
            verifiedUsernames.put(code, playerName);
            return true;
        }
    }

    public UUID getVerifiedPlayer(String code) {
        return verifiedCodes.get(code.toUpperCase());
    }

    public String getVerifiedUsername(String code) {
        return verifiedUsernames.get(code.toUpperCase());
    }

    public String getCodeForSession(String sessionId) {
        return sessionToCode.get(sessionId);
    }

    public boolean isVerified(String code) {
        return verifiedCodes.containsKey(code.toUpperCase());
    }

    public void consumeCode(String code) {
        synchronized (lock) {
            code = code.toUpperCase();
            String session = codeToSession.remove(code);
            if (session != null) {
                sessionToCode.remove(session);
            }
            verifiedCodes.remove(code);
            verifiedUsernames.remove(code);
        }
    }

    public void invalidateForSession(String sessionId) {
        synchronized (lock) {
            String code = sessionToCode.remove(sessionId);
            if (code != null) {
                codeToSession.remove(code);
                verifiedCodes.remove(code);
                verifiedUsernames.remove(code);
            }
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
