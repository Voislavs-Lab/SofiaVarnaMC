package com.sofiavarna.gamemodeswitch;

import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Survival and spectator for players who are not op, by every route a player has:
 * /spec and /surv, vanilla /gamemode on yourself, and the F3+N / F3+F4 client keys.
 *
 * <p>Players hold {@code minecraft.command.gamemode} (granted in LuckPerms), so Paper accepts
 * the command and the game mode switcher packet. This plugin narrows that down: only survival
 * and spectator, and only on yourself. {@code gamemodeswitch.unrestricted} (op) skips the checks.
 *
 * <p>The client enables F3+N and F3+F4 only when the server says it has op level 2. For players
 * with {@code gamemodeswitch.use} who are not op, the plugin sends that level with
 * {@link Player#sendOpLevel(byte)}. That changes nothing on the server: commands and every other
 * permission check still use the real op status.
 */
public final class GameModeSwitchPlugin extends JavaPlugin implements Listener {
    private static final String USE = "gamemodeswitch.use";
    private static final String UNRESTRICTED = "gamemodeswitch.unrestricted";
    /** Level 2 ("gamemaster") is what the client needs for F3+N and F3+F4. */
    private static final byte CLIENT_OP_LEVEL = 2;
    private static final Set<String> GAMEMODE_LABELS = Set.of("gamemode", "minecraft:gamemode");
    private static final Set<String> OP_LABELS = Set.of("op", "deop", "minecraft:op", "minecraft:deop");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getOnlinePlayers().forEach(this::syncClientOpLevel);
    }

    // ---- /spec and /surv -------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only a player can switch their own game mode. Use /gamemode from the console.");
            return true;
        }
        if (args.length > 0) {
            player.sendMessage(Component.text("/" + label + " takes no arguments and only changes your own mode.",
                    NamedTextColor.RED));
            return true;
        }

        GameMode target = switch (command.getName()) {
            case "spectator" -> GameMode.SPECTATOR;
            case "survival" -> GameMode.SURVIVAL;
            default -> null;
        };
        if (target == null) {
            return false;
        }
        if (player.getGameMode() == target) {
            player.sendMessage(Component.text("You are already in " + name(target) + ".", NamedTextColor.GRAY));
            return true;
        }

        player.setGameMode(target);
        player.sendMessage(Component.text("Game mode: " + name(target) + ".", NamedTextColor.GREEN));
        getLogger().info(player.getName() + " switched to " + name(target));
        return true;
    }

    // ---- vanilla /gamemode -----------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGameModeCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String[] parts = event.getMessage().substring(1).trim().split("\\s+");
        if (parts.length == 0 || !GAMEMODE_LABELS.contains(parts[0].toLowerCase(Locale.ROOT))) {
            return;
        }
        if (player.hasPermission(UNRESTRICTED)) {
            return;
        }
        if (parts.length < 2) {
            return; // vanilla prints the usage and changes nothing
        }

        String mode = parts[1].toLowerCase(Locale.ROOT);
        if (!mode.equals("survival") && !mode.equals("spectator")) {
            deny(event, "You can only use /gamemode survival or /gamemode spectator.");
            return;
        }
        if (parts.length > 3) {
            deny(event, "/gamemode takes a mode and at most your own name.");
            return;
        }
        if (parts.length == 3) {
            String who = parts[2];
            if (!who.equals("@s") && !who.equalsIgnoreCase(player.getName())) {
                deny(event, "You can only change your own game mode.");
            }
        }
    }

    private static void deny(PlayerCommandPreprocessEvent event, String message) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text(message, NamedTextColor.RED));
    }

    // ---- F3+N / F3+F4 ----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getCause() != PlayerGameModeChangeEvent.Cause.GAMEMODE_SWITCHER) {
            return; // commands are checked above, plugins and the console are trusted
        }
        Player player = event.getPlayer();
        GameMode mode = event.getNewGameMode();
        if (mode == GameMode.SURVIVAL || mode == GameMode.SPECTATOR || player.hasPermission(UNRESTRICTED)) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(Component.text("Only survival and spectator are allowed.", NamedTextColor.RED));
    }

    // The server re-sends the real op level on join, respawn, world change and op/deop,
    // so send ours again one tick after each of those.

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        later(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        later(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        later(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerOpCommand(PlayerCommandPreprocessEvent event) {
        if (isOpCommand(event.getMessage().substring(1))) {
            laterAll();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleOpCommand(ServerCommandEvent event) {
        if (isOpCommand(event.getCommand())) {
            laterAll();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRconOpCommand(RemoteServerCommandEvent event) {
        if (isOpCommand(event.getCommand())) {
            laterAll();
        }
    }

    private static boolean isOpCommand(String command) {
        String label = command.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        return OP_LABELS.contains(label);
    }

    private void later(Player player) {
        getServer().getScheduler().runTaskLater(this, () -> syncClientOpLevel(player), 1L);
    }

    private void laterAll() {
        getServer().getScheduler().runTaskLater(this,
                () -> getServer().getOnlinePlayers().forEach(this::syncClientOpLevel), 1L);
    }

    private void syncClientOpLevel(Player player) {
        if (!player.isOnline() || player.isOp()) {
            return; // ops already have their real level; do not lower it
        }
        if (player.hasPermission(USE) && player.hasPermission("minecraft.command.gamemode")) {
            player.sendOpLevel(CLIENT_OP_LEVEL);
        }
    }

    private static String name(GameMode mode) {
        return mode == GameMode.SPECTATOR ? "spectator" : "survival";
    }
}
