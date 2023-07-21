package me.truemb.rentit.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.handler.RentTypeHandler;

public class ItemBuyEvent extends Event implements Cancellable{
	
	private static final HandlerList handlers = new HandlerList();
	
	private Player buyer;
	private RentTypeHandler rentHandler;
	private double price;
	private ItemStack item;
	
	private boolean cancel;
		
	public ItemBuyEvent(Player buyer, RentTypeHandler rentHandler, ItemStack item, double price) {
		this.buyer = buyer;
		this.rentHandler = rentHandler;
		this.item = item;
		this.price = price;
	}
	
	public RentTypeHandler getRentTypeHandler() {
		return this.rentHandler;
	}
	
	public double getPrice() {
		return this.price;
	}
	public ItemStack getItem() {
		return this.item;
	}
	
	public Player getBuyer() {
		return this.buyer;
	}

	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return this.cancel;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}

}
