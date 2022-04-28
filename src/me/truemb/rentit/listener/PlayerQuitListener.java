package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.truemb.rentit.main.Main;

public class PlayerQuitListener implements Listener{

	private Main instance;

	public PlayerQuitListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		if(this.instance.playerHandlers.containsKey(uuid))
			this.instance.playerHandlers.get(uuid).exit(this.instance);
	}

}
