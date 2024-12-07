package com.rizinos.mikosav.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.kyori.adventure.text.Component.text;
import static org.bukkit.Bukkit.getServer;

public class InventoryUtils {

    private static final Gson gson = new GsonBuilder().create();

    public static String exportInventory(PlayerInventory inventory) {
        Map<String, Object> data = new HashMap<>();
        data.put("inventory", serializeInventory(inventory));
        data.put("contents", serializeItemStacks(inventory.getContents()));
        data.put("armor", serializeItemStacks(inventory.getArmorContents()));
        data.put("offhand", serializeItemStack(inventory.getItemInOffHand()));
        return gson.toJson(data);
    }

    private static List<Map<String, Object>> serializeInventory(Inventory inventory) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                items.add(item.serialize());
            } else {
                items.add(null);
            }
        }
        return items;
    }

    private static List<Map<String, Object>> serializeItemStacks(ItemStack[] itemStacks) {
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                serializedItems.add(itemStack.serialize());
            } else {
                serializedItems.add(null);
            }
        }
        return serializedItems;
    }

    private static Map<String, Object> serializeItemStack(ItemStack itemStack) {
        if (itemStack != null) {
            return itemStack.serialize();
        }
        return null;
    }

    public static void importInventory(String json, PlayerInventory inventory) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> data = gson.fromJson(json, type);

        ItemStack[] mainInventory = deserializeInventory((Map<String, Object>[]) data.get("inventory"), inventory.getSize());
        for (int i = 0; i < mainInventory.length; i++) {
            inventory.setItem(i, mainInventory[i]);
        }

        ItemStack[] armorContents = deserializeItemStacks((Map<String, Object>[]) data.get("armor"));
        inventory.setArmorContents(armorContents);

        ItemStack offHandItem = deserializeItemStack((Map<String, Object>) data.get("offhand"));
        inventory.setItemInOffHand(offHandItem);
    }

    private static ItemStack[] deserializeInventory(Map<String, Object>[] items, int size) {
        ItemStack[] inventory = new ItemStack[size];
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                inventory[i] = ItemStack.deserialize(items[i]);
            }
        }
        return inventory;
    }

    private static ItemStack[] deserializeItemStacks(Map<String, Object>[] serializedItems) {
        ItemStack[] itemStacks = new ItemStack[serializedItems.length];
        for (int i = 0; i < serializedItems.length; i++) {
            if (serializedItems[i] != null) {
                itemStacks[i] = ItemStack.deserialize(serializedItems[i]);
            }
        }
        return itemStacks;
    }

    private static ItemStack deserializeItemStack(Map<String, Object> serializedItem) {
        if (serializedItem != null) {
            return ItemStack.deserialize(serializedItem);
        }
        return null;
    }

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


    public static List<ItemStack> itemsToJson(ItemStack[] contents) {
        List<ItemStack> items = new ArrayList<>();
        ItemStack newItemStack;
        for (ItemStack itemStack : contents)
            if (itemStack == null) {
                items.add(null);
            } else {
                newItemStack = new ItemStack(itemStack.getType(), itemStack.getAmount());
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) newItemStack.setItemMeta(meta);
                items.add(newItemStack);
            }
        return items;
    }

    public static ItemStack[] jsonToItems(String json) {
        List<MyItemStack> items = gson.fromJson(json, new TypeToken<List<MyItemStack>>() {
        }.getType());
        return items.stream().map(MyItemStack::toItemStack).toArray(ItemStack[]::new);
    }

    public static Inventory parseJsonToInventory(String json, int rows) {
        Type type = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        List<Map<String, Object>> items = gson.fromJson(json, type);

        Inventory inventory = Bukkit.createInventory(null, rows * 9);

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> itemData = items.get(i);
            if (itemData != null) {
                ItemStack itemStack = ItemStack.deserialize(itemData);
                inventory.setItem(i, itemStack);
            }
        }

        return inventory;
    }

    public static Inventory createInv(ItemStack[] contents) {
        return createInv(null, contents);
    }

    public static Inventory createInv(InventoryHolder owner, ItemStack[] contents) {
        Inventory inventory = Bukkit.createInventory(owner, (int) (Math.ceil(contents.length / 9.0) * 9));
        inventory.setContents(contents);
        return inventory;
    }

    public static class MyItemStack {
        //TODO: You need to add meta as well.
        Material type;
        int amount;

        MyItemStack(ItemStack is) {
            new MyItemStack(is.getType(), is.getAmount());
        }

        MyItemStack(Material it, int amount) {
            this.type = it;
            this.amount = amount;
        }

        ItemStack toItemStack() {
            return new ItemStack(this.type, this.amount);
        }
    }
}
