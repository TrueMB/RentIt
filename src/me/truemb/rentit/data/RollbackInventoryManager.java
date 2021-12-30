package me.truemb.rentit.data;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.inventory.Inventory;

import me.truemb.rentit.gui.RollbackGUI;
import me.truemb.rentit.main.Main;

public class RollbackInventoryManager {
	
	private Main instance;
	
	/***
		uuid = UUID of the target Rollback Inventory
		Integer = Site of the Inventory -> if multiple chests were in the shop
		Inventory = Tthe Inventory
	*/
	private HashMap<UUID, HashMap<Integer, Inventory>> player_site_inventories = new HashMap<>();
	
	public RollbackInventoryManager(Main plugin) {
		this.instance = plugin;
	}
	
	public Inventory getSiteInventory(UUID uuid, int site) {
		
		if(!this.player_site_inventories.containsKey(uuid))
			return null;
		
		HashMap<Integer, Inventory> hash = this.player_site_inventories.get(uuid);
		
		if(hash == null || hash.containsKey(site))
			return null;
		
		return hash.get(site);
	}
	
	public Inventory getMainInventory(UUID uuid, int shopId) {
		List<Inventory> inventories = RollbackGUI.getRollbackInventories(this.instance, uuid, shopId);
		
		if(inventories.size() <= 0)
			return null;

		HashMap<Integer, Inventory> hash = new HashMap<>();
		
		for(int i = 0; i < inventories.size(); i++) {
			hash.put(i + 1, inventories.get(i));
		}
		
		this.player_site_inventories.put(uuid, hash);
		
		return inventories.get(0);
	}

}
