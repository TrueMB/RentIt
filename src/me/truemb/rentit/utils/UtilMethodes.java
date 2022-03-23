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
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
		
		if(id > 0) {
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, id);
		}

		if(site > 0) {
			NamespacedKey key = new NamespacedKey(this.instance, "Site");
			meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, site);
		}

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

		NamespacedKey key = new NamespacedKey(this.instance, "ID");
		if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
			meta.getPersistentDataContainer().remove(key);

		item.setItemMeta(meta);
		return item;
	}
	
	public ItemStack removeSiteKeyFromItem(ItemStack item) {
		item = item.clone();
		ItemMeta meta = item.getItemMeta();

		NamespacedKey key = new NamespacedKey(this.instance, "Site");
		if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
			meta.getPersistentDataContainer().remove(key);

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
		p.sendMessage(this.instance.getMessage(type.toString().toLowerCase() + "PriceChanged").replace("%price%", String.valueOf(price)).replace("%catId%", String.valueOf(catID)));

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
		UUID ownerUUID = this.instance.getAreaFileManager().getOwner(type, id);
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
	
	public Collection<RentTypeHandler> getPaymentsOfRentTypes(RentTypes type) {

		if(!this.instance.rentTypeHandlers.containsKey(type))
			return Collections.emptyList();

		HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);

		return typeHash.values().stream().filter(rentType -> rentType.getOwnerUUID() != null && rentType.getNextPayment() != null && rentType.getNextPayment().getTime() <= System.currentTimeMillis()).collect(Collectors.toList());
	}

	public CategoryHandler getCategory(RentTypes type, Integer id) {

		if (!this.instance.catHandlers.containsKey(type))
			return null;

		HashMap<Integer, CategoryHandler> catHash = this.instance.catHandlers.get(type);

		return catHash.get(id);
	}

	public PlayerHandler getPlayerHandler(UUID uuid) {

		if (!this.instance.playerHandlers.containsKey(uuid))
			return null;

		return this.instance.playerHandlers.get(uuid);
	}
	// ==========================================================

	public void createType(RentTypes type, int id, int catID) {

		if (type.equals(RentTypes.SHOP)) {
			instance.getShopsSQL().createShop(id, catID);
		} else if (type.equals(RentTypes.HOTEL)) {
			instance.getHotelsSQL().createHotel(id, catID);
		}

		// ADD HANDLER
		HashMap<Integer, RentTypeHandler> typeHash = new HashMap<>();
		if (this.instance.rentTypeHandlers.containsKey(type))
			typeHash = this.instance.rentTypeHandlers.get(type);

		RentTypeHandler typeHandler = new RentTypeHandler(type, id, catID, null, null, new Timestamp(System.currentTimeMillis()), true);
		typeHash.put(id, typeHandler);

		this.instance.rentTypeHandlers.put(type, typeHash);
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
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.sellShopSign.line" + i).replace("%time%", time).replace("%size%", String.valueOf(size)).replace("%price%", String.valueOf(price))));
						}
					} else {
						// BEREITS VERMIETET
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.boughtShopSign.line" + i).replace("%time%", time).replace("%owner%", owner).replace("%size%", String.valueOf(size)).replace("%price%", String.valueOf(price))));
						}
					}
				} else if (type.equals(RentTypes.HOTEL)) {

					if (owner == null) {
						// ZU MIETEN
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.sellHotelSign.line" + i).replace("%time%", time).replace("%price%", String.valueOf(price))));
						}
					} else {
						// BEREITS VERMIETET
						for (int i = 1; i <= 4; i++) {
							s.setLine(i - 1, ChatColor.translateAlternateColorCodes('&', instance.manageFile().getString("Options.shop.sign.boughtHotelSign.line" + i).replace("%time%", time).replace("%owner%", owner).replace("%price%", String.valueOf(price))));
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

		p.sendMessage(this.instance.getMessage(type.toString().toLowerCase() + "RentTimeChanged").replace("%time%", timeS).replace("%catId%", String.valueOf(catID)));

	}

	public void setSize(Player p, int catID, int shopId, String arg) {

		int size = 0;

		try {
			size = Integer.parseInt(arg);
		} catch (NumberFormatException ex) {
			p.sendMessage(this.instance.getMessage("notANumber"));
			return;
		}

		if (size < 0 || size % 9 > 0 || size > 54) {
			p.sendMessage(this.instance.getMessage("shopSizeNotValid"));
			return;
		}

		boolean success = catID > 0;

		if (success) {

			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, catID);

			if (catHandler != null) {
				catHandler.setSize(size);
			}

			this.instance.getCategorySQL().setSize(catID, size);
		}

		if (!success) {
			p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
			return;
		}

		// UPDATE INVENTORY TO SIZE
		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

		if (rentHandler == null) {
			p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
			return;
		}

		this.instance.getShopsInvSQL().setupShopInventories(rentHandler); // UPDATE SHOP INVENTORIES

		this.updateAllSigns(RentTypes.SHOP, catID);
		p.sendMessage(this.instance.getMessage("shopSizeChanged").replace("%catId%", String.valueOf(catID)));

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

	public List<Inventory> getShopChestInventories(int shopId) {
		World world = this.instance.getAreaFileManager().getWorldFromArea(RentTypes.SHOP, shopId);
		BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(RentTypes.SHOP, shopId);
		BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(RentTypes.SHOP, shopId);
		
		List<Inventory> chestInventories = new ArrayList<>();

		for (int minX = min.getBlockX(); minX <= max.getBlockX(); minX++) {
			for (int minZ = min.getBlockZ(); minZ <= max.getBlockZ(); minZ++) {
				for (int minY = min.getBlockY(); minY <= max.getBlockY(); minY++) {
					Block b = world.getBlockAt(minX, minY, minZ);
					if (b != null && b.getType() == Material.CHEST) {
						Chest chest = (Chest) b.getState();
						Inventory chestInv = chest.getBlockInventory();
						
						chestInventories.add(chestInv);
					}
				}
			}
		}
		
		return chestInventories;
	}

	// CHECK CHESTS FOR ITEM
	public boolean checkChestsinArea(int shopId, ItemStack item) {

		List<Inventory> chestInventories = this.getShopChestInventories(shopId);
		int amount = item.getAmount();

		for(Inventory inv : chestInventories) {
			for (ItemStack items : inv.getContents()) {
				if (items != null) {
					if (items.isSimilar(item)) {
						amount -= items.getAmount();
						if (amount <= 0)
							return true;
					}
				}
			}
		}
		return false;
	}
	
	// CHECK IF SPACES
	public boolean checkForSpaceinArea(int shopId, ItemStack item) {

		List<Inventory> chestInventories = this.getShopChestInventories(shopId);
		int amount = item.getAmount();

		for(Inventory inv : chestInventories) {
			for (ItemStack items : inv.getContents()) {
				if (items == null || items.getType() == Material.AIR) {
					return true;
				} else if (items.isSimilar(item)) {
					if (amount > 64 - items.getAmount()) {
						amount -= 64 - items.getAmount();
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}

	// REMOVES ITEMS FROM CHESTS
	public void removeItemFromChestsInArea(int shopId, ItemStack item) {
		
		List<Inventory> chestInventories = this.getShopChestInventories(shopId);
		int amount = item.getAmount();

		for(Inventory inv : chestInventories) {
			for (ItemStack items : inv.getContents()) {
				if (items != null) {
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
	}

	// REMOVES ITEMS FROM CHESTS
	public void addItemToChestsInArea(int shopId, ItemStack item) {

		List<Inventory> chestInventories = this.getShopChestInventories(shopId);
		int amount = item.getAmount();

		for(Inventory inv : chestInventories) {
			for (int i = 0; i < inv.getSize(); i++) {
				ItemStack items = inv.getItem(i);
				if (items == null || items.getType() == Material.AIR) {
					inv.setItem(i, item);
					return;
				} else if (items.isSimilar(item)) {
					if (amount > 64 - items.getAmount()) {
						items.setAmount(64);
						amount -= 64 - items.getAmount();
						inv.setItem(i, items);
					} else {
						items.setAmount(items.getAmount() + amount);
						inv.setItem(i, items);
						return;
					}
				}
			}
		}
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
		
	public void removeMemberToRegion(RentTypes type, int id, World world, UUID uuid) {
		if(uuid == null) return;
		// WORLDGUARD
		if (this.instance.getWorldGuard() == null)
			return;
		
		String region = type.toString() + "_" + String.valueOf(id);
		this.instance.getWorldGuardUtils().removeMember(world, region, uuid);
	}

}
