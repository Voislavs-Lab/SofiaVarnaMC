# Infra

The Proxmox host, the Minecraft container, systemd, and Paper configuration that should be reviewable.

Committed files are the ones we mean to reproduce. Live files that contain credentials stay on the machine.

`minecraft-backup.conf` is the backup settings file. It names paths and retention only. `systemd/` holds the three backup timers: local at 05:00 and 17:00, RAID check at 05:25, and Google Drive on Sunday at 05:45, all Europe/Sofia.
