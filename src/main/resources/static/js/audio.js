/**
 * Audio Manager - handles 3D audio positioning and playback
 */
class AudioManager {
    constructor(voiceChat) {
        this.voiceChat = voiceChat;
        this.audioContext = null;
        this.micStream = null;
        this.micGain = null;
        this.analyser = null;
        this.processor = null;
        this.players = new Map();
    }

    /**
     * Initialize audio context and microphone
     */
    async initialize(micId, micVolume) {
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 48000 });

        this.updateListenerOrientation();

        this.micStream = await navigator.mediaDevices.getUserMedia({
            audio: {
                deviceId: { exact: micId },
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true
            }
        });

        const source = this.audioContext.createMediaStreamSource(this.micStream);
        this.micGain = this.audioContext.createGain();
        this.micGain.gain.value = micVolume;
        this.analyser = this.audioContext.createAnalyser();
        this.analyser.fftSize = 256;

        source.connect(this.micGain);
        this.micGain.connect(this.analyser);
    }

    /**
     * Set microphone volume
     */
    setMicVolume(volume) {
        if (this.micGain) {
            this.micGain.gain.value = volume;
        }
    }

    /**
     * Update the AudioListener orientation (always faces -Z, we handle rotation in panner)
     */
    updateListenerOrientation() {
        if (!this.audioContext) return;

        const listener = this.audioContext.listener;

        if (listener.forwardX) {
            listener.forwardX.value = 0;
            listener.forwardY.value = 0;
            listener.forwardZ.value = -1;
            listener.upX.value = 0;
            listener.upY.value = 1;
            listener.upZ.value = 0;
        } else if (listener.setOrientation) {
            listener.setOrientation(0, 0, -1, 0, 1, 0);
        }

        this.updateAllPanners();
    }

    /**
     * Update panner position for a specific user based on relative angle
     */
    updatePannerPosition(panner, user, listenerPosition, serverConfig) {
        const dx = user.x - listenerPosition.x;
        const dy = user.y - listenerPosition.y;
        const dz = user.z - listenerPosition.z;
        const horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (!serverConfig || serverConfig.voiceDimension === '2D') {
            panner.positionX.value = 0;
            panner.positionY.value = 0;
            panner.positionZ.value = -horizontalDist;
            return;
        }

        // Too close - center audio
        if (horizontalDist < 0.1) {
            panner.positionX.value = 0;
            panner.positionY.value = dy;
            panner.positionZ.value = -0.1;
            return;
        }

        // Calculate relative angle
        const angleToSource = Math.atan2(dx, dz);
        let yawDeg = (listenerPosition.yaw || 0) % 360;
        if (yawDeg < 0) yawDeg += 360;
        const listenerYaw = yawDeg * Math.PI / 180;

        let relativeAngle = angleToSource - listenerYaw;
        while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI;
        while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI;

        const leftRight = Math.sin(relativeAngle);
        const frontBack = Math.cos(relativeAngle);

        // Stereo softening
        const maxStereoSeparation = 0.6;
        const sign = leftRight >= 0 ? 1 : -1;
        const softLeftRight = sign * Math.pow(Math.abs(leftRight), 0.7) * maxStereoSeparation;

        panner.positionX.value = softLeftRight * horizontalDist;
        panner.positionY.value = dy;
        panner.positionZ.value = -frontBack * horizontalDist;
    }

    /**
     * Update panner for a specific user
     */
    updatePanner(odapId) {
        const p = this.players.get(odapId);
        const u = this.voiceChat.users.get(odapId);
        if (p && u) {
            this.updatePannerPosition(p.panner, u, this.voiceChat.position, this.voiceChat.serverConfig);
        }
    }

    /**
     * Update all panners
     */
    updateAllPanners() {
        for (const id of this.players.keys()) {
            this.updatePanner(id);
        }
    }

    /**
     * Apply panner settings from server config to a single panner
     */
    applyPannerSettings(panner, serverConfig) {
        const distanceFormula = serverConfig ? serverConfig.distanceFormula : 'LINEAR';
        switch (distanceFormula) {
            case 'LINEAR':
                panner.distanceModel = 'linear';
                break;
            case 'EXPONENTIAL':
                panner.distanceModel = 'exponential';
                break;
            case 'INVERSE_SQUARE':
                panner.distanceModel = 'inverse';
                break;
        }

        panner.refDistance = serverConfig ? serverConfig.refDistance : 1.0;
        panner.maxDistance = serverConfig ? serverConfig.maxDistance : 150;
        panner.rolloffFactor = serverConfig ? serverConfig.rolloffFactor : 1.0;
    }

    /**
     * Update panner settings when server config changes
     */
    updatePannerSettings(serverConfig) {
        if (!serverConfig) return;

        for (const [id, p] of this.players) {
            this.applyPannerSettings(p.panner, serverConfig);

            // Update position based on new config (2D vs 3D)
            const user = this.voiceChat.users.get(id);
            if (user) {
                this.updatePannerPosition(p.panner, user, this.voiceChat.position, serverConfig);
            }
        }
    }

    /**
     * Play received audio from a user
     */
    playAudio(odapId, data, userVolume, masterVolume, serverConfig) {
        let p = this.players.get(odapId);

        if (!p) {
            p = this.createPlayerAudioNodes(odapId, userVolume, masterVolume, serverConfig);
            this.players.set(odapId, p);
        }

        const int16 = new Int16Array(data);
        const float = new Float32Array(int16.length);
        for (let i = 0; i < int16.length; i++) {
            float[i] = (int16[i] / (int16[i] < 0 ? 0x8000 : 0x7FFF)) * 1.2;
        }

        const buffer = this.audioContext.createBuffer(1, float.length, this.audioContext.sampleRate);
        buffer.getChannelData(0).set(float);

        const source = this.audioContext.createBufferSource();
        source.buffer = buffer;
        source.connect(p.panner);

        const now = this.audioContext.currentTime;
        if (!p.nextTime || p.nextTime < now) p.nextTime = now + 0.08;
        source.start(p.nextTime);
        p.nextTime += buffer.duration;
        if (p.nextTime - now > 0.5) p.nextTime = now + 0.08;
    }

    /**
     * Create audio nodes for a player
     */
    createPlayerAudioNodes(odapId, userVolume, masterVolume, serverConfig) {
        const p = {
            gain: this.audioContext.createGain(),
            panner: this.audioContext.createPanner(),
            compressor: this.audioContext.createDynamicsCompressor(),
            nextTime: 0
        };

        p.compressor.threshold.value = -24;
        p.compressor.knee.value = 30;
        p.compressor.ratio.value = 12;

        p.panner.panningModel = 'HRTF';

        this.applyPannerSettings(p.panner, serverConfig);

        const user = this.voiceChat.users.get(odapId);
        if (user) {
            this.updatePannerPosition(p.panner, user, this.voiceChat.position, serverConfig);
        }

        p.gain.gain.value = (userVolume / 100) * masterVolume;

        p.panner.connect(p.compressor);
        p.compressor.connect(p.gain);
        p.gain.connect(this.audioContext.destination);

        return p;
    }

    /**
     * Update volume for a specific player
     */
    setPlayerVolume(odapId, userVolume, masterVolume) {
        const p = this.players.get(odapId);
        if (p) {
            p.gain.gain.value = (userVolume / 100) * masterVolume;
        }
    }

    /**
     * Update all player volumes
     */
    updateAllVolumes(getUserVolume, masterVolume) {
        for (const [id, p] of this.players) {
            p.gain.gain.value = (getUserVolume(id) / 100) * masterVolume;
        }
    }

    /**
     * Remove a player's audio nodes
     */
    removePlayer(odapId) {
        const p = this.players.get(odapId);
        if (p) {
            p.gain.disconnect();
            this.players.delete(odapId);
        }
    }

    /**
     * Cleanup all audio resources
     */
    cleanup() {
        if (this.processor) this.processor.disconnect();
        if (this.micStream) this.micStream.getTracks().forEach(t => t.stop());
        if (this.audioContext) this.audioContext.close();
        this.players.forEach(p => {
            p.panner?.disconnect();
            p.compressor?.disconnect();
            p.gain?.disconnect();
        });
        this.players.clear();
    }
}

window.AudioManager = AudioManager;
