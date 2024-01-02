package me.truemb.rentit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.gui.ShopBuyOrSell;
import me.truemb.rentit.main.Main;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;

public class NPCShopListener implements Listener{
	
	private Main instance;
	
	public NPCShopListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
		
	}
	
	
	@EventHandler
	public void onNPCSpawn(NPCSpawnEvent e) {
		
		NPC npc = e.getNPC();
		int shopId = this.instance.getNPCFileManager().getShopIdFromNPC(npc.getUniqueId());

		if(shopId > 0)
			npc.getEntity().setMetadata("shopid", new FixedMetadataValue(this.instance, String.valueOf(shopId))); //PUTTING THE SHOP AS ENTITY META
	}
	

	//SHOP SELL INVENTORY

	@EventHandler
	public void onRightClick(NPCRightClickEvent e) {
		Player p = e.getClicker();
		NPC npc = e.getNPC();

		if (npc.getEntity() != null && npc.getEntity().hasMetadata("shopid")) {
			int shopId = npc.getEntity().getMetadata("shopid").get(0).asInt();
			p.openInventory(ShopBuyOrSell.getSelectInv(this.instance, shopId));
		}
	}
}
