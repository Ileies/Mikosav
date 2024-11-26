package com.rizinos.mikosav.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PlayerData {
    private final String username;
    private final String[] permissions;
    private final String welcomeMessage;
    private final String bannedReason;
    private final long bannedUntil;
    private long mutedUntil;
    private Integer credit;
    private Location home;
    private Double onlineTime = 0.0;
    private Location deathLocation = null;
    private Player tpaTarget = null;

    public PlayerData(String username, Integer credit, Location home, String welcomeMessage, String[] permissions, String bannedReason, long mutedUntil, Long bannedUntil) {
        this.username = username;
        this.credit = credit;
        this.home = home;
        this.welcomeMessage = welcomeMessage;
        this.permissions = permissions;
        this.bannedReason = bannedReason;
        this.bannedUntil = bannedUntil;
        this.mutedUntil = mutedUntil;
    }

    public String getUsername() {
        return username;
    }

    public Integer getCredit() {
        return credit;
    }

    public void setCredit(Integer credit) {
        this.credit = credit;
    }

    public Double getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(Double onlineTime) {
        this.onlineTime = onlineTime;
    }

    public Location getDeathLocation() {
        return deathLocation;
    }

    public void setDeathLocation(Location deathLocation) {
        this.deathLocation = deathLocation;
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public Player getTpa() {
        return tpaTarget;
    }

    public void setTpa(Player tpaTarget) {
        this.tpaTarget = tpaTarget;
    }

    public String getWelcomeMessage() {
        if (welcomeMessage.equals("")) return "§b§lHey %1, Welcome to RizinOS!";
        return welcomeMessage;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public boolean isBanned() {
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;
        return bannedUntil >= currentTimeInSeconds;
    }

    public String getBannedMessage() {
        String dateTime = LocalDateTime.ofEpochSecond(bannedUntil, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "You are banned until §c" + dateTime + "§r: " + bannedReason;
    }

    public boolean isMuted() {
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;
        return mutedUntil >= currentTimeInSeconds;
    }

    public String getMutedMessage() {
        String dateTime = LocalDateTime.ofEpochSecond(mutedUntil, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "You are muted until §c" + dateTime;
    }

    // TODO: Add a command for this
    public void muteFor(Integer minutes) {
        long currentTime = System.currentTimeMillis() / 1000;
        this.mutedUntil = currentTime + (minutes * 60);
    }
}
