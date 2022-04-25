package me.truemb.rentit.listener;

import java.sql.Timestamp;
import java.util.UUID;

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
import me.truemb.rentit.gui.UserListGUI;
import me.truemb.rentit.gui.UserRentGUI;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class RentTimeClickListener implements Listener{
	
	private Main instance;
	
	public RentTimeClickListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();
        
		if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.shopUser.displayNameShopRentSettings")))) {
			
			e.setCancelled(true);
	        
	        if(e.getClickedInventory() == null)
	        	return;
	        
	        if(e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
	        	return;
	        
			ItemStack item = e.getCurrentItem();
			
			if(item == null)
				return;
						
			ItemMeta meta = item.getItemMeta();
			//int shopId = this.instance.openId.get(p.getName());
			
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int shopId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

			if (rentHandler == null) {
				p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
				return;
			}
			
			int catId = rentHandler.getCatID();
			
			if(this.instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
								
				if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catId + ".autoPaymentDisabled") && this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catId + ".autoPaymentDisabled")) {
					p.sendMessage(this.instance.getMessage("autoPaymentDisabled"));
					return;
				}
				
				this.instance.getShopsSQL().setAutoPayment(shopId, false);
				rentHandler.setAutoPayment(false);
				
				e.setCurrentItem(this.instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem", shopId));
				e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items.shopInfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, RentTypes.SHOP, shopId)); //UPDATE INFO ITEM
				
			}else if(this.instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
				if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catId + ".autoPaymentDisabled") && this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catId + ".autoPaymentDisabled")) {
					p.sendMessage(this.instance.getMessage("autoPaymentDisabled"));
					return;
				}
				
				this.instance.getShopsSQL().setAutoPayment(shopId, true);
				rentHandler.setAutoPayment(true);
				
				e.setCurrentItem(this.instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem", shopId));
				e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items.shopInfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, RentTypes.SHOP, shopId)); //UPDATE INFO ITEM
				
			}else if(this.instance.getMethodes().getGUIItem("shopUser", "buyRentTimeItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {

				UUID uuid = rentHandler.getOwnerUUID();
				if (uuid == null)
					return;

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

				if (catHandler == null)
					return;
				
				double costs = catHandler.getPrice();
				String time = catHandler.getTime();
				
		    	if(this.instance.getEconomy().has(p, costs)) {


					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catId + ".maxRentExtendAmount")) {

						Timestamp pufTs = rentHandler.getNextPayment(); //Counts the time back
			    		int countExtendedTimes = 0;
			    		while(pufTs.after(new Timestamp(System.currentTimeMillis()))) {
			    			countExtendedTimes++;
			    			pufTs = UtilitiesAPI.getTimestampBefore(pufTs, time);
			    		}
			    		countExtendedTimes--; //So that the normal rent doesnt count
			    		
			    		if(countExtendedTimes >= this.instance.manageFile().getInt("Options.categorySettings.ShopCategory." + catId + ".maxRentExtendAmount")) {
			    			p.sendMessage(this.instance.getMessage("maxExtendedReached"));
			    			return;
			    		}
					}
		    		
		    		
		    		//VERL�NGER SHOP
					this.instance.getEconomy().withdrawPlayer(p, costs);

					Timestamp oldTs = rentHandler.getNextPayment();
			    	Timestamp ts = UtilitiesAPI.addTimeToTimestamp(oldTs, time);
			    	this.instance.getShopsSQL().setNextPayment(shopId, ts);
			    	rentHandler.setNextPayment(ts);

					e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items.shopInfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, RentTypes.SHOP, shopId)); //UPDATE INFO ITEM
	        		p.sendMessage(this.instance.getMessage("shopExtendRent"));
	        		return;
			    	
		    	}else {
	        		p.sendMessage(this.instance.getMessage("notEnoughtMoney")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(costs - this.instance.getEconomy().getBalance(p))));
	        		return;
		    	}
				
			}else if(instance.getMethodes().getGUIItem("shopUser", "backItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				p.closeInventory();
				p.openInventory(UserListGUI.getListGUI(this.instance, RentTypes.SHOP, p.getUniqueId(), 1));
			}
		}else if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.shopUser.displayNameHotelRentSettings")))) {
			
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
			
			int hotelId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.HOTEL, hotelId);

			if (rentHandler == null) {
				p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
				return;
			}
			
			int catId = rentHandler.getCatID();
			
			if(this.instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
				if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catId + ".autoPaymentDisabled") && this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catId + ".autoPaymentDisabled")) {
					p.sendMessage(this.instance.getMessage("autoPaymentDisabled"));
					return;
				}
				
				this.instance.getHotelsSQL().setAutoPayment(hotelId, false);
				rentHandler.setAutoPayment(false);
				
				e.setCurrentItem(this.instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem", hotelId));
		    	e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items.hotelInfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, RentTypes.HOTEL, hotelId)); //UPDATE INFO ITEM
				
			}else if(this.instance.getMethodes().getGUIItem("shopUser", "schedulerDeactiveItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
				if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catId + ".autoPaymentDisabled") && this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catId + ".autoPaymentDisabled")) {
					p.sendMessage(this.instance.getMessage("autoPaymentDisabled"));
					return;
				}
				
				this.instance.getHotelsSQL().setAutoPayment(hotelId, true);
				rentHandler.setAutoPayment(true);
				
				e.setCurrentItem(this.instance.getMethodes().getGUIItem("shopUser", "schedulerActiveItem", hotelId));
		    	e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items.hotelInfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, RentTypes.HOTEL, hotelId)); //UPDATE INFO ITEM
				
			}else if(this.instance.getMethodes().getGUIItem("shopUser", "buyRentTimeItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				
				UUID uuid = rentHandler.getOwnerUUID();
				if (uuid == null)
					return;

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.HOTEL, rentHandler.getCatID());

				if (catHandler == null)
					return;
				
				double costs = catHandler.getPrice();
				String time = catHandler.getTime();
				
		    	if(this.instance.getEconomy().has(p, costs)) {

					if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catId + ".maxRentExtendAmount")) {
						
						Timestamp pufTs = rentHandler.getNextPayment(); //Counts the time back
			    		int countExtendedTimes = 0;
			    		while(pufTs.after(new Timestamp(System.currentTimeMillis()))) {
			    			countExtendedTimes++;
			    			pufTs = UtilitiesAPI.getTimestampBefore(pufTs, time);
			    		}
			    		countExtendedTimes--; //So that the normal rent doesnt count
			    		
			    		if(countExtendedTimes >= this.instance.manageFile().getInt("Options.categorySettings.HotelCategory." + catId + ".maxRentExtendAmount")) {
			    			p.sendMessage(this.instance.getMessage("maxExtendedReached"));
			    			return;
			    		}
					}
		    		
		    		//VERL�NGER SHOP
					this.instance.getEconomy().withdrawPlayer(p, costs);

					Timestamp oldTs = rentHandler.getNextPayment();
			    	Timestamp ts = UtilitiesAPI.addTimeToTimestamp(oldTs, time);
			    	this.instance.getHotelsSQL().setNextPayment(hotelId, ts);
			    	rentHandler.setNextPayment(ts);
			    	
			    	e.getClickedInventory().setItem(this.instance.manageFile().getInt("GUI.shopUser.items.hotelInfoItem.slot") - 1, UserRentGUI.getInfoItem(this.instance, RentTypes.HOTEL, hotelId)); //UPDATE INFO ITEM
	        		p.sendMessage(this.instance.getMessage("hotelExtendRent"));
	        		return;
		    	}else {
	        		p.sendMessage(this.instance.getMessage("notEnoughtMoney")
	        				.replaceAll("(?i)%" + "amount" + "%", String.valueOf(costs - this.instance.getEconomy().getBalance(p))));
	        		return;
		    	}
				
			}else if(instance.getMethodes().getGUIItem("shopUser", "backItem").isSimilar(this.instance.getMethodes().removeIDKeyFromItem(item))) {
				p.closeInventory();
				p.openInventory(UserListGUI.getListGUI(this.instance, RentTypes.HOTEL, p.getUniqueId(), 1));
			}
		}
	}
}
