package me.truemb.rentit.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.rentit.main.Main;

public class RentItCOMMAND extends BukkitCommand {

	private Main instance;
	private List<String> adminSubCommands = new ArrayList<>();
	
	public RentItCOMMAND(Main plugin) {
		super("rentit", "For all important commands", null, Collections.emptyList());
		this.instance = plugin;

		this.adminSubCommands.add("reload");

		//DISABLED COMMANDS
		this.instance.manageFile().getStringList("Options.commands.rentit.disabledSubCommands").forEach(disabledSubCmds -> {
			new ArrayList<>(this.adminSubCommands).forEach(adminSubCmds -> {
				if(adminSubCmds.equalsIgnoreCase(disabledSubCmds)) {
					this.adminSubCommands.remove(adminSubCmds);
				}
			});
		});
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		
		if(!sender.hasPermission(this.instance.manageFile().getString("Permissions.admin"))) {
			sender.sendMessage(this.instance.getMessage("perm"));
			return true;
		}
		
		if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			
			if(!this.instance.getMethodes().isSubCommandEnabled("rentit", "reload")) {
				sender.sendMessage(this.instance.getMessage("commandDisabled"));
				return true;
			}
			
			this.instance.initRestart(sender);
			return true;
		}
		
		sender.sendMessage(this.instance.getMessage("rentitHelp"));
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		List<String> result = new ArrayList<>();

		if(sender instanceof Player) {
			Player p = (Player) sender;
		
			if(args.length == 1) {
				for(String subCMD : this.adminSubCommands)
					if(this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", subCMD))
						if(subCMD.toLowerCase().startsWith(args[0].toLowerCase()))
							result.add(subCMD);
			}
		}else {
			result = this.adminSubCommands;
		}
		return result;
	}
	
}
