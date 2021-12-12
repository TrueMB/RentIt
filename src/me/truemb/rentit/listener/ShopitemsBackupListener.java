package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;

public class ShopitemsBackupListener implements Listener {

	private Main instance;
	
	private NamespacedKey idKey;
	private NamespacedKey typeKey;
	private NamespacedKey ownerKey;

	public ShopitemsBackupListener(Main plugin) {
		this.instance = plugin;

		this.idKey = new NamespacedKey(this.instance, "id");
		this.typeKey = new NamespacedKey(this.instance, "type");
		this.ownerKey = new NamespacedKey(this.instance, "id");
		
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler
	public void onInteractWithItemBackup(PlayerInteractEvent e) {
		
		Player p = e.getPlayer();
		UUID uuid = p.getUniqueId();
		
		if(e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		int id = -1;
		RentTypes type = null;
		UUID ownerUUID = null;
		
		//Check Item for needed values
		if(p.getEquipment().getItemInMainHand() != null && p.getEquipment().getItemInMainHand().getType() != Material.AIR) {
			ItemStack item = p.getEquipment().getItemInMainHand();
			ItemMeta meta = item.getItemMeta();
			PersistentDataContainer container = meta.getPersistentDataContainer();
			
			id = container.getOrDefault(this.idKey, PersistentDataType.INTEGER, -1);
			
			String typeS = container.getOrDefault(this.typeKey, PersistentDataType.STRING, null);
			type = typeS != null ? RentTypes.valueOf(typeS.toUpperCase()) : null;
			
			String ownerS = container.getOrDefault(this.ownerKey, PersistentDataType.STRING, null);
			ownerUUID = ownerS != null ? UUID.fromString(ownerS) : null;
			
		}
		
		//COULDNT FIND ANY DATA
		if(id == -1 || type == null || ownerUUID == null)
			return;
		
		//FOUND DATA
		//Is Player Owner or Admin?
		if(!ownerUUID.equals(uuid) && !p.hasPermission(this.instance.manageFile().getString("Permissions.backupitems"))) {
			p.sendMessage(this.instance.getMessage("notOwnBackupItems"));
			return;
		}
		
		//Open Backup Items
		
		
	}

}
