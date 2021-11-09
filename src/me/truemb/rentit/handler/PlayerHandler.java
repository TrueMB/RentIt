package me.truemb.rentit.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;

public class PlayerHandler {
	
	private Player player;
	
	private HashMap<RentTypes, List<Integer>> owningRents = new HashMap<>();
	
	private HashMap<RentTypes, SettingsHandler> settingHandlers = new HashMap<>(); //Contains for each type one SettingsHandler. In the Settingshandler are alle the shops/hotels
	private HashMap<RentTypes, PermissionsHandler> permsHandlers = new HashMap<>(); //Contains Permissions Handler
	
	public PlayerHandler(Player player) {
		this.player = player;
	}
	
	public void init(Main plugin) {
		
		UUID uuid = PlayerManager.getUUID(this.getPlayer());
		
		plugin.playerHandlers.put(uuid, this);
		
		//OWNINGS
		plugin.getShopsSQL().setupOwningIds(this); //SHOP
		plugin.getHotelsSQL().setupOwningIds(this); //HOTEL
		
		//SETTINGS
		for(RentTypes type : RentTypes.values()) {
			SettingsHandler settingsHandler = new SettingsHandler();
			settingsHandler.init(plugin, uuid, type);
			this.setSettingsHandler(type, settingsHandler);
		}
		
		//PERMISSIONS
		for(RentTypes type : RentTypes.values()) {
			PermissionsHandler permsHandler = new PermissionsHandler();
			permsHandler.init(plugin, uuid, type);
			this.setPermsHandler(type, permsHandler);
		}
	}

	//GETTER
	public SettingsHandler getSettingsHandler(RentTypes type) {
		return this.settingHandlers.get(type);
	}
	
	public PermissionsHandler getPermsHandler(RentTypes type) {
		return this.permsHandlers.get(type);
	}
	
	public List<Integer> getOwningList(RentTypes type) {
		List<Integer> ids = new ArrayList<>();
		if(this.owningRents.containsKey(type))
			ids = this.owningRents.get(type);
		return ids;
	}

	public Player getPlayer() {
		return this.player;
	}

	//SETTER
	public void setSettingsHandler(RentTypes type, SettingsHandler settingHandler) {
		this.settingHandlers.put(type, settingHandler);
	}
	
	public void setPermsHandler(RentTypes type, PermissionsHandler permsHandler) {
		this.permsHandlers.put(type, permsHandler);
	}

	public void addOwningRent(RentTypes type, int id) {
		List<Integer> ids = new ArrayList<>();
		if(this.owningRents.containsKey(type))
			ids = this.owningRents.get(type);
		ids.add(id);
		this.owningRents.put(type, ids);
	}
	
	public void removeOwningRent(RentTypes type, int id) {
		List<Integer> ids = new ArrayList<>();
		if(this.owningRents.containsKey(type))
			ids = this.owningRents.get(type);
		
		for(int i = 0; i < ids.size(); i++)
			if(ids.get(i) == id)
				ids.remove(i);
		
		this.owningRents.put(type, ids);
	}

	public void setOwningRent(RentTypes type, List<Integer> ids) {
		this.owningRents.put(type, ids);
	}
}
