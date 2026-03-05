# McGPT — ChatGPT Integration for Minecraft Paper Servers

A production-ready Minecraft Paper plugin that integrates ChatGPT via the OpenAI API, letting players interact with AI directly from the Minecraft chat.

## Features

- **Chat trigger**: Players type `!ai <question>` in chat to query ChatGPT
- **`/ai` command**: `/ai <message>` works the same as the chat trigger; `/ai reload` for admins
- **Broadcast or private**: Configurable — broadcast AI responses to all players or only to the requester
- **Per-player cooldown** + optional **global cooldown** to prevent spam
- **Request stacking prevention**: Reject or queue duplicate requests while one is in flight
- **Chat context**: Optionally include recent chat history for richer AI responses
- **Async HTTP**: All OpenAI calls are non-blocking — the server thread is never stalled
- **Configurable AI prefix**: `[AI]` with color code support
- **Secure API key loading**: Environment variable first, config fallback
- **Graceful error handling**: User-friendly error messages + detailed console logs
- **Sanitization**: Strips color codes from player input before sending to OpenAI

## Requirements

- Paper 1.21.x (requires Java 21)
- An OpenAI API key

## Building

```bash
./gradlew build
```

The compiled JAR will be at `build/libs/mc-gpt-1.0.0.jar`.

## Installation

1. Copy `build/libs/mc-gpt-1.0.0.jar` to your server's `plugins/` folder.
2. Set your OpenAI API key (see below).
3. Restart or reload the server.

## API Key Setup

**Recommended — Environment Variable (most secure):**

```bash
export OPENAI_API_KEY=sk-...
```

**Alternative — Config file:**

Edit `plugins/McGPT/config.yml` and set:

```yaml
openaiApiKey: "sk-..."
```

If no API key is found on startup, the plugin will log an error and disable itself.

## Configuration

After first run, edit `plugins/McGPT/config.yml`:

```yaml
# Trigger keyword in chat
triggerKeyword: "!ai"

# Broadcast AI response to all players (false = only to requester)
broadcastToAll: true

# Prefix for AI responses (&-color codes supported)
aiPrefix: "&b[AI]&f "

# OpenAI model
model: "gpt-4o-mini"

# Temperature (0.0–2.0)
temperature: 0.7

# Max tokens per response
maxTokens: 200

# Per-player cooldown (seconds)
cooldownSeconds: 10

# Global cooldown in seconds (0 = disabled)
globalCooldownSeconds: 0

# HTTP timeout (seconds)
timeoutSeconds: 20

# Include recent chat as context
includeChatContext: false
contextMessageCount: 10

# API key source and value
apiKeySource: "ENV_OR_CONFIG"
openaiApiKey: ""

# System prompt (sets AI personality)
systemPrompt: "You are a helpful Minecraft assistant. Keep responses concise and relevant to Minecraft."

# Log requests/responses to console
logRequests: false

# REJECT or QUEUE duplicate requests
requestStackingMode: "REJECT"

# Max reply characters (longer responses are truncated)
maxReplyLength: 256
```

Reload the config in-game without restarting:

```
/ai reload
```

## Usage

| Action | Example |
|--------|---------|
| Ask AI via chat | `!ai How do I craft a beacon?` |
| Ask AI via command | `/ai How do I craft a beacon?` |
| Reload config | `/ai reload` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `chatgpt.use` | Can use `!ai` in chat and `/ai` command | `true` (all players) |
| `chatgpt.admin` | Can run `/ai reload` | `op` |

## Troubleshooting

**Plugin doesn't enable / "No OpenAI API key configured"**
- Make sure `OPENAI_API_KEY` is set in your environment, **or** `openaiApiKey` is set in `config.yml`.

**"Sorry, the AI is currently unavailable."**
- Check console for the full error. Common causes: invalid API key, rate limit hit, network issue.
- Enable `logRequests: true` in config for detailed request/response logging.

**Chat messages with `!ai` still appear in chat**
- Ensure no other plugin is intercepting `AsyncChatEvent` at a higher priority before McGPT.

**Players get "Please wait for the current response."**
- Set `requestStackingMode: "QUEUE"` to queue requests instead of rejecting them.

**Response is cut off**
- Increase `maxTokens` and/or `maxReplyLength` in config.
