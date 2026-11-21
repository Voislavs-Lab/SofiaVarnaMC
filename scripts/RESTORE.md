# SofiaVarna restore

Backups of Paper 26.3 on Proxmox CT 111. The live world is `/opt/minecraft/world` inside the container. Two tiers of copies exist:

| Tier | Where | When | Keep |
| --- | --- | --- | --- |
| Local | `/var/backups/minecraft` on the Proxmox root disk (same Samsung NVMe as the container) | 05:00 and 17:00 Europe/Sofia | 4 |
| R2 | rclone crypt remote `sofiavarna-r2:` over a Cloudflare R2 bucket in the EU jurisdiction (folder `sofiavarna`) | Daily 05:25, the newest local archive | 7 |

The local copies survive a broken world and die with the NVMe. The R2 copies survive a dead NVMe or a dead host. The RAID0 array is not used for backups: either WD disk failing destroys it.

Until 2026-11-21 copies also went to `/mnt/hdd-raid0/minecraft-backups` and to Google Drive. Those were deleted after their Nov 10 to Nov 20 archives were copied to R2.

Each archive is `sofia-varna-YYYYmmdd-HHMM.tar.zst` plus `.tar.zst.sha256` and `.manifest.txt`. The manifest embeds `/etc/pve/lxc/111.conf` and `minecraft.service`. The archive contains `server.properties`, including the RCON password. The R2 (and old Drive) copies are rclone crypt. The crypt password is `/root/minecraft-gdrive-crypt-pass` on the Proxmox host and must also live in a password manager that is not this machine. This file does not contain that password.

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

A dead NVMe: fetch the newest archive from R2 as below, then create an unprivileged Debian CT with the CPU, RAM, and bridge from the manifest's `111.conf` (4 cores, 12 GiB, no swap, nesting, vmbr0). Install Java 25 and `/usr/local/bin/mcrcon`. Extract the archive to `/opt/minecraft`, `chown -R minecraft:minecraft /opt/minecraft`, install the unit from the manifest, and start it. Do not point the new disk at the Plex media directory.

A dead host: install rclone anywhere. Get the bucket's R2 key from the password manager ("R2 S3 credentials · minecraft-backups"), copy `scripts/minecraft-backup-setup-r2` over, and create a plain crypt remote first so the helper has one to copy: `rclone config create sofiavarna crypt remote=unused: password=<crypt password>` (a 44-character base64 string, 1Password item "rclone crypt password · Minecraft backups (R2)"; if rclone treats it as already obscured, pass the obscured form with `--no-obscure`). The remote has no salt (`password2`). Then run `minecraft-backup-setup-r2 <account id> minecraft-backups` and paste the access key id and secret on two lines. In the R2 dashboard the object names are encrypted; they sit under `sofiavarna/`. Copy the newest `sofia-varna-*.tar.zst` down with `rclone copy sofiavarna-r2:<name> .`, check the SHA-256, then follow the dead-NVMe steps.

Left out of every archive on purpose: `logs/`, `crash-reports/`, `cache/`, `.cache/`, `.config/`, `.local/`, `.paper/`, `.minecraft/`, `/opt/headlessmc`, and `session.lock`.

Do not delete Proxmox snapshots `wither-skeleton-farm` or `pre-paper`, and do not delete `/mnt/hdd-raid0/dump/vzdump-lxc-111-2026_10_22-23_07_50.tar.zst`. Those are the Paper cutover points, not part of this rotation.
