package com.rizinos.mikosav.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static org.bukkit.Bukkit.getServer;

public class InventoryUtils {
    public static Inventory createInventory(Player player, int size, String title) {
        return getServer().createInventory(null, size * 9, text(title));
    }

    public static Inventory openInventory(Player player, List<ItemStack> items, int size, String title) {
        Inventory inventory = Bukkit.createInventory(null, size, Component.text(title));
        for (ItemStack item : items) inventory.addItem(item);
        player.openInventory(inventory);
        return inventory;
    }

    public static Inventory openInventory(Player player, List<ItemStack> items, String title) {
        return openInventory(player, items, (int) Math.ceil(items.size() / 9.0) * 9, title);
    }

    public static Inventory createInv(ItemStack[] contents) {
        return createInv(null, contents);
    }

    public static Inventory createInv(InventoryHolder owner, ItemStack[] contents) {
        Inventory inventory = Bukkit.createInventory(owner, (int) (Math.ceil(contents.length / 9.0) * 9));
        inventory.setContents(contents);
        return inventory;
    }
}