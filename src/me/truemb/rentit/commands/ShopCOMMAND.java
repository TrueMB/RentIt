package me.truemb.rentit.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.database.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PermissionsHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.handler.SettingsHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;
import me.truemb.rentit.utils.ShopItemManager;
import me.truemb.rentit.utils.UtilitiesAPI;

public class ShopCOMMAND implements CommandExecutor, TabCompleter {

	private Main instance;
	private List<String> subCommands = new ArrayList<>();
	private List<String> adminSubCommands = new ArrayList<>();

	private RentTypes type = RentTypes.SHOP;

	public ShopCOMMAND(Main plugin) {
		this.instance = plugin;
		this.instance.getCommand("shop").setExecutor(this);

		subCommands.add("noinfo");
		subCommands.add("users");
		subCommands.add("permissions");
		subCommands.add("setPermission");
		subCommands.add("buy");
		subCommands.add("sellItem");
		subCommands.add("buyItem");
		subCommands.add("help");

		adminSubCommands.add("createCat");
		adminSubCommands.add("setNPC");
		adminSubCommands.add("setArea");
		adminSubCommands.add("reset");
		adminSubCommands.add("delete");
		adminSubCommands.add("updateBackup");
		adminSubCommands.add("info");
		adminSubCommands.add("door");
		adminSubCommands.add("rollback");
		adminSubCommands.add("setTime");
		adminSubCommands.add("setSize");
		adminSubCommands.add("setPrice");
		adminSubCommands.add("list");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = PlayerManager.getUUID(p);

		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("setNPC")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setnpc")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				} else {
					
					if(this.instance.getNpcUtils() != null) {
						//NPC
						
						if(this.instance.getNpcUtils().existsNPCForShop(shopId)) {
							this.instance.getNpcUtils().moveNPC(shopId, p.getLocation());
						}else {
							//CREATE NPC IN CITIZENS DATABASE
							this.instance.getNpcUtils().createNPC(shopId);

							UUID ownerUUID = this.instance.getAreaFileManager().getOwner(this.type, shopId);
							
							//SHOP IS OWNED
							if(ownerUUID != null) {
								String playerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
								String prefix = instance.getPermissionsAPI().getPrefix(ownerUUID);
								
								this.instance.getNpcUtils().spawnAndEditNPC(shopId, prefix, ownerUUID, playerName);
							}
						}
						
					}else {
						//VILLAGER
						
						if(this.instance.getVillagerUtils().isVillagerSpawned(shopId)) {
							this.instance.getVillagerUtils().moveVillager(shopId, p.getLocation());
						}else {
							UUID ownerUUID = this.instance.getAreaFileManager().getOwner(this.type, shopId);
							
							//SHOP IS OWNED
							if(ownerUUID != null) {
								String playerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
								String prefix = instance.getPermissionsAPI().getPrefix(ownerUUID);

								this.instance.getVillagerUtils().spawnVillager(shopId, prefix, ownerUUID, playerName);
							}
						}
					}
					
					this.instance.getNPCFileManager().setNPCLocForShop(shopId, p.getLocation());

					p.sendMessage(this.instance.getMessage("shopCitizenCreated").replace("%shopId%", String.valueOf(shopId)));
					return true;
				}
			} else if (args[0].equalsIgnoreCase("list")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "list")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				this.instance.getMethodes().sendList(p, this.type, 1);
				return true;
				
			} else if (args[0].equalsIgnoreCase("reset")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "reset")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}
				UUID ownerUUID = this.instance.getAreaFileManager().getOwner(this.type, shopId);
				ItemStack[] contents = rentHandler.getSellInv() != null ? rentHandler.getSellInv().getContents() : null;
				if(ownerUUID != null && contents != null)
					this.instance.getShopCacheFileManager().setShopBackup(ownerUUID, shopId, contents);

				BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, shopId);
				BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, shopId);
				this.instance.getBackupManager().paste(this.type, shopId, min, max, p.getWorld(), false);
				this.instance.getAreaFileManager().clearMember(this.type, shopId);

				if(this.instance.getNpcUtils() != null) {
					if (this.instance.getNpcUtils().isNPCSpawned(shopId))
						this.instance.getNpcUtils().despawnNPC(shopId);
				}else {
					if(this.instance.getVillagerUtils().isVillagerSpawned(shopId))
						this.instance.getVillagerUtils().destroyVillager(shopId);
				}

				rentHandler.reset(this.instance);
				
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".autoPaymentDefault") ? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".autoPaymentDefault") : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
				this.instance.getShopsSQL().reset(shopId, autoPaymentDefault);
				this.instance.getAreaFileManager().setOwner(this.type, shopId, null);
				
				this.instance.getPermissionsSQL().reset(this.type, shopId);
				this.instance.getMethodes().clearPlayersFromRegion(type, shopId, p.getWorld());

				this.instance.getDoorFileManager().closeDoors(this.type, shopId);
				this.instance.getAreaFileManager().unsetDoorClosed(this.type, shopId);
				this.instance.getMethodes().updateSign(this.type, shopId);

				p.sendMessage(this.instance.getMessage("shopReseted").replace("%shopId%", String.valueOf(shopId)));
				return true;

			} else if (args[0].equalsIgnoreCase("delete")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "delete")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				if(this.instance.getNpcUtils() != null) {
					if (this.instance.getNpcUtils().isNPCSpawned(shopId)) {
						// SHOP IS OWNED AND NPC SPAWNED
						this.instance.getNpcUtils().destroyNPC(shopId);
						this.instance.getNPCFileManager().removeNPCinConfig(shopId);
					}
				}else {
					if(this.instance.getVillagerUtils().isVillagerSpawned(shopId)) {
						this.instance.getVillagerUtils().destroyVillager(shopId);
						this.instance.getNPCFileManager().removeNPCinConfig(shopId);
					}
				}

				UUID ownerUUID = this.instance.getAreaFileManager().getOwner(this.type, shopId);
				ItemStack[] contents = rentHandler.getSellInv() != null ? rentHandler.getSellInv().getContents() : null;
				if(ownerUUID != null && contents != null)
					this.instance.getShopCacheFileManager().setShopBackup(ownerUUID, shopId, contents);

				BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, shopId);
				BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, shopId);
				this.instance.getBackupManager().paste(this.type, shopId, min, max, p.getWorld(), false);
				this.instance.getBackupManager().deleteSchem(this.type, shopId);
				this.instance.getMethodes().deleteType(this.type, shopId);
				this.instance.getMethodes().deleteArea(p, this.type, shopId);
				this.instance.getMethodes().deleteSigns(this.type, shopId);
				this.instance.getMethodes().clearPlayersFromRegion(this.type, shopId, p.getWorld());

				this.instance.getAreaFileManager().unsetDoorClosed(this.type, shopId);
				this.instance.getDoorFileManager().clearDoors(this.type, shopId);

				p.sendMessage(this.instance.getMessage("shopDeleted").replace("%shopId%", String.valueOf(shopId)));
				return true;

			} else if (args[0].equalsIgnoreCase("info")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "info")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				this.sendShopInfo(p, shopId);
				return true;

			} else if (args[0].equalsIgnoreCase("noInfo")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "noinfo")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Sell")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
				if (playerHandler == null) {
					p.sendMessage(this.instance.getMessage("pleaseReconnect"));
					return true;
				}

				SettingsHandler settingsHandler = playerHandler.getSettingsHandler(RentTypes.SHOP);
				if (settingsHandler == null) {
					p.sendMessage(this.instance.getMessage("pleaseReconnect"));
					return true;
				}

				if (settingsHandler.isSettingActive(shopId, Settings.shopMessaging)) {
					settingsHandler.setSetting(shopId, Settings.shopMessaging, false);
					this.instance.getPlayerSettingSQL().setSetting(uuid, this.type, shopId, Settings.shopMessaging, false);
					p.sendMessage(this.instance.getMessage("shopMessageNoInfoOFF"));
					return true;
				} else {
					settingsHandler.setSetting(shopId, Settings.shopMessaging, true);
					this.instance.getPlayerSettingSQL().setSetting(uuid, this.type, shopId, Settings.shopMessaging, true);
					p.sendMessage(this.instance.getMessage("shopMessageNoInfoON"));
					return true;
				}

			} else if (args[0].equalsIgnoreCase("permissions")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "permissions")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				p.sendMessage(this.instance.getMessage("permissionListHeader"));
				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.shop").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.shop." + permsPath);

					p.sendMessage(this.instance.getMessage("permissionListBody").replace("%permission%", String.valueOf(cfgPerm)));
				}
				return true;
			} else if (args[0].equalsIgnoreCase("users")) {
				
				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "users")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin")) && !sender.hasPermission(this.instance.manageFile().getString("Permissions.shop"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				AsyncSQL sql = this.instance.getAsyncSQL();
				sql.prepareStatement("SELECT * FROM " + sql.t_perms + " WHERE type='" + this.type + "' AND id='" + shopId + "' AND value='" + 1 + "';", new Consumer<ResultSet>() {

					@Override
					public void accept(ResultSet rs) {
						try {
							HashMap<UUID, List<String>> hash = new HashMap<>();

							while (rs.next()) {
								UUID targetUUID = UUID.fromString(rs.getString("userUUID"));
								List<String> list = new ArrayList<>();
								if (hash.get(targetUUID) != null)
									list = hash.get(targetUUID);

								list.add(rs.getString("permission"));
								hash.put(targetUUID, list);
							}
							
							if(hash.isEmpty()) {
								p.sendMessage(instance.getMessage("noUserPermissionsSet").replace("%type%", StringUtils.capitalize(type.toString().toLowerCase())));
								return;
							}

							for (UUID uuids : hash.keySet()) {

								OfflinePlayer target = Bukkit.getOfflinePlayer(uuids);
								String ingameName = target.getName();

								List<String> list = hash.get(uuids);
								String permissions = "";
								for (String s : list)
									permissions += ", " + s;
								permissions = permissions.substring(2);

								p.sendMessage(instance.getMessage("userPermission").replace("%player%", ingameName).replace("%permissions%", permissions));
							}
							return;
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				return true;
			}else if(args[0].equalsIgnoreCase("updateBackup")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "updatebackup")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}
				BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, shopId);
				BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, shopId);

				this.instance.getBackupManager().deleteSchem(this.type, shopId);
				this.instance.getBackupManager().save(this.type, shopId, min, max, p.getWorld());
				p.sendMessage(this.instance.getMessage("shopBackupUpdated"));
				return true;
				
			}else if (args[0].equalsIgnoreCase("help")) {
				
				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "help")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
				BookMeta meta = (BookMeta) item.getItemMeta();
				
				for(String content : this.instance.manageFile().getStringList("Messages.shopHelpBook")) {
					meta.addPage(ChatColor.translateAlternateColorCodes('&', content.replace("\\n", "\n")));
				}
				meta.setAuthor(this.instance.getDescription().getAuthors().get(0));
				meta.setTitle(this.instance.getDescription().getName());
				
				item.setItemMeta(meta);
				p.openBook(item);
				
				return true;
			}
		} else if (args.length == 2) {

			if (args[0].equalsIgnoreCase("setArea")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setarea")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int catID = Integer.parseInt(args[1]);

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, catID);

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}
				
				AsyncSQL sql = this.instance.getAsyncSQL();
				sql.prepareStatement("SELECT " + (sql.isSqlLite() ? "I" : "") + "IF( EXISTS(SELECT * FROM " + sql.t_shops + " WHERE ID='1'), (SELECT t1.id+1 as lowestId FROM " + sql.t_shops + " AS t1 LEFT JOIN " + sql.t_shops + " AS t2 ON t1.id+1 = t2.id WHERE t2.id IS NULL LIMIT 1), 1) as lowestId LIMIT 1;", new Consumer<ResultSet>() {

					@Override
					public void accept(ResultSet otherRS) {
						try {
							int shopId = 1;
							while (otherRS.next()) {
								shopId = otherRS.getInt("lowestId");
								break;
							}

							boolean success = instance.getMethodes().createArea(p, type, shopId);
							instance.getMethodes().createType(type, shopId, catID);

							if (success) {
								// CREATED
								sender.sendMessage(instance.getMessage("shopAreaCreated").replace("%shopId%", String.valueOf(shopId)));

							} else {
								// NOTE CREATED
								sender.sendMessage(instance.getMessage("shopAreaError"));
							}
							return;
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				return true;

			} else if (args[0].equalsIgnoreCase("info")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "info")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int shopId = Integer.parseInt(args[1]);

				this.sendShopInfo(p, shopId);
				return true;
				
			} else if (args[0].equalsIgnoreCase("list")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "list")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int site = Integer.parseInt(args[1]);

				this.instance.getMethodes().sendList(p, this.type, site);
				return true;
				
			} else if (args[0].equalsIgnoreCase("door")) {

				if (args[1].equalsIgnoreCase("close")) {
					
					if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "door.close")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}

					int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

					if (shopId < 0) {
						// PLAYER NOT IN SHOP AREA, CANT FIND ID
						p.sendMessage(this.instance.getMessage("notInShop"));
						return true;
					}

					RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

					if (rentHandler == null) {
						p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
						return true;
					}
					
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".disableDoorCommand") && this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".disableDoorCommand")) {
						sender.sendMessage(this.instance.getMessage("doorCommandDisabled"));
						return true;
					}


					if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Door")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
						p.sendMessage(this.instance.getMessage("notShopOwner"));
						return true;
					}

					if (this.instance.getAreaFileManager().isDoorClosed(this.type, shopId)) {
						sender.sendMessage(this.instance.getMessage("doorsAlreadyLocked"));
						return true;
					}

					this.instance.getAreaFileManager().setDoorClosed(this.type, shopId, true);
					this.instance.getDoorFileManager().closeDoors(this.type, shopId);

					sender.sendMessage(this.instance.getMessage("shopDoorClosed"));
					return true;

				} else if (args[1].equalsIgnoreCase("open")) {
					
					if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "door.open")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}

					int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

					if (shopId < 0) {
						// PLAYER NOT IN SHOP AREA, CANT FIND ID
						p.sendMessage(this.instance.getMessage("notInShop"));
						return true;
					}

					RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

					if (rentHandler == null) {
						p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
						return true;
					}
					
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".disableDoorCommand") && this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".disableDoorCommand")) {
						sender.sendMessage(this.instance.getMessage("doorCommandDisabled"));
						return true;
					}

					if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Door")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
						p.sendMessage(this.instance.getMessage("notShopOwner"));
						return true;
					}

					if (!this.instance.getAreaFileManager().isDoorClosed(this.type, shopId)) {
						sender.sendMessage(this.instance.getMessage("doorsAlreadyUnlocked"));
						return true;
					}

					this.instance.getAreaFileManager().setDoorClosed(this.type, shopId, false);

					sender.sendMessage(this.instance.getMessage("shopDoorOpened"));
					return true;

				} else if (args[1].equalsIgnoreCase("remove")) {

					if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "door.remove")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}

					Block b = p.getTargetBlockExact(7);
					
					boolean status = false;
					if(b.getState().getBlockData() instanceof Door) {
						Door door = (Door) b.getState().getBlockData();
						status = this.instance.getDoorFileManager().removeDoor(door, b.getLocation());
						
					}else if(b.getState().getBlockData() instanceof TrapDoor || b.getState().getBlockData() instanceof Gate) {
						status = this.instance.getDoorFileManager().removeDoor(b.getLocation());
						
					}else {
						p.sendMessage(this.instance.getMessage("notADoor"));
						return true;
					}
					p.sendMessage(this.instance.getMessage(status ? "shopDoorRemoved" : "shopDoorNotFound"));
					return true;

				} else {
					p.sendMessage(this.instance.getMessage("wrongDoorArgument"));
					return true;
				}

			} else if (args[0].equalsIgnoreCase("buy")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "buy")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
				if (playerHandler == null) {
					p.sendMessage(this.instance.getMessage("pleaseReconnect"));
					return true;
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
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int shopId = Integer.parseInt(args[1]);

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();
				if (ownerUUID != null) {
					p.sendMessage(instance.getMessage("shopAlreadyBought"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}
				
				int catId = rentHandler.getCatID();
				if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catId + ".usePermission") && this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catId + ".usePermission")) {
					if(!p.hasPermission(this.instance.manageFile().getString("Permissions.category") + "." + rentHandler.getType().toString().toLowerCase() + "." + catId)) {
						p.sendMessage(this.instance.getMessage("noPermsForCategory"));
						return true;
					}
				}

				double costs = catHandler.getPrice();
				int size = catHandler.getSize();
				String time = catHandler.getTime();

				if (!this.instance.getEconomy().has(p, costs)) {
					p.sendMessage(instance.getMessage("notEnoughtMoney").replace("%amount%", String.valueOf(costs - instance.getEconomy().getBalance(p))));
					return true;
				}

				String owner = rentHandler.getOwnerName();
				UUID uuidOwner = rentHandler.getOwnerUUID();
				if (uuidOwner != null) {
					p.sendMessage(instance.getMessage("shopAlreadyBought"));
					return true;
				}

				this.instance.getEconomy().withdrawPlayer(p, costs);

				this.instance.getAreaFileManager().setOwner(this.type, shopId, uuid);
				
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catHandler.getCatID() + ".autoPaymentDefault") ? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catHandler.getCatID() + ".autoPaymentDefault") : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
				this.instance.getShopsSQL().setOwner(shopId, uuid, p.getName(), autoPaymentDefault);
				
				rentHandler.setOwner(uuid, p.getName());
				
				String prefix = instance.getPermissionsAPI().getPrefix(ownerUUID);

				if(this.instance.getNpcUtils() != null)
					this.instance.getNpcUtils().spawnAndEditNPC(shopId, prefix, uuidOwner, owner);
				else
					this.instance.getVillagerUtils().spawnVillager(shopId, prefix, ownerUUID, owner);

				this.instance.getMethodes().updateSign(this.type, shopId, owner, time, costs, size);

				Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
				rentHandler.setNextPayment(ts);
				this.instance.getShopsSQL().setNextPayment(shopId, ts);

				p.teleport(instance.getAreaFileManager().getAreaSpawn(this.type, shopId));
				p.sendMessage(instance.getMessage("shopBought").replace("%shopId%", String.valueOf(shopId)));

				return true;

			} else if (args[0].equalsIgnoreCase("rollback")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "rollback")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				String playerName = args[1];
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				UUID uuidTarget = null;
				if (PlayerManager.getPlayer(playerName) != null) {
					uuidTarget = PlayerManager.getUUID(playerName);
				} else {
					uuidTarget = PlayerManager.getUUIDOffline(playerName);
				}
				
				if(uuidTarget == null) {
					p.sendMessage(this.instance.getMessage("playerDoesntExists"));
					return true;
				}

				Inventory inv = this.instance.getShopCacheFileManager().getShopBackup(uuidTarget, shopId);
				if(inv != null)
					p.openInventory(inv);
				return true;

			} else if (args[0].equalsIgnoreCase("setTime")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "settime")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				String timeS = args[1];

				if (!this.instance.getMethodes().isTimeFormat(timeS)) {
					p.sendMessage(this.instance.getMessage("notATime"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				this.instance.getMethodes().setTime(p, this.type, rentHandler.getCatID(), timeS);
				this.instance.getMethodes().updateAllSigns(this.type, shopId);

				return true;

			} else if (args[0].equalsIgnoreCase("setsize")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setsize")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}
				this.instance.getMethodes().setSize(p, shopId, rentHandler.getCatID(), args[1]);
				return true;

			} else if (args[0].equalsIgnoreCase("setPrice")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setprice")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				this.instance.getMethodes().setPrice(p, this.type, rentHandler.getCatID(), args[1]);
				return true;

			} else if (args[0].equalsIgnoreCase("sellitem")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "sellitem")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Sell")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				ItemStack item = p.getInventory().getItemInMainHand().clone();
				if (item == null || item.getType() == Material.AIR) {
					p.sendMessage(this.instance.getMessage("shopNoItemInMainHand"));
					return true;
				}

				double price = 0D;

				try {
					price = Double.parseDouble(args[1]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				item = ShopItemManager.createShopItem(this.instance, item, shopId, price); // UPDATED ITEM WITH PRICE IN IT

				Inventory inv = rentHandler.getSellInv();

				int used = 0; // CHECKS HOW MANY SLOTS ARE USED
				for (ItemStack items : inv.getContents()) {
					if (items != null && items.getType() != Material.AIR) {
						used++;

						if (items.isSimilar(item)) {
							p.sendMessage(this.instance.getMessage("shopContainsItem"));
							return true;
						}
					}
				}

				if (used == inv.getContents().length) {
					p.sendMessage(this.instance.getMessage("shopInvFull"));
					return true;
				}

				inv.addItem(item);
				p.getInventory().setItemInMainHand(null);
				this.instance.getShopsInvSQL().updateSellInv(shopId, inv.getContents()); // DATABASE UPDATE
				// this.instance.getMethodes().updateSellInv(p, shopId, inv.getContents()); // UPDATE DATABASE AND OPEN INVS

				p.sendMessage(this.instance.getMessage("shopItemAdded").replace("%price%", String.valueOf(price)).replace("%type%", StringUtils.capitalize(item.getType().toString())).replace("%amount%", String.valueOf(item.getAmount())));

				return true;

			} else if (args[0].equalsIgnoreCase("buyitem")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "buyitem")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();

				if (ownerUUID == null) {
					p.sendMessage(this.instance.getMessage("shopNotBought"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Buy")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				ItemStack item = p.getInventory().getItemInMainHand().clone();
				if (item == null || item.getType() == Material.AIR) {
					p.sendMessage(this.instance.getMessage("shopNoItemInMainHand"));
					return true;
				}

				double price = 0D;

				try {
					price = Double.parseDouble(args[1]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				this.setBuyItem(p, rentHandler, catHandler, item, price);
				return true;

			}
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("buyitem")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "buyitem")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();

				if (ownerUUID == null) {
					p.sendMessage(this.instance.getMessage("shopNotBought"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Buy")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				if (Material.matchMaterial(args[1].toUpperCase()) == null) {
					p.sendMessage(this.instance.getMessage("notAMaterial"));
					return true;
				}

				Material m = Material.getMaterial(args[1].toUpperCase());
				double price = 0D;

				try {
					price = Double.parseDouble(args[2]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				ItemStack item = new ItemStack(m);
				item.setAmount(1);
				this.setBuyItem(p, rentHandler, catHandler, item, price);
				return true;

			} else if (args[0].equalsIgnoreCase("door")) {

				Block b = p.getTargetBlockExact(7);

				int shopId = 0;
				try {
					shopId = Integer.parseInt(args[2]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				if (args[1].equalsIgnoreCase("add")) {

					if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "door.add")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}
					
					if (this.instance.getDoorFileManager().isProtectedDoor(b.getLocation())) {
						p.sendMessage(this.instance.getMessage("shopDoorAlreadyAdded"));
						return true;
					}

					if(b.getState().getBlockData() instanceof Door) {
						Door door = (Door) b.getState().getBlockData();
						this.instance.getDoorFileManager().addDoor(door, b.getLocation(), this.type, shopId);
					}else if(b.getState().getBlockData() instanceof TrapDoor || b.getState().getBlockData() instanceof Gate) {
						this.instance.getDoorFileManager().addDoor(b.getLocation(), this.type, shopId);
					}else {
						p.sendMessage(this.instance.getMessage("notADoor"));
						return true;
					}
					p.sendMessage(this.instance.getMessage("shopDoorAdded").replace("%shopId%", String.valueOf(shopId)));
					return true;

				} else {
					p.sendMessage(this.instance.getMessage("wrongDoorAdminArgument"));
					return true;
				}

			}
		} else if (args.length == 4) {
			if (args[0].equalsIgnoreCase("setPermission")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "setpermission")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
					p.sendMessage(this.instance.getMessage("notABoolean"));
					return true;
				}

				String target = args[1];
				String permission = args[2];
				boolean value = args[3].equalsIgnoreCase("true") ? true : false;

				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.shop").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.shop." + permsPath);
					if (permission.equalsIgnoreCase(cfgPerm)) {

						UUID uuidTarget = null;
						if (PlayerManager.getPlayer(target) != null) {
							uuidTarget = PlayerManager.getUUID(target);
						} else {
							uuidTarget = PlayerManager.getUUIDOffline(target);
						}

						//IF TARGET IS ONLINE
						PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuidTarget);
						if (playerHandler != null) {
							PermissionsHandler permsHandler = playerHandler.getPermsHandler(this.type);
							if (permsHandler != null) {
								permsHandler.setPermission(shopId, permission, value);
							}
						}

						this.instance.getPermissionsSQL().setPermission(uuidTarget, this.type, shopId, permission, value);

						if (this.instance.manageFile().getString("UserPermissions.shop.Admin").equalsIgnoreCase(permission) || this.instance.manageFile().getString("UserPermissions.shop.Build").equalsIgnoreCase(permission)) {

							if (value) {
								this.instance.getAreaFileManager().addMember(this.type, shopId, uuidTarget);
								this.instance.getMethodes().addMemberToRegion(this.type, shopId, this.instance.getAreaFileManager().getWorldFromArea(this.type, shopId), uuidTarget);

							}else if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuidTarget, this.instance.manageFile().getString("UserPermissions.shop.Admin"))
									&& !this.instance.getMethodes().hasPermission(this.type, shopId, uuidTarget, this.instance.manageFile().getString("UserPermissions.shop.Build"))) {

								this.instance.getAreaFileManager().removeMember(this.type, shopId, uuidTarget);
								this.instance.getMethodes().removeMemberToRegion(this.type, shopId, this.instance.getAreaFileManager().getWorldFromArea(this.type, shopId), uuidTarget);
							}
						}

						p.sendMessage(this.instance.getMessage("permissionSet").replace("%permission%", String.valueOf(permission)).replace("%player%", String.valueOf(target)).replace("%status%", String.valueOf(value)));
						return true;
					}
				}
				// PERM DOESNT EXISTS
				p.sendMessage(this.instance.getMessage("notAPermission"));
				return true;
				
			} else if (args[0].equalsIgnoreCase("buyitem")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "buyitem")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();

				if (ownerUUID == null) {
					p.sendMessage(this.instance.getMessage("shopNotBought"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Buy")) && !this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				if (Material.matchMaterial(args[1].toUpperCase()) == null) {
					p.sendMessage(this.instance.getMessage("notAMaterial"));
					return true;
				}

				Material m = Material.getMaterial(args[1].toUpperCase());
				int amount = 1;
				double price = 0D;

				try {
					amount = Integer.parseInt(args[2]);
					price = Double.parseDouble(args[3]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				ItemStack item = new ItemStack(m);
				item.setAmount(amount);
				this.setBuyItem(p, rentHandler, catHandler, item, price);
				return true;
			}
			
		} else if (args.length == 5) {
			if (args[0].equalsIgnoreCase("createCat")) {

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "createcat")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int catID = 0;
				int price = 0;
				int size = 0;
				String timeS = args[4];

				// SHOP PRICE
				try {
					catID = Integer.parseInt(args[1]);
					price = Integer.parseInt(args[2]);
					size = Integer.parseInt(args[3]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				if (size < 0 || size % 9 > 0 || size > 54) {
					p.sendMessage(this.instance.getMessage("shopSizeNotValid"));
					return true;
				}

				if (!this.instance.getMethodes().isTimeFormat(timeS)) {
					p.sendMessage(this.instance.getMessage("notATime"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, catID);

				if (catHandler == null) {
					catHandler = new CategoryHandler(catID, price, timeS);

					HashMap<Integer, CategoryHandler> hash = new HashMap<>();
					if(this.instance.catHandlers.containsKey(this.type))
						hash = this.instance.catHandlers.get(this.type);
					
					hash.put(catID, catHandler);
					this.instance.catHandlers.put(this.type, hash);
					
					catHandler.setSize(size);
				}else {
					catHandler.setPrice(price);
					catHandler.setTime(timeS);
					catHandler.setSize(size);
				}

				this.instance.getCategorySQL().updateShopCategory(catID, size, price, timeS);
				this.instance.getMethodes().updateAllSigns(this.type, catID);
				p.sendMessage(this.instance.getMessage("shopCategoryUpdated").replace("%catId%", String.valueOf(catID)).replace("%price%", String.valueOf(price)).replace("%size%", String.valueOf(size)).replace("%time%", timeS));
				return true;
			}
		}

		if (p.hasPermission(this.instance.manageFile().getString("Permissions.shop")))
			this.sendHelp(p, "shopAdminHelp");
		else
			this.sendHelp(p, "shopUserHelp");

		return true;
	}

	private void sendHelp(Player p, String path) {
		for (String s : this.instance.manageFile().getStringList("Messages." + path)) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.instance.translateHexColorCodes(s)));
		}
	}

	private void sendShopInfo(Player p, int shopId) {

		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

		if (rentHandler == null)
			return;

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

		if (catHandler == null)
			return;
		
		double costs = catHandler.getPrice();
		int size = catHandler.getSize();
		String time = catHandler.getTime();
		boolean doorsClosed = this.instance.getAreaFileManager().isDoorClosed(this.type, shopId);
		Location loc = this.instance.getAreaFileManager().getAreaSpawn(this.type, shopId);
		
		String ownerName = rentHandler.getOwnerName();

		for (String s : this.instance.manageFile().getStringList("Messages.shopInfo")) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.instance.translateHexColorCodes(s))
					.replaceAll("(?i)%" + "shopid" + "%", String.valueOf(shopId))
					.replaceAll("(?i)%" + "catid" + "%", String.valueOf(rentHandler.getCatID()))
					.replaceAll("(?i)%" + "owner" + "%", ownerName == null ? "" : ownerName)
					.replaceAll("(?i)%" + "price" + "%", String.valueOf(costs))
					.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
					.replaceAll("(?i)%" + "time" + "%", time)
					.replaceAll("(?i)%" + "doorstatus" + "%", String.valueOf(doorsClosed))
					.replaceAll("(?i)%" + "x" + "%", String.valueOf(loc.getBlockX()))
					.replaceAll("(?i)%" + "y" + "%", String.valueOf(loc.getBlockY()))
					.replaceAll("(?i)%" + "z" + "%", String.valueOf(loc.getBlockZ()))
					.replaceAll("(?i)%" + "world" + "%", String.valueOf(loc.getWorld().getName()))
			);
		}
	}

	private void setBuyItem(Player p, RentTypeHandler rentHandler, CategoryHandler catHandler, ItemStack item, double price) {

		item = ShopItemManager.createShopItem(this.instance, item, rentHandler.getShopID(), price); // UPDATED ITEM WITH PRICE IN IT

		Inventory inv = rentHandler.getBuyInv();
		
		int used = 0; // CHECKS HOW MANY SLOTS ARE USED
		for (ItemStack items : inv.getContents()) {
			if (items != null && items.getType() != Material.AIR) {
				used++;

				if (items.isSimilar(item)) {
					p.sendMessage(this.instance.getMessage("shopContainsItem"));
					return;
				}
			}
		}

		if (used == inv.getContents().length) {
			p.sendMessage(this.instance.getMessage("shopInvFull"));
			return;
		}

		inv.addItem(item);

		this.instance.getShopsInvSQL().updateBuyInv(rentHandler.getShopID(), inv.getContents()); // DATABASE UPDATE

		p.sendMessage(this.instance.getMessage("shopItemAdded").replace("%price%", String.valueOf(price)).replace("%type%", StringUtils.capitalize(item.getType().toString())).replace("%amount%", String.valueOf(item.getAmount())));
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		List<String> list = new ArrayList<>();
		
		if(!(sender instanceof Player))
			return list;
		
		Player p = (Player) sender;
		
		if(args.length == 1) {
			for(String subCMD : this.adminSubCommands) {
				if(this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", subCMD)) {
					if(subCMD.toLowerCase().startsWith(args[0].toLowerCase())) {
						list.add(subCMD);
					}
				}
			}
		
			for(String subCMD : this.subCommands) {
				if(this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", subCMD)) {
					if(subCMD.toLowerCase().startsWith(args[0].toLowerCase())) {
						list.add(subCMD);
					}
				}
			}
	
		}else if(args.length == 2 && args[0].equalsIgnoreCase("door")) {
			if("add".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "door.add")) {
				list.add("add");
			}else if("remove".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "door.remove")) {
				list.add("remove");
			}else if("open".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "door.open")) {
				list.add("open");
			}else if("close".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "door.close")) {
				list.add("close");
			}
		}
		
		return list;
	}
}
