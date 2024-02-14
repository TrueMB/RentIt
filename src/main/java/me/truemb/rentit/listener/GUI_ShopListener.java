package me.truemb.rentit.listener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.events.ItemBuyEvent;
import me.truemb.rentit.events.ItemSellEvent;
import me.truemb.rentit.gui.ShopBuyOrSell;
import me.truemb.rentit.guiholder.GuiHolder;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.ShopItemManager;
import me.truemb.rentit.utils.UtilitiesAPI;

public class GUI_ShopListener implements Listener {

	private Main instance;

	public GUI_ShopListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
	public void onUserClick(InventoryClickEvent e) {

		Player p = (Player) e.getWhoClicked();
		UUID uuid = p.getUniqueId();

        Inventory inv = e.getClickedInventory();
        ItemStack item = e.getCurrentItem();

        InventoryHolder holder = e.getInventory().getHolder();
		NumberFormat formatter = new DecimalFormat("#0.00");
        
        if(holder == null)
        	return;
        
        if(!(holder instanceof GuiHolder))
        	return;
        
        GuiHolder guiHolder = (GuiHolder) holder;
        RentTypes rentType = guiHolder.getRentType();
        
        if(guiHolder.getGuiType() != GuiType.SHOP)
        	return;
        
        int id = guiHolder.getID();
        ShopInventoryType shopInvType = guiHolder.getShopInvType();

        //Cancel everything, if player is in a RentIt GUI Inventory.
		e.setCancelled(true);
	    
	    if(inv == null || !inv.equals(e.getView().getTopInventory()))
	    	return;

	    if(item == null || item.getType() == Material.AIR)
	    	return;
	    
		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(rentType, id);
		
		if (rentHandler == null)
			return;

		UUID ownerUUID = rentHandler.getOwnerUUID();
		OfflinePlayer owner = ownerUUID != null ? Bukkit.getOfflinePlayer(ownerUUID) : null;

		//checks if item is from Inventory GUI. F.e. next site item
		if (this.checkSpecialItem(p, item))
			return;

	    if(shopInvType == ShopInventoryType.SELL) {
	    	
			// Owner sells Items
			if (e.isLeftClick() && (rentHandler.isAdmin() || !ownerUUID.equals(uuid))) {

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
						freeSpace += copyItem.getMaxStackSize() - items.getAmount() <= 0 ? 0 : copyItem.getMaxStackSize() - items.getAmount();
					}
				}

				if (freeSpace < copyItem.getAmount()) {
					p.sendMessage(this.instance.getMessage("notEnoughInvSpace")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount() - freeSpace)));
					return;
				}

				if (!this.instance.getEconomySystem().has(p, price)) {
					p.sendMessage(this.instance.getMessage("notEnoughMoney")
							.replaceAll("(?i)%" + "amount" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price - this.instance.getEconomySystem().getBalance(p))));
					return;
				}

				copyItem.setAmount(amount); // SETS HOW MUCH THE PLAYER BOUGHT

				ItemSellEvent event = new ItemSellEvent(p, rentHandler, copyItem, price);
				Bukkit.getPluginManager().callEvent(event);

				if (event.isCancelled())
					return;

				this.instance.getEconomySystem().withdraw(p, price); // REMVOES THE MONEY FROM THE BUYER
				p.getInventory().addItem(copyItem); // GIVES BUYER THE ITEM
				
				if(!rentHandler.isAdmin()) {
					this.instance.getEconomySystem().deposit(owner, price); // GIVE SHOPOWNER THE MONEY

					if (this.instance.getChestsUtils().checkChestsInArea(id, copyItem)) {
						// LOOKS IN NEARBY CHESTS
						this.instance.getChestsUtils().removeItemFromChestsInArea(id, copyItem);
	
					}else {
				        item.setAmount(item.getAmount() - amount);
				        if (item != null && item.getAmount() > 0) {
				            item = ShopItemManager.editShopItemPrice(this.instance, item, itemPrice - price);
				        } else {
				        	this.moveItems(e, rentHandler, shopInvType);
				    	} 
				    } 
	
					// UPDATES THE ITEM IN THE DATABASE, CACHE GETS AUTOMATICLY UPDATED
					this.instance.getShopsInvSQL().updateInventories(id, shopInvType);
				}

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
				if(rentHandler.isAdmin()) {
					if(!this.instance.getMethodes().hasPermissionForCommand(p, true, "adminshop", null)) {
						p.sendMessage(this.instance.getMessage("adminshopPerm"));
						return;
					}
				}else {
					if (!this.instance.getMethodes().hasPermission(rentType, id, uuid, this.instance.manageFile().getString("UserPermissions." + rentType.toString().toLowerCase() + ".Sell"))
							&& !this.instance.getMethodes().hasPermission(rentType, id, uuid, this.instance.manageFile().getString("UserPermissions." + rentType.toString().toLowerCase() + ".Admin"))) {
						return;
					}
				}

				ItemStack copyItem = ShopItemManager.removeShopItem(this.instance, item.clone());

				int freeSpace = 0;
				for (ItemStack items : p.getInventory().getStorageContents()) {
					if (items == null || items.getType() == Material.AIR) {
						freeSpace += copyItem.getMaxStackSize();
					} else if (items.isSimilar(copyItem)) {
						freeSpace += copyItem.getMaxStackSize() - items.getAmount() <= 0 ? 0 : copyItem.getMaxStackSize() - items.getAmount();
					}
				}

				if (freeSpace < copyItem.getAmount()) {
					p.sendMessage(this.instance.getMessage("notEnoughInvSpace")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount() - freeSpace)));
					return;
				}

		        this.moveItems(e, rentHandler, shopInvType);
				p.getInventory().addItem(copyItem);

				this.instance.getShopsInvSQL().updateInventories(id, shopInvType); // UPDATES THE ITEM IN THE DATABASE, CACHE GETS AUTOMATICLY UPDATED

				String type = StringUtils.capitalize(copyItem.getType().toString());
				String itemName = copyItem.hasItemMeta() && copyItem.getItemMeta().hasDisplayName() ? copyItem.getItemMeta().getDisplayName() : type;

				p.sendMessage(this.instance.getMessage("shopItemRemoved")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount()))
						.replaceAll("(?i)%" + "itemname" + "%", itemName).replaceAll("(?i)%" + "type" + "%", type));

			}

			// Owner buys Item from Player
		} else if (shopInvType == ShopInventoryType.BUY) {

			// Player selling to owner
			if (e.isLeftClick() && (rentHandler.isAdmin() || !ownerUUID.equals(uuid))) {

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
					p.sendMessage(this.instance.getMessage("notEnoughOwningItems").replaceAll("(?i)%" + "amount" + "%", String.valueOf(1)));
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
				
				copyItem.setAmount(amount); // SETS HOW MUCH THE PLAYER SELLED

				if(!rentHandler.isAdmin()) {
					if (!this.instance.getEconomySystem().has(owner, price)) {
						p.sendMessage(this.instance.getMessage("notEnoughMoneyOwner"));
						return;
					}
	
					if (!this.instance.getChestsUtils().checkForSpaceInArea(id, copyItem)) {
						p.sendMessage(this.instance.getMessage("notEnoughSpace"));
						return;
					}
				}

				ItemBuyEvent event = new ItemBuyEvent(p, rentHandler, copyItem, price);
				Bukkit.getPluginManager().callEvent(event);

				if (event.isCancelled())
					return;

				// LOOKS IN NEARBY CHESTS
				if(!rentHandler.isAdmin()) {
					this.instance.getChestsUtils().addItemToChestsInArea(id, copyItem);
					this.instance.getEconomySystem().withdraw(owner, price); // GIVE SHOPOWNER THE MONEY

					this.instance.getShopsInvSQL().updateInventories(id, shopInvType);
				}

				this.instance.getMethodes().removeItemFromPlayer(p, copyItem);
				this.instance.getEconomySystem().deposit(p, price); // REMVOES THE MONEY FROM THE BUYER

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
				if(rentHandler.isAdmin()) {
					if(!this.instance.getMethodes().hasPermissionForCommand(p, true, "adminshop", null)) {
						p.sendMessage(this.instance.getMessage("adminshopPerm"));
						return;
					}
				}else {
					if (!this.instance.getMethodes().hasPermission(rentType, id, uuid, this.instance.manageFile().getString("UserPermissions." + rentType.toString().toLowerCase() + ".Buy"))
							&& !this.instance.getMethodes().hasPermission(rentType, id, uuid, this.instance.manageFile().getString("UserPermissions." + rentType.toString().toLowerCase() + ".Admin"))) {
						return;
					}
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

		        this.moveItems(e, rentHandler, shopInvType);

				this.instance.getShopsInvSQL().updateInventories(id, shopInvType);

				String type = StringUtils.capitalize(copyItem.getType().toString());
				String itemName = copyItem.hasItemMeta() && copyItem.getItemMeta().hasDisplayName() ? copyItem.getItemMeta().getDisplayName() : type;

				p.sendMessage(this.instance.getMessage("shopItemRemoved")
						.replaceAll("(?i)%" + "amount" + "%", String.valueOf(copyItem.getAmount()))
						.replaceAll("(?i)%" + "itemname" + "%", itemName).replaceAll("(?i)%" + "type" + "%", type));

			}
		}
	}

	private void moveItems(InventoryClickEvent e, RentTypeHandler rentHandler, ShopInventoryType type) {
		Player p = (Player) e.getWhoClicked();
		
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
			e.getInventory().setItem(e.getSlot(), lastItem); //e.setCurrentItem doesn't work. Maybe because of cancel
			if (lastItem != null)
				lastInv.setItem(lastItemSlot, null);

			//this.instance.getShopsInvSQL().updateInventories(rentHandler.getID(), type);
		}
		
		if (multiSite && counter <= 1 && last > 1) {
			rentHandler.setInventory(type, last, null);
			
			if(lastInv.equals(e.getInventory())) {
				this.instance.getShopInvBuilder(p.getUniqueId()).beforeSite();
				
				rentHandler.setInventory(type, last, null);
				//this.instance.getShopsInvSQL().updateInventories(rentHandler.getID(), type);
			}
			
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
