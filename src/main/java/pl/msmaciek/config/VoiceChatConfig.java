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

    private int webSocketPort = 8443;
    private boolean useSSL = true;
    private String sslKeystorePath = "example.keystore";
    private String sslKeystorePassword = "changeit";
    private double maxDistance = 75.0;
    private DistanceFormula distanceFormula = DistanceFormula.LINEAR;
    private VoiceDimension voiceDimension = VoiceDimension.THREE_D;
    private double rolloffFactor = 1.0;
    private double refDistance = 1.0;
    private double serverCutoffMultiplier = 1.1;
    private double blend2dDistance = 10.0;
    private double full3dDistance = 30.0;

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
