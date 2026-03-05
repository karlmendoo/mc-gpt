# McGPT — ChatGPT Integration for Minecraft Paper Servers

A Minecraft Paper plugin that integrates [OpenAI ChatGPT](https://openai.com/) into your server's chat. Players can ask the AI questions directly in chat or via the `/ai` command.

---

## Features

- **Chat trigger**: Players type `!ai <message>` in chat to query the AI.
- **Command**: `/ai <message>` works the same way.
- **Config reload**: `/ai reload` reloads configuration without a server restart.
- **Broadcast or whisper**: Replies can be broadcast to all players or sent only to the requester.
- **Per-player & global cooldowns**: Prevent spam.
- **Pending request guard**: Prevents request stacking per player.
- **Optional chat context**: Include recent chat messages as context for the AI.
- **Color-coded prefix**: Configurable via `&` codes (e.g., `&b[AI]&f `).
- **Safe API key handling**: Reads from environment variable `OPENAI_API_KEY` first, then `config.yml`.

---

## Requirements

- **Paper** 1.21.4+
- **Java** 21+

---

## Build Instructions

```bash
./gradlew shadowJar
```

The output JAR will be in `build/libs/mc-gpt-1.0.0.jar`.

---

## Installation

1. Build the plugin as above, or download a release JAR.
2. Copy `mc-gpt-1.0.0.jar` into your server's `plugins/` folder.
3. Set your OpenAI API key (see below).
4. Start or restart the server.

---

## API Key Setup

**Recommended — Environment Variable:**

```bash
export OPENAI_API_KEY=sk-...
```

Start your server after setting the variable.

**Fallback — config.yml:**

```yaml
openaiApiKey: "sk-..."
```

> If no API key is found, the plugin will disable itself and print a warning to the console.

---

## Configuration Reference

After the first launch, edit `plugins/McGPT/config.yml`:

| Key | Default | Description |
|-----|---------|-------------|
| `triggerKeyword` | `!ai` | Chat prefix that triggers the AI |
| `broadcastToAll` | `true` | Broadcast reply to all players |
| `aiPrefix` | `&b[AI]&f ` | Prefix before AI replies (supports `&` color codes) |
| `model` | `gpt-4o-mini` | OpenAI model to use |
| `temperature` | `0.7` | Creativity of responses (0.0–2.0) |
| `maxTokens` | `200` | Maximum tokens per response |
| `cooldownSeconds` | `10` | Per-player cooldown between requests |
| `globalCooldownSeconds` | `0` | Global cooldown (0 = disabled) |
| `timeoutSeconds` | `20` | HTTP request timeout |
| `includeChatContext` | `false` | Include recent chat as context |
| `contextMessageCount` | `10` | Number of recent messages to include |
| `apiKeySource` | `ENV_OR_CONFIG` | Key source: check env var first, then config |
| `openaiApiKey` | `` | API key (if not using env var) |
| `systemPrompt` | *(Minecraft assistant)* | Personality prompt for the AI |
| `enableLogging` | `false` | Log requests/responses to console |
| `maxReplyLength` | `500` | Max characters in AI reply (truncated if longer) |

---

## Usage

### Chat Trigger

```
!ai What is the best strategy for fighting the Ender Dragon?
```

### Command

```
/ai What enchantments should I put on my sword?
```

### Reload Config

```
/ai reload
```

*(Requires `chatgpt.admin` permission)*

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `chatgpt.use` | `true` (all players) | Use the AI trigger and `/ai` command |
| `chatgpt.admin` | `op` | Reload config with `/ai reload` |

---

## Troubleshooting

**Plugin disabled on startup:**
- Check console for `[McGPT] No OpenAI API key found!`
- Set `OPENAI_API_KEY` environment variable or `openaiApiKey` in `config.yml`.

**"AI is unavailable right now" in chat:**
- Check console for the detailed error (usually an HTTP error or timeout).
- Verify your API key is valid and has credits.
- Check `timeoutSeconds` in config if requests time out.

**Players can't use the trigger:**
- Ensure the player has the `chatgpt.use` permission.
- Check `triggerKeyword` in config matches what players are typing.

**Responses are cut off:**
- Increase `maxTokens` and/or `maxReplyLength` in config.