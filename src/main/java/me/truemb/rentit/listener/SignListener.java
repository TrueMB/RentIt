package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.AdminGUI;
import me.truemb.rentit.gui.UserConfirmGUI;
import me.truemb.rentit.gui.UserRentGUI;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class SignListener implements Listener{
	
	private Main instance;
	
	public SignListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
	}

	@EventHandler
	public void onSignPlace(SignChangeEvent e) {
		Block b = e.getBlock();
		Player p = e.getPlayer();
		
		if(e.getLine(0).equalsIgnoreCase("[shop]")) {
		
			if(!p.hasPermission(this.instance.manageFile().getString("Permissions.sign")))
				return;
			
			int shopId = 0;
			try {
				shopId = Integer.parseInt(e.getLine(1));
			}catch(NumberFormatException ex) {
				b.breakNaturally();
				p.sendMessage(this.instance.getMessage("notANumber"));
				return;
			}
			
			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

			if (rentHandler == null) {
				b.breakNaturally();
				p.sendMessage(this.instance.getMessage("shopDatabaseEntryMissing"));
				return;
			}

			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

			if (catHandler == null) {
				b.breakNaturally();
				p.sendMessage(this.instance.getMessage("categoryError"));
				return;
			}
			
			this.instance.getSignFileManager().addSign(b.getLocation(), RentTypes.SHOP, shopId);
			
			String time = catHandler.getTime();
			double price = catHandler.getPrice();
			int size = catHandler.getSize();

			this.instance.getMethodes().updateSign(RentTypes.SHOP, shopId, (Sign) e.getBlock().getState(), rentHandler.getOwnerName(), time, price, size);

		    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
		    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
		    
			p.sendMessage(this.instance.getMessage("shopSignCreated")
					.replaceAll("(?i)%" + "shopid" + "%", String.valueOf(shopId))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
			
		}else if(e.getLine(0).equalsIgnoreCase("[hotel]")) {
		
			if(!p.hasPermission(this.instance.manageFile().getString("Permissions.sign")))
				return;
			
			int hotelId = 0;
			try {
				hotelId = Integer.parseInt(e.getLine(1));
			}catch(NumberFormatException ex) {
				b.breakNaturally();
				p.sendMessage(this.instance.getMessage("notANumber"));
				return;
			}

			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.HOTEL, hotelId);

			if (rentHandler == null) {
				b.breakNaturally();
				p.sendMessage(this.instance.getMessage("hotelDatabaseEntryMissing"));
				return;
			}
			
			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.HOTEL, rentHandler.getCatID());

			if (catHandler == null) {
				b.breakNaturally();
				p.sendMessage(this.instance.getMessage("categoryError"));
				return;
			}
			
			this.instance.getSignFileManager().addSign(b.getLocation(), RentTypes.HOTEL, hotelId);
			
			
			String time = catHandler.getTime();
			double price = catHandler.getPrice();
			
			this.instance.getMethodes().updateSign(RentTypes.HOTEL, hotelId, (Sign) e.getBlock().getState(), rentHandler.getOwnerName(), time, price, catHandler.getSize());

		    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
		    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());
		    
			p.sendMessage(this.instance.getMessage("hotelSignCreated")
					.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
			
		}
	}

	@EventHandler
	public void onSignBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		Player p = e.getPlayer();
		
		if(b.getState() instanceof Sign) {
			this.checkIfSign(e, p, b);
		}else {
			final BlockFace[] faces = { BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
			for (BlockFace face : faces) {
				Block signBlock = b.getRelative(face);
				if(signBlock.getState() instanceof Sign) {
					Sign sign = (Sign) signBlock.getState();
					if(sign.getBlockData() instanceof Directional) {
						BlockFace blockFace = ((Directional) sign.getBlockData()).getFacing();
						if(blockFace == face) {
							this.checkIfSign(e, p, signBlock);
						}
					}
				}
			}
		}
	}
	
	private void checkIfSign(BlockBreakEvent e, Player p, Block b) {
		int shopId = this.instance.getSignFileManager().getIdFromSign(b.getLocation(), RentTypes.SHOP);
		if(shopId > 0) {
			
			RentTypes type = RentTypes.SHOP;
			
			if(!p.hasPermission(this.instance.manageFile().getString("Permissions.sign"))) {
				e.setCancelled(true);
				return;
			}
			
			this.instance.getSignFileManager().removeSign(b.getLocation(), type);

			RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(type, shopId);
			CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, rentHandler.getCatID());
			
		    String alias = rentHandler != null && rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(shopId);
		    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(rentHandler.getCatID());
		    
			p.sendMessage(this.instance.getMessage("shopSignDeleted")
					.replaceAll("(?i)%" + "shopid" + "%", String.valueOf(shopId))
					.replaceAll("(?i)%" + "alias" + "%", alias)
					.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
			
		}else {
			int hotelId = this.instance.getSignFileManager().getIdFromSign(b.getLocation(), RentTypes.HOTEL);
			if(hotelId > 0) {
				
				RentTypes type = RentTypes.HOTEL;
				
				if(!p.hasPermission(this.instance.manageFile().getString("Permissions.sign"))) {
					e.setCancelled(true);
					return;
				}
				
				this.instance.getSignFileManager().removeSign(b.getLocation(), type);
				
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(type, hotelId);
				CategoryHandler catHandler = this.instance.getMethodes().getCategory(type, rentHandler.getCatID());
				
			    String alias = rentHandler != null && rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(hotelId);
			    String catAlias = catHandler != null && catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(rentHandler.getCatID());
			    
				p.sendMessage(this.instance.getMessage("hotelDeleted")
						.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
						.replaceAll("(?i)%" + "alias" + "%", alias)
						.replaceAll("(?i)%" + "catAlias" + "%", catAlias));
				
			}
		}
	}

	@EventHandler
	public void onSignClick(PlayerInteractEvent e) {
		
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		Block b = e.getClickedBlock();
		Player p = e.getPlayer();
		
		if(!(b.getState() instanceof Sign))
			return;
		
		int shopId = this.instance.getSignFileManager().getIdFromSign(b.getLocation(), RentTypes.SHOP);
		if(shopId > 0) {
		
			if(p.isSneaking() && p.hasPermission(this.instance.manageFile().getString("Permissions.shop"))) {
				//ADMIN GUI
				p.openInventory(AdminGUI.getAdminShopGui(this.instance, shopId));
				return;
			}else {
				//PLAYER RENT SHOP
			    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

				if (rentHandler == null)
					return;

				UUID uuid = rentHandler.getOwnerUUID();
				if(uuid == null) {
					
					int catId = rentHandler.getCatID();
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + catId + "." + CategorySettings.usePermission.toString()) 
							&& this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + catId + "." + CategorySettings.usePermission.toString())) {
						if(!p.hasPermission(this.instance.manageFile().getString("Permissions.category") + "." + rentHandler.getType().toString().toLowerCase() + "." + catId)) {
							p.sendMessage(this.instance.getMessage("noPermsForCategory"));
							return;
						}
					}
					
					p.openInventory(UserConfirmGUI.getShopConfirmationGUI(this.instance, shopId));
				}else if(uuid.equals(p.getUniqueId())) {
					p.openInventory(UserRentGUI.getRentSettings(this.instance, RentTypes.SHOP, shopId, true));
				}else {
					p.sendMessage(this.instance.getMessage("shopAlreadyBought"));
					return;
				}
			}
		}else {
			int hotelId = this.instance.getSignFileManager().getIdFromSign(b.getLocation(), RentTypes.HOTEL);
			if(hotelId > 0) {
				if(p.isSneaking() && p.hasPermission(this.instance.manageFile().getString("Permissions.hotel"))) {
					//ADMIN GUI
					p.openInventory(AdminGUI.getAdminHotelGui(this.instance, hotelId));
					return;
				}else {
					//PLAYER RENT SHOP
				    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(RentTypes.HOTEL, hotelId);

					if (rentHandler == null)
						return;

					UUID uuid = rentHandler.getOwnerUUID();
					if(uuid == null) {
						
						int catId = rentHandler.getCatID();
						if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + catId + "." + CategorySettings.usePermission.toString()) 
								&& this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + catId + "." + CategorySettings.usePermission.toString())) {
							if(!p.hasPermission(this.instance.manageFile().getString("Permissions.category") + "." + rentHandler.getType().toString().toLowerCase() + "." + catId)) {
								p.sendMessage(this.instance.getMessage("noPermsForCategory"));
								return;
							}
						}
						
						p.openInventory(UserConfirmGUI.getHotelConfirmationGUI(this.instance, hotelId));
					}else if(uuid.equals(p.getUniqueId())) {
						p.openInventory(UserRentGUI.getRentSettings(this.instance, RentTypes.HOTEL, hotelId, true));
					}else {
						p.sendMessage(this.instance.getMessage("hotelAlreadyBought"));
						return;
					}
				}
			}
		}
	}
}
