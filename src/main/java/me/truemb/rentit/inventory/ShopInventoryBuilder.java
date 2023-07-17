package me.truemb.rentit.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.handler.RentTypeHandler;

public class ShopInventoryBuilder {
	
	private Player player;
	
	private RentTypeHandler shopHandler;
	private ShopInventoryType type;
	private int currentSite;
	
	public ShopInventoryBuilder(Player p, RentTypeHandler shopHandler) {
		this.player = p;
		
		this.shopHandler = shopHandler;
		this.currentSite = 1;
	}

	public ShopInventoryBuilder beforeSite() {
		if(this.currentSite == 0)
			return this;
		
		this.currentSite--;
		this.openInventory();
		
		return this;
	}
	
	public ShopInventoryBuilder nextSite() {
		if(this.currentSite >= this.shopHandler.getInventories(this.type).size())
			return this;
		
		this.currentSite++;
		this.openInventory();
		
		return this;
	}
	
	public ShopInventoryBuilder build(ShopInventoryType type) {
		this.type = type;
		
		this.openInventory();
		
		return this;
	}
	
	public int getSite() {
		return this.currentSite;
	}
	
	private void openInventory() {
		Inventory inv = this.shopHandler.getInventory(this.type, this.currentSite);
		this.player.openInventory(inv);
	}

}
