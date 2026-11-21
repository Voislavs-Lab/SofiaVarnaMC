# GameModeSwitch

Survival and spectator for players who are not op, by every route a player has:

| Route | Does |
| --- | --- |
| `/spectator`, `/spec` | Switch yourself to spectator |
| `/survival`, `/surv` | Switch yourself to survival |
| `/gamemode survival\|spectator [@s\|<your name>]` | Vanilla command, limited to those two modes on yourself |
| F3+N, F3+F4 | Client game mode keys. Creative and adventure are refused |

## Permissions

| Node | Default | Gives |
| --- | --- | --- |
| `gamemodeswitch.use` | false | `/spec`, `/surv`, and the client op level for F3+N / F3+F4 |
| `gamemodeswitch.unrestricted` | op | Skip every limit above |

The `/gamemode` command and the F3 keys also need the vanilla node `minecraft.command.gamemode`. On its own, that node allows any mode on any player. This plugin is what narrows it, so do not grant it without this plugin enabled.

## How F3+N works without op

The client enables F3+N and F3+F4 only when the server has told it that it has op level 2. For a player with `gamemodeswitch.use` and `minecraft.command.gamemode` who is not op, the plugin sends that level with Paper's `Player#sendOpLevel`. It re-sends on join, respawn, world change, and one tick after any `op`/`deop` command, because the server overwrites it with the real level at those points.

The level the client sees changes nothing on the server. Paper accepts the switcher packet because of `minecraft.command.gamemode`, and fires `PlayerGameModeChangeEvent` with cause `GAMEMODE_SWITCHER`. The plugin cancels that event for any mode other than survival or spectator. Every other op check, such as commands, F3+I data queries, and command blocks, still uses the real op status.

## Build

With JDK 25, from this directory:

```
gradle build
```

The jar is `build/libs/gamemode-switch-1.1.0.jar`. Copy it to `/opt/minecraft/plugins/` and restart.
