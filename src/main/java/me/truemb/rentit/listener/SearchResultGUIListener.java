package me.truemb.rentit.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.SearchResultGUI;
import me.truemb.rentit.gui.ShopBuyOrSell;
import me.truemb.rentit.main.Main;

public class SearchResultGUIListener implements Listener{
	
	private Main instance;
	
	public SearchResultGUIListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
	}

	@EventHandler
    public void onSearchGuiClick(InventoryClickEvent e) {
        
        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();
		
        Inventory inv = e.getClickedInventory();
        ItemStack item = e.getCurrentItem();
        
		if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.searchInventory.displayName")))) {
			
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
				
			}else if(this.instance.getMethodes().removeSiteKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("searchInventory", "nextSiteItem")) 
					|| this.instance.getMethodes().removeSiteKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("searchInventory", "beforeSiteItem"))) {

				ItemMeta meta = item.getItemMeta();
				NamespacedKey key = new NamespacedKey(this.instance, "Site");
				if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
					
				int site = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

				Material m = this.instance.search.get(uuid);
				
				if(m == null) {
					p.closeInventory();
					return;
				}
				
				HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(RentTypes.SHOP);
				
				List<Integer> foundShopIds = new ArrayList<>();
				for(int shopId : typeHash.keySet()) {
					RentTypeHandler handler = typeHash.get((Integer) shopId);
					if(handler == null)
						continue;
					
					if(handler.searchMaterial(m) > 0)
						foundShopIds.add(handler.getID());
				}
				
				inv.setContents(SearchResultGUI.getGUI(this.instance, uuid, site, foundShopIds).getContents());
			}else {
				
				//SHOP
				ItemMeta meta = item.getItemMeta();

				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				RentTypeHandler typeHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, id);
				if(typeHandler == null) return;
				
				if(e.isRightClick()) {
					
					//OPEN SHOP MENU
					p.openInventory(ShopBuyOrSell.getSelectInv(this.instance, id));
					
				}else if(e.isLeftClick()) {
					//Teleport to Shop
					
					CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, typeHandler.getCatID());
					if(catHandler == null) return;
					
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + String.valueOf(catHandler.getCatID()) + ".teleport") && 
							!this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + String.valueOf(catHandler.getCatID()) + ".teleport")) {
						
						p.sendMessage(this.instance.getMessage("shopTeleportNotAllowed"));
						return;
					}
					
					p.teleport(this.instance.getAreaFileManager().getAreaSpawn(RentTypes.SHOP, id));
				}
				
			}
			
		}
	}
}
