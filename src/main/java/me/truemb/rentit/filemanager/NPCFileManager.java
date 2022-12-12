package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import me.truemb.rentit.main.Main;

public class NPCFileManager {
	
	private File file;

	private Main instance;
	
	
	//shopId:
	//   Location: ...
	//   npcId: null (kein Shop Owner) oder NPCID (Wenn Shop gemietet)
	
	public NPCFileManager(Main plugin) {
		this.instance = plugin;
		
		this.file = new File(this.instance.getDataFolder(), "ShopsNPC.yml");
		
		if(!this.file.exists()) {
			try {
				this.file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private YamlConfiguration getConfig() {
		return YamlConfiguration.loadConfiguration(this.file);
	}
	
	public void setNPCLocForShop(int shopId, Location loc) {
		YamlConfiguration config = this.getConfig();
		try {
			config.set(shopId + ".Location.World", loc.getWorld().getName());
			config.set(shopId + ".Location.X", loc.getX());
			config.set(shopId + ".Location.Y", loc.getY());
			config.set(shopId + ".Location.Z", loc.getZ());
			config.set(shopId + ".Location.Yaw", loc.getYaw());
			config.set(shopId + ".Location.Pitch", loc.getPitch());
			config.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setNPCinConfig(int shopId, UUID uuid) {
		YamlConfiguration config = this.getConfig();
		try {
			config.set(shopId + ".NPCId", uuid.toString());
			config.set("NPC." + uuid.toString(), shopId);
			config.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void removeNPCinConfig(int shopId) {
		YamlConfiguration config = this.getConfig();
		int npcId = config.getInt(shopId + ".NPCId");
		
		try {
			config.set("NPC." + npcId, null);
			config.set(shopId + ".NPCId", null);
			config.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public UUID getNPCIdFromShop(int shopId) {
		YamlConfiguration config = this.getConfig();
		return config.getString(shopId + ".NPCId") != null ? UUID.fromString(config.getString(shopId + ".NPCId")) : null;
	}

	public int getShopIdFromNPC(UUID uuid) {
		YamlConfiguration config = this.getConfig();
		return config.getInt("NPC." + uuid);
	}
	
	public Location getNPCLocForShop(int shopId) {
		YamlConfiguration config = this.getConfig();
		
		if(!config.isSet(shopId + ".Location")) {
			this.instance.getLogger().warning("Please set the NPC Spawn Location for the Shop: " + shopId + ". /shop setnpc");
			return null;
		}
		
		World world = Bukkit.getWorld(config.getString(shopId + ".Location.World"));
		double x = config.getDouble(shopId + ".Location.X");
		double y = config.getDouble(shopId + ".Location.Y");
		double z = config.getDouble(shopId + ".Location.Z");
		float yaw = (float) config.getDouble(shopId + ".Location.Yaw");
		float pitch = (float) config.getDouble(shopId + ".Location.Pitch");
		
		return new Location(world, x, y, z, yaw, pitch);
	}
}
