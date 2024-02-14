package me.truemb.rentit.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class CategoryGUI {

	public static Inventory getCategoryGUI(Main instance, RentTypes type) {

		String typeS = StringUtils.capitalize(type.toString().toLowerCase());
		Inventory inv = Bukkit.createInventory(new GuiHolder(type, GuiType.CATEGORY), instance.manageFile().getInt("GUI.category" + typeS + ".invSize"), instance.translateHexColorCodes(instance.manageFile().getString("GUI.category" + typeS + ".displayName")));

		for (String itemPath : instance.manageFile().getConfigurationSection("GUI.category" + typeS + ".items").getKeys(false)) {
			int slot = instance.manageFile().getInt("GUI.category" + typeS + ".items." + itemPath + ".slot") - 1;
			if(slot >= 0)
				inv.setItem(slot, instance.getMethodes().getGUIItem("category" + typeS, itemPath, instance.manageFile().getInt("GUI.category" + typeS + ".items." + itemPath + ".categoryID")));
		}
		
		return inv;
	}

	public static Inventory getSubCategoryGUI(Main instance, RentTypes type, int catID, int site) {

		String typeS = StringUtils.capitalize(type.toString().toLowerCase());

		Inventory inv = Bukkit.createInventory(new GuiHolder(type, GuiType.CATEGORY_LIST), 18, instance.translateHexColorCodes(instance.manageFile().getString("GUI.categorySub.displayName" + typeS)));

		HashMap<Integer, RentTypeHandler> hash = new HashMap<>();
		if(instance.rentTypeHandlers.containsKey(type))
			hash = instance.rentTypeHandlers.get(type);
		
		List<Integer> ids = new ArrayList<>();
		for(int id : hash.keySet()) {
			RentTypeHandler rentHandler = hash.get(id);
			if(rentHandler.getOwnerUUID() == null && rentHandler.getCatID() == catID)
				ids.add(id);
		}
		
		int size = ids.size();
		
		for(int i = 0; i < 9; i++) {
			int puf = i + 9 * (site - 1);
			if(size <= puf)
				break;
			
			int id = ids.get(puf);
			ItemStack item = CategoryGUI.getListItem(instance, type, id);
			inv.setItem(i, item);
		}
		
		if(site > 1) { 
			inv.setItem(9, instance.getMethodes().getGUIItem("categorySub", "beforeSiteItem", catID, site - 1));
		}else if(size > 9 - 9 * (site - 1)) { 
			inv.setItem(17, instance.getMethodes().getGUIItem("categorySub", "nextSiteItem", catID, site + 1));
		}
		
		inv.setItem(13, instance.getMethodes().getGUIItem("categorySub", "backItem"));
		
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
	    
	    if(timeS == null)
	    	timeS = "!ERR!";
	    	    
		List<String> lore = new ArrayList<>();
		
		NamespacedKey key = new NamespacedKey(instance, "ID");
		
		for(String s : instance.manageFile().getStringList("GUI.categorySub.items." + type.toString().toLowerCase() + "ListItem.lore")) {

			lore.add(instance.translateHexColorCodes(s)
					.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
					.replaceAll("(?i)%" + "price" + "%", String.valueOf(price))
					.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
					.replaceAll("(?i)%" + "time" + "%", timeS));
		}

		ItemStack item = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI.categorySub.items." + type.toString().toLowerCase() + "ListItem.type").toUpperCase()));
	        
	    ItemMeta itemMeta = item.getItemMeta();
	    itemMeta.setDisplayName(instance.translateHexColorCodes(instance.manageFile().getString("GUI.categorySub.items." + type.toString().toLowerCase() + "ListItem.displayName")
				.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(id))
				.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
				.replaceAll("(?i)%" + "alias" + "%", alias)
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)));
	    
		itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, id);
		
		itemMeta.setLore(lore);
	    item.setItemMeta(itemMeta);
	    return item;
	}
}
