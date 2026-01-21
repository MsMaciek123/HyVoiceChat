package pl.msmaciek.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;

@Getter
public class VoiceChatConfig {

    // Nested config classes
    @Getter
    public static class ServerConfig {
        public static final BuilderCodec<ServerConfig> CODEC = BuilderCodec.builder(ServerConfig.class, ServerConfig::new)
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
                .build();

        private int webSocketPort = 8443;
        private boolean useSSL = true;
        private String sslKeystorePath = "example.keystore";
        private String sslKeystorePassword = "changeit";

        public ServerConfig() {}
    }

    @Getter
    public static class MessagesConfig {
        public static final BuilderCodec<MessagesConfig> CODEC = BuilderCodec.builder(MessagesConfig.class, MessagesConfig::new)
                .append(new KeyedCodec<>("JoinMessage", Codec.STRING),
                        (config, value, extraInfo) -> config.joinMessage = value,
                        (config, extraInfo) -> config.joinMessage).add()
                .build();

        private String joinMessage = "This server has Voice Chat! Connect at: {tunnelurl}";

        public MessagesConfig() {}
    }

    @Getter
    public static class AudioConfig {
        public static final BuilderCodec<AudioConfig> CODEC = BuilderCodec.builder(AudioConfig.class, AudioConfig::new)
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
                .build();

        private double maxDistance = 75.0;
        private DistanceFormula distanceFormula = DistanceFormula.EXPONENTIAL;
        private VoiceDimension voiceDimension = VoiceDimension.THREE_D;
        private double rolloffFactor = 1.5;
        private double refDistance = 10.0;
        private double serverCutoffMultiplier = 1.1;
        private double blend2dDistance = 20.0;
        private double full3dDistance = 50.0;

        public AudioConfig() {}
    }

    @Getter
    public static class GeneralConfig {
        public static final BuilderCodec<GeneralConfig> CODEC = BuilderCodec.builder(GeneralConfig.class, GeneralConfig::new)
                .append(new KeyedCodec<>("UpdateIntervalMs", Codec.LONG),
                        (config, value, extraInfo) -> config.updateIntervalMs = value,
                        (config, extraInfo) -> config.updateIntervalMs).add()
                .append(new KeyedCodec<>("OverrideNameplates", Codec.BOOLEAN),
                        (config, value, extraInfo) -> config.overrideNameplates = value,
                        (config, extraInfo) -> config.overrideNameplates).add()
                .append(new KeyedCodec<>("RequirePermissionToConnect", Codec.BOOLEAN),
                        (config, value, extraInfo) -> config.requirePermissionToConnect = value,
                        (config, extraInfo) -> config.requirePermissionToConnect).add()
                .append(new KeyedCodec<>("EnableUI", Codec.BOOLEAN),
                        (config, value, extraInfo) -> config.enableUI = value,
                        (config, extraInfo) -> config.enableUI).add()
                .build();

        private long updateIntervalMs = 50;
        private boolean overrideNameplates = true;
        private boolean requirePermissionToConnect = false;
        private boolean enableUI = true;

        public GeneralConfig() {}
    }

    @Getter
    public static class TunnelConfig {
        public static final BuilderCodec<TunnelConfig> CODEC = BuilderCodec.builder(TunnelConfig.class, TunnelConfig::new)
                .append(new KeyedCodec<>("UseTunnel", Codec.BOOLEAN),
                        (config, value, extraInfo) -> config.useTunnel = value,
                        (config, extraInfo) -> config.useTunnel).add()
                .build();

        private boolean useTunnel = true;

        public TunnelConfig() {}
    }

    public static final BuilderCodec<VoiceChatConfig> CODEC = BuilderCodec.builder(VoiceChatConfig.class, VoiceChatConfig::new)
            .append(new KeyedCodec<>("Server", ServerConfig.CODEC),
                    (config, value, extraInfo) -> config.server = value,
                    (config, extraInfo) -> config.server).add()
            .append(new KeyedCodec<>("Messages", MessagesConfig.CODEC),
                    (config, value, extraInfo) -> config.messages = value,
                    (config, extraInfo) -> config.messages).add()
            .append(new KeyedCodec<>("Audio", AudioConfig.CODEC),
                    (config, value, extraInfo) -> config.audio = value,
                    (config, extraInfo) -> config.audio).add()
            .append(new KeyedCodec<>("General", GeneralConfig.CODEC),
                    (config, value, extraInfo) -> config.general = value,
                    (config, extraInfo) -> config.general).add()
            .append(new KeyedCodec<>("Tunnel", TunnelConfig.CODEC),
                    (config, value, extraInfo) -> config.tunnel = value,
                    (config, extraInfo) -> config.tunnel).add()
            .build();

    private ServerConfig server = new ServerConfig();
    private MessagesConfig messages = new MessagesConfig();
    private AudioConfig audio = new AudioConfig();
    private GeneralConfig general = new GeneralConfig();
    private TunnelConfig tunnel = new TunnelConfig();

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
