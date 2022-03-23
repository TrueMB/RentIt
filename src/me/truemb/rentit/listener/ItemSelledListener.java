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
import me.truemb.rentit.utils.PlayerManager;

public class ItemSelledListener implements Listener {

	private Main instance;

	public ItemSelledListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
	public void onItemSelled(ItemSellEvent e) {

		Player buyer = e.getBuyer();
		RentTypeHandler rentHandler = e.getRentTypeHandler();
		
		int shopId = rentHandler.getID();

		for (Player all : Bukkit.getOnlinePlayers()) {
			UUID uuid = PlayerManager.getUUID(all);

			if (this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Sell")) 
					|| this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
				if(this.instance.getMethodes().isSettingActive(uuid, RentTypes.SHOP, shopId, Settings.shopMessaging))
					all.sendMessage(this.instance.getMessage("shopSellMessage").replace("%type%", e.getItem().getType().toString()).replace("%player%", buyer.getName()));
			}
		}

	}
}
