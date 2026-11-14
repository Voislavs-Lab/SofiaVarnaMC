# SofiaVarna restore

Backups of Paper 26.3 on Proxmox CT 111. The live world is `/opt/minecraft/world` inside the container. Three extra copies exist:

| Tier | Where | When | Keep |
| --- | --- | --- | --- |
| Local | `/var/backups/minecraft` on the Proxmox root disk (same Samsung NVMe as the container) | 05:00 and 17:00 Europe/Sofia | 4 |
| RAID | `/mnt/hdd-raid0/minecraft-backups` | Copy of the newest local archive, at most every 46 hours, checked daily at 05:25 | 6 |
| Google Drive | rclone crypt remote `sofiavarna:` (`gdrive:SofiaVarna` inside Drive folder `1XxwsZF6EZaP1wdQizqB5Gs08Bid0XPwI`), scope `drive.file` | Sunday 05:45, copy of the newest RAID archive | 4 |

`md0` is RAID0. Either WD disk failing destroys the RAID copies and the Plex library together. The weekly Drive copy is the one that survives that, or a dead host. The local copies survive a broken world and die with the NVMe. The RAID copies survive a dead NVMe.

Each archive is `sofia-varna-YYYYmmdd-HHMM.tar.zst` plus `.tar.zst.sha256` and `.manifest.txt`. The manifest embeds `/etc/pve/lxc/111.conf` and `minecraft.service`. The archive contains `server.properties`, including the RCON password. The Drive copies are rclone crypt. The crypt password is `/root/minecraft-gdrive-crypt-pass` on the Proxmox host and must also live in a password manager that is not this machine. This file does not contain that password.

Commands, on the Proxmox host:

```
/usr/local/sbin/minecraft-backup status
/usr/local/sbin/minecraft-backup restore-test /var/backups/minecraft/sofia-varna-YYYYmmdd-HHMM.tar.zst
/usr/local/sbin/minecraft-backup restore-world /var/backups/minecraft/sofia-varna-YYYYmmdd-HHMM.tar.zst --apply
```

`restore-test` extracts on the host, checks the world layout, and deletes the extract. It does not stop the game.

`restore-world` without `--apply` only prints what it would do. With `--apply` it:

1. Checks the SHA-256 and the archive listing.
2. Stops `minecraft.service` and waits for the stop (the unit allows 180 seconds).
3. Renames `/opt/minecraft/world` to `/opt/minecraft/world.pre-restore-<timestamp>`. That tree stays until you delete it.
4. Extracts `world/` from the archive, removes `session.lock`, and chowns it to `minecraft`.
5. Starts `minecraft.service`.

If the extract fails, the script moves the renamed world back and starts the service.

A dead NVMe, with the RAID array still up: create an unprivileged Debian CT with the CPU, RAM, and bridge from the manifest's `111.conf` (4 cores, 12 GiB, no swap, nesting, vmbr0). Install Java 25 and `/usr/local/bin/mcrcon`. Extract the archive to `/opt/minecraft`, `chown -R minecraft:minecraft /opt/minecraft`, install the unit from the manifest, and start it. Do not point the new disk at the Plex media directory.

A dead host or a dead WD disk: install rclone elsewhere and recreate the `gdrive` remote with scope `drive.file` and root folder id `1XxwsZF6EZaP1wdQizqB5Gs08Bid0XPwI`. Recreate `sofiavarna` as a crypt remote of `gdrive:SofiaVarna`, using the crypt password from the password manager. That password is a 44-character base64 string. If rclone treats it as already obscured, pass the obscured form with `--no-obscure`. On the Drive website the archive names are encrypted; they sit in the folder named SofiaVarna inside that parent folder. Copy the newest `sofia-varna-*.tar.zst` down, check the SHA-256, then follow the dead-NVMe steps.

Left out of every archive on purpose: `logs/`, `crash-reports/`, `cache/`, `.cache/`, `.config/`, `.local/`, `.paper/`, `.minecraft/`, `/opt/headlessmc`, and `session.lock`.

Do not delete Proxmox snapshots `wither-skeleton-farm` or `pre-paper`, and do not delete `/mnt/hdd-raid0/dump/vzdump-lxc-111-2026_10_22-23_07_50.tar.zst`. Those are the Paper cutover points, not part of this rotation.
