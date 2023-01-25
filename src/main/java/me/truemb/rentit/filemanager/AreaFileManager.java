package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BlockVector;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;

public class AreaFileManager {
	
	private Main instance;
	
	private File file;
	private YamlConfiguration config;
	
	public AreaFileManager(Main plugin) {
		this.instance = plugin;
		
		this.file = new File(this.instance.getDataFolder(), "Areas.yml");
		this.config = YamlConfiguration.loadConfiguration(this.file);
	}
		
	public boolean isInArea(RentTypes type, Location loc) {
		return this.getIdFromArea(type, loc) > 0;
	}
	
	public void setArea(RentTypes type, int id, Location loc, BlockVector3 min, BlockVector3 max) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		cfg.set(path + ".World", loc.getWorld().getName());
		
		cfg.set(path + ".Min.X", min.getBlockX());
		cfg.set(path + ".Min.Y", min.getBlockY());
		cfg.set(path + ".Min.Z", min.getBlockZ());
		
		cfg.set(path + ".Max.X", max.getBlockX());
		cfg.set(path + ".Max.Y", max.getBlockY());
		cfg.set(path + ".Max.Z", max.getBlockZ());
		
		cfg.set(path + ".Spawn.X", loc.getX());
		cfg.set(path + ".Spawn.Y", loc.getY());
		cfg.set(path + ".Spawn.Z", loc.getZ());
		cfg.set(path + ".Spawn.Yaw", loc.getYaw());
		cfg.set(path + ".Spawn.Pitch", loc.getPitch());

		this.instance.getAdvancedChestsUtils().getChestsInArea(min, max, loc.getWorld())
				.forEach((chest, locations) -> cfg.set(path + ".AdvancedChests." + chest, locations));

		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void setOwner(RentTypes type, int id, UUID ownerUUID) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		cfg.set(path + ".OwnerUUID", ownerUUID == null ? null : ownerUUID.toString());
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public UUID getOwner(RentTypes type, int id) {
		String path = type.toString().toUpperCase() + "." + id;
		
		return this.config.getString(path + ".OwnerUUID") != null ? UUID.fromString(this.config.getString(path + ".OwnerUUID")) : null;
	}
	
	@Deprecated //The loaded rentType Handler should be used
	public boolean isOwner(RentTypes type, int id, UUID ownerUUID) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		return cfg.getString(path + ".OwnerUUID") != null ? cfg.getString(path + ".OwnerUUID").equalsIgnoreCase(ownerUUID.toString()) : false;
	}
	
	public World getWorldFromArea(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		return cfg.getString(path + ".World") != null ? Bukkit.getWorld(cfg.getString(path + ".World")) : null;
	}
	
	public void deleteArea(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		cfg.set(path, null);
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public List<String> getMembers(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		List<String> memberList = new ArrayList<>();
		if(cfg.isSet(path + ".MemberUUIDs"))
			memberList = cfg.getStringList(path + ".MemberUUIDs");
		
		return memberList;
	}
	
	public void addMember(RentTypes type, int id, UUID memberUUID) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		List<String> memberList = new ArrayList<>();
		if(cfg.isSet(path + ".MemberUUIDs"))
			memberList = cfg.getStringList(path + ".MemberUUIDs");
		
		if(memberList.contains(memberUUID.toString()))
			return;
		
		memberList.add(memberUUID.toString());
		
		cfg.set(path + ".MemberUUIDs", memberList);
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	public void clearMember(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		cfg.set(path + ".MemberUUIDs", new ArrayList<>());
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void removeMember(RentTypes type, int id, UUID memberUUID) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		List<String> memberList = new ArrayList<>();
		if(cfg.isSet(path + ".MemberUUIDs"))
			memberList = cfg.getStringList(path + ".MemberUUIDs");
		
		memberList.remove(memberUUID.toString());
		
		cfg.set(path + ".MemberUUIDs", memberList);
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public boolean isMember(RentTypes type, int id, UUID memberUUID) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		List<String> memberList = new ArrayList<>();
		if(cfg.isSet(path + ".MemberUUIDs"))
			memberList = cfg.getStringList(path + ".MemberUUIDs");
		
		return memberList.contains(memberUUID.toString());
	}
	
	public Location getAreaSpawn(RentTypes type, int id) {
		String path = type.toString().toUpperCase() + "." + id;
		
		return new Location(Bukkit.getWorld(this.config.getString(path + ".World")), this.config.getDouble(path + ".Spawn.X"), this.config.getDouble(path + ".Spawn.Y"), this.config.getDouble(path + ".Spawn.Z"), (float) this.config.getDouble(path + ".Spawn.Yaw"), (float) this.config.getDouble(path + ".Spawn.Pitch"));
	}
	
	
	public BlockVector3 getMinBlockpoint(RentTypes type, int id) {
		String path = type.toString().toUpperCase() + "." + id;
		
		return BlockVector3.at(this.config.getInt(path + ".Min.X"), this.config.getInt(path + ".Min.Y"), this.config.getInt(path + ".Min.Z"));
	}
	
	public BlockVector3 getMaxBlockpoint(RentTypes type, int id) {
		String path = type.toString().toUpperCase() + "." + id;
		
		return BlockVector3.at(this.config.getInt(path + ".Max.X"), this.config.getInt(path + ".Max.Y"), this.config.getInt(path + ".Max.Z"));
	}
	
	public int getIdFromArea(RentTypes type, Location loc) {
		BlockVector pVec = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		
		if(this.config.getConfigurationSection(type.toString().toUpperCase()) != null) {
			for(String idS : this.config.getConfigurationSection(type.toString().toUpperCase()).getKeys(false)) {
				
				String path = type.toString().toUpperCase() + "." + idS;
				String worldname = this.config.getString(path + ".World");
				
				if(worldname == null) {
					this.instance.getLogger().warning("Something went wrong, while reading the worldname for " + type.toString() + " ID: " + idS);
					continue;
				}
				
				if(!worldname.equalsIgnoreCase(loc.getWorld().getName())) 
					continue;
				
				BlockVector min = new BlockVector(this.config.getInt(path + ".Min.X"), this.config.getInt(path + ".Min.Y"), this.config.getInt(path + ".Min.Z"));
				BlockVector max = new BlockVector(this.config.getInt(path + ".Max.X"), this.config.getInt(path + ".Max.Y"), this.config.getInt(path + ".Max.Z"));
				
				if(pVec.isInAABB(min, max))
					return Integer.parseInt(idS);
			}
		}
		return -1;
	}

	public void unsetDoorClosed(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		cfg.set(path + ".Doors", null);
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void setDoorClosed(RentTypes type, int id, boolean closed) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		cfg.set(path + ".Doors", closed);
		
		try {
			cfg.save(this.file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public boolean isDoorStatusSet(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		return cfg.isSet(path + ".Doors");
	}
	
	public boolean isDoorClosed(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id;
		
		return cfg.getBoolean(path + ".Doors");
	}

	public Map<String, List<String>> getAdvancedChests(RentTypes type, int id) {
		YamlConfiguration cfg = this.config;
		String path = type.toString().toUpperCase() + "." + id + ".AdvancedChests";

		Set<String> keys = cfg.getConfigurationSection(path) != null
				? cfg.getConfigurationSection(path).getKeys(false)
				: Collections.emptySet();

		return keys.stream()
				.collect(Collectors.toMap(
						e -> e,
						e -> cfg.getStringList(path + "." + e))
				);
	}
}
