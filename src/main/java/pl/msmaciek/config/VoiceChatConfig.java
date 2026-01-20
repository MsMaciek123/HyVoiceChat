package pl.msmaciek.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;

@Getter
public class VoiceChatConfig {

    public static final BuilderCodec<VoiceChatConfig> CODEC = BuilderCodec.builder(VoiceChatConfig.class, VoiceChatConfig::new)
            .append(new KeyedCodec<>("WebSocketPort", Codec.INTEGER),
                    (config, value, extraInfo) -> config.webSocketPort = value,
                    (config, extraInfo) -> config.webSocketPort).add()
            .append(new KeyedCodec<>("UseSSL", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.useSSL = value,
                    (config, extraInfo) -> config.useSSL).add()
            .append(new KeyedCodec<>("SSLKeystorePath", Codec.STRING),
                    (config, value, extraInfo) -> config.sslKeystorePath = value,
                    (config, extraInfo) -> config.sslKeystorePath).add()
            .append(new KeyedCodec<>("SSLKeystorePassword", Codec.STRING),
                    (config, value, extraInfo) -> config.sslKeystorePassword = value,
                    (config, extraInfo) -> config.sslKeystorePassword).add()
            .append(new KeyedCodec<>("JoinMessage", Codec.STRING),
                    (config, value, extraInfo) -> config.joinMessage = value,
                    (config, extraInfo) -> config.joinMessage).add()
            .append(new KeyedCodec<>("MaxDistance", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.maxDistance = value,
                    (config, extraInfo) -> config.maxDistance).add()
            .append(new KeyedCodec<>("DistanceFormula", Codec.STRING),
                    (config, value, extraInfo) -> config.distanceFormula = DistanceFormula.fromString(value),
                    (config, extraInfo) -> config.distanceFormula.name()).add()
            .append(new KeyedCodec<>("VoiceDimension", Codec.STRING),
                    (config, value, extraInfo) -> config.voiceDimension = VoiceDimension.fromString(value),
                    (config, extraInfo) -> config.voiceDimension.name()).add()
            .append(new KeyedCodec<>("RolloffFactor", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.rolloffFactor = value,
                    (config, extraInfo) -> config.rolloffFactor).add()
            .append(new KeyedCodec<>("RefDistance", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.refDistance = value,
                    (config, extraInfo) -> config.refDistance).add()
            .append(new KeyedCodec<>("ServerCutoffMultiplier", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.serverCutoffMultiplier = value,
                    (config, extraInfo) -> config.serverCutoffMultiplier).add()
            .append(new KeyedCodec<>("Blend2dDistance", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.blend2dDistance = value,
                    (config, extraInfo) -> config.blend2dDistance).add()
            .append(new KeyedCodec<>("Full3dDistance", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.full3dDistance = value,
                    (config, extraInfo) -> config.full3dDistance).add()
            .append(new KeyedCodec<>("UpdateIntervalMs", Codec.LONG),
                    (config, value, extraInfo) -> config.updateIntervalMs = value,
                    (config, extraInfo) -> config.updateIntervalMs).add()
            .append(new KeyedCodec<>("OverrideNameplates", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.overrideNameplates = value,
                    (config, extraInfo) -> config.overrideNameplates).add()
            .append(new KeyedCodec<>("RequirePermissionToConnect", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.requirePermissionToConnect = value,
                    (config, extraInfo) -> config.requirePermissionToConnect).add()
            .build();

    private int webSocketPort = 8443;
    private boolean useSSL = true;
    private String sslKeystorePath = "example.keystore";
    private String sslKeystorePassword = "changeit";
    private String joinMessage = "This server has Voice Chat! Connect at: https://your-server.com:{port}";
    private double maxDistance = 75.0;
    private DistanceFormula distanceFormula = DistanceFormula.EXPONENTIAL;
    private VoiceDimension voiceDimension = VoiceDimension.THREE_D;
    private double rolloffFactor = 1.5;
    private double refDistance = 10.0;
    private double serverCutoffMultiplier = 1.1;
    private double blend2dDistance = 20.0;
    private double full3dDistance = 50.0;
    private long updateIntervalMs = 50;
    private boolean overrideNameplates = true;
    private boolean requirePermissionToConnect = false;

    public VoiceChatConfig() {}


    public enum DistanceFormula {
        LINEAR,
        EXPONENTIAL,
        INVERSE_SQUARE;

        public static DistanceFormula fromString(String value) {
            try {
                return DistanceFormula.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return LINEAR;
            }
        }
    }

    public enum VoiceDimension {
        TWO_D,
        THREE_D;

        public static VoiceDimension fromString(String value) {
            try {
                if (value.equalsIgnoreCase("2D")) return TWO_D;
                if (value.equalsIgnoreCase("3D")) return THREE_D;
                return VoiceDimension.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return THREE_D;
            }
        }

        @Override
        public String toString() {
            return this == TWO_D ? "2D" : "3D";
        }
    }
}
