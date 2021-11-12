package me.truemb.rentit.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import me.truemb.rentit.main.Main;

public class RentItCOMMAND implements CommandExecutor, TabCompleter{

	private Main instance;
	
	public RentItCOMMAND(Main plugin) {
		this.instance = plugin;
		this.instance.getCommand("rentit").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if(!sender.hasPermission(this.instance.manageFile().getString("Permissions.admin"))) {
			sender.sendMessage(this.instance.getMessage("perm"));
			return true;
		}
		
		if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			this.instance.initRestart();
			sender.sendMessage(this.instance.getMessage("reloaded"));
			return true;
		}
		
		sender.sendMessage(this.instance.getMessage("rentitHelp"));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> result = new ArrayList<>();
		
		if(args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
			result.add("reload");
		}
		return result;
	}
	
}
