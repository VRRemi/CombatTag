package dev.vrremi.combattag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Events implements Listener {

    public static int TIME = 30;

    public Events(int time){
        TIME = time;
    }

    public void combatTag(Player p, String other){
        BossBar bar;
        if(Main.tagged.containsKey(p.getUniqueId())){
            Main.CombatTag tag = Main.tagged.get(p.getUniqueId());
            tag.task.cancel();
            bar = tag.bar;
            //tag.bar.removeAll();
        }else{
            bar = Bukkit.createBossBar(Utils.chat("&cCombat tagged: &a15s"), BarColor.RED, BarStyle.SOLID);
            p.sendMessage(Utils.chat(Main.getPlugin().getConfig().getString("tagMsg").replace("{PLAYER}", p.getName()).replace("{OTHER}", other)));
        }
        BukkitTask task = new BukkitRunnable() {
            int seconds = 0;
            @Override
            public void run() {
                if(seconds == 0){
                    // First run
                    bar.setVisible(true);
                    bar.addPlayer(p);
                }
                bar.setProgress((double) (TIME-seconds)/TIME);
                bar.setTitle(Utils.chat("&cCombat tagged: &a" + (TIME-seconds) + "s"));
                if(Main.villagers.containsKey(p.getUniqueId())){
                    Entity e = Bukkit.getEntity(Main.villagers.get(p.getUniqueId()));
                    e.setCustomName(Utils.chat("&c" + p.getName() + " &6&lHIT &a" + (TIME-seconds) + "s"));
                }
                //p.sendMessage("Combat tagged for " + (TIME-seconds) + " more seconds");
                if(seconds == TIME){
                    cancel();
                    bar.removeAll();
                    Main.tagged.remove(p.getUniqueId());
                    if(!p.isOnline()){
                        Bukkit.getEntity(Main.villagers.get(p.getUniqueId())).remove();
                        Main.villagers.remove(p.getUniqueId());
                        Main.getPlugin().data.set("villagers." + p.getUniqueId().toString(), null);
                        Main.storedItems.remove(p.getUniqueId());
                        Main.storedItems.put(p.getUniqueId(), null);
                    }else{
                        p.sendMessage(Utils.chat(Main.getPlugin().getConfig().getString("untagMsg").replace("{PLAYER}", p.getName())));
                    }
                }
                seconds++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, 20);
        Main.tagged.put(p.getUniqueId(), new Main.CombatTag(task, bar));
    }

    @EventHandler
    public void playerDamage(EntityDamageByEntityEvent e){
        if(e.getDamager().getType() != EntityType.PLAYER || e.getEntity().getType() != EntityType.PLAYER) return;
        Player p = (Player) e.getEntity();
        //Bukkit.broadcastMessage("OK");
        combatTag(p, e.getDamager().getName());
        combatTag((Player) e.getDamager(), p.getName());

    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        if(!Main.tagged.containsKey(p.getUniqueId())) return;
        Villager villager = (Villager) p.getWorld().spawnEntity(p.getLocation(), EntityType.VILLAGER);
        villager.setAI(false);
        villager.setCustomNameVisible(true);
        villager.setCustomName(Utils.chat("&c" + p.getName() + " &6&lHIT"));
        villager.addScoreboardTag("TAG/" + p.getUniqueId());
        villager.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(0.1);
        villager.setHealth(0.1);
        villager.setRemoveWhenFarAway(false);
        Main.villagers.put(p.getUniqueId(), villager.getUniqueId());
        List<ItemStack> items = new ArrayList<>();
        for(ItemStack item : p.getInventory().getContents()){
            if(item != null){
                items.add(item);
            }
        }
        Main.storedItems.put(p.getUniqueId(), items);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if(Main.tagged.containsKey(p.getUniqueId())) Main.tagged.get(p.getUniqueId()).bar.addPlayer(p);
        if(Main.killedWhenOffline.contains(p.getUniqueId())){
            p.getInventory().clear();
            p.setHealth(0);
            Main.CombatTag tag = Main.tagged.get(p.getUniqueId());
            if(tag != null){
                tag.task.cancel();
                tag.bar.removeAll();
            }
            Main.tagged.remove(p.getUniqueId());
            return;
        }
        if(!Main.villagers.containsKey(p.getUniqueId())) return;
        Bukkit.getEntity(Main.villagers.get(p.getUniqueId())).remove();
        Main.villagers.remove(p.getUniqueId());
        Main.getPlugin().data.set("villagers." + p.getUniqueId().toString(), null);
        Main.storedItems.remove(p.getUniqueId());
        Main.storedItems.put(p.getUniqueId(), null);
    }

    @EventHandler
    public void villagerDeath(EntityDeathEvent e){
        Entity entity = e.getEntity();
        if(entity.getType() != EntityType.VILLAGER) return;
        for(String s : entity.getScoreboardTags()){
            if(s.startsWith("TAG/")){
                String uuid = s.split("TAG/")[1];
                Main.villagers.remove(UUID.fromString(uuid));
                for(ItemStack item : Main.storedItems.get(UUID.fromString(uuid))){
                    entity.getWorld().dropItemNaturally(entity.getLocation(), item);
                }
                Main.CombatTag tag = Main.tagged.get(UUID.fromString(uuid));
                if(tag != null){
                    tag.task.cancel();
                    tag.bar.removeAll();
                }
                Main.getPlugin().data.set("villagers." + uuid, null);
                Main.storedItems.remove(UUID.fromString(uuid));
                Main.storedItems.put(UUID.fromString(uuid), null);
                Main.killedWhenOffline.add(UUID.fromString(uuid));
                //Bukkit.broadcastMessage(uuid);
                break;
            }
        }
    }

    @EventHandler
    public void villagerDamage(EntityDamageEvent e){
        Entity entity = e.getEntity();
        if(entity.getType() != EntityType.VILLAGER) return;
        for(String s : entity.getScoreboardTags()){
            if(s.startsWith("TAG/")){
                if(e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void villagerHit(EntityDamageByEntityEvent e){
        Entity entity = e.getEntity();
        for(String s : entity.getScoreboardTags()){
            if(s.startsWith("TAG/")){
                if(e.getDamager().getType() != EntityType.PLAYER){
                    e.setCancelled(true);
                }else{
                    Player p = (Player) e.getDamager();
                    ItemStack item = p.getInventory().getItemInMainHand();
                    ItemMeta meta = item.getItemMeta();
                    if(meta != null && meta.hasDisplayName()){
                        Bukkit.broadcastMessage(ChatColor.stripColor(entity.getCustomName().split(" ")[0]) + " was killed by " + p.getName() + Utils.chat(" using &b&o[" + meta.getDisplayName() + "&r&b&o]"));
                    }else{
                        Bukkit.broadcastMessage(ChatColor.stripColor(entity.getCustomName().split(" ")[0]) + " was killed by " + p.getName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent e){
        if(Main.killedWhenOffline.contains(e.getEntity().getUniqueId())){
            Main.killedWhenOffline.remove(e.getEntity().getUniqueId());
            e.setDeathMessage(null);
            return;
        }
        if(!Main.tagged.containsKey(e.getEntity().getUniqueId())) return;
        Main.CombatTag tag = Main.tagged.get(e.getEntity().getUniqueId());
        tag.bar.removeAll();
        tag.task.cancel();
        Main.tagged.remove(e.getEntity().getUniqueId());
    }


}
