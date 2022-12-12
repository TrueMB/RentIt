package me.truemb.rentit.listener;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.main.Main;

public class PlayerCommandSendListener implements Listener{
	
	private Main instance;
	
	public PlayerCommandSendListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
	}
	
	//REMOVE DISABLED COMMANDS OF LIST

	
	@EventHandler
	public void onPlayerCommandSend(PlayerCommandSendEvent e) {
		List<String> commands = new ArrayList<>(e.getCommands());
		commands.forEach(command -> {
			this.instance.manageFile().getConfigurationSection("Options.commands").getKeys(false).forEach(configCmds -> {
				if(command.equalsIgnoreCase(configCmds) || command.equalsIgnoreCase(this.instance.getDescription().getName() + ":" + configCmds)) {
					if(this.instance.manageFile().getBoolean("Options.commands." + configCmds + ".disabled"))
						e.getCommands().remove(command);
				}
			});
		});
	}
}
