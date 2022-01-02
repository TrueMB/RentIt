package me.truemb.rentit.data;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.rentit.main.Main;

public class RollbackInventoryManager {
	
	private Main instance;
	/***
		uuid = UUID of the player, who used the command
		RollbackInventoryData = Contains all the Informations about the targets Inventory
	*/
	private HashMap<UUID, RollbackInventoryData> player_inventory_data = new HashMap<>();
	
	public RollbackInventoryManager(Main plugin) {
		this.instance = plugin;
	}
	
	public RollbackInventoryData getRollbackInventoryData(UUID uuid) {
		return this.player_inventory_data.get(uuid);
	}
	
	public RollbackInventoryData createRollbackInventoryData(UUID uuid, UUID targetUUID, int shopId) {
		RollbackInventoryData data = new RollbackInventoryData(this.instance, targetUUID, shopId);
		this.player_inventory_data.put(uuid, data);
		return data;
	}
	
	public boolean isOpen(UUID targetUUID) {
		for(RollbackInventoryData data : this.player_inventory_data.values())
			if(data.getOwnerUUID().equals(targetUUID))
				return true;
		return false;
	}
	
	public void closeInventory(UUID uuid) {
		this.player_inventory_data.remove(uuid);
	}

}
