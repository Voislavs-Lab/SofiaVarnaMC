package com.sofiavarna.sharedenderchest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class SharedEnderChestPlugin extends JavaPlugin implements Listener {
    private static final String USE = "sharedenderchest.use";
    private static final String ADMIN = "sharedenderchest.admin";
    /** Vanilla keeps an ender chest open while the player is within 8 blocks of it. */
    private static final double MAX_DISTANCE_SQUARED = 8.0 * 8.0;

    private NamespacedKey modeKey;
    private SharedChestStore store;
    private Inventory shared;
    /** Player id to the ender chest block they opened the shared chest from. */
    private final Map<UUID, Location> viewers = new HashMap<>();
    /** Ender chest block to how many players are viewing the shared chest through it. */
    private final Map<Location, Integer> openLids = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        modeKey = new NamespacedKey(this, "mode");

        int rows = getConfig().getInt("rows", 3);
        if (rows < 1 || rows > 6) {
            getLogger().severe("rows must be from 1 to 6, got " + rows + ". SharedEnderChest is disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Path worldDir = mainWorldDirectory();
        store = new SharedChestStore(worldDir.resolve("sharedenderchest").resolve("shared.dat"));
        ItemStack[] saved;
        try {
            saved = store.load();
        } catch (IOException e) {
            // Do not start empty: the next save would overwrite the only copy.
            getLogger().severe("Cannot read " + store.file() + ": " + e.getMessage()
                    + ". SharedEnderChest is disabled and the file is left untouched.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int size = rows * 9;
        int needed = SharedChestStore.lastUsedSlot(saved) + 1;
        if (needed > size) {
            size = ((needed + 8) / 9) * 9;
            getLogger().warning("Shared chest holds items in slot " + needed + ", so it stays at "
                    + (size / 9) + " rows instead of the configured " + rows + ".");
        }
        String title = getConfig().getString("title", "Shared Ender Chest");
        shared = Bukkit.createInventory(new Holder(), size, Component.text(title));
        for (int i = 0; i < saved.length && i < size; i++) {
            shared.setItem(i, saved[i]);
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::checkViewers, 10L, 10L);
        getLogger().info("Shared chest: " + (size / 9) + " rows, " + countItems() + " stack(s), file " + store.file());
    }

    @Override
    public void onDisable() {
        if (shared == null) {
            return;
        }
        for (UUID id : List.copyOf(viewers.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.closeInventory();
            }
        }
        viewers.clear();
        for (Location lid : List.copyOf(openLids.keySet())) {
            setLid(lid, false);
        }
        openLids.clear();
        save("shutdown");
    }

    /** The level folder (the one with level.dat), so the chest is restored together with player data. */
    private Path mainWorldDirectory() {
        World main = Bukkit.getWorlds().get(0);
        Path dir = main.getWorldFolder().toPath().toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("level.dat"))) {
                return p;
            }
        }
        getLogger().warning("No level.dat above " + dir + ", storing the shared chest there.");
        return dir;
    }

    // ---- opening ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }
        if (event.useInteractedBlock() == Event.Result.DENY) {
            return; // spawn protection or another plugin said no
        }
        Player player = event.getPlayer();
        if (!isShared(player) || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // Sneaking with something in hand places or uses the item instead, as in vanilla.
        if (player.isSneaking() && (!player.getInventory().getItemInMainHand().isEmpty()
                || !player.getInventory().getItemInOffHand().isEmpty())) {
            return;
        }
        // Vanilla will not open an ender chest with a solid block on top.
        if (block.getRelative(BlockFace.UP).getType().isOccluding()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // the main-hand event opens it; just stop the off hand from acting
        }
        openShared(player, block);
    }

    private void openShared(Player player, Block block) {
        if (player.openInventory(shared) == null) {
            return;
        }
        Location lid = block.getLocation();
        Location previous = viewers.put(player.getUniqueId(), lid);
        if (previous != null) {
            release(previous);
        }
        if (openLids.merge(lid, 1, Integer::sum) == 1) {
            setLid(lid, true);
            playSound(lid, Sound.BLOCK_ENDER_CHEST_OPEN);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Holder)) {
            return;
        }
        Location lid = viewers.remove(event.getPlayer().getUniqueId());
        if (lid != null) {
            release(lid);
        }
        save("close");
    }

    private void release(Location lid) {
        Integer left = openLids.computeIfPresent(lid, (key, count) -> count > 1 ? count - 1 : null);
        if (left == null) {
            setLid(lid, false);
            playSound(lid, Sound.BLOCK_ENDER_CHEST_CLOSE);
        }
    }

    /** Close the chest for anyone who walked away or whose ender chest block is gone, like vanilla. */
    private void checkViewers() {
        List<Player> toClose = new ArrayList<>();
        Iterator<Map.Entry<UUID, Location>> it = viewers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Location> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            Location lid = entry.getValue();
            if (player == null || !(player.getOpenInventory().getTopInventory().getHolder(false) instanceof Holder)) {
                it.remove();
                release(lid);
                continue;
            }
            boolean gone = lid.getWorld() == null || !lid.getWorld().isChunkLoaded(lid.getBlockX() >> 4, lid.getBlockZ() >> 4)
                    || lid.getBlock().getType() != Material.ENDER_CHEST;
            boolean far = player.getWorld() != lid.getWorld()
                    || player.getLocation().distanceSquared(lid.clone().add(0.5, 0.5, 0.5)) > MAX_DISTANCE_SQUARED;
            if (gone || far) {
                toClose.add(player);
            }
        }
        // Outside the loop: closing fires onClose, which edits viewers, releases the lid and saves.
        for (Player player : toClose) {
            player.closeInventory();
        }
    }

    private void setLid(Location lid, boolean open) {
        World world = lid.getWorld();
        if (world == null || !world.isChunkLoaded(lid.getBlockX() >> 4, lid.getBlockZ() >> 4)) {
            return;
        }
        Block block = lid.getBlock();
        if (block.getType() != Material.ENDER_CHEST) {
            return;
        }
        BlockState state = block.getState(false);
        if (state instanceof Lidded lidded) {
            if (open) {
                lidded.open();
            } else {
                lidded.close();
            }
        }
    }

    private static void playSound(Location lid, Sound sound) {
        World world = lid.getWorld();
        if (world != null) {
            world.playSound(lid.clone().add(0.5, 0.5, 0.5), sound, SoundCategory.BLOCKS, 0.5f, 0.9f + (float) Math.random() * 0.1f);
        }
    }

    // ---- saving ----

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        // One write per server save. Autosave and the backup's "save-all flush" both fire this.
        if (event.getWorld().equals(Bukkit.getWorlds().get(0))) {
            save("world save");
        }
    }

    private boolean save(String why) {
        try {
            store.save(shared.getContents());
            return true;
        } catch (IOException e) {
            getLogger().severe("Could not save the shared chest (" + why + ") to " + store.file() + ": " + e);
            return false;
        }
    }

    private int countItems() {
        int n = 0;
        for (ItemStack item : shared.getContents()) {
            if (item != null && !item.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    // ---- mode ----

    private boolean isShared(Player player) {
        if (!player.hasPermission(USE)) {
            return false;
        }
        Byte mode = player.getPersistentDataContainer().get(modeKey, PersistentDataType.BYTE);
        return mode != null && mode == 1;
    }

    private void setShared(Player player, boolean on) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (on) {
            pdc.set(modeKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(modeKey);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
            if (!sender.hasPermission(ADMIN)) {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            } else if (save("command")) {
                sender.sendMessage(Component.text("Shared chest saved (" + countItems() + " stack(s)).", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Save failed. Check the server log.", NamedTextColor.RED));
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only. Console can use: /" + label + " save");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(modeMessage(isShared(player)));
            sender.sendMessage(Component.text("Switch with /" + label + " shared or /" + label + " private.", NamedTextColor.GRAY));
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("shared") || args[0].equalsIgnoreCase("private"))) {
            boolean on = args[0].equalsIgnoreCase("shared");
            setShared(player, on);
            sender.sendMessage(modeMessage(on));
            return true;
        }
        return false; // prints usage
    }

    private static Component modeMessage(boolean shared) {
        return shared
                ? Component.text("Ender chests open the SHARED chest (everyone sees it).", NamedTextColor.LIGHT_PURPLE)
                : Component.text("Ender chests open your PRIVATE chest.", NamedTextColor.GREEN);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> options = sender.hasPermission(ADMIN) ? List.of("shared", "private", "save") : List.of("shared", "private");
        String prefix = args[0].toLowerCase();
        return options.stream().filter(o -> o.startsWith(prefix)).toList();
    }

    /** Marks the shared chest window. */
    private final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return shared;
        }
    }
}
