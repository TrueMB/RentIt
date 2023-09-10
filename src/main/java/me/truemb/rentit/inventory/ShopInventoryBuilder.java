package me.truemb.rentit.inventory;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.handler.RentTypeHandler;

public class ShopInventoryBuilder {
	
	private Player player;
	private UUID targetUUID;
	
	private RentTypeHandler shopHandler;
	private ShopInventoryType type;
	private int currentSite;
	
	public ShopInventoryBuilder(Player player, RentTypeHandler shopHandler, ShopInventoryType type) {
		this.player = player;
		this.shopHandler = shopHandler;
		this.type = type;
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
		if(this.type != ShopInventoryType.ROLLBACK) {
			if(this.currentSite >= this.shopHandler.getInventories(this.type).size())
				return this;
		}else {
			if(this.currentSite >= this.shopHandler.getRollbackInventories().get(this.targetUUID != null ? this.targetUUID : this.player.getUniqueId()).size())
				return this;
		}
		
		this.currentSite++;
		this.openInventory();
		
		return this;
	}
	
	/**
	 * Sets the Target. Currently used if an Admin opens another Players Rollback Inventory
	 * 
	 * @param uuid
	 * @return
	 */
	public ShopInventoryBuilder setTarget(UUID uuid) {
		this.targetUUID = uuid;
		return this;
	}
	
	public ShopInventoryBuilder build() {
		this.openInventory();
		return this;
	}
	
	public void setSite(int currentSite) {
		this.currentSite = currentSite;
	}
	
	public int getSite() {
		return this.currentSite;
	}
	
	public RentTypeHandler getShopHandler() {
		return this.shopHandler;
	}
	
	public ShopInventoryType getType() {
		return this.type;
	}
	
	public Inventory getCurrentInventory() {
		if(this.type == ShopInventoryType.ROLLBACK) {
			return this.shopHandler.getRollbackInventory(this.targetUUID != null ? this.targetUUID : this.player.getUniqueId(), this.currentSite);
		}else
			return this.shopHandler.getInventory(this);
	}
	
	private void openInventory() {
		Inventory inv = this.getCurrentInventory();
		if(inv != null)
			this.player.openInventory(inv);
	}

}
