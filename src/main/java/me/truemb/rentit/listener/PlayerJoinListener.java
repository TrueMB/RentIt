package me.truemb.rentit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.main.Main;

public class PlayerJoinListener implements Listener{

	private Main instance;

	public PlayerJoinListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		
		Player p = e.getPlayer();
		
		PlayerHandler playerHandler = new PlayerHandler(p);
		playerHandler.init(this.instance);
	}

}
