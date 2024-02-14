package me.truemb.rentit.listener;

import java.util.UUID;

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
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.gui.UserRentGUI;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.main.Main;

public class GUI_OwningListListener implements Listener{
	
	private Main instance;
	
	public GUI_OwningListListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
	}

	@EventHandler
    public void onUserListClick(InventoryClickEvent e) {
        
        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();
		
        Inventory inv = e.getClickedInventory();
        ItemStack item = e.getCurrentItem();

        InventoryHolder holder = e.getInventory().getHolder();
        
        if(holder == null)
        	return;
        
        if(!(holder instanceof GuiHolder))
        	return;
        
        GuiHolder guiHolder = (GuiHolder) holder;
        RentTypes type = guiHolder.getRentType();
        
        if(guiHolder.getGuiType() != GuiType.LIST)
        	return;

        //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
	        
	    if(inv == null)
	    	return;
	        
	    if(item == null || item.getType() == Material.AIR)
	    	return;

		PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
		if (playerHandler == null) {
			p.sendMessage(this.instance.getMessage("pleaseReconnect"));
			return;
		}
			
		if(item.isSimilar(this.instance.getMethodes().getGUIItem("owningList", "backItem"))) {
				
			p.closeInventory();
				
		}else if(this.instance.getMethodes().removeSiteKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("owningList", "nextSiteItem")) 
				|| this.instance.getMethodes().removeSiteKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("owningList", "beforeSiteItem"))) {

			ItemMeta meta = item.getItemMeta();
			NamespacedKey key = new NamespacedKey(this.instance, "Site");
			if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
					
			int site = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			inv.setContents(UserListGUI.getListGUI(this.instance, type, uuid, site).getContents());
			
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
				//OPEN RENT SETTINGS
				
				p.closeInventory();
				p.openInventory(UserRentGUI.getRentSettings(this.instance, type, id, false));
					
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
