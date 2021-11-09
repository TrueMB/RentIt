package me.truemb.rentit.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.PluginManager;

import me.truemb.rentit.gui.ShopBuyOrSell;
import me.truemb.rentit.main.Main;

public class VillagerShopListener implements Listener{
	
	private Main instance;
	
	public VillagerShopListener(Main plugin) {
		this.instance = plugin;
		
		PluginManager pm = this.instance.getServer().getPluginManager();
		pm.registerEvents(this, this.instance);
		
	}

	@EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        
        Entity en = e.getEntity();
        
        if(!(en instanceof Villager)) 
        	return;
        
		Villager v = (Villager) en;
		
		if(this.instance.getVillagerUtils().isShopVillager(v))
			e.setCancelled(true);
	}
	

	@EventHandler
    public void onDMG(EntityDamageEvent e) {
		        
        Entity en = e.getEntity();
        
        if(!(en instanceof Villager)) 
        	return;
        
		Villager v = (Villager) en;
		
		if(this.instance.getVillagerUtils().isShopVillager(v)) {
			e.setCancelled(true);
		}

	}
	
	@EventHandler
	public void onRightClick(PlayerInteractEntityEvent e) {

		Player p = e.getPlayer();
        Entity en = e.getRightClicked();
        
        if(!(en instanceof Villager)) 
        	return;
        
		Villager v = (Villager) en;
		
		if (v.hasMetadata("shopid")) {
			e.setCancelled(true);
			int shopId = v.getMetadata("shopid").get(0).asInt();
			p.openInventory(ShopBuyOrSell.getSelectInv(this.instance, shopId));
		}
	}
}
