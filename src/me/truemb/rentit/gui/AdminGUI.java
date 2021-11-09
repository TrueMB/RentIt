package me.truemb.rentit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import me.truemb.rentit.main.Main;

public class AdminGUI {
	
	public static Inventory getAdminShopGui(Main instance, int shopId) {
		
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.shopAdmin.displayName")));
		
		int changeTimeItemSlot = instance.manageFile().getInt("GUI.shopAdmin.items.changeTimeItem.slot") - 1;
		if(changeTimeItemSlot >= 0)
			inv.setItem(changeTimeItemSlot, instance.getMethodes().getGUIItem("shopAdmin", "changeTimeItem", shopId));
		
		int changePriceItemSlot = instance.manageFile().getInt("GUI.shopAdmin.items.changePriceItem.slot") - 1;
		if(changePriceItemSlot >= 0)
			inv.setItem(changePriceItemSlot, instance.getMethodes().getGUIItem("shopAdmin", "changePriceItem", shopId));
		
		int changeSizeItemSlot = instance.manageFile().getInt("GUI.shopAdmin.items.changeSizeItem.slot") - 1;
		if(changeSizeItemSlot >= 0)
			inv.setItem(changeSizeItemSlot, instance.getMethodes().getGUIItem("shopAdmin", "changeSizeItem", shopId));
		
		return inv;
		
	}
	
	public static Inventory getAdminHotelGui(Main instance, int hotelId) {
		
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.hotelAdmin.displayName")));

		int changeTimeItemSlot = instance.manageFile().getInt("GUI.hotelAdmin.items.changeTimeItem.slot") - 1;
		if(changeTimeItemSlot >= 0)
			inv.setItem(changeTimeItemSlot, instance.getMethodes().getGUIItem("hotelAdmin", "changeTimeItem", hotelId));
		
		int changePriceItemSlot = instance.manageFile().getInt("GUI.hotelAdmin.items.changePriceItem.slot") - 1;
		if(changePriceItemSlot >= 0)
			inv.setItem(changePriceItemSlot, instance.getMethodes().getGUIItem("hotelAdmin", "changePriceItem", hotelId));
		
		
		return inv;
		
	}

}
