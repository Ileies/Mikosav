package com.rizinos.mikosav.util;

import org.bukkit.Location;

import java.util.List;

public class Warp {
    private final String name;
    private Location location;
    private List<String> restrict;

    public Warp(String name, Location location, List<String> restrict) {
        this.name = name;
        this.location = location;
        this.restrict = restrict;
    }

    public void update(Location location, List<String> restrict) {
        this.location = location;
        this.restrict = restrict;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public List<String> getRestrict() {
        return restrict;
    }
}