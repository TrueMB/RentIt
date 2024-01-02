package me.truemb.rentit.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.CategoryGUI;
import me.truemb.rentit.main.Main;

public class FreeHotelsCOMMAND extends BukkitCommand{

	private Main instance;
	
	public FreeHotelsCOMMAND(Main plugin, String cmd) {
		super(cmd, "Lists all free Hotelrooms", null, plugin.manageFile().getStringList("Options.commands.freehotels.aliases"));
		this.instance = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		
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
