# HyVoiceChat

Proximity voice chat mod for Hytale servers. Players connect through a web browser and can hear each other based on in-game distance and position.
Download on CurseForge: https://www.curseforge.com/hytale/mods/hyvoicechat
Note: This mod requires additional port for website/websocket

## How it works

1. Players open the voice chat website (hosted by the mod)
2. They get a verification code to type in-game (`/voicechat CODE`)
3. After verification, they can join voice chat
4. Audio is spatialized - players hear others from the direction they're standing, with volume based on distance

## Setup

The mod runs an embedded web server. By default it uses HTTPS on port 8443.

### Why SSL?

Browsers require HTTPS to access the microphone. The mod includes a self-signed certificate that works out of the box - you'll just need to accept the browser warning ("Your connection is not private" / "Accept the risk").

For production use, you can:
- Put a reverse proxy (nginx, caddy) in front with a real certificate
- Use Cloudflare tunnel
- Generate your own keystore with a proper certificate

## Configuration

Config file: `WebVoiceChat.json`

| Setting | Default            | Description |
|---------|--------------------|-------------|
| `WebSocketPort` | `8443`             | Port for the web server |
| `UseSSL` | `true`             | Enable HTTPS (required for microphone access) |
| `SSLKeystorePath` | `example.keystore` | Path to Java keystore file |
| `SSLKeystorePassword` | `changeit`         | Keystore password |
| `MaxDistance` | `75.0`             | Maximum distance (blocks) to hear other players |
| `DistanceFormula` | `LINEAR`           | How volume drops with distance: `LINEAR`, `EXPONENTIAL`, `INVERSE_SQUARE` |
| `VoiceDimension` | `3D`               | `3D` for spatial audio, `2D` for flat (no left/right) |
| `RolloffFactor` | `1.0`              | How quickly volume decreases with distance |
| `RefDistance` | `1.0`              | Distance at which volume is 100% |
| `ServerCutoffMultiplier` | `1.1`              | Server stops sending audio beyond MaxDistance * this value |
| `Blend2dDistance` | `10.0`             | Distance below which audio is more centered |
| `Full3dDistance` | `30.0`             | Distance at which full 3D positioning kicks in |

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
