package dev.vrremi.combattag;

import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Main extends JavaPlugin {

    public static Map<UUID, CombatTag> tagged = new HashMap<>();
    public static Map<UUID, UUID> villagers = new HashMap<>();
    public static List<UUID> killedWhenOffline = new ArrayList<>();
    public static Map<UUID, List<ItemStack>> storedItems = new HashMap<>();
    FileConfiguration data;
    private static Main instance;

    public static class CombatTag {
        public BukkitTask task;
        public BossBar bar;

        public CombatTag(BukkitTask task, BossBar bar){
            this.task = task;
            this.bar = bar;
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();
        new ReloadCommand(this);
        try {
            initDataStorage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        load();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(this, 5 * 60 * 20, 5 * 60 * 20);
        getServer().getPluginManager().registerEvents(new Events(getConfig().getInt("combatTagTime")), this);
    }

    public void save() throws IOException {
        for(UUID uuid : villagers.keySet()){
            data.set("villagers." + uuid.toString(), villagers.get(uuid).toString());
        }
        List<String> c = new ArrayList<>();
        for(UUID uuid : killedWhenOffline){
            c.add(uuid.toString());
        }
        data.set("killed", c);
        for(UUID uuid : storedItems.keySet()){
            data.set("storedItems." + uuid.toString(), storedItems.get(uuid));
        }
        File file = new File(getDataFolder() + "/data/data.yml");
        data.save(file);
    }

    public void load(){
        if(data.contains("villagers")){
            for(String key : data.getConfigurationSection("villagers").getKeys(false)){
                villagers.put(UUID.fromString(key), UUID.fromString(data.getString("villagers." + key)));
            }
        }
        if(data.contains("killed")){
            List<String> c = new ArrayList<>();
            for(String s : data.getStringList("killed")){
                killedWhenOffline.add(UUID.fromString(s));
            }
        }
        if(data.contains("storedItems")){
            for(String s : data.getConfigurationSection("storedItems").getKeys(false)){
                List<ItemStack> items = (List<ItemStack>) data.getList("storedItems." + s);
                storedItems.put(UUID.fromString(s), items);
            }
        }
    }

    public void initDataStorage() throws IOException {
        File folder = new File(getDataFolder() + "/data");
        if(!folder.exists()) folder.mkdir();
        File file = new File(getDataFolder() + "/data/data.yml");
        if(!file.exists()) file.createNewFile();
        data = YamlConfiguration.loadConfiguration(file);
    }

    public static Main getPlugin(){
        return instance;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for(CombatTag tag : tagged.values()){
            tag.task.cancel();
            tag.bar.removeAll();
        }
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
