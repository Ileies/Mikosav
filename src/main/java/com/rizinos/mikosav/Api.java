package com.rizinos.mikosav;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.rizinos.mikosav.util.IO;
import com.rizinos.mikosav.util.ItemSerialization;
import com.rizinos.mikosav.util.PlayerData;
import com.rizinos.mikosav.util.Warp;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.rizinos.mikosav.util.ItemSerialization.jsonToItems;
import static com.rizinos.mikosav.util.Utils.locationToString;
import static com.rizinos.mikosav.util.Utils.stringToLocation;

public class Api {
    // TODO: Check everywhere if the playerData is saved appropriately. (rewritten to players.replace)
    boolean online = true;
    String apiAddress = "http://localhost/api/mc/";
    String unavailable = "There was a problem with our Webservice. Please contact Ileies";
    Gson gson = new Gson();
    Type mapType = new TypeToken<Map<String, Object>>() {
    }.getType();

    private String get(String endpoint) {
        if (!online) return null;
        String res = IO.get(apiAddress + endpoint);
        if (res.equals("ERRINV")) {
            online = false;
            Bukkit.getLogger().severe(unavailable);
        }
        return online ? res : null;
    }

    private String post(String endpoint, Object json) {
        if (!online) return null;
        String res = IO.post(apiAddress + endpoint, gson.toJson(json));
        if (res.equals("ERRINV")) {
            online = false;
            Bukkit.getLogger().severe(unavailable);
        }
        return online ? res : null;
    }

    public String getRaw(String endpoint) {
        return get(endpoint);
    }

    public Map<String, Object> getConfig(String version) {
        // TODO: This should download the latest version from the server
        if (!online) return null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", version);
        String data = post("start", jsonObject);
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(data, type);
    }

    public void shutDown() {
        get("shutdown");
    }

    public Integer getCredit(String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        String data = post("getCredit", jsonObject);
        return gson.fromJson(data, Integer.class);
    }

    public List<Map<String, Object>> getCredit(List<String> uuidList) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for (String uuid : uuidList) {
            jsonArray.add(uuid);
        }
        jsonObject.add("uuids", jsonArray);

        String response = post("getCredit", jsonObject);
        Type type = new TypeToken<List<Map<String, Object>>>() {
        }.getType();

        return gson.fromJson(response, type);
    }

    public PlayerData getPlayerData(String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        String data = post("getPlayerData", jsonObject);

        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> playerData = gson.fromJson(data, type);

        if (playerData == null) return null;

        String username = (String) playerData.get("username");
        int credit = ((Double) playerData.get("credit")).intValue();
        Location home = stringToLocation((String) playerData.get("home"));
        String welcomeMessage = (String) playerData.get("welcomeMessage");
        List<?> permissionsObj = (List<?>) playerData.get("permissions");
        if (permissionsObj == null) return null;
        List<String> permissions = permissionsObj.stream()
                .map(String.class::cast)
                .collect(Collectors.toList());
        String bannedReason = (String) playerData.get("bannedReason");
        Long mutedUntil = (Long) playerData.get("mutedUntil");
        Long bannedUntil = (Long) playerData.get("bannedUntil");
        return new PlayerData(username, credit, home, welcomeMessage, permissions, bannedReason, mutedUntil, bannedUntil);
    }

    public boolean setHome(String uuid, Location location) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("homeLocation", locationToString(location));
        return Objects.equals(post("setHome", jsonObject), "true");
    }

    public boolean setWarp(Warp warp) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("warpName", warp.getName());
        jsonObject.addProperty("location", locationToString(warp.getLocation()));
        JsonArray jsonArray = new JsonArray();
        for (String item : warp.getRestrict()) jsonArray.add(item);
        jsonObject.add("restrict", jsonArray);
        return Objects.equals(post("setWarp", jsonObject), "true");
    }

    public boolean deleteWarp(String warpName) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("warpName", warpName);
        return Objects.equals(post("setWarp", jsonObject), "true");
    }

    public void saveInventory(String uuid, String worldName, ItemStack[] items) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("world", worldName);
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("inventory", ItemSerialization.itemsToJson(items));
        post("saveInventory", jsonObject);
    }

    public ItemStack[] loadInventory(String uuid, String worldName) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("world", worldName);
        String response = post("loadInventory", jsonObject);
        return jsonToItems(response);
    }

    public void addPermission(String uuid, String permission) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("permission", permission);
        post("addPermission", jsonObject);
    }

    public void removePermission(String uuid, String permission) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("permission", permission);
        post("removePermission", jsonObject);
    }

    public boolean pay(String from, String to, int amount) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("from", from);
        jsonObject.addProperty("to", to);
        jsonObject.addProperty("amount", amount);
        return Objects.equals(post("pay", jsonObject), "true");
    }

    public Map<String, Object> worldTp(String uuid, String from, String to, Boolean isOp) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("from", from);
        jsonObject.addProperty("to", to);
        jsonObject.addProperty("op", isOp);
        String data = post("canTp", jsonObject);
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(data, type);
    }

    // TODO: Repair and use this:
    public Integer banPlayer(String uuid, Integer minutes, String reason) {
        long currentTime = System.currentTimeMillis() / 1000;
        long bannedUntil = currentTime + (minutes * 60L);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("bannedUntil", bannedUntil);
        jsonObject.addProperty("reason", reason);
        String data = post("banPlayer", jsonObject);
        return gson.fromJson(data, Integer.class);
    }
}
