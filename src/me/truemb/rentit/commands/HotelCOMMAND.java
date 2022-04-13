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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.database.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PermissionsHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;
import me.truemb.rentit.utils.UtilitiesAPI;
import net.md_5.bungee.api.ChatColor;

public class HotelCOMMAND implements CommandExecutor, TabCompleter {

	private Main instance;
	private List<String> subCommands = new ArrayList<>();
	private List<String> adminSubCommands = new ArrayList<>();
	
	private RentTypes type = RentTypes.HOTEL;

	public HotelCOMMAND(Main plugin) {
		this.instance = plugin;
		this.instance.getCommand("hotel").setExecutor(this);

		subCommands.add("users");
		subCommands.add("permissions");
		subCommands.add("setPermission");
		subCommands.add("buy");
		subCommands.add("resign");
		subCommands.add("help");

		adminSubCommands.add("createCat");
		adminSubCommands.add("deleteCat");
		adminSubCommands.add("listCat");
		adminSubCommands.add("setArea");
		adminSubCommands.add("reset");
		adminSubCommands.add("delete");
		adminSubCommands.add("updateBackup");
		adminSubCommands.add("info");
		adminSubCommands.add("door");
		adminSubCommands.add("rollback");
		adminSubCommands.add("setTime");
		adminSubCommands.add("setPrice");
		adminSubCommands.add("list");
		
		
		//DISABLED COMMANDS
		this.instance.manageFile().getStringList("Options.commands.hotel.disabledSubCommands").forEach(disabledSubCmds -> {
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
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = PlayerManager.getUUID(p);

		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("reset")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "reset")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "reset")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN hotel AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}
				
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}
				
				this.resetArea(p, rentHandler);
				p.sendMessage(this.instance.getMessage("hotelReseted").replace("%hotelId%", String.valueOf(hotelId)));
				return true;

			} else if (args[0].equalsIgnoreCase("resign")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "resign")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "resign")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN hotel AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}
				
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}
				
				if (!p.getUniqueId().equals(rentHandler.getOwnerUUID())) {
					p.sendMessage(this.instance.getMessage("notHotelOwner"));
					return true;
				}
				
				this.resetArea(p, rentHandler);
				p.sendMessage(this.instance.getMessage("hotelResignContract").replace("%hotelId%", String.valueOf(hotelId)));
				return true;

			} else if (args[0].equalsIgnoreCase("delete")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "delete")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "delete")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN hotel AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, hotelId);
				BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, hotelId);
				this.instance.getBackupManager().paste(this.type, hotelId, min, max, p.getWorld(), false);
				this.instance.getBackupManager().deleteSchem(this.type, hotelId);
				this.instance.getMethodes().deleteType(this.type, hotelId);
				this.instance.getMethodes().deleteArea(p, this.type, hotelId);
				this.instance.getMethodes().deleteSigns(this.type, hotelId);
				this.instance.getMethodes().clearPlayersFromRegion(this.type, hotelId, p.getWorld());

				this.instance.getAreaFileManager().unsetDoorClosed(this.type, hotelId);
				this.instance.getDoorFileManager().clearDoors(this.type, hotelId);

				p.sendMessage(this.instance.getMessage("hotelDeleted").replace("%hotelId%", String.valueOf(hotelId)));
				return true;
			} else if (args[0].equalsIgnoreCase("permissions")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "permissions")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "permissions")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				p.sendMessage(this.instance.getMessage("permissionListHeader"));
				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.hotel").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.hotel." + permsPath);

					p.sendMessage(this.instance.getMessage("permissionListBody").replace("%permission%", String.valueOf(cfgPerm)));
				}
				return true;
				
			} else if (args[0].equalsIgnoreCase("info")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "info")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}
				
				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "info")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN hotel AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				this.sendHotelInfo(p, hotelId);
				return true;

			} else if (args[0].equalsIgnoreCase("listcat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "listcat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}
				
				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "listcat")) {
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
					String time = catHandler.getTime();
					
					p.sendMessage(this.instance.getMessage("hotelCategoryList")
							.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
							.replaceAll("(?i)%" + "price" + "%", String.valueOf(price))
							.replaceAll("(?i)%" + "time" + "%", time)
							);
				}
				
				return true;

			} else if (args[0].equalsIgnoreCase("list")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "list")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "list")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				this.instance.getMethodes().sendList(p, this.type, 1);
				return true;
				
			} else if (args[0].equalsIgnoreCase("users")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "users")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "users")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, instance.manageFile().getString("UserPermissions.hotel.Admin")) && !sender.hasPermission(this.instance.manageFile().getString("Permissions.hotel"))) {
					p.sendMessage(instance.getMessage("notHotelOwner"));
					return true;
				}

				// =========
				AsyncSQL sql = this.instance.getAsyncSQL();
				sql.prepareStatement("SELECT * FROM " + sql.t_perms + " WHERE type='" + this.type + "' AND id='" + hotelId + "' AND value='" + 1 + "';", new Consumer<ResultSet>() {

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
								String ingameName = target != null && target.getName() != null ? target.getName() : PlayerManager.getName(uuids.toString());

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
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "updateBackup")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "updatebackup")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}
				BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, hotelId);
				BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, hotelId);
				
				this.instance.getBackupManager().deleteSchem(this.type, hotelId);
				this.instance.getBackupManager().save(this.type, hotelId, min, max, p.getWorld());
				p.sendMessage(this.instance.getMessage("hotelBackupUpdated"));
				return true;
				
			}else if (args[0].equalsIgnoreCase("help")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "help")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "help")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}
				
				ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
				BookMeta meta = (BookMeta) item.getItemMeta();
				
				for(String content : this.instance.manageFile().getStringList("Messages.hotelHelpBook")) {
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
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "setarea")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "setarea")) {
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
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}

				AsyncSQL sql = this.instance.getAsyncSQL();
				                                //NEEDS TO ADD ONE "I" FOR SQLLITE
				sql.prepareStatement("SELECT " + (sql.isSqlLite() ? "I" : "") + "IF( EXISTS(SELECT * FROM " + sql.t_hotels + " WHERE ID='1'), (SELECT t1.id+1 as lowestId FROM " + sql.t_hotels + " AS t1 LEFT JOIN " + sql.t_hotels + " AS t2 ON t1.id+1 = t2.id WHERE t2.id IS NULL LIMIT 1), 1) as lowestId LIMIT 1;", new Consumer<ResultSet>() {

					@Override
					public void accept(ResultSet otherRS) {
						try {
							int hotelId = 1;
							
							while (otherRS.next()) {
								hotelId = otherRS.getInt("lowestId");
								break;
							}

							boolean success = instance.getMethodes().createArea(p, type, hotelId);
							instance.getMethodes().createType(type, hotelId, catID);
							if (success) {
								// CREATED
								sender.sendMessage(instance.getMessage("hotelAreaCreated").replace("%hotelId%", String.valueOf(hotelId)));
							} else {
								// NOTE CREATED
								sender.sendMessage(instance.getMessage("hotelAreaError"));
							}
							return;
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
				return true;

			} else if (args[0].equalsIgnoreCase("info")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "info")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "info")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int hotelId = Integer.parseInt(args[1]);

				this.sendHotelInfo(p, hotelId);
				return true;
				
			} else if (args[0].equalsIgnoreCase("deletecat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "deletecat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "deletecat")) {
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
				
				if(this.instance.rentTypeHandlers.containsKey(this.type)) {
					for(RentTypeHandler handler : this.instance.rentTypeHandlers.get(this.type).values()) {
						if(handler.getCatID() == catId) {
							p.sendMessage(this.instance.getMessage("hotelCouldntDeleteCategory")
									.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId)));
							return true;
						}
					}
				}
				
				this.instance.catHandlers.get(this.type).remove(catId);
				this.instance.getCategorySQL().delete(this.type, catId);
				
				p.sendMessage(this.instance.getMessage("hotelCategoryDeleted")
						.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId)));
				
				return true;
				
			} else if (args[0].equalsIgnoreCase("list")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "list")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "list")) {
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
				
			}else if (args[0].equalsIgnoreCase("buy")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "buy")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "buy")) {
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
				for(String configGroups : this.instance.manageFile().getConfigurationSection("Options.maxPossible.hotel").getKeys(false)) {
					if(configGroups.equalsIgnoreCase(group)) {
						maxPossible = this.instance.manageFile().getInt("Options.maxPossible.hotel." + configGroups);
						break;
					}else if(configGroups.equalsIgnoreCase("default")) {
						maxPossible = this.instance.manageFile().getInt("Options.maxPossible.hotel." + configGroups);
					}
				}
				
				if(maxPossible >= 0 && maxPossible <= playerHandler.getOwningList(RentTypes.HOTEL).size()) {
					p.sendMessage(this.instance.getMessage("hotelLimitReached"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int hotelId = Integer.parseInt(args[1]);

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}

				UUID ownerUUID = rentHandler.getOwnerUUID();
				if (ownerUUID != null) {
					p.sendMessage(instance.getMessage("hotelAlreadyBought"));
					return true;
				}

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

				if (catHandler == null) {
					p.sendMessage(instance.getMessage("categoryError"));
					return true;
				}
				
				int catId = rentHandler.getCatID();
				if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catId + ".usePermission") && this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catId + ".usePermission")) {
					if(!p.hasPermission(this.instance.manageFile().getString("Permissions.category") + "." + rentHandler.getType().toString().toLowerCase() + "." + catId)) {
						p.sendMessage(this.instance.getMessage("noPermsForCategory"));
						return true;
					}
				}

				String ownerName = rentHandler.getOwnerName();

				double costs = catHandler.getPrice();
				String time = catHandler.getTime();

				if (!instance.getEconomy().has(p, costs)) {
					p.sendMessage(instance.getMessage("notEnoughtMoney").replace("%amount%", String.valueOf(costs - instance.getEconomy().getBalance(p))));
					return true;
				}

				this.instance.getEconomy().withdrawPlayer(p, costs);

				this.instance.getAreaFileManager().setOwner(this.type, hotelId, ownerUUID);
				
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catHandler.getCatID() + ".autoPaymentDefault") ? this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catHandler.getCatID() + ".autoPaymentDefault") : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
				this.instance.getHotelsSQL().setOwner(hotelId, uuid, p.getName(), autoPaymentDefault);
				
				rentHandler.setOwner(uuid, p.getName());

				this.instance.getMethodes().updateSign(this.type, hotelId, ownerName, time, costs, -1);

				Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
				this.instance.getHotelsSQL().setNextPayment(hotelId, ts);
				rentHandler.setNextPayment(ts);

				p.teleport(instance.getAreaFileManager().getAreaSpawn(this.type, hotelId));
				p.sendMessage(instance.getMessage("hotelBought").replace("%hotelId%", String.valueOf(hotelId)));
				return true;

			} else if (args[0].equalsIgnoreCase("setTime")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "settime")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "settime")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				String timeS = args[1];

				if (!this.instance.getMethodes().isTimeFormat(timeS)) {
					p.sendMessage(this.instance.getMessage("notATime"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN hotel AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}

				instance.getMethodes().setTime(p, this.type, rentHandler.getCatID(), timeS);
				instance.getMethodes().updateAllSigns(this.type, hotelId);
				return true;

			} else if (args[0].equalsIgnoreCase("setPrice")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "setprice")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "setprice")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN hotel AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}

				int catID = rentHandler.getCatID();
				instance.getMethodes().setPrice(p, this.type, catID, args[1]);
				return true;

			} else if (args[0].equalsIgnoreCase("door")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "door")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (args[1].equalsIgnoreCase("close")) {

					if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "door.close")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}
					
					int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

					if (hotelId < 0) {
						// PLAYER NOT IN hotel AREA, CANT FIND ID
						p.sendMessage(this.instance.getMessage("notInHotel"));
						return true;
					}

					RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

					if (rentHandler == null) {
						p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
						return true;
					}
					
					if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".disableDoorCommand") && this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".disableDoorCommand")) {
						sender.sendMessage(this.instance.getMessage("doorCommandDisabled"));
						return true;
					}
					
					if (!this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, this.instance.manageFile().getString("UserPermissions.hotel.Door")) 
							&& !this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, this.instance.manageFile().getString("UserPermissions.hotel.Admin"))) {
						p.sendMessage(this.instance.getMessage("notHotelOwner"));
						return true;
					}

					if (this.instance.getAreaFileManager().isDoorClosed(this.type, hotelId)) {
						sender.sendMessage(this.instance.getMessage("doorsAlreadylocked"));
						return true;
					}

					this.instance.getAreaFileManager().setDoorClosed(this.type, hotelId, true);
					this.instance.getDoorFileManager().closeDoors(this.type, hotelId);

					sender.sendMessage(this.instance.getMessage("hotelDoorClosed"));
					return true;

				} else if (args[1].equalsIgnoreCase("open")) {

					if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "door.open")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}
					
					int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

					if (hotelId < 0) {
						// PLAYER NOT IN hotel AREA, CANT FIND ID
						p.sendMessage(this.instance.getMessage("notInHotel"));
						return true;
					}

					RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

					if (rentHandler == null) {
						p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
						return true;
					}
					
					if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".disableDoorCommand") && this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".disableDoorCommand")) {
						sender.sendMessage(this.instance.getMessage("doorCommandDisabled"));
						return true;
					}

					if (!this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, this.instance.manageFile().getString("UserPermissions.hotel.Door")) 
							&& !this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, this.instance.manageFile().getString("UserPermissions.hotel.Admin"))) {
						p.sendMessage(this.instance.getMessage("notHotelOwner"));
						return true;
					}

					if (!this.instance.getAreaFileManager().isDoorClosed(this.type, hotelId)) {
						sender.sendMessage(this.instance.getMessage("doorsAlreadyUnlocked"));
						return true;
					}

					this.instance.getAreaFileManager().setDoorClosed(this.type, hotelId, false);
					// this.instance.getDoorFileManager().openDoors("shop", shopId); NOT NEEDED, SINCE PLAYERS CAN OPEN IT

					sender.sendMessage(this.instance.getMessage("hotelDoorOpened"));
					return true;

				} else if (args[1].equalsIgnoreCase("remove")) {

					if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "door.remove")) {
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
					p.sendMessage(this.instance.getMessage(status ? "hotelDoorRemoved" : "hotelDoorNotFound"));
					return true;

				} else {
					p.sendMessage(this.instance.getMessage("wrongDoorArgument"));
					return true;
				}

			}
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("door")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "door")) {
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

					if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "door.add")) {
						p.sendMessage(this.instance.getMessage("perm"));
						return true;
					}
					
					if (this.instance.getDoorFileManager().isProtectedDoor(b.getLocation())) {
						p.sendMessage(this.instance.getMessage("hotelDoorAlreadyAdded"));
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
					
					p.sendMessage(this.instance.getMessage("hotelDoorAdded").replace("%hotelId%", String.valueOf(shopId)));
					return true;

				} else {
					p.sendMessage(this.instance.getMessage("wrongDoorAdminArgument"));
					return true;
				}
			}
		} else if (args.length == 4) {
			if (args[0].equalsIgnoreCase("setPermission")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "setpermission")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "setpermission")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInShop"));
					return true;
				}

				if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
					p.sendMessage(this.instance.getMessage("notABoolean"));
					return true;
				}

				String target = args[1];
				String permission = args[2];
				boolean value = args[3].equalsIgnoreCase("true") ? true : false;

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, instance.manageFile().getString("UserPermissions.hotel.Admin"))) {
					p.sendMessage(instance.getMessage("notHotelOwner"));
					return true;
				}

				for (String permsPath : instance.manageFile().getConfigurationSection("UserPermissions.hotel").getKeys(false)) {
					String cfgPerm = instance.manageFile().getString("UserPermissions.hotel." + permsPath);
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
								permsHandler.setPermission(hotelId, permission, value);
							}
						}
						
						this.instance.getPermissionsSQL().setPermission(uuidTarget, this.type, hotelId, permission, value);

						if (this.instance.manageFile().getString("UserPermissions.hotel.Admin").equalsIgnoreCase(permission) || instance.manageFile().getString("UserPermissions.hotel.Build").equalsIgnoreCase(permission)) {

							if (value) {
								this.instance.getAreaFileManager().addMember(this.type, hotelId, uuidTarget);
								this.instance.getMethodes().addMemberToRegion(this.type, hotelId, this.instance.getAreaFileManager().getWorldFromArea(this.type, hotelId), uuidTarget);

							}else if (!this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, instance.manageFile().getString("UserPermissions.hotel.Admin"))
									&& !this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, instance.manageFile().getString("UserPermissions.hotel.Build"))) {

								this.instance.getAreaFileManager().removeMember(this.type, hotelId, uuidTarget);
								this.instance.getMethodes().removeMemberToRegion(this.type, hotelId, this.instance.getAreaFileManager().getWorldFromArea(this.type, hotelId), uuidTarget);
							}
						}

						p.sendMessage(this.instance.getMessage("permissionSet").replace("%permission%", String.valueOf(permission)).replace("%player%", String.valueOf(target)).replace("%status%", String.valueOf(value)));
						return true;
					}
				}
				// PERM DOESNT EXISTS
				p.sendMessage(this.instance.getMessage("notAPermission"));

				return true;

			} else if (args[0].equalsIgnoreCase("createCat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "createcat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "createcat")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int catID = 0;
				int price = 0;
				String timeS = args[3];

				// SHOP PRICE
				try {
					catID = Integer.parseInt(args[1]);
					price = Integer.parseInt(args[2]);
				} catch (NumberFormatException ex) {
					p.sendMessage(this.instance.getMessage("notANumber"));
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
				}else {
					catHandler.setPrice(price);
					catHandler.setTime(timeS);
				}

				this.instance.getCategorySQL().updateHotelCategory(catID, price, timeS);
				this.instance.getMethodes().updateAllSigns(this.type, catID);
				p.sendMessage(this.instance.getMessage("hotelCategoryUpdated").replace("%catId%", String.valueOf(catID)).replace("%price%", String.valueOf(price)).replace("%time%", timeS));
				return true;
			}
		}
		if (p.hasPermission(this.instance.manageFile().getString("Permissions.hotel")))
			this.sendHelp(p, "hotelAdminHelp");
		else
			this.sendHelp(p, "hotelUserHelp");

		return true;
	}

	private void sendHotelInfo(Player p, int hotelId) {

		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

		if (rentHandler == null) {
			p.sendMessage(instance.getMessage("hotelDatabaseEntryMissing"));
			return;
		}

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

		if (catHandler == null) {
			p.sendMessage(instance.getMessage("categoryError"));
			return;
		}
		
		double costs = catHandler.getPrice();
		String time = catHandler.getTime();
		boolean doorsClosed = instance.getAreaFileManager().isDoorClosed(this.type, hotelId);
		Location loc = this.instance.getAreaFileManager().getAreaSpawn(this.type, hotelId);

		String owner = rentHandler.getOwnerName();

		for (String s : instance.manageFile().getStringList("Messages.hotelInfo")) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.instance.translateHexColorCodes(s))
					.replaceAll("(?i)%" + "hotelid" + "%", String.valueOf(hotelId))
					.replaceAll("(?i)%" + "catid" + "%", String.valueOf(rentHandler.getCatID()))
					.replaceAll("(?i)%" + "owner" + "%", owner == null ? "" : owner)
					.replaceAll("(?i)%" + "price" + "%", String.valueOf(costs))
					.replaceAll("(?i)%" + "time" + "%", time)
					.replaceAll("(?i)%" + "doorstatus" + "%", String.valueOf(doorsClosed))
					.replaceAll("(?i)%" + "x" + "%", String.valueOf(loc.getBlockX()))
					.replaceAll("(?i)%" + "y" + "%", String.valueOf(loc.getBlockY()))
					.replaceAll("(?i)%" + "z" + "%", String.valueOf(loc.getBlockZ()))
					.replaceAll("(?i)%" + "world" + "%", String.valueOf(loc.getWorld().getName()))
			);
		}
		return;

	}

	private void sendHelp(Player p, String path) {
		for (String s : this.instance.manageFile().getStringList("Messages." + path)) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.instance.translateHexColorCodes(s)));
		}
	}
	
	private void resetArea(Player p, RentTypeHandler rentHandler) {

		int hotelId = rentHandler.getID();
		
		BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, hotelId);
		BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, hotelId);
		this.instance.getBackupManager().paste(this.type, hotelId, min, max, p.getWorld(), false);
		this.instance.getAreaFileManager().clearMember(this.type, hotelId);
		
		rentHandler.reset(this.instance);
		
    	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".autoPaymentDefault") ? this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".autoPaymentDefault") : true;
    	rentHandler.setAutoPayment(autoPaymentDefault);
		this.instance.getHotelsSQL().reset(hotelId, autoPaymentDefault); // Resets the Owner and Payment
		this.instance.getAreaFileManager().setOwner(this.type, hotelId, null);
		
		this.instance.getPermissionsSQL().reset(this.type, hotelId);
		this.instance.getMethodes().clearPlayersFromRegion(this.type, hotelId, p.getWorld());

		this.instance.getDoorFileManager().closeDoors(this.type, hotelId);
		this.instance.getAreaFileManager().unsetDoorClosed(this.type, hotelId);
		this.instance.getMethodes().updateSign(this.type, hotelId);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		List<String> list = new ArrayList<>();
		
		if(!(sender instanceof Player))
			return list;
		
		Player p = (Player) sender;
		
		if(args.length == 1) {
			for(String subCMD : this.adminSubCommands) {
				if(this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", subCMD)) {
					if(subCMD.toLowerCase().startsWith(args[0].toLowerCase())) {
						list.add(subCMD);
					}
				}
			}
		
			for(String subCMD : this.subCommands) {
				if(this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", subCMD)) {
					if(subCMD.toLowerCase().startsWith(args[0].toLowerCase())) {
						list.add(subCMD);
					}
				}
			}
	
		}else if(args.length == 2 && args[0].equalsIgnoreCase("door")) {
			if("add".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "door.add")) {
				list.add("add");
			}else if("remove".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "door.remove")) {
				list.add("remove");
			}else if("open".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "door.open")) {
				list.add("open");
			}else if("close".startsWith(args[1]) && this.instance.getMethodes().hasPermissionForCommand(p, false, "hotel", "door.close")) {
				list.add("close");
			}
		}
		
		return list;
	}
}
