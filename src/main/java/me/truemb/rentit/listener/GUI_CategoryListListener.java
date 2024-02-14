package me.truemb.rentit.listener;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.CategoryGUI;
import me.truemb.rentit.gui.UserConfirmGUI;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class GUI_CategoryListListener implements Listener {

	private Main instance;

	public GUI_CategoryListListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
	public void onCatClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		
		Inventory inv = e.getClickedInventory();
		ItemStack item = e.getCurrentItem();
		
		InventoryHolder holder = e.getInventory().getHolder();
	        
	    if(holder == null)
	    	return;
	        
	    if(!(holder instanceof GuiHolder))
	    	return;
	        
	    GuiHolder guiHolder = (GuiHolder) holder;
	    RentTypes type = guiHolder.getRentType();
	        
	    if(guiHolder.getGuiType() != GuiType.CATEGORY_LIST)
	    	return;

	    //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
		        
		if(inv == null)
			return;
		        
		if(item == null || item.getType() == Material.AIR)
			return;
		
		if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "backItem"))) {
			
			p.closeInventory();
			p.openInventory(CategoryGUI.getCategoryGUI(this.instance, type));
			
		}else if(this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "nextSiteItem"))
				|| this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "beforeSiteItem"))) {

			ItemMeta meta = item.getItemMeta();
			NamespacedKey siteKey = new NamespacedKey(this.instance, "Site");
			if (!meta.getPersistentDataContainer().has(siteKey, PersistentDataType.INTEGER))
				return;
				
			int site = meta.getPersistentDataContainer().get(siteKey, PersistentDataType.INTEGER);
			
			ItemStack firstItem = p.getOpenInventory().getTopInventory().getItem(0);
			if(firstItem == null || firstItem.getType() == Material.AIR)
				return;
			
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(type, id);

			if (rentHandler == null)
				return;
			
			p.getOpenInventory().getTopInventory().setContents(CategoryGUI.getSubCategoryGUI(this.instance, type, rentHandler.getCatID(), site).getContents());
			
		}else {
			
			ItemMeta meta = item.getItemMeta();
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			
			RentTypeHandler typeHandler = this.instance.getMethodes().getTypeHandler(type, id);
			if(typeHandler == null) 
				return;
			
			if(e.isRightClick()) {
				//OPEN BUY MENU
				
				p.closeInventory();
				p.openInventory(UserConfirmGUI.getConfirmationGUI(this.instance, type, id));
				
			}else if(e.isLeftClick()) {
				//Teleport
				
				CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, typeHandler.getCatID());
				if(catHandler == null) 
					return;
				
				if(this.instance.manageFile().isSet("Options.categorySettings." + StringUtils.capitalize(type.toString().toLowerCase()) + "Category." + String.valueOf(catHandler.getCatID()) + ".teleport") &&
						!this.instance.manageFile().getBoolean("Options.categorySettings." + StringUtils.capitalize(type.toString().toLowerCase()) + "Category." + String.valueOf(catHandler.getCatID()) + ".teleport")) {
					
					p.sendMessage(this.instance.getMessage(type.toString().toLowerCase() + "TeleportNotAllowed"));
					return;
				}
				
				p.teleport(this.instance.getAreaFileManager().getAreaSpawn(type, id));
			}
		}
	}
}
