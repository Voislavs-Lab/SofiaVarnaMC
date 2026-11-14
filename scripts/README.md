# Scripts

Ops scripts for SofiaVarna: backups, deploys, and one-off maintenance.

`minecraft-backup` is the host script at `/usr/local/sbin/minecraft-backup`. It reads settings from `/etc/minecraft-backup.conf` (committed under `infra/`) and reads the RCON password from `server.properties` on the container. The Drive crypt password stays in `/root/minecraft-gdrive-crypt-pass`. Do not commit those secret files or copy passwords into a script.

`RESTORE.md` is the restore note installed with the backups. The systemd timers live under `infra/systemd/`.
