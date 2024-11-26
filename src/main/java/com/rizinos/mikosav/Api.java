package com.rizinos.mikosav;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.rizinos.mikosav.util.IO;
import com.rizinos.mikosav.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.rizinos.mikosav.util.InventoryUtils.itemsToJson;
import static com.rizinos.mikosav.util.Utils.locationToString;

public class Api {
    // TODO: Check everywhere if the playerData is saved appropriately. (rewritten to players.replace)
    boolean online = true;
    String domain;
    String unavailable = "There was a problem with our Webservice. Please contact Ileies";
    Gson gson = new Gson();

    public Api(String domain) {
        this.domain = domain;
    }

    private String get(String endpoint) {
        if (!online) return null;
        String res = IO.get("http://api." + domain + "/mc/" + endpoint);
        if (res.equals("ERRINV")) {
            online = false;
            Bukkit.getLogger().severe(unavailable);
        }
        return online ? res : null;
    }

    private String post(String endpoint, Object json) {
        if (!online) return null;
        String res = IO.post("http://api." + domain + "/" + endpoint, gson.toJson(json));
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
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", version);
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        String data = post("getConfig", jsonObject);
        if (!online) return null;
        Bukkit.getLogger().severe(data);
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

    public Map<String, Double> getCredit(List<String> uuidList) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for (String uuid : uuidList) {
            jsonArray.add(uuid);
        }
        jsonObject.add("uuids", jsonArray);

        String response = post("getCredit", jsonObject);
        Type type = new TypeToken<Map<String, Double>>() {
        }.getType();

        return gson.fromJson(response, type);
    }

    public PlayerData getPlayerData(String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        String data = post("getPlayerData", jsonObject);
/*
        username: data.user.username;
        welcomeMessage: data.welcomeMessage;
        permissions: data.permissions;
        credit: data.user.credit;
        home: data.homeLocation;
        mutedUntil: data.mutedUntil;
        bannedUntil: data.bannedUntil;
        bannedReason: data.bannedReason;
*/

        // TODO: Add a custom deserializer for this
        return gson.fromJson(data, PlayerData.class);
    }

    public boolean setHome(String uuid, Location location) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("home", locationToString(location));
        return Objects.equals(post("setHome", jsonObject), "true");
    }

    public boolean setWarp(String warpName, Location location, List<String> restrict) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("warp", warpName);
        jsonObject.addProperty("location", locationToString(location));
        JsonArray jsonArray = new JsonArray();
        for (String item : restrict) jsonArray.add(item);
        jsonObject.add("restrict", jsonArray);
        return Objects.equals(post("setWarp", jsonObject), "true");
    }

    public boolean deleteWarp(String warpName) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("warp", warpName);
        return Objects.equals(post("setWarp", jsonObject), "true");
    }

    public void saveInventory(String uuid, String worldName, PlayerInventory inventory) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("worldName", worldName);
        jsonObject.addProperty("uuid", uuid);
        //itemsToJson();
        post("saveInventory/1", inventory);
        post("saveInventory/2", itemsToJson(inventory.getContents()));
        post("saveInventory/3", inventory.getContents());
    }

    public PlayerInventory loadInventory(String uuid, String worldName) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("worldName", worldName);
        String data = post("loadInventory", jsonObject);
        return gson.fromJson(data, PlayerInventory.class);
        //return createInv(jsonToItems(data));
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

    public String worldTp(String uuid, String from, String to) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("from", from);
        jsonObject.addProperty("to", to);
        return post("worldTp", jsonObject);
    }

    // TODO: Repair and use this:
    public void banPlayer(String uuid, Integer minutes, String to) {
        long currentTime = System.currentTimeMillis() / 1000;
        long bannedUntil = currentTime + (minutes * 60L);
    }
}
