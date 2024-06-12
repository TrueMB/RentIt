package me.truemb.rentit.threads;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import me.truemb.rentit.main.Main;

public class FoliaThreadHandler extends ThreadWrapper{
	
	private Main instance;
	
	public FoliaThreadHandler(Main plugin) {
		this.instance = plugin;
	}

	@Override
	public void runTaskSync(Entity entity, Consumer<Void> consumer) {
		entity.getScheduler().run(this.instance, (t) -> consumer.accept(null), null);
	}
	
	@Override
	public void runTaskSync(Location loc, Consumer<Void> consumer) {
		this.instance.getServer().getRegionScheduler().run(this.instance, loc, (t) -> consumer.accept(null));
	}

	@Override
	public void runTaskTimerSync(Entity entity, Consumer<Void> consumer, long delayInTicks, long periodeInTicks) {
		entity.getScheduler().runAtFixedRate(this.instance, (t) -> consumer.accept(null), null, delayInTicks, periodeInTicks);
		
	}
	
	@Override
	public void runTaskLaterSync(Entity entity, Consumer<Void> consumer, long delayInTicks) {
		entity.getScheduler().runDelayed(this.instance, (t) -> consumer.accept(null), null, delayInTicks);
	}

	@Override
	public void runTaskLaterSync(Location loc, Consumer<Void> consumer, long delayInTicks) {
		this.instance.getServer().getRegionScheduler().runDelayed(this.instance, loc, (t) -> consumer.accept(null), delayInTicks);
	}
	
	@Override
	public void runTaskAsync(Consumer<Void> consumer) {
		GlobalRegionScheduler globalScheduler = this.instance.getServer().getGlobalRegionScheduler();
		globalScheduler.run(this.instance, (t) -> consumer.accept(null));
	}

	@Override
	public void runTaskTimerAsync(Consumer<Void> consumer, long delayInTicks, long periodeInTicks) {
		GlobalRegionScheduler globalScheduler = this.instance.getServer().getGlobalRegionScheduler();
		globalScheduler.runAtFixedRate(this.instance, (t) -> consumer.accept(null), delayInTicks, periodeInTicks);
	}

	@Override
	public void runTaskLaterAsync(Consumer<Void> consumer, long delayInTicks) {
		GlobalRegionScheduler globalScheduler = this.instance.getServer().getGlobalRegionScheduler();
		globalScheduler.runDelayed(this.instance, (t) -> consumer.accept(null), delayInTicks);
		
	}

	@Override
	public void stopTasks() {
		GlobalRegionScheduler globalScheduler = this.instance.getServer().getGlobalRegionScheduler();
		globalScheduler.cancelTasks(this.instance);
	}

}
