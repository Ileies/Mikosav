package com.rizinos.mikosav.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

import static net.kyori.adventure.text.Component.text;

public class Skipper {
    public double speed = 1.57;
    Set<Integer> skipper = new HashSet<>();

    public void entityShootBow(Plugin plugin, EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Firework)) return;
        ItemMeta meta = event.getConsumable().getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals("§4Skipper")) return;
        Player player = (Player) event.getEntity();
        int range = 64;
        BlockIterator blockIterator = new BlockIterator(player, range);
        Block block;
        double nearestDistance = 0;
        while (blockIterator.hasNext()) {
            block = blockIterator.next();
            if (!block.getType().equals(Material.AIR) && !block.getType().equals(Material.WATER) && !block.getType().equals(Material.LAVA))
                break;
            nearestDistance = player.getLocation().distance(block.getLocation());
        }
        nearestDistance += 2;
        double minAngle = .3;
        double distance;
        Entity nearestEntity = null;
        for (Entity entity : player.getNearbyEntities(range, range, range))
            if (entity instanceof LivingEntity && !entity.isDead() && player.getEyeLocation().getDirection().angle(entity.getLocation().toVector().clone().subtract(player.getLocation().toVector())) < minAngle) {
                distance = entity.getLocation().distance(player.getLocation());
                if (distance < nearestDistance) {
                    nearestEntity = entity;
                    nearestDistance = distance;
                }
            }
        if (nearestEntity == null) {
            event.setCancelled(true);
            ItemStack skipper = new ItemStack(Material.FIREWORK_ROCKET);
            player.getInventory().addItem(skipper);
            return;
        }
        Firework firework = (Firework) event.getProjectile();
        LivingEntity target = (LivingEntity) nearestEntity;
        skipper.add(firework.getEntityId());
        new BukkitRunnable() {
            final double maxRotationAngle = speed / 12;

            @Override
            public void run() {
                if (firework.getLocation().distance(target.getLocation()) < 2) firework.detonate();
                if (firework.isDead() || target.isDead()) {
                    skipper.remove(firework.getEntityId());
                    cancel();
                    return;
                }
                Vector dirVelocity = firework.getVelocity().clone().normalize();
                Vector dirToTarget = target.getLocation().clone().add(new Vector(0, 0.5, 0)).subtract(firework.getLocation()).toVector().clone().normalize();
                double angle = dirVelocity.angle(dirToTarget);
                Vector newVelocity;
                if (angle < maxRotationAngle) newVelocity = dirVelocity.clone().multiply(speed);
                else {
                    Vector newDir = dirVelocity.clone().multiply((angle - maxRotationAngle) / angle).add(dirToTarget.clone().multiply(maxRotationAngle / angle));
                    newDir.normalize();
                    newVelocity = newDir.clone().multiply(speed);
                }
                firework.setVelocity(newVelocity.add(new Vector(0, 0.03, 0)));
                firework.getWorld().playEffect(firework.getLocation(), Effect.SMOKE, 0);
                firework.getWorld().playSound(firework.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1, 1);
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    public void projectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Firework) || !skipper.contains(event.getEntity().getEntityId())) return;
        Firework firework = (Firework) event.getEntity();
        Location location = firework.getLocation();
        firework.getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), 2, true, false);
    }

    public void playerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity().getPlayer();
        if (player == null) return;
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
            if (!(damageEvent.getDamager() instanceof Firework)) return;
            ItemMeta meta = ((Firework) damageEvent.getDamager()).getFireworkMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals("§4Skipper"))
                event.deathMessage(text(player.getName() + " died trying to escape Skipper."));
        }
    }
}
