package me.truemb.rentit.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class UserListGUI {
	
	public static Inventory getListGUI(Main instance, RentTypes type, UUID uuid, int site) {
		
		PlayerHandler playerHandler = instance.getMethodes().getPlayerHandler(uuid);
		if (playerHandler == null)
			return null;
		
		Inventory inv = Bukkit.createInventory(null, 18, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.owningList.displayName" + StringUtils.capitalize(type.toString().toLowerCase()) + "List")));
		
		List<Integer> ids = playerHandler.getOwningList(type);
		
		int size = ids.size();
				
		for(int i = 0; i < 9; i++) {
			int puf = i + 9 * (site - 1);
			if(size <= puf)
				break;
			
			int id = ids.get(puf);
			ItemStack item = UserListGUI.getListItem(instance, type, id);
			inv.setItem(i, item);
		}
		
		if(site > 1) { 
			inv.setItem(9, instance.getMethodes().getGUIItem("owningList", "beforeSiteItem"));
		}else if(size > 9 - 9 * (site - 1)) { 
			inv.setItem(17, instance.getMethodes().getGUIItem("owningList", "nextSiteItem"));
		}
		
		inv.setItem(13, instance.getMethodes().getGUIItem("owningList", "backItem"));
		return inv;
	}
	
	private static ItemStack getListItem(Main instance, RentTypes type, int id) {
	    
	    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, id);

		if (rentHandler == null)
			return null;

		CategoryHandler catHandler = instance.getMethodes().getCategory(type, rentHandler.getCatID());

		if (catHandler == null)
			return null;
		
	    double price = catHandler.getPrice();
	    int size = catHandler.getSize();
	    String timeS = catHandler.getTime();

	    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(id);
	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		NamespacedKey key = new NamespacedKey(instance, "ID");

		ItemStack item = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI.owningList.items." + type.toString().toLowerCase() + "ListItem.type").toUpperCase()));
	        
	    ItemMeta itemMeta = item.getItemMeta();
	    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.owningList.items." + type.toString().toLowerCase() + "ListItem.displayName")
				.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(id))
				.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
				.replaceAll("(?i)%" + "alias" + "%", alias)
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)));
	    
		itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, id);
		
		List<String> lore = new ArrayList<>();
		
		for(String s : instance.manageFile().getStringList("GUI.owningList.items." + type.toString().toLowerCase() + "ListItem.lore")) {
			lore.add(ChatColor.translateAlternateColorCodes('&', s)
					.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
					.replaceAll("(?i)%" + "price" + "%", String.valueOf(price))
					.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
					.replaceAll("(?i)%" + "time" + "%", timeS));
		}
		itemMeta.setLore(lore);
	    item.setItemMeta(itemMeta);
	    return item;
	}
	
	

}
