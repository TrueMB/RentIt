package me.truemb.rentit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;

public class ShopsCOMMAND implements CommandExecutor, TabCompleter{

	private Main instance;
	
	public ShopsCOMMAND(Main plugin) {
		this.instance = plugin;
		this.instance.getCommand("shops").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}
		
		Player p = (Player) sender;

		if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shops", null)) {
			p.sendMessage(this.instance.getMessage("perm"));
			return true;
		}
		
		UUID uuid = PlayerManager.getUUID(p);
		p.openInventory(UserListGUI.getListGUI(this.instance, RentTypes.SHOP, uuid, 1));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return new ArrayList<>();
	}
	
}
