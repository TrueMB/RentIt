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
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.database.connector.AsyncSQL;
import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.gui.SearchResultGUI;
import me.truemb.rentit.gui.UserShopGUI;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PermissionsHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.handler.SettingsHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;
import me.truemb.rentit.utils.ShopItemManager;
import me.truemb.rentit.utils.UtilitiesAPI;

public class ShopCOMMAND extends BukkitCommand {

	private Main instance;
	private List<String> subCommands = new ArrayList<>();
	private List<String> adminSubCommands = new ArrayList<>();

	private RentTypes type = RentTypes.SHOP;

	public ShopCOMMAND(Main plugin) {
		super("shop", "Shop Main Command", null, plugin.manageFile().getStringList("Options.commands.shop.aliases"));
		this.instance = plugin;

		this.subCommands.add("noinfo");
		this.subCommands.add("users");
		this.subCommands.add("permissions");
		this.subCommands.add("setPermission");
		this.subCommands.add("buy");
		this.subCommands.add("sellItem");
		this.subCommands.add("buyItem");
		this.subCommands.add("resign");
		this.subCommands.add("search");
		this.subCommands.add("help");

		this.adminSubCommands.add("createCat");
		this.adminSubCommands.add("deleteCat");
		this.adminSubCommands.add("setAliasCat");
		this.adminSubCommands.add("listCat");
		this.adminSubCommands.add("setNPC");
		this.adminSubCommands.add("setArea");
		this.adminSubCommands.add("setAlias");
		this.adminSubCommands.add("reset");
		this.adminSubCommands.add("delete");
		this.adminSubCommands.add("updateBackup");
		this.adminSubCommands.add("info");
		this.adminSubCommands.add("door");
		this.adminSubCommands.add("rollback");
		this.adminSubCommands.add("setTime");
		this.adminSubCommands.add("setSize");
		this.adminSubCommands.add("setPrice");
		this.adminSubCommands.add("list");
		

		//DISABLED COMMANDS
		this.instance.manageFile().getStringList("Options.commands.shop.disabledSubCommands").forEach(disabledSubCmds -> {
			new ArrayList<>(this.subCommands).forEach(subCmds -> {
				if(subCmds.equalsIgnoreCase(disabledSubCmds)) {
					this.subCommands.remove(subCmds);
				}
			});
			new ArrayList<>(this.adminSubCommands).forEach(adminSubCmds -> {
				if(adminSubCmds.equalsIgnoreCase(disabledSubCmds)) {
					this.adminSubCommands.remove(adminSubCmds);
				}
			});
		});
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();

		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("setNPC")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setnpc")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				} else {

		        	RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

					if (rentHandler == null) {
						p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
						return true;
					}
					
					CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

					if (catHandler == null) {
						p.sendMessage(this.instance.getMessage("categoryError"));
						return true;
					}
					
		        	boolean allowUsersToMoveNPC = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catHandler.getCatID() + "." + CategorySettings.allowUsersToMoveNPC.toString()) 
		        			? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catHandler.getCatID() + "." + CategorySettings.allowUsersToMoveNPC.toString()) : false;
		        	
					if((rentHandler.getOwnerUUID() == null || !rentHandler.getOwnerUUID().equals(uuid) || !allowUsersToMoveNPC) && !this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setnpc")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}
					
					//SET NPC LOCATION
					this.instance.getNPCFileManager().setNPCLocForShop(shopId, p.getLocation());

					if(!instance.manageFile().getBoolean("Options.disableNPC")) {
						if(instance.manageFile().getBoolean("Options.useNPCs")) {
							//NPC
							if(this.instance.getNpcUtils().existsNPCForShop(shopId)) {
								this.instance.getNpcUtils().moveNPC(shopId, p.getLocation());
							}else {
								//CREATE NPC IN CITIZENS DATABASE
								this.instance.getNpcUtils().createNPC(shopId);
								UUID ownerUUID = rentHandler.getOwnerUUID();
								
								//SHOP IS OWNED
								if(ownerUUID != null) {
									String ownerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
									String prefix = this.instance.getPermissionsAPI().getPrefix(ownerUUID);
									
									if(instance.getVillagerUtils() != null) {
										instance.getVillagerUtils().spawnVillager(shopId, prefix, ownerUUID, ownerName);
									}else {
										instance.getNpcUtils().spawnAndEditNPC(shopId, prefix, ownerUUID, ownerName);
									}
								}
							}
							
						}else {
							//VILLAGER
							if(this.instance.getVillagerUtils().isVillagerSpawned(shopId)) {
								this.instance.getVillagerUtils().moveVillager(shopId, p.getLocation());
							}else {
								UUID ownerUUID = rentHandler.getOwnerUUID();
								
								//SHOP IS OWNED
								if(ownerUUID != null) {
									String playerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
									String prefix = instance.getPermissionsAPI().getPrefix(ownerUUID);
	
									this.instance.getVillagerUtils().spawnVillager(shopId, prefix, ownerUUID, playerName);
								}
							}
						}
					}
					
				    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
				    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

					p.sendMessage(this.instance.getMessage("shopCitizenCreated")
							.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
							.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
							.replaceAll("(?i)%" + "alias" + "%", alias));
					return true;
				}
			} else if (args[0].equalsIgnoreCase("list")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "list")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "list")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				this.instance.getMethodes().sendList(p, this.type, 1);
				return true;
				
			} else if (args[0].equalsIgnoreCase("reset")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "reset")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
			    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				this.resetArea(p, rentHandler);
				p.sendMessage(this.instance.getMessage("shopReseted")
						.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));
				return true;

			} else if (args[0].equalsIgnoreCase("resign")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "resign")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "resign")) {
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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}
				
				if (!p.getUniqueId().equals(rentHandler.getOwnerUUID())) {
					p.sendMessage(this.instance.getMessage("notShopOwner"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
			    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				this.resetArea(p, rentHandler);
				p.sendMessage(this.instance.getMessage("shopResignContract")
						.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));
				return true;

			}else if (args[0].equalsIgnoreCase("delete")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "delete")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				if(!this.instance.manageFile().getBoolean("Options.disableNPC")) {
					if(this.instance.manageFile().getBoolean("Options.useNPCs")) {
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
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();
				
				if(ownerUUID != null)
					this.instance.getShopCacheFileManager().createShopBackup(ownerUUID, shopId);

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
				
				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
			    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

				p.sendMessage(this.instance.getMessage("shopDeleted")
						.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));
				return true;

			} else if (args[0].equalsIgnoreCase("info")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "info")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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

			} else if (args[0].equalsIgnoreCase("listcat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "listcat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}
				
				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "listcat")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				if(!this.instance.catHandlers.containsKey(this.type) || this.instance.catHandlers.get(this.type).size() <= 0) {
					p.sendMessage(this.instance.getMessage("noCategoriesExists"));
					return true;
				}

				HashMap<Integer, CategoryHandler> catHash = this.instance.catHandlers.get(this.type);
				
				for(CategoryHandler catHandler : catHash.values()) {
					int catId = catHandler.getCatID();
					double price = catHandler.getPrice();
					int size = catHandler.getSize();
					String time = catHandler.getTime();
					
				    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
					
					p.sendMessage(this.instance.getMessage("shopCategoryList")
							.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
							.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
							.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))
							.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
							.replaceAll("(?i)%" + "time" + "%", time)
							);
				}
				
				return true;

			} else if (args[0].equalsIgnoreCase("noInfo")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "noinfo")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "permissions")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "permissions")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				p.sendMessage(this.instance.getMessage("permissionListHeader"));
				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.shop").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.shop." + permsPath);

					p.sendMessage(this.instance.getMessage("permissionListBody")
							.replaceAll("(?i)%" + "permission" + "%", String.valueOf(cfgPerm)));
				}
				return true;
			} else if (args[0].equalsIgnoreCase("users")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "users")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}
				
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
								p.sendMessage(instance.getMessage("noUserPermissionsSet")
										.replaceAll("(?i)%" + "type" + "%", StringUtils.capitalize(type.toString().toLowerCase())));
								return;
							}

							for (UUID uuids : hash.keySet()) {

								OfflinePlayer target = Bukkit.getOfflinePlayer(uuids);
								String ingameName = target != null && target.getName() != null ? target.getName() : PlayerManager.getName(uuids.toString());

								List<String> list = hash.get(uuids);
								String permissions = "";
								for (String s : list)
									permissions += ", " + s;
								permissions = permissions.substring(2);

								p.sendMessage(instance.getMessage("userPermission")
										.replaceAll("(?i)%" + "player" + "%", ingameName)
										.replaceAll("(?i)%" + "permissions" + "%", permissions));
							}
							return;
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				return true;
			}else if(args[0].equalsIgnoreCase("updateBackup")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "updatebackup")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "help")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}
				
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
			}else if (args[0].equalsIgnoreCase("rollback")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "rollback")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "rollback")) {
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
				
				if(rentHandler.getOwnerUUID() != null && rentHandler.getOwnerUUID().equals(uuid)) {
					p.sendMessage(this.instance.getMessage("shopStillOwning"));
					return true;
				}
				
				ShopInventoryBuilder builder = new ShopInventoryBuilder(p, rentHandler, ShopInventoryType.ROLLBACK);
				
				if(builder.getCurrentInventory() == null) {
					p.sendMessage(this.instance.getMessage("shopRollbackNoItems"));
					return true;
				}
				
				this.instance.setShopInvBuilder(uuid, builder);
				builder.build();
				
				return true;
			}
		} else if (args.length == 2) {

			if (args[0].equalsIgnoreCase("setArea")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setarea")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
							RentTypeHandler rentHandler = instance.getMethodes().createType(type, shopId, catID);

							if (success) {
								// CREATED
							    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
							    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
							    
								sender.sendMessage(instance.getMessage("shopAreaCreated")
										.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
										.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
										.replaceAll("(?i)%" + "alias" + "%", alias));

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

			} else if (args[0].equalsIgnoreCase("setAliasCat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setAliasCat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setAliasCat")) {
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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
				
				if(catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}
				
				String alias = args[1];
				alias = alias.substring(0, alias.length() > 100 ? 100 : alias.length());

				this.instance.getCategorySQL().setAlias(catHandler.getCatID(), this.type, alias);
				catHandler.setAlias(alias);
				
				p.sendMessage(this.instance.getMessage("shopCategoryChangedAlias")
						.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catHandler.getCatID()))
						.replaceAll("(?i)%" + "catAlias" + "%", alias));
				
				return true;
				
			} else if (args[0].equalsIgnoreCase("setAlias")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setAlias")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "setAlias")) {
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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}
				
				String alias = args[1];
				alias = alias.substring(0, alias.length() > 100 ? 100 : alias.length());

				this.instance.getShopsSQL().setAlias(shopId, alias);
				rentHandler.setAlias(alias);
				
				p.sendMessage(this.instance.getMessage("shopChangedAlias")
						.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
						.replaceAll("(?i)%" + "alias" + "%", alias));
				return true;
				
			}else if (args[0].equalsIgnoreCase("info")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "info")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
				
			} else if (args[0].equalsIgnoreCase("deletecat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "deletecat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "deletecat")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int catId = Integer.parseInt(args[1]);

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, catId);
				
				if(catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}

			    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				if(this.instance.rentTypeHandlers.containsKey(this.type)) {
					for(RentTypeHandler handler : this.instance.rentTypeHandlers.get(this.type).values()) {
						if(handler.getCatID() == catId) {
						    
							p.sendMessage(this.instance.getMessage("shopCouldntDeleteCategory")
									.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
									.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
							return true;
						}
					}
				}
				
				this.instance.catHandlers.get(this.type).remove(catId);
				this.instance.getCategorySQL().delete(this.type, catId);
			    
				p.sendMessage(this.instance.getMessage("shopCategoryDeleted")
						.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
						.replaceAll("(?i)%" + "catAlias" + "%", String.valueOf(catAlias)));
				
				return true;
				
			} else if (args[0].equalsIgnoreCase("list")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "list")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "door")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString()) 
							&& this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString())) {
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
						p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
						return true;
					}
					
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString()) 
							&& this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString())) {
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
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "buy")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();
				if (ownerUUID != null) {
					p.sendMessage(this.instance.getMessage("shopAlreadyBought"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}
				
				int catId = rentHandler.getCatID();
				if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catId + "." + CategorySettings.usePermission.toString()) 
						&& this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catId + "." + CategorySettings.usePermission.toString())) {
					if(!p.hasPermission(this.instance.manageFile().getString("Permissions.category") + "." + rentHandler.getType().toString().toLowerCase() + "." + catId)) {
						p.sendMessage(this.instance.getMessage("noPermsForCategory"));
						return true;
					}
				}

				double costs = catHandler.getPrice();
				int size = catHandler.getSize();
				String time = catHandler.getTime();

				if (!this.instance.getEconomySystem().has(p, costs)) {
					p.sendMessage(this.instance.getMessage("notEnoughMoney")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(costs - this.instance.getEconomySystem().getBalance(p))));
					return true;
				}

				String owner = rentHandler.getOwnerName();
				UUID uuidOwner = rentHandler.getOwnerUUID();
				if (uuidOwner != null) {
					p.sendMessage(this.instance.getMessage("shopAlreadyBought"));
					return true;
				}

				this.instance.getEconomySystem().withdraw(p, costs);

				this.instance.getAreaFileManager().setOwner(this.type, shopId, uuid);
				
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) 
	        			? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
				this.instance.getShopsSQL().setOwner(shopId, uuid, p.getName(), autoPaymentDefault);
				
				rentHandler.setOwner(uuid, p.getName());
				
				String prefix = instance.getPermissionsAPI().getPrefix(ownerUUID);
				if(!instance.manageFile().getBoolean("Options.disableNPC")) {
					if(instance.manageFile().getBoolean("Options.useNPCs")) {
						instance.getNpcUtils().spawnAndEditNPC(shopId, prefix, ownerUUID, owner);
					}else {
						instance.getVillagerUtils().spawnVillager(shopId, prefix, ownerUUID, owner);
					}
				}

				this.instance.getMethodes().updateSign(this.type, shopId, owner, time, costs, size);

				Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
				rentHandler.setNextPayment(ts);
				this.instance.getShopsSQL().setNextPayment(shopId, ts);
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
			    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				p.teleport(this.instance.getAreaFileManager().getAreaSpawn(this.type, shopId));
				p.sendMessage(this.instance.getMessage("shopBought")
						.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));

				return true;

			} else if (args[0].equalsIgnoreCase("rollback")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "rollback")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "rollback")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				String target = args[1];
				int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (shopId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				UUID uuidTarget = null;
				if (Bukkit.getPlayer(target) != null) {
					uuidTarget = Bukkit.getPlayer(target).getUniqueId();
				} else if (this.instance.manageFile().getBoolean("Options.offlineMode")) {
					uuidTarget = PlayerManager.generateOfflineUUID(target);
				} else {
					uuidTarget = PlayerManager.getUUIDOffline(target);
				}
				
				if(uuidTarget == null) {
					p.sendMessage(this.instance.getMessage("playerDoesntExists"));
					return true;
				}
				
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);
				
				if(rentHandler.getOwnerUUID() != null && rentHandler.getOwnerUUID().equals(uuidTarget)) {
					p.sendMessage(this.instance.getMessage("shopStillOwning"));
					return true;
				}

				ShopInventoryBuilder builder = new ShopInventoryBuilder(p, rentHandler, ShopInventoryType.ROLLBACK).setTarget(uuidTarget);
				
				if(builder.getCurrentInventory() == null) {
					p.sendMessage(this.instance.getMessage("shopRollbackNoItems"));
					return true;
				}
				
				this.instance.setShopInvBuilder(uuid, builder);
				builder.build();
				
				return true;

			} else if (args[0].equalsIgnoreCase("setTime")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "settime")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}

				this.instance.getMethodes().setTime(p, this.type, rentHandler.getCatID(), timeS);
				this.instance.getMethodes().updateAllSigns(this.type, shopId);

				return true;

			} else if (args[0].equalsIgnoreCase("setsize")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setsize")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}
				this.instance.getMethodes().setSize(p, shopId, rentHandler.getCatID(), args[1]);
				return true;

			} else if (args[0].equalsIgnoreCase("setPrice")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setprice")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}

				this.instance.getMethodes().setPrice(p, this.type, rentHandler.getCatID(), args[1]);
				return true;

			} else if (args[0].equalsIgnoreCase("sellItem")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "sellitem")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
				}

				this.addShopItem(p, rentHandler, catHandler, ShopInventoryType.SELL, item, price);
				return true;

			} else if (args[0].equalsIgnoreCase("buyitem")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "buyitem")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
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
				
				this.addShopItem(p, rentHandler, catHandler, ShopInventoryType.BUY, item, price);
				return true;

			} else if (args[0].equalsIgnoreCase("search")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "search")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "shop", "search")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				if (Material.matchMaterial(args[1].toUpperCase()) == null) {
					p.sendMessage(this.instance.getMessage("notAMaterial"));
					return true;
				}
				
				Material m = Material.matchMaterial(args[1]);
				
				if (!this.instance.rentTypeHandlers.containsKey(this.type)) {
					p.sendMessage(this.instance.getMessage("shopSearchNothingFound"));
					return true;
				}
				HashMap<Integer, RentTypeHandler> typeHash = this.instance.rentTypeHandlers.get(type);
				
				List<Integer> foundShopIds = new ArrayList<>();
				for(int shopId : typeHash.keySet()) {
					RentTypeHandler handler = typeHash.get((Integer) shopId);
					if(handler == null)
						continue;
					
					for(Inventory inv : handler.getInventories(ShopInventoryType.SELL)) {
						int foundAmount = inv.all(m).size();
						if(foundAmount > 0)
							foundShopIds.add(handler.getID());
					}
					
				}

				this.instance.search.put(uuid, m);
				p.openInventory(SearchResultGUI.getGUI(this.instance, uuid, 1, foundShopIds));
				return true;

			}
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("buyitem")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "buyitem")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
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
				this.addShopItem(p, rentHandler, catHandler, ShopInventoryType.BUY, item, price);
				return true;

			} else if (args[0].equalsIgnoreCase("door")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "door")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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

					RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);
					CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
					
				    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
				    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
				    
					p.sendMessage(this.instance.getMessage("shopDoorAdded")
							.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
							.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
							.replaceAll("(?i)%" + "alias" + "%", alias));
					return true;

				} else {
					p.sendMessage(this.instance.getMessage("wrongDoorAdminArgument"));
					return true;
				}

			}
		} else if (args.length == 4) {
			if (args[0].equalsIgnoreCase("setPermission")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "setpermission")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
				String permission = args[2].toLowerCase();
				boolean value = args[3].equalsIgnoreCase("true") ? true : false;

				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.shop").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.shop." + permsPath);
					if (permission.equalsIgnoreCase(cfgPerm)) {

						UUID[] uuidTarget = {null};
						if (Bukkit.getPlayer(target) != null) {
							uuidTarget[0] = Bukkit.getPlayer(target).getUniqueId();
						} else if (this.instance.manageFile().getBoolean("Options.offlineMode")) {
							uuidTarget[0] = PlayerManager.generateOfflineUUID(target);
						} else {
							uuidTarget[0] = PlayerManager.getUUIDOffline(target);
						}

						//IF TARGET IS ONLINE
						PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuidTarget[0]);
						if (playerHandler != null) {
							PermissionsHandler permsHandler = playerHandler.getPermsHandler(this.type);
							if (permsHandler != null) {
								permsHandler.setPermission(shopId, permission, value);
							}
						}

						this.instance.getPermissionsSQL().setPermission(uuidTarget[0], this.type, shopId, permission, value);

						if (this.instance.manageFile().getString("UserPermissions.shop.Admin").equalsIgnoreCase(permission) || this.instance.manageFile().getString("UserPermissions.shop.Build").equalsIgnoreCase(permission)) {

							if (value) {
								this.instance.getAreaFileManager().addMember(this.type, shopId, uuidTarget[0]);
								this.instance.getMethodes().addMemberToRegion(this.type, shopId, this.instance.getAreaFileManager().getWorldFromArea(this.type, shopId), uuidTarget[0]);

							}else {

								if (playerHandler != null && playerHandler.getPermsHandler(this.type) != null) {
									String adminPerm = this.instance.manageFile().getString("UserPermissions." + this.type.toString().toLowerCase() + ".Admin");
									String buildPerm = this.instance.manageFile().getString("UserPermissions." + this.type.toString().toLowerCase() + ".Build");
									
									PermissionsHandler permsHandler = playerHandler.getPermsHandler(this.type);
									
									if(!permsHandler.hasPermission(shopId, adminPerm) && !permsHandler.hasPermission(shopId, buildPerm)) {
										this.instance.getAreaFileManager().removeMember(this.type, shopId, uuidTarget[0]);
										this.instance.getMethodes().removeMemberFromRegion(this.type, shopId, this.instance.getAreaFileManager().getWorldFromArea(this.type, shopId), uuidTarget[0]);
									}
								}else {
									this.instance.getPermissionsSQL().hasBuildPermissions(uuidTarget[0], this.type, shopId, new Consumer<Boolean>() {
										
										@Override
										public void accept(Boolean b) {
											if(!b) {
												instance.getAreaFileManager().removeMember(type, shopId, uuidTarget[0]);
												instance.getMethodes().removeMemberFromRegion(type, shopId, instance.getAreaFileManager().getWorldFromArea(type, shopId), uuidTarget[0]);
											}
										}
									});
								}
							}
						}

						RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);
						CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

					    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
					    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
					    
						p.sendMessage(this.instance.getMessage("permissionSet")
								.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
								.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
								.replaceAll("(?i)%" + "alias" + "%", alias)
								.replaceAll("(?i)%" + "permission" + "%", String.valueOf(permission))
								.replaceAll("(?i)%" + "player" + "%", String.valueOf(target))
								.replaceAll("(?i)%" + "status" + "%", String.valueOf(value)));
						return true;
					}
				}
				// PERM DOESNT EXISTS
				p.sendMessage(this.instance.getMessage("notAPermission"));
				return true;
				
			} else if (args[0].equalsIgnoreCase("buyitem")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "buyitem")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

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
					p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(this.instance.getMessage("categoryError"));
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
				this.addShopItem(p, rentHandler, catHandler, ShopInventoryType.BUY, item, price);
				return true;
			}
			
		} else if (args.length == 5 || args.length == 6) {
			if (args[0].equalsIgnoreCase("createCat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "createcat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "createcat")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int catID = 0;
				int price = 0;
				int size = 0;
				int maxSite = 1;
				String timeS = args.length == 6 ? args[5] : args[4];

				// SHOP PRICE
				try {
					catID = Integer.parseInt(args[1]);
					price = Integer.parseInt(args[2]);
					size = Integer.parseInt(args[3]);
					maxSite = args.length == 6 ? Integer.parseInt(args[4]) : 1;
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
					catHandler.setMaxSite(maxSite);
				}else {
					catHandler.setPrice(price);
					catHandler.setTime(timeS);
					catHandler.setSize(size);
					catHandler.setMaxSite(maxSite);
				}

			    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				this.instance.getCategorySQL().updateShopCategory(catID, size, price, timeS);
				this.instance.getMethodes().updateAllSigns(this.type, catID);
				
				p.sendMessage(this.instance.getMessage("shopCategoryUpdated")
						.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catID))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "price" + "%", String.valueOf(price))
						.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
						.replaceAll("(?i)%" + "maxSite" + "%", String.valueOf(maxSite))
						.replaceAll("(?i)%" + "time" + "%", timeS));
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
		
	    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		for (String s : this.instance.manageFile().getStringList("Messages.shopInfo")) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.instance.translateHexColorCodes(s))
					.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
					.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catHandler.getCatID()))
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "owner" + "%", ownerName == null ? "" : ownerName)
					.replaceAll("(?i)%" + "price" + "%", String.valueOf(costs))
					.replaceAll("(?i)%" + "size" + "%", String.valueOf(size))
					.replaceAll("(?i)%" + "sites" + "%", String.valueOf(catHandler.getMaxSite()))
					.replaceAll("(?i)%" + "time" + "%", time)
					.replaceAll("(?i)%" + "doorstatus" + "%", String.valueOf(doorsClosed))
					.replaceAll("(?i)%" + "x" + "%", String.valueOf(loc.getBlockX()))
					.replaceAll("(?i)%" + "y" + "%", String.valueOf(loc.getBlockY()))
					.replaceAll("(?i)%" + "z" + "%", String.valueOf(loc.getBlockZ()))
					.replaceAll("(?i)%" + "world" + "%", String.valueOf(loc.getWorld().getName()))
			);
		}
	}

	private void addShopItem(Player p, RentTypeHandler rentHandler, CategoryHandler catHandler, ShopInventoryType shopInvType, ItemStack item, double price) {
		
		//Check if Item Blacklisted
		List<String> blacklistedItems = this.instance.manageFile().getStringList("Options.categorySettings.ShopCategory." + String.valueOf(catHandler.getCatID()) + ".blacklistedItems");
		for(String blacklistedMaterial : blacklistedItems) {
			if(item.getType().toString().equalsIgnoreCase(blacklistedMaterial)) {
				p.sendMessage(this.instance.getMessage("shopItemBlacklisted"));
				return;
			}
		}
		
		item = ShopItemManager.createShopItem(this.instance, item, rentHandler.getID(), price); // UPDATED ITEM WITH PRICE IN IT

		int site = rentHandler.getInventories(shopInvType).size();
		boolean multiSite = catHandler.getMaxSite() > 1;
		if(site > catHandler.getMaxSite())
			site = catHandler.getMaxSite();
		
		Inventory currentLastInv = rentHandler.getInventory(shopInvType, site);

		//Check if Item is already in the Shop
		for(Inventory inventories : rentHandler.getInventories(shopInvType)) {
			for (int i = 0; i < inventories.getSize() - (multiSite ? 9 : 0); i++) {
				ItemStack items = inventories.getItem(i);
				if (items != null && items.getType() != Material.AIR) {
	
					if (items.isSimilar(item)) {
						p.sendMessage(this.instance.getMessage("shopContainsItem"));
						return;
					}
				}
			}
		}

		int used = 0;
		for (int i = 0; i < currentLastInv.getSize() - (multiSite ? 9 : 0); i++) {
			ItemStack items = currentLastInv.getItem(i);
			if (items != null && items.getType() != Material.AIR) {
				used++;
			}
		}
		

		if (used >= currentLastInv.getSize() - (multiSite ? 9 : 0)) {
			if(site >= catHandler.getMaxSite()) {
				p.sendMessage(this.instance.getMessage("shopInvFull"));
				return;
			}else {
				//Site is full but a new one can be created
				
				ShopInventoryBuilder builder = new ShopInventoryBuilder(p, rentHandler, shopInvType);
				builder.setSite(site);
				
				//New last Inventory
				currentLastInv = UserShopGUI.getInventory(this.instance, builder);
			}
		}
		
		//Add the Item to the Shop
		currentLastInv.addItem(item);
		
		//TODO Next Item missing?

		//Remove the multi Site Row, so that this is not in the Database
		ItemStack[] content = null;
		if(multiSite) {
			content = new ItemStack[currentLastInv.getSize() - 9];
			
			for(int i = 0; i < content.length; i++)
				content[i] = currentLastInv.getContents()[i];
		} else
			content = currentLastInv.getContents();
		

		//Remove item if the player wants to sell it in the Shop
		if(shopInvType == ShopInventoryType.SELL)
			p.getInventory().setItemInMainHand(null);

		this.instance.getShopsInvSQL().updateInventory(rentHandler.getID(), shopInvType, site, content); // DATABASE UPDATE
		
		String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(rentHandler.getID());
		String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			
		String type = StringUtils.capitalize(item.getType().toString());
		String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : type;
		    
		p.sendMessage(this.instance.getMessage("shopItemAdded")
				.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(rentHandler.getID()))
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
				.replaceAll("(?i)%" + "alias" + "%", alias)
				.replaceAll("(?i)%" + "price" + "%", String.valueOf(price))
				.replaceAll("(?i)%" + "itemname" + "%", itemName)
				.replaceAll("(?i)%" + "type" + "%", type)
				.replaceAll("(?i)%" + "amount" + "%", String.valueOf(item.getAmount())));
	}
	
	private void resetArea(Player p, RentTypeHandler rentHandler) {
		
		int shopId = rentHandler.getID();
		UUID ownerUUID = rentHandler.getOwnerUUID();

		if(ownerUUID != null)
			this.instance.getShopCacheFileManager().createShopBackup(ownerUUID, shopId);

		if(!instance.manageFile().getBoolean("Options.disableNPC")) {
			if(instance.manageFile().getBoolean("Options.useNPCs")) {
				if (this.instance.getNpcUtils().isNPCSpawned(shopId))
					this.instance.getNpcUtils().despawnNPC(shopId);
			}else {
				if(this.instance.getVillagerUtils().isVillagerSpawned(shopId))
					this.instance.getVillagerUtils().destroyVillager(shopId);
			}
		}

		this.instance.getChestsUtils().getShopChests(shopId).stream().forEach(chest -> chest.remove());
			
		BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, shopId);
		BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, shopId);
		this.instance.getBackupManager().paste(this.type, shopId, min, max, p.getWorld(), false);
		this.instance.getAdvancedChestsUtils().pasteChestsInArea(this.type, shopId);
		this.instance.getAreaFileManager().clearMember(this.type, shopId);

		rentHandler.reset();
		
    	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) 
    			? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) : true;
    	rentHandler.setAutoPayment(autoPaymentDefault);
		this.instance.getShopsSQL().reset(shopId, autoPaymentDefault);
		this.instance.getAreaFileManager().setOwner(this.type, shopId, null);
		
		this.instance.getPermissionsSQL().reset(this.type, shopId);
		this.instance.getMethodes().clearPlayersFromRegion(type, shopId, p.getWorld());

		this.instance.getDoorFileManager().closeDoors(this.type, shopId);
		this.instance.getAreaFileManager().unsetDoorClosed(this.type, shopId);
		this.instance.getMethodes().updateSign(this.type, shopId);
		
		this.instance.getShopsInvSQL().resetInventories(shopId);

	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		
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

		}else if(args.length == 2 && args[0].equalsIgnoreCase("search")) {
			for(Material m : Material.values())
				if(m.toString().toLowerCase().startsWith(args[1].toLowerCase()))
					list.add(m.toString());
			
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
			
		}else if(args.length > 2 && args[0].equalsIgnoreCase("setPermission")) {
			if(args.length == 2) {
				
				for(Player all : Bukkit.getOnlinePlayers())
					if(all.getName().toLowerCase().startsWith(args[1].toLowerCase()))
						list.add(all.getName());
				
			}else if(args.length == 3) {

				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.shop").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.shop." + permsPath);
					
					if(cfgPerm.toLowerCase().startsWith(args[2].toLowerCase()))
						list.add(cfgPerm);
				}
					
			}else if(args.length == 4) {
				
				if("true".toLowerCase().startsWith(args[3].toLowerCase()))
					list.add("true");
				
				if("false".toLowerCase().startsWith(args[3].toLowerCase()))
					list.add("false");
				
			}
		}
		
		return list;
	}
}
