# Infra

The Proxmox host, the Minecraft container, systemd, and Paper configuration that should be reviewable.

Committed files are the ones we mean to reproduce. Live files that contain credentials stay on the machine.

`minecraft-backup.conf` is the backup settings file. It names paths and retention only. `systemd/` holds the two backup timers: local at 05:00 and 17:00, and the R2 copy at 05:25, both Europe/Sofia.

`luckperms/default-group.yml` and `luckperms/admin-group.yml` are the LuckPerms `default` and `admin` groups as they are stored live in `plugins/LuckPerms/yaml-storage/groups/`. Only the groups are kept here. Per-user files stay on the server. Change it with `lp group default permission set|unset <node>` over RCON, then copy the live file back here.
