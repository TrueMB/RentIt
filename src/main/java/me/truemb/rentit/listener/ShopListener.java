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
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.events.ItemBuyEvent;
import me.truemb.rentit.events.ItemSellEvent;
import me.truemb.rentit.gui.ShopBuyOrSell;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.ShopItemManager;
import me.truemb.rentit.utils.UtilitiesAPI;

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
		if (e.getView().getTitle().startsWith(ChatColor.translateAlternateColorCodes('&',
				this.instance.manageFile().getString("GUI.ShopBuyAndSell.displayNameSell")))) {

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

			if (this.checkSpecialItem(p, item))
				return;

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

				// Player has space in the Inventory?
				int freeSpace = 0;
				for (ItemStack items : p.getInventory().getStorageContents()) {
					if (items == null || items.getType() == Material.AIR) {
						freeSpace += copyItem.getMaxStackSize();
					} else if (items.isSimilar(copyItem)) {
						freeSpace += copyItem.getMaxStackSize() - items.getAmount() <= 0 ? 0
								: copyItem.getMaxStackSize() - items.getAmount();
					}
				}

				if (freeSpace < copyItem.getAmount()) {
					p.sendMessage(this.instance.getMessage("notEnoughInvSpace").replaceAll("(?i)%" + "amount" + "%",
							String.valueOf(copyItem.getAmount() - freeSpace)));
					return;
				}

				if (!this.instance.getEconomySystem().has(p, price)) {
					p.sendMessage(this.instance.getMessage("notEnoughMoney").replaceAll("(?i)%" + "amount" + "%",
							UtilitiesAPI.getHumanReadablePriceFromNumber(
									price - this.instance.getEconomySystem().getBalance(p))));
					return;
				}

				copyItem.setAmount(amount); // SETS HOW MUCH THE PLAYER BOUGHT

				ItemSellEvent event = new ItemSellEvent(p, rentHandler, copyItem, price);
				Bukkit.getPluginManager().callEvent(event);

				if (event.isCancelled())
					return;

				this.instance.getEconomySystem().withdraw(p, price); // REMVOES THE MONEY FROM THE BUYER
				this.instance.getEconomySystem().deposit(owner, price); // GIVE SHOPOWNER THE MONEY

				p.getInventory().addItem(copyItem); // GIVES BUYER THE ITEM

				int site = this.instance.getShopInvBuilder(ownerUUID).getSite();

				if (this.instance.getChestsUtils().checkChestsInArea(shopId, copyItem)) {
					// LOOKS IN NEARBY CHESTS
					this.instance.getChestsUtils().removeItemFromChestsInArea(shopId, copyItem);

				}else {
			        item.setAmount(item.getAmount() - amount);
			        if (item != null && item.getAmount() > 0) {
			            item = ShopItemManager.editShopItemPrice(this.instance, item, itemPrice - price);
			        } else {
			        	this.moveItems(e, rentHandler, ShopInventoryType.SELL);
			    	} 
			    } 

				// UPDATES THE ITEM IN THE DATABASE, CACHE GETS AUTOMATICLY UPDATED
				this.instance.getShopsInvSQL().updateInventory(shopId, ShopInventoryType.SELL, site, inv.getContents());

				String type = StringUtils.capitalize(copyItem.getType().toString());
				String itemName = copyItem.hasItemMeta() && copyItem.getItemMeta().hasDisplayName()
						? copyItem.getItemMeta().getDisplayName()
						: type;

				p.sendMessage(this.instance.getMessage("shopItemBought")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(amount))
						.replaceAll("(?i)%" + "price" + "%", String.valueOf(formatter.format(price)))
						.replaceAll("(?i)%" + "itemname" + "%", itemName).replaceAll("(?i)%" + "type" + "%", type));
				return;

			} else if (e.isRightClick()) {

				// NO PERMISSIONS TO REMOVE
				if (!this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid,
						this.instance.manageFile().getString("UserPermissions.shop.Sell"))
						&& !this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid,
								this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					return;
				}

				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());

				int freeSpace = 0;
				for (ItemStack items : p.getInventory().getStorageContents()) {
					if (items == null || items.getType() == Material.AIR) {
						freeSpace += copyItem.getMaxStackSize();
					} else if (items.isSimilar(copyItem)) {
						freeSpace += copyItem.getMaxStackSize() - items.getAmount() <= 0 ? 0
								: copyItem.getMaxStackSize() - items.getAmount();
					}
				}

				if (freeSpace < copyItem.getAmount()) {
					p.sendMessage(this.instance.getMessage("notEnoughInvSpace").replaceAll("(?i)%" + "amount" + "%",
							String.valueOf(copyItem.getAmount() - freeSpace)));
					return;
				}

		        this.moveItems(e, rentHandler, ShopInventoryType.SELL);

				e.setCurrentItem(null);
				p.getInventory().addItem(copyItem);

				int site = this.instance.getShopInvBuilder(ownerUUID).getSite();
				this.instance.getShopsInvSQL().updateInventory(shopId, ShopInventoryType.SELL, site, inv.getContents()); // UPDATES THE ITEM IN THE DATABASE, CACHE GETS AUTOMATICLY UPDATED

				String type = StringUtils.capitalize(copyItem.getType().toString());
				String itemName = copyItem.hasItemMeta() && copyItem.getItemMeta().hasDisplayName()
						? copyItem.getItemMeta().getDisplayName()
						: type;

				p.sendMessage(this.instance.getMessage("shopItemRemoved")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount()))
						.replaceAll("(?i)%" + "itemname" + "%", itemName).replaceAll("(?i)%" + "type" + "%", type));

			}

			// Owner buys Item from Player
		} else if (e.getView().getTitle().startsWith(ChatColor.translateAlternateColorCodes('&',
				this.instance.manageFile().getString("GUI.ShopBuyAndSell.displayNameBuy")))) {

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

			// Player selling to owner
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
					p.sendMessage(this.instance.getMessage("notEnoughOwningItems").replaceAll("(?i)%" + "amount" + "%",
							String.valueOf(1)));
					return;
				}

				// Shiftclick buys one item, for the price of one
				if (e.isShiftClick()) {

					// ONE ITEM BUY
					amount = 1;
					price = itemPrice / item.getAmount();

				} else if (targetItemAmount >= item.getAmount()) {

					// MULTIPLE BUY
					amount = item.getAmount();
					price = itemPrice;

				} else {

					// REST BUY
					amount = targetItemAmount;
					price = itemPrice / item.getAmount() * targetItemAmount;

				}

				if (!this.instance.getEconomySystem().has(owner, price)) {
					p.sendMessage(this.instance.getMessage("notEnoughMoneyOwner"));
					return;
				}

				copyItem.setAmount(amount); // SETS HOW MUCH THE PLAYER SELLED

				if (!this.instance.getChestsUtils().checkForSpaceInArea(shopId, copyItem)) {
					p.sendMessage(this.instance.getMessage("notEnoughSpace"));
					return;
				}

				ItemBuyEvent event = new ItemBuyEvent(p, rentHandler, copyItem, price);
				Bukkit.getPluginManager().callEvent(event);

				if (event.isCancelled())
					return;

				// LOOKS IN NEARBY CHESTS
				this.instance.getChestsUtils().addItemToChestsInArea(shopId, copyItem);
				this.instance.getMethodes().removeItemFromPlayer(p, copyItem);

				this.instance.getEconomySystem().deposit(p, price); // REMVOES THE MONEY FROM THE BUYER
				this.instance.getEconomySystem().withdraw(owner, price); // GIVE SHOPOWNER THE MONEY

				int site = this.instance.getShopInvBuilder(ownerUUID).getSite();
				this.instance.getShopsInvSQL().updateInventory(shopId, ShopInventoryType.BUY, site, inv.getContents());

				String type = StringUtils.capitalize(copyItem.getType().toString());
				String itemName = copyItem.hasItemMeta() && copyItem.getItemMeta().hasDisplayName()
						? copyItem.getItemMeta().getDisplayName()
						: type;

				p.sendMessage(this.instance.getMessage("shopItemSold")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(amount))
						.replaceAll("(?i)%" + "price" + "%", String.valueOf(formatter.format(price)))
						.replaceAll("(?i)%" + "itemname" + "%", itemName).replaceAll("(?i)%" + "type" + "%", type));
				return;

			} else if (e.isRightClick()) {

				// NO PERMISSIONS TO REMOVE
				if (!this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid,
						this.instance.manageFile().getString("UserPermissions.shop.Buy"))
						&& !this.instance.getMethodes().hasPermission(RentTypes.SHOP, shopId, uuid,
								this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					return;
				}

				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());
				e.setCurrentItem(null);

		        this.moveItems(e, rentHandler, ShopInventoryType.BUY);

				int site = this.instance.getShopInvBuilder(ownerUUID).getSite();
				this.instance.getShopsInvSQL().updateInventory(shopId, ShopInventoryType.BUY, site, inv.getContents());

				String type = StringUtils.capitalize(copyItem.getType().toString());
				String itemName = copyItem.hasItemMeta() && copyItem.getItemMeta().hasDisplayName()
						? copyItem.getItemMeta().getDisplayName()
						: type;

				p.sendMessage(this.instance.getMessage("shopItemRemoved")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount()))
						.replaceAll("(?i)%" + "itemname" + "%", itemName).replaceAll("(?i)%" + "type" + "%", type));

			}
		}
	}

	private void moveItems(InventoryClickEvent e, RentTypeHandler rentHandler, ShopInventoryType type) {
		CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());
		if (catHandler == null)
			return;
		
		boolean multiSite = (catHandler.getMaxSite() > 1);
		int last = rentHandler.getInventories(type).size();
		Inventory lastInv = rentHandler.getInventory(type, last);
		int counter = 0;
		int lastItemSlot = -1;
		for (int i = 0; i < lastInv.getSize() - (multiSite ? 9 : 0); i++) {
			ItemStack temp = lastInv.getItem(i);
			if (temp != null && temp.getType() != Material.AIR) {
				counter++;
				lastItemSlot = i;
			}
		}
		
		ItemStack lastItem = (lastItemSlot >= 0) ? lastInv.getItem(lastItemSlot) : null;
		if (e.getCurrentItem() != null && e.getCurrentItem().equals(lastItem)) {
			e.setCurrentItem(null);
		} else {
			e.setCurrentItem(lastItem);
			if (lastItem != null)
				lastInv.setItem(lastItemSlot, null);
		}
		
		if (multiSite && counter <= 1 && last > 1) {
			rentHandler.setInventory(type, last, null);
			if (last - 1 >= 1) {
				Inventory buttonInv = rentHandler.getInventory(type, last - 1);
				int slot = buttonInv.getSize() - 1;
				buttonInv.setItem(slot, this.instance.getMethodes().getGUIItem("ShopBuyAndSell", "placeholderItem"));
			}
		}
	}

	private boolean checkSpecialItem(Player p, ItemStack item) {

		UUID uuid = p.getUniqueId();

		ShopInventoryBuilder builder = this.instance.getShopInvBuilder(uuid);

		ItemStack clonedItem = this.instance.getMethodes().removeIDKeyFromItem(item.clone());
		boolean isNextItem = clonedItem
				.isSimilar(this.instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem"));
		boolean isBeforeItem = clonedItem
				.isSimilar(this.instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem"));
		boolean isReturnItem = clonedItem
				.isSimilar(this.instance.getMethodes().getGUIItem("ShopBuyAndSell", "returnItem"));
		boolean isPlaceholder = clonedItem
				.isSimilar(this.instance.getMethodes().getGUIItem("ShopBuyAndSell", "placeholderItem"));

		if (isNextItem) {
			builder.nextSite();
			return true;
		} else if (isBeforeItem) {
			builder.beforeSite();
			return true;
		} else if (isReturnItem) {

			this.instance.removeShopInvBuilder(uuid);
			p.openInventory(ShopBuyOrSell.getSelectInv(this.instance, builder.getShopHandler().getID()));

			return true;
		} else if (isPlaceholder) {
			return true;
		}

		return false;
	}
}
