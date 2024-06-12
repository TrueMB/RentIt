package me.truemb.rentit.threads;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import me.truemb.rentit.main.Main;

public class SpigotThreadHandler extends ThreadWrapper{
	
	private Main instance;
	
	public SpigotThreadHandler(Main plugin) {
		this.instance = plugin;
	}

	@Override
	public void runTaskSync(Entity entity, Consumer<Void> consumer) {
		Bukkit.getScheduler().runTask(this.instance, () -> consumer.accept(null));
	}
	
	@Override
	public void runTaskSync(Location loc, Consumer<Void> consumer) {
		Bukkit.getScheduler().runTask(this.instance, () -> consumer.accept(null));
	}

	@Override
	public void runTaskTimerSync(Entity entity, Consumer<Void> consumer, long delayInTicks, long periodeInTicks) {
		Bukkit.getScheduler().runTaskTimer(this.instance, () -> consumer.accept(null), delayInTicks, periodeInTicks);
	}
	
	@Override
	public void runTaskLaterSync(Entity entity, Consumer<Void> consumer, long delayInTicks) {
		Bukkit.getScheduler().runTaskLater(this.instance, () -> consumer.accept(null), delayInTicks);
	}
	
	@Override
	public void runTaskLaterSync(Location loc, Consumer<Void> consumer, long delayInTicks) {
		Bukkit.getScheduler().runTaskLater(this.instance, () -> consumer.accept(null), delayInTicks);
	}

	@Override
	public void runTaskAsync(Consumer<Void> consumer) {
		Bukkit.getScheduler().runTaskAsynchronously(this.instance, () -> consumer.accept(null));
	}

	@Override
	public void runTaskTimerAsync(Consumer<Void> consumer, long delayInTicks, long periodeInTicks) {
		Bukkit.getScheduler().runTaskTimerAsynchronously(this.instance, () -> consumer.accept(null), delayInTicks, periodeInTicks);
		
	}

	@Override
	public void runTaskLaterAsync(Consumer<Void> consumer, long delayInTicks) {
		Bukkit.getScheduler().runTaskLaterAsynchronously(this.instance, () -> consumer.accept(null), delayInTicks);
		
	}

	@Override
	public void stopTasks() {
		Bukkit.getScheduler().cancelTasks(this.instance);
	}

}
