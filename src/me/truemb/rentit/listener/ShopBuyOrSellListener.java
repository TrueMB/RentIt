package me.truemb.rentit.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class ShopBuyOrSellListener implements Listener{
	
	private Main instance;
	
	public ShopBuyOrSellListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
		
	}
	
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();
        
		if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("GUI.shopBuyOrSell.displayName")))) {

			e.setCancelled(true);
			
	        if(e.getClickedInventory() == null)
	        	return;
	        
	        if(e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
	        	return;
			
			ItemStack item = e.getCurrentItem();
			
			if(item == null)
				return;
						
			ItemMeta meta = item.getItemMeta();
			
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int shopId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			
			if(this.instance.getMethodes().getGUIItem("shopBuyOrSell", "sellItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return;
				}
				
				p.openInventory(rentHandler.getSellInv());
				
			}else if(this.instance.getMethodes().getGUIItem("shopBuyOrSell", "buyItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return;
				}
				
				p.openInventory(rentHandler.getBuyInv());

			}else if(this.instance.getMethodes().getGUIItem("shopBuyOrSell", "cancelItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				p.closeInventory();
			}
		}
	}

}
