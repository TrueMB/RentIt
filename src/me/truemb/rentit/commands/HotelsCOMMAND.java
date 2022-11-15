package me.truemb.rentit.commands;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.main.Main;

public class HotelsCOMMAND extends BukkitCommand {

	private Main instance;
	
	public HotelsCOMMAND(Main plugin) {
		super("hotels", "Lists all Hotelrooms", null, plugin.manageFile().getStringList("Options.commands.hotels.aliases"));
		this.instance = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}
		
		Player p = (Player) sender;
		
		if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotels", null)) {
			p.sendMessage(this.instance.getMessage("perm"));
			return true;
		}
		
		UUID uuid = p.getUniqueId();
		p.openInventory(UserListGUI.getListGUI(this.instance, RentTypes.HOTEL, uuid, 1));
		return true;
	}
	
	//Prevents the Player Names Completion from the Server
	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		return Collections.emptyList();
	}
	
}
