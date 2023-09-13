package me.truemb.rentit.utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PermissionsHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.handler.SettingsHandler;
import me.truemb.rentit.main.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class UtilMethodes {

	private Main instance;

	public UtilMethodes(Main plugin) {
		this.instance = plugin;
	}

	public boolean isTimeFormat(String feString) {
		return UtilitiesAPI.getTimeParsed(feString) != null;
	}

	public ItemStack getGUIItem(String subGUI, String itemName, int id, int site) {
		ItemStack item = new ItemStack(Material.valueOf(this.instance.manageFile().getString("GUI." + subGUI + ".items." + itemName + ".type")));
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("GUI." + subGUI + ".items." + itemName + ".displayName")));

		List<String> lore = new ArrayList<>();
		for (String s : instance.manageFile().getStringList("GUI." + subGUI + ".items." + itemName + ".lore")) {
			lore.add(ChatColor.translateAlternateColorCodes('&', s));
		}

		meta.getPersistentDataContainer().set(this.instance.guiItem, PersistentDataType.STRING, "true");
		
		if(id > 0)
			meta.getPersistentDataContainer().set(this.instance.idKey, PersistentDataType.INTEGER, id);

		if(site > 0)
			meta.getPersistentDataContainer().set(this.instance.siteKey, PersistentDataType.INTEGER, site);

		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public ItemStack getGUIItem(String subGUI, String itemName, int id) {
		return this.getGUIItem(subGUI, itemName, id, -1);
	}

	public ItemStack getGUIItem(String subGUI, String itemName) {
		return this.getGUIItem(subGUI, itemName, -1);
	}

	public ItemStack removeIDKeyFromItem(ItemStack item) {
		item = item.clone();
		ItemMeta meta = item.getItemMeta();

		if (meta.getPersistentDataContainer().has(this.instance.idKey, PersistentDataType.INTEGER))
			meta.getPersistentDataContainer().remove(this.instance.idKey);

		item.setItemMeta(meta);
		return item;
	}
	
	public ItemStack removeSiteKeyFromItem(ItemStack item) {
		item = item.clone();
		ItemMeta meta = item.getItemMeta();

		if (meta.getPersistentDataContainer().has(this.instance.siteKey, PersistentDataType.INTEGER))
			meta.getPersistentDataContainer().remove(this.instance.siteKey);

		item.setItemMeta(meta);
		return item;
	}

	public void setPrice(Player p, RentTypes type, int catID, String arg) {
		double price = 0D;

		try {
			price = Double.parseDouble(arg);
		} catch (NumberFormatException ex) {
			p.sendMessage(this.instance.getMessage("notANumber"));
			return;
		}

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, catID);

		if (catHandler != null) {
			catHandler.setPrice(price);
		}

		this.instance.getCategorySQL().setCosts(catID, type, price);
		this.updateAllSigns(type, catID);
		
	    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
	    
		p.sendMessage(this.instance.getMessage(type.toString().toLowerCase() + "PriceChanged")
				.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catID))
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
				.replaceAll("(?i)%" + "price" + "%", String.valueOf(price)));

	}

	public boolean hasPermissionForCommand(Player p, boolean adminCommand, String mainCmd, String subCmd) {
		boolean isAdvancedPermission = this.instance.manageFile().getBoolean("Options.advancedPermissions");
		String defaultPermission = this.instance.manageFile().getString("Permissions." + mainCmd);
		
		if(defaultPermission == null)
			defaultPermission = "rentit." + mainCmd;
		
		if(!isAdvancedPermission) {
			if(adminCommand) {
				if(!p.hasPermission(defaultPermission))
					return false;
			}
			return true;
		}else {
			if(subCmd != null)
				defaultPermission += "." + subCmd;

			return p.hasPermission(defaultPermission);
		}
	}
	
	public boolean hasPermission(RentTypes type, int id, UUID uuid, String permission) {
		RentTypeHandler handler = this.getTypeHandler(type, id);
		if (handler == null)
			return false;
		
		UUID ownerUUID = handler.getOwnerUUID();
		if (ownerUUID != null && ownerUUID.equals(uuid))
			return true;

		PlayerHandler playerHandler = this.getPlayerHandler(uuid);
		if (playerHandler == null)
			return false;

		PermissionsHandler permsHandler = playerHandler.getPermsHandler(type);
		if (permsHandler == null)
			return false;

		return permsHandler.hasPermission(id, permission);
	}

	// =================== HANDLERS ===========================

	public RentTypeHandler getTypeHandler(RentTypes type, Integer id) {

		if (!this.instance.rentTypeHandlers.containsKey(type))
			return null;

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);

		return typeHash.get(id);
	}
	
	public Collection<RentTypeHandler> getRemindersOfRentTypes(RentTypes type) {

		if(!this.instance.rentTypeHandlers.containsKey(type))
			return Collections.emptyList();

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);

		return typeHash.values().stream().filter(rentType -> !rentType.isAutoPayment() && rentType.getOwnerUUID() != null && !rentType.isReminded() && rentType.getReminder() != null && rentType.getReminder().getTime() <= System.currentTimeMillis()).collect(Collectors.toList());
	}
	
	public Collection<RentTypeHandler> getPaymentsOfRentTypes(RentTypes type) {

		if(!this.instance.rentTypeHandlers.containsKey(type))
			return Collections.emptyList();

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);

		return typeHash.values().stream().filter(rentType -> rentType.getOwnerUUID() != null && rentType.getNextPayment() != null && rentType.getNextPayment().getTime() <= System.currentTimeMillis()).collect(Collectors.toList());
	}

	public Collection<RentTypeHandler> getFreeRentTypesOfCategory(RentTypes type, int catId) {

		if(!this.instance.rentTypeHandlers.containsKey(type))
			return Collections.emptyList();

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);
		
		return typeHash.values().stream().filter(rentType -> rentType.getOwnerUUID() == null && rentType.getCatID() == catId).collect(Collectors.toList());
	}
	
	public Collection<RentTypeHandler> getRentTypesOfCategory(RentTypes type, int catId) {

		if(!this.instance.rentTypeHandlers.containsKey(type))
			return Collections.emptyList();

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);
		
		return typeHash.values().stream().filter(rentType -> rentType.getCatID() == catId).collect(Collectors.toList());
	}
	
	public Collection<RentTypeHandler> getFreeRentTypes(RentTypes type) {

		if(!this.instance.rentTypeHandlers.containsKey(type))
			return Collections.emptyList();

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);

		return typeHash.values().stream().filter(rentType -> rentType.getOwnerUUID() == null).collect(Collectors.toList());
	}
	
	public CategoryHandler getCategory(RentTypes type, Integer id) {

		if (!this.instance.catHandlers.containsKey(type))
			return null;

		HashMap<Integer, CategoryHandler> catHash = this.instance.catHandlers.get(type);

		return catHash.get((Integer) id);
	}

	public PlayerHandler getPlayerHandler(UUID uuid) {

		if (!this.instance.playerHandlers.containsKey(uuid))
			return null;

		return this.instance.playerHandlers.get(uuid);
	}
	// ==========================================================

	public RentTypeHandler createType(RentTypes type, int id, int catID) {

		if (type.equals(RentTypes.SHOP)) {
			instance.getShopsSQL().createShop(id, catID);
		} else if (type.equals(RentTypes.HOTEL)) {
			instance.getHotelsSQL().createHotel(id, catID);
		}

		// ADD HANDLER
		HashMap<Integer, RentTypeHandler> typeHash = new HashMap<>();
		if (this.instance.rentTypeHandlers.containsKey(type))
			typeHash = this.instance.rentTypeHandlers.get(type);

		RentTypeHandler typeHandler = new RentTypeHandler(this.instance, type, id, catID, null, null, new Timestamp(System.currentTimeMillis()), true);
		typeHash.put(id, typeHandler);

		this.instance.rentTypeHandlers.put(type, typeHash);
		
		return typeHandler;
	}

	public void deleteType(RentTypes type, int id) {

		if (type.equals(RentTypes.SHOP)) {
			this.instance.getShopsSQL().delete(id); // Deletes Shop Entry
		} else if (type.equals(RentTypes.HOTEL)) {
			this.instance.getHotelsSQL().delete(id); // Deletes Hotel Entry
		}

		this.instance.getPermissionsSQL().reset(type, id);

		// REMOVES HANDLER
		if (this.instance.rentTypeHandlers.containsKey(type)) {
			HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);
			typeHash.remove(id);
		}
	}

	// ===========================================================

	public boolean isSettingActive(UUID uuid, RentTypes type, int id, Settings setting) {

		PlayerHandler playerHandler = this.getPlayerHandler(uuid);
		if (playerHandler == null)
			return false;

		SettingsHandler settingsHandler = playerHandler.getSettingsHandler(type);
		if (settingsHandler == null)
			return setting.equals(Settings.shopMessaging) ? true : false;

		return settingsHandler.isSettingActive(id, setting);
	}

	public void deleteSigns(RentTypes type, int id) {

		List<Sign> list = this.instance.getSignFileManager().getSigns(type, id);

		list.forEach(signs -> {
			signs.getBlock().breakNaturally();
		});
		this.instance.getSignFileManager().clearSigns(type, id);
	}

	public void updateSign(RentTypes type, int id, Sign s, String owner, String time, double price, int size) {
		
		Bukkit.getScheduler().runTask(this.instance, new Runnable() {
			
			@Override
			public void run() {

				if (type.equals(RentTypes.SHOP)) {

					if (owner == null) {
						// ZU MIETEN
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.sellShopSign.line" + i)
									.replaceAll("(?i)%" + "time" + "%", time)
									.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
									.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))));
						}
					} else {
						// BEREITS VERMIETET
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.boughtShopSign.line" + i)
									.replaceAll("(?i)%" + "time" + "%", time)
									.replaceAll("(?i)%" + "owner" + "%", owner)
									.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
									.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))));
						}
					}
				} else if (type.equals(RentTypes.HOTEL)) {

					if (owner == null) {
						// ZU MIETEN
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.sellHotelSign.line" + i)
									.replaceAll("(?i)%" + "time" + "%", time)
									.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
									.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))));
						}
					} else {
						// BEREITS VERMIETET
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.boughtHotelSign.line" + i)
									.replaceAll("(?i)%" + "time" + "%", time)
									.replaceAll("(?i)%" + "owner" + "%", owner)
									.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
									.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))));
						}
					}
				}
				s.update();
				
			}
		});

	}

	public void updateSign(RentTypes type, int id, String owner, String time, double price, int size) {

		List<Sign> list = this.instance.getSignFileManager().getSigns(type, id);

		list.forEach(signs -> {
			this.updateSign(type, id, signs, owner, time, price, size);
		});
	}

	public void updateAllSigns(RentTypes type, int catId) {

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, catId);

		if (catHandler == null)
			return;
		
		HashMap<Integer, RentTypeHandler> hash = this.instance.rentTypeHandlers.get(type);
		
		if(hash == null)
			return;
		
		for(int typeId : hash.keySet()) {
			RentTypeHandler rentHandler = hash.get(typeId);
			if(rentHandler.getCatID() == catId)
				this.updateSign(type, typeId, rentHandler.getOwnerName(), catHandler.getTime(), catHandler.getPrice(), catHandler.getSize());
		}
	}
	
	public void updateSign(RentTypes type, int id) {

		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(type, id);

		if (rentHandler == null)
			return;

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, rentHandler.getCatID());

		if (catHandler == null)
			return;
		

		this.updateSign(type, id, rentHandler.getOwnerName(), catHandler.getTime(), catHandler.getPrice(), catHandler.getSize());
	}

	public void setTime(Player p, RentTypes type, int catID, String timeS) {

		this.instance.getCategorySQL().setTime(catID, type, timeS);

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, catID);

		if (catHandler != null) {
			catHandler.setTime(timeS);
		}

		this.updateAllSigns(type, catID);

	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
	    
		p.sendMessage(this.instance.getMessage(type.toString().toLowerCase() + "RentTimeChanged")
				.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catID))
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
				.replaceAll("(?i)%" + "time" + "%", timeS));

	}

	public void setSize(Player p, int catID, String arg) {
		
		int oldSize = 0;
		int size = 0;

		try {
			size = Integer.parseInt(arg);
		} catch (NumberFormatException ex) {
			p.sendMessage(this.instance.getMessage("notANumber"));
			return;
		}

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, catID);
		
		if (catHandler == null) {
			p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
			return;
		}
			
		if (size < 0 || size % 9 > 0 || size > 54 || catHandler.getMaxSite() > 1 && size > 45) {
			p.sendMessage(this.instance.getMessage("shopSizeNotValid"));
			return;
		}
			
		oldSize = (Integer) catHandler.getSize();
		catHandler.setSize(size);
		
		this.instance.getCategorySQL().setSize(catID, size);
		
		Collection<RentTypeHandler> shops = this.getRentTypesOfCategory(RentTypes.SHOP, catID);
		
		/*TEST - Needs to ignore gui items and set them himself?
		if(oldSize > size) {
			//TODO Check if every Item has an Inventory Slot
		}else {
			//TODO Move Items from the next Site to the site before, if space
			shops.forEach(shop -> {
				Collection<Inventory> sellInventories = shop.getInventories(ShopInventoryType.SELL);
				Collection<Inventory> buyInventories = shop.getInventories(ShopInventoryType.BUY);
				
				Inventory currentSellInv = null;
				int freeSellSlots;
				int sellSite = 1;
				for(Inventory inv : sellInventories) {
					
					//Set the first Inventory, only multi site Shops can pass that
					if(currentSellInv == null) {
						currentSellInv = inv;
						continue;
					}
					
					freeSellSlots = 0;
					for(int i = 0; i < currentSellInv.getSize(); i++) {
						ItemStack item = currentSellInv.getItem(i);
						if(item == null || item.getType() == Material.AIR) {
							freeSellSlots++;
						}
					}
					
					for(ItemStack items : inv.getContents()) {
						if(items != null && items.getType() != Material.AIR && !items.getItemMeta().getPersistentDataContainer().has(this.instance.guiItem, PersistentDataType.STRING)) {
							if(freeSellSlots > 0) {
								//Add Item to Inventory before the current
								freeSellSlots--;
								for(int i = 0; i < currentSellInv.getSize(); i++) {
									ItemStack item = currentSellInv.getItem(i);
									if(item == null || item.getType() == Material.AIR) {
										currentSellInv.setItem(i, items);
									}
								}
								
								if(freeSellSlots == 0) {
									this.instance.getShopsInvSQL().updateInventory(shop.getID(), ShopInventoryType.SELL, sellSite, currentSellInv.getContents());
									currentSellInv = inv;
									freeSellSlots = 0;
									sellSite++;
								}
							}else {
								//TODO Move all Items to first position. There might have changed some
							}
						}
					}
				}
				
				
			});
		}
		*/
		// UPDATE INVENTORY TO SIZE
		shops.forEach(shop -> {
			shop.resetInventories(); //Deletes the Inventories, so that the new Size will be used
			this.instance.getShopsInvSQL().setupShopInventories(shop); // Load the Shop Inventory again with the correct size
		});
		
	    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		this.updateAllSigns(RentTypes.SHOP, catID);
		p.sendMessage(this.instance.getMessage("shopSizeChanged")
				.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
				.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catID))
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias));

	}

	public void setMaxSite(Player p, int catID, String arg) {

		int maxSite = 1;

		try {
			maxSite = Integer.parseInt(arg);
		} catch (NumberFormatException ex) {
			p.sendMessage(this.instance.getMessage("notANumber"));
			return;
		}


		boolean success = catID > 0;
		CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, catID);
		
		if (success) {
			
			if (maxSite < 1) {
				p.sendMessage(this.instance.getMessage("shopMaxSiteInvalid"));
				return;
			}
			
			if (catHandler != null)
				catHandler.setMaxSite(maxSite);

			this.instance.getCategorySQL().setMaxSite(catID, maxSite);
			
		}else {
			p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
			return;
		}

		// UPDATE INVENTORY TO SIZE
		for(RentTypeHandler shops : this.getRentTypesOfCategory(RentTypes.SHOP, catID)) {
			shops.resetInventories(); //Deletes the Inventories, so that the new Size will be used
			this.instance.getShopsInvSQL().setupShopInventories(shops); // Load the Shop Inventory again with the correct size
		}
		
	    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		this.updateAllSigns(RentTypes.SHOP, catID);
		p.sendMessage(this.instance.getMessage("shopMaxSiteChanged")
				.replaceAll("(?i)%" + "maxSite" + "%", String.valueOf(maxSite))
				.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catID))
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias));

	}
	
	public boolean isSubCommandEnabled(String command, String arg) {
		String path = "Options.commands." + command.toLowerCase() + ".disabledSubCommands";
		
		if(this.instance.manageFile().isSet(path))
			for(String args : this.instance.manageFile().getStringList(path))
				if(args.equalsIgnoreCase(arg))
					return false;
		
		return true;
	}
	
	//SEND LIST
	public void sendList(Player p, RentTypes type, int site) {
		
		HashMap<Integer, RentTypeHandler> hash = this.instance.rentTypeHandlers.get(type);
		String path = type == RentTypes.HOTEL ? "hotelList" : "shopList";
		
		if(hash == null) {
			p.sendMessage(this.instance.getMessage(path + ".couldntFind"));
			return;
		}
		
		if(site < 1) site = 1;
		
		int end = site * 10;
		int start = end - 10;
		
		int size = hash.size();
		
		if(end > size) end = size;
		
		
		p.sendMessage(this.instance.getMessage(path + ".header"));
		
		for(int i = start; i < end; i++) {
			RentTypeHandler handler = hash.get(hash.keySet().toArray()[i]);
			String id = String.valueOf(handler.getID());
			
			TextComponent component = new TextComponent(this.instance.getMessage(path + ".body")
					.replaceAll("(?i)%" + "id" + "%", id)
					.replaceAll("(?i)%" + "owner" + "%", handler.getOwnerName() == null ? ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("Messages." + path + ".notOwned")) : handler.getOwnerName()));
			
			component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', this.instance.manageFile().getString("Messages." + path + ".hover")))));
			component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + type.toString().toLowerCase() + " info " + id));

			p.spigot().sendMessage(component);
		}
		

		p.sendMessage(this.instance.getMessage(path + ".footer"));
	}

	public void removeItemFromPlayer(Player p, ItemStack item) {

		int amount = item.getAmount();

		for (ItemStack items : p.getInventory().getContents()) {
			if (items != null && items.getType() != Material.AIR) {
				if (items.isSimilar(item)) {
					if (amount >= items.getAmount()) {
						amount -= items.getAmount();
						items.setAmount(0);
					} else {
						items.setAmount(items.getAmount() - amount);
						amount = 0;
						return;
					}
				}
			}
		}
	}

	public void deleteArea(Player p, RentTypes type, int id) {

		this.instance.getAreaFileManager().deleteArea(type, id);

		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().deleteRegion(p.getWorld(), region);
	}

	public boolean createArea(Player p, RentTypes type, int id) {
		World world = p.getWorld();
		Region sel = null;
		try {
			sel = this.instance.getWorldEdit().getSession(p).getSelection(BukkitAdapter.adapt(world));
		} catch (IncompleteRegionException ex) {}

		if (sel == null)
			return false;

		BlockVector3 min = sel.getMinimumPoint();
		BlockVector3 max = sel.getMaximumPoint();

		this.instance.getAreaFileManager().setArea(type, id, p.getLocation(), min, max);
		this.createWorldGuardRegion(type, id, world, min, max);
		
		// CREATES BACKUP FOR OWNER CHANGE
		this.instance.getBackupManager().save(type, id, min, max, p.getWorld());
		return true;
	}
	
	public boolean existsWorldGuardRegion(RentTypes type, int id, World world) {
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return false;
		
		return this.instance.getWorldGuardUtils().regionExists(world, type.toString() + "_" + String.valueOf(id));
	}
	
	public void createWorldGuardRegion(RentTypes type, int id, World world, BlockVector3 min, BlockVector3 max) {
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().createRegion(world, region, min, max);
	}
	
	public void clearPlayersFromRegion(RentTypes type, int id, World world) {
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().clearMembers(world, region);
	}

	public boolean isMemberFromRegion(RentTypes type, int id, World world, UUID uuid) {
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return false;
		
		String region = type.toString() + "_" + String.valueOf(id);
		return this.instance.getWorldGuardUtils().isMember(world, region, uuid);
	}
	
	public void addMemberToRegion(RentTypes type, int id, World world, UUID uuid) {
		if(uuid == null) return;
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().addMember(world, region, uuid);
	}
	
	public void setOwnerFromRegion(RentTypes type, int id, World world, UUID uuid) {
		if(uuid == null) return;
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().setOwnerFromRegion(world, region, uuid);
	}
		
	public void removeMemberFromRegion(RentTypes type, int id, World world, UUID uuid) {
		if(uuid == null) return;
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().removeMember(world, region, uuid);
	}

}
