# GameModeSwitch

Lets a player switch themselves between survival and spectator without being op.

| Command | Does |
| --- | --- |
| `/spectator`, `/spec` | Switch yourself to spectator |
| `/survival`, `/surv` | Switch yourself to survival |

Neither command takes a target, so a player cannot change someone else. There is no creative or adventure.

Permission `gamemodeswitch.use` defaults to **false**. LuckPerms grants it to the `default` group (see `infra/luckperms/`). Ops do not get it implicitly, because they already have vanilla `/gamemode`.

Players get this instead of vanilla `/gamemode`. Bukkit has one node for that command (`minecraft.command.gamemode`), and whoever holds it can pick any mode for any player.

## Build

With JDK 25, from this directory:

```
gradle build
```

The jar is `build/libs/gamemode-switch-1.0.0.jar`. Copy it to `/opt/minecraft/plugins/` and restart.
