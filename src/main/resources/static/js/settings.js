/**
 * Settings Manager - handles loading/saving user preferences
 * Uses localStorage for persistent storage with automatic serialization
 */
class SettingsManager {
    constructor() {
        this.storageKey = 'voicechat_settings';
        this.defaults = {
            micVolume: 1.0,
            masterVolume: 1.0,
            threshold: 5,
            micId: null,
            userVolumes: {}
        };
        this.settings = { ...this.defaults };
        this.load();
    }

    /**
     * Load settings from localStorage
     */
    load() {
        try {
            const stored = localStorage.getItem(this.storageKey);
            if (stored) {
                const parsed = JSON.parse(stored);
                this.settings = { ...this.defaults, ...parsed };
            }
        } catch (e) {
            console.warn('Failed to load settings:', e);
            this.settings = { ...this.defaults };
        }
    }

    /**
     * Save current settings to localStorage
     */
    save() {
        try {
            localStorage.setItem(this.storageKey, JSON.stringify(this.settings));
        } catch (e) {
            console.warn('Failed to save settings:', e);
        }
    }

    /**
     * Get a setting value
     */
    get(key) {
        return this.settings[key];
    }

    /**
     * Set a setting value and auto-save
     */
    set(key, value) {
        this.settings[key] = value;
        this.save();
    }

    /**
     * Get user volume for a specific user
     */
    getUserVolume(odapId) {
        return this.settings.userVolumes[odapId] || 100;
    }

    /**
     * Set user volume for a specific user
     */
    setUserVolume(odapId, volume) {
        this.settings.userVolumes[odapId] = volume;
        this.save();
    }

    /**
     * Apply loaded settings to UI elements
     */
    applyToUI() {
        const $ = id => document.getElementById(id);

        // Mic volume
        const micVol = Math.round(this.settings.micVolume * 100);
        $('micVolume').value = micVol;
        $('micVolumeValue').textContent = micVol + '%';

        // Master volume
        const masterVol = Math.round(this.settings.masterVolume * 100);
        $('outputVolume').value = masterVol;
        $('outputVolumeValue').textContent = masterVol + '%';

        // Threshold
        $('thresholdSlider').value = this.settings.threshold;
        $('thresholdValue').textContent = this.settings.threshold + '%';
        $('meterThreshold').style.left = this.settings.threshold + '%';

        // Mic selection
        if (this.settings.micId) {
            const micSelect = $('micSelect');
            for (let opt of micSelect.options) {
                if (opt.value === this.settings.micId) {
                    micSelect.value = this.settings.micId;
                    break;
                }
            }
        }
    }
}

window.SettingsManager = SettingsManager;
