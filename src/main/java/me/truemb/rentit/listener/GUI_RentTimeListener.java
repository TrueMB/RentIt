package me.truemb.rentit.listener;

import java.sql.Timestamp;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.gui.UserRentGUI;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class GUI_RentTimeListener implements Listener{
	
	private Main instance;
	
	public GUI_RentTimeListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();

        Inventory inv = e.getClickedInventory();
        ItemStack item = e.getCurrentItem();

        InventoryHolder holder = e.getInventory().getHolder();
        
        if(holder == null)
        	return;
        
        if(!(holder instanceof GuiHolder))
        	return;
        
        GuiHolder guiHolder = (GuiHolder) holder;
        RentTypes type = guiHolder.getRentType();
        String typeS = StringUtils.capitalize(type.toString().toLowerCase());
        
        if(guiHolder.getGuiType() != GuiType.RENT)
        	return;

        //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
	        
	    if(inv == null)
	    	return;
	        
	    if(item == null || item.getType() == Material.AIR)
	    	return;
        
	    int id = guiHolder.getID();
	    RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(type, id);

	    if (rentHandler == null) {
			p.sendMessage(this.instance.getMessage(typeS.toLowerCase() + "DatabaseEntryMissing"));
			return;
		}
			
		int catId = rentHandler.getCatID();
			
		if(this.instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
								
			if(this.instance.manageFile().isSet("Options.categorySettings." + typeS + "Category." + catId + "." + CategorySettings.autoPaymentDisabled.toString()) 
					&& this.instance.manageFile().getBoolean("Options.categorySettings." + typeS + "Category." + catId + "." + CategorySettings.autoPaymentDisabled.toString())) {
				p.sendMessage(this.instance.getMessage("autoPaymentDisabled"));
				return;
			}
			
		    if(type == RentTypes.SHOP)
		    	this.instance.getShopsSQL().setAutoPayment(id, false);
		    else
		    	this.instance.getHotelsSQL().setAutoPayment(id, false);
			rentHandler.setAutoPayment(false);
				
			e.setCurrentItem(this.instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem", id));
			e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items." + typeS.toLowerCase() + "InfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, type, id)); //UPDATE INFO ITEM
				
		}else if(this.instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
			if(this.instance.manageFile().isSet("Options.categorySettings." + typeS + "Category." + catId + "." + CategorySettings.autoPaymentDisabled.toString()) 
					&& this.instance.manageFile().getBoolean("Options.categorySettings." + typeS + "Category." + catId + "." + CategorySettings.autoPaymentDisabled.toString())) {
				p.sendMessage(this.instance.getMessage("autoPaymentDisabled"));
				return;
			}

		    if(type == RentTypes.SHOP)
		    	this.instance.getShopsSQL().setAutoPayment(id, true);
		    else
		    	this.instance.getHotelsSQL().setAutoPayment(id, true);
			rentHandler.setAutoPayment(true);
				
			e.setCurrentItem(this.instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem", id));
			e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items." + typeS.toLowerCase() + "InfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, type, id)); //UPDATE INFO ITEM
				
		}else if(this.instance.getMethodes().getGUIItem("shopUser", "buyRentTimeItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {

			UUID uuid = rentHandler.getOwnerUUID();
			if (uuid == null)
				return;

			CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, catId);

			if (catHandler == null)
				return;
				
			double costs = catHandler.getPrice();
			String time = catHandler.getTime();
				
		    if(this.instance.getEconomySystem().has(p, costs)) {
				if(this.instance.manageFile().isSet("Options.categorySettings." + typeS + "Category." + catId + "." + CategorySettings.maxRentExtendAmount.toString())) {

					Timestamp pufTs = rentHandler.getNextPayment(); //Counts the time back
			    	int countExtendedTimes = 0;
			    	while(pufTs.after(new Timestamp(System.currentTimeMillis()))) {
			    		countExtendedTimes++;
			    		pufTs = UtilitiesAPI.getTimestampBefore(pufTs, time);
			    	}
			    	countExtendedTimes--; //So that the normal rent doesnt count
			    		
			    	if(countExtendedTimes >= this.instance.manageFile().getInt("Options.categorySettings." + typeS + "Category." + catId + "." + CategorySettings.maxRentExtendAmount.toString())) {
			    		p.sendMessage(this.instance.getMessage("maxExtendedReached"));
			    		return;
			    	}
				}
		    	
		    	//EXTEND RENT
				this.instance.getEconomySystem().withdraw(p, costs);

				Timestamp oldTs = rentHandler.getNextPayment();
			    Timestamp ts = UtilitiesAPI.addTimeToTimestamp(oldTs, time);
			    
			    if(type == RentTypes.SHOP)
			    	this.instance.getShopsSQL().setNextPayment(id, ts);
			    else
			    	this.instance.getHotelsSQL().setNextPayment(id, ts);
			    rentHandler.setNextPayment(ts);

				e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items." + typeS.toLowerCase() + "InfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, type, id)); //UPDATE INFO ITEM
	        	p.sendMessage(this.instance.getMessage(typeS.toLowerCase() + "ExtendRent"));
	        	return;
			    	
		    }else {
	        	p.sendMessage(this.instance.getMessage("notEnoughMoney")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(costs - this.instance.getEconomySystem().getBalance(p))));
	        	return;
		    }
			
		}else if(instance.getMethodes().getGUIItem("shopUser", "backItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
			p.closeInventory();
			p.openInventory(UserListGUI.getListGUI(this.instance, type, p.getUniqueId(), 1));
		}
	}
}
