package com.sofiavarna.beaconrange;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class BeaconRangePlugin extends JavaPlugin {
    private static final int MAX_RANGE = 512;
    private final double[] ranges = new double[5];
    private boolean reported;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!loadRanges()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Beacon effect application runs every 80 ticks. Refresh twice as often so a
        // pyramid change is stored before the next vanilla pass. Chunk loads can be
        // async, so this stays on the main thread.
        getServer().getScheduler().runTaskTimer(this, this::refreshLoaded, 20L, 40L);
        getLogger().info("Beacon range by tier: " + formatRanges());
    }

    private boolean loadRanges() {
        String[] keys = {null, "range.tier1", "range.tier2", "range.tier3", "range.tier4"};
        double[] defaults = {0, 45, 70, 100, 128};
        for (int tier = 1; tier <= 4; tier++) {
            double value = getConfig().getDouble(keys[tier], defaults[tier]);
            if (!Double.isFinite(value) || value <= 0 || value > MAX_RANGE) {
                getLogger().severe(keys[tier] + " must be from 1 to " + MAX_RANGE + ", got " + value);
                return false;
            }
            ranges[tier] = value;
        }
        return true;
    }

    private void refreshLoaded() {
        int changed = 0;
        int seen = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof Beacon beacon)) {
                        continue;
                    }
                    int tier = beacon.getTier();
                    if (tier < 1 || tier > 4) {
                        continue;
                    }
                    seen++;
                    changed += apply(beacon, tier);
                }
            }
        }
        if (!reported) {
            reported = true;
            getLogger().info("Checked " + seen + " active beacon(s), updated " + changed);
        } else if (changed > 0) {
            getLogger().info("Updated " + changed + " beacon(s)");
        }
    }

    private int apply(Beacon beacon, int tier) {
        double want = ranges[tier];
        if (same(beacon.getEffectRange(), want)) {
            return 0;
        }
        // force is required. A beacon snapshot whose block type did not change
        // skips writing the block entity unless update is forced.
        beacon.setEffectRange(want);
        if (!beacon.update(true, false)) {
            getLogger().warning("Could not update beacon at " + where(beacon));
            return 0;
        }
        BlockState fresh = beacon.getBlock().getState();
        if (!(fresh instanceof Beacon live) || !same(live.getEffectRange(), want)) {
            double got = fresh instanceof Beacon liveNow ? liveNow.getEffectRange() : Double.NaN;
            getLogger().warning("Beacon at " + where(beacon) + " reads " + got + " after setting " + want);
            return 0;
        }
        return 1;
    }

    private static String where(Beacon beacon) {
        return beacon.getWorld().getName() + " " + beacon.getX() + " " + beacon.getY() + " " + beacon.getZ();
    }

    private static boolean same(double left, double right) {
        return Double.doubleToLongBits(left) == Double.doubleToLongBits(right);
    }

    private String formatRanges() {
        return ranges[1] + " / " + ranges[2] + " / " + ranges[3] + " / " + ranges[4];
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("beaconrange.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            if (!loadRanges()) {
                sender.sendMessage("Beacon range config was rejected. Check the server log.");
                return true;
            }
            reported = false;
            refreshLoaded();
            sender.sendMessage("Beacon range reloaded: " + formatRanges());
            return true;
        }
        sender.sendMessage("Beacon range: " + formatRanges());
        return true;
    }
}
