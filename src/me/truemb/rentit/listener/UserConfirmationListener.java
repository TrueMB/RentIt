package me.truemb.rentit.listener;

import java.sql.Timestamp;
import java.util.UUID;

import org.bukkit.Bukkit;
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

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserShopGUI;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class UserConfirmationListener implements Listener {

	private Main instance;
	
	public UserConfirmationListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
    public void onConfirmClick(InventoryClickEvent e) {
        
        Player p = (Player) e.getWhoClicked();
        UUID uuid = p.getUniqueId();
        
        if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.shopConfirmation.displayName")))) {
          
	        e.setCancelled(true);
	        
	        if(e.getClickedInventory() == null)
	        	return;
	        
	        if(e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
	        	return;

	        ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
	        
	        if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("shopConfirmation", "confirmItem"))) {
				
				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int shopId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
	        	
				PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
				if (playerHandler == null) {
					p.sendMessage(this.instance.getMessage("pleaseReconnect"));
					return;
				}

				String group = this.instance.getPermissionsAPI().getPrimaryGroup(uuid);
				int maxPossible = 0;
				for(String configGroups : this.instance.manageFile().getConfigurationSection("Options.maxPossible.shop").getKeys(false)) {
					if(configGroups.equalsIgnoreCase(group)) {
						maxPossible = this.instance.manageFile().getInt("Options.maxPossible.shop." + configGroups);
						break;
					}else if(configGroups.equalsIgnoreCase("default")) {
						maxPossible = this.instance.manageFile().getInt("Options.maxPossible.shop." + configGroups);
					}
				}

				if(maxPossible >= 0 && maxPossible <= playerHandler.getOwningList(RentTypes.SHOP).size()) {
					p.sendMessage(this.instance.getMessage("shopLimitReached"));
					return;
				}
	        	
	        	RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return;
				}
				
				UUID ownerUUID = rentHandler.getOwnerUUID();
				if(ownerUUID != null) {
					p.closeInventory();
					p.sendMessage(this.instance.getMessage("shopAlreadyBought"));
					return;
				}
				
				double costs = catHandler.getPrice();
				String time = catHandler.getTime();
				
	        	if(!this.instance.getEconomy().has(p, costs)) {
	        		p.sendMessage(this.instance.getMessage("notEnoughtMoney").replace("%amount%", String.valueOf(costs - this.instance.getEconomy().getBalance(p))));
	        		return;
	        	}

        		this.instance.getEconomy().withdrawPlayer(p, costs);
	        	
	        	this.instance.getAreaFileManager().setOwner(RentTypes.SHOP, shopId, uuid);
	        	
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catHandler.getCatID() + ".autoPaymentDefault") ? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catHandler.getCatID() + ".autoPaymentDefault") : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
	        	this.instance.getShopsSQL().setOwner(shopId, uuid, p.getName(), autoPaymentDefault);
	        	
	        	playerHandler.addOwningRent(RentTypes.SHOP, shopId);
	        	rentHandler.setOwner(uuid, p.getName());
	        	this.instance.getMethodes().addMemberToRegion(RentTypes.SHOP, shopId, this.instance.getAreaFileManager().getWorldFromArea(RentTypes.SHOP, shopId), uuid);
				
				String prefix = instance.getPermissionsAPI().getPrefix(uuid);
	        	
	        	Bukkit.getScheduler().runTaskLater(this.instance, new Runnable() {
					
					@Override
					public void run() {
						if(instance.getNpcUtils() != null)
							instance.getNpcUtils().spawnAndEditNPC(shopId, prefix, rentHandler.getOwnerUUID(), rentHandler.getOwnerName());
						else
							instance.getVillagerUtils().spawnVillager(shopId, prefix, rentHandler.getOwnerUUID(), rentHandler.getOwnerName());
					}
				}, 20);
	        	
	        	this.instance.getMethodes().updateSign(RentTypes.SHOP, shopId);
	        	
	        	Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
	        	this.instance.getShopsSQL().setNextPayment(shopId, ts);
	        	rentHandler.setNextPayment(ts);
	        	rentHandler.setSellInv(UserShopGUI.getSellInv(this.instance, shopId, null));
	        	rentHandler.setBuyInv(UserShopGUI.getBuyInv(this.instance, shopId, null));
	        	
	            p.closeInventory();
	            return;
	            
	        }else if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("shopConfirmation", "cancelItem"))){
	        	
	            p.closeInventory();
	            return;
	        	
	        }
        }else if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.hotelConfirmation.displayName")))) {
            
  	        e.setCancelled(true);
  	        
  	        if(e.getClickedInventory() == null)
  	        	return;
  	        
  	        if(e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
  	        	return;

	        ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int hotelId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
  	        
  	        if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("hotelConfirmation", "confirmItem"))) {
  	        	
				PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
				if (playerHandler == null) {
					p.sendMessage(this.instance.getMessage("pleaseReconnect"));
					return;
				}

				String group = this.instance.getPermissionsAPI().getPrimaryGroup(uuid);
				int maxPossible = 0;
				for(String configGroups : this.instance.manageFile().getConfigurationSection("Options.maxPossible.hotel").getKeys(false)) {
					if(configGroups.equalsIgnoreCase(group)) {
						maxPossible = this.instance.manageFile().getInt("Options.maxPossible.hotel." + configGroups);
						break;
					}else if(configGroups.equalsIgnoreCase("default")) {
						maxPossible = this.instance.manageFile().getInt("Options.maxPossible.hotel." + configGroups);
					}
				}
				
				if(maxPossible >= 0 && maxPossible <= playerHandler.getOwningList(RentTypes.HOTEL).size()) {
					p.sendMessage(this.instance.getMessage("hotelLimitReached"));
					return;
				}
  	        	
  	        	RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.HOTEL, hotelId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
					return;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.HOTEL, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return;
				}
				
				UUID ownerUUID = rentHandler.getOwnerUUID();
				if(ownerUUID != null) {
					p.closeInventory();
					p.sendMessage(this.instance.getMessage("hotelAlreadyBought"));
					return;
				}
					        	
  	        	double costs = catHandler.getPrice();
	        	String time = catHandler.getTime();
	        	
	        	if(!this.instance.getEconomy().has(p, costs)) {
	        		p.sendMessage(this.instance.getMessage("notEnoughtMoney").replace("%amount%", String.valueOf(costs - this.instance.getEconomy().getBalance(p))));
	        		return;
	        	}

        		this.instance.getEconomy().withdrawPlayer(p, costs);

	        	this.instance.getAreaFileManager().setOwner(RentTypes.HOTEL, hotelId, uuid);
	        	
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catHandler.getCatID() + ".autoPaymentDefault") ? this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catHandler.getCatID() + ".autoPaymentDefault") : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
	        	this.instance.getHotelsSQL().setOwner(hotelId, uuid, p.getName(), autoPaymentDefault);
	        	
	        	playerHandler.addOwningRent(RentTypes.HOTEL, hotelId);
	        	rentHandler.setOwner(uuid, p.getName());
	        	this.instance.getMethodes().addMemberToRegion(RentTypes.HOTEL, hotelId, this.instance.getAreaFileManager().getWorldFromArea(RentTypes.HOTEL, hotelId), uuid);
	        	
	        	this.instance.getMethodes().updateSign(RentTypes.HOTEL, hotelId);
	        	
	        	Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
	        	this.instance.getHotelsSQL().setNextPayment(hotelId, ts);
	        	rentHandler.setNextPayment(ts);
	        	
	            p.closeInventory();
	            return;
  	            
  	        }else if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("hotelConfirmation", "cancelItem"))){
  	            p.closeInventory();
  	            return;
  	        	
  	        }
        }
    }
}
