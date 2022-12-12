package me.truemb.rentit.listener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.events.ItemBuyEvent;
import me.truemb.rentit.events.ItemSellEvent;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.ShopItemManager;

public class ShopListener implements Listener {

	private Main instance;

	public ShopListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
	public void onUserClick(InventoryClickEvent e) {

		Player p = (Player) e.getWhoClicked();
		UUID uuid = p.getUniqueId();

		// ANKAUF
		if (e.getView().getTitle().startsWith(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.shopUser.displayNameSell") + " "))) {

			e.setCancelled(true);

			if (e.getClickedInventory() == null)
				return;

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
				return;

			if (!e.getClickedInventory().equals(e.getView().getTopInventory()))
				return;

			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();

			NamespacedKey key = new NamespacedKey(this.instance, "ID");

			if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;

			int shopId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

			if (rentHandler == null)
				return;

			UUID ownerUUID = rentHandler.getOwnerUUID();
			OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
			Inventory inv = e.getClickedInventory();

			NumberFormat formatter = new DecimalFormat("#0.00");

			// Owner sells Items
			if (e.isLeftClick() && !ownerUUID.equals(uuid)) {

				double itemPrice = ShopItemManager.getPriceFromShopItem(this.instance, item);
				double price;
				int amount;
				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());

				// Shiftclick buys one item, for the price of one
				if (e.isShiftClick()) {

					// ONE ITEM BUY
					amount = 1;
					price = itemPrice / item.getAmount();

				} else {

					// MULTIPLE BUY
					amount = item.getAmount();
					price = itemPrice;

				}

				if (!this.instance.getEconomySystem().has(p, price)) {
					p.sendMessage(this.instance.getMessage("notEnoughtMoney")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(formatter.format(price - this.instance.getEconomySystem().getBalance(p)))));
					return;
				}

				copyItem.setAmount(amount); // SETS HOW MUCH THE PLAYER BOUGHT

				this.instance.getEconomySystem().withdraw(p, price); // REMVOES THE MONEY FROM THE BUYER
				this.instance.getEconomySystem().deposit(owner, price); // GIVE SHOPOWNER THE MONEY

				p.getInventory().addItem(copyItem); // GIVES BUYER THE ITEM
				Bukkit.getPluginManager().callEvent(new ItemSellEvent(p, rentHandler, copyItem, price));

				if (this.instance.getChestsUtils().checkChestsInArea(shopId, copyItem)) {
					// LOOKS IN NEARBY CHESTS
					this.instance.getChestsUtils().removeItemFromChestsInArea(shopId, copyItem);

				} else {
					// CHANGES THE SHOP INV
					item.setAmount(item.getAmount() - amount); // DELETES ITEM FROM SHOP

					if (item != null && item.getAmount() > 0)
						item = ShopItemManager.editShopItemPrice(this.instance, item, itemPrice - price);

					this.instance.getShopsInvSQL().updateSellInv(shopId, inv.getContents()); // UPDATES THE ITEM IN THE DATABASE, CACHE GETS AUTOMATICLY UPDATED
				}

				this.instance.getShopsInvSQL().updateSellInv(shopId, inv.getContents());
				p.sendMessage(this.instance.getMessage("shopItemBought")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(amount))
						.replaceAll("(?i)%" + "price" + "%", String.valueOf(formatter.format(price)))
						.replaceAll("(?i)%" + "type" + "%", StringUtils.capitalize(copyItem.getType().toString())));
				return;
			} else if (e.isRightClick()) {

				// NO PERMISSIONS TO REMOVE
				if (!this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Sell"))
						&& !this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					return;
				}

				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());
				e.setCurrentItem(null);

				p.getInventory().addItem(copyItem);
				this.instance.getShopsInvSQL().updateSellInv(shopId, inv.getContents()); // UPDATES THE ITEM IN THE DATABASE, CACHE GETS AUTOMATICLY UPDATED

				p.sendMessage(this.instance.getMessage("shopItemRemoved")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount()))
						.replaceAll("(?i)%" + "type" + "%", StringUtils.capitalize(copyItem.getType().toString())));

			}

			// Owner buys Item from Player
		} else if (e.getView().getTitle().startsWith(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI.shopUser.displayNameBuy") + " "))) {

			e.setCancelled(true);

			if (e.getClickedInventory() == null)
				return;

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
				return;

			if (!e.getClickedInventory().equals(e.getView().getTopInventory()))
				return;

			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();

			NamespacedKey key = new NamespacedKey(this.instance, "ID");

			if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;

			int shopId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

			if (rentHandler == null)
				return;

			UUID ownerUUID = rentHandler.getOwnerUUID();
			OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
			Inventory inv = e.getClickedInventory();

			NumberFormat formatter = new DecimalFormat("#0.00");

			// Verkaufen
			if (e.isLeftClick() && !ownerUUID.equals(uuid)) {
				double itemPrice = ShopItemManager.getPriceFromShopItem(this.instance, item);
				double price;
				int amount;
				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());

				// CHECK IF PLAYER GOTS THE ITEM IN THE AMOUNT
				int targetItemAmount = 0;

				for (ItemStack items : p.getInventory().getContents())
					if (items != null && items.isSimilar(copyItem))
						targetItemAmount += items.getAmount();

				if (targetItemAmount <= 0) {
					p.sendMessage(this.instance.getMessage("notEnoughOwningItems")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(1)));
					return;
				}
				
				// Shiftclick buys one item, for the price of one
				if (e.isShiftClick()) {

					// ONE ITEM BUY
					amount = 1;
					price = itemPrice / item.getAmount();

				} else if (targetItemAmount >= item.getAmount()){

					// MULTIPLE BUY
					amount = item.getAmount();
					price = itemPrice;

				} else {

					// REST BUY
					amount = targetItemAmount;
					price = itemPrice / item.getAmount() * targetItemAmount;

				}


				if (!this.instance.getEconomySystem().has(owner, price)) {
					p.sendMessage(this.instance.getMessage("notEnoughtMoneyOwner"));
					return;
				}

				copyItem.setAmount(amount); // SETS HOW MUCH THE PLAYER SELLED

				if (this.instance.getChestsUtils().checkForSpaceInArea(shopId, copyItem)) {

					// LOOKS IN NEARBY CHESTS
					this.instance.getChestsUtils().addItemToChestsInArea(shopId, copyItem);
					this.instance.getMethodes().removeItemFromPlayer(p, copyItem);

					this.instance.getEconomySystem().deposit(p, price); // REMVOES THE MONEY FROM THE BUYER
					this.instance.getEconomySystem().withdraw(owner, price); // GIVE SHOPOWNER THE MONEY

					Bukkit.getPluginManager().callEvent(new ItemBuyEvent(p, rentHandler, copyItem, price));

				} else {
					p.sendMessage(instance.getMessage("notEnoughtSpace"));
					return;
				}

				this.instance.getShopsInvSQL().updateBuyInv(shopId, inv.getContents());
				p.sendMessage(this.instance.getMessage("shopItemSelled")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(amount))
						.replaceAll("(?i)%" + "price" + "%", String.valueOf(formatter.format(price)))
						.replaceAll("(?i)%" + "type" + "%", StringUtils.capitalize(copyItem.getType().toString())));
				return;
			} else if (e.isRightClick()) {

				// NO PERMISSIONS TO REMOVE
				if (!this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Buy"))
						&& !this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					return;
				}

				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());
				e.setCurrentItem(null);
				this.instance.getShopsInvSQL().updateBuyInv(shopId, inv.getContents());

				p.sendMessage(this.instance.getMessage("shopItemRemoved")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount()))
						.replaceAll("(?i)%" + "type" + "%", StringUtils.capitalize(copyItem.getType().toString())));

			}
		} else if (e.getView().getTitle().startsWith("BACKUP SHOP ")) {
			e.setCancelled(true);

			ItemStack item = e.getCurrentItem();

			if (item != null)
				p.getInventory().addItem(ShopItemManager.removeShopItem(this.instance, item));

			e.setCurrentItem(null);
		}
	}
}
