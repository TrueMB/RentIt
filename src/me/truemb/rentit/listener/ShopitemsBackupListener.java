package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.gui.RollbackGUI;
import me.truemb.rentit.main.Main;

public class ShopitemsBackupListener implements Listener {

	private Main instance;
	
	public ShopitemsBackupListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onRollbackGUIClick(InventoryClickEvent e) {
		
		Player p = (Player) e.getWhoClicked();
		UUID uuid = p.getUniqueId();
		
		if(e.getView().getTitle() == null || !e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.rollback.displayName"))))
			return;
		
		if(e.getClick() != ClickType.SHIFT_LEFT && e.getClick() != ClickType.LEFT){
			e.setCancelled(true);
			return;
		}

		if(e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) {
			e.setCancelled(true);
			return;
		}
			
		if(e.getSlot() >= 45){
			e.setCancelled(true);
			
			Inventory inv = e.getView().getTopInventory();
			ItemStack item = e.getCurrentItem();
			
			if(item == null || item.getType() == Material.AIR)
				return;
			
			ItemMeta meta = item.getItemMeta();
			
			NamespacedKey idKey = new NamespacedKey(this.instance, "ID");
			NamespacedKey siteKey = new NamespacedKey(this.instance, "Site");
			
			if (!meta.getPersistentDataContainer().has(idKey, PersistentDataType.INTEGER))
				return;
			
			int shopId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.INTEGER);
			
			if(item.isSimilar(this.instance.getMethodes().getGUIItem("rollback", "refreshItem"))) {
				
				//CALCULATE SITE
				ItemStack nextItem = inv.getItem(53);
				ItemMeta nextMeta = nextItem.getItemMeta();
				
				if (!nextMeta.getPersistentDataContainer().has(siteKey, PersistentDataType.INTEGER))
					return;
					
				int site = nextMeta.getPersistentDataContainer().get(siteKey, PersistentDataType.INTEGER);

				Inventory nextInv = this.instance.getRollbackInventoryManager().getSiteInventory(uuid, site);
				inv.setContents(nextInv.getContents());
				
			}else if(item.isSimilar(this.instance.getMethodes().getGUIItem("rollback", "returnItem"))) {
				
				//CLOSE INV
				p.closeInventory();
				
			}else if(this.instance.getMethodes().removeSiteKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("rollback", "nextSiteItem")) 
					|| this.instance.getMethodes().removeSiteKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("rollback", "beforeSiteItem"))) {

				//GO TO NEXT OR SITE BEFORE
				if (!meta.getPersistentDataContainer().has(siteKey, PersistentDataType.INTEGER))
					return;
					
				int site = meta.getPersistentDataContainer().get(siteKey, PersistentDataType.INTEGER);

				Inventory nextInv = this.instance.getRollbackInventoryManager().getSiteInventory(uuid, site);
				inv.setContents(nextInv.getContents());
			}
			
			return;
		}
		
		
		
		
	}

}
