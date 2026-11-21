# Scripts

Ops scripts for SofiaVarna: backups, deploys, and one-off maintenance.

`minecraft-backup` is the host script at `/usr/local/sbin/minecraft-backup`. It reads settings from `/etc/minecraft-backup.conf` (committed under `infra/`) and reads the RCON password from `server.properties` on the container. The crypt password stays in `/root/minecraft-gdrive-crypt-pass`, and the R2 key only in root's `rclone.conf`. Do not commit those secret files or copy passwords into a script.

`minecraft-backup-setup-r2` is a one-time helper that adds the `r2-minecraft:` and `sofiavarna-r2:` rclone remotes. It reads the R2 key on stdin and copies the crypt settings from the existing `sofiavarna:` remote.

`RESTORE.md` is the restore note installed with the backups. The systemd timers live under `infra/systemd/`.
