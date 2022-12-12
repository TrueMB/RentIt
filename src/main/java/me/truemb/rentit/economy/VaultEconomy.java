package me.truemb.rentit.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.truemb.rentit.main.Main;
import net.milkbowl.vault.economy.Economy;

public class VaultEconomy extends EconomySystem{
	
	private Main instance;
	private Economy econ;
	
	public VaultEconomy(Main plugin) {
		this.instance = plugin;
		
		this.setupEconomy();
	}

	@Override
	public boolean has(OfflinePlayer op, double cost) {
		return this.getEconomy().has(op, cost);
	}

	@Override
	public boolean withdraw(OfflinePlayer op, double money) {
		return this.getEconomy().withdrawPlayer(op, money).transactionSuccess();
	}

	@Override
	public boolean deposit(OfflinePlayer op, double money) {
		return this.getEconomy().depositPlayer(op, money).transactionSuccess();
	}

	@Override
	public double getBalance(OfflinePlayer op) {
		return this.getEconomy().getBalance(op);
	}
	
	public Economy getEconomy() {
		return this.econ;
	}

	private boolean setupEconomy() {
		if (this.instance.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.instance.getLogger().warning("Vault is missing!");
			return false;
	    }
	    RegisteredServiceProvider<Economy> rsp = this.instance.getServer().getServicesManager().getRegistration(Economy.class);
	    if (rsp == null || rsp.getProvider() == null) {
	    	this.instance.getLogger().warning("An Economy Plugin is missing!");
	    	return false;
	    }
	    this.instance.getLogger().info(rsp.getPlugin().getName() + " Economy System was found.");
	    this.econ = rsp.getProvider();
	    return this.econ != null;
	}

}
