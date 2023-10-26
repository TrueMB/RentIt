package me.truemb.rentit.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import me.truemb.rentit.main.Main;

public class ShopBuyOrSell {
	
	public static Inventory getSelectInv(Main instance, int shopId) {
		
		Inventory inv = Bukkit.createInventory(null, 9, instance.translateHexColorCodes(instance.manageFile().getString("GUI.shopBuyOrSell.displayName")));
		
		int sellItemSlot = instance.manageFile().getInt("GUI.shopBuyOrSell.items.sellItem.slot") - 1;
		if(sellItemSlot >= 0)
			inv.setItem(sellItemSlot, instance.getMethodes().getGUIItem("shopBuyOrSell", "sellItem", shopId));
		
		int cancelItemSlot = instance.manageFile().getInt("GUI.shopBuyOrSell.items.cancelItem.slot") - 1;
		if(cancelItemSlot >= 0)
			inv.setItem(cancelItemSlot, instance.getMethodes().getGUIItem("shopBuyOrSell", "cancelItem", shopId));
		
		int buyItemSlot = instance.manageFile().getInt("GUI.shopBuyOrSell.items.buyItem.slot") - 1;
		if(buyItemSlot >= 0)
			inv.setItem(buyItemSlot, instance.getMethodes().getGUIItem("shopBuyOrSell", "buyItem", shopId));
		
		return inv;
		
	}

}
