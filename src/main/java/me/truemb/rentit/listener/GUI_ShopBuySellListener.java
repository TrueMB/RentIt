package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;

public class GUI_ShopBuySellListener implements Listener{
	
	private Main instance;
	
	public GUI_ShopBuySellListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
		
	}
	
	//TODO Test Old Items, if namespaces are getting removed for new items.
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();
        
        InventoryHolder holder = e.getInventory().getHolder();
        
        if(holder == null)
        	return;
        
        if(!(holder instanceof GuiHolder))
        	return;
        
        GuiHolder guiHolder = (GuiHolder) holder;
        
        if(guiHolder.getGuiType() != GuiType.SELECT)
        	return;

        //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
	
		//Farther actions need a specific Inventory to be clicked.
		if(e.getClickedInventory() == null)
			return;

		ItemStack item = e.getCurrentItem();
		
	    if(item == null || item.getType() == Material.AIR)
	        return;
		
	    int shopId = guiHolder.getID();
		//ItemMeta meta = item.getItemMeta();
		
		//TODO do this in the holder? ID should only be needed on buy. Doesn't need to be on every Item
		//if(!meta.getPersistentDataContainer().has(this.instance.idKey, PersistentDataType.INTEGER))
		//	return;
			
		//int shopId = meta.getPersistentDataContainer().get(this.instance.idKey, PersistentDataType.INTEGER);
			
		if(this.instance.getMethodes().getGUIItem("shopBuyOrSell", "sellItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);
				
			if (rentHandler == null) {
				p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
				return;
			}
			
			ShopInventoryBuilder builder = new ShopInventoryBuilder(p, rentHandler, ShopInventoryType.SELL).build();
			this.instance.setShopInvBuilder(uuid, builder);
				
		}else if(this.instance.getMethodes().getGUIItem("shopBuyOrSell", "buyItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
			
			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);
			
			if (rentHandler == null) {
				p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
				return;
			}

			ShopInventoryBuilder builder = new ShopInventoryBuilder(p, rentHandler, ShopInventoryType.BUY).build();
			this.instance.setShopInvBuilder(uuid, builder);

		}else if(this.instance.getMethodes().getGUIItem("shopBuyOrSell", "cancelItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
			p.closeInventory();
		}
	}

}
