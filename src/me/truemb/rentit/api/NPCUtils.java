package me.truemb.rentit.api;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.FixedMetadataValue;

import com.denizenscript.denizen.npc.traits.SittingTrait;

import me.truemb.rentit.main.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;

public class NPCUtils {
	
	private Main instance;
	
	public NPCUtils(Main plugin) {
		this.instance = plugin;
	}
	
	private NPC getNPC(int shopId) {
		UUID npcUUID = this.instance.getNPCFileManager().getNPCIdFromShop(shopId);

		if (npcUUID == null)
			return null;

		return CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);
	}
	
	public void createNPC(int shopId) {
		NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "shop_" + shopId);
		CitizensAPI.getNPCRegistry().saveToStore();
		
		this.instance.getNPCFileManager().setNPCinConfig(shopId, npc.getUniqueId());
	}
	
	public void destroyNPC(int shopId) {
		NPC npc = this.getNPC(shopId);
		if(npc == null)
			return;

		npc.destroy();
		CitizensAPI.getNPCRegistry().saveToStore();
	}
	
	public boolean existsNPCForShop(int shopId) {
		NPC npc = this.getNPC(shopId);
		return npc != null;
	}

	public void despawnNPC(int shopId) {
		NPC npc = this.getNPC(shopId);

		if(npc == null)
			return;

        npc.despawn(DespawnReason.PLUGIN);
	}
	
	public boolean isNPCSpawned(int shopId) {
		NPC npc = this.getNPC(shopId);
		return npc != null && npc.isSpawned();
	}

	public void moveNPC(int shopId, Location loc) {
		NPC npc = this.getNPC(shopId);
		if(npc == null)
			return;

		npc.teleport(loc, TeleportCause.PLUGIN);
		CitizensAPI.getNPCRegistry().saveToStore();
		
	}

	public void sitNPC(int shopId, boolean value) {
		NPC npc = this.getNPC(shopId);
		if(npc == null)
			return;

		SittingTrait trait = npc.getOrAddTrait(SittingTrait.class);
		if(value) 
			trait.sit();
		else
			trait.stand();
		
		CitizensAPI.getNPCRegistry().saveToStore();
		
	}
	
	public void spawnAndEditNPC(int shopId, String prefix, UUID ownerUUID, String playerName) {

		Location loc = this.instance.getNPCFileManager().getNPCLocForShop(shopId);
		if (loc == null)
			return;

		NPC npc = this.getNPC(shopId);
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
		
		npc.spawn(loc);
		if(npc.getEntity() != null)
			npc.getEntity().setMetadata("shopid", new FixedMetadataValue(this.instance, String.valueOf(shopId))); // PUTTING THE SHOP AS ENTITY META
		CitizensAPI.getNPCRegistry().saveToStore();
	}
}
