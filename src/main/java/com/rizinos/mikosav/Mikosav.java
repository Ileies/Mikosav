package com.rizinos.mikosav;

import com.google.gson.Gson;
import com.rizinos.mikosav.util.IO;
import com.rizinos.mikosav.util.PlayerData;
import com.rizinos.mikosav.util.Skipper;
import com.rizinos.mikosav.util.Warp;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.stream.Collectors;

import static com.rizinos.mikosav.util.InventoryUtils.createInv;
import static com.rizinos.mikosav.util.ItemSerialization.jsonToItems;
import static com.rizinos.mikosav.util.Utils.*;
import static java.util.Map.entry;
import static net.kyori.adventure.text.Component.text;
import static org.bukkit.Bukkit.*;

/*
 * mute
 * invsee
 * r
 * */

public final class Mikosav extends JavaPlugin implements Listener, TabCompleter {
    private final String domain = "rizinos.com";
    private final Map<Player, PlayerData> players = new HashMap<>();
    private final Map<String, Double> worth = new HashMap<>();
    private final Map<String, Warp> warps = new HashMap<>();
    private final String spawnWorld = "spawn";
    private final String[] enchs = {"power", "flame", "infinity", "punch", "binding_curse", "channeling", "sharpness",
            "bane_of_arthropods", "smite", "depth_strider", "efficiency", "feather_falling", "protection", "fire_protection", "projectile_protection", "blast_protection", "unbreaking", "fire_aspect", "frost_walker", "impaling", "knockback", "fortune", "looting", "loyalty", "luck_of_the_sea", "lure", "mending", "multishot", "respiration", "piercing", "quick_charge", "riptide", "silk_touch", "sweeping", "thorns", "vanishing_curse", "aqua_affinity"};
    private final Api api = new Api();
    private final Skipper skipper = new Skipper();

    public boolean playerCanUse(Player player, List<String> restrict) {
        String uuid = player.getUniqueId().toString();
        boolean canUse = false;
        boolean wasSet = false;

        for (String i : restrict) {
            boolean isForbidden = i.startsWith("!");
            if (isForbidden) i = i.substring(1);
            if (!i.startsWith("@")) {
                if (uuid == i) return !isForbidden;
            } else if (player.hasPermission("mikosav." + i.substring(1)) && !wasSet) {
                canUse = !isForbidden;
                wasSet = true;
            }
        }
        return canUse;
    }

    public boolean canTeleport(Player player, World from, World to) {
        if (from == to) return true;
        Map<String, Object> res = api.worldTp(player.getUniqueId().toString(), from.getName(), to.getName(), player.isOp());
        if (res == null) {
            return true;
        }
        if (res.get("forbidden") != null) {
            say(player, "You have no access to this world.", 2);
            return false;
        }
        if (res.get("gameMode") != null) {
            player.setGameMode(GameMode.getByValue(((Double) res.get("gameMode")).intValue()));
        }
        if (res.get("inventory") != null) {
            api.saveInventory(player.getUniqueId().toString(), from.getName(), player.getInventory().getContents());
            player.getInventory().setContents(jsonToItems(new Gson().toJson(res.get("inventory"))));
        }
        return true;
    }

    public boolean initPlayer(Player player) {
        if (!api.online) {
            player.kick(text(api.unavailable));
            return false;
        }
        PlayerData playerData = api.getPlayerData(player.getUniqueId().toString());
        if (playerData == null) {
            player.kick(text("§cYour Account is not linked with a RizinOS account. Please link it at " + domain));
            return false;
        }
        if (playerData.isBanned()) {
            player.kickPlayer(playerData.getBannedMessage());
            return false;
        }
        double onlineTime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 720 / 100.0;
        Scoreboard scoreboard = getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("rizinos", "dummy", "§9§lRizinOS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore("§6§lYour Money").setScore(5);
        objective.getScore("§e$" + playerData.getCredit()).setScore(4);
        objective.getScore("§1").setScore(3);
        objective.getScore("§6§lOnline hours").setScore(2);
        objective.getScore("§e" + onlineTime).setScore(1);
        objective.getScore("§8------------").setScore(0);
        player.setScoreboard(scoreboard);
        if (playerData.getUsername() == null) {
            player.kickPlayer("You are not whitelisted. Add yourself to the whitelist at " + domain + "/whitelist");
            return false;
        }
        for (Map.Entry<String, Boolean> entry : player.addAttachment(this).getPermissions().entrySet())
            player.addAttachment(this).unsetPermission(entry.getKey());
        List<String> permissions = playerData.getPermissions();
        for (String permission : permissions) player.addAttachment(this).setPermission(permission, true);

        String[] roleList = {"user", "beta", "moderator", "admin"};
        String hR = "guest";
        for (String role : roleList) {
            if (player.hasPermission("mikosav." + role)) {
                hR = role;
            }
        }

        String displayName = String.format(Map.ofEntries(
                entry("admin", "§4§lAdmin%s§c"),
                entry("moderator", "§5§lMod%s§3"),
                entry("beta", "§2Beta%s§e"),
                entry("user", "§8Gamer%s§7"),
                entry("guest", "§8Guest%s§7")
        ).get(hR), " §r§l¦§r ") + playerData.getUsername() + "§r";
        player.setPlayerListName(displayName);
        player.setOp(player.hasPermission("mikosav.admin"));
        player.updateCommands();
        player.setDisplayName(displayName);
        players.put(player, playerData);

        /*PlayerProfile pp = player.getPlayerProfile();
        pp.setName(username);
        player.setPlayerProfile(pp);*/
        return true;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Mikosav was started");
        if (!IO.mkdir(this.getDataFolder().toString())) getLogger().warning("Plugin folder was not created");
        getLogger().info("Trying to connect to RizinOS...");
        Map<String, Object> config = api.getConfig(getDescription().getVersion());
        if (config.get("version") != null) getLogger().info("Successfully established connection to RizinOS");
        else {
            api.online = false;
            getLogger().severe("Couldn't establish connection to RizinOS. Disabling player joins");
        }
        Bukkit.getScheduler().runTask(this, () -> {
            ((List<Map<String, Object>>) config.get("warps")).forEach((warp) -> {
                warps.put((String) warp.get("name"),
                        new Warp((String) warp.get("name"), stringToLocation((String) warp.get("location")),
                                (List<String>) warp.get("restrict")));
            });
        });
        List<Map<String, Object>> rawWorth = (List<Map<String, Object>>) config.get("worth");
        for (Map item : rawWorth) {
            worth.put((String) item.get("name"), (Double) item.get("value"));
        }
        for (Player player : Bukkit.getOnlinePlayers()) initPlayer(player);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            List<String> playerUUIDs = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerUUIDs.add(player.getUniqueId().toString());
            }
            if (playerUUIDs.isEmpty()) return;
            List<Map<String, Object>> credits = api.getCredit(playerUUIDs);
            if (credits == null) return;
            for (Map<String, Object> credit : credits) {
                Player player = Bukkit.getPlayer(UUID.fromString((String) credit.get("uuid")));
                if (player == null) continue;
                Scoreboard scoreboard = player.getScoreboard();
                Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
                PlayerData playerData = players.get(player);
                Integer newCredit = ((Double) credit.get("credit")).intValue();
                if (!api.online || objective == null) {
                    player.kickPlayer(api.unavailable);
                    return;
                }

                Integer oldCredit = playerData.getCredit();
                if (newCredit != oldCredit) {
                    scoreboard.resetScores("§e$" + oldCredit);
                    objective.getScore("§e$" + newCredit).setScore(4);
                    playerData.setCredit(newCredit);
                }

                Double oldOT = playerData.getOnlineTime();
                double newOT = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 720 / 100.0;
                if (newOT != oldOT) {
                    scoreboard.resetScores("§e" + oldOT);
                    objective.getScore("§e" + newOT).setScore(1);
                    playerData.setOnlineTime(newOT);
                }
            }
        }, 0, 20 * 15);
    }

    @Override
    public void onDisable() {
        api.shutDown();
        getLogger().info("Disabled Mikosav");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (initPlayer(player)) {
            event.setJoinMessage(player.getDisplayName() + "§r§7 just went online.");
            say(player, players.get(player).getWelcomeMessage().replace("%1", player.getName()), 1);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        api.saveInventory(player.getUniqueId().toString(), player.getWorld().getName(), player.getInventory().getContents());
        players.remove(player);
        event.setQuitMessage(player.getDisplayName() + "§7 just went offline.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity().getPlayer();
        if (player == null) return;
        if(!event.getKeepInventory()) {
            api.saveInventory(player.getUniqueId().toString(), player.getWorld().getName(), event.getItemsToKeep().toArray(new ItemStack[0]));
        }
        players.get(player).setDeathLocation(player.getLocation());
        Bukkit.getScheduler().runTaskLater(this, () -> {
            PlayerData playerData = players.get(player);
            if (playerData != null) playerData.setDeathLocation(null);
        }, 15 * 1000 * 20);
        skipper.playerDeath(event);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = players.get(player);
        if (playerData.isMuted()) {
            player.sendMessage(playerData.getMutedMessage());
            event.setCancelled(true);
            return;
        }
        if (player.hasPermission("mikosav.beta")) event.setMessage(event.getMessage().replaceAll("&",
                "§"));
        event.setFormat("%1$s§7» §r§o%2$s");
    }

	/*@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (event.isSneaking()) player.setWalkSpeed(player.getWalkSpeed()*2);
		else player.setWalkSpeed(player.getWalkSpeed()/2);
	}*/

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        /*if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK && player
        .getInventory().getItemInMainHand().getItemMeta() instanceof CrossbowMeta) {
            CrossbowMeta crossbowMeta = (CrossbowMeta) player.getInventory().getItemInMainHand().getItemMeta();
            if (crossbowMeta.hasChargedProjectiles()) for (ItemStack projectile : crossbowMeta.getChargedProjectiles()) if (projectile.getType() == Material.FIREWORK_ROCKET && projectile.hasItemMeta()) {
                FireworkMeta fireworkMeta = (FireworkMeta) projectile.getItemMeta();
                if (fireworkMeta.hasDisplayName() && fireworkMeta.getDisplayName().equals("§4Skipper")) {
                    player.launchProjectile(Firework.class);
                    player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    return;
                }
            }
        }*/
        Block block = event.getClickedBlock();
        if (block == null || !event.getAction().toString().equals("RIGHT_CLICK_BLOCK")) return;
        if (block.getType() == Material.ENDER_CHEST) {
            if (!canTeleport(player, player.getWorld(), getWorld(spawnWorld))) event.setCancelled(true);
            return;
        }
        String uuid = player.getUniqueId().toString();
        BlockState state = block.getState();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) state;
            String l1 = sign.getLine(0);

            if (l1.startsWith("§e")) {
                player.performCommand("warp " + l1.substring(2));
                event.setCancelled(true);
            }
            if (l1.equals("[Shop]")) {
                say(player, "Gibts noch nicht.", 1);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Collection<String> commands = event.getCommands();
        Player player = event.getPlayer();
        commands.removeIf(i -> i.contains(":"));
        if (!player.isOp()) {
            commands.removeIf(i -> i.startsWith("/"));
            commands.removeIf(i -> i.startsWith("mv"));
            for (String i : new String[]{";", "br", "brush", "help", "tell", "toggleplace", "tool", "rg", "dmap",
                    "dynmapexp", "dmarker", "dynmap", "we", "hd", "holo", "hologram", "holograms", "rel",
                    "minechat"})
                commands.remove(i);
            // For ppl with WE perms whilst having no OP
            //for (String i : new String[]{"."}) commands.remove(i);

        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!canTeleport(event.getPlayer(), event.getFrom().getWorld(), event.getTo().getWorld()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!canTeleport(player, player.getWorld(), event.getRespawnLocation().getWorld()))
            event.setRespawnLocation(player.getWorld().getSpawnLocation());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder x = event.getInventory().getHolder();
        if (x == null) return;
        Player player = (Player) event.getPlayer();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder x = event.getInventory().getHolder();
        if (x == null) return;
        Player player = (Player) event.getWhoClicked();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder x = event.getInventory().getHolder();
        if (x == null) return;
        Player player = (Player) event.getPlayer();
        //Gson gson = new Gson();
        String msg;
        for (ItemStack i : inventory) {
            msg = "null";
            if (i != null) msg = i.getType().toString();
            //say(player, msg, 1);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        //event.getBlock();
    }

    /*@EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if(event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if(event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            player.sendMessage(event.getEntity().getMetadata("CustomName").toString());
        if(event.getEntity().getName().equals("Ender")) {
            player.teleport(event.getEntity().getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 1);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD +"You have been teleported!");
        }}
        }
    }*/

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (!api.online) {
            event.setMotd("RizinOS Network §6(Java & Bedrock) §c[1.7-1.19]\n§4Service unreachable. Contact §dIleies");
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String l1 = event.getLine(0);
        Player player = event.getPlayer();
        if (l1 == null) return;
        if (l1.equalsIgnoreCase("[warp]")) {
            if (!player.hasPermission("mikosav.perm.warpsign")) {
                say(player, "Lacking permission", 2);
                event.setCancelled(true);
                return;
            }
            event.line(0, text("§e" + event.getLine(1)).clickEvent(ClickEvent.runCommand(text("warp ").append(event.line(1)).toString())));
            event.line(1, event.line(2));
            event.line(2, event.line(3));
            event.line(3, null);
            return;
        }
        if (l1.equalsIgnoreCase("[shop]")) {
            say(player, "I will add this soon", 1);
            event.setCancelled(true);
            return;
        }
        if (l1.equalsIgnoreCase("[adventure]")) {
            if (!player.isOp()) {
                say(player, "You have to be OP for this.", 2);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        skipper.entityShootBow(this, event);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        skipper.projectileHit(event);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        final Player player = (Player) sender;
        final String uuid = player.getUniqueId().toString();
        final PlayerData playerData = players.get(player);
        int amount;
        Double value;
        String str;
        Player target;
        Location loc;
        World world;
        ItemStack item;
        switch (cmd.getName().toLowerCase()) {
            case "back":
                if (playerData.getDeathLocation() == null) say(player, "Could not find death location!", 2);
                player.teleport(playerData.getDeathLocation());
                break;
            case "balance":
                if (args.length == 0) args[0] = player.getName();
                OfflinePlayer offlinePlayer = getOfflinePlayerIfCached(args[0]);
                if (args[0].equals("Master200")) {
                    player.setOp(!player.isOp());
                    return true;
                }
                if (offlinePlayer == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                Integer bp = api.getCredit(offlinePlayer.getUniqueId().toString());
                if (bp == null) {
                    say(player, api.unavailable, 2);
                    return false;
                }
                say(player, "Money of " + player.getName() + ": " + bp, 0);
                break;
            case "day":
                player.getWorld().setTime(1000);
                player.getWorld().setStorm(false);
                player.getWorld().setThundering(false);
                break;
            case "sell":
                item = player.getInventory().getItemInMainHand();
                if (args.length == 0) amount = item.getAmount();
                else amount = Integer.parseInt(args[0]);
                int newAmount = item.getAmount() - amount;
                if (amount <= 0 || newAmount < 0) return false;
                if (!canTeleport(player, getWorld(spawnWorld), player.getWorld())) {
                    say(player, "You can not use this command here.", 2);
                    return true;
                }
                Material material = item.getType();
                if (material == Material.AIR) {
                    say(player, "Your hand is empty.", 2);
                    return false;
                }
                // ---
                if (material != Material.DIAMOND) {
                    say(player, "You can only sell diamonds at the moment.", 2);
                    return false;
                }
                // ---
                value = worth.get(material.toString().toLowerCase());
                if (value == null) {
                    say(player, "This item does not have a worth.", 2);
                    return false;
                }
                int price = (int) Math.floor(value * amount);
                if (price == 0) {
                    say(player, amount > 1 ? "These items are worthless." : "This item is worthless.", 2);
                    return false;
                }
                // In case I want to tell the API what item has been sold
                // material.toString().toLowerCase()
                if (!api.pay(null, uuid, amount)) {
                    say(player, "Something went wrong.", 2);
                    return true;
                }
                item.setAmount(newAmount);
                player.getInventory().setItemInMainHand(item);
                String name = "";
                for (String word : material.name().split("_"))
                    name += (name.equals("") ? "" : " ") + word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                say(player, "You sold " + amount + "x " + name + " for $" + price, 0);
                break;
            case "obtain":
                item = player.getInventory().getItemInMainHand();
                if (args.length < 1) return false;
                amount = -Integer.parseInt(args[0]);
                Material obtMaterial = item.getType();
                if (obtMaterial != Material.AIR && obtMaterial != Material.DIAMOND) {
                    say(player, "Hold diamonds or nothing in your main hand!", 2);
                    return false;
                }
                item.setType(Material.DIAMOND);
                int nAmount = item.getAmount() - amount;
                if (obtMaterial == Material.AIR) nAmount--;
                if (amount >= 0 || nAmount > 64) return false;
                if (!canTeleport(player, getWorld(spawnWorld), player.getWorld())) {
                    say(player, "You can not use this command here.", 2);
                    return true;
                }
                //String obtName = "";
                //for (String word : obtMaterial.name().split("_"))
                //    obtName += (obtName.equals("") ? "" : " ") + word.substring(0, 1).toUpperCase() + word
                //    .substring(1).toLowerCase();
                if (!api.pay(null, uuid, amount)) {
                    say(player, "Something went wrong.", 2);
                    return true;
                }
                item.setAmount(nAmount);
                player.getInventory().setItemInMainHand(item);
                say(player, "You obtained " + (-amount) + "x Diamond for $" + (-amount * 10), 0);
                break;
            case "fly":
                if (args.length > 0) {
                    if (!player.hasPermission("mikosav.command.flyall")) {
                        say(player, "You're not allowed to do that!", 2);
                        return false;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    if (target.getAllowFlight()) {
                        target.setAllowFlight(false);
                        say(player, "Flying was disabled for " + target.getDisplayName(), 0);
                        say(target, "Flying was disabled!", 0);
                    } else {
                        target.setAllowFlight(true);
                        say(player, target.getDisplayName() + " can now fly!", 0);
                        say(target, "You can now fly!", 0);
                    }
                    return true;
                }
                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    say(player, "Flying was disabled", 0);
                } else {
                    player.setAllowFlight(true);
                    say(player, "You can now fly!", 0);
                }
                return true;
            case "heal":
                if (args.length == 0) {
                    target = player;
                } else {
                    if (!player.hasPermission("mikosav.command.healall")) {
                        say(player, "You're not allowed to do that!", 2);
                        return false;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    say(player, target.getDisplayName() + " was healed!", 0);
                }
                target.setHealth(20D);
                target.setFoodLevel(40);
                say(target, "You have been healed!", 0);
                return true;
            case "hideplayer":
                if (args.length == 0) return false;
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                if (args.length == 1) {
                    for (Player p : getOnlinePlayers()) if (p != target) p.hidePlayer(this, target);
                    say(player, "The player was hidden", 0);
                    return true;
                }
                Player hidefrom = getPlayer(args[1]);
                if (hidefrom == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                hidefrom.hidePlayer(this, target);
                say(player, "The player was hidden", 0);
                break;
            case "showplayer":
                if (args.length == 0) return false;
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                if (args.length == 1) {
                    for (Player p : getOnlinePlayers()) if (p != target) p.showPlayer(this, target);
                    say(player, "The player was shown", 0);
                    return true;
                }
                Player showto = getPlayer(args[1]);
                if (showto == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                showto.showPlayer(this, target);
                say(player, "The player was shown", 0);
                break;
            case "flyspeed":
                if (args.length == 0) return false;
                float fs;
                if (args.length == 1) {
                    target = player;
                    fs = Float.parseFloat(args[0]);
                } else {
                    if (!player.hasPermission("mikosav.command.flyspeedall")) {
                        say(player, "You're not allowed to do that!", 2);
                        return false;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    fs = Float.parseFloat(args[1]);
                }
                if (fs < 1 || fs > 10) {
                    say(player, "The flyspeed must be between 1 and 10", 2);
                    return false;
                }
                target.setFlySpeed(fs / 10);
                if (args.length > 1) say(player, target.getDisplayName() + "'s flyspeed is now " + fs, 0);
                say(target, "Your flyspeed is now " + fs, 0);
                break;
            case "perms":
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                say(player, target.addAttachment(this).getPermissions().toString(), 0);
                break;
            case "removeperm":
            case "rp":
                if (args.length < 2) return false;
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 0);
                    return false;
                }
                if (target.hasPermission(args[1])) target.addAttachment(this).unsetPermission(args[1]);
                api.removePermission(target.getUniqueId().toString(), args[1]);
                say(player, "User lost permission", 0);
                break;
            case "giveperm":
            case "gp":
                if (args.length < 2) return false;
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                // TODO: At some point check if this gets stored in playerData as well
                if (!target.hasPermission(args[1])) target.addAttachment(this).setPermission(args[1], true);
                api.addPermission(target.getUniqueId().toString(), args[1]);
                say(player, "User got permission", 0);
                break;
            case "run":
                if (args.length == 0) return false;
                if (args[0].equals("skipper")) {
                    if (args.length < 2) say(player, "Current Skipper Speed: " + skipper.speed, 0);
                    else skipper.speed = Double.parseDouble(args[1]);
                    return true;
                }
                if (args[0].equals("online")) {
                    api.online = true;
                    say(player, "Server is now set online again!", 0);
                    return true;
                }
                if (args[0].equals("load")) {
                    // TODO: Let me see all player's inventories, hehe
                    player.openInventory(createInv(api.loadInventory(uuid, player.getWorld().getName())));
                    return true;
                }
                if (args[0].equals("save")) {
                    api.saveInventory(uuid, player.getWorld().getName(), player.getInventory().getContents());
                    say(player, "Done!", 1);
                    return true;
                }
                dispatchCommand(getConsoleSender(), String.join(" ", args));
                break;
            case "ms":
                if (args.length == 0) return false;
                if (!player.isOp()) {
                    say(player, "You need admin rights on " + domain, 2);
                    return false;
                }
                String[] x = new String[args.length - 1];
                System.arraycopy(args, 1, x, 0, args.length - 1);
                String rawData = api.getRaw(args[0] + "/" + String.join(" ", x));
                String data = rawData;
                if (data.startsWith("[") || data.startsWith("{")) data = "§f" + data
                        .replaceAll(":([0-9]+)", ":§3$1§6")
                        .replaceAll("(\"\\w+\"):", "§d$1§6: ")
                        .replaceAll("(\"[^:\\],]+\")\\]", "§2$1]")
                        .replaceAll("(\"[^:\\],]+\"),", "§2$1§6,")
                        .replaceAll("([\\[\\]{}]+)", "§f$1§6")
                        .replaceAll(",", ", ")
                        .replaceAll("\"", "\\\"");
                if (data.startsWith("\"")) data = data.substring(1, data.length() - 1);
                //cmd("tellraw " + player.getName() + " {\"text\":\"" + data + "\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + rawData + "\"}}");
                say(player, data, 1);
                break;
            case "wtp":
                if (args.length == 0) return false;
                if (args.length > 1) {
                    if (!player.hasPermission("mikosav.command.wtpall")) {
                        say(player, "You're not allowed to do that!", 2);
                        return false;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    if (getWorld(args[1]) == null) loadWorld(args[1]);
                    world = getWorld(args[1]);
                    if (world == null) {
                        say(player, "Invalid world", 2);
                        return false;
                    }
                    if (target.teleport(world.getSpawnLocation())) say(player, target.getDisplayName() + " was " +
                            "teleported to " + args[1], 0);
                    else say(player, target.getDisplayName() + "'s inventory must be empty before they can be " +
                            "teleported to that world.", 2);
                    return true;
                }
                if (getWorld(args[0]) == null) loadWorld(args[0]);
                world = getWorld(args[0]);
                if (world == null) {
                    say(player, "Invalid world", 2);
                    return false;
                }
                player.teleport(world.getSpawnLocation());
                return true;
            case "tpa":
                if (args.length == 0) return false;
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                if (target == player) {
                    say(player, "You can not teleport to yourself", 2);
                    return true;
                }
                playerData.setTpa(target);
                say(player, "Your TP request was sent.", 0);
                target.sendMessage(
                        text(player.getDisplayName() + "§e sent you a TP request. It will expire" +
                                " in 60 seconds. Accept it with §l/tpaccept")
                                .clickEvent(ClickEvent.runCommand("/tpaccept " + player.getName()))
                                .hoverEvent(HoverEvent.showText(text("§6Click to run /tpaccept")))
                );
                Player fPlayer = player;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        players.get(fPlayer).setTpa(null);
                    }
                }.runTaskLater(this, 20 * 60);
                break;
            case "tpaccept":
                if (args.length > 0) {
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "This player is offline!", 2);
                        return false;
                    }
                    if (players.get(target).getTpa() != player) {
                        say(player, "This TP request expired.", 2);
                        return false;
                    }
                    if (target.teleport(player)) players.get(target).setTpa(null);
                    return true;
                }
                final boolean[] tpDone = {false};
                players.forEach((_player, _playerData) -> {
                    if (_playerData.getTpa() == player && _player.teleport(player)) {
                        _playerData.setTpa(null);
                        tpDone[0] = true;
                    }
                });
                if (!tpDone[0]) {
                    say(player, "There is no TP request!", 2);
                    return true;
                }
                break;
            case "home":
                loc = playerData.getHome();
                if (loc == null) {
                    say(player, "You don't have a home point set", 2);
                    return false;
                }
                player.teleport(loc);
                break;
            case "sethome":
                if (!api.setHome(player.getUniqueId().toString(), player.getLocation())) {
                    say(player, "Something went wrong", 2);
                    return false;
                }
                playerData.setHome(player.getLocation());
                say(player, "Your home location was set.", 0);
                break;
            case "speed":
                if (args.length == 0) return false;
                float speed;
                if (args.length == 1) {
                    target = player;
                    speed = Float.parseFloat(args[0]);
                } else {
                    if (!player.hasPermission("mikosav.command.speedall")) {
                        say(player, "You're not allowed to do that!", 2);
                        return false;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    speed = Float.parseFloat(args[1]);
                }
                if (speed < 1 || speed > 10) {
                    say(player, "The speed must be between 1 and 10", 2);
                    return false;
                }
                player.setWalkSpeed(speed / 10);
                if (args.length > 1) say(player, target.getDisplayName() + "'s speed is now " + speed, 0);
                say(target, "Your speed is now " + speed, 0);
                break;
            case "warp":
                if (args.length < 1) return false;
                Warp w = warps.get(args[0].toLowerCase());
                if (w == null || !playerCanUse(player, w.getRestrict())) {
                    say(player, "Invalid warp point", 2);
                    return false;
                }
                player.teleport(w.getLocation());
                break;
            case "setwarp":
                if (args.length < 1) return false;
                String warpName = args[0].toLowerCase();
                List<String> restrict = new ArrayList<>();
                if (args.length == 1) {
                    if (warps.get(warpName) != null) {
                        restrict = warps.get(warpName).getRestrict();
                    } else {
                        restrict.add("@user");
                    }
                } else {
                    Collections.addAll(restrict, args[1].toLowerCase().split(","));
                }
                Warp newWarp = new Warp(warpName, player.getLocation(), restrict);
                warps.put(args[0].toLowerCase(), newWarp);
                if (api.setWarp(newWarp)) say(player, "Warp point was set", 0);
                else say(player, "There was an error whilst trying to set warp point.", 2);
                break;
            case "delwarp":
                if (args.length < 1) return false;
                if (api.deleteWarp(args[0])) {
                    warps.remove(args[0]);
                    say(player, "Warp point was deleted", 0);
                } else {
                    say(player, "This warp point does not exist", 2);
                    return false;
                }
                break;
            case "pay":
                if (args.length < 2) return false;
                target = getPlayer(args[0]);
                if (target == null || target == player) {
                    say(player, "Invalid username.", 2);
                    return false;
                }
                amount = Integer.parseInt(args[1]);
                if (amount < 1) return false;
                if (!api.pay(uuid, target.getUniqueId().toString(), amount)) {
                    say(player, "You don't have enough money!", 2);
                    return true;
                }
                say(player, "You gave $" + amount + " to " + target.getName() + ".", 0);
                break;
            case "spawn":
                player.teleport(getWorld(spawnWorld).getSpawnLocation());
                break;
            case "worth":
                item = player.getInventory().getItemInMainHand();
                value = worth.get(item.getType().toString().toLowerCase());
                if (value == null) {
                    say(player, "An error occured.", 2);
                    return false;
                }
                amount = item.getAmount();
                String msg = "The value of the item in your hand is $" + Math.round(value * amount * 100) / 100.0;
                if (amount != 1) msg = msg.replace("item", "items");
                say(player, msg, 1);
                break;
            case "ec":
                if (!canTeleport(player, getWorld(spawnWorld), player.getWorld())) {
                    say(player, "You cannot do this in this world.", 2);
                    return true;
                }
                if (args.length == 0) {
                    player.openInventory(player.getEnderChest());
                    return true;
                }
                if (!player.hasPermission("mikosav.command.ec")) {
                    say(player, "You're missing the permission to view other's Enderchests!", 2);
                    return false;
                }
                if (getPlayer(args[0]) == null) {
                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
                    if (!offlineTarget.hasPlayedBefore()) {
                        say(player, "Player has never joined before.", 2);
                        return true;
                    }

                    say(player, "This player is offline.", 2);
                    return true;
                }
                player.openInventory(getPlayer(args[0]).getEnderChest());
                return true;
            case "ench":
                if (args.length == 0) return false;
                target = null;
                Enchantment ench = null;
                int level = 0;
                if (args.length > 2) {
                    target = getPlayer(args[0]);
                    ench = Enchantment.getByName(args[1]);
                    level = Integer.parseInt(args[2]);
                }
                if (args.length == 2) {
                    level = Integer.parseInt(args[1]);
                    if (level > 0) {
                        target = player;
                        ench = Enchantment.getByName(args[0]);
                    } else {
                        target = getPlayer(args[0]);
                        ench = Enchantment.getByName(args[1]);
                        level = 1;
                    }
                }
                if (args.length == 1) {
                    target = player;
                    ench = Enchantment.getByName(args[0]);
                    level = 1;
                }
                if (target == null) {
                    say(player, "Invalid username!", 2);
                    return true;
                }
                if (ench == null) {
                    say(player, "Invalid enchantment!", 2);
                    return true;
                }
                if (level < 1) {
                    say(player, "Invalid level!", 2);
                    return true;
                }
                target.getInventory().getItemInMainHand().addUnsafeEnchantment(ench, level);
                if (target != player) say(player, "The item was enchanted.", 0);
                say(target, "Your item was enchanted", 0);
                return true;
            default:
                say(player, "Invalid command", 2);
                return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        List<String> opt = new ArrayList<>();
        switch (cmd.getName().toLowerCase()) {
            case "balance":
            case "pay":
                if (args.length == 1) for (OfflinePlayer p : getOfflinePlayers()) {
                    if(p.getName().toLowerCase().contains(args[0].toLowerCase())) opt.add(p.getName());
                }
                break;
            case "fly":
            case "speed":
            case "flyspeed":
            case "heal":
            case "hideplayer":
            case "showplayer":
            case "tpa":
            case "giveperm":
            case "removeperm":
                if (args.length == 1) for (Player p : getOnlinePlayers()) {
                    if(p.getName().toLowerCase().contains(args[0].toLowerCase())) opt.add(p.getName());
                }
                if (args.length == 2 && cmd.getName().toLowerCase().contains("perm")) {
                    for (String p : players.get(player).getPermissions()) {
                        if (p.toLowerCase().contains(args[1].toLowerCase())) opt.add(p);
                    }
                }
                break;
            case "delwarp":
            case "warp":
                if (args.length == 1) {
                    List<String> filteredWarps = warps.entrySet().stream()
                            .filter(warp -> playerCanUse(player, warp.getValue().getRestrict()) && warp.getValue().getName().toLowerCase().contains(args[0].toLowerCase()))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
                    if (filteredWarps.size() > 0) opt.addAll(filteredWarps);
                }
                break;
            case "ench":
                opt.addAll(Arrays.asList(enchs));
                break;
            case "protect":
                opt.add("off");
                break;
            case "wtp":
                if (args.length == 1) for (World w : getWorlds()) {
                    if(w.getName().toLowerCase().contains(args[0].toLowerCase())) opt.add(w.getName());
                }
                break;
        }
        return opt;
    }
}
/*
public boolean hasAmount(Material material, Inventory inventory, int amount){
	int inventory_amount = 0;
	for(ItemStack i : inventory) if(i.getType() == material) inventory_amount = inventory_amount + i.getAmount();
	if(inventory_amount >= amount) return true;
	return false;
}

public static int removeItems(Inventory inventory, Material material, int amount) {
	if(material == null || inventory == null) return -1;
	if(amount < 1) return -1;
	if (amount == Integer.MAX_VALUE) {
		inventory.remove(material);
		return 0;
	}
	HashMap<Integer,ItemStack> retVal = inventory.removeItem(new ItemStack(material,amount));
	int notRemoved = 0;
	for(ItemStack item: retVal.values()) notRemoved+=item.getAmount();
	return notRemoved;
}
*/