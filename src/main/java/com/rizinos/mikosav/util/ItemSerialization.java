package com.rizinos.mikosav.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class ItemSerialization {
    private static final Gson gson = new Gson();

    // Base64 Middleware variant
    public static ItemStack[] jsonToItems(String json) {
        Type type = new TypeToken<List<Map<String, Object>>>() {
        }.getType();
        List<Map<String, Object>> list = gson.fromJson(json, type);
        return list.stream().map(map -> {
            if (map == null) return null;
            Material material = Material.getMaterial((String) map.get("type"));
            if (material == null) return null;
            ItemStack itemStack = new ItemStack(material, ((Double) map.get("amount")).intValue());
            itemStack.setItemMeta(metaFrom64((String) map.get("meta")));
            return itemStack;
        }).toArray(ItemStack[]::new);
    }

    public static String itemsToJson(ItemStack[] itemStacks) {
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack == null) {
                serializedItems.add(null);
            } else {
                Map<String, Object> serializedItem = new LinkedHashMap<>();
                serializedItem.put("type", itemStack.getType().name());
                serializedItem.put("amount", itemStack.getAmount());
                serializedItem.put("meta", metaTo64(itemStack.getItemMeta()));

                serializedItems.add(serializedItem);
            }
        }
        return gson.toJson(serializedItems);
    }

    private static String metaTo64(ItemMeta itemMeta) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemMeta);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray()).replace("\n", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ItemMeta metaFrom64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decode(data));
            try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                return (ItemMeta) dataInput.readObject();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
