package me.truemb.rentit.economy;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

import me.truemb.rentit.main.Main;

public class PlayerPointsEconomy extends EconomySystem{
	
	private Main instance;
    private PlayerPointsAPI ppAPI;
	
	public PlayerPointsEconomy(Main plugin) {
		this.instance = plugin;
		
		this.setupPlayerPoints();
	}

	@Override
	public boolean has(OfflinePlayer op, double cost) {
		return this.getPlayerPoints().look(op.getUniqueId()) >= cost;
	}

	@Override
	public boolean withdraw(OfflinePlayer op, double money) {
		return this.getPlayerPoints().take(op.getUniqueId(), (int) money);
	}

	@Override
	public boolean deposit(OfflinePlayer op, double money) {
		return this.getPlayerPoints().give(op.getUniqueId(), (int) money);
	}

	@Override
	public double getBalance(OfflinePlayer op) {
		return this.getPlayerPoints().look(op.getUniqueId());
	}
	
	public PlayerPointsAPI getPlayerPoints() {
		return this.ppAPI;
	}

	private boolean setupPlayerPoints() {
		if (this.instance.getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
			this.instance.getLogger().warning("PlayerPoints is missing!");
			return false;
	    }
        this.ppAPI = PlayerPoints.getInstance().getAPI();
	    this.instance.getLogger().info("PlayerPoints System was found.");
	    return this.ppAPI != null;
	}

}
