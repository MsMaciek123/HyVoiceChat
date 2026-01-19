class VoiceChat {
    constructor() {
        this.odapId = null;
        this.userName = null;
        this.users = new Map();
        this.position = { x: 0, y: 0, z: 0, yaw: 0 };
        this.muted = false;
        this.deafened = false;
        this.speaking = false;
        this.verified = false;
        this.verificationCheckInterval = null;

        this.serverConfig = null;

        // Initialize managers
        this.settings = new SettingsManager();
        this.audio = new AudioManager(this);
        this.connection = new ConnectionManager(this);
        this.ui = new UIManager(this);

        this.init();
    }

    async init() {
        await this.loadMicrophones();
        this.bindEvents();
        this.settings.applyToUI();
    }

    async loadMicrophones() {
        try {
            await navigator.mediaDevices.getUserMedia({ audio: true });
            const devices = await navigator.mediaDevices.enumerateDevices();
            const mics = devices.filter(d => d.kind === 'audioinput');
            const select = document.getElementById('micSelect');

            select.innerHTML = mics.map((m, i) =>
                `<option value="${m.deviceId}">${m.label || 'Microphone ' + (i + 1)}</option>`
            ).join('');

            if (mics.length && !this.settings.get('micId')) {
                this.settings.set('micId', mics[0].deviceId);
            }
        } catch (e) {
            document.getElementById('micSelect').innerHTML = '<option>Microphone access denied</option>';
        }
    }

    bindEvents() {
        const $ = id => document.getElementById(id);

        $('micSelect').onchange = (e) => {
            this.settings.set('micId', e.target.value);
            this.ui.updateJoinButton(this.verified);
        };

        $('joinBtn').onclick = () => {
            if (this.verified && this.settings.get('micId')) {
                this.connection.send({ type: 'join' });
            }
        };

        $('micVolume').oninput = (e) => {
            const vol = e.target.value / 100;
            this.settings.set('micVolume', vol);
            $('micVolumeValue').textContent = e.target.value + '%';
            this.audio.setMicVolume(vol);
        };

        $('outputVolume').oninput = (e) => {
            const vol = e.target.value / 100;
            this.settings.set('masterVolume', vol);
            $('outputVolumeValue').textContent = e.target.value + '%';
            this.updateAllVolumes();
        };

        $('thresholdSlider').oninput = (e) => {
            const threshold = parseInt(e.target.value);
            this.settings.set('threshold', threshold);
            $('thresholdValue').textContent = threshold + '%';
            $('meterThreshold').style.left = threshold + '%';
        };

        $('muteBtn').onclick = () => this.toggleMute();
        $('deafenBtn').onclick = () => this.toggleDeafen();
    }

    // Connection callbacks
    onConnected() {
        // Don't show the status message if user is already verified and joined
        if (!this.verified || document.getElementById('loginScreen').classList.contains('hidden')) {
            this.ui.setConnectionStatus('Connected! Verify in-game to continue.', 'connected');
        }

        // Start checking verification status every 2 seconds
        this.startVerificationCheck();
    }

    onDisconnected() {
        this.ui.setConnectionStatus('Disconnected from server', 'disconnected');
        this.ui.setVoiceStatus(false, '');
        this.audio.cleanup();
        this.stopVerificationCheck();
        this.verified = false;

        // Reset UI to login screen
        document.getElementById('loginScreen').classList.remove('hidden');
        document.getElementById('mainContainer').classList.add('hidden');
    }

    onConnectionError() {
        this.ui.setConnectionStatus('Connection error', 'error');
    }

    onReconnecting(attempt, max, delay) {
        this.ui.setConnectionStatus(
            `Reconnecting in ${Math.round(delay / 1000)}s... (attempt ${attempt}/${max})`,
            'disconnected'
        );
    }

    onReconnectFailed() {
        this.ui.setConnectionStatus('Failed to connect. Click refresh to retry.', 'error');
    }

    startVerificationCheck() {
        if (this.verificationCheckInterval) return;

        this.verificationCheckInterval = setInterval(() => {
            if (!this.verified && this.connection.isConnected()) {
                this.connection.send({ type: 'check_verification' });
            }
        }, 2000);
    }

    stopVerificationCheck() {
        if (this.verificationCheckInterval) {
            clearInterval(this.verificationCheckInterval);
            this.verificationCheckInterval = null;
        }
    }

    onMessageReceived(msg) {
        switch (msg.type) {
            case 'id':
                this.odapId = msg.id;
                break;

            case 'config':
                this.serverConfig = msg;
                console.log('Server config received/updated:', this.serverConfig);
                this.audio.updatePannerSettings(this.serverConfig);
                break;

            case 'verification_code':
                this.ui.setVerificationCode(msg.command);
                break;

            case 'verification_status':
                this.verified = msg.verified;
                this.userName = msg.username || null;
                this.ui.setVerificationStatus(msg.verified, msg.username);
                if (msg.verified) {
                    this.stopVerificationCheck();
                }
                break;

            case 'join_error':
                alert('Join error: ' + msg.error);
                break;

            case 'kicked':
                alert('Disconnected: ' + (msg.reason || 'You have been kicked.'));
                this.onDisconnected();
                break;

            case 'join_success':
                this.handleJoinSuccess(msg);
                break;

            case 'players_snapshot':
                this.handlePlayersSnapshot(msg);
                break;

            case 'pong':
                this.ui.setPing(Date.now() - msg.timestamp);
                break;
        }
    }

    onAudioReceived(data) {
        if (this.deafened) return;

        const view = new DataView(data);
        const senderId = view.getInt32(0);
        if (senderId === this.odapId) return;

        // Mark sender as speaking (will be cleared by timeout)
        this.markUserSpeaking(senderId);

        const userVol = this.settings.getUserVolume(senderId);
        const masterVol = this.settings.get('masterVolume');
        this.audio.playAudio(senderId, data.slice(4), userVol, masterVol, this.serverConfig);
    }

    markUserSpeaking(userId) {
        const user = this.users.get(userId);
        if (user) {
            user.speaking = true;
            user.speakingTimeout && clearTimeout(user.speakingTimeout);
            user.speakingTimeout = setTimeout(() => {
                user.speaking = false;
                this.renderUsers();
            }, 300); // Clear speaking after 300ms of no audio
            this.renderUsers();
        }
    }

    handleJoinSuccess(msg) {
        this.odapId = msg.id;
        this.userName = msg.name;
        this.ui.showMainView();
        this.ui.setVoiceStatus(true, this.userName);
        this.startPing();
        this.startAudio();
    }

    handlePlayersSnapshot(msg) {
        // Update self position
        const self = msg.self;
        this.odapId = self.id;
        this.position = { x: self.x, y: self.y, z: self.z, yaw: self.yaw };

        // Build new users map (preserve speaking state from audio)
        const newUserIds = new Set();

        // Add self
        newUserIds.add(self.id);
        const existingSelf = this.users.get(self.id);
        this.users.set(self.id, {
            id: self.id,
            name: self.name,
            x: self.x,
            y: self.y,
            z: self.z,
            yaw: self.yaw,
            speaking: existingSelf?.speaking || false,
            speakingTimeout: existingSelf?.speakingTimeout
        });

        // Add nearby players
        for (const player of msg.players) {
            newUserIds.add(player.id);
            const existing = this.users.get(player.id);
            this.users.set(player.id, {
                id: player.id,
                name: player.name,
                x: player.x,
                y: player.y,
                z: player.z,
                yaw: player.yaw,
                speaking: existing?.speaking || false,
                speakingTimeout: existing?.speakingTimeout
            });
        }

        // Remove players no longer in range
        for (const [id, user] of this.users) {
            if (!newUserIds.has(id)) {
                user.speakingTimeout && clearTimeout(user.speakingTimeout);
                this.users.delete(id);
                this.audio.removePlayer(id);
            }
        }

        // Update audio and UI
        this.audio.updateListenerOrientation();
        this.audio.updateAllPanners();
        this.renderUsers();
    }

    async startAudio() {
        try {
            await this.audio.initialize(
                this.settings.get('micId'),
                this.settings.get('micVolume')
            );
            this.startMicMeter();
            this.startCapture();
        } catch (e) {
            alert('Audio error: ' + e.message);
        }
    }

    startMicMeter() {
        const data = new Uint8Array(this.audio.analyser.frequencyBinCount);
        const update = () => {
            if (!this.audio.analyser) return;

            this.audio.analyser.getByteFrequencyData(data);
            const avg = data.reduce((a, b) => a + b, 0) / data.length;
            const pct = Math.min(100, (avg / 128) * 100);
            this.ui.setMicMeter(pct);

            // Track local speaking state for UI (green indicator for self)
            const threshold = this.settings.get('threshold');
            const isSpeaking = pct > threshold && !this.muted;
            const self = this.users.get(this.odapId);
            if (self && self.speaking !== isSpeaking) {
                self.speaking = isSpeaking;
                this.renderUsers();
            }

            requestAnimationFrame(update);
        };
        update();
    }

    startCapture() {
        // Use 8192 buffer size for higher quality audio (double the previous 4096)
        // This results in ~170ms chunks at 48kHz, providing better audio fidelity
        const processor = this.audio.audioContext.createScriptProcessor(8192, 1, 1);
        const threshold = this.settings.get('threshold');

        processor.onaudioprocess = (e) => {
            if (this.muted || !this.connection.isConnected()) return;

            const input = e.inputBuffer.getChannelData(0);
            let sum = 0;
            for (let i = 0; i < input.length; i++) sum += input[i] * input[i];
            if (Math.sqrt(sum / input.length) < (threshold / 100) * 0.15) return;

            const out = new Int16Array(input.length);
            for (let i = 0; i < input.length; i++) {
                const s = Math.tanh(input[i] * 1.5);
                out[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
            }

            this.connection.sendBinary(out.buffer);
        };

        this.audio.micGain.connect(processor);
        processor.connect(this.audio.audioContext.destination);
        this.audio.processor = processor;
    }

    renderUsers() {
        this.ui.renderUsers(
            this.users,
            this.odapId,
            (user) => this.calculateDistance(user),
            (id) => this.settings.getUserVolume(id)
        );
    }

    calculateDistance(user) {
        const dx = this.position.x - user.x;
        const dy = this.position.y - user.y;
        const dz = this.position.z - user.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    setUserVolume(odapId, volume) {
        this.settings.setUserVolume(odapId, parseInt(volume));
        this.audio.setPlayerVolume(odapId, volume, this.settings.get('masterVolume'));
    }

    updateAllVolumes() {
        this.audio.updateAllVolumes(
            (id) => this.settings.getUserVolume(id),
            this.settings.get('masterVolume')
        );
    }

    toggleMute() {
        this.muted = !this.muted;
        this.ui.setMuteState(this.muted);

        if (this.audio.micStream) {
            this.audio.micStream.getAudioTracks().forEach(t => t.enabled = !this.muted);
        }

        if (!this.muted && this.deafened) {
            // Unmuting - also undeafen
            this.deafened = false;
            this.ui.setDeafenState(false);
        }
    }

    toggleDeafen() {
        this.deafened = !this.deafened;
        this.ui.setDeafenState(this.deafened);

        if (this.deafened && !this.muted) {
            // Deafening - also mute
            this.muted = true;
            this.ui.setMuteState(true);
            if (this.audio.micStream) {
                this.audio.micStream.getAudioTracks().forEach(t => t.enabled = false);
            }
        }
    }

    startPing() {
        setInterval(() => {
            if (this.connection.isConnected()) {
                this.connection.send({ type: 'ping', timestamp: Date.now() });
            }
        }, 2000);
    }
}

const chat = new VoiceChat();

window.addEventListener('load', () => {
    chat.connection.connect();
});
