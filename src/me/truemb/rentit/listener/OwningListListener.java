package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.gui.UserRentGUI;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.main.Main;

public class OwningListListener implements Listener{
	
	private Main instance;
	
	public OwningListListener(Main plugin) {
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
        
		if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.owningList.displayNameShopList")))) {
			
			e.setCancelled(true);
	        
	        if(e.getClickedInventory() == null)
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
				inv.setContents(UserListGUI.getListGUI(this.instance, RentTypes.SHOP, uuid, site).getContents());
			}else {
				
				//SHOP
				ItemMeta meta = item.getItemMeta();

				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				
				if(e.isRightClick()) {
					
					//OPEN RENT SETTINGS
					p.closeInventory();
					//this.instance.openId.put(p.getName(), id);
					p.openInventory(UserRentGUI.getRentSettings(this.instance, RentTypes.SHOP, id, false));
					
				}else if(e.isLeftClick()) {
					//TP
					if(!this.instance.manageFile().getBoolean("Options.defaultPermissions.shop.teleport.ownings") && !p.hasPermission(this.instance.manageFile().getString("Permissions.teleport")))
						return;
					
					p.teleport(this.instance.getAreaFileManager().getAreaSpawn(RentTypes.SHOP, id));
				}
				
			}
			
			
		}else if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.owningList.displayNameHotelList")))) {

			e.setCancelled(true);
	        
	        if(e.getClickedInventory() == null)
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
				
				inv.setContents(UserListGUI.getListGUI(this.instance, RentTypes.HOTEL, uuid, site).getContents());
			}else {
				
				//SHOP
				ItemMeta meta = item.getItemMeta();

				NamespacedKey key = new NamespacedKey(instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				
				if(e.isRightClick()) {
					
					//OPEN RENT SETTINGS
					p.closeInventory();
					//this.instance.openId.put(p.getName(), id);
					p.openInventory(UserRentGUI.getRentSettings(this.instance, RentTypes.HOTEL, id, false));
					
				}else if(e.isLeftClick()) {
					//TP
					if(!this.instance.manageFile().getBoolean("Options.defaultPermissions.hotel.teleport.ownings") && !p.hasPermission(this.instance.manageFile().getString("Permissions.teleport")))
						return;
					
					p.teleport(this.instance.getAreaFileManager().getAreaSpawn(RentTypes.HOTEL, id));
				}
				
			}
			
		}
	}
}
