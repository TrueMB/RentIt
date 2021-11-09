package me.truemb.rentit.utils;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.FixedMetadataValue;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Spawned;

public class NPCUtils {
	
	private Main instance;
	
	public NPCUtils(Main plugin) {
		this.instance = plugin;
	}

	public void checkAllNPCStates() {
		
		HashMap<Integer, RentTypeHandler> hash = this.instance.rentTypeHandlers.get(RentTypes.SHOP);
		
		if(hash == null)
			return;
		
		//CREATE VILLAGERS 
		for(int shopId : hash.keySet()) {
			RentTypeHandler rentHandler = hash.get(shopId);
			if(!rentHandler.isOwned())
				this.despawnNPC(shopId);
		}
	}
	
	public void disableNPCs() {
		
		HashMap<Integer, RentTypeHandler> hash = this.instance.rentTypeHandlers.get(RentTypes.SHOP);
		
		if(hash == null)
			return;
		
		//CREATE VILLAGERS 
		for(int shopId : hash.keySet()) {
			this.despawnNPC(shopId);
		}
	}

	public void despawnNPC(int shopId) {
		UUID npcUUID = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);
		NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);

		if(npc == null)
			return;

        npc.getOrAddTrait(Spawned.class).setSpawned(false);
        npc.despawn(DespawnReason.PLUGIN);
		//CitizensAPI.getNPCRegistry().saveToStore();
	}
	
	public void destroyNPC(int shopId) {
		UUID npcUUID = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);
		NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);

		if(npc == null)
			return;

		npc.destroy();
		CitizensAPI.getNPCRegistry().saveToStore();
	}

	public boolean isNPCSpawned(int shopId) {

		UUID npcUUID = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);

		if (npcUUID == null)
			return false;

		NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);

		if (npc != null && npc.isSpawned())
			return true;

		return false;
	}
	
	public boolean existsNPCForShop(int shopId) {

		UUID npcUUID = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);

		if (npcUUID == null)
			return false;

		NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);

		if (npc != null)
			return true;
		return false;
	}

	public void moveNPC(int shopId, Location loc) {

		UUID npcUUID = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);

		if (npcUUID == null)
			return;

		NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);

		npc.teleport(loc, TeleportCause.PLUGIN);
		CitizensAPI.getNPCRegistry().saveToStore();
		
	}
	
	public void createNPC(int shopId) {
		NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "shop_" + shopId);
		CitizensAPI.getNPCRegistry().saveToStore();
		
		this.instance.getNPCFileManager().setNPCinConfig(shopId, npc.getUniqueId());
	}

	public void spawnAndEditNPC(int shopId, String prefix, UUID ownerUUID, String playerName) {

		Location loc = this.instance.getNPCFileManager().getNPCLocForShop(shopId);
		if (loc == null)
			return;
		
		UUID uuid = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);
		
		NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(uuid);
		
		if(npc == null) {
			this.instance.getLogger().warning("Please use the command /shop setNPC again for the Shop: " + shopId + ". It seems like there was problem.");
			return;
		}
		 
		npc.data().remove("player-skin-name");
		npc.data().remove("player-skin-textures");
		npc.data().remove("player-skin-signature");
		npc.data().remove("cached-skin-uuid-name");
		npc.data().remove("cached-skin-uuid");

		// Set the skin
		npc.data().set("player-skin-use-latest-skin", true);
		npc.data().set("cached-skin-uuid-name", playerName);
		npc.data().set("cached-skin-uuid", ownerUUID);
		npc.data().setPersistent("player-skin-name", playerName);

		npc.setProtected(true);
		
		String customName = instance.manageFile().getBoolean("Options.useDisplayName") ? prefix + playerName: ChatColor.translateAlternateColorCodes('&', "&6" + playerName);
		npc.setName(customName);

        npc.getOrAddTrait(Spawned.class).setSpawned(true);
		npc.spawn(loc);
		npc.getEntity().setMetadata("shopid", new FixedMetadataValue(this.instance, String.valueOf(shopId))); // PUTTING THE SHOP AS ENTITY META
		CitizensAPI.getNPCRegistry().saveToStore();
	}
}
