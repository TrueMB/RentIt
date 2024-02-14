package me.truemb.rentit.guiholder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import me.truemb.rentit.enums.GuiType;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.ShopInventoryType;

public class GuiHolder implements InventoryHolder {
	
	private RentTypes rentType;
	private GuiType guiType;
	private ShopInventoryType shopInvType;
	
	private int id; //Shop or Hotel ID
	
	public GuiHolder(RentTypes rentType, GuiType guiType) {
		this.rentType = rentType;
		this.guiType = guiType;
	}
	
	@Override
	public Inventory getInventory() {
		return null;
	}

	public RentTypes getRentType() {
		return this.rentType;
	}

	public GuiType getGuiType() {
		return this.guiType;
	}

	public int getID() {
		return this.id;
	}
	
	public ShopInventoryType getShopInvType() {
		return this.shopInvType;
	}

	public GuiHolder setID(int id) {
		this.id = id;
		return this;
	}

	public GuiHolder setShopInvType(ShopInventoryType shopInvType) {
		this.shopInvType = shopInvType;
		return this;
	}

}
