package me.truemb.rentit.gui;

import java.util.ArrayList;
import java.util.List;

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
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class UserConfirmGUI {
	
	public static Inventory getShopConfirmationGUI(Main instance, int shopId) {
		
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.shopConfirmation.displayName")));
		
		int confirmItemSlot = instance.manageFile().getInt("GUI.shopConfirmation.items.confirmItem.slot") - 1;
		if(confirmItemSlot >= 0)
			inv.setItem(confirmItemSlot, instance.getMethodes().getGUIItem("shopConfirmation", "confirmItem", shopId));
		
		int infoItemSlot = instance.manageFile().getInt("GUI.shopConfirmation.items.infoItem.slot") - 1;
		if(infoItemSlot >= 0)
			inv.setItem(infoItemSlot, UserConfirmGUI.getInfoItem(instance, RentTypes.SHOP, shopId));
		
		int cancelItemSlot = instance.manageFile().getInt("GUI.shopConfirmation.items.cancelItem.slot") - 1;
		if(cancelItemSlot >= 0)
			inv.setItem(cancelItemSlot, instance.getMethodes().getGUIItem("shopConfirmation", "cancelItem", shopId));
		
		return inv;
	}
	
	public static Inventory getHotelConfirmationGUI(Main instance, int hotelId) {
		
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.hotelConfirmation.displayName")));
		
		int confirmItemSlot = instance.manageFile().getInt("GUI.hotelConfirmation.items.confirmItem.slot") - 1;
		if(confirmItemSlot >= 0)
			inv.setItem(confirmItemSlot, instance.getMethodes().getGUIItem("hotelConfirmation", "confirmItem", hotelId));
		
		int infoItemSlot = instance.manageFile().getInt("GUI.hotelConfirmation.items.infoItem.slot") - 1;
		if(infoItemSlot >= 0)
			inv.setItem(infoItemSlot, UserConfirmGUI.getInfoItem(instance, RentTypes.HOTEL, hotelId));
		
		int cancelItemSlot = instance.manageFile().getInt("GUI.hotelConfirmation.items.cancelItem.slot") - 1;
		if(cancelItemSlot >= 0)
			inv.setItem(cancelItemSlot, instance.getMethodes().getGUIItem("hotelConfirmation", "cancelItem", hotelId));
		
		return inv;
	}
	
	private static ItemStack getInfoItem(Main instance, RentTypes type, int id) {

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
	    	    
		List<String> lore = new ArrayList<>();
		for(String s : instance.manageFile().getStringList("GUI." + type.toString().toLowerCase() + "Confirmation.items.infoItem.lore")) {
			lore.add(ChatColor.translateAlternateColorCodes('&', s)
					.replace("%shopId%", String.valueOf(id))
					.replace("%hotelId%", String.valueOf(id))
					.replace("%alias%", alias)
					.replace("%catAlias%", catAlias)
					.replace("%price%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))
					.replace("%size%", String.valueOf(size))
					.replace("%time%", timeS));
		}
		
		NamespacedKey key = new NamespacedKey(instance, "ID");
		
		ItemStack infoItem = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI." + type.toString().toLowerCase() + "Confirmation.items.infoItem.type").toUpperCase()));
	        
	    ItemMeta infoItemMeta = infoItem.getItemMeta();
	    infoItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI." + type.toString().toLowerCase() + "Confirmation.items.infoItem.displayName")));
	    
	    infoItemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, id);
		
	    infoItemMeta.setLore(lore);
		infoItem.setItemMeta(infoItemMeta);
	    return infoItem;
	}

}
