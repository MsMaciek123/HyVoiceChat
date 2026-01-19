/**
 * UI Manager - handles all UI updates and rendering
 */
class UIManager {
    constructor(voiceChat) {
        this.voiceChat = voiceChat;
    }

    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Update connection status display
     */
    setConnectionStatus(text, className) {
        const status = document.getElementById('connectStatus');
        status.textContent = text;
        status.className = 'status-text ' + className;
    }

    /**
     * Update voice chat status (connected/disconnected)
     */
    setVoiceStatus(connected, userName) {
        const dot = document.getElementById('statusDot');
        const text = document.getElementById('statusText');
        dot.classList.toggle('connected', connected);
        text.textContent = connected ? `Connected as ${userName}` : 'Disconnected';
    }

    /**
     * Update ping display
     */
    setPing(ms) {
        document.getElementById('pingDisplay').textContent = ms + 'ms';
    }

    /**
     * Update microphone meter
     */
    setMicMeter(percentage) {
        document.getElementById('micMeter').style.width = percentage + '%';
    }

    /**
     * Show main chat view, hide login
     */
    showMainView() {
        document.getElementById('loginScreen').classList.add('hidden');
        document.getElementById('mainContainer').classList.remove('hidden');
    }

    /**
     * Display verification code
     */
    setVerificationCode(command) {
        const codeBox = document.getElementById('verificationCommand');
        codeBox.textContent = command;

        // Add click to copy
        codeBox.onclick = () => {
            navigator.clipboard.writeText(command).then(() => {
                codeBox.classList.add('copied');
                codeBox.textContent = 'Copied!';
                setTimeout(() => {
                    codeBox.classList.remove('copied');
                    codeBox.textContent = command;
                }, 1500);
            });
        };
    }

    /**
     * Update verification status
     */
    setVerificationStatus(verified, username) {
        const status = document.getElementById('verificationStatus');

        if (verified) {
            status.textContent = `✓ Verified as ${username}`;
            status.className = 'help-text verification-success';
        } else {
            status.textContent = 'Waiting for verification...';
            status.className = 'help-text verification-pending';
        }

        this.updateJoinButton(verified);
    }

    /**
     * Update join button state
     */
    updateJoinButton(verified = false) {
        const joinBtn = document.getElementById('joinBtn');
        const micId = this.voiceChat.settings.get('micId');
        joinBtn.disabled = !(verified && micId);
    }

    /**
     * Update mute button state
     */
    setMuteState(muted) {
        const btn = document.getElementById('muteBtn');
        btn.classList.toggle('active', muted);
        btn.textContent = muted ? 'Unmute' : 'Mute';
    }

    /**
     * Update deafen button state
     */
    setDeafenState(deafened) {
        const btn = document.getElementById('deafenBtn');
        btn.classList.toggle('active', deafened);
        btn.textContent = deafened ? 'Undeafen' : 'Deafen';
    }

    /**
     * Render the users list
     */
    renderUsers(users, selfId, calculateDistance, getUserVolume) {
        const container = document.getElementById('usersList');

        if (!users.size) {
            container.innerHTML = '<p class="empty-state">No users connected</p>';
            return;
        }

        const sorted = [...users.values()].sort((a, b) => {
            if (a.id === selfId) return -1;
            if (b.id === selfId) return 1;
            return a.name.localeCompare(b.name);
        });

        container.innerHTML = sorted.map(u => {
            const isSelf = u.id === selfId;
            const vol = getUserVolume(u.id);
            const distance = isSelf ? 0 : calculateDistance(u);
            const distanceText = isSelf ? '' : ` (${distance.toFixed(1)}m)`;

            return `
                <div class="user-item ${u.speaking ? 'speaking' : ''} ${isSelf ? 'self' : ''}">
                    <div class="user-info">
                        <div class="user-name">${this.escapeHtml(u.name)}${isSelf ? ' <span class="tag">(you)</span>' : ''}${distanceText}</div>
                        <div class="user-pos">${u.x.toFixed(1)}, ${u.y.toFixed(1)}, ${u.z.toFixed(1)}</div>
                    </div>
                    ${!isSelf ? `<div class="user-volume"><input type="range" min="0" max="200" value="${vol}" oninput="chat.setUserVolume(${u.id}, this.value)"></div>` : ''}
                </div>`;
        }).join('');
    }
}

window.UIManager = UIManager;
