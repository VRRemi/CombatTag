package dev.vrremi.combattag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    public ReloadCommand(Main plugin){
        plugin.getCommand("combattag").setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1){
            sender.sendMessage(Utils.chat("&4CombatTag plugin made by VRRemi. &aUse &2/combattag reload &ato reload the config!"));
            return false;
        }else if(args.length == 1 && args[0].equalsIgnoreCase("reload")){
            if(sender.hasPermission("combattag.admin")){
                Main.getPlugin().reloadConfig();
                sender.sendMessage(Utils.chat("&aConfig reloaded."));
            }else{
                sender.sendMessage(Utils.chat("&cNo permission!"));
            }
            return false;
        }else{
            sender.sendMessage(Utils.chat("&cUnknown command."));
            return false;
        }
    }
}
