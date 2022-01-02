package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.data.RollbackInventoryData;
import me.truemb.rentit.main.Main;

public class ShopItemsBackupListener implements Listener {

	private Main instance;
	
	public ShopItemsBackupListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onRollbackGUIClick(InventoryClickEvent e) {
		
		Player p = (Player) e.getWhoClicked();
		UUID uuid = p.getUniqueId();
		
		if(e.getView().getTitle() == null || !e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.rollback.displayName"))))
			return;
		
		ItemStack item = e.getCurrentItem();
		ItemStack cursorItem = e.getCursor();

		//NO ITEMS FROM BOTTOM INVENTORY
		if((item != null && item.getType() != Material.AIR) && (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory()))) {
			e.setCancelled(true);
			return;
		}
		
		//ONLY SHIFT AND LEFT CLICK
		if(e.getClick() != ClickType.SHIFT_LEFT && e.getClick() != ClickType.LEFT){
			e.setCancelled(true);
			return;
		}
			
		if(e.getSlot() >= 45){
			e.setCancelled(true);

			//REMOVE ITEM FIRST
			if(cursorItem != null && cursorItem.getType() != Material.AIR)
				return;
			
			Inventory inv = e.getView().getTopInventory();
			
			if(item == null || item.getType() == Material.AIR)
				return;
			
			ItemMeta meta = item.getItemMeta();
			
			NamespacedKey siteKey = new NamespacedKey(this.instance, "Site");
			
			
			boolean isNextItem = this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("rollback", "nextSiteItem"));
			boolean isBeforeItem = this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("rollback", "beforeSiteItem"));
			
			if(item.isSimilar(this.instance.getMethodes().getGUIItem("rollback", "returnItem"))) {

				//CLOSE INV - Delay so that the event gets canceled
				Bukkit.getScheduler().runTaskLater(this.instance, new Runnable() {
					
					@Override
					public void run() {
						p.closeInventory();
					}
				}, 2);
				
			}else if(isNextItem || isBeforeItem) {

				//GO TO NEXT OR SITE BEFORE
				if (!meta.getPersistentDataContainer().has(siteKey, PersistentDataType.INTEGER))
					return;
					
				int site = meta.getPersistentDataContainer().get(siteKey, PersistentDataType.INTEGER);

				RollbackInventoryData data = this.instance.getRollbackInventoryManager().getRollbackInventoryData(uuid);
				Inventory nextInv = data.getSiteInventory(site);
				
				if(nextInv == null)
					return;
				
				data.getSiteInventory(data.getCurrentSite()).setContents(inv.getContents()); //SET CHANGES
				data.setCurrentSite(site);
				
				inv.setContents(nextInv.getContents());
			}
		}
	}
	

	@EventHandler
	public void onRollbackGUIDrag(InventoryDragEvent e) {
		
		if(e.getView().getTitle() == null || !e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.rollback.displayName"))))
			return;
		
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		
		Player p = (Player) e.getPlayer();
		UUID uuid = p.getUniqueId();
		Inventory inv = e.getInventory();
		
		RollbackInventoryData data = this.instance.getRollbackInventoryManager().getRollbackInventoryData(uuid);
		
		if(data == null)
			return;

		data.getSiteInventory(data.getCurrentSite()).setContents(inv.getContents()); //SET CHANGES
		
		//SAVE INVENTORIES IN THE FILE
		this.instance.getShopCacheFileManager().updateShopBackup(data.getOwnerUUID(), data.getShopId(), data.getRollbackInventories());
		
		//CLOSE OLD DATA AND MAKE THE INVENTORY OPENABLE
		this.instance.getRollbackInventoryManager().closeInventory(uuid);
		
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		
		Player p = (Player) e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		RollbackInventoryData data = this.instance.getRollbackInventoryManager().getRollbackInventoryData(uuid);
		
		if(data == null)
			return;

		Inventory inv = p.getOpenInventory().getTopInventory();
		
		data.getSiteInventory(data.getCurrentSite()).setContents(inv.getContents()); //SET CHANGES
		
		//SAVE INVENTORIES IN THE FILE
		this.instance.getShopCacheFileManager().updateShopBackup(data.getOwnerUUID(), data.getShopId(), data.getRollbackInventories());
		
		//CLOSE OLD DATA AND MAKE THE INVENTORY OPENABLE
		this.instance.getRollbackInventoryManager().closeInventory(uuid);
	}

}
