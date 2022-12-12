package me.truemb.rentit.data;

import java.util.List;
import java.util.UUID;

import org.bukkit.inventory.Inventory;

import me.truemb.rentit.gui.RollbackGUI;
import me.truemb.rentit.main.Main;

public class RollbackInventoryData {
	
	private Main instance;
	
	private UUID ownerUUID;
	private int shopId;
	private int currentSite;
	
	private List<Inventory> rollbackInv;
	
	public RollbackInventoryData(Main plugin, UUID ownerUUID, int shopId) {
		
		this.instance = plugin;
		
		this.ownerUUID = ownerUUID;
		this.shopId = shopId;
		
		this.rollbackInv = RollbackGUI.getRollbackInventories(this.instance, this.ownerUUID, this.shopId);
	}
	
	public Inventory getSiteInventory(int site) {
		if(site > this.rollbackInv.size())
			return null;
		
		return this.rollbackInv.get(site - 1);
	}
	
	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}
	
	public int getShopId() {
		return this.shopId;
	}
	
	public List<Inventory> getRollbackInventories(){
		return this.rollbackInv;
	}

	public int getCurrentSite() {
		return currentSite;
	}

	public void setCurrentSite(int currentSite) {
		this.currentSite = currentSite;
	}

}
