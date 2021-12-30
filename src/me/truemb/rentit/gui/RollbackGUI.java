package me.truemb.rentit.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.main.Main;

public class RollbackGUI {
	
	public static List<Inventory> getRollbackInventories(Main instance, UUID uuid, int shopId) {
		
		List<Inventory> inventories = new ArrayList<>();
		Inventory inv = null;
		
		int counter = 0;
		for(ItemStack[] itemstacks : instance.getShopCacheFileManager().getShopBackupList(uuid, shopId)) {
			for(ItemStack items : itemstacks) {
				
				//NO NEED TO ADD AIR TO THE INV
				if(items == null || items.getType() == Material.AIR)
					continue;
				
				if(counter >= 45 || inv == null) {
					
					//SAVE INV IN LIST
					if(inv != null) {
						int site = inventories.size() + 1;
						inv.setItem(49, instance.getMethodes().getGUIItem("rollback", "returnItem"));

						if(site > 1)
							inv.setItem(45, instance.getMethodes().getGUIItem("rollback", "beforeSiteItem", shopId, site - 1));
						if(counter >= 45)
							inv.setItem(53, instance.getMethodes().getGUIItem("rollback", "nextSiteItem", shopId, site + 1));
						
						inventories.add(inv);
					}
					
					//CREATE A NEW ONE (Next Site)
					counter = 0;
					inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.rollback.displayName")));
				}
				//Add saved Items to menu
				inv.setItem(counter, items);
				counter++;
			}
		}
		
		//TODO SAVE ON CLOSE
		//OVERWRITE TAKEN ITEMS BUT NOT OF THE NEXT SITE
		//MAYBE HASHING TO SEPERATE AND NOT ALWAYS USE THE FILE?
		
		
		return inventories;
		
	}
}
