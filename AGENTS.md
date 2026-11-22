# Agent notes

Facts for agents working in this repo or on the live SofiaVarna server. Checked 2026-11-14. Backups, firewall, and permissions updated 2026-11-21.

This repo is public. Leave credentials, internal addresses, the whitelist, and backup tokens out of it. That includes `server.properties`, RCON and management secrets, rclone config, SSH private keys, and whitelist files. Read a secret on the host when a task needs it, and do not print it.

## Server

Players connect to `mc.nikola-voislav.com:25565`. MOTD is SofiaVarna.

| Setting | Value |
| --- | --- |
| Software | Paper 26.3 build 214 (`26.3-214`, API `26.3.build.214-stable`) |
| Minecraft | 26.3, world format 5023, protocol 777 |
| Java | 25 (`java` on the host is a JRE; it has no `javac`) |
| Process | `minecraft.service`, user `minecraft`, `/opt/minecraft`, `server.jar --nogui` |
| Heap | `-Xms8G -Xmx8G`, G1, `TimeoutStopSec=180` |
| Host | 4 cores, 12 GiB RAM, no swap |
| World | `/opt/minecraft/world` |
| View distance | 10 |
| Simulation distance | 8 |
| Players | 20 max, survival, hard, online-mode on, whitelist on |
| Spawn protection | 16 |

`spigot.yml` leaves view-distance and simulation-distance at `default`, so `server.properties` is the source for both.

The world stays in Minecraft 26.3 format. Dimensions are `world/dimensions/minecraft/{overworld,the_nether,the_end}`. Player files are `world/players/{data,stats,advancements}`. Paper did not split off `world_nether` or `world_the_end`. Do not upgrade the server jar to a newer Minecraft version unless the owner asks. A version change rewrites the world.

The vanilla jar is kept beside the live server as `server-vanilla-26.3.jar`. Leave `/opt/minecraft/.minecraft` and `/opt/headlessmc` alone.

Anti-xray is off (`config/paper-world-defaults.yml`, `anti-xray.enabled: false`). Leave it off. In `config/paper-global.yml`, `allow-piston-duplication`, `allow-headless-pistons`, and `allow-permanent-block-break-exploits` are false. Leave them false.

Client render distance does not raise the server cap. Redstone, crops, and mobs tick only out to simulation distance 8. `delay-chunk-unloads-by` is 10s. Player entities track to 128 blocks (`spigot.yml` `entity-tracking-range.players`). View distance does not extend that.

Management JSON-RPC is disabled, so view distance and simulation distance cannot be changed on a live server. Paper can write the in-memory view distance back into `server.properties` on stop. After `systemctl stop`, put the intended `view-distance` back in the file, then start, and confirm a new boot in the log (new pid and a new `Done (` line).

## Plugins

Jars live in `/opt/minecraft/plugins`. A newly copied jar is not hot-loaded. Installing or removing one needs a restart, which needs the owner's approval.

| Plugin | Version | Notes |
| --- | --- | --- |
| BeaconRange | 1.0.0 | Ours. Source is `plugins/beacon-range`. |
| SharedEnderChest | 1.0.0 | Ours. Source is `plugins/shared-enderchest`. Installed 2026-11-21. |
| GameModeSwitch | 1.0.0 | Ours. Source is `plugins/gamemode-switch`. `/spec` and `/surv` for yourself only. Installed 2026-11-21. |
| LuckPerms | 5.5.91 | Bukkit build from download.luckperms.net. YAML storage. See "Permissions". Installed 2026-11-21. |
| CopperGolemClerk | 3.2.0 | Copper golem sorting. Player nodes default to everyone. `coppergolemclerk.alerts` is granted to `default`. |
| BukkitOfUtils | 1.19.5 | Required by XaeroSupport. It downloads libraries on startup. |
| XaeroSupport | 0.0.1 | Server id 80744. Channels `xaeroworldmap:main` and `xaerominimap:main`. |
| SyncmaticaPaper | 0.3.0 | Litematica sharing. Protocol 0.3.20. |
| ServuxPluginEdition | 0.2.1 | Litematica Easy Place. |
| VoxyServerSide | 0.20.0 | LOD terrain and far-player positions. |
| JEIRecipeBridge | 3.0.0 | Sends server recipes to JEI clients. See "JEI". Installed 2026-11-22. |

Only BeaconRange, SharedEnderChest, and GameModeSwitch are source in this repo. Do not vendor the other jars.

Leave these uninstalled unless the owner asks: VeryMcProto, WorldEdit, Essentials, claim plugins, Geyser, Carpet.

## Permissions

The owner's account is the only op (level 4). The other players were de-opped on 2026-11-21. The old file is `/opt/minecraft/ops.json.pre-luckperms`. Player names stay out of this repo, the same as the whitelist.

Everyone is in the LuckPerms group `default`. That group has:

| Node | Gives |
| --- | --- |
| `minecraft.command.teleport` | Full vanilla `/tp` and `/teleport`: to players, to coordinates, and moving others |
| `gamemodeswitch.use` | `/spec` and `/surv` on yourself |
| `coppergolemclerk.alerts` | CopperGolemClerk full-chest and lost-golem alerts |

Do not grant `minecraft.command.gamemode` to `default`. It is one node for every mode and every target, so it would allow creative. Non-ops cannot use F3+F4 or F3+N.

Storage is YAML in `plugins/LuckPerms/yaml-storage`, which the backup tar includes. `infra/luckperms/default-group.yml` mirrors the group. To add a node, run `lp group default permission set <node> true` over RCON. LuckPerms prints its replies to the console log, not to the RCON response, so read the result from the group file. Then copy the file back to `infra/`.

Installed jars: `LuckPerms-Bukkit-5.5.91.jar` SHA-256 `2a6834c51830b133ec8763703391b20240f7a3b4748746503251a45d70cf1b39`. `gamemode-switch-1.0.0.jar` SHA-256 `8c28cebb4440cd37675415081c18f1894c124030d9abfab01f057e2c94a40486`. `CopperGolemClerk-3.2.0.jar` SHA-256 `cd51869f7eece3d517d425e5acf5e5733c4c4dd6452f9d9cffa24591eca22fe2`.

Rollback: remove the LuckPerms and gamemode-switch jars, put `ops.json.pre-luckperms` back as `ops.json`, then restart (the owner says yes first).

XaeroSupport logs a legacy api-version warning on 26.3 and still enables. Leave it installed. The id 80744 is the client's map folder `mw$80744`. Maps and waypoints stay on each player's computer. The connect address has to stay `mc.nikola-voislav.com`, or the client starts a new map. XaeroSupport does not sync explored terrain.

Syncmatica quota is 40000000 bytes per player and resets on restart. `permissions.enforce` is false, so whitelist players may share. Each player who sends or receives a schematic needs the Syncmatica Fabric mod.

Servux has Easy Place on. Paste, tweak data, and seed sharing are off. It can log an error while disabling on shutdown and still enable on the next boot.

Voxy config is `plugins/VoxyServerSide/vss-server-config.yaml`. `generation.enabled` is false, so LOD requests do not generate new chunks. `request_no_map_ingestion` is true. LOD radius is 512 chunks in the overworld and the end, and 64 in the nether. Far players are mode `on`, from 0 to 2048 blocks, every 10 ticks. Those are position snapshots. They do not simulate the distant player. Command is `/vsslod`. `/vsslod reload` applies the config without a restart.

## JEI

Since Minecraft 1.21.2 the server no longer sends the recipe list to clients, and Paper does not send it on the mod-loader channels either. Without help, JEI says "This server does not provide recipes to JEI" and shows only the recipes bundled with the client. JEIRecipeBridge (Mrbysco, MIT, [Modrinth](https://modrinth.com/plugin/jei-recipe-bridge)) sends the server's recipes on `fabric:recipe_sync` and `neoforge:recipe_content`, so JEI shows what the server actually has. It has no config, no commands, and no permissions. Clients need no extra mod.

It is built against Paper internals for one Minecraft version. Before a Paper or Minecraft upgrade, get the matching build, or JEI goes back to client-only recipes. The fallback is JReiProxyServer ([Modrinth](https://modrinth.com/plugin/jreiproxyserver)). It also adds JEI/REI cheat mode, so check its permissions before installing it.

Installed jar: `/opt/minecraft/plugins/JEI-Recipe-Bridge-3.0.0.jar`. SHA-256 `849c7a27522cad006575d5c94da1b660a5605e69852d2069d7a07a7bb3ce188e`. Rollback: remove the jar and restart (the owner says yes first).

## Beacon range

Vanilla beacon range is 20, 30, 40, and 50 for pyramid tiers 1 to 4. BeaconRange sets 45, 70, 100, and 128 through Paper's `Beacon.setEffectRange`. Config is `plugins/BeaconRange/config.yml`. `/beaconrange` prints the ranges. `/beaconrange reload` applies the config without a restart. Permission `beaconrange.admin` defaults to op. No client mod.

The pass runs on the main thread every 40 ticks and calls `update(true, false)`. The force flag is required, or an unchanged block type skips the block-entity write. The value is stored on the beacon. A negative range means Paper's vanilla formula. The plugin rejects a tier above 512.

A beacon refreshes its effect only while its chunk is ticking. Simulation distance 8 is 128 blocks, so a solo player loses the buff shortly after walking past that (10s unload delay, then the remaining effect, 17s at tier 4). A range above 128 is valid and Paper applies it by scanning the online player list, but the chunk still has to be ticking. Force-loading that one chunk is what keeps a farther range alive. Raising simulation distance ticks many more chunks per player and needs a restart. Ask before doing either.

Installed jar: `/opt/minecraft/plugins/BeaconRange-1.0.0.jar`. SHA-256 `cafcc52728c428969418af23ec6157a2ea88572dd6d8458732ca7196c5c87b50`.

Build with JDK 25 from `plugins/beacon-range` (`gradle build`). Paper API is `io.papermc.paper:paper-api:26.3.build.214-stable` from `https://repo.papermc.io/repository/maven-public/`. There is no Gradle wrapper in this repo. The admin workstation has no JDK. Do not leave a temporary JDK on the server.

## Shared ender chest

SharedEnderChest gives each player a choice: `/ec shared` makes ender chest blocks open one chest shared by everyone, and `/ec private` (the default) keeps vanilla. `/ec` shows the mode, and `/ec save` (op or console) writes the chest now. There is no remote opening. Permission `sharedenderchest.use` defaults to everyone. LuckPerms is not needed, but it can revoke that node later.

The mode is stored in each player's data under `sharedenderchest:mode`. The chest is stored in `world/sharedenderchest/shared.dat`, inside the world on purpose: `restore-world` rolls it back together with player inventories. It is saved on close, on each world save (including the backup's `save-all flush`), and on shutdown, through a temp file and a move. If the file is unreadable at startup, the plugin disables itself and leaves the file alone. Do not delete that file to "fix" it. It is the only copy outside backups.

Installed jar: `/opt/minecraft/plugins/shared-enderchest-1.0.0.jar`. SHA-256 `6a670cbf236b19a24e78fabe8771b2d4797a0392c05fed78b2f3461b479f2cc9`.

## Clients

Players use Fabric 26.3. The server is Paper and does not load Fabric mods.

Matching client mods: Xaero's Minimap, Xaero's World Map, Litematica, MaLiLib, Syncmatica, Servux, Sodium, and for LOD streaming Fabric Voxy 0.2.20-beta plus `voxy-server-side-fabric` 0.20.0. Fabric Voxy by itself only builds LODs from chunks that client has loaded. `VoxyServer` 1.2.x does not handshake with this plugin. Players without the Voxy companion still join and keep the normal 10-chunk view.

## Repo

```
plugins/   one Gradle project per plugin we write
scripts/   backup, deploy, and other ops scripts
infra/     host, container, and Paper config we intend to reproduce
```

The default branch is `master`.

`scripts/minecraft-backup` is the host backup script. `infra/minecraft-backup.conf` and `infra/systemd/` are its settings and timers. `scripts/RESTORE.md` is the restore note. The script reads secrets from the host. Do not commit `server.properties`, the RCON password, or the Drive crypt password, and do not copy a password into a script.

Commits on this repo use author and committer `NikolaVoislav <nikolavoislav.sekulov@gmail.com>`. Do not change the machine's global git config.

## Backups

The live copy of the script is `/usr/local/sbin/minecraft-backup` on the Proxmox host. It reads `/etc/minecraft-backup.conf`.

| Copy | When (Europe/Sofia) | Keep |
| --- | --- | --- |
| Local, `/var/backups/minecraft` | 05:00 and 17:00 | 4 |
| Cloudflare R2, crypt remote `sofiavarna-r2:` | Daily 05:25, newest local archive (skipped if R2's newest is under 20 hours old) | 7 |

The owner decided on 2026-11-21: no backups on the RAID0 array, and R2 replaces Google Drive (Drive was out of space). The old copies on `/mnt/hdd-raid0/minecraft-backups` and Drive were deleted on 2026-11-21, after the Nov 10 to Nov 20 archives were copied to R2 and checked.

Archives are named `sofia-varna-YYYYmmdd-HHMM.tar.zst` and include `server.properties`. `restore-test` does not stop the game. `restore-world` only prints a plan unless `--apply` is passed. `--apply` stops the server and renames the live world. Do not pass `--apply` unless the owner asked for a restore.

The world is frozen only on the local job: `save-on`, then `save-off`, `save-all flush`, tar, then `save-on`. If the service is up and RCON fails, the job aborts and does not archive. The R2 job copies a finished archive and does not read the live world. One lock covers both jobs. Prune only after `zstd -t` and SHA-256, and never delete the newest archive. Skip the local job when the Proxmox root filesystem has under 15 GiB free.

RCON is reached with `/usr/local/bin/mcrcon` and `MCRCON_PASS` through `pct exec` on the Proxmox host. Do not SSH to the container for that, and do not publish RCON.

R2 details are in `scripts/RESTORE.md`. `r2-minecraft:` is an S3 remote (provider Cloudflare, EU jurisdiction endpoint, `no_check_bucket`) with a key scoped to the one bucket. `sofiavarna-r2:` is a crypt remote over `r2-minecraft:<bucket>/sofiavarna` with the same password as the old Drive copies (no salt). `scripts/minecraft-backup-setup-r2` creates both from a `sofiavarna:` crypt remote, which you create first (see `scripts/RESTORE.md`). Do not generate a new crypt password. The password file is `/root/minecraft-gdrive-crypt-pass` (a 44-character base64 string; if rclone treats it as already obscured, pass the obscured form with `--no-obscure`). Verify an upload with `rclone cryptcheck --one-way` of the files just uploaded, using hardlinks inside `/var/backups/minecraft`. `rclone md5sum` does not work on a crypt remote. Do not print `rclone.conf`, the crypt password, the R2 key, or an OAuth token. Keep the account id and bucket endpoint out of this repo.

R2 has no hard spending cap. The owner has a Cloudflare budget alert. Keep retention short and do not upload more than one archive a day without asking.

`md0` is RAID0. A copy on it dies with either WD disk, and so does the Plex library. Do not store archives on it, under `media`, or in the vzdump `dump/` directory.

rclone on the host is 1.75.2 at `/usr/local/bin/rclone`. The Google Drive remotes (`gdrive:`, `sofiavarna:`) and their OAuth token were removed on 2026-11-21. Do not set Drive up again unless the owner asks. The crypt password is in the owner's 1Password as "rclone crypt password · Minecraft backups (R2) — REQUIRED to restore".

Leave Proxmox snapshots `pre-paper` and `wither-skeleton-farm` in place. `pre-paper` is the Paper cutover rollback point. The matching vzdump is named in `scripts/RESTORE.md`. Those are outside this rotation. No recurring vzdump job.

## Before you change the live server

Tell the owner and wait for an explicit yes before an install, a plugin or Minecraft upgrade, a restart, a kick, or a world restore.

Do not change the CPU or RAM. Do not expose RCON any further.

SSH: since 2026-11-21 CT 111 accepts keys only (no password login). Root takes the owner's two 1Password keys and the CT117 Ansible key. It is managed by the `ssh_hardening` Ansible role in Voislavs-Lab/infra; change keys there, not by hand.

Firewall: since 2026-11-21 CT 111 is in the Proxmox firewall security group `dmz-games` (owner-approved). Inbound is unchanged (policy ACCEPT), so players connect as before. Outbound from the container may reach the internet and the gateway's DHCP, but not other LAN hosts. RCON and backups still work because they go through `pct exec` on the host, not the network. Do not change those rules without the owner. If a plugin needs to reach a LAN service, ask first.
