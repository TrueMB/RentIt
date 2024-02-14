package me.truemb.rentit.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class SearchResultGUI {
	
	public static Inventory getGUI(Main instance, UUID uuid, int site, List<Integer> ids) {
		
		PlayerHandler playerHandler = instance.getMethodes().getPlayerHandler(uuid);
		if (playerHandler == null)
			return null;
		
		Inventory inv = Bukkit.createInventory(new GuiHolder(RentTypes.SHOP, GuiType.SEARCH), 18, instance.translateHexColorCodes(instance.manageFile().getString("GUI.searchInventory.displayName")));
		
		int size = ids.size();
				
		for(int i = 0; i < 9; i++) {
			int puf = i + 9 * (site - 1);
			if(size <= puf)
				break;
			
			int id = ids.get(puf);
			ItemStack item = SearchResultGUI.getListItem(instance, id);
			inv.setItem(i, item);
		}
		
		if(site > 1) { 
			inv.setItem(9, instance.getMethodes().getGUIItem("searchInventory", "beforeSiteItem"));
		}else if(size > 9 - 9 * (site - 1)) { 
			inv.setItem(17, instance.getMethodes().getGUIItem("searchInventory", "nextSiteItem"));
		}
		
		inv.setItem(13, instance.getMethodes().getGUIItem("searchInventory", "backItem"));
		return inv;
	}
	
	private static ItemStack getListItem(Main instance, int id) {
	    
	    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(RentTypes.SHOP, id);

		if (rentHandler == null)
			return null;

		CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

		if (catHandler == null)
			return null;
		
	    double price = catHandler.getPrice();
	    int size = catHandler.getSize();
	    String timeS = catHandler.getTime();

	    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(id);
	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		NamespacedKey key = new NamespacedKey(instance, "ID");

		ItemStack item = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI.searchInventory.items.shopItem.type").toUpperCase()));
	        
	    ItemMeta itemMeta = item.getItemMeta();
	    itemMeta.setDisplayName(instance.translateHexColorCodes(instance.manageFile().getString("GUI.searchInventory.items.shopItem.displayName")
				.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(id))
				.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
				.replaceAll("(?i)%" + "alias" + "%", alias)
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)));
	    
		itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, id);
		
		List<String> lore = new ArrayList<>();
		
		for(String s : instance.manageFile().getStringList("GUI.searchInventory.items.shopItem.lore")) {
			lore.add(instance.translateHexColorCodes(s)
					.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
					.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))
					.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
					.replaceAll("(?i)%" + "time" + "%", timeS));
		}
		itemMeta.setLore(lore);
	    item.setItemMeta(itemMeta);
	    return item;
	}
	
	

}
