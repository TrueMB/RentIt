package me.truemb.rentit.listener;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.events.ItemSellEvent;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class ItemSoldListener implements Listener {

	private Main instance;

	public ItemSoldListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
	public void onItemSold(ItemSellEvent e) {

		Player seller = e.getSeller();
		RentTypeHandler rentHandler = e.getRentTypeHandler();
		
		int shopId = rentHandler.getID();

		for (Player all : Bukkit.getOnlinePlayers()) {
			UUID uuid = all.getUniqueId();

			if (this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Sell")) 
					|| this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
				if(this.instance.getMethodes().isSettingActive(uuid, RentTypes.SHOP, shopId, Settings.shopMessaging))
					all.sendMessage(this.instance.getMessage("shopSellMessage")
							.replaceAll("(?i)%" + "type" + "%", e.getItem().getType().toString())
							.replaceAll("(?i)%" + "player" + "%", seller.getName()));
			}
		}

	}
}
