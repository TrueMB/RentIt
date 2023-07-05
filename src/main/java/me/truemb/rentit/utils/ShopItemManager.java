package me.truemb.rentit.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.main.Main;

public class ShopItemManager {
	
	public static ItemStack createShopItem(Main plugin, ItemStack item, int shopId, double price) {

		NumberFormat formatter = new DecimalFormat("#0.00"); 
		
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		
		if(lore == null)
			lore = new ArrayList<>();

		lore.add(ChatColor.translateAlternateColorCodes('&', plugin.manageFile().getString("GUI.shopUser.loreSellItemPrice"))
				.replace("%price%", String.valueOf(formatter.format(price))));
		
		NamespacedKey key = new NamespacedKey(plugin, "price");
		meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, price);
		
		NamespacedKey idKey = new NamespacedKey(plugin, "ID");
		meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, shopId);
		
		meta.setLore(lore);
		item.setItemMeta(meta);
		
		
		return item;
	}

	public static boolean isShopItem(Main plugin, ItemStack item) {
		
		ItemMeta meta = item.getItemMeta();
		
		NamespacedKey key = new NamespacedKey(plugin, "price");
		PersistentDataContainer container = meta.getPersistentDataContainer();
		
		return container.has(key, PersistentDataType.DOUBLE);
	}
	
	public static double getPriceFromShopItem(Main plugin, ItemStack item) {
		double price = -1D;
		
		ItemMeta meta = item.getItemMeta();
		NamespacedKey key = new NamespacedKey(plugin, "price");
		PersistentDataContainer container = meta.getPersistentDataContainer();
		
		if(container.has(key , PersistentDataType.DOUBLE)) {
		   price = container.get(key, PersistentDataType.DOUBLE);
		}
		return price;
	}

	public static ItemStack editShopItemPrice(Main plugin, ItemStack item, double price) {
		
		if(!ShopItemManager.isShopItem(plugin, item))
			return item;
		
		
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		
		if(lore == null)
			lore = new ArrayList<>();
		
		lore.set(lore.size() - 1, ChatColor.translateAlternateColorCodes('&', plugin.manageFile().getString("GUI.shopUser.loreSellItemPrice"))
				.replace("%price%", String.valueOf(UtilitiesAPI.getHumanReadablePriceFromNumber(price)))); //USER DISPLAY PRICE
		
		NamespacedKey key = new NamespacedKey(plugin, "price");
		PersistentDataContainer container = meta.getPersistentDataContainer();
		if(container.has(key , PersistentDataType.DOUBLE)) {
			container.set(key, PersistentDataType.DOUBLE, price);
		}
		
		meta.setLore(lore);
		item.setItemMeta(meta);
		
		return item;
	}
	public static ItemStack removeShopItem(Main plugin, ItemStack item) {
		
		if(!ShopItemManager.isShopItem(plugin, item))
			return item;
		
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		
		if(lore == null)
			lore = new ArrayList<>();
		
		if(ShopItemManager.isShopItem(plugin, item)) {
			lore.remove(lore.size() - 1); //USER DISPLAY PRICE
			NamespacedKey key = new NamespacedKey(plugin, "price");
			NamespacedKey idKey = new NamespacedKey(plugin, "ID");
			PersistentDataContainer container = meta.getPersistentDataContainer();
			
			if(container.has(key , PersistentDataType.DOUBLE))
				container.remove(key);
				
			if(container.has(idKey , PersistentDataType.INTEGER))
				container.remove(idKey);
			
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		
		return item;
	}
}
