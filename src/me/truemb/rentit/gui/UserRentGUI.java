package me.truemb.rentit.gui;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

public class UserRentGUI {
	

	public static Inventory getRentSettings(Main instance, RentTypes type, int id, boolean skipBackItem) {
		
		RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, id);

		if (rentHandler == null)
			return null;
		
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.shopUser." + String.valueOf(type.equals(RentTypes.SHOP) ? "displayNameShopRentSettings" : "displayNameHotelRentSettings"))));
		
		int schedulerActiveItemSlot = instance.manageFile().getInt("GUI.shopUser.items.schedulerActiveItem.slot") - 1;
		int schedulerDeactiveItemSlot = instance.manageFile().getInt("GUI.shopUser.items.schedulerDeactiveItem.slot") - 1;
		boolean active = rentHandler.isAutoPayment();
		
		if(active && schedulerActiveItemSlot >= 0)
			inv.setItem(schedulerActiveItemSlot, instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem", id));
		else if(schedulerDeactiveItemSlot >= 0)
			inv.setItem(schedulerDeactiveItemSlot, instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem", id));
		
		int shopInfoItemSlot = instance.manageFile().getInt("GUI.shopUser.items.shopInfoItem.slot") - 1;
		if(shopInfoItemSlot >= 0)
			inv.setItem(shopInfoItemSlot, UserRentGUI.getInfoItem(instance, type, id));
		
		int buyRentTimeItemSlot = instance.manageFile().getInt("GUI.shopUser.items.buyRentTimeItem.slot") - 1;
		if(buyRentTimeItemSlot >= 0)
			inv.setItem(buyRentTimeItemSlot, instance.getMethodes().getGUIItem("shopUser", "buyRentTimeItem", id));
		
		int backitemSlot = instance.manageFile().getInt("GUI.shopUser.items.backItem.slot") - 1;
		if(!skipBackItem && backitemSlot >= 0)
			inv.setItem(backitemSlot, instance.getMethodes().getGUIItem("shopUser", "backItem", id));
		
		return inv;
		
	}
		
	public static ItemStack getInfoItem(Main instance, RentTypes type, int id) {
		ItemStack acceptItem = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI.shopUser.items.shopInfoItem.type").toUpperCase()));
	        
	    ItemMeta acceptItemMeta = acceptItem.getItemMeta();
	    acceptItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.shopUser.items.shopInfoItem.displayName")));

	    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, id);

		if (rentHandler == null)
			return null;

		CategoryHandler catHandler = instance.getMethodes().getCategory(type, rentHandler.getCatID());

		if (catHandler == null)
			return null;
		
	    double price = catHandler.getPrice();
	    int size = catHandler.getSize();
	    String timeS = catHandler.getTime();
	    Timestamp ts = rentHandler.getNextPayment();
		boolean active = rentHandler.isAutoPayment();

		DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	    
		List<String> lore = new ArrayList<>();
		for(String s : instance.manageFile().getStringList("GUI.shopUser.items.shopInfoItem.lore")) {
			lore.add(ChatColor.translateAlternateColorCodes('&', s)
					.replace("%shopId%", String.valueOf(id))
					.replace("%hotelId%", String.valueOf(id))
					.replace("%price%", String.valueOf(price))
					.replace("%size%", String.valueOf(size))
					.replace("%rentEnd%", df.format(ts))
					.replace("%auto%", String.valueOf(active))
					.replace("%time%", timeS));
		}

		NamespacedKey key = new NamespacedKey(instance, "ID");
		acceptItemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, id);
		
		acceptItemMeta.setLore(lore);
	    acceptItem.setItemMeta(acceptItemMeta);
	    return acceptItem;
	}
}
