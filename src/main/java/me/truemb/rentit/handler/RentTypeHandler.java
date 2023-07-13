package me.truemb.rentit.handler;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.truemb.rentit.enums.CategorySettings;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.UserShopGUI;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class RentTypeHandler {
	
	private Main instance;
	
	private RentTypes type;
	private int id = -1;
	private String alias = null;
	private int catID = -1;
	
	private UUID ownerUUID;
	private String ownerName;
	private Timestamp nextPayment;
	private boolean autoPayment;
	
	private Timestamp reminder;
	private boolean reminded;
	
	
	private HashMap<Integer, Inventory> sellInvHash; //INVENTORY IS ALREADY LOADED AND KEEPS THE INSTANCE. SO EVERYBODY UPDATE, IF SOMETHING CHANGES
	private HashMap<Integer, Inventory> buyInvHash;
	
	public RentTypeHandler(Main plugin, RentTypes type, int id, int catID, UUID ownerUUID, String ownerName, Timestamp nextPayment, boolean autoPayment) {
		this.instance = plugin;
		
		this.type = type;
		this.id = id;
		this.catID = catID;

		this.setOwner(ownerUUID, ownerName);
		this.setAutoPayment(autoPayment);
		this.setNextPayment(nextPayment);
	}
	
	//GET METHODES
	public int getID() {
		return this.id;
	}
	
	public String getAlias() {
		return this.alias;
	}

	public int getCatID() {
		return this.catID;
	}

	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public Timestamp getNextPayment() {
		return this.nextPayment;
	}

	public Timestamp getReminder() {
		return this.reminder;
	}

	public boolean isReminded() {
		return reminded;
	}
	
	public boolean isAutoPayment() {
		return this.autoPayment;
	}

	public RentTypes getType() {
		return this.type;
	}


	//SET METHODES
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public void setOwner(UUID ownerUUID, String ingameName) {
		this.ownerUUID = ownerUUID;
		this.ownerName = ingameName;
	}
	
	public void setAutoPayment(boolean autoPayment) {
		this.autoPayment = autoPayment;
	}

	public void setNextPayment(Timestamp nextPayment) {
		this.nextPayment = nextPayment;
		
		//NEXT PAYMENT CHANGED - Reminder needs to change as well
		if(nextPayment != null)
			this.calculateReminderTimestamp();
	}

	public void setReminder(Timestamp reminder) {
		this.reminder = reminder;
	}

	public void setReminded(boolean reminded) {
		this.reminded = reminded;
	}

	//OTHERS
	public void reset() {
		
		//PERMISSIONS RESET IN CACHE
		for(Player all : Bukkit.getOnlinePlayers()) {
		
			UUID uuid = all.getUniqueId();
			if(!this.instance.playerHandlers.containsKey(uuid))
				continue;
			
			PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
			
			if(playerHandler == null)
				continue;
			
			PermissionsHandler permsHandler = playerHandler.getPermsHandler(type);

			if(permsHandler == null)
				continue;
			
			permsHandler.reset(this.getID());
		}
		
		//REMOVE OWNING LIST ENTRY FROM OWNER IN CACHE
		UUID uuid = this.getOwnerUUID();

		PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
		
		if(playerHandler != null)
			playerHandler.removeOwningRent(this.type, this.getID());
		
		//RESET CACHE
		this.setOwner(null, null);
		this.setNextPayment(null);
		this.setReminder(null);
		this.setReminded(false);
	}
	
	public boolean isOwned() {
		return this.getOwnerUUID() != null;
	}
	
	/**
	 * Calculates the Reminder, if it is needed
	 */
	public void calculateReminderTimestamp() {
		
		String timeS = this.instance.manageFile().isSet("Options.categorySettings." + StringUtils.capitalize(this.getType().toString().toLowerCase()) + "Category." + this.getCatID() + "." + CategorySettings.reminderRentRunningOut.toString()) ? 
				this.instance.manageFile().getString("Options.categorySettings." + StringUtils.capitalize(this.getType().toString().toLowerCase()) + "Category." + this.getCatID() + "." + CategorySettings.reminderRentRunningOut.toString()) : null;
		
		if(timeS == null) return; //NO REMINDER SET
		
		//GETS TIME BEFORE THE RENT IS RUNNING OUT
		Timestamp reminderTs = UtilitiesAPI.getTimestampBefore(this.getNextPayment(), timeS);
		
		this.setReminder(reminderTs);
	}

	public void setBuyInv(int site, Inventory buyInv) {
		Inventory inv = this.buyInvHash.get(site);
		if(inv != null) {
			
			//Inventory got the same size. Changing the content
			if(inv.getSize() == buyInv.getSize()) {
				inv.setContents(buyInv.getContents());

			//Inventory is not the same size. Reopening the Inventory
			}else {
				List<HumanEntity> list = inv.getViewers();
				for(int i = list.size() - 1; i >= 0; i--)
					list.get(i).openInventory(buyInv);
			}
		}
		this.buyInvHash.put(site, buyInv);
	}

	public void setSellInv(int site, Inventory sellInv) {
		Inventory inv = this.sellInvHash.get(site);
		if(inv != null) {
			
			//Inventory got the same size. Changing the content
			if(inv.getSize() == sellInv.getSize()) {
				inv.setContents(sellInv.getContents());

			//Inventory is not the same size. Reopening the Inventory
			}else {
				List<HumanEntity> list = inv.getViewers();
				for(int i = list.size() - 1; i >= 0; i--)
					list.get(i).openInventory(sellInv);
			}
		}
		this.sellInvHash.put(site, sellInv);
	}
	
	public int searchMaterial(Material m) {
		int amount = 0;
		for(Inventory inv : this.sellInvHash.values()) {
			amount += inv.all(m).size();
		}
		return amount;
	}
	
	public Collection<Inventory> getBuyInventories(){
		return this.buyInvHash.values();
	}

	public Inventory getBuyInv(int site) {
		Inventory result = this.buyInvHash.get(site);
		
		if(result == null)
			this.buyInvHash.put(site, result = UserShopGUI.getBuyInv(this.instance, this.getID(), site, null));
		
		return result;
	}

	public Collection<Inventory> getSellInventories(){
		return this.sellInvHash.values();
	}
	
	public Inventory getSellInv(int site) {
		Inventory result = this.sellInvHash.get(site);
		
		if(result == null)
			this.sellInvHash.put(site, result = UserShopGUI.getSellInv(this.instance, this.getID(), site, null));
		
		return result;
	}

}
