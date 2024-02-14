package me.truemb.rentit.listener;

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
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.main.Main;

public class GUI_CategoryListener implements Listener {

	private Main instance;

	public GUI_CategoryListener(Main plugin) {
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
	        
	    if(guiHolder.getGuiType() != GuiType.CATEGORY)
	    	return;

	    //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
		        
		if(inv == null)
			return;
		        
		if(item == null || item.getType() == Material.AIR)
			return;

		//SHOWS ALL SHOP CATEGORIES
		ItemMeta meta = item.getItemMeta();
		NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
		if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
			return;
			
		int catID = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			
		CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, catID);

		if (catHandler == null) {
			p.sendMessage(this.instance.getMessage("categoryError"));
			return;
		}

		p.closeInventory();
		p.openInventory(CategoryGUI.getSubCategoryGUI(this.instance, type, catID, 1));
		return;
		
	}
}
