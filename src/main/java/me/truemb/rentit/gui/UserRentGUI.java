package me.truemb.rentit.gui;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class UserRentGUI {

	public static Inventory getRentSettings(Main instance, RentTypes type, int id, boolean skipBackItem) {
		
		RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, id);

		if (rentHandler == null)
			return null;
		
		Inventory inv = Bukkit.createInventory(new GuiHolder(type, GuiType.RENT).setID(id), 9, instance.translateHexColorCodes(instance.manageFile().getString("GUI.shopUser." + String.valueOf(type.equals(RentTypes.SHOP) ? "displayNameShopRentSettings" : "displayNameHotelRentSettings"))));
		
		int schedulerActiveItemSlot = instance.manageFile().getInt("GUI.shopUser.items.schedulerActiveItem.slot") - 1;
		int schedulerDeactiveItemSlot = instance.manageFile().getInt("GUI.shopUser.items.schedulerDeactiveItem.slot") - 1;
		boolean active = rentHandler.isAutoPayment();
		
		if(active && schedulerActiveItemSlot >= 0)
			inv.setItem(schedulerActiveItemSlot, instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem"));
		else if(schedulerDeactiveItemSlot >= 0)
			inv.setItem(schedulerDeactiveItemSlot, instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem"));
		
		int shopInfoItemSlot = instance.manageFile().getInt("GUI.shopUser.items.shopInfoItem.slot") - 1;
		if(shopInfoItemSlot >= 0)
			inv.setItem(shopInfoItemSlot, UserRentGUI.getInfoItem(instance, type, id));
		
		int buyRentTimeItemSlot = instance.manageFile().getInt("GUI.shopUser.items.buyRentTimeItem.slot") - 1;
		if(buyRentTimeItemSlot >= 0)
			inv.setItem(buyRentTimeItemSlot, instance.getMethodes().getGUIItem("shopUser", "buyRentTimeItem"));
		
		int backitemSlot = instance.manageFile().getInt("GUI.shopUser.items.backItem.slot") - 1;
		if(!skipBackItem && backitemSlot >= 0)
			inv.setItem(backitemSlot, instance.getMethodes().getGUIItem("shopUser", "backItem"));
		
		return inv;
		
	}
		
	public static ItemStack getInfoItem(Main instance, RentTypes type, int id) {

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

	    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(id);
	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	    
		List<String> lore = new ArrayList<>();
		for(String s : instance.manageFile().getStringList("GUI.shopUser.items." + type.toString().toLowerCase() + "InfoItem.lore")) {
			lore.add(instance.translateHexColorCodes(s)
					.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(id))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
					.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))
					.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
					.replaceAll("(?i)%" + "time" + "%", timeS)
					.replaceAll("(?i)%" + "rentEnd" + "%", df.format(ts))
					.replaceAll("(?i)%" + "auto" + "%", String.valueOf(active)));
		}

		ItemStack infoItem = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI.shopUser.items." + type.toString().toLowerCase() + "InfoItem.type").toUpperCase()));
	        
	    ItemMeta infoItemMeta = infoItem.getItemMeta();
	    infoItemMeta.setDisplayName(instance.translateHexColorCodes(instance.manageFile().getString("GUI.shopUser.items." + type.toString().toLowerCase() + "InfoItem.displayName")));
		
	    infoItemMeta.setLore(lore);
		infoItem.setItemMeta(infoItemMeta);
	    return infoItem;
	}
}
