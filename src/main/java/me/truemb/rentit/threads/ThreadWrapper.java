package me.truemb.rentit.threads;

import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public abstract class ThreadWrapper {

	public abstract void runTaskSync(Entity entity, Consumer<Void> consumer);
	
	public abstract void runTaskSync(Location loc, Consumer<Void> consumer);
	
	public abstract void runTaskLaterSync(Entity entity, Consumer<Void> consumer, long delayInTicks);
	
	public abstract void runTaskLaterSync(Location loc, Consumer<Void> consumer, long delayInTicks);
	
	public abstract void runTaskTimerSync(Entity entity,Consumer<Void> consumer, long delayInTicks, long periodeInTicks);
	
	public abstract void runTaskAsync(Consumer<Void> consumer);
	
	public abstract void runTaskLaterAsync(Consumer<Void> consumer, long delayInTicks);
	
	public abstract void runTaskTimerAsync(Consumer<Void> consumer, long delayInTicks, long periodeInTicks);
	
	public abstract void stopTasks();

}
