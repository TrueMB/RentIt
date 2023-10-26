package me.truemb.rentit.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.main.Main;

public class RollbackGUI {
	
	public static List<Inventory> getRollbackInventories(Main instance, UUID uuid, int shopId) {
		
		List<Inventory> inventories = new ArrayList<>();
		Inventory inv = null;
		
		if(uuid != null) {
			int counter = 0;
			for(ItemStack[] itemstacks : instance.getShopCacheFileManager().getShopBackupList(uuid, shopId)) {
				for(ItemStack items : itemstacks) {
					
					//NO NEED TO ADD AIR TO THE INV
					if(items == null || items.getType() == Material.AIR)
						continue;
					
					if(counter >= 45 || inv == null) {
	
						//THE ORDER OF THIS BLOCK IS IMPORTANT!
						
						int site = inventories.size() + 1;
						
						//ONLY CAN HAPPEN, IF AN INVENTORY WAS ALREADY CREATED. OTHERWISE IT WOULDNT REACH SUCH A HIGH COUNTER
						if(counter >= 45)
							inv.setItem(53, instance.getMethodes().getGUIItem("rollback", "nextSiteItem", shopId, site));
						
						//CREATE A NEW ONE (Next Site/First Site)
						inv = Bukkit.createInventory(null, 54, instance.translateHexColorCodes(instance.manageFile().getString("GUI.rollback.displayName")));
						//ADD INVENTORY TO LIST
						inventories.add(inv);
						
						//ONLY ADDED IN THE NEXT INVENTORY, IF COUNTER REACHED
						if(site > 1)
							inv.setItem(45, instance.getMethodes().getGUIItem("rollback", "beforeSiteItem", shopId, site - 1));
	
						//ADD RETURN GUI ITEMS, SINCE THIS IS ALWAYS "NEEDED"
						inv.setItem(49, instance.getMethodes().getGUIItem("rollback", "returnItem"));
						
						//RESET COUNTER
						counter = 0;
					}
					//Add saved Items to menu
					inv.setItem(counter, items);
					counter++;
				}
			}
		}
		
		// Add at least one Inventory, even if it is empty
		if(inventories.size() == 0)
			inventories.add(Bukkit.createInventory(null, 54, instance.translateHexColorCodes(instance.manageFile().getString("GUI.rollback.displayName"))));
		
		return inventories;
		
	}
}
