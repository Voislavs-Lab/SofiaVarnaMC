# SharedEnderChest

Paper 26.3 plugin. Each player chooses what an ender chest block opens for them: their own private chest (vanilla) or one shared chest that every player can use.

| Command | Who | Effect |
| --- | --- | --- |
| `/ec` | everyone | Show your mode |
| `/ec shared` | everyone | Ender chests open the shared chest |
| `/ec private` | everyone | Ender chests open your private chest (default) |
| `/ec save` | op, console | Write the shared chest to disk now |

`/echest` is the full name; `/ec` is an alias. Nothing opens remotely: you still need an ender chest block, and the usual rules apply (no solid block on top, spawn protection, sneak with an item places it). The private chest is untouched in shared mode.

Permissions: `sharedenderchest.use` (default everyone) and `sharedenderchest.admin` (default op). Revoking `use`, for example with LuckPerms, puts that player back on the private chest.

## Data

- A player's mode is stored in their own player data (`sharedenderchest:mode`).
- The shared chest is `world/sharedenderchest/shared.dat`, in Paper's item format. Keeping it inside `world/` means a world restore rolls it back together with player inventories, so a restore cannot duplicate or lose items between the two.
- It is saved when a player closes it, on every world save (autosave and `save-all`), on `/ec save`, and on shutdown. Writes go to a temp file and are then moved into place.
- If the file cannot be read at startup, the plugin disables itself and leaves the file alone. Players then get vanilla ender chests until it is fixed.

Several players can have the shared chest open at once and see the same slots live. It closes when a player moves more than 8 blocks from the ender chest or the block is broken, as in vanilla.

`config.yml`: `rows` (1 to 6, default 3) and `title`, both read at startup. Lowering `rows` never drops items: the chest stays large enough for its last used slot.

## Build

With JDK 25, from this directory:

```
gradle build
```

The jar is `build/libs/shared-enderchest-1.0.0.jar`. Copy it to `/opt/minecraft/plugins/` and restart.
