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
		
		int changeMaxSiteItemSlot = instance.manageFile().getInt("GUI.shopAdmin.items.changeMaxSiteItem.slot") - 1;
		if(changeMaxSiteItemSlot >= 0)
			inv.setItem(changeMaxSiteItemSlot, instance.getMethodes().getGUIItem("shopAdmin", "changeMaxSiteItem", shopId));
		
		int changeAliasItemSlot = instance.manageFile().getInt("GUI.shopAdmin.items.changeAliasItem.slot") - 1;
		if(changeAliasItemSlot >= 0)
			inv.setItem(changeAliasItemSlot, instance.getMethodes().getGUIItem("shopAdmin", "changeAliasItem", shopId));
		
		int changeCategoryAliasItemSlot = instance.manageFile().getInt("GUI.shopAdmin.items.changeCategoryAliasItem.slot") - 1;
		if(changeCategoryAliasItemSlot >= 0)
			inv.setItem(changeCategoryAliasItemSlot, instance.getMethodes().getGUIItem("shopAdmin", "changeCategoryAliasItem", shopId));
		
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
		
		int changeAliasItemSlot = instance.manageFile().getInt("GUI.hotelAdmin.items.changeAliasItem.slot") - 1;
		if(changeAliasItemSlot >= 0)
			inv.setItem(changeAliasItemSlot, instance.getMethodes().getGUIItem("hotelAdmin", "changeAliasItem", hotelId));
		
		int changeCategoryAliasItemSlot = instance.manageFile().getInt("GUI.hotelAdmin.items.changeCategoryAliasItem.slot") - 1;
		if(changeCategoryAliasItemSlot >= 0)
			inv.setItem(changeCategoryAliasItemSlot, instance.getMethodes().getGUIItem("hotelAdmin", "changeCategoryAliasItem", hotelId));
		
		
		return inv;
		
	}

}
