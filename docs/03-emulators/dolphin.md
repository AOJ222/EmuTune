# Dolphin

Dolphin is the first real integration target because it is mature with documented Android behaviour. See the [Dolphin integration spike](../08-research/spikes/dolphin-integration.md) for the full sourced findings.

## Verdict summary

| Capability | Verdict |
| --- | --- |
| `INSTALLATION_DETECTION` | Supported (package `org.dolphinemu.dolphinemu`) |
| `GAME_LAUNCH` | Supported, constrained — `dolphinemu://app/play/<channelId>/<gameId>` for games already in Dolphin's library |
| `GAME_DETECTION` | Not supported (no exported game-list surface) |
| `CONFIG_READ` / `CONFIG_WRITE` / `PER_GAME_CONFIG` | Not supported without root (config in app-private storage) |
| `CONFIG_IMPORT` / `CONFIG_EXPORT` | Not invocable programmatically (activities not exported) |
| `GUIDED_CONFIG` | The honest lever — guide the user to configure inside Dolphin |

## Consequence

The `DolphinAdapter` claims only `INSTALLATION_DETECTION`, `GAME_LAUNCH` and `GUIDED_CONFIG`. `CONFIG_WRITE` is false, so the UI must not offer automatic modification for Dolphin.
