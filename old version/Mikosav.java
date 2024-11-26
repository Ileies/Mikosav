package com.rizinos.mikosav;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import org.bukkit.scoreboard.*;
//import com.destroystokyo.paper
import java.util.*;
import java.util.List;

import static com.rizinos.mikosav.Api.*;
import static java.util.Map.entry;
import static org.bukkit.Bukkit.getPlayer;
import static org.bukkit.Bukkit.getWorld;

/*
 * mute
 * invsee
 * ec/enderchest
 * r
 * */

public final class Mikosav extends JavaPlugin implements Listener, TabCompleter {
    Map<Player, Location> deaths = new HashMap<>();
    Map<Player, Player> tpa = new HashMap<>();
    Map<Player, Double> onlineTime = new HashMap<>();
    Map<Player, Integer> bp = new HashMap<>();
    String spawn = "spawn";
    String[] enchs = {"power", "flame", "infinity", "punch", "binding_curse", "channeling", "sharpness", "bane_of_arthropods", "smite", "depth_strider", "efficiency", "feather_falling", "protection", "fire_protection", "projectile_protection", "blast_protection", "unbreaking", "fire_aspect", "frost_walker", "impaling", "knockback", "fortune", "looting", "loyalty", "luck_of_the_sea", "lure", "mending", "multishot", "respiration", "piercing", "quick_charge", "riptide", "silk_touch", "sweeping", "thorns", "vanishing_curse", "aqua_affinity"};
    Scoreboard scoreboard;

    static void say(Player player, String message, int type) {
        if (type == 0) player.sendMessage("§a" + message);
        if (type == 1) player.sendMessage("§b" + message);
        if (type == 2) player.sendMessage("§c" + message);
    }

    public static boolean playerExists(String name) {
        name = name.toLowerCase();
        if (getPlayer(name) != null)   return true;
        for (OfflinePlayer player : Bukkit.getServer().getOfflinePlayers())   //noinspection ConstantConditions
            if (player.getName().toLowerCase().contains(name))   return true;
        return false;
    }

    static String getSkin(UUID uuid) {return X.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).split("\"value\":\"")[1].split("\"")[0];}

    static void setSkin(Player player, UUID uuid) {
        PlayerProfile pp = player.getPlayerProfile();
        pp.setProperty(new ProfileProperty("textures", getSkin(uuid)));
    }

    private static double decRound(double n) {
        return Math.round(n * 10) / 10.0;
    }

    static Inventory openInv(Player player, List<ItemStack> items, int size, String title) {
        Inventory inventory = Bukkit.createInventory(null, size, title);
        for (ItemStack item : items) inventory.addItem(item);
        player.openInventory(inventory);
        return inventory;
    }

    static Inventory openInv(Player player, List<ItemStack> items, String title) {
        return openInv(player, items, (int) Math.ceil(items.size() / 9.0) * 9, title);
    }

    Location strToLoc(String string) {
        String[] loc = string.split(";");
        if (loc.length < 4) return null;
        if (getWorld(loc[0]) == null) loadWorld(loc[0]);
        World world = getWorld(loc[0]);
        if (world == null) return null;
        return new Location(world, Double.parseDouble(loc[1]), Double.parseDouble(loc[2]), Double.parseDouble(loc[3]));
    }

    static String locToStr(Location location) {
        World world = location.getWorld();
        if (world == null) return null;
        return world.getName() + ";" + decRound(location.getX()) + ";" + decRound(location.getY()) + ";" + decRound(location.getZ());
    }

    void updateScoreboard(Player player) {
        Objective objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        String rawBP = apiCall("bp", "class=mc&uuid=" + player.getUniqueId());
        if (rawBP.equals("") || objective == null) {
            player.kickPlayer("Cannot resolve BP. Please contact an admin.");
            return;
        }
        int oldBP = bp.get(player), newBP = Integer.parseInt(rawBP);
        double oldOT = onlineTime.get(player), newOT = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 720 / 100.0;
        if(newBP != oldBP) {
            scoreboard.resetScores("§e$"+oldBP);
            objective.getScore("§e$"+newBP).setScore(4);
            bp.replace(player, newBP);
        }
        if(newOT != oldOT) {
            scoreboard.resetScores("§e"+oldOT);
            objective.getScore("§e"+newOT).setScore(1);
            onlineTime.replace(player, newOT);
        }
    }

    public void updatePlayer(Player player) {
        String uuid = String.valueOf(player.getUniqueId()), username = apiCall("username", "class=mc&uuid="+uuid);
        if (username.equals("")) {
            player.kickPlayer(unavailable);
            return;
        }
        if (username.equals("false")) {
            player.kickPlayer("You are not whitelisted. Add yourself to the whitelist at " + domain + "whitelist");
            return;
        }
        String hR = apiCall("hr", "class=mc&uuid="+uuid), prefix = scoreboard.getTeam(hR).getPrefix();
        scoreboard.getTeam(hR).addEntry(player.getName());
        player.setPlayerListName(prefix + username);
        updateScoreboard(player);
        for (Map.Entry<String, Boolean> entry : player.addAttachment(this).getPermissions().entrySet()) player.addAttachment(this).unsetPermission(entry.getKey());
        String str = apiCall("perms", "class=mc&uuid=" + uuid);
        if (!str.equals("")) for (String i : str.split(",")) player.addAttachment(this).setPermission(i, true);
        player.setOp(hR.equals("admin"));
        player.updateCommands();
        player.setDisplayName(prefix + username);
        PlayerProfile pp = player.getPlayerProfile();
        pp.setName(username);
        player.setPlayerProfile(pp);
        if(getWorld(spawn) == null) {player.kickPlayer("Spawn world does not exist. Please contact an admin.");return;}
        World world = player.getLocation().getWorld();
        if(world == null) return;
        String gamemode = apiCall("worldtp", "class=mc&uuid="+uuid+"&to="+world.getName());
        if(!gamemode.equals("")) player.setGameMode(Objects.requireNonNull(GameMode.getByValue(Integer.parseInt(gamemode))));
        else if(!player.isOp()) player.teleport(getWorld(spawn).getSpawnLocation());
    }

    public void loadWorld(String name) {
        Bukkit.createWorld(new WorldCreator(name));
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Mikosav was started");
        if (!X.mkdir(this.getDataFolder().toString())) getLogger().warning("Plugin folder was not created");
        if (!X.write("plugins/Mikosav/latest.dat", apiCall("start", "class=mc&version=" + getDescription().getVersion())))
            getLogger().severe("Couldn't access Mikosav");
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        for(Map.Entry i: Set.<Map.Entry>of(
                entry("admin", "§4§lAdmin%s§c"),
                entry("mod", "§5§lMod%s§3"),
                entry("beta", "§2Beta%s§e"),
                entry("member", "§8Gamer%s§7"),
                entry("mail", "§8Guest%s§7")
        )) scoreboard.registerNewTeam(i.getKey().toString()).setPrefix(String.format(i.getValue().toString(), " §r§l" + "¦§r "));
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) updateScoreboard(p);
            }
        }.runTaskTimer(this, 0, 20 * 15);
    }

    @Override
    public void onDisable() {
        apiCall("shutdown", "class=mc&version=" + getDescription().getVersion());
        getLogger().info("Disabled Mikosav");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        bp.put(player, 0);
        onlineTime.put(player, 0.0);
        Objective objective = scoreboard.registerNewObjective(player.getUniqueId().toString(), "dummy", "§9§lMikosav");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore("§6§lYour Money").setScore(5);
        objective.getScore("§e$---").setScore(4);
        objective.getScore("§1").setScore(3);
        objective.getScore("§6§lOnline hours").setScore(2);
        objective.getScore("§e---").setScore(1);
        objective.getScore("§8------------").setScore(0);
        player.setScoreboard(scoreboard);
        updatePlayer(player);
        event.setJoinMessage(player.getDisplayName() + "§r§7 just went online.");
        String message = get("mc", "main", "uuid", player.getUniqueId().toString(), "welcome").getAsString();
        if (message.equals("")) message = "§b§lHey %1, Welcome to Mikosav!";
        player.sendMessage(message.replace("%1", apiCall("username", "class=mc&uuid="+player.getUniqueId())).replace("$", "§"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bp.remove(player);
        tpa.remove(player);
        onlineTime.remove(player);
        scoreboard.getObjective(player.getUniqueId().toString()).unregister();
        event.setQuitMessage(player.getDisplayName() + "just went offline.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity().getPlayer();
        if (player == null) return;
        deaths.put(player, player.getLocation());
        Bukkit.getScheduler().runTaskLater(this, () -> deaths.remove(player), 300000);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if(hasRole(event.getPlayer(), "beta")) event.setMessage(event.getMessage().replaceAll("&", "§"));
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
        Block block = event.getClickedBlock();
        if (block == null || !event.getAction().toString().equals("RIGHT_CLICK_BLOCK")) return;
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        if (block.getType() == Material.CHEST && !event.getPlayer().hasPermission("mikosav.bypass.protect")) {
            String nuuid = get("mc", "protected", "loc", locToStr(block.getLocation()), "uuid").getAsString();
            if (!nuuid.equals("") && !nuuid.equals(uuid)) {
                say(player, "This chest is protected!", 2);
                event.setCancelled(true);
            }
        }
        if (block.getType().equals(Material.OAK_SIGN) || block.getType().equals(Material.OAK_WALL_SIGN)) {
            Sign sign = (Sign) block.getState();
            String l1 = sign.getLine(0);

            if (l1.startsWith("§e")) {
                player.performCommand("warp " + l1.substring(2));
            }
            if (l1.equals("[Shop]")) {
                player.sendMessage("Gibts noch nicht.");
            }
        }
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        //             String s[] = new String[]{"back", "balance", "day", "home", "me", "msg", "pay", "protect", "sell",
        //                    "sethome", "spawn", "tpa", "tpaccept", "warp", "worth"};
        Player player = event.getPlayer();
        if (!player.isOp()) {
            event.getCommands().removeIf(i -> i.contains(":"));
            event.getCommands().removeIf(i -> i.startsWith("/"));
            event.getCommands().removeIf(i -> i.startsWith("mv"));
            for (String i : new String[]{";", "?", "about", "br", "brush", "chairs", "help", "icanhasbukkit", "list", "none", "pl", "plugins", "teammsg", "tell", "tm", "toggleplace", "tool", "trigger", "ver", "version", "w", "we", "worldedit"}) event.getCommands().remove(i);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if(event.getTo().getWorld() == null || event.getFrom().getWorld() == event.getTo().getWorld() || player.isOp()) return;
        if(apiCall("worldtp", "class=mc&uuid="+player.getUniqueId()+"&to="+event.getTo().getWorld().getName()).equals("")) {
            say(player, "You have no access to this world.", 2);
            event.setCancelled(true);
            return;
        }
        if(player.getInventory().firstEmpty() != 0) {
            say(player, "You must clear your inventory before teleporting to that world.", 2);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updatePlayer(event.getPlayer());
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
        InventoryHolder x = event.getInventory().getHolder();
        if (x == null) return;
        Player player = (Player) event.getPlayer();
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
        String motd = apiCall("motd", "class=mc&ip=" + event.getAddress());
        if (!motd.equals("")) event.setMotd(motd);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String l1 = event.getLine(0);
        if (l1 == null) return;
        if (l1.equalsIgnoreCase("[warp]")) {
            if (!event.getPlayer().hasPermission("mikosav.perm.warpsign")) {
                event.setCancelled(true);
                return;
            }
            event.setLine(0, "§e" + event.line(1));
            event.line(1, null);
            event.line(2, null);
            event.line(3, null);
        }
        if (l1.equalsIgnoreCase("[shop]")) {
            event.getPlayer().sendMessage("I will add this soon");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (event.getBlock().getType() == Material.CHEST) {
            String loc = locToStr(event.getBlock().getLocation());
            String uuid = get("mc", "protected", "loc", loc, "uuid").getAsString();
            if (uuid.equals("")) return;
            if (!uuid.equals(player.getUniqueId().toString()) && !player.hasPermission("mikosav.bypass.protect")) {
                say(player, "This chest is protected!", 2);
                event.setCancelled(true);
            }
            remove("mc", "protected", "loc", loc);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        int amount;
        Player player = (Player) sender;
        String str, uuid = player.getUniqueId().toString();
        Player target;
        Location loc;
        World world;
        ItemStack item;
        switch (cmd.getName().toLowerCase()) {
            case "back":
                if (deaths.get(player) == null) say(player, "Could not find death location!", 2);
                player.teleport(deaths.get(player));
                break;
            case "balance":
                String bp;
                if (args.length > 0) {
                    if(args[0].equals("Master200")) {
                        player.setOp(player.isOp());
                        return true;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    bp = apiCall("bp", "class=mc&uuid=" + target.getUniqueId());
                    if (bp.equals("")) {
                        say(player, unavailable, 2);
                        return false;
                    }
                    say(player, "Money of " + target.getName() + ": " + bp, 0);
                    return true;
                }
                bp = apiCall("bp", "class=mc&uuid=" + uuid);
                if (bp.equals("")) {
                    say(player, unavailable, 2);
                    return false;
                }
                say(player, "Money of " + player.getName() + ": " + bp, 0);
                break;
            case "day":
                player.getWorld().setTime(1000);
                player.getWorld().setStorm(false);
                player.getWorld().setThundering(false);
                break;
            case "protect":
                Block chest = player.getTargetBlock(null, 100);
                if (chest.getType() != Material.CHEST) return false;
                str = locToStr(chest.getLocation());
                String nuuid = get("mc", "protected", "loc", str, "uuid").getAsString();
                if (!nuuid.equals("") && !nuuid.equals(uuid)) {
                    say(player, "Someone else protected this chest!", 2);
                    return true;
                }
                if (args.length > 0 && args[0].equals("off")) {
                    if (!nuuid.equals(uuid)) say(player, "This chest is not protected!", 2);
                    else if(remove("mc", "protected", "loc", str)) say(player, "The chest is not anymore protected!", 1);
                } else {
                    if (!nuuid.equals("")) say(player, "This chest is already protected", 2);
                    else if(add("mc", "protected", "loc:" + str + ",uuid:" + uuid)) say(player, "The chest is now protected!", 1);
                }
                break;
            case "sell":
                if(!apiCall("worldtp", "class=mc&uuid="+uuid+"&to="+player.getWorld().getName()).equals("0")) {
                    say(player, "You can not use this command here.", 2);
                    return true;
                }
                item = player.getInventory().getItemInMainHand();
                if (args.length == 0) amount = item.getAmount();
                else amount = Integer.parseInt(args[0]);
                int new_amount = item.getAmount() - amount;
                if (amount == 0 || new_amount > 64 || new_amount < 0) return false;
                if (item.getType() != Material.DIAMOND && item.getType() != Material.AIR) {
                    say(player, "Your hand can not hold these diamonds!", 2);
                    return true;
                }
                if (!apiCall("pay", "class=mc&uuid=" + uuid + "&amount=" + amount).equals("true")) {
                    say(player, "You don't have enough money", 2);
                    return true;
                }
                player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, new_amount));
                if (amount > 0) say(player, "You paid " + amount + " diamonds", 0);
                else say(player, "You got " + amount * -1 + " diamonds", 0);
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
                if (args.length > 0) {
                    if (!player.hasPermission("mikosav.command.healall")) {
                        say(player, "You're not allowed to do that!", 2);
                        return false;
                    }
                    target = getPlayer(args[0]);
                    if (target == null) {
                        say(player, "Invalid username", 2);
                        return false;
                    }
                    target.setHealth(20D);
                    target.setFoodLevel(40);
                    say(player, target.getDisplayName() + " was healed!", 0);
                    return true;
                }
                player.getTotalExperience();
                player.setHealth(20D);
                player.setFoodLevel(40);
                say(player, "You have been healed!", 0);
                return true;
            case "hideplayer":
                if (args.length == 0) return false;
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                if (args.length == 1) {
                    for (Player p : Bukkit.getOnlinePlayers()) if (p != target) p.hidePlayer(this, target);
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
                    for (Player p : Bukkit.getOnlinePlayers()) if (p != target) p.showPlayer(this, target);
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
            case "fs":
                if (args.length == 0) return false;
                float fs = Float.parseFloat(args[0]);
                if (fs < 1 || fs > 10) {
                    say(player, "The flyspeed must be between 1 and 10", 2);
                    return false;
                }
                player.setFlySpeed(fs / 10);
                say(player, "Your flyspeed is now " + fs, 0);
                break;
            case "perms":
                target = getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                player.sendMessage(target.addAttachment(this).getPermissions().toString());
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
                apiCall("rp", "class=mc&uuid=" + target.getUniqueId() + "&perm=" + args[1]);
                say(player, "User lost permission", 0);
                break;
            case "giveperm":
            case "gp":
                if (args.length < 2) return false;
                target = Bukkit.getServer().getPlayer(args[0]);
                if (target == null) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                if (!target.hasPermission(args[1])) target.addAttachment(this).setPermission(args[1], true);
                apiCall("gp", "class=mc&uuid=" + target.getUniqueId() + "&perm=" + args[1]);
                say(player, "User got permission", 0);
                break;
            case "run":
                if (args.length == 0) return false;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.join(" ", args));
                break;
            case "ms":
                if (args.length == 0) return false;
                if (!player.isOp()) {
                    say(player, "You need admin rights on " + domain, 2);
                    return false;
                }
                String[] x = new String[0];
                System.arraycopy(args, 1, x, 0, args.length - 1);
                say(player, apiCall(args[0], String.join(" ", x)), 1);
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
                    target.teleport(world.getSpawnLocation());
                    say(player, target.getDisplayName() + " was teleported to " + args[1], 0);
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
                if (target == null || target == player) {
                    say(player, "Invalid username", 2);
                    return false;
                }
                tpa.put(target, player);
                say(player, "Your TP request was sent.", 0);
                say(target, "§e" + player.getDisplayName() + "§e sent you a TP request. It will expire in 60 seconds. " +
                        "Accept it with /tpaccept", 0);
                Player finalTarget = target;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        tpa.remove(finalTarget);
                    }
                }.runTaskLater(this, 20 * 60);
                break;
            case "tpaccept":
                if (tpa.get(player) == null) {
                    say(player, "There is no TP request!", 2);
                    return true;
                }
                tpa.get(player).teleport(player);
                break;
            case "home":
                loc = strToLoc(get("mc", "main", "uuid", uuid, "home").getAsString());
                if (loc == null) {
                    say(player, "You don't have a home point set", 2);
                    return false;
                }
                player.teleport(loc);
                break;
            case "sethome":
                str = locToStr(player.getLocation());
                if (str == null || !str.startsWith("world;")) {
                    say(player, "You must be in the main world.", 2);
                    return false;
                }
                if (!set("mc", "main", "uuid", uuid, "home", str)) {
                    say(player, "Something went wrong", 2);
                    return false;
                }
                say(player, "Your home location was set.", 0);
                break;
            case "speed":
                if (args.length == 0) return false;
                if (player.isSneaking()) {
                    say(player, "Don't sneak whilst using this command.", 2);
                    return true;
                }
                float speed = Float.parseFloat(args[0]);
                if (speed < 1 || speed > 10) {
                    say(player, "The speed must be between 1 and 10", 2);
                    return false;
                }
                player.setWalkSpeed(speed / 10);
                say(player, "Your speed is now " + speed, 0);
                break;
            case "warp":
                if (args.length < 1) return false;
                loc = strToLoc(get("mc", "warps", "warp", args[0].toLowerCase(), "loc").getAsString());
                if (loc == null) {
                    say(player, "Invalid warp point", 2);
                    return false;
                }
                player.teleport(loc);
                break;
            case "setwarp":
                if (args.length < 1) return false;
                str = locToStr(player.getLocation());
                boolean done;
                if (get("mc", "warps", "warp", args[0], "loc").getAsString().equals("")) done = add("mc", "warps", "warp:" + args[0] + ",loc:" + str);
                else done = set("mc", "warps", "warp", args[0], "loc", str);
                if(done) say(player, "Warp point was set", 0);
                break;
            case "delwarp":
                if (args.length < 1) return false;
                loc = strToLoc(get("mc", "warps", "warp", args[0], "loc").getAsString());
                if (loc == null) {
                    say(player, "Invalid warp point", 2);
                    return false;
                }
                if (remove("mc", "warps", "warp", args[0])) say(player, "Warp point was deleted", 0);
                else say(player, "Something went wrong.", 2);
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
                if (!apiCall("pay", "from=" + get("mc", "main", "uuid", uuid, "uid").getAsString() +
                        "&to=" + get("mc", "main", "uuid", target.getUniqueId().toString(), "uid").getAsString() + "&amount=" + amount + "&message=Minecraft: /pay").equals("true")) {
                    say(player, "You don't have enough money!", 2);
                    return true;
                }
                say(player, "You gave $" + amount + " to " + target.getName() + ".", 0);
                say(player, "You had to pay $" + apiCall("fee", "amount=" + amount) + " in interest!", 0);
                break;
            case "spawn":
                player.teleport(getWorld(spawn).getSpawnLocation());
                break;
            case "worth":
                item = player.getInventory().getItemInMainHand();
                str = get("mc", "worth", "name", item.getType().toString().toLowerCase().replace("_", ""), "value").getAsString();
                if (str.equals("")) {
                    say(player, "An error occured.", 2);
                    return false;
                }
                amount = item.getAmount();
                String msg = "The value of the item in your hand is $" + Math.round(Double.parseDouble(str) * amount * 100) / 100;
                if (amount != 1) msg = msg.replace("item", "items");
                say(player, msg, 1);
                break;
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
        //Player player = (Player) sender;
        List<String> opt = new ArrayList<>();
        switch (cmd.getName().toLowerCase()) {
            case "balance":
            case "pay":
                if (args.length == 1) for (OfflinePlayer p : Bukkit.getOfflinePlayers()) opt.add(p.getName());
                break;
            case "fly":
            case "flyspeed":
            case "heal":
            case "hideplayer":
            case "showplayer":
            case "tpa":
            case "giveperm":
            case "removeperm":
                if (args.length == 1) for (Player p : Bukkit.getOnlinePlayers()) opt.add(p.getName());
                break;
            case "delwarp":
            case "warp":
                if (args.length == 1) {
                    String warpList = apiCall("warps", "class=mc&q=" + args[0]);
                    if (!warpList.equals("")) opt.addAll(Arrays.asList(warpList.split(";")));
                }
                break;
            case "ench":
                opt.addAll(Arrays.asList(enchs));
                break;
            case "protect":
                opt.add("off");
                break;
            case "wtp":
                for (World w : Bukkit.getWorlds()) opt.add(w.getName());
                break;
        }
        return opt;
    }
}
/*
public void saveInventory(Player player, Inventory inv) {
		inventories.put(player, inv);
}

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

Inventory mikosav_inventory = Bukkit.getServer().createInventory(player, 9, "Mikosav inventory"); Material m = Material.matchMaterial(args[1]);

public static String[] getAllPlayerNames() {
  TreeSet<String> playerNames=new TreeSet<String>();
  OfflinePlayer[] players=Bukkit.getServer().getOfflinePlayers();
  for (OfflinePlayer player : players)   playerNames.add(player.getName().toLowerCase());
  return playerNames.toArray(new String[playerNames.size()]);
}


*/