package me.truemb.rentit.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class UserShopGUI {

	public static Inventory getInventory(Main instance, ShopInventoryType type, int shopId, int site, ItemStack[] contents) {

		RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

		if (rentHandler == null)
			return null;

		CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

		if (catHandler == null)
			return null;

		int size = catHandler.getSize();
		String title = "";
		
		switch (type) {
		case BUY: {
				title = instance.manageFile().getString("GUI.shopUser.displayNameBuy");
				break;
			}
			case SELL: {
				title = instance.manageFile().getString("GUI.shopUser.displayNameSell");
				break;
			}
			case ROLLBACK: {
				title = instance.manageFile().getString("GUI.rollback.displayName");
				break;
			}
		}
		
		Inventory inv = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', title));
		
		int puf = 0;
		if (contents != null) {
			for (int i = 0; i < inv.getSize() && i < contents.length; i++) {
					
				ItemStack item = contents[i];
				if (item == null || item.getType() == Material.AIR) {
					puf++;
					continue;
				}
				
				inv.setItem(i - puf, item);
			}
		}
		return inv;
	}
}
