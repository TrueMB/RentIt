package me.truemb.rentit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.CategoryGUI;
import me.truemb.rentit.main.Main;

public class FreeHotelsCOMMAND implements CommandExecutor{

	private Main instance;
	
	public FreeHotelsCOMMAND(Main plugin) {
		this.instance = plugin;
		this.instance.getCommand("freehotels").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}
		
		Player p = (Player) sender;

		if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "freehotels", null)) {
			p.sendMessage(this.instance.getMessage("perm"));
			return true;
		}
		
		p.openInventory(CategoryGUI.getCategoryGUI(this.instance, RentTypes.HOTEL));
		return true;
	}
	
}
