package me.truemb.rentit.economy;

import org.bukkit.OfflinePlayer;

public abstract class EconomySystem {
	
	public abstract boolean has(OfflinePlayer op, double cost);
	
	public abstract boolean withdraw(OfflinePlayer op, double money);
	
	public abstract boolean deposit(OfflinePlayer op, double money);

	public abstract double getBalance(OfflinePlayer op);

}
