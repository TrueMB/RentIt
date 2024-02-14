package me.truemb.rentit.gui;

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

public class UserConfirmGUI {
	
	public static Inventory getConfirmationGUI(Main instance, RentTypes type, int id) {
		
		String typeS = type.toString().toLowerCase();
		
		Inventory inv = Bukkit.createInventory(new GuiHolder(type, GuiType.CONFIRM).setID(id), 9, instance.translateHexColorCodes(instance.manageFile().getString("GUI." + typeS + "Confirmation.displayName")));
		
		int confirmItemSlot = instance.manageFile().getInt("GUI." + typeS + "Confirmation.items.confirmItem.slot") - 1;
		if(confirmItemSlot >= 0)
			inv.setItem(confirmItemSlot, instance.getMethodes().getGUIItem(typeS + "Confirmation", "confirmItem"));
		
		int infoItemSlot = instance.manageFile().getInt("GUI." + typeS + "Confirmation.items.infoItem.slot") - 1;
		if(infoItemSlot >= 0)
			inv.setItem(infoItemSlot, UserConfirmGUI.getInfoItem(instance, type, id));
		
		int cancelItemSlot = instance.manageFile().getInt("GUI." + typeS + "Confirmation.items.cancelItem.slot") - 1;
		if(cancelItemSlot >= 0)
			inv.setItem(cancelItemSlot, instance.getMethodes().getGUIItem(typeS + "Confirmation", "cancelItem"));
		
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
			lore.add(instance.translateHexColorCodes(s)
					.replace("%shopId%", String.valueOf(id))
					.replace("%hotelId%", String.valueOf(id))
					.replace("%alias%", alias)
					.replace("%catAlias%", catAlias)
					.replace("%price%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))
					.replace("%size%", String.valueOf(size))
					.replace("%time%", timeS));
		}
		
		ItemStack infoItem = new ItemStack(Material.valueOf(instance.manageFile().getString("GUI." + type.toString().toLowerCase() + "Confirmation.items.infoItem.type").toUpperCase()));
	        
	    ItemMeta infoItemMeta = infoItem.getItemMeta();
	    infoItemMeta.setDisplayName(instance.translateHexColorCodes(instance.manageFile().getString("GUI." + type.toString().toLowerCase() + "Confirmation.items.infoItem.displayName")));
		
	    infoItemMeta.setLore(lore);
		infoItem.setItemMeta(infoItemMeta);
	    return infoItem;
	}

}
