/**
 * Connection Manager - handles WebSocket connection with reconnection logic
 */
class ConnectionManager {
    constructor(voiceChat) {
        this.voiceChat = voiceChat;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectDelay = 1000;
        this.maxReconnectDelay = 30000;
        this.reconnectTimer = null;
    }

    /**
     * Connect to WebSocket server
     */
    connect() {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';

        try {
            this.ws = new WebSocket(`${proto}//${location.host}/voice`);
        } catch (e) {
            console.error('WebSocket creation failed:', e);
            this.scheduleReconnect();
            return;
        }

        this.ws.binaryType = 'arraybuffer';

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;
            this.voiceChat.onConnected();
        };

        this.ws.onmessage = (e) => {
            if (e.data instanceof ArrayBuffer) {
                this.voiceChat.onAudioReceived(e.data);
            } else {
                this.voiceChat.onMessageReceived(JSON.parse(e.data));
            }
        };

        this.ws.onclose = (event) => {
            console.log('WebSocket closed:', event.code, event.reason);
            this.handleDisconnect();
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.voiceChat.onConnectionError();
        };
    }

    /**
     * Handle disconnection
     */
    handleDisconnect() {
        const isOnLoginScreen = !document.getElementById('loginScreen').classList.contains('hidden');

        if (isOnLoginScreen) {
            this.scheduleReconnect();
        } else {
            this.voiceChat.onDisconnected();
        }
    }

    /**
     * Schedule a reconnection attempt
     */
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            this.voiceChat.onReconnectFailed();
            return;
        }

        this.reconnectAttempts++;
        const delay = Math.min(
            this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1),
            this.maxReconnectDelay
        );

        this.voiceChat.onReconnecting(this.reconnectAttempts, this.maxReconnectAttempts, delay);

        this.reconnectTimer = setTimeout(() => {
            this.connect();
        }, delay);
    }

    /**
     * Send JSON data
     */
    send(data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(data));
        }
    }

    /**
     * Send binary data
     */
    sendBinary(data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(data);
        }
    }

    /**
     * Check if connected
     */
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}

window.ConnectionManager = ConnectionManager;
