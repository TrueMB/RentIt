package me.truemb.rentit.utils;

import java.util.UUID;

import org.bukkit.World;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

public class WorldGuardUtils {
	
	public void deleteRegion(World world, String regionName) {

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));

		ProtectedRegion pr = regions.getRegion(regionName);
		if (pr != null) {
			regions.removeRegion(regionName, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
		}
	}
	
	public void createRegion(World world, String regionName, BlockVector3 min, BlockVector3 max) {

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));

		ProtectedCuboidRegion pr = new ProtectedCuboidRegion(regionName, min, max);
		regions.addRegion(pr);
		
		StateFlag buildFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("build");
		pr.setFlag(buildFlag, StateFlag.State.ALLOW);
		
		StateFlag breakFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("block-break");
		pr.setFlag(breakFlag, StateFlag.State.ALLOW);
		
		StateFlag placeFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("block-place");
		pr.setFlag(placeFlag, StateFlag.State.ALLOW);
		
		StateFlag useFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("use");
		pr.setFlag(useFlag, StateFlag.State.ALLOW);
		
        try {
    		regions.save();
    	} catch (StorageException e) {
    		e.printStackTrace();
    	}
	}
	
	public boolean regionExists(World world, String regionName) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));
		return regions.hasRegion(regionName);
	}
	
	public void clearMembers(World world, String regionName) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));
		if(regions.hasRegion(regionName)) {
			ProtectedRegion pr = regions.getRegion(regionName);
			
			pr.getOwners().clear();
			pr.getMembers().clear();
			
		    try {
		    	regions.save();
		    } catch (StorageException e) {
		    	e.printStackTrace();
		    }
		}
	}
	
	public void addMember(World world, String regionName, UUID uuid) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));
		if(regions.hasRegion(regionName)) {
			ProtectedRegion pr = regions.getRegion(regionName);
			
			if(!pr.getMembers().contains(uuid) && !pr.getOwners().contains(uuid)) {
				pr.getMembers().addPlayer(uuid);
				
		        try {
		    		regions.save();
		    	} catch (StorageException e) {
		    		e.printStackTrace();
		    	}
			}
		}
	}
	
	public void removeMember(World world, String regionName, UUID uuid) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));
		if(regions.hasRegion(regionName)) {
			ProtectedRegion pr = regions.getRegion(regionName);

			if(pr.getMembers().contains(uuid)) {
				pr.getMembers().removePlayer(uuid);
				
		        try {
		    		regions.save();
		    	} catch (StorageException e) {
		    		e.printStackTrace();
		    	}
			}
		}
	}
	
	public void setOwnerFromRegion(World world, String regionName, UUID uuid) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));
		if(regions.hasRegion(regionName)) {
			ProtectedRegion pr = regions.getRegion(regionName);
			
			pr.getOwners().clear();
			pr.getOwners().addPlayer(uuid);
				
		    try {
		    	regions.save();
		    } catch (StorageException e) {
		    	e.printStackTrace();
		    }
			
		}
	}
	
	public boolean isMember(World world, String regionName, UUID uuid) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(world));
		if(regions.hasRegion(regionName)) {
			ProtectedRegion pr = regions.getRegion(regionName);
			
			if(pr.getMembers().contains(uuid) || pr.getOwners().contains(uuid)) {
				return true;
			}
		}
		return false;
	}

}
