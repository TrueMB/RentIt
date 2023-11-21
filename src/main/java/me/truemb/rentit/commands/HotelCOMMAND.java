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
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.database.connector.AsyncSQL;
import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PermissionsHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;
import me.truemb.rentit.utils.UtilitiesAPI;

public class HotelCOMMAND extends BukkitCommand {

	private Main instance;
	private List<String> subCommands = new ArrayList<>();
	private List<String> adminSubCommands = new ArrayList<>();
	
	private RentTypes type = RentTypes.HOTEL;

	public HotelCOMMAND(Main plugin) {
		super("hotel", "Hotel Main Command", null, plugin.manageFile().getStringList("Options.commands.hotel.aliases"));
		this.instance = plugin;

		this.subCommands.add("users");
		this.subCommands.add("permissions");
		this.subCommands.add("setPermission");
		this.subCommands.add("buy");
		this.subCommands.add("resign");
		this.subCommands.add("help");

		this.adminSubCommands.add("createCat");
		this.adminSubCommands.add("deleteCat");
		this.adminSubCommands.add("setAliasCat");
		this.adminSubCommands.add("listCat");
		this.adminSubCommands.add("setArea");
		this.adminSubCommands.add("setAlias");
		this.adminSubCommands.add("reset");
		this.adminSubCommands.add("delete");
		this.adminSubCommands.add("updateBackup");
		this.adminSubCommands.add("info");
		this.adminSubCommands.add("catInfo");
		this.adminSubCommands.add("door");
		this.adminSubCommands.add("rollback");
		this.adminSubCommands.add("setTime");
		this.adminSubCommands.add("setPrice");
		this.adminSubCommands.add("list");
		
		
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
	public boolean execute(CommandSender sender, String label, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getMessage("console"));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();

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
				
				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
			    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
				
				this.resetArea(p, rentHandler);
				p.sendMessage(this.instance.getMessage("hotelReseted")
						.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));
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

				CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
			    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				this.resetArea(p, rentHandler);
				p.sendMessage(this.instance.getMessage("hotelResignContract")
						.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));
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

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);
				
				this.deleteHotel(p, rentHandler);
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

					p.sendMessage(this.instance.getMessage("permissionListBody")
							.replaceAll("(?i)%" + "permission" + "%", String.valueOf(cfgPerm)));
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
					
				    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
					
					p.sendMessage(this.instance.getMessage("hotelCategoryList")
							.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
							.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
							.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(price))
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
					meta.addPage(this.instance.translateHexColorCodes(content.replace("\\n", "\n")));
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

				this.instance.getHotelsSQL().getNextHotelId((hotelId) -> {
					
					boolean success = this.instance.getMethodes().createArea(p, this.type, hotelId);
					this.instance.getMethodes().createType(this.type, hotelId, catID, false);
					
					if (success) {
						// CREATED
						RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);
						
					    String alias = rentHandler != null & rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
					    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
					    
						sender.sendMessage(this.instance.getMessage("hotelAreaCreated")
								.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
								.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
								.replaceAll("(?i)%" + "alias" + "%", alias));
					} else {
						// NOTE CREATED
						sender.sendMessage(instance.getMessage("hotelAreaError"));
					}
				});
				return true;

			}  else if (args[0].equalsIgnoreCase("delete")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "delete")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "delete")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int hotelId = Integer.parseInt(args[1]);
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);
				
				if (hotelId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("hotelIdNotValid"));
					return true;
				}
				
				this.deleteHotel(p, rentHandler);
				return true;

			} else if (args[0].equalsIgnoreCase("catInfo")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("shop", "catInfo")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "shop", "catInfo")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}
				
				int catId = Integer.parseInt(args[1]);
				
				boolean success = this.sendCategoryInfo(p, catId);
				
				if(!success) {
					p.sendMessage(this.instance.getMessage("categoryError"));
					return true;
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

				if (!args[1].matches("[0-9]+")) {
					p.sendMessage(this.instance.getMessage("notANumber"));
					return true;
				}

				int hotelId = Integer.parseInt(args[1]);

				this.sendHotelInfo(p, hotelId);
				return true;
				
			} else if (args[0].equalsIgnoreCase("setAliasCat")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "setAliasCat")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "setAliasCat")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
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
				
				p.sendMessage(this.instance.getMessage("hotelCategoryChangedAlias")
						.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catHandler.getCatID()))
						.replaceAll("(?i)%" + "catAlias" + "%", alias));
				
				return true;
				
			} else if (args[0].equalsIgnoreCase("setAlias")) {
				
				if(!this.instance.getMethodes().isSubCommandEnabled("hotel", "setAlias")) {
					sender.sendMessage(this.instance.getMessage("commandDisabled"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermissionForCommand(p, true, "hotel", "setAlias")) {
					p.sendMessage(this.instance.getMessage("perm"));
					return true;
				}

				int hotelId = this.instance.getAreaFileManager().getIdFromArea(this.type, p.getLocation());

				if (hotelId < 0) {
					// PLAYER NOT IN SHOP AREA, CANT FIND ID
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}
				
				String alias = args[1];
				alias = alias.substring(0, alias.length() > 100 ? 100 : alias.length());

				this.instance.getHotelsSQL().setAlias(hotelId, alias);
				rentHandler.setAlias(alias);
				
				p.sendMessage(this.instance.getMessage("hotelChangedAlias")
						.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
						.replaceAll("(?i)%" + "alias" + "%", alias));
				
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
				
			    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
				
				if(this.instance.rentTypeHandlers.containsKey(this.type)) {
					for(RentTypeHandler handler : this.instance.rentTypeHandlers.get(this.type).values()) {
						if(handler.getCatID() == catId) {
						    
							p.sendMessage(this.instance.getMessage("hotelCouldntDeleteCategory")
									.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
									.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
							return true;
						}
					}
				}
				
				this.instance.catHandlers.get(this.type).remove(catId);
				this.instance.getCategorySQL().delete(this.type, catId);
				
				p.sendMessage(this.instance.getMessage("hotelCategoryDeleted")
						.replaceAll("(?i)%" + "catid" + "%", String.valueOf(catId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
				
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
				if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catId + "." + CategorySettings.usePermission.toString()) 
						&& this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catId + "." + CategorySettings.usePermission.toString())) {
					if(!p.hasPermission(this.instance.manageFile().getString("Permissions.category") + "." + rentHandler.getType().toString().toLowerCase() + "." + catId)) {
						p.sendMessage(this.instance.getMessage("noPermsForCategory"));
						return true;
					}
				}

				String ownerName = rentHandler.getOwnerName();

				double costs = catHandler.getPrice();
				String time = catHandler.getTime();

				if (!this.instance.getEconomySystem().has(p, costs)) {
					p.sendMessage(this.instance.getMessage("notEnoughMoney")
							.replaceAll("(?i)%" + "amount" + "%", String.valueOf(costs - this.instance.getEconomySystem().getBalance(p))));
					return true;
				}

				this.instance.getEconomySystem().withdraw(p, costs);

				this.instance.getAreaFileManager().setOwner(this.type, hotelId, ownerUUID);
				
	        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) 
	        			? this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) : true;
	        	rentHandler.setAutoPayment(autoPaymentDefault);
				this.instance.getHotelsSQL().setOwner(hotelId, uuid, p.getName(), autoPaymentDefault);
				
				rentHandler.setOwner(uuid, p.getName());

				this.instance.getMethodes().updateSign(this.type, hotelId, ownerName, time, costs, -1);

				Timestamp ts = UtilitiesAPI.getNewTimestamp(time);
				this.instance.getHotelsSQL().setNextPayment(hotelId, ts);
				rentHandler.setNextPayment(ts);
				
			    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
			    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

				p.teleport(this.instance.getAreaFileManager().getAreaSpawn(this.type, hotelId));
				p.sendMessage(this.instance.getMessage("hotelBought")
						.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "alias" + "%", alias));
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
					p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}

				this.instance.getMethodes().setTime(p, this.type, rentHandler.getCatID(), timeS);
				this.instance.getMethodes().updateAllSigns(this.type, hotelId);
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
				this.instance.getMethodes().setPrice(p, this.type, catID, args[1]);
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
					
					if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString()) 
							&& this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString())) {
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
						p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
						return true;
					}
					
					if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString()) 
							&& this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + "." + CategorySettings.disableDoorCommand.toString())) {
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
					// this.instance.getDoorFileManager().openDoors("shop", hotelId); NOT NEEDED, SINCE PLAYERS CAN OPEN IT

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
				
				int hotelId = 0;
				try {
					hotelId = Integer.parseInt(args[2]);
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
						this.instance.getDoorFileManager().addDoor(door, b.getLocation(), this.type, hotelId);
					}else if(b.getState().getBlockData() instanceof TrapDoor || b.getState().getBlockData() instanceof Gate) {
						this.instance.getDoorFileManager().addDoor(b.getLocation(), this.type, hotelId);
					}else {
						p.sendMessage(this.instance.getMessage("notADoor"));
						return true;
					}

					RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, hotelId);
					CategoryHandler catHandler = instance.getMethodes().getCategory(type, rentHandler.getCatID());
					
				    String alias = rentHandler != null & rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
				    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
					
					p.sendMessage(this.instance.getMessage("hotelDoorAdded")
							.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
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
					p.sendMessage(this.instance.getMessage("notInHotel"));
					return true;
				}

				if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
					p.sendMessage(this.instance.getMessage("notABoolean"));
					return true;
				}

				String target = args[1];
				String permission = args[2].toLowerCase();
				boolean value = args[3].equalsIgnoreCase("true") ? true : false;

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

				if (rentHandler == null) {
					p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
					return true;
				}

				if (!this.instance.getMethodes().hasPermission(this.type, hotelId, uuid, this.instance.manageFile().getString("UserPermissions.hotel.Admin"))) {
					p.sendMessage(this.instance.getMessage("notHotelOwner"));
					return true;
				}

				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.hotel").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.hotel." + permsPath);
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
								permsHandler.setPermission(hotelId, permission, value);
							}
						}
						
						this.instance.getPermissionsSQL().setPermission(uuidTarget[0], this.type, hotelId, permission, value);

						if (this.instance.manageFile().getString("UserPermissions.hotel.Admin").equalsIgnoreCase(permission) || this.instance.manageFile().getString("UserPermissions.hotel.Build").equalsIgnoreCase(permission)) {

							if (value) {
								this.instance.getAreaFileManager().addMember(this.type, hotelId, uuidTarget[0]);
								this.instance.getMethodes().addMemberToRegion(this.type, hotelId, this.instance.getAreaFileManager().getWorldFromArea(this.type, hotelId), uuidTarget[0]);
								
							}else {
								if (playerHandler != null && playerHandler.getPermsHandler(this.type) != null) {
									String adminPerm = this.instance.manageFile().getString("UserPermissions." + this.type.toString().toLowerCase() + ".Admin");
									String buildPerm = this.instance.manageFile().getString("UserPermissions." + this.type.toString().toLowerCase() + ".Build");
									
									PermissionsHandler permsHandler = playerHandler.getPermsHandler(this.type);
									
									if(!permsHandler.hasPermission(hotelId, adminPerm) && !permsHandler.hasPermission(hotelId, buildPerm)) {
										this.instance.getAreaFileManager().removeMember(this.type, hotelId, uuidTarget[0]);
										this.instance.getMethodes().removeMemberFromRegion(this.type, hotelId, this.instance.getAreaFileManager().getWorldFromArea(this.type, hotelId), uuidTarget[0]);
									}
								}else {
									this.instance.getPermissionsSQL().hasBuildPermissions(uuidTarget[0], this.type, hotelId, new Consumer<Boolean>() {
		
										@Override
										public void accept(Boolean b) {
											if(!b) {
												instance.getAreaFileManager().removeMember(type, hotelId, uuidTarget[0]);
												instance.getMethodes().removeMemberFromRegion(type, hotelId, instance.getAreaFileManager().getWorldFromArea(type, hotelId), uuidTarget[0]);
											}
										}
									});
								}
							}
							
							p.sendMessage(this.instance.getMessage("permissionSet")
									.replaceAll("(?i)%" + "permission" + "%", String.valueOf(permission))
									.replaceAll("(?i)%" + "player" + "%", target)
									.replaceAll("(?i)%" + "status" + "%", String.valueOf(value)));
						}
						
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

			    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
			    
				this.instance.getCategorySQL().updateHotelCategory(catID, price, timeS);
				this.instance.getMethodes().updateAllSigns(this.type, catID);
				p.sendMessage(this.instance.getMessage("hotelCategoryUpdated")
						.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catID))
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
						.replaceAll("(?i)%" + "price" + "%", String.valueOf(price))
						.replaceAll("(?i)%" + "time" + "%", timeS));
				return true;
			}
		}
		if (p.hasPermission(this.instance.manageFile().getString("Permissions.hotel")))
			this.sendHelp(p, "hotelAdminHelp");
		else
			this.sendHelp(p, "hotelUserHelp");

		return true;
	}

	private void deleteHotel(Player p, RentTypeHandler rentHandler) {

		int hotelId = rentHandler.getID();
		
		CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());
		
	    String alias = rentHandler != null && rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
	    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
	    
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

		p.sendMessage(this.instance.getMessage("hotelDeleted")
				.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
				.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
				.replaceAll("(?i)%" + "alias" + "%", alias));
		
	}

	private boolean sendCategoryInfo(Player p, int catId) {

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, catId);

		if (catHandler == null)
			return false;
		
		double costs = catHandler.getPrice();
		String time = catHandler.getTime();
		
	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
	    
	    int amount = this.instance.getMethodes().getRentTypesOfCategory(this.type, catId).size();

		for (String s : this.instance.manageFile().getStringList("Messages.hotelCategoryInfo")) {
			p.sendMessage(this.instance.translateHexColorCodes(s)
					.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catHandler.getCatID()))
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
					.replaceAll("(?i)%" + "price" + "%", String.valueOf(costs))
					.replaceAll("(?i)%" + "time" + "%", time)
					.replaceAll("(?i)%" + "catHotelAmount" + "%", String.valueOf(amount))
			);
		}
		return true;
	}
	
	private void sendHotelInfo(Player p, int hotelId) {

		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, hotelId);

		if (rentHandler == null) {
			p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
			return;
		}

		CategoryHandler catHandler = this.instance.getMethodes().getCategory(this.type, rentHandler.getCatID());

		if (catHandler == null) {
			p.sendMessage(this.instance.getMessage("categoryError"));
			return;
		}
		
		double costs = catHandler.getPrice();
		String time = catHandler.getTime();
		boolean doorsClosed = this.instance.getAreaFileManager().isDoorClosed(this.type, hotelId);
		Location loc = this.instance.getAreaFileManager().getAreaSpawn(this.type, hotelId);
		
	    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
	    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

		String owner = rentHandler.getOwnerName();

		for (String s : this.instance.manageFile().getStringList("Messages.hotelInfo")) {
			p.sendMessage(this.instance.translateHexColorCodes(s)
					.replaceAll("(?i)%" + "hotelid" + "%", String.valueOf(hotelId))
					.replaceAll("(?i)%" + "catId" + "%", String.valueOf(catHandler.getCatID()))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
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
			p.sendMessage(this.instance.translateHexColorCodes(s));
		}
	}
	
	private void resetArea(Player p, RentTypeHandler rentHandler) {

		int hotelId = rentHandler.getID();
		
		BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, hotelId);
		BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, hotelId);
		this.instance.getBackupManager().paste(this.type, hotelId, min, max, p.getWorld(), false);
		this.instance.getAdvancedChestsUtils().pasteChestsInArea(this.type, hotelId);
		this.instance.getAreaFileManager().clearMember(this.type, hotelId);
		
		rentHandler.reset();
		
    	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) 
    			? this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + "." + CategorySettings.autoPaymentDefault.toString()) : true;
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
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {

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
			
		}else if(args.length > 2 && args[0].equalsIgnoreCase("setPermission")) {
			if(args.length == 2) {
				
				for(Player all : Bukkit.getOnlinePlayers())
					if(all.getName().toLowerCase().startsWith(args[1].toLowerCase()))
						list.add(all.getName());
				
			}else if(args.length == 3) {

				for (String permsPath : this.instance.manageFile().getConfigurationSection("UserPermissions.hotel").getKeys(false)) {
					String cfgPerm = this.instance.manageFile().getString("UserPermissions.hotel." + permsPath);
					
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
