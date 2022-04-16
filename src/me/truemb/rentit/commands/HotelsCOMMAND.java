package me.truemb.rentit.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;

public class HotelsCOMMAND extends BukkitCommand implements TabCompleter {

	private Main instance;
	
	public HotelsCOMMAND(Main plugin) {
		super("hotels", "Lists all Hotelrooms", null, Collections.emptyList());
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
		
		UUID uuid = PlayerManager.getUUID(p);
		p.openInventory(UserListGUI.getListGUI(this.instance, RentTypes.HOTEL, uuid, 1));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return new ArrayList<>();
	}
	
}
