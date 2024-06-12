package me.truemb.rentit.listener;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.events.ItemBuyEvent;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class ItemBoughtListener implements Listener {

	private Main instance;
	
	public ItemBoughtListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
    public void onItemBought(ItemBuyEvent e) {
		
		Player buyer = e.getBuyer();
		RentTypeHandler rentHandler = e.getRentTypeHandler();
		
		ItemStack item = e.getItem();
		int shopId = rentHandler.getID();
		double price = e.getPrice();
		
		this.instance.getRILogger().getLogger().info("Player " + buyer.getName() + " bought " + item.toString() + " from Shop: " + String.valueOf(shopId) + " for " + String.valueOf(price) + "$.");

		for (Player all : Bukkit.getOnlinePlayers()) {
			UUID uuid = all.getUniqueId();

			if (this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Sell")) 
					|| this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
				if(this.instance.getMethodes().isSettingActive(uuid, RentTypes.SHOP, shopId, Settings.shopMessaging)){
					
					String type = StringUtils.capitalize(item.getType().toString());
					String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : type;
					
					all.sendMessage(this.instance.getMessage("shopBuyMessage")
							.replaceAll("(?i)%" + "itemname" + "%", itemName)
							.replaceAll("(?i)%" + "type" + "%", type)
							.replaceAll("(?i)%" + "player" + "%", buyer.getName()));
				}
			}
		}
    }
}
