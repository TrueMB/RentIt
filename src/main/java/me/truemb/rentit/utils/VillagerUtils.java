package me.truemb.rentit.utils;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;

import me.truemb.rentit.main.Main;

public class VillagerUtils {
	
	private Main instance;
	
	private HashMap<Integer, Villager> shop_villagers = new HashMap<>();
	
	public VillagerUtils(Main plugin) {
		this.instance = plugin;
	}
	
	public void loadVillagers() {
		YamlConfiguration config = this.instance.getNPCFileManager().getConfig();
		for(String shopIdS : config.getConfigurationSection("").getKeys(false)) {
			int shopId = Integer.parseInt(shopIdS);
			
			Location loc = this.instance.getNPCFileManager().getNPCLocForShop(shopId);
			this.instance.getThreadHandler().runTaskLaterSync(loc, (t) -> {
				for(Entity entities : loc.getNearbyEntities(1, 1, 1)) {
					if(entities instanceof Villager) {
						Villager v = (Villager) entities;
						//Replace Meta since it gets removed
						v.setMetadata("shopid", new FixedMetadataValue(this.instance, String.valueOf(shopId))); // PUTTING THE SHOP AS ENTITY META
						
						this.shop_villagers.put(shopId, v);
						break;
					}
				}
			}, 30);
			
		}
	}

	public void destroyVillager(int shopId) {
		Villager vil = this.shop_villagers.get(shopId);

		if(vil != null)
			vil.remove();
	}

	public boolean isShopVillager(Villager vil) {
		return this.shop_villagers.containsValue(vil);
	}
	
	public boolean isVillagerSpawned(int shopId) {
		Villager vil = this.shop_villagers.get(shopId);
		return vil != null && !vil.isDead();
	}
	
	public void moveVillager(int shopId, Location loc) {
		Villager vil = this.shop_villagers.get(shopId);
		vil.teleport(loc);
	}

	public void createVillager(int shopId, String prefix, String playerName) {

		Location loc = this.instance.getNPCFileManager().getNPCLocForShop(shopId);
		if (loc == null)
			return;
				
		Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER); //SHOP VILLAGER
		this.shop_villagers.put(shopId, v);
			
		v.setAI(false); //CANT MOVE
		v.setAware(false); //CANT GET PUSHED
		v.setCollidable(false); // "
		v.setGravity(false); //CANT FALL
		v.setSilent(true); // NO SOUNDS
			
		String displayname = prefix + playerName;
		String customName = this.instance.translateHexColorCodes(this.instance.manageFile().getString("Options.userShopName")
				.replaceAll("(?i)%" + "displayname" + "%", displayname != null ? displayname : "")
				.replaceAll("(?i)%" + "username" + "%", playerName != null ? playerName : "")
			);
			
		v.setCustomName(customName);
		v.setCustomNameVisible(true);
	
		v.setMetadata("shopid", new FixedMetadataValue(this.instance, String.valueOf(shopId))); // PUTTING THE SHOP AS ENTITY META
	}
}
