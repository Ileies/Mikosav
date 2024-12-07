package com.rizinos.mikosav.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Utils {
    public static void say(Player player, String message, int type) {
        if (type == 0) player.sendMessage("§a" + message);
        if (type == 1) player.sendMessage("§b" + message);
        if (type == 2) player.sendMessage("§c" + message);
        player.saveData();
    }

    public static boolean playerExists(String name) {
        name = name.toLowerCase();
        if (Bukkit.getPlayer(name) != null) return true;
        for (OfflinePlayer player : Bukkit.getServer().getOfflinePlayers())   //noinspection ConstantConditions
            if (player.getName().toLowerCase().contains(name)) return true;
        return false;
    }

    public static String getSkin(UUID uuid) {
        return IO.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).split("\"value\":\"")[1].split("\"")[0];
    }

    public static void setSkin(Player player, UUID uuid) {
        PlayerProfile pp = player.getPlayerProfile();
        pp.setProperty(new ProfileProperty("textures", getSkin(uuid)));
    }

    public static double decRound(double n) {
        return Math.round(n * 10) / 10.0;
    }

    public static String getFriendlyName(ItemStack item) {
        Material m = item.getType();
        StringBuilder c = new StringBuilder();
        for (String word : m.name().split("_"))
            c.append(c.toString().equals("") ? "" : " ")
                    .append(word.substring(0, 1).toUpperCase())
                    .append(word.substring(1).toLowerCase());
        return c.toString();
    }

    public static void cmd(String cmd) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    public static void loadWorld(String name) {
        Bukkit.createWorld(new WorldCreator(name));
    }

    public static String locationToString(Location location) {
        String loc = location.toString();
        return loc.substring(9, loc.length() - 1);
    }

    public static Location stringToLocation(String locationString) {
        String[] parts = locationString.split(",");
        String worldName = null;
        double x = 0, y = 0, z = 0;
        float pitch = 0, yaw = 0;
        for (String part : parts) {
            String[] keyValue = part.split("=");
            String key = keyValue[0];
            String value = keyValue[1];

            switch (key) {
                case "world":
                    worldName = value;
                    break;
                case "x":
                    x = Double.parseDouble(value);
                    break;
                case "y":
                    y = Double.parseDouble(value);
                    break;
                case "z":
                    z = Double.parseDouble(value);
                    break;
                case "pitch":
                    pitch = Float.parseFloat(value);
                    break;
                case "yaw":
                    yaw = Float.parseFloat(value);
                    break;
            }
        }

        if (worldName == null) return null;
        if (Bukkit.getWorld(worldName) == null) loadWorld(worldName);
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
