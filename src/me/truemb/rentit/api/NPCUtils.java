package me.truemb.rentit.api;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.FixedMetadataValue;

import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Spawned;
import net.citizensnpcs.trait.SkinTrait;
import net.citizensnpcs.util.PlayerAnimation;

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

        npc.getOrAddTrait(Spawned.class).setSpawned(false);
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
		
		String skinSignature = this.instance.manageFile().getString("Options.fixedSkinSignature");
		String skinTexture = this.instance.manageFile().getString("Options.fixedSkinTexture");
		boolean shouldSit = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId) != null 
				&& this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId).getCatID() + "." + CategorySettings.npcShouldSit.toString()) 
				? this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId).getCatID() + "." + CategorySettings.npcShouldSit.toString()) : false;
		
        // Set the skin
		if(skinTexture != null && !skinTexture.equals("") && skinSignature != null && !skinSignature.equals(""))
	        npc.getOrAddTrait(SkinTrait.class).setSkinPersistent("RENTIT", skinSignature, skinTexture);
		else
	        npc.getOrAddTrait(SkinTrait.class).setSkinName(playerName);
		
		npc.setProtected(true);
		
		String customName = this.instance.manageFile().getBoolean("Options.useDisplayName") ? prefix + playerName: ChatColor.translateAlternateColorCodes('&', "&6" + playerName);
		npc.setName(customName);
		
		npc.spawn(loc);
		
		if(npc.getEntity() != null)
			npc.getEntity().setMetadata("shopid", new FixedMetadataValue(this.instance, String.valueOf(shopId))); // PUTTING THE SHOP AS ENTITY META

		if(shouldSit) {
			Bukkit.getScheduler().runTaskLater(this.instance, new Runnable() {
				
				@Override
				public void run() {
					npc.teleport(loc, TeleportCause.PLUGIN);
					PlayerAnimation.SIT.play((Player) npc.getEntity());
				}
			}, 10);
		}

		CitizensAPI.getNPCRegistry().saveToStore();
	}
}
