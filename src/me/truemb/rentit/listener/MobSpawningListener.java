package me.truemb.rentit.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import me.truemb.rentit.main.Main;

public class MobSpawningListener implements Listener {

	private Main instance;
	
	public MobSpawningListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpawn(CreatureSpawnEvent e) {
		LivingEntity en = e.getEntity();
		
		if(this.instance.getVillagerUtils() == null)
			return;

		if(en instanceof Villager) {
			Villager vil = (Villager) en;
			System.out.println(vil.getMetadata("shopid").size());
			if(vil.getMetadata("shopid") != null && vil.getMetadata("shopid").size() > 0) {
				System.out.println("4");
				e.setCancelled(false);
			}
		}
	}


}
