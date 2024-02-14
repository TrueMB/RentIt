package me.truemb.rentit.listener;

import java.sql.Timestamp;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class GUI_ConfirmationListener implements Listener {

	private Main instance;
	
	public GUI_ConfirmationListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
    public void onConfirmClick(InventoryClickEvent e) {
        
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
        String typeS = type.toString().toLowerCase();
        
        if(guiHolder.getGuiType() != GuiType.CONFIRM)
        	return;

        //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
	        
	    if(inv == null)
	    	return;
	        
	    if(item == null || item.getType() == Material.AIR)
	    	return;
	    
	    int id = guiHolder.getID();
	    
	    if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem(typeS + "Confirmation", "confirmItem"))) {
	        	
			PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
			if (playerHandler == null) {
				p.sendMessage(this.instance.getMessage("pleaseReconnect"));
				return;
			}

			String group = this.instance.getPermissionsAPI().getPrimaryGroup(uuid);
			int maxPossible = 0;
			for(String configGroups : this.instance.manageFile().getConfigurationSection("Options.maxPossible." + typeS).getKeys(false)) {
				if(configGroups.equalsIgnoreCase(group)) {
					maxPossible = this.instance.manageFile().getInt("Options.maxPossible." + typeS + "." + configGroups);
					break;
				}else if(configGroups.equalsIgnoreCase("default")) {
					maxPossible = this.instance.manageFile().getInt("Options.maxPossible." + typeS + "." + configGroups);
				}
			}

			if(maxPossible >= 0 && maxPossible <= playerHandler.getOwningList(type).size()) {
				p.sendMessage(this.instance.getMessage(typeS + "LimitReached"));
				return;
			}
	        	
	        RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(type, id);

			if (rentHandler == null) {
				p.sendMessage(this.instance.getMessage(typeS + "DatabaseEntryMissing"));
				return;
			}

			CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, rentHandler.getCatID());

			if (catHandler == null) {
				p.sendMessage(this.instance.getMessage("categoryError"));
				return;
			}

			//Only to disable the feature for admin shops. Normal Shops/Hotels are not affected.
			if (type == RentTypes.SHOP && rentHandler.isAdmin()) {
				p.sendMessage(this.instance.getMessage("adminshopNoSupport"));
				return;
			}
				
			UUID ownerUUID = rentHandler.getOwnerUUID();
			if(ownerUUID != null) {
				p.closeInventory();
				p.sendMessage(this.instance.getMessage(typeS + "AlreadyBought"));
				return;
			}
				
			double costs = catHandler.getPrice();
			String time = catHandler.getTime();
				
	        if(!this.instance.getEconomySystem().has(p, costs)) {
	        	p.sendMessage(this.instance.getMessage("notEnoughMoney")
	        			.replaceAll("(?i)%" + "amount" + "%", String.valueOf(costs - this.instance.getEconomySystem().getBalance(p))));
	        	return;
	        }

        	this.instance.getEconomySystem().withdraw(p, costs);
	        	
	        this.instance.getAreaFileManager().setOwner(type, id, uuid);
	        	
	        boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings." + StringUtils.capitalize(typeS) + "Category." + catHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) 
	        		? this.instance.manageFile().getBoolean("Options.categorySettings." + StringUtils.capitalize(typeS) + "Category." + catHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) : true;
	        rentHandler.setAutoPayment(autoPaymentDefault);
	        
	        if(type == RentTypes.SHOP)
	        	this.instance.getShopsSQL().setOwner(id, uuid, p.getName(), autoPaymentDefault);
	        else
	        	this.instance.getHotelsSQL().setOwner(id, uuid, p.getName(), autoPaymentDefault);
	        	
	        playerHandler.addOwningRent(type, id);
	        rentHandler.setOwner(uuid, p.getName());
	        this.instance.getMethodes().addMemberToRegion(type, id, this.instance.getAreaFileManager().getWorldFromArea(type, id), uuid);
				
			String prefix = instance.getPermissionsAPI().getPrefix(uuid);
	        
			//NPC, only for Shops
			if(type == RentTypes.SHOP) {
		        Bukkit.getScheduler().runTaskLater(this.instance, new Runnable() {
						
					@Override
					public void run() {
						if(!instance.manageFile().getBoolean("Options.disableNPC")) {
							if(instance.getNpcUtils() != null) {
								instance.getNpcUtils().spawnAndEditNPC(id, prefix, rentHandler.getOwnerName());
							}else {
								instance.getVillagerUtils().spawnVillager(id, prefix, rentHandler.getOwnerName());
							}
						}
					}
				}, 20);
			}
	        	
	       	this.instance.getMethodes().updateSign(type, id);
	        	
	        Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
	        if(type == RentTypes.SHOP)
	        	this.instance.getShopsSQL().setNextPayment(id, ts);
	        else
	        	this.instance.getHotelsSQL().setNextPayment(id, ts);
	        
	        rentHandler.setNextPayment(ts);
	        	
	        p.closeInventory();
	            
	    }else if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem(typeS + "Confirmation", "cancelItem"))){
	        	
	    	p.closeInventory();
	        	
	    }
    }
}
