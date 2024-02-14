package me.truemb.rentit.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;

public class UserShopGUI {

	public static Inventory getInventory(Main instance, ShopInventoryBuilder builder) {

		RentTypeHandler rentHandler = builder.getShopHandler();

		if (rentHandler == null)
			return null;

		CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

		if (catHandler == null)
			return null;

		int size = catHandler.getSize();
		String title = "";
		
		switch (builder.getType()) {
			case BUY: {
				title = instance.manageFile().getString("GUI.ShopBuyAndSell.displayNameBuy");
				break;
			}
			case SELL: {
				title = instance.manageFile().getString("GUI.ShopBuyAndSell.displayNameSell");
				break;
			}
			case ROLLBACK: {
				//Is not called from this class
				break;
			}
		}
		
		int shopId = rentHandler.getID();
		int site = builder.getSite();
		Inventory inv = rentHandler.getInventory(builder); //Uses the builder current Site
		boolean hasNextSite = rentHandler.getInventories(builder.getType()).size() > site;
		boolean multiSite = catHandler.getMaxSite() > 1;
		
		//Inventory not found
		if(inv == null) {
			inv = Bukkit.createInventory(new GuiHolder(RentTypes.SHOP, GuiType.SHOP).setShopInvType(builder.getType()).setID(shopId), size + (multiSite ? 9 : 0), instance.translateHexColorCodes(title));
			rentHandler.setInventory(builder.getType(), site, inv); //Links the Inventory; No need to wait for items, since no player could have opened the Inventory
		}
		
		ItemStack[] contents = inv.getContents();
		
		int puf = 0;
		if (contents != null) {
			for (int i = 0; i < inv.getSize() && i < contents.length; i++) {
					
				ItemStack item = contents[i];
				if (item == null || item.getType() == Material.AIR || item.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING)) {
					puf++;
					continue;
				}
				
				inv.setItem(i - puf, item);
			}
		}
		
		//ONLY ADDED IN THE NEXT INVENTORY, IF COUNTER REACHED
		if(multiSite) {
			int start = inv.getSize() - 9;
			for(int i = start; i < inv.getSize(); i++)
				inv.setItem(i, instance.getMethodes().getGUIItem("ShopBuyAndSell", "placeholderItem"));
			
			if(site > 1)
				inv.setItem(start, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem"));
				
			if(hasNextSite)
				inv.setItem(start + 8, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem"));
			
			inv.setItem(start + 4, instance.getMethodes().getGUIItem("ShopBuyAndSell", "returnItem"));
		}
		
		return inv;
	}
}
