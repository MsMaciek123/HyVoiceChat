# HyVoiceChat

Proximity voice chat mod for Hytale servers. Players connect through a web browser and can hear each other based on in-game distance and position. \
Download on CurseForge: https://www.curseforge.com/hytale/mods/hyvoicechat

## How it works

1. Players open the voice chat website (hosted by the mod)
2. They get a verification code to type in-game (`/voicechat CODE`)
3. After verification, they can join voice chat
4. Audio is spatialized - players hear others from the direction they're standing, with volume based on distance

## Setup

The mod runs an embedded web server. By default it uses HTTPS on port 8443. \
You can either use serveo tunnel (built-in) and just join your server, click link in chat and talk. \
The second way is you can self-host it and disable tunnel in config. That way you get less ping and more direct connection. \
However, with self-hosting you most likely want to have some kind of domain to avoid SSL warnings.

### Self-hosting without domain
Browsers require HTTPS to access the microphone. The mod includes a self-signed certificate that works out of the box — you will need to accept the browser warning (“Your connection is not private” / “Accept the risk”). \
This is because SSL certificates are mostly issued for domains, not IPs.

⚠️ Important notice:
Accepting an invalid certificate is insecure. It allows man-in-the-middle (MITM) attacks, meaning an attacker on the network could intercept traffic and listen to microphone audio. \
This setup is acceptable only for testing or private environments on trusted networks. \
For production use, you should use:
- Put a reverse proxy (nginx, caddy) in front with a real certificate
- Use Cloudflare tunnel
- Generate your own keystore with a proper certificate

By accepting the invalid certificate, you acknowledge that your microphone audio may be intercepted.

## Configuration

Config file: `HyVoiceChat.json`

The configuration is organized into categories:

### Server Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `Server.WebSocketPort` | `8443` | Port for the web server |
| `Server.UseSSL` | `true` | Enable HTTPS (required for microphone access) |
| `Server.SSLKeystorePath` | `example.keystore` | Path to Java keystore file |
| `Server.SSLKeystorePassword` | `changeit` | Keystore password |

### Messages Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `Messages.JoinMessage` | `"This server has Voice Chat!..."` | Message shown to players on join |

### Audio Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `Audio.MaxDistance` | `75.0` | Maximum distance (blocks) to hear other players |
| `Audio.DistanceFormula` | `EXPONENTIAL` | How volume drops with distance: `LINEAR`, `EXPONENTIAL`, `INVERSE_SQUARE` |
| `Audio.VoiceDimension` | `THREE_D` | `THREE_D` for spatial audio, `TWO_D` for flat (no left/right) |
| `Audio.RolloffFactor` | `1.5` | How quickly volume decreases with distance |
| `Audio.RefDistance` | `10.0` | Distance at which volume is 100% |
| `Audio.ServerCutoffMultiplier` | `1.1` | Server stops sending audio beyond MaxDistance × this value |
| `Audio.Blend2dDistance` | `20.0` | Distance below which audio is more centered |
| `Audio.Full3dDistance` | `50.0` | Distance at which full 3D positioning kicks in |

### General Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `General.UpdateIntervalMs` | `50` | How often player positions are updated (milliseconds) |
| `General.OverrideNameplates` | `true` | Show speaking indicator on player nameplates |
| `General.RequirePermissionToConnect` | `false` | Require permission to use voice chat |
| `General.EnableUI` | `true` | Show in-game UI for nearby speaking players |

### Tunnel Settings

| Setting                              | Default | Description                  |
|--------------------------------------|---------|------------------------------|
| `Tunnel.UseTunnel`                   | `true`  | Whether to use serveo tunnel |

## Commands

| Command | Description |
|---------|-------------|
| `/voicechat <code>` | Verify your voice chat connection (aliases: `/vc`, `/voice`) |
| `/voicechat-reload` | Reload config (console only) |

## Usage

1. Start the server with the mod installed
2. Open `https://your-server-ip:8443` in a browser
3. Accept the SSL warning if using the default certificate
4. Copy the verification command shown on the page
5. Paste it in game chat
6. Select your microphone and click "Join Voice Chat"
