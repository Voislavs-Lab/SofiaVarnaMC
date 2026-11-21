package com.sofiavarna.sharedenderchest;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import org.bukkit.inventory.ItemStack;

/**
 * Reads and writes the shared chest contents.
 *
 * The file is Paper's item serialization ({@link ItemStack#serializeItemsAsBytes}), which
 * records the data version so items upgrade with the server. Writes go to a temporary
 * file first and then replace the old one in a single move.
 */
final class SharedChestStore {
    private final Path file;

    SharedChestStore(Path file) {
        this.file = file;
    }

    Path file() {
        return file;
    }

    /** Returns the saved items, or an empty array when nothing was saved yet. Throws if the file is unreadable. */
    ItemStack[] load() throws IOException {
        if (!Files.exists(file)) {
            return new ItemStack[0];
        }
        byte[] bytes = Files.readAllBytes(file);
        try {
            return ItemStack.deserializeItemsFromBytes(bytes);
        } catch (RuntimeException e) {
            throw new IOException("cannot decode " + file + ": " + e, e);
        }
    }

    void save(ItemStack[] contents) throws IOException {
        ItemStack[] copy = Arrays.stream(contents)
                .map(item -> item == null ? ItemStack.empty() : item)
                .toArray(ItemStack[]::new);
        byte[] bytes = ItemStack.serializeItemsAsBytes(copy);
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Index of the last slot that holds an item, or -1. */
    static int lastUsedSlot(ItemStack[] items) {
        for (int i = items.length - 1; i >= 0; i--) {
            if (items[i] != null && !items[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
