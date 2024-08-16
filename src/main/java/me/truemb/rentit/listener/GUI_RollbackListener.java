package me.truemb.rentit.listener;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;

public class GUI_RollbackListener implements Listener {

	private Main instance;
	
	public GUI_RollbackListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onRollbackGUIClick(InventoryClickEvent e) {
		
		Player p = (Player) e.getWhoClicked();
		UUID uuid = p.getUniqueId();
		
        ItemStack item = e.getCurrentItem();
		ItemStack cursorItem = e.getCursor();

        InventoryHolder holder = e.getInventory().getHolder();
        
        if(holder == null)
        	return;
        
        if(!(holder instanceof GuiHolder))
        	return;
        
        GuiHolder guiHolder = (GuiHolder) holder;
        
        if(guiHolder.getGuiType() != GuiType.ROLLBACK)
        	return;

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
				this.instance.getThreadHandler().runTaskLaterSync(p, (t) -> {
					p.closeInventory();
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
		
		InventoryHolder holder = e.getInventory().getHolder();
	        
	    if(holder == null)
	    	return;
	        
	    if(!(holder instanceof GuiHolder))
	    	return;
	        
	    GuiHolder guiHolder = (GuiHolder) holder;
	        
	    if(guiHolder.getGuiType() != GuiType.ROLLBACK)
	    	return;
		
		e.setCancelled(true);
	}

}
