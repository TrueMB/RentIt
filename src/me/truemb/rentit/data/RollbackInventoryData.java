package me.truemb.rentit.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.inventory.Inventory;

public class RollbackInventoryData {
	
	//TODO dont allow multiple connections to the inventory
	
	private UUID owner;
	private List<Inventory> rollbackInv = new ArrayList<>();

}
