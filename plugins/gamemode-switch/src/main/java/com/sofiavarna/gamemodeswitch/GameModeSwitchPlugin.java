package com.sofiavarna.gamemodeswitch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /spec and /surv switch only the sender, and only between survival and spectator.
 * Players get these instead of vanilla /gamemode, which would also allow creative
 * and changing other players.
 */
public final class GameModeSwitchPlugin extends JavaPlugin {

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

    private static String name(GameMode mode) {
        return mode == GameMode.SPECTATOR ? "spectator" : "survival";
    }
}
