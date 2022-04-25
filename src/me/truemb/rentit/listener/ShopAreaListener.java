package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.EquipmentSlot;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class ShopAreaListener implements Listener {

	private Main instance;
	private RentTypes type = RentTypes.SHOP;
	
	public ShopAreaListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
    public void onPermissionForBreak(BlockBreakEvent e) {
		
		Player p = e.getPlayer();
		Block b = e.getBlock();
		Location loc = b.getLocation();
		
		boolean canceled = this.protectedRegion(p, loc, true);
		if(canceled)
			e.setCancelled(canceled);
    }
	
	@EventHandler
    public void onPermissionForPlace(BlockPlaceEvent e) {
		
		Player p = e.getPlayer();
		Block b = e.getBlockPlaced();
		Location loc = b.getLocation();
		
		boolean canceled = this.protectedRegion(p, loc, true);
		if(canceled)
			e.setCancelled(canceled);
    }

	@EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleDMG(VehicleDamageEvent e) {
		
		Player p = null;
		if(e.getAttacker() instanceof Player)
			p = (Player) e.getAttacker();
		else if(e.getAttacker() instanceof AbstractArrow && ((AbstractArrow) e.getAttacker()).getShooter() instanceof Player)
			p = (Player) ((AbstractArrow) e.getAttacker()).getShooter();
		
		if(p == null) return;
		
		Vehicle target = e.getVehicle();
		Location loc = target.getLocation();

		boolean canceled = this.protectedRegion(p, loc, true);
		if(canceled)
			e.setCancelled(canceled);
    }

	@EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDMG(EntityDamageByEntityEvent e) {
		
		Player p = null;
		if(e.getDamager() instanceof Player)
			p = (Player) e.getDamager();
		else if(e.getDamager() instanceof AbstractArrow && ((AbstractArrow) e.getDamager()).getShooter() instanceof Player)
			p = (Player) ((AbstractArrow) e.getDamager()).getShooter();
		
		if(p == null) return;
		
		Entity target = e.getEntity();
		Location loc = target.getLocation();

		boolean canceled = this.protectedRegion(p, loc, true);
		if(canceled)
			e.setCancelled(canceled);
    }

	@EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingDMG(HangingBreakByEntityEvent e) {
		
		Player p = null;
		if(e.getRemover() instanceof Player)
			p = (Player) e.getRemover();
		else if(e.getRemover() instanceof AbstractArrow && ((AbstractArrow) e.getRemover()).getShooter() instanceof Player)
			p = (Player) ((AbstractArrow) e.getRemover()).getShooter();
		
		if(p == null) return;
		
		Entity target = e.getEntity();
		Location loc = target.getLocation();

		boolean canceled = this.protectedRegion(p, loc, true);
		if(canceled)
			e.setCancelled(canceled);
    }

	@EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteraction(PlayerInteractEntityEvent e) {
		
		if(e.getHand() != EquipmentSlot.HAND)
			return;
		
		Player p = e.getPlayer();
		Entity target = e.getRightClicked();
		Location loc = target.getLocation();
		
		if(target.hasMetadata("NPC"))
			return;
		
		boolean canceled = this.protectedRegion(p, loc, true);
		if(canceled)
			e.setCancelled(canceled);
    }
	
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onInteraction(PlayerInteractEvent e) {
		
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		if(e.getHand() != EquipmentSlot.HAND)
			return;

		Block b = e.getClickedBlock();
		if(b == null)
			return;
		
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		Location loc = b.getLocation();
		
		if(p.hasPermission(this.instance.manageFile().getString("Permissions.build")))
			return;
		
		if((b.getBlockData() instanceof Door || b.getBlockData() instanceof TrapDoor || b.getBlockData() instanceof Gate) && this.instance.getDoorFileManager().isProtectedDoor(loc) && this.instance.getDoorFileManager().getTypeFromDoor(loc).equals(this.type)){
			//DOOR LOCKED?
			
			int shopId = this.instance.getDoorFileManager().getIdFromDoor(loc);
	
		    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(this.type, shopId);
	
			if (rentHandler == null)
				return;
			
			if(!this.instance.getAreaFileManager().isDoorStatusSet(this.type, shopId)) {
				
				//SHOP DOORS CLOSED THROUGH CONFIG SETTINGS
				if(!p.hasPermission(this.instance.manageFile().getString("Permissions.bypass.doors"))
						&& this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".doorsClosedUntilBuy") 
						&& this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".doorsClosedUntilBuy")) {
					
					e.setCancelled(true);
					e.setUseInteractedBlock(Result.DENY);
					
					//SHOP ALREADY OWNED, BUT OWNERS CLOSED DOORS
					if(rentHandler.getOwnerUUID() != null)
						if(e.getHand() == EquipmentSlot.HAND)
							p.sendMessage(this.instance.getMessage("shopDoorStillClosed"));
					return;
				}else {
					e.setCancelled(false);
					e.setUseInteractedBlock(Result.ALLOW);
					return;
				}
			}
			if(this.instance.getAreaFileManager().isDoorClosed(this.type, shopId)) {
				if(p.hasPermission(this.instance.manageFile().getString("Permissions.bypass.doors")) || (
						(!this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".ownerBypassLock") 
						|| this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".ownerBypassLock")) 
						&& (this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Door")) || 
						this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin"))))) {
					
					e.setCancelled(false);
					e.setUseInteractedBlock(Result.ALLOW);
					return;
				}else {
					e.setCancelled(true);
					e.setUseInteractedBlock(Result.DENY);
					
					if(e.getHand() == EquipmentSlot.HAND)
						p.sendMessage(this.instance.getMessage("shopDoorStillClosed"));
					return;
				}
			}
		}else if(b.getType() == Material.ENDER_CHEST) {
			
			//ENDER CHEST INTERACTION
			return;
			
		}else if(b.getType() == Material.CHEST) {
			//CHEST INTERACTION
			
			int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, loc);

		    RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(this.type, shopId);

			if (rentHandler == null)
				return; //DOES SHOP EXISTS?
		    
			if(!p.hasPermission(this.instance.manageFile().getString("Permissions.bypass.chests")) 
					&& (!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Fill")) &&
					!this.instance.getMethodes().hasPermission(this.type, shopId, uuid, this.instance.manageFile().getString("UserPermissions.shop.Admin")))) {
				
				e.setCancelled(true);
				
				if(e.getHand() == EquipmentSlot.HAND)
					p.sendMessage(this.instance.getMessage("notShopOwner"));
			}
		}else {
			
			boolean canceled = this.protectedRegion(p, loc, e.getHand() == EquipmentSlot.HAND);
			if(canceled)
				e.setCancelled(canceled);
			else {
				//SPAWNING
				
				if(p.getInventory().getItemInMainHand() == null && p.getInventory().getItemInOffHand() == null || p.getInventory().getItemInMainHand().getType().isBlock() && p.getInventory().getItemInOffHand().getType().isBlock())
					return;
				
				BlockFace facing = e.getBlockFace();
				Block facingBlock = b.getRelative(facing);
				
				if(facingBlock == null)
					return;

				boolean canceledSpawn = this.protectedRegion(p, facingBlock.getLocation(), e.getHand() == EquipmentSlot.HAND);
				if(canceledSpawn)
					e.setCancelled(canceledSpawn);
			}
		}
    }
	
	private boolean protectedRegion(Player p, Location loc, boolean withMessages) {
		
		if(p.hasPermission(this.instance.manageFile().getString("Permissions.build")))
			return false;
		
		UUID uuid = p.getUniqueId();
		int shopId = this.instance.getAreaFileManager().getIdFromArea(this.type, loc);

	    RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(this.type, shopId);

		if (rentHandler == null)
			return false;

		if(!this.instance.manageFile().getBoolean("Options.defaultPermissions.shop.build")) {
			if(withMessages)
				p.sendMessage(this.instance.getMessage("featureDisabled"));
			return true;
		}

		if(this.instance.getWorldGuard() != null) {
			if(!this.instance.getMethodes().isMemberFromRegion(this.type, shopId, p.getWorld(), uuid)) {
				if(withMessages)
					p.sendMessage(this.instance.getMessage("notShopOwner"));
				return true;
			}
			return false;
		}

		if(!this.instance.getAreaFileManager().isOwner(this.type, shopId, uuid) && !this.instance.getAreaFileManager().isMember(this.type, shopId, uuid)) {
			if(withMessages)
				p.sendMessage(this.instance.getMessage("notShopOwner"));
			return true;
		}
		return false;
	}
}
