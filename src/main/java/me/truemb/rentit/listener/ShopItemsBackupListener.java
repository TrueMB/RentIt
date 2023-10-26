package me.truemb.rentit.listener;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
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
		
		if(e.getView().getTitle() == null || !e.getView().getTitle().equalsIgnoreCase(this.instance.translateHexColorCodes(this.instance.manageFile().getString("GUI.rollback.displayName"))))
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
			
			if(item == null || item.getType() == Material.AIR)
				return;
			
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
				
			}else if(isNextItem) {
				this.instance.getShopInvBuilder(uuid).nextSite();
			}else if(isBeforeItem) {
				this.instance.getShopInvBuilder(uuid).beforeSite();
			}
		}else {
			ShopInventoryBuilder builder = this.instance.getShopInvBuilder(uuid);
			
			//Check if Inventory is a Rollback Inventory
			if(builder == null || builder.getType() != ShopInventoryType.ROLLBACK)
				return;
			
			RentTypeHandler handler = builder.getShopHandler();
			UUID targetUUID = builder.getTarget() != null ? builder.getTarget() : uuid;

			HashMap<UUID, List<Inventory>> rollbackHash = handler.getRollbackInventories();
			
			//SAVE INVENTORIES IN THE FILE
			List<Inventory> inventories = rollbackHash.get(targetUUID);
			if(inventories != null) {
				instance.getShopCacheFileManager().updateShopBackup(targetUUID, handler.getID(), rollbackHash.get(targetUUID));
			}
		}
	}
	

	@EventHandler
	public void onRollbackGUIDrag(InventoryDragEvent e) {
		
		if(e.getView().getTitle() == null || !e.getView().getTitle().equalsIgnoreCase(this.instance.translateHexColorCodes(this.instance.manageFile().getString("GUI.rollback.displayName"))))
			return;
		
		e.setCancelled(true);
	}

}
